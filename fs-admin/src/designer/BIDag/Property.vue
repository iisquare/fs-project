<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import LayoutIcon from '@/components/Layout/LayoutIcon.vue'
import CodeEditor from '@/components/Editor/CodeEditor.vue'
import DatasourceApi from '@/api/bi/DatasourceApi'
import DiagramApi from '@/api/bi/DiagramApi'
import OntologyApi from '@/api/kg/OntologyApi'
import ApiUtil from '@/utils/ApiUtil'
import SqlUtil from '@/utils/SqlUtil'
import PropertyPanel from './PropertyPanel.vue'
import PropertySection from '@/components/Layout/PropertySection.vue'
import TipText from '@/components/Layout/TipText.vue'
import FieldMapEditor from './FieldMapEditor.vue'
import TableBrowser from './TableBrowser.vue'

const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
const props = defineProps<{
  config: any,
  instance: any,
  context?: any,
}>()

/** 依据节点类型取参数编辑字段 */
const fields = computed(() => {
  const type = model.value?.data?.type
  return (props.config?.fields ?? {})[type] ?? []
})

const nodeGuide = computed(() => {
  const type = model.value?.data?.type
  return (props.config?.nodeGuides ?? {})[type] ?? null
})

const fieldHelps = computed(() => fields.value.filter((field: any) => field.help))

const mappingSources = ref<any[]>([])
const mappingTargets = ref<any[]>([])
const mappingLoading = ref(false)
const mappingMessage = ref('')
const mappingInputNames = ref('')
const mappingOutputNames = ref('')
const sqlEditorRef = ref()
const tableBrowserVisible = ref(false)
const tableBrowserSqlMode = ref(false)
const tableBrowserField = ref<any>(null)

const resetMappingSchema = () => {
  mappingSources.value = []
  mappingTargets.value = []
  mappingLoading.value = false
  mappingMessage.value = ''
  mappingInputNames.value = ''
  mappingOutputNames.value = ''
}

const hydrateMappingSchema = () => {
  resetMappingSchema()
  const schema = model.value?.data?.options?.mappingSchema
  if (!schema || typeof schema !== 'object') return
  mappingSources.value = Array.isArray(schema.sources) ? schema.sources : []
  mappingTargets.value = Array.isArray(schema.targets) ? schema.targets : []
  mappingInputNames.value = schema.inputNames || ''
  mappingOutputNames.value = schema.outputNames || ''
  mappingMessage.value = schema.message || ''
}

watch(() => model.value?.id, hydrateMappingSchema, { immediate: true })

const optionValue = (item: any) => typeof item === 'object' ? item.value : item
const optionLabel = (item: any) => typeof item === 'object' ? item.label : item

/**
 * SQLTransform 会把直接上游数据以「上游节点名称」注册为临时表。
 * 这里按同一规则展示当前可引用的表名，便于编写和排查 SQL。
 */
const sqlInputTables = computed(() => {
  if (model.value?.data?.type !== 'SQLTransform') return []
  const graph = props.instance?.flow?.graph
  const cell = graph?.getCellById?.(model.value.id) ?? model.value
  const edges: any[] = graph?.getIncomingEdges?.(cell) ?? []
  const visited = new Set<string>()
  const tables: Array<{ id: string, name: string }> = []
  for (const edge of edges) {
    const source = edge.getSourceCell?.()
      ?? graph?.getCellById?.(edge.getSourceCellId?.())
    if (!source || visited.has(source.id)) continue
    visited.add(source.id)
    const data = source.getData?.() ?? source.data ?? {}
    if (String(data.type ?? '').endsWith('Config')) continue
    tables.push({ id: source.id, name: String(data.name ?? '') })
  }
  return tables
})

/** 后端使用 createOrReplaceTempView/createTemporaryView，同名上游后注册的会覆盖先注册的 */
const duplicateSqlTables = computed(() => {
  const counts = new Map<string, number>()
  sqlInputTables.value.forEach(table => {
    counts.set(table.name, (counts.get(table.name) ?? 0) + 1)
  })
  return Array.from(counts)
    .filter(([name, count]) => name && count > 1)
    .map(([name]) => name)
})

