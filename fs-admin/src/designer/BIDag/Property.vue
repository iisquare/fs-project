<script setup lang="ts">
import { computed } from 'vue'
import LayoutIcon from '@/components/Layout/LayoutIcon.vue'
import DatasourceApi from '@/api/bi/DatasourceApi'
import OntologyApi from '@/api/kg/OntologyApi'
import PropertyPanel from './PropertyPanel.vue'
import PropertySection from '@/components/Layout/PropertySection.vue'
import TipText from '@/components/Layout/TipText.vue'

const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
const props = defineProps<{
  config: any,
  instance: any,
}>()

/** 依据节点类型取参数编辑字段 */
const fields = computed(() => {
  const type = model.value?.data?.type
  return (props.config?.fields ?? {})[type] ?? []
})

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

const updateDatasource = (field: any, value: any) => {
  options.value[field.key] = value
  for (const key of field.clearOptions ?? []) delete options.value[key]
}

const setRoot = (text: string) => {
  try {
    const parsed = text.trim() ? JSON.parse(text) : {}
    model.value.data.options = (parsed && 'object' === typeof parsed && !Array.isArray(parsed)) ? parsed : {}
  } catch (e) {
    // 非法JSON暂不写回，保留上次有效配置
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
          <el-form-item :label="field.label" v-for="field in fields" :key="field.key">
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
              <el-option v-for="item in field.options" :key="item" :value="item" :label="item" />
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
            <el-input
              v-else-if="field.component === 'sql'"
              type="textarea"
              :rows="8"
              v-model="options[field.key]"
              :placeholder="field.placeholder"
            />
            <el-input v-else v-model="options[field.key]" :placeholder="field.placeholder" />
            <div v-if="field.component === 'sql'" class="sql-table-tip">
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

      <property-section v-else title="参数配置" :reset-key="model.id">
          <el-form-item label="参数JSON">
            <el-input
              type="textarea"
              :rows="8"
              :model-value="render(model.data.options)"
              @update:model-value="setRoot"
            />
          </el-form-item>
      </property-section>
    </el-form>
  </property-panel>
</template>

<style lang="scss" scoped>
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
</style>
