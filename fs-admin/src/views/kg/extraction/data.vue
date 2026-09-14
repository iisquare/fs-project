<script setup lang="ts">
/**
 * 数据管理 - 基于本体定义管理图数据库中的实体与关系数据
 *
 * 交互约定：
 * 1. 先选本体，再选左侧实体或关系类型，右侧表格按本体字段动态生成列；
 * 2. 表格列可由"显示字段"按需勾选，值按类型渲染（列表显示为标签）；
 * 3. 新增编辑抽屉按字段类型生成控件，主键在编辑时只读，必填字段先校验；
 * 4. 支持关键字检索、按字段高级筛选、批量导入JSON、变更记录查看；
 * 5. 每行可一键进入图探索，以该数据为起点查看关系网络。
 */
import { computed, onMounted, ref } from 'vue'
import type { FormInstance } from 'element-plus'
import { ElMessage } from 'element-plus'
import * as ElementPlusIcons from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import ApiUtil from '@/utils/ApiUtil'
import TableUtil from '@/utils/TableUtil'
import RouteUtil from '@/utils/RouteUtil'
import DateUtil from '@/utils/DateUtil'
import GraphApi from '@/api/kg/GraphApi'
import OntologyApi from '@/api/kg/OntologyApi'
import LayoutHeading from '@/components/Layout/LayoutHeading.vue'
import LayoutHelp from '@/components/Layout/LayoutHelp.vue'

const route = useRoute()
const router = useRouter()

const ontologies = ref<any[]>([])
const ontologyId = ref<any>(null)
const summary = ref<any>({ entities: [], relationships: [] })
const model = ref<any>({ entities: [], relationships: [] })
const kind = ref('ENTITY')
const current = ref<any>(null)
const pending = ref<any>(null) // 从图探索跳转过来时待定位的数据

const loading = ref(false)
const rows = ref<any[]>([])
const selection = ref<any[]>([])
const keyword = ref('')
const labelFilter = ref('')
const filters = ref<any[]>([])
const advancedVisible = ref(false)
const detach = ref(true)
const visibleFields = ref<string[]>([])
const searchable = ref(true)
const pagination = ref<any>(RouteUtil.pagination({}))
const sortField = ref('')
const sortOrder = ref('asc')

const operators = [
  { value: 'eq', label: '等于' }, { value: 'ne', label: '不等于' },
  { value: 'contains', label: '包含' }, { value: 'startsWith', label: '开头是' },
  { value: 'ge', label: '大于等于' }, { value: 'le', label: '小于等于' },
  { value: 'in', label: '在列表中（逗号分割）' }, { value: 'isNull', label: '为空' }, { value: 'isNotNull', label: '不为空' },
]

const entityFields = computed(() => current.value?.fields ?? [])
// 左侧列表的标签说明：展示主标签与附加标签，与显示名称一致的主标签不重复展示
const labelLine = (item: any) => {
  const labels: string[] = Array.isArray(item.labels) && item.labels.length ? item.labels : (item.label ? [item.label] : [])
  const values: string[] = []
  labels.forEach((label: string, index: number) => {
    if (!label || values.includes(label)) return
    if (0 === index && label === item.name) return
    values.push(label)
  })
  return values
}
const displayFields = computed(() => {
  const fields = entityFields.value
  if (!visibleFields.value.length) return fields
  return fields.filter((field: any) => visibleFields.value.includes(field.name))
})
const primaryField = computed(() => current.value?.primaryField ?? '')
const sourceEntityDef = computed(() => model.value.entities?.find((item: any) => item.label === current.value?.sourceLabel))
const targetEntityDef = computed(() => model.value.entities?.find((item: any) => item.label === current.value?.targetLabel))

const entityDef = (label: string) => model.value.entities?.find((item: any) => item.label === label)
const nodeCaption = (node: any) => {
  if (!node) return ''
  const def = entityDef(node.labels?.[0])
  const properties = node.properties ?? {}
  const keys = [def?.captionField, def?.primaryField].filter(Boolean)
  for (const key of keys) {
    if (properties[key] != null && properties[key] !== '') return String(properties[key])
  }
  const first = Object.values(properties).find(value => value != null && value !== '')
  return first == null ? node.elementId : String(first)
}
const renderValue = (value: any) => {
  if (value == null || value === '') return '-'
  if (Array.isArray(value)) return value.join(', ')
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}
const actionText = (action: string) => ({
  SAVE: '保存', REMOVE: '删除',
}[action] ?? action)
const widgetOf = (field: any) => {
  const type = String(field?.type ?? '').toLowerCase()
  if (type.includes('bool')) return 'switch'
  if (type.includes('int') || type.includes('float') || type.includes('double') || type.includes('number')) return 'number'
  if (type.includes('date') || type.includes('time')) return 'date'
  if (type.includes('list') || type.includes('array')) return 'list'
  return 'text'
}
const fieldTip = (field: any) => {
  const parts = [`类型：${field.type || 'String'}`]
  if (field.required) parts.push('必填')
  if (field.comment) parts.push(field.comment)
  return parts.join(' ｜ ')
}

const handleOntologies = () => {
  OntologyApi.list({ page: 1, pageSize: 200 }).then((result: any) => {
    ontologies.value = ApiUtil.data(result)?.rows ?? []
  }).catch(() => {})
}

const loadDefinition = () => {
  if (!ontologyId.value) return
  GraphApi.summary({ ontologyId: ontologyId.value }, { warning: false }).then((result: any) => {
    summary.value = ApiUtil.data(result) ?? { entities: [], relationships: [] }
    if (pending.value) {
      const wait = pending.value
      pending.value = null
      const item = (summary.value.entities ?? []).find((entity: any) => entity.label === wait.entity || entity.code === wait.entity)
      if (item) {
        selectItem('ENTITY', item)
        if (wait.id != null && wait.id !== '') {
          GraphApi.info({ ontologyId: ontologyId.value, entity: item.code || item.label, id: wait.id }, { warning: false })
            .then((response: any) => {
              const node = ApiUtil.data(response)
              if (!node) return
              form.value = { properties: Object.assign({}, node.properties), source: '', target: '', editing: true }
              formVisible.value = true
            }).catch(() => {})
        }
      }
    }
  }).catch(() => {})
  OntologyApi.model({ id: ontologyId.value }, { warning: false }).then((result: any) => {
    model.value = ApiUtil.data(result) ?? { entities: [], relationships: [] }
  }).catch(() => {})
}

const handleOntologyChange = () => {
  current.value = null
  rows.value = []
  selection.value = []
  loadDefinition()
}

const selectItem = (itemKind: string, item: any) => {
  kind.value = itemKind
  current.value = item
  visibleFields.value = (item.fields ?? []).map((field: any) => field.name)
  keyword.value = ''
  labelFilter.value = ''
  filters.value = []
  refresh(true)
}