const sqlEditorHints = computed(() => {
  if (model.value?.data?.type !== 'SQLTransform') return []
  return sqlInputTables.value
    .filter(table => table.name)
    .map(table => ({
      text: `\`${table.name}\``,
      displayText: table.name,
      className: 'fs-sql-hint-table',
    }))
})

const options = computed(() => {
  if (!model.value.data.options || typeof model.value.data.options !== 'object') {
    model.value.data.options = {}
  }
  // 兼容历史 Elasticsearch 节点使用 index 保存索引名的数据
  if (model.value.data.type === 'ElasticsearchSink'
      && !model.value.data.options.collection
      && model.value.data.options.index) {
    model.value.data.options.collection = model.value.data.options.index
  }
  return model.value.data.options
})

const handleDelete = () => {
  props.instance.flow.remove(model.value)
}

const render = (value: any) => {
  if (undefined === value || null === value) return ''
  if ('object' === typeof value) return JSON.stringify(value, null, 2)
  return String(value)
}

const parse = (key: string, text: string) => {
  try {
    options.value[key] = text.trim() ? JSON.parse(text) : ''
  } catch (e) {
    options.value[key] = text
  }
}

/** 数据源节点统一从数据源管理中选择连接，避免在编排中重复维护账号和连接地址 */
const loadDatasource = async (field: any, params: any) => {
  const types: string[] = field.datasourceTypes?.length ? field.datasourceTypes : ['']
  const results = await Promise.all(types.map(type => DatasourceApi.list({
    ...params,
    ...(type ? { type } : {}),
  })))
  const rows = results
    .flatMap((result: any) => result.data?.rows ?? [])
    .filter((row: any, index: number, list: any[]) => list.findIndex(item => item.id === row.id) === index)
    .map((row: any) => ({ ...row, disabled: String(row.status) !== '1' }))
  const first: any = results[0] ?? { data: {} }
  return {
    ...first,
    data: {
      ...(first.data ?? {}),
      rows,
    },
  }
}

const datasourceCallback = (field: any) => (params: any) => loadDatasource(field, params)

const datasourceLabel = (row: any) => {
  const type = row.typeText || row.type
  return [row.name, type].filter(Boolean).join(' · ') + (String(row.status) === '1' ? '' : '（已禁用）')
}

/** 知识图谱本体只用于解析实体和字段，实际数据仍由节点选择的 Neo4j 数据源承载 */
const loadOntology = async (params: any) => {
  const result: any = await OntologyApi.list(params)
  const data = result?.data ?? {}
  return {
    ...result,
    data: {
      ...data,
      rows: (data.rows ?? []).map((row: any) => ({
        ...row,
        disabled: String(row.status) !== '1',
      })),
    },
  }
}

const ontologyLabel = (row: any) => {
  const name = row.name || row.code || row.id
  return row.label ? `${name}（${row.label}）` : name
}

const loadOntologyEntities = async (params: any) => {
  const ontologyId = options.value.ontologyId
  if (!ontologyId) return { data: { rows: [] } }
  const result: any = await OntologyApi.model({ id: ontologyId }, { warning: false })
  const data = result?.data ?? {}
  const rows = (data.entities ?? []).map((entity: any) => ({
    ...entity,
    id: entity.label,
    disabled: !entity.label,
  }))
  return {
    ...result,
    data: {
      ...data,
      rows,
    },
  }
}

const ontologyEntityLabel = (row: any) => {
  return row.name && row.name !== row.label ? `${row.name}（${row.label}）` : row.label
}

const updateOntology = (value: any) => {
  options.value.ontologyId = value
  options.value.entityLabel = ''
  delete options.value.entity
}

const updateDatasource = async (field: any, value: any) => {
  options.value[field.key] = value
  for (const key of field.clearOptions ?? []) delete options.value[key]
  const databaseField = fields.value.find((item: any) => item.resourceDatabaseField)?.resourceDatabaseField
  if (!(Number(value) > 0) || !databaseField || options.value[databaseField]) return
  try {
    const result: any = await DatasourceApi.list({ id: Number(value), page: 1, pageSize: 1 }, { success: false })
    if (String(options.value[field.key]) !== String(value)) return
    const row = ApiUtil.data(result)?.rows?.[0]
    const content = parseJson(row?.content)
    const database = String(content?.database ?? '').trim()
    if (database) options.value[databaseField] = database
  } catch {
    // 默认数据库只用于交互预填，读取失败不阻断数据源选择
  }
}