const refresh = (reset = false) => {
  if (!ontologyId.value || !current.value) return
  if (reset) pagination.value.currentPage = 1
  const param: any = {
    ontologyId: ontologyId.value,
    keyword: keyword.value || undefined,
    label: kind.value === 'ENTITY' ? (labelFilter.value || undefined) : undefined,
    filters: filters.value.filter(item => item.field).map(item => ({
      field: item.field, operator: item.operator, value: item.value,
    })),
    page: pagination.value.currentPage,
    pageSize: pagination.value.pageSize,
    sortField: sortField.value || undefined,
    sortOrder: sortOrder.value,
  }
  loading.value = true
  const request = kind.value === 'ENTITY'
    ? GraphApi.search(Object.assign(param, { entity: current.value.code || current.value.label }), { warning: false })
    : GraphApi.relationshipSearch(Object.assign(param, { relationship: current.value.code || current.value.label }), { warning: false })
  request.then((result: any) => {
    const data = ApiUtil.data(result) ?? {}
    rows.value = data.rows ?? []
    largeResult.value = (data.total ?? 0) > 10000
    RouteUtil.result2pagination(pagination.value, result)
  }).catch((result: any) => {
    ElMessage.warning(ApiUtil.message(result))
  }).finally(() => {
    loading.value = false
  })
}

/* ---------------- 新增与编辑 ---------------- */

const formVisible = ref(false)
const formLoading = ref(false)
const formRef = ref<FormInstance>()
const form = ref<any>({ properties: {}, source: '', target: '', editing: false })
const sourceOptions = ref<any[]>([])
const targetOptions = ref<any[]>([])

const handleAdd = () => {
  const properties: any = {}
  entityFields.value.forEach((field: any) => {
    if (widgetOf(field) === 'switch') properties[field.name] = false
  })
  form.value = { properties, source: '', target: '', editing: false }
  form.value.labels = []
  sourceOptions.value = []
  targetOptions.value = []
  formVisible.value = true
  if (kind.value === 'RELATIONSHIP') {
    loadEndpointOptions('source', '')
    loadEndpointOptions('target', '')
  }
}

const handleEdit = (row: any) => {
  if (kind.value === 'ENTITY') {
    form.value = {
      properties: Object.assign({}, row.properties),
      labels: (row.labels ?? []).filter((label: string) => !(current.value.labels ?? []).includes(label)),
      source: '', target: '', editing: true,
    }
  } else {
    form.value = {
      properties: Object.assign({}, row.relationship?.properties ?? {}),
      source: row.source?.properties?.[sourceEntityDef.value?.primaryField],
      target: row.target?.properties?.[targetEntityDef.value?.primaryField],
      editing: true,
    }
  }
  formVisible.value = true
}

const loadEndpointOptions = (which: string, query: string) => {
  const def = which === 'source' ? sourceEntityDef.value : targetEntityDef.value
  if (!def || !ontologyId.value) return
  GraphApi.search({ ontologyId: ontologyId.value, entity: def.label, keyword: query || undefined, page: 1, pageSize: 20 }, { warning: false })
    .then((result: any) => {
      const options = (ApiUtil.data(result)?.rows ?? []).map((node: any) => ({
        value: node.properties?.[def.primaryField],
        label: nodeCaption(node),
      }))
      if (which === 'source') sourceOptions.value = options
      else targetOptions.value = options
    }).catch(() => {})
}

const handleSubmit = () => {
  const fields = current.value?.fields ?? []
  const missing = fields
    .filter((field: any) => (field.required || (kind.value === 'ENTITY' && field.name === primaryField.value)))
    .filter((field: any) => {
      const value = form.value.properties?.[field.name]
      return value == null || value === '' || (Array.isArray(value) && value.length === 0)
    })
    .map((field: any) => field.title || field.name)
  if (missing.length > 0) return ElMessage.warning(`请填写：${missing.join('、')}`)
  if (kind.value === 'RELATIONSHIP' && (form.value.source === '' || form.value.target === '')) {
    return ElMessage.warning('请选择关系的起点与终点数据')
  }
  formRef.value?.validate((valid: boolean) => {
    if (!valid || formLoading.value) return
    formLoading.value = true
    const request = kind.value === 'ENTITY'
      ? GraphApi.save({
        ontologyId: ontologyId.value,
        entity: current.value.code || current.value.label,
        properties: form.value.properties,
        labels: form.value.labels ?? [],
      }, { success: true })
      : GraphApi.relationshipSave({
        ontologyId: ontologyId.value,
        relationship: current.value.code || current.value.label,
        source: form.value.source,
        target: form.value.target,
        properties: form.value.properties,
      }, { success: true })
    request.then(() => {
      formVisible.value = false
      refresh()
    }).catch((result: any) => {
      ElMessage.warning(ApiUtil.message(result))
    }).finally(() => {
      formLoading.value = false
    })
  })
}

/* ---------------- 删除 ---------------- */

const deleteVisible = ref(false)
const deleteTargets = ref<any[]>([])
const deleteDetach = ref(true)

const handleDelete = (targets: any[]) => {
  if (!targets.length) return
  deleteTargets.value = targets
  deleteDetach.value = detach.value
  deleteVisible.value = true
}

const confirmDelete = () => {
  const targets = deleteTargets.value
  if (!targets.length) return
  loading.value = true
  const request = kind.value === 'ENTITY'
    ? GraphApi.remove({
      ontologyId: ontologyId.value,
      entity: current.value.code || current.value.label,
      ids: targets.map((row: any) => row.properties?.[primaryField.value]),
      detach: deleteDetach.value,
    }, { success: true })
    : GraphApi.relationshipRemove({
      ids: targets.map((row: any) => row.relationship?.elementId),
    }, { success: true })
  request.then(() => {
    deleteVisible.value = false
    refresh(true)
  }).catch((result: any) => {
    ElMessage.warning(ApiUtil.message(result))
  }).finally(() => {
    loading.value = false
  })
}

/* ---------------- 统计与巡检 ---------------- */

const aggregateVisible = ref(false)
const aggregateLoading = ref(false)
const aggregateField = ref('')
const aggregateRows = ref<any[]>([])

const handleAggregate = () => {
  if (!current.value) return ElMessage.warning('请先选择实体或关系类型')
  if (!aggregateField.value) return ElMessage.warning('请选择统计字段')
  aggregateLoading.value = true
  GraphApi.aggregate(Object.assign(exportParam(), { field: aggregateField.value, limit: 50 }), { warning: false })
    .then((result: any) => {
      aggregateRows.value = ApiUtil.data(result) ?? []
    }).catch((result: any) => {
      ElMessage.warning(ApiUtil.message(result))
    }).finally(() => {
      aggregateLoading.value = false
    })
}

const inspectVisible = ref(false)
const inspectLoading = ref(false)
const inspectRows = ref<any[]>([])

const handleInspect = () => {
  inspectVisible.value = true
  inspectLoading.value = true
  GraphApi.inspect({ ontologyId: ontologyId.value }, { warning: false }).then((result: any) => {
    inspectRows.value = ApiUtil.data(result) ?? []
  }).catch((result: any) => ElMessage.warning(ApiUtil.message(result))).finally(() => {
    inspectLoading.value = false
  })
}

/* ---------------- 检索方案 ---------------- */

const queryList = ref<any[]>([])
const queryVisible = ref(false)
const queryName = ref('')
const queryLoading = ref(false)
const largeResult = ref(false)

const loadQueries = () => {
  if (!current.value || !ontologyId.value) return
  GraphApi.queryList({
    ontologyId: ontologyId.value,
    kind: kind.value,
    label: current.value.code || current.value.label,
  }, { warning: false }).then((result: any) => {
    queryList.value = ApiUtil.data(result) ?? []
  }).catch(() => {})
}