const activeSqlField = computed(() => fields.value.find((field: any) => field.component === 'sql'))
const sqlEditor = () => Array.isArray(sqlEditorRef.value) ? sqlEditorRef.value[0] : sqlEditorRef.value

const formatSql = (field: any = activeSqlField.value) => {
  if (!field) return
  const value = String(options.value[field.key] ?? '')
  if (!value.trim()) {
    ElMessage.warning('请先输入 SQL 语句')
    return
  }
  try {
    const editor = sqlEditor()
    const selected = editor?.getSelection?.() || ''
    if (selected.trim()) {
      const formatted = SqlUtil.format(selected, field.dialect || 'sql')
      if (formatted !== selected) editor?.replaceSelection?.(formatted)
    } else {
      options.value[field.key] = SqlUtil.format(value, field.dialect || 'sql')
    }
  } catch (error: any) {
    ElMessage.error('SQL 格式化失败：' + (error?.message || error))
  }
}

const sqlEditorKeys = {
  'Shift-Alt-F': () => {
    formatSql()
    return true
  },
}

const openTableBrowser = (sqlMode: boolean, field: any) => {
  if (!(Number(options.value.datasourceId) > 0)) {
    ElMessage.warning('请先选择数据源')
    return
  }
  tableBrowserSqlMode.value = sqlMode
  tableBrowserField.value = field
  tableBrowserVisible.value = true
}

const tableBrowserSelected = computed(() => {
  if (tableBrowserSqlMode.value || !tableBrowserField.value) return ''
  return options.value[tableBrowserField.value.key] || ''
})

const tableBrowserDatabase = computed(() => {
  const field = tableBrowserField.value
  if (!field?.resourceDatabaseFilter || !field.resourceDatabaseField) return ''
  return String(options.value[field.resourceDatabaseField] ?? '').trim()
})

const applyTableSelection = (table: any, action: 'name' | 'query') => {
  const field = tableBrowserField.value
  if (!field) return
  const quotedName = table.quotedName || table.qualifiedName || table.name
  const editor = sqlEditor()
  if (tableBrowserSqlMode.value) {
    if (action === 'query') {
      const query = `SELECT * FROM ${quotedName}`
      const current = String(options.value[field.key] ?? '')
      if (!current.trim()) {
        options.value[field.key] = query
      } else {
        editor?.replaceSelection?.(`\n${query}`)
      }
    } else {
      editor?.replaceSelection?.(quotedName)
    }
    editor?.focus?.()
  } else {
    options.value[field.key] = table.value || table.qualifiedName || table.name
    if (field.resourceDatabaseField && table.catalog) {
      options.value[field.resourceDatabaseField] = table.catalog
    }
  }
  tableBrowserVisible.value = false
}

const setRoot = (text: string) => {
  try {
    const parsed = text.trim() ? JSON.parse(text) : {}
    model.value.data.options = (parsed && 'object' === typeof parsed && !Array.isArray(parsed)) ? parsed : {}
  } catch (e) {
    // 非法JSON暂不写回，保留上次有效配置
  }
}

const parseJson = (value: any) => {
  if (typeof value !== 'string') return value
  const text = value.trim()
  if (!text || !['{', '['].includes(text[0])) return value
  try {
    return JSON.parse(text)
  } catch {
    return value
  }
}

const typeToClass = (field: any) => {
  const text = `${field?.className ?? ''} ${field?.type ?? ''}`.toLowerCase()
  if (/timestamp|datetime/.test(text)) return 'java.sql.Timestamp'
  if (/date/.test(text)) return 'java.sql.Date'
  if (/bigint|int8|long/.test(text)) return 'java.lang.Long'
  if (/integer|int4|smallint|int2|tinyint|int\b/.test(text)) return 'java.lang.Integer'
  if (/double|float|decimal|numeric|real/.test(text)) return 'java.lang.Double'
  if (/bool/.test(text)) return 'java.lang.Boolean'
  return 'java.lang.String'
}