const applyQuery = (item: any) => {
  let params: any = {}
  try {
    params = JSON.parse(item.params || '{}')
  } catch (e) {
    params = {}
  }
  keyword.value = params.keyword ?? ''
  labelFilter.value = params.label ?? ''
  filters.value = params.filters ?? []
  advancedVisible.value = (filters.value ?? []).length > 0
  refresh(true)
}

const saveQuery = () => {
  if (!queryName.value) return ElMessage.warning('请填写方案名称')
  queryLoading.value = true
  GraphApi.querySave({
    ontologyId: ontologyId.value,
    kind: kind.value,
    label: current.value.code || current.value.label,
    name: queryName.value,
    params: { keyword: keyword.value, label: labelFilter.value, filters: filters.value },
  }, { success: true }).then(() => {
    queryVisible.value = false
    queryName.value = ''
    loadQueries()
  }).catch((result: any) => {
    ElMessage.warning(ApiUtil.message(result))
  }).finally(() => {
    queryLoading.value = false
  })
}

const deleteQuery = (item: any) => {
  GraphApi.queryDelete({ ids: [item.id] }, { success: true }).then(() => loadQueries()).catch(() => {})
}

/* ---------------- 批量导入 ---------------- */

const importVisible = ref(false)
const importLoading = ref(false)
const importText = ref('')
const importErrors = ref<any[]>([])

/**
 * 按字段类型给出示例值
 */
const sampleValue = (field: any) => {
  const widget = widgetOf(field)
  if (widget === 'switch') return false
  if (widget === 'number') return 0
  if (widget === 'date') return DateUtil.format(Date.now(), 'yyyy-MM-dd HH:mm:ss')
  if (widget === 'list') return []
  return ''
}

const importTemplate = () => {
  const fields = kind.value === 'ENTITY' ? entityFields.value : (current.value?.fields ?? [])
  const properties: any = {}
  fields.forEach((field: any) => {
    properties[field.name] = field.name === primaryField.value ? 'ID001' : sampleValue(field)
  })
  if (kind.value === 'ENTITY') {
    if (current.value?.extendableLabels) properties.labels = []
    importText.value = JSON.stringify([properties], null, 2)
  } else {
    importText.value = JSON.stringify([{ source: 'ID001', target: 'ID002', properties }], null, 2)
  }
}

const openImport = () => {
  importErrors.value = []
  importVisible.value = true
  importTemplate()
}

const handleImport = () => {
  let items: any
  try {
    items = JSON.parse(importText.value || '[]')
  } catch (e) {
    return ElMessage.error('导入内容不是合法的JSON')
  }
  if (!Array.isArray(items) || items.length === 0) return ElMessage.warning('请填写至少一条数据')
  importLoading.value = true
  GraphApi.batch({
    ontologyId: ontologyId.value,
    entity: kind.value === 'ENTITY' ? (current.value.code || current.value.label) : undefined,
    relationship: kind.value === 'RELATIONSHIP' ? (current.value.code || current.value.label) : undefined,
    items,
  }).then((result: any) => {
    const results = ApiUtil.data(result) ?? []
    const failed = results.filter((item: any) => item.code !== 0)
    importErrors.value = failed
    if (failed.length > 0) {
      ElMessage.warning(`导入完成：成功 ${results.length - failed.length} 条，失败 ${failed.length} 条，详见失败明细`)
    } else {
      ElMessage.success(`导入完成，共 ${results.length} 条`)
      importVisible.value = false
    }
    refresh(true)
  }).catch((result: any) => {
    ElMessage.warning(ApiUtil.message(result))
  }).finally(() => {
    importLoading.value = false
  })
}

/**
 * 解析CSV文本，支持引号包裹与转义
 */
const parseCsv = (text: string) => {
  const rows: string[][] = []
  let row: string[] = []
  let cell = ''
  let quoted = false
  for (let index = 0; index < text.length; index++) {
    const char = text[index]
    if (quoted) {
      if (char === '"') {
        if (text[index + 1] === '"') { cell += '"'; index++ }
        else quoted = false
      } else cell += char
      continue
    }
    if (char === '"') quoted = true
    else if (char === ',') { row.push(cell); cell = '' }
    else if (char === '\n') { row.push(cell); rows.push(row); row = []; cell = '' }
    else if (char !== '\r') cell += char
  }
  if (cell !== '' || row.length > 0) { row.push(cell); rows.push(row) }
  return rows
}

const handleCsvUpload = (file: any) => {
  const reader = new FileReader()
  reader.onload = () => {
    const rows = parseCsv(String(reader.result ?? ''))
    if (rows.length < 2) return ElMessage.warning('CSV至少需要表头与一行数据')
    applyRows(rows[0].map(item => item.trim()), rows.slice(1))
  }
  reader.readAsText(file.raw, 'utf-8')
}

/**
 * 单元格按字段类型转换：列表拆分多个值，布尔兼容中文写法
 */
const cellValue = (field: any, value: string) => {
  const widget = field ? widgetOf(field) : 'text'
  if (widget === 'list') {
    return value.split(/[,，;；、|]/).map(item => item.trim()).filter(item => item !== '')
  }
  if (widget === 'switch') {
    const text = value.toLowerCase()
    if (['true', '1', 'y', 'yes', '是', '有', '启用'].includes(text)) return true
    if (['false', '0', 'n', 'no', '否', '无', '禁用'].includes(text)) return false
  }
  return value
}

/**
 * 将表格行按本体字段映射为待导入JSON，表头支持字段名与显示名，空单元格表示不修改
 */
const applyRows = (headers: string[], body: string[][]) => {
  const rows = body.filter(row => row.some(cell => String(cell ?? '').trim() !== ''))
  const fields = kind.value === 'ENTITY' ? entityFields.value : (current.value?.fields ?? [])
  const match = (header: string) => fields.find((item: any) => item.name === header || (item.title || item.name) === header)
  if (kind.value === 'RELATIONSHIP') {
    const items = rows.map(row => {
      const record: any = { source: '', target: '', properties: {} }
      headers.forEach((header, index) => {
        const value = String(row[index] ?? '').trim()
        if (['source', '起点', '起点主键'].includes(header)) {
          record.source = value
          return
        }
        if (['target', '终点', '终点主键'].includes(header)) {
          record.target = value
          return
        }
        if (value === '') return
        const field = match(header)
        record.properties[field ? field.name : header] = cellValue(field, value)
      })
      return record
    })
    importText.value = JSON.stringify(items, null, 2)
    ElMessage.success(`已解析 ${items.length} 行，请确认后点击导入`)
    return
  }
  const items = rows.map(row => {
    const record: any = {}
    headers.forEach((header, index) => {
      const value = String(row[index] ?? '').trim()
      if (value === '') return
      const field = match(header)
      const name = field ? field.name : header
      record[name] = 'labels' === name ? cellValue({ type: 'List' }, value) : cellValue(field, value)
    })
    return record
  })
  importText.value = JSON.stringify(items, null, 2)
  ElMessage.success(`已解析 ${items.length} 行，请确认后点击导入`)
}

const handleExcelUpload = (file: any) => {
  importLoading.value = true
  GraphApi.importExcel({ file: file.raw }, { warning: false }).then((result: any) => {
    const rows: string[][] = ApiUtil.data(result) ?? []
    if (rows.length < 2) return ElMessage.warning('Excel至少需要表头与一行数据')
    applyRows(rows[0].map((item: string) => String(item ?? '').trim()), rows.slice(1))
  }).catch((result: any) => {
    ElMessage.warning(ApiUtil.message(result))
  }).finally(() => {
    importLoading.value = false
  })
}

/* ---------------- 变更记录 ---------------- */

const logVisible = ref(false)
const logLoading = ref(false)
const logRows = ref<any[]>([])
const logRange = ref<any>([])

const handleLog = () => {
  logVisible.value = true
  logLoading.value = true
  GraphApi.log({
    ontologyId: ontologyId.value,
    label: current.value?.label,
    kind: kind.value,
    beginTime: logRange.value?.[0] ? new Date(logRange.value[0]).getTime() : undefined,
    endTime: logRange.value?.[1] ? new Date(logRange.value[1]).getTime() : undefined,
    page: 1,
    pageSize: 50,
  }, { warning: false }).then((result: any) => {
    logRows.value = ApiUtil.data(result)?.rows ?? []
  }).catch(() => {}).finally(() => {
    logLoading.value = false
  })
}

const resetFilters = () => {
  keyword.value = ''
  labelFilter.value = ''
  filters.value = []
  refresh(true)
}

const handleDataCommand = (command: string) => {
  if (command === 'import') return openImport()
  handleExportCommand(command)
}

const handleMoreCommand = (command: string) => {
  if (command === 'log') return handleLog()
  if (command === 'inspect') return handleInspect()
  if (command === 'aggregate') {
    aggregateVisible.value = true
    aggregateField.value = ''
    aggregateRows.value = []
  }
}

const handleLogClean = () => {
  const before = logRange.value?.[0] ? new Date(logRange.value[0]).getTime() : 0
  if (!before) return ElMessage.warning('请先选择清理的起始时间')
  TableUtil.confirm(`将删除 ${DateUtil.format(before)} 之前的变更记录，确认继续？`, '清理变更记录').then(() => {
    GraphApi.logClean({ beforeTime: before }, { success: true }).then(() => handleLog()).catch(() => {})
  }).catch(() => {})
}

const handleExplore = (row: any) => {
  router.push({
        path: '/kg/retrieval/traverse',
        query: {
          ontologyId: String(ontologyId.value),
          // 图探索页按主标签匹配实体，这里传标签，避免带编码时匹配不上
          entity: current.value.label || current.value.code,
          id: String(row.properties?.[primaryField.value] ?? ''),
        },
  })
}

const detailVisible = ref(false)
const detail = ref<any>(null)
const handleDetail = (row: any) => {
  detail.value = row
  detailVisible.value = true
}

const handleSortChange = ({ prop, order }: any) => {
  if (!order) {
    sortField.value = ''
    sortOrder.value = 'asc'
  } else {
    sortField.value = String(prop ?? '').replace('relationship.properties.', '').replace('properties.', '')
    sortOrder.value = order === 'descending' ? 'desc' : 'asc'
  }
  refresh(true)
}

/* ---------------- 导出 ---------------- */

const exportParam = () => ({
  ontologyId: ontologyId.value,
  entity: kind.value === 'ENTITY' ? (current.value.code || current.value.label) : undefined,
  relationship: kind.value === 'RELATIONSHIP' ? (current.value.code || current.value.label) : undefined,
  keyword: keyword.value || undefined,
  label: kind.value === 'ENTITY' ? (labelFilter.value || undefined) : undefined,
  filters: filters.value.filter(item => item.field).map(item => ({ field: item.field, operator: item.operator, value: item.value })),
})

const downloadText = (text: string, filename: string, mime: string) => {
  downloadBlob(new Blob([text], { type: mime }), filename)
}

const downloadBlob = (blob: Blob, filename: string) => {
  const link = document.createElement('a')
  link.href = window.URL.createObjectURL(blob)
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(link.href)
}