const normalizeSchema = (value: any, origin: string) => {
  value = parseJson(value)
  if (value && typeof value === 'object' && typeof value.preview === 'string') {
    const preview = parseJson(value.preview)
    if (preview !== value.preview) value = preview
  }
  const result: any[] = []
  const seen = new Set<string>()
  const add = (name: any, type: any = '', className: any = '') => {
    const field = String(name ?? '').trim()
    if (!field || seen.has(field)) return
    seen.add(field)
    const meta = { className, type: type || className }
    result.push({ name: field, type: meta.type || 'unknown', clsType: typeToClass(meta), origin })
  }
  const columns = Array.isArray(value?.columns) ? value.columns : []
  if (columns.length) {
    columns.forEach((column: any) => {
      add(column.name ?? column.field ?? column.label, column.type, column.className)
    })
  } else {
    const schema = value?.schema ?? value
    if (schema && typeof schema === 'object' && !Array.isArray(schema)) {
      Object.entries(schema).forEach(([name, type]) => {
        if (typeof type === 'object' && type) {
          const field: any = type
          add(name, field.type, field.className)
        } else if (name !== 'columns') {
          add(name, type)
        }
      })
    }
  }
  const rows = Array.isArray(value?.rows) ? value.rows : (Array.isArray(value) ? value : [])
  rows.forEach((row: any) => {
    if (!row || typeof row !== 'object' || Array.isArray(row)) return
    Object.entries(row).forEach(([name, raw]) => {
      const type = raw === null || raw === undefined ? 'unknown' : typeof raw
      add(name, type)
    })
  })
  return result
}

const sourceNode = (edge: any) => {
  const graph = props.instance?.flow?.graph
  return edge?.getSourceCell?.() ?? graph?.getCellById?.(edge?.getSourceCellId?.())
}

const targetNode = (edge: any) => {
  const graph = props.instance?.flow?.graph
  return edge?.getTargetCell?.() ?? graph?.getCellById?.(edge?.getTargetCellId?.())
}

const nodeInfo = (node: any) => {
  const data = node?.getData?.() ?? node?.data ?? {}
  return { id: node?.id ?? '', name: data.name || node?.id || '未命名节点', type: data.type || '', options: data.options ?? {} }
}

const downstreamSinks = (cell: any) => {
  const graph = props.instance?.flow?.graph
  if (!graph) return []
  const result: any[] = []
  const visited = new Set<string>()
  const queue = [cell]
  while (queue.length) {
    const current = queue.shift()
    for (const edge of graph.getOutgoingEdges?.(current) ?? []) {
      const node = targetNode(edge)
      if (!node || visited.has(node.id)) continue
      visited.add(node.id)
      const info = nodeInfo(node)
      if (info.type === 'JDBCSink') result.push(info)
      queue.push(node)
    }
  }
  return result
}

const saveDiagramSnapshot = () => DiagramApi.save(Object.assign({}, props.context ?? {}, {
  schedule: props.context?.schedule ? true : false,
  autoRecover: props.context?.autoRecover ? true : false,
  content: props.instance.flow.toJSON(),
}), { success: false })