const toCsv = (fields: any[], rows: any[]) => {
  const escape = (value: any) => {
    if (value == null) return ''
    const text = Array.isArray(value) ? value.join(',') : String(value)
    return /[",\n\r]/.test(text) ? '"' + text.replace(/"/g, '""') + '"' : text
  }
  const lines = [fields.map(field => escape(field.title || field.name)).join(',')]
  rows.forEach(row => lines.push(fields.map(field => escape(row[field.name])).join(',')))
  return '\ufeff' + lines.join('\r\n')
}

/**
 * 将后端返回的Base64内容保存为文件（Excel等二进制格式）
 */
const downloadBase64 = (content: string, filename: string, mime: string) => {
  const binary = window.atob(content)
  const bytes = new Uint8Array(binary.length)
  for (let index = 0; index < binary.length; index++) bytes[index] = binary.charCodeAt(index)
  downloadBlob(new Blob([bytes], { type: mime }), filename)
}

const EXCEL_MIME = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'

/**
 * 下载导入模板：实体、关系或本体内全部结构（多工作表Excel）
 */
const handleTemplateCommand = (command: string) => {
  if (!current.value) return ElMessage.warning('请先选择实体或关系类型')
  const name = current.value.name || current.value.label
  const param: any = { ontologyId: ontologyId.value }
  if (command === 'all') {
    param.scope = 'all'
  } else {
    param.format = command
    if (kind.value === 'ENTITY') param.entity = current.value.code || current.value.label
    else param.relationship = current.value.code || current.value.label
  }
  GraphApi.importTemplate(param, { warning: false }).then((result: any) => {
    const data = ApiUtil.data(result) ?? {}
    if (!data.content) return ElMessage.info('没有可下载的模板')
    if (data.format === 'csv') {
      downloadText('\ufeff' + data.content, data.filename || `${name}_导入模板.csv`, 'text/csv;charset=utf-8')
    } else {
      downloadBase64(data.content, data.filename || `${name}_导入模板.xlsx`, EXCEL_MIME)
    }
    ElMessage.success('模板已生成，按表头填写后再上传')
  }).catch((result: any) => {
    ElMessage.warning(ApiUtil.message(result))
  })
}

const handleExport = (format: string) => {
  if (!current.value) return ElMessage.warning('请先选择实体或关系类型')
  loading.value = true
  GraphApi.exportData(Object.assign(exportParam(), { limit: 20000 }), { warning: false }).then((result: any) => {
    const data = ApiUtil.data(result) ?? {}
    const rows = data.rows ?? []
    if (rows.length === 0) return ElMessage.info('没有可导出的数据')
    const name = current.value.name || current.value.label
    const stamp = DateUtil.format(Date.now(), 'yyyyMMddHHmmss')
    if (format === 'json') {
      downloadText(JSON.stringify(rows, null, 2), `${name}_${stamp}.json`, 'application/json;charset=utf-8')
    } else {
      downloadText(toCsv(data.fields ?? [], rows), `${name}_${stamp}.csv`, 'text/csv;charset=utf-8')
    }
    ElMessage.success(`已导出 ${rows.length} 条数据`)
  }).catch((result: any) => {
    ElMessage.warning(ApiUtil.message(result))
  }).finally(() => {
    loading.value = false
  })
}

const handleExportExcel = () => {
  if (!current.value) return ElMessage.warning('请先选择实体或关系类型')
  loading.value = true
  GraphApi.exportExcel(Object.assign(exportParam(), { limit: 20000 }), { warning: false }).then((result: any) => {
    const data = ApiUtil.data(result) ?? {}
    if (!data.content) return ElMessage.info('没有可导出的数据')
    const name = current.value.name || current.value.label
    const stamp = DateUtil.format(Date.now(), 'yyyyMMddHHmmss')
    downloadBase64(data.content, `${name}_${stamp}.xlsx`, EXCEL_MIME)
    ElMessage.success(`已导出 ${data.total ?? 0} 条数据`)
  }).catch((result: any) => {
    ElMessage.warning(ApiUtil.message(result))
  }).finally(() => {
    loading.value = false
  })
}

const handleExportCommand = (command: string) => {
  if (command === 'excel') return handleExportExcel()
  handleExport(command)
}

onMounted(() => {
  const query = route.query as any
  // 路由参数是字符串，需转成与下拉选项一致的数值类型，否则会直接显示本体 id
  if (query.ontologyId) {
    ontologyId.value = /^\d+$/.test(String(query.ontologyId)) ? Number(query.ontologyId) : query.ontologyId
  }
  handleOntologies()
  if (ontologyId.value) {
    pending.value = query.entity ? { entity: query.entity, id: query.id } : null
    loadDefinition()
  }
})
</script>

<template>
  <LayoutHeading title="图谱数据" description="按本体定义管理、检索与导入导出图数据库中的数据">
    <template #extra>
      <el-space wrap>
        <span class="dm-label">本体</span>
        <el-select v-model="ontologyId" clearable filterable placeholder="请选择本体" style="width: 220px" @change="handleOntologyChange">
          <el-option v-for="item in ontologies" :key="item.id" :value="item.id" :label="item.name" />
        </el-select>
        <el-tag v-if="current" effect="plain">{{ kind === 'ENTITY' ? '实体' : '关系' }}：{{ current.name || current.label }}</el-tag>
      </el-space>
    </template>
  </LayoutHeading>

  <el-card v-if="ontologyId" :bordered="false" shadow="never" class="fs-table-card dm-body">
    <el-splitter>
      <el-splitter-panel size="240px" min="180px" max="380px">
        <el-scrollbar class="dm-aside">
          <div class="dm-aside__title">实体（{{ (summary.entities ?? []).length }}）</div>
      <div
        v-for="item in (summary.entities ?? [])"
        :key="'e-' + item.label"
        class="dm-item"
        :class="{ 'dm-item--active': kind === 'ENTITY' && current?.label === item.label }"
        @click="selectItem('ENTITY', item)"
      >
        <span class="dm-item__name">
          <span class="dm-item__title">{{ item.name || item.label }}</span>
          <span v-if="labelLine(item).length" class="dm-item__labels">
            <span>{{ labelLine(item)[0] }}</span>
            <em v-for="label in labelLine(item).slice(1)" :key="label" class="dm-item__extra">{{ label }}</em>
          </span>
        </span>
        <el-tag size="small" effect="plain">{{ item.count ?? 0 }}</el-tag>
      </div>
      <div class="dm-aside__title">关系（{{ (summary.relationships ?? []).length }}）</div>
      <div
        v-for="item in (summary.relationships ?? [])"
        :key="'r-' + item.label"
        class="dm-item"
        :class="{ 'dm-item--active': kind === 'RELATIONSHIP' && current?.label === item.label }"
        @click="selectItem('RELATIONSHIP', item)"
      >
        <span class="dm-item__name">
          <span class="dm-item__title">{{ item.name || item.label }}</span>
          <span v-if="labelLine(item).length" class="dm-item__labels">
            <span>{{ labelLine(item)[0] }}</span>
            <em v-for="label in labelLine(item).slice(1)" :key="label" class="dm-item__extra">{{ label }}</em>
          </span>
        </span>
        <el-tag size="small" type="success" effect="plain">{{ item.count ?? 0 }}</el-tag>
      </div>
        </el-scrollbar>
      </el-splitter-panel>
      <el-splitter-panel>
        <el-scrollbar class="dm-main">
      <template v-if="!current">
        <el-empty description="请选择左侧的实体或关系类型开始管理数据" />
      </template>
      <template v-else>
        <el-alert
          v-if="kind === 'ENTITY' && current.pkUnique === false"
          type="warning"
          show-icon
          :closable="false"
          class="mb-10"
          title="该实体的主键还没有唯一性约束，并发写入可能产生重复数据"
        >
          <el-button link type="primary" @click="router.push('/kg/modeling/constraints')">去创建唯一约束</el-button>
        </el-alert>
        <el-alert
          v-if="largeResult"
          type="info"
          show-icon
          :closable="false"
          class="mb-10"
          title="结果集较大（超过1万条），建议缩小筛选条件或使用导出功能"
        />
        <div class="dm-filter" v-show="searchable">
          <div class="dm-filter__row">
            <div class="dm-filter__field">
              <span class="dm-filter__label">关键字</span>
              <el-input v-model="keyword" placeholder="检索标题或属性" clearable style="width: 260px" @keyup.enter="refresh(true)" />
            </div>
            <div v-if="kind === 'ENTITY'" class="dm-filter__field">
              <span class="dm-filter__label">标签</span>
              <el-input v-model="labelFilter" placeholder="按标签筛选" clearable style="width: 200px" @keyup.enter="refresh(true)" />
            </div>
            <div class="dm-filter__action">
              <el-button type="primary" @click="refresh(true)">查询</el-button>
              <el-button @click="resetFilters">重置</el-button>
              <el-button link type="primary" @click="advancedVisible = !advancedVisible">
                {{ advancedVisible ? '收起高级筛选' : '高级筛选' }}
              </el-button>
            </div>
          </div>
          <el-collapse-transition>
            <div v-show="advancedVisible" class="dm-advanced">
              <div v-if="!filters.length" class="dm-advanced__tip">还没有筛选条件，点击下方“添加条件”按字段精确筛选</div>
              <div v-for="(filter, index) in filters" :key="index" class="dm-advanced__row">
                <span class="dm-advanced__index">{{ index + 1 }}</span>
                <el-select v-model="filter.field" placeholder="选择字段" style="width: 200px">
                  <el-option v-for="field in (kind === 'ENTITY' ? entityFields : current.fields)" :key="field.name" :value="field.name" :label="field.title || field.name" />
                </el-select>
                <el-select v-model="filter.operator" placeholder="条件" style="width: 170px">
                  <el-option v-for="item in operators" :key="item.value" :value="item.value" :label="item.label" />
                </el-select>
                <el-input v-model="filter.value" placeholder="值" style="width: 220px" @keyup.enter="refresh(true)" />
                <el-button link type="danger" @click="filters.splice(index, 1)">移除</el-button>
              </div>
              <div class="dm-advanced__action">
                <el-button link type="primary" @click="filters.push({ field: '', operator: 'eq', value: '' })">添加条件</el-button>
                <el-button link @click="filters = []; refresh(true)">清空条件</el-button>
                <el-button type="primary" @click="refresh(true)">应用筛选</el-button>
              </div>
            </div>
          </el-collapse-transition>
        </div>

        <div class="fs-table-toolbar flex-between">
          <el-space wrap :size="10">
            <el-button v-permit="'kg:graph:add'" type="success" :icon="ElementPlusIcons.Plus" @click="handleAdd">
              新增{{ kind === 'ENTITY' ? '数据' : '关系' }}
            </el-button>
            <el-tooltip content="先勾选需要删除的行" placement="top" :disabled="selection.length > 0">
              <span>
                <el-button v-permit="'kg:graph:delete'" type="danger" :icon="ElementPlusIcons.Delete" :disabled="selection.length === 0" @click="handleDelete(selection)">
                  删除<template v-if="selection.length">（{{ selection.length }}）</template>
                </el-button>
              </span>
            </el-tooltip>
            <el-checkbox v-if="kind === 'ENTITY'" v-model="detach" label="删除时级联删除关系" />
          </el-space>
          <el-space wrap :size="10">
            <el-popover placement="bottom" trigger="click" :width="280" @show="loadQueries">
              <template #reference><el-button :icon="ElementPlusIcons.Collection">检索方案</el-button></template>
              <el-button link type="primary" @click="queryVisible = true">保存当前条件</el-button>
              <el-divider style="margin: 8px 0" />
              <div v-if="!queryList.length" class="dm-form-tip">暂无保存的方案</div>
              <div v-for="item in queryList" :key="item.id" class="dm-query">
                <span class="dm-query__name" @click="applyQuery(item)">{{ item.name }}</span>
                <el-button link type="danger" @click="deleteQuery(item)">删除</el-button>
              </div>
            </el-popover>
            <el-dropdown @command="handleDataCommand">
              <el-button :icon="ElementPlusIcons.Files">导入导出<el-icon class="el-icon--right"><ElementPlusIcons.ArrowDown /></el-icon></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-permit="'kg:graph:add'" command="import">批量导入</el-dropdown-item>
                  <el-dropdown-item command="csv" divided>导出 CSV</el-dropdown-item>
                  <el-dropdown-item command="excel">导出 Excel</el-dropdown-item>
                  <el-dropdown-item command="json">导出 JSON</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-popover v-if="kind === 'ENTITY'" placement="bottom" trigger="click" :width="220">
              <template #reference><el-button :icon="ElementPlusIcons.View">显示字段</el-button></template>
              <el-checkbox-group v-model="visibleFields">
                <div v-for="field in entityFields" :key="field.name">
                  <el-checkbox :value="field.name">{{ field.title || field.name }}</el-checkbox>
                </div>
              </el-checkbox-group>
            </el-popover>
            <el-dropdown @command="handleMoreCommand">
              <el-button>更多<el-icon class="el-icon--right"><ElementPlusIcons.ArrowDown /></el-icon></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="log">变更记录</el-dropdown-item>
                  <el-dropdown-item command="aggregate">字段统计</el-dropdown-item>
                  <el-dropdown-item command="inspect">标签巡检</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <button-search @click="searchable = !searchable" />
            <button-refresh @click="refresh()" :loading="loading" />
          </el-space>
        </div>

        <el-table
          v-loading="loading"
          :data="rows"
          :row-key="(row: any) => kind === 'ENTITY' ? row.elementId : row.relationship?.elementId"
          :border="true"
          table-layout="auto"
          @sort-change="handleSortChange"
          @selection-change="(value: any) => selection = value"
        >
          <el-table-column type="selection" width="45px" />
          <template v-if="kind === 'ENTITY'">
            <el-table-column
              v-for="field in displayFields"
              :key="field.name"
              :prop="'properties.' + field.name"
              :label="field.title || field.name"
              min-width="140"
              sortable="custom"
              show-overflow-tooltip
            >
              <template #default="scope">
                <template v-if="Array.isArray(scope.row.properties?.[field.name])">
                  <el-tag v-for="(value, index) in scope.row.properties[field.name]" :key="index" size="small" effect="plain" style="margin-right: 4px">{{ value }}</el-tag>
                </template>
                <span v-else>{{ renderValue(scope.row.properties?.[field.name]) }}</span>
                <el-tag v-if="field.name === primaryField" size="small" type="warning" effect="plain" style="margin-left: 6px">主键</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="实际标签" width="200px">
              <template #default="scope">
                <el-tag
                  v-for="label in (scope.row.labels ?? [])"
                  :key="label"
                  size="small"
                  effect="plain"
                  :type="(current.labels ?? []).includes(label) ? 'info' : 'warning'"
                  style="margin-right: 4px"
                >{{ label }}</el-tag>
                <span v-if="(scope.row.labels ?? []).length === 0">-</span>
              </template>
            </el-table-column>
          </template>
          <template v-else>
            <el-table-column label="起点" min-width="160">
              <template #default="scope">{{ nodeCaption(scope.row.source) }}</template>
            </el-table-column>
            <el-table-column v-for="field in (current.fields ?? [])" :key="field.name" :prop="'relationship.properties.' + field.name" :label="field.title || field.name" min-width="140" sortable="custom" show-overflow-tooltip>
              <template #default="scope">{{ renderValue(scope.row.relationship?.properties?.[field.name]) }}</template>
            </el-table-column>
            <el-table-column label="终点" min-width="160">
              <template #default="scope">{{ nodeCaption(scope.row.target) }}</template>
            </el-table-column>
          </template>
          <el-table-column label="操作" width="200px" fixed="right">
            <template #default="scope">
              <el-space>
                <el-button link type="primary" @click="handleDetail(scope.row)">详情</el-button>
            <el-button v-permit="'kg:graph:modify'" link type="primary" @click="handleEdit(scope.row)">编辑</el-button>
                <el-button v-if="kind === 'ENTITY'" link type="primary" @click="handleExplore(scope.row)">图探索</el-button>
              </el-space>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty :description="`暂无${kind === 'ENTITY' ? '实体' : '关系'}数据`">
              <el-space>
                <el-button type="primary" @click="handleAdd">新增</el-button>
                <el-button @click="openImport">批量导入</el-button>
              </el-space>
            </el-empty>
          </template>
        </el-table>
        <TablePagination v-model="pagination" @change="refresh()" />
      </template>
        </el-scrollbar>
      </el-splitter-panel>
    </el-splitter>
  </el-card>
  <el-empty v-else description="请先选择本体，再管理该本体下的实体与关系数据">
    <el-button type="primary" @click="router.push('/kg/modeling/ontology')">去本体建模</el-button>
  </el-empty>

  <el-drawer v-model="formVisible" size="520px" :close-on-click-modal="false" :destroy-on-close="true">
    <template #header="{ titleId, titleClass }">
      <h4 :id="titleId" :class="titleClass">
        {{ form.editing ? '编辑' : '新增' }}{{ kind === 'ENTITY' ? '数据' : '关系' }}
      </h4>
    </template>
    <el-form ref="formRef" :model="form.properties" label-position="top">
      <template v-if="kind === 'RELATIONSHIP'">
        <el-form-item required>
          <template #label><span>起点</span><LayoutHelp text="按标题或主键搜索并选择起点数据" /></template>
          <el-select v-model="form.source" filterable remote :remote-method="(query: string) => loadEndpointOptions('source', query)" placeholder="搜索起点数据">
            <el-option v-for="item in sourceOptions" :key="item.value" :value="item.value" :label="item.label" />
          </el-select>
        </el-form-item>
        <el-form-item required>
          <template #label><span>终点</span><LayoutHelp text="按标题或主键搜索并选择终点数据" /></template>
          <el-select v-model="form.target" filterable remote :remote-method="(query: string) => loadEndpointOptions('target', query)" placeholder="搜索终点数据">
            <el-option v-for="item in targetOptions" :key="item.value" :value="item.value" :label="item.label" />
          </el-select>
        </el-form-item>
      </template>
      <el-form-item
        v-for="field in (current?.fields ?? [])"
        :key="field.name"
        :required="field.required"
        :class="{ 'fs-form-inline': widgetOf(field) === 'switch' }"
      >
        <template #label>
          <span>{{ field.title || field.name }}</span>
          <LayoutHelp :text="fieldTip(field)" />
        </template>
        <el-switch v-if="widgetOf(field) === 'switch'" v-model="form.properties[field.name]" />
        <el-input-number v-else-if="widgetOf(field) === 'number'" v-model="form.properties[field.name]" :controls="false" style="width: 100%" />
        <el-date-picker v-else-if="widgetOf(field) === 'date'" v-model="form.properties[field.name]" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        <el-select v-else-if="widgetOf(field) === 'list'" v-model="form.properties[field.name]" multiple filterable allow-create default-first-option style="width: 100%" />
        <el-input
          v-else
          v-model="form.properties[field.name]"
          :readonly="form.editing && kind === 'ENTITY' && field.name === primaryField"
          :placeholder="form.editing && field.name === primaryField ? '主键不可修改' : ''"
        />
      </el-form-item>
      <el-form-item v-if="kind === 'ENTITY' && current?.extendableLabels">
        <template #label>
          <span>扩展标签</span>
          <LayoutHelp text="本体已声明的标签会自动带上，这里只填额外标签；写入后节点会同时带有这些标签" />
        </template>
        <el-select v-model="form.labels" multiple filterable allow-create default-first-option style="width: 100%"
          placeholder="本体声明的标签会自动带上，这里只填额外标签" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-space>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="formLoading" @click="handleSubmit">确定</el-button>
      </el-space>
    </template>
  </el-drawer>

  <el-drawer v-model="detailVisible" size="480px" title="数据详情">
    <el-descriptions v-if="detail" :column="1" border label-width="120px">
      <template v-if="kind === 'ENTITY'">
        <el-descriptions-item v-for="field in (current?.fields ?? [])" :key="field.name" :label="field.title || field.name">
          {{ renderValue(detail.properties?.[field.name]) }}
          <el-tag v-if="field.display" size="small" type="info" effect="plain" style="margin-left: 6px">画布展示</el-tag>
        </el-descriptions-item>
      </template>
      <template v-else>
        <el-descriptions-item label="起点">{{ nodeCaption(detail.source) }}</el-descriptions-item>
        <el-descriptions-item v-for="field in (current?.fields ?? [])" :key="field.name" :label="field.title || field.name">
          {{ renderValue(detail.relationship?.properties?.[field.name]) }}
          <el-tag v-if="field.display" size="small" type="info" effect="plain" style="margin-left: 6px">画布展示</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="终点">{{ nodeCaption(detail.target) }}</el-descriptions-item>
      </template>
    </el-descriptions>
  </el-drawer>

  <el-dialog v-model="importVisible" title="批量导入" width="720px">
    <el-alert type="info" :closable="false" show-icon
      :title="kind === 'ENTITY'
        ? '每行为一条实体数据，主键相同的记录会被合并更新'
        : '每行为一条关系，起点与终点填写两端实体的主键值'" class="mb-10" />
    <div class="flex-between dm-import-bar">
      <el-space wrap :size="10">
        <el-dropdown @command="handleTemplateCommand">
          <el-button :icon="ElementPlusIcons.Download">
            下载模板<el-icon class="el-icon--right"><ElementPlusIcons.ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="xlsx">当前{{ kind === 'ENTITY' ? '实体' : '关系' }}（Excel）</el-dropdown-item>
              <el-dropdown-item command="csv">当前{{ kind === 'ENTITY' ? '实体' : '关系' }}（CSV）</el-dropdown-item>
              <el-dropdown-item command="all" divided>全部实体与关系（Excel，多工作表）</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-button link type="primary" @click="importTemplate">填入示例JSON</el-button>
      </el-space>
      <el-space wrap :size="10">
        <el-upload :auto-upload="false" :show-file-list="false" accept=".csv,text/csv" :on-change="handleCsvUpload">
          <el-button :icon="ElementPlusIcons.Upload">上传CSV</el-button>
        </el-upload>
        <el-upload :auto-upload="false" :show-file-list="false" accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" :on-change="handleExcelUpload">
          <el-button :icon="ElementPlusIcons.Upload">上传Excel</el-button>
        </el-upload>
      </el-space>
    </div>
    <div class="dm-form-tip mb-10">表头使用字段名或显示名均可；留空的单元格表示不修改该属性；列表类型多个值用英文逗号分隔，关系数据用 source/target 两列表示两端</div>
    <el-input v-model="importText" type="textarea" :rows="14" spellcheck="false" />
    <div v-if="importErrors.length" class="dm-import-errors">
      <el-alert type="warning" :closable="false" show-icon :title="`有 ${importErrors.length} 条未导入，修正后可再次导入`" class="mb-10" />
      <el-table :data="importErrors" size="small" border max-height="180">
        <el-table-column prop="message" label="失败原因" min-width="220" />
        <el-table-column label="内容" min-width="260">
          <template #default="scope"><code>{{ JSON.stringify(scope.row.data) }}</code></template>
        </el-table-column>
      </el-table>
    </div>
    <template #footer>
      <el-space>
        <el-button type="primary" :loading="importLoading" @click="handleImport">导入</el-button>
        <el-button @click="importVisible = false">取消</el-button>
      </el-space>
    </template>
  </el-dialog>

  <el-drawer v-model="logVisible" size="720px" title="变更记录">
    <el-space class="mb-10" wrap>
      <el-date-picker v-model="logRange" type="datetimerange" value-format="YYYY-MM-DD HH:mm:ss" start-placeholder="开始时间" end-placeholder="结束时间" />
      <el-button @click="handleLog">查询</el-button>
      <el-button v-permit="'kg:graph:delete'" type="danger" plain @click="handleLogClean">清理所选开始时间之前</el-button>
    </el-space>
    <el-table v-loading="logLoading" :data="logRows" size="small" border>
      <el-table-column type="expand" width="45px">
        <template #default="scope">
          <div class="dm-payload">{{ scope.row.payload }}</div>
        </template>
      </el-table-column>
      <el-table-column label="时间" width="170px">
        <template #default="scope">{{ DateUtil.format(scope.row.createdTime) }}</template>
      </el-table-column>
      <el-table-column prop="uidName" label="操作人" width="110px" />
      <el-table-column label="动作" width="90px">
        <template #default="scope">
          <el-tag size="small" :type="scope.row.action === 'REMOVE' || scope.row.action === 'PURGE' ? 'danger' : 'success'" effect="plain">
            {{ actionText(scope.row.action) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="label" label="类型" width="140px" />
      <el-table-column prop="targets" label="对象" min-width="160px" />
      <el-table-column label="结果" width="90px">
        <template #default="scope">
          <el-tag size="small" :type="scope.row.resultCode === 0 ? 'success' : 'danger'">{{ scope.row.resultCode === 0 ? '成功' : '失败' }}</el-tag>
        </template>
      </el-table-column>
    </el-table>
  </el-drawer>

  <el-dialog v-model="deleteVisible" title="删除确认" width="460px">
    <el-alert type="error" :closable="false" show-icon title="删除是物理删除，删除后不可恢复" class="mb-10" />
    <el-form label-width="110px">
      <el-form-item label="删除数量">
        <span>{{ deleteTargets.length }} 条{{ kind === 'ENTITY' ? '数据' : '关系' }}</span>
      </el-form-item>
      <el-form-item v-if="kind === 'ENTITY'" label="关联关系">
        <el-checkbox v-model="deleteDetach">一并删除关联关系</el-checkbox>
      </el-form-item>
      <el-form-item v-if="kind === 'ENTITY' && !deleteDetach" label="提示">
        <span class="dm-form-tip">数据存在关联关系时删除会失败</span>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-space>
        <el-button type="primary" @click="confirmDelete">确定</el-button>
        <el-button @click="deleteVisible = false">取消</el-button>
      </el-space>
    </template>
  </el-dialog>

  <el-drawer v-model="aggregateVisible" size="560px" title="字段统计">
    <el-space class="mb-10">
      <el-select v-model="aggregateField" filterable placeholder="选择统计字段" style="width: 220px">
        <el-option v-for="field in (current?.fields ?? [])" :key="field.name" :value="field.name" :label="field.title || field.name" />
      </el-select>
      <el-button type="primary" :loading="aggregateLoading" @click="handleAggregate">统计</el-button>
    </el-space>
    <el-table v-loading="aggregateLoading" :data="aggregateRows" size="small" border>
      <el-table-column prop="value" label="值" min-width="180">
        <template #default="scope">{{ renderValue(scope.row.value) }}</template>
      </el-table-column>
      <el-table-column prop="total" label="数量" width="120px" />
    </el-table>
  </el-drawer>

  <el-drawer v-model="inspectVisible" size="760px" title="标签巡检">
    <el-alert type="info" :closable="false" show-icon
      title="检查节点实际标签与本体定义是否一致，异常数据会同时出现在下方的样例中" class="mb-10" />
    <el-table v-loading="inspectLoading" :data="inspectRows" size="small" border>
      <el-table-column type="expand" width="45px">
        <template #default="scope">
          <div v-for="item in (scope.row.samples ?? [])" :key="item.elementId" class="dm-sample">
            <el-tag size="small" type="warning" effect="plain">{{ (item.labels ?? []).join(':') }}</el-tag>
            <span class="dm-form-tip">{{ nodeCaption(item) }}</span>
          </div>
          <div v-if="!(scope.row.samples ?? []).length" class="dm-form-tip">无异常数据</div>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="实体" width="160px" />
      <el-table-column label="定义标签" min-width="200px">
        <template #default="scope">
          <el-tag v-for="label in (scope.row.labels ?? [])" :key="label" size="small" effect="plain" style="margin-right: 4px">{{ label }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="abnormal" label="异常数据" width="100px">
        <template #default="scope">
          <el-tag :type="scope.row.abnormal > 0 ? 'warning' : 'success'" size="small" effect="plain">{{ scope.row.abnormal }}</el-tag>
        </template>
      </el-table-column>
    </el-table>
  </el-drawer>

  <el-dialog v-model="queryVisible" title="保存检索方案" width="420px">
    <el-form label-width="80px">
      <el-form-item label="方案名称" required>
        <el-input v-model="queryName" placeholder="例如：近一年高价值客户" />
      </el-form-item>
      <el-form-item label="包含条件">
        <span class="dm-form-tip">关键字、标签筛选与 {{ filters.length }} 条高级筛选</span>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-space>
        <el-button type="primary" :loading="queryLoading" @click="saveQuery">保存</el-button>
        <el-button @click="queryVisible = false">取消</el-button>
      </el-space>
    </template>
  </el-dialog>
</template>

<style lang="scss" scoped>
.dm-label { color: var(--el-text-color-secondary); font-size: 13px; }
.dm-body {
  padding: 0;
  height: calc(100vh - 250px);
  min-height: 460px;
  :deep(.el-card__body) { height: 100%; }
  :deep(.el-splitter) { height: 100%; }
}
.dm-aside {
  height: 100%;
  padding: 12px;
  border-right: 1px solid var(--el-border-color-lighter);
  &__title {
    margin: 10px 0 6px;
    font-size: 13px;
    color: var(--el-text-color-secondary);
  }
}
.dm-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  &__name {
    display: flex;
    flex-direction: column;
    min-width: 0;
    font-size: 13px;
  }
  &__title {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  &__labels {
    margin-top: 2px;
    font-size: 11px;
    line-height: 16px;
    color: var(--el-text-color-secondary);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  &__extra { font-style: normal; color: var(--el-color-warning); margin-left: 4px; }
  &:hover { background: var(--el-fill-color-light); }
  &--active { background: var(--el-color-primary-light-9); color: var(--el-color-primary); }
}
.dm-main { height: 100%; padding: 12px 16px; }
.dm-advanced {
  padding: 4px 0 0;
  border-bottom: 1px dashed var(--el-border-color-lighter);
}
.dm-advanced__tip {
  padding: 8px 0 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.dm-advanced__row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  & + .dm-advanced__row { border-top: 1px dashed var(--el-border-color-lighter); }
}
.dm-advanced__index {
  flex: none;
  width: 20px;
  height: 20px;
  line-height: 20px;
  border-radius: 50%;
  text-align: center;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color-light);
}
.dm-advanced__action {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0 12px;
  > .el-button + .el-button { margin-left: 0; }
}
.dm-filter {
  padding: 12px 16px 6px;
  margin-bottom: 8px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: var(--el-fill-color-blank);
}
.dm-filter__row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px;
}
.dm-filter__field {
  display: flex;
  align-items: center;
  gap: 8px;
}
.dm-filter__label {
  flex: none;
  font-size: 13px;
  color: var(--el-text-color-regular);
}
.dm-filter__action {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
  > .el-button + .el-button { margin-left: 0; }
}
.dm-form-tip { font-size: 12px; color: var(--el-text-color-secondary); }
.dm-import-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 10px;
}
.dm-payload {
  padding: 10px 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: var(--el-fill-color-blank);
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 12px;
  line-height: 1.7;
}
.dm-sample {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 0;
  font-size: 12px;
  line-height: 1.6;
  & + .dm-sample { border-top: 1px dashed var(--el-border-color-lighter); }
}
.dm-import-errors { margin-top: 10px; }
.dm-query {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 0;
  &__name { cursor: pointer; font-size: 13px; color: var(--el-color-primary); }
}
.mb-10 { margin-bottom: 10px; }
.fs-table-toolbar {
  height: auto;
  min-height: 56px;
  padding: 8px 0 4px;
  row-gap: 10px;
}

@media (max-width: 1280px) {
  .dm-body { height: auto; min-height: 0; }
}
</style>