const loadMappingSchema = async () => {
  if (model.value?.data?.type !== 'ConvertTransform') return
  const graph = props.instance?.flow?.graph
  const cell = graph?.getCellById?.(model.value.id) ?? model.value
  if (!graph || !cell) {
    mappingMessage.value = '无法读取画布连线'
    return
  }
  const incoming = (graph.getIncomingEdges?.(cell) ?? [])
    .map(sourceNode)
    .filter((node: any) => node)
    .map(nodeInfo)
    .filter((node: any) => !String(node.type).endsWith('Config'))
  if (!incoming.length) {
    mappingMessage.value = '请先把上游数据节点连接到字段映射节点'
    return
  }
  mappingLoading.value = true
  mappingMessage.value = ''
  mappingInputNames.value = incoming.map((node: any) => node.name).join('、')
  try {
    const saved: any = await saveDiagramSnapshot()
    const diagramId = Number(ApiUtil.data(saved)?.id)
    if (!(diagramId > 0)) throw new Error('保存编排失败，无法读取上游字段')
    if (props.context) props.context.id = diagramId
    const sources: any[] = []
    const warnings: string[] = []
    for (const node of incoming) {
      const debugId = `dag_${diagramId}_${node.id}`
      try {
        const result: any = await DiagramApi.debug({
          id: diagramId, nodeId: debugId, previewLimit: 1,
        }, { success: false })
        const data = ApiUtil.data(result) ?? {}
        const output = parseJson(data.outputs?.[debugId] ?? data.final)
        const fields = normalizeSchema(output, node.name)
        if (fields.length) sources.push(...fields)
        else warnings.push(`${node.name} 未返回字段结构`)
      } catch (error: any) {
        warnings.push(`${node.name}：${ApiUtil.message(error) || '读取失败'}`)
      }
    }
    mappingSources.value = sources.filter((field, index, list) =>
      list.findIndex(item => item.name === field.name) === index)

    const sinks = downstreamSinks(cell)
    mappingOutputNames.value = sinks.map((node: any) => node.name).join('、')
    const targets: any[] = []
    for (const sink of sinks) {
      const datasourceId = Number(sink.options?.datasourceId)
      const table = String(sink.options?.table ?? '').trim()
      if (!(datasourceId > 0) || !table) {
        warnings.push(`${sink.name} 尚未选择目标数据源或目标表`)
        continue
      }
      try {
        const result: any = await DiagramApi.columns({ datasourceId, table }, { success: false })
        targets.push(...normalizeSchema(ApiUtil.data(result), sink.name))
      } catch (error: any) {
        warnings.push(`${sink.name}：${ApiUtil.message(error) || '读取目标表字段失败'}`)
      }
    }
    mappingTargets.value = targets.filter((field, index, list) =>
      list.findIndex(item => item.name === field.name) === index)
    if (!mappingTargets.value.length) {
      mappingTargets.value = mappingSources.value.map((field: any) => ({ ...field }))
      mappingOutputNames.value = mappingOutputNames.value || '未连接固定结构的目标节点'
      warnings.push('下游未提供固定表结构，已使用上游字段名作为目标字段')
    }
    mappingMessage.value = warnings.join('；')
    options.value.mappingSchema = {
      sources: mappingSources.value,
      targets: mappingTargets.value,
      inputNames: mappingInputNames.value,
      outputNames: mappingOutputNames.value,
      message: mappingMessage.value,
      updatedAt: Date.now(),
    }
    try {
      await saveDiagramSnapshot()
    } catch (error: any) {
      warnings.push(`字段结构已读取，但自动保存失败：${ApiUtil.message(error) || '未知错误'}`)
    }
    mappingMessage.value = warnings.join('；')
  } catch (error: any) {
    mappingSources.value = []
    mappingTargets.value = []
    mappingMessage.value = ApiUtil.message(error) || '读取连线字段失败'
  } finally {
    mappingLoading.value = false
  }
}
</script>

<template>
  <property-panel title="节点属性">
    <el-form :model="model" label-position="top">
      <property-section title="基础信息" :reset-key="model.id">
        <template #extra>
          <el-popconfirm title="确认删除该节点？" width="180" @confirm="handleDelete">
            <template #reference>
              <LayoutIcon name="Delete" class="delete" />
            </template>
          </el-popconfirm>
        </template>
        <el-form-item label="类型">{{ model.data.type }}</el-form-item>
        <el-form-item label="名称">
          <el-input v-model="model.data.name" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input type="textarea" v-model="model.data.description" />
        </el-form-item>
        <el-form-item label="并行度">
          <el-input-number v-model="model.data.parallelism" :min="1" :controls="false" />
        </el-form-item>
        <el-form-item label="参数前缀">
          <el-input v-model="model.data.kvConfigPrefix" placeholder="用于引用上游配置项" />
        </el-form-item>
      </property-section>

      <property-section v-if="fields.length" title="参数配置" :reset-key="model.id">
          <el-form-item
            :label="['mapping', 'sql'].includes(field.component) ? undefined : field.label"
            v-for="field in fields"
            :key="field.key"
          >
            <el-switch v-if="field.component === 'switch'" v-model="options[field.key]" />
            <el-input-number v-else-if="field.component === 'number'" v-model="options[field.key]" :controls="false" />
            <form-select
              v-else-if="field.component === 'datasource'"
              :model-value="options[field.key]"
              :callback="datasourceCallback(field)"
              :label-formatter="datasourceLabel"
              :placeholder="field.placeholder"
              clearable
              @update:model-value="(value: any) => updateDatasource(field, value)"
            />
            <form-select
              v-else-if="field.component === 'ontology'"
              :model-value="options[field.key]"
              :callback="loadOntology"
              :label-formatter="ontologyLabel"
              :placeholder="field.placeholder"
              clearable
              @update:model-value="(value: any) => updateOntology(value)"
            />
            <form-select
              v-else-if="field.component === 'ontologyEntity'"
              :model-value="options[field.key]"
              :callback="loadOntologyEntities"
              :label-formatter="ontologyEntityLabel"
              :placeholder="options.ontologyId ? field.placeholder : '请先选择知识图谱'"
              :disabled="!options.ontologyId"
              clearable
            />
            <el-select v-else-if="field.component === 'select'" v-model="options[field.key]" clearable :placeholder="field.placeholder">
              <el-option
                v-for="item in field.options"
                :key="optionValue(item)"
                :value="optionValue(item)"
                :label="optionLabel(item)"
              />
            </el-select>
            <el-select v-else-if="field.component === 'timezone'" v-model="options[field.key]" clearable>
              <el-option v-for="item in (config.timezones || [])" :key="item" :value="item" :label="item" />
            </el-select>
            <el-select v-else-if="field.component === 'locale'" v-model="options[field.key]" clearable>
              <el-option v-for="item in (config.locales || [])" :key="item" :value="item" :label="item" />
            </el-select>
            <el-input
              v-else-if="field.component === 'textarea'"
              type="textarea"
              :rows="4"
              v-model="options[field.key]"
              :placeholder="field.placeholder"
            />
            <el-input
              v-else-if="field.component === 'json'"
              type="textarea"
              :rows="5"
              :model-value="render(options[field.key])"
              :placeholder="field.placeholder"
              @update:model-value="(value: string) => parse(field.key, value)"
            />
            <template v-else-if="field.component === 'sql'">
              <div class="sql-editor-field">
                <div class="sql-editor-field__toolbar">
                  <strong>{{ field.label }}</strong>
                  <div>
                    <el-button
                      v-if="model.data.type === 'JDBCSource'"
                      link
                      type="primary"
                      @click="openTableBrowser(true, field)"
                    >获取表名</el-button>
                    <el-button
                      link
                      type="primary"
                      title="格式化（Shift+Alt+F）"
                      @click="formatSql(field)"
                    >格式化</el-button>
                  </div>
                </div>
                <CodeEditor
                  ref="sqlEditorRef"
                  v-model="options[field.key]"
                  mode="sql"
                  :height="240"
                  :hints="sqlEditorHints"
                  :keys="sqlEditorKeys"
                  :placeholder="field.placeholder"
                  resizable
                />
              </div>
            </template>
            <el-input
              v-else-if="field.component === 'resource'"
              v-model="options[field.key]"
              :placeholder="field.placeholder"
            >
              <template #append>
                <el-button @click="openTableBrowser(false, field)">
                  获取{{ field.resourceLabel || '表名' }}
                </el-button>
              </template>
            </el-input>
            <FieldMapEditor
              v-else-if="field.component === 'mapping'"
              v-model="options[field.key]"
              :sources="mappingSources"
              :targets="mappingTargets"
              :loading="mappingLoading"
              :message="mappingMessage"
              @refresh="loadMappingSchema"
            />
            <el-input v-else v-model="options[field.key]" :placeholder="field.placeholder" />
            <div v-if="field.component === 'sql' && model.data.type === 'SQLTransform'" class="sql-table-tip">
              <TipText>
                SQL 节点的直接上游数据会以对应上游节点的<em>名称</em>注册为临时表，
                不使用节点类型或 ID 作为表名。
              </TipText>
              <div v-if="sqlInputTables.length" class="sql-table-tip__names">
                <span>当前可用表名</span>
                <el-tag
                  v-for="table in sqlInputTables"
                  :key="table.id"
                  type="info"
                  effect="plain"
                  size="small"
                >{{ table.name || '（名称为空）' }}</el-tag>
              </div>
              <TipText v-else>
                当前没有直接上游数据；连线上游节点后，这里会显示可用的表名。
              </TipText>
              <TipText v-if="sqlInputTables.length">
                引用示例：<code>SELECT * FROM `{{ sqlInputTables[0].name }}`</code>，
                名称含空格或特殊字符时必须使用反引号。
              </TipText>
              <TipText v-if="duplicateSqlTables.length">
                <em>表名重复：</em>{{ duplicateSqlTables.join('、') }}。
                同名临时表会互相覆盖，请先修改上游节点名称。
              </TipText>
            </div>
          </el-form-item>
      </property-section>

      <property-section v-else-if="!nodeGuide" title="参数配置" :reset-key="model.id">
          <el-form-item label="参数JSON">
            <el-input
              type="textarea"
              :rows="8"
              :model-value="render(model.data.options)"
              @update:model-value="setRoot"
            />
          </el-form-item>
      </property-section>

      <property-section
        v-if="nodeGuide || fieldHelps.length"
        title="配置说明"
        :reset-key="model.id"
        :open="model.data.type !== 'ConvertTransform' || !(model.data.options?.items?.length)"
      >
        <div class="node-guide">
          <template v-if="nodeGuide">
            <p>{{ nodeGuide.summary }}</p>
            <ul v-if="nodeGuide.points?.length">
              <li v-for="point in nodeGuide.points" :key="point">{{ point }}</li>
            </ul>
            <code v-if="nodeGuide.example">{{ nodeGuide.example }}</code>
          </template>
          <div v-if="fieldHelps.length" class="node-guide__fields">
            <strong>字段说明</strong>
            <dl>
              <template v-for="field in fieldHelps" :key="field.key">
                <dt>{{ field.label }}</dt>
                <dd>{{ field.help }}</dd>
              </template>
            </dl>
          </div>
        </div>
      </property-section>
    </el-form>
  </property-panel>
  <TableBrowser
    v-model="tableBrowserVisible"
    :datasource-id="options.datasourceId"
    :database="tableBrowserDatabase"
    :selected-table="tableBrowserSelected"
    :sql-mode="tableBrowserSqlMode"
    @select="applyTableSelection"
  />
</template>

<style lang="scss" scoped>
.sql-editor-field {
  width: 100%;

  &__toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
    margin-bottom: 5px;

    strong {
      color: var(--el-text-color-regular);
      font-size: var(--el-font-size-base);
      font-weight: 400;
      line-height: 1.4;
    }

    > div {
      display: flex;
      flex: none;
      align-items: center;
      gap: 8px;
    }
  }
}

.sql-table-tip {
  width: 100%;
  margin-top: 8px;
  padding: 8px 10px;
  box-sizing: border-box;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: var(--el-fill-color-extra-light);

  &__names {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 5px;
    margin: 6px 0 3px;

    > span {
      color: var(--el-text-color-secondary);
      font-size: 12px;
    }
  }

  code {
    padding: 1px 4px;
    border-radius: 3px;
    color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
    font-family: var(--el-font-family-mono, monospace);
  }
}

.node-guide {
  width: 100%;
  padding: 9px 10px;
  box-sizing: border-box;
  border: 1px solid var(--el-color-primary-light-7);
  border-radius: 6px;
  background: var(--el-color-primary-light-9);

  p {
    margin: 0;
    color: var(--el-text-color-regular);
    font-size: 12px;
    line-height: 1.65;
  }

  ul {
    margin: 6px 0 0;
    padding-left: 16px;
    color: var(--el-text-color-secondary);
    font-size: 11px;
    line-height: 1.65;
  }

  code {
    display: block;
    margin-top: 6px;
    padding: 5px 7px;
    border-radius: 4px;
    color: var(--el-color-primary-dark-2);
    background: var(--el-bg-color);
    font-family: var(--el-font-family-mono, monospace);
    font-size: 11px;
    line-height: 1.55;
    white-space: normal;
  }

  &__fields {
    margin-top: 10px;
    padding-top: 8px;
    border-top: 1px solid var(--el-color-primary-light-7);

    strong {
      color: var(--el-text-color-regular);
      font-size: 11px;
    }

    dl {
      display: grid;
      grid-template-columns: 78px minmax(0, 1fr);
      gap: 4px 8px;
      margin: 6px 0 0;
      font-size: 11px;
      line-height: 1.55;
    }

    dt {
      color: var(--el-text-color-regular);
      font-weight: 500;
    }

    dd {
      margin: 0;
      color: var(--el-text-color-secondary);
    }
  }
}
</style>
