<script setup lang="ts">
/**
 * 数据集自定义查询
 * 通过字段、条件与排序生成安全的 Trino SELECT 语句，复用数据集查询接口完成分页浏览和导出。
 */
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import type { TableInstance } from 'element-plus';
import * as ElementPlusIcons from '@element-plus/icons-vue';
import DatasetApi from '@/api/bi/DatasetApi';
import ApiUtil from '@/utils/ApiUtil';
import FormUtil from '@/utils/FormUtil';
import DataQueryFilter from '@/components/Data/DataQueryFilter.vue';
import TablePagination from '@/components/Table/TablePagination.vue';
import TableColumnSetting from '@/components/Table/TableColumnSetting.vue';
import TableSort from '@/components/Table/TableSort.vue';
import RowDataDetail from '@/views/bi/components/RowDataDetail.vue';

interface QueryField {
  name: string
  title?: string
  type?: string
  comment?: string
}

interface FilterNode {
  id?: string
  enabled: boolean
  type: 'RELATION' | 'OPERATION'
  value: string
  left?: string
  right?: string
  children?: FilterNode[]
}

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const datasetLoading = ref(false)
const initialDatasetId = Number(route.query.id)
const datasetId = ref<any>(Number.isInteger(initialDatasetId) && initialDatasetId > 0 ? initialDatasetId : undefined)
const dataset: any = ref(null)
const fields = ref<QueryField[]>([])
const selectedFields = ref<string[]>([])
const columnSearch = ref('')
const activePanel = ref<'none' | 'filter' | 'sql'>('none')
const filterTree = ref<FilterNode[]>([])
const sortValue = ref('')
const rows = ref<any[]>([])
const resultColumns = ref<any[]>([])
const resultVersion = ref(0)
const generatedSql = ref('')
const queryError = ref('')
const queryErrorDetail = ref('')
const queryDetailVisible = ref(false)
const queryDetailIndex = ref(-1)
const pagination = ref<any>({
  currentPage: 1,
  pageSize: 20,
  total: 0,
  background: true,
  layout: 'total, sizes, prev, pager, next, jumper',
  pageSizes: [10, 20, 50, 100, 200],
})
let querySerial = 0
let datasetSerial = 0

const operators = [
  { label: '等于', value: 'EQUAL', valuable: true },
  { label: '不等于', value: 'NOT_EQUAL', valuable: true },
  { label: '小于', value: 'LESS_THAN', valuable: true },
  { label: '小于等于', value: 'LESS_THAN_OR_EQUAL', valuable: true },
  { label: '大于', value: 'GREATER_THAN', valuable: true },
  { label: '大于等于', value: 'GREATER_THAN_OR_EQUAL', valuable: true },
  { label: '包含', value: 'LIKE', valuable: true },
  { label: '不包含', value: 'NOT_LIKE', valuable: true },
  { label: '在列表中', value: 'IN', valuable: true },
  { label: '不在列表中', value: 'NOT_IN', valuable: true },
  { label: '为空', value: 'IS_NULL', valuable: false },
  { label: '不为空', value: 'IS_NOT_NULL', valuable: false },
]
const operatorMap = Object.fromEntries(operators.map(item => [item.value, item]))

const datasetName = computed(() => dataset.value?.name || '')
const fieldMap = computed(() => Object.fromEntries(fields.value.map(field => [field.name, field])))
const visibleResultColumns = computed(() => resultColumns.value.filter((column: any) => !column.hide))
const filterCount = (nodes: FilterNode[]): number => nodes.reduce((total, node) => {
  if (!node.enabled) return total
  if (node.type === 'RELATION') return total + filterCount(node.children || [])
  return total + (node.left ? 1 : 0)
}, 0)
const conditionCount = computed(() => filterCount(filterTree.value))
const sortEntries = computed(() => sortValue.value
  .split(',')
  .map(item => item.trim())
  .filter(Boolean)
  .map(item => {
    const dot = item.lastIndexOf('.')
    return {
      field: dot > 0 ? item.slice(0, dot) : item,
      direction: dot > 0 && item.slice(dot + 1) === 'desc' ? 'desc' : 'asc',
    }
  }))
const sortCount = computed(() => sortEntries.value.length)
const sortColumns = computed(() => fields.value.map(field => ({
  prop: field.name,
  label: field.title || field.name,
})))
const sortableFields = computed(() => Object.fromEntries(fields.value.map(field => [field.name, 'asc'])))
const filteredFields = computed(() => {
  const keyword = columnSearch.value.trim().toLowerCase()
  if (!keyword) return fields.value
  return fields.value.filter(field =>
    (field.name || '').toLowerCase().includes(keyword)
    || (field.title || '').toLowerCase().includes(keyword)
    || (field.comment || '').toLowerCase().includes(keyword)
  )
})
const filterFields = computed<any[]>(() => fields.value)
const queryErrorCopyText = computed(() => [queryError.value, queryErrorDetail.value].filter(Boolean).join('\n\n'))

const icon = (name: string) => (ElementPlusIcons as any)[name]
const fieldLabel = (name: string) => {
  const field = fieldMap.value[name]
  return field?.title || field?.name || name
}
const fieldType = (name: string) => fieldMap.value[name]?.type || 'string'
const isValuable = (operator: string) => operatorMap[operator]?.valuable !== false
const fieldIcon = (type: any) => {
  if (isNumericType(type)) return 'DataLine'
  if (isBooleanType(type)) return 'Switch'
  if (isDateType(type) || isDateTimeType(type) || isTimeType(type)) return 'Calendar'
  return 'Document'
}
const fieldTone = (type: any) => {
  if (isNumericType(type)) return 'number'
  if (isBooleanType(type)) return 'boolean'
  if (isDateType(type) || isDateTimeType(type) || isTimeType(type)) return 'date'
  return 'text'
}
const toggleField = (name: string) => {
  const index = selectedFields.value.indexOf(name)
  if (index >= 0) {
    selectedFields.value.splice(index, 1)
  } else {
    selectedFields.value.push(name)
  }
}
const togglePanel = (panel: 'filter' | 'sql') => {
  activePanel.value = activePanel.value === panel ? 'none' : panel
}

const quoteIdentifier = (value: any) => '"' + String(value ?? '').replace(/"/g, '""') + '"'
const quoteLiteral = (value: any) => "'" + String(value ?? '').replace(/'/g, "''") + "'"
const normalizedType = (value: any) => String(value || '').toLowerCase()
const isNumericType = (value: any) => {
  const type = normalizedType(value)
  return /^(tinyint|smallint|integer|int|bigint|long|real|double|float|decimal|numeric)/.test(type)
}
const isBooleanType = (value: any) => /^(boolean|bool)/.test(normalizedType(value))
const isDateType = (value: any) => normalizedType(value) === 'date'
const isTimeType = (value: any) => normalizedType(value) === 'time'
const isDateTimeType = (value: any) => /^(datetime|timestamp)/.test(normalizedType(value))
const isTextType = (value: any) => !isNumericType(value)
  && !isBooleanType(value)
  && !isDateType(value)
  && !isTimeType(value)
  && !isDateTimeType(value)

const referenceExpression = (raw: string) => {
  const value = raw.trim()
  if (/^`[^`]+`$/.test(value)) return quoteIdentifier(value.slice(1, -1))
  if (/^"[^"]+"$/.test(value)) return value
  return ''
}

const typedLiteral = (fieldName: string, rawValue: string) => {
  const value = rawValue.trim()
  const reference = referenceExpression(value)
  if (reference) return reference
  const type = fieldType(fieldName)
  if (!value) {
    if (isTextType(type)) return quoteLiteral(rawValue)
    throw new Error(`字段「${fieldLabel(fieldName)}」的查询值不能为空`)
  }
  if (isNumericType(type)) {
    if (!/^[-+]?(?:\d+(?:\.\d*)?|\.\d+)(?:e[-+]?\d+)?$/i.test(value)) {
      throw new Error(`字段「${fieldLabel(fieldName)}」需要输入有效数值`)
    }
    return value
  }
  if (isBooleanType(type)) {
    if (!/^(true|false)$/i.test(value)) {
      throw new Error(`字段「${fieldLabel(fieldName)}」只能输入 true 或 false`)
    }
    return value.toLowerCase()
  }
  if (isDateType(type)) return `DATE ${quoteLiteral(value)}`
  if (isDateTimeType(type)) return `TIMESTAMP ${quoteLiteral(value)}`
  if (isTimeType(type)) return `TIME ${quoteLiteral(value)}`
  return quoteLiteral(rawValue)
}

const listExpression = (fieldName: string, raw: string) => {
  const values = raw.split(',').map(item => item.trim()).filter(Boolean)
  if (!values.length) throw new Error(`字段「${fieldLabel(fieldName)}」的列表值不能为空`)
  return values.map(value => typedLiteral(fieldName, value)).join(', ')
}

const buildFilterNode = (node: FilterNode): string => {
  if (!node.enabled) return ''
  if (node.type === 'RELATION') {
    const children = (node.children || []).map(buildFilterNode).filter(Boolean)
    if (!children.length) return ''
    const glue = node.value === 'OR' ? 'OR' : 'AND'
    return `(${children.join(` ${glue} `)})`
  }
  const fieldName = String(node.left || '').trim()
  if (!fieldName) throw new Error('请选择筛选条件字段')
  const identifier = quoteIdentifier(fieldName)
  const operation = node.value
  const raw = String(node.right || '')
  const allowEmpty = ['EQUAL', 'NOT_EQUAL'].includes(operation) && isTextType(fieldType(fieldName))
  if (isValuable(operation) && !raw.trim() && !allowEmpty) {
    throw new Error(`请填写字段「${fieldLabel(fieldName)}」的查询值`)
  }
  switch (operation) {
    case 'IS_NULL':
      return `${identifier} IS NULL`
    case 'IS_NOT_NULL':
      return `${identifier} IS NOT NULL`
    case 'IN':
      return `${identifier} IN (${listExpression(fieldName, raw)})`
    case 'NOT_IN':
      return `${identifier} NOT IN (${listExpression(fieldName, raw)})`
    case 'NOT_EQUAL':
      return `${identifier} <> ${typedLiteral(fieldName, raw)}`
    case 'LESS_THAN':
      return `${identifier} < ${typedLiteral(fieldName, raw)}`
    case 'LESS_THAN_OR_EQUAL':
      return `${identifier} <= ${typedLiteral(fieldName, raw)}`
    case 'GREATER_THAN':
      return `${identifier} > ${typedLiteral(fieldName, raw)}`
    case 'GREATER_THAN_OR_EQUAL':
      return `${identifier} >= ${typedLiteral(fieldName, raw)}`
    case 'LIKE':
      return `CAST(${identifier} AS VARCHAR) LIKE ${quoteLiteral(raw)}`
    case 'NOT_LIKE':
      return `CAST(${identifier} AS VARCHAR) NOT LIKE ${quoteLiteral(raw)}`
    case 'EQUAL':
    default:
      return `${identifier} = ${typedLiteral(fieldName, raw)}`
  }
}

const buildFilterSql = () => filterTree.value
  .map(buildFilterNode)
  .filter(Boolean)
  .join(' AND ')

const validate = () => {
  if (!datasetName.value) {
    ElMessage.warning('请先选择数据集')
    return false
  }
  if (!selectedFields.value.length) {
    ElMessage.warning('请至少选择一个查询字段')
    return false
  }
  return true
}

const buildSql = (includePage = true) => {
  const selected = selectedFields.value.map(quoteIdentifier).join(', ')
  const parts = [`SELECT ${selected}`, `FROM ${quoteIdentifier(datasetName.value)}`]
  const where = buildFilterSql()
  if (where) parts.push(`WHERE ${where}`)
  if (sortEntries.value.length) {
    parts.push(`ORDER BY ${sortEntries.value.map(item => `${quoteIdentifier(item.field)} ${item.direction.toUpperCase()}`).join(', ')}`)
  }
  if (includePage) {
    const offset = (pagination.value.currentPage - 1) * pagination.value.pageSize
    // 当前 Trino 版本要求 OFFSET 位于 LIMIT 之前
    parts.push(`OFFSET ${offset} LIMIT ${pagination.value.pageSize}`)
  }
  return parts.join('\n')
}

const buildCountSql = () => {
  const where = buildFilterSql()
  return `SELECT COUNT(*) AS "total" FROM ${quoteIdentifier(datasetName.value)}${where ? ` WHERE ${where}` : ''}`
}

const clearQueryError = () => {
  queryError.value = ''
  queryErrorDetail.value = ''
}

const setQueryError = (message: any, detail: any) => {
  queryError.value = String(message || '查询失败')
  if (detail === null || detail === undefined || detail === '') {
    queryErrorDetail.value = ''
  } else if (typeof detail === 'string') {
    queryErrorDetail.value = detail
  } else {
    try {
      queryErrorDetail.value = JSON.stringify(detail, null, 2)
    } catch {
      queryErrorDetail.value = String(detail)
    }
  }
}

const handleQuery = (keepPage = false) => {
  if (loading.value || !validate()) return
  queryDetailVisible.value = false
  queryDetailIndex.value = -1
  if (!keepPage) pagination.value.currentPage = 1
  let sql = ''
  let countSql = ''
  try {
    sql = buildSql()
    countSql = buildCountSql()
  } catch (error: any) {
    ElMessage.warning(error?.message || String(error))
    return
  }
  generatedSql.value = sql
  clearQueryError()
  loading.value = true
  const serial = ++querySerial
  Promise.all([
    DatasetApi.query({ sql, limit: pagination.value.pageSize, timeout: 60 }, { warning: false }),
    DatasetApi.query({ sql: countSql, limit: 1, timeout: 60 }, { warning: false }),
  ]).then(([result, countResult]: any[]) => {
    if (serial !== querySerial) return
    const data = ApiUtil.data(result) || {}
    const countData = ApiUtil.data(countResult) || {}
    rows.value = data.rows || []
    resultColumns.value = (data.columns || []).map((column: any) => ({
      prop: column.name,
      label: fieldLabel(column.name),
      type: column.type,
      name: column.name,
    }))
    pagination.value.total = Number(countData.rows?.[0]?.total ?? 0)
    resultVersion.value++
  }).catch((result: any) => {
    if (serial !== querySerial) return
    rows.value = []
    resultColumns.value = []
    pagination.value.total = 0
    setQueryError(ApiUtil.message(result), ApiUtil.data(result))
  }).finally(() => {
    if (serial === querySerial) loading.value = false
  })
}

const selectAllFields = (checked: boolean) => {
  selectedFields.value = checked ? fields.value.map(field => field.name) : []
}

const invertFields = () => {
  const selected = new Set(selectedFields.value)
  selectedFields.value = fields.value
    .filter(field => !selected.has(field.name))
    .map(field => field.name)
}

const loadDataset = (id: any) => {
  if (!id) return
  querySerial++
  const serial = ++datasetSerial
  loading.value = false
  datasetLoading.value = true
  rows.value = []
  resultColumns.value = []
  generatedSql.value = ''
  clearQueryError()
  pagination.value.total = 0
  DatasetApi.columns(id, { warning: false }).then((result: any) => {
    if (serial !== datasetSerial) return
    const data = ApiUtil.data(result) || {}
    dataset.value = {
      id: data.id,
      name: data.name,
      description: data.description,
    }
    fields.value = (data.columns || []).filter((field: any) => field.name)
    selectedFields.value = fields.value.map(field => field.name)
    filterTree.value = []
    sortValue.value = ''
    pagination.value.currentPage = 1
    if (String(route.query.id || '') !== String(id)) {
      router.replace({ path: route.path, query: { ...route.query, id } }).catch(() => {})
    }
    if (fields.value.length) handleQuery()
  }).catch((result: any) => {
    if (serial !== datasetSerial) return
    dataset.value = null
    fields.value = []
    selectedFields.value = []
    ElMessage.warning(ApiUtil.message(result) || '数据集字段加载失败')
  }).finally(() => {
    if (serial === datasetSerial) datasetLoading.value = false
  })
}

const handleDatasetChange = (id: any) => {
  if (!id) {
    querySerial++
    datasetSerial++
    loading.value = false
    datasetLoading.value = false
    queryDetailVisible.value = false
    queryDetailIndex.value = -1
    dataset.value = null
    fields.value = []
    selectedFields.value = []
    filterTree.value = []
    sortValue.value = ''
    rows.value = []
    resultColumns.value = []
    pagination.value.total = 0
    generatedSql.value = ''
    clearQueryError()
    const query = { ...route.query }
    delete query.id
    router.replace({ path: route.path, query }).catch(() => {})
    return
  }
  loadDataset(id)
}

const handlePageChange = () => {
  handleQuery(true)
}

const handleSortChange = () => {
  pagination.value.currentPage = 1
  handleQuery()
}

const exportLoading = ref(false)
const handleExport = () => {
  if (exportLoading.value || !validate()) return
  let sql = ''
  try {
    sql = buildSql(false)
  } catch (error: any) {
    ElMessage.warning(error?.message || String(error))
    return
  }
  exportLoading.value = true
  FormUtil.download(import.meta.env.VITE_APP_API_URL + '/bi/dataset/query', {
    sql,
    limit: 10000,
    timeout: 60,
    download: true,
  })
  ElMessage.success('导出任务已提交')
  window.setTimeout(() => {
    exportLoading.value = false
  }, 1000)
}

const showRow = (row: any) => {
  queryDetailIndex.value = rows.value.indexOf(row)
  queryDetailVisible.value = true
}

const formatCell = (value: any) => {
  if (value === null || value === undefined) return 'NULL'
  if (value === '') return '空字符串'
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

const cellKind = (value: any) => {
  if (value === null || value === undefined) return 'null'
  if (value === '') return 'empty-string'
  return 'value'
}

const handleBack = () => {
  const history = router.options.history as any
  const backPath = String(history?.state?.back || '')
  const currentPath = router.currentRoute.value.fullPath
  const skipPaths = ['/startup', '/login', '/signup', '/forgot', '/redirect']
  const skipBack = skipPaths.some(path => backPath === path || backPath.startsWith(path + '?') || backPath.startsWith(path + '#'))
  if (backPath.startsWith('/') && backPath !== currentPath && !skipBack) {
    router.back()
    return
  }
  router.replace('/bi/data/dataset').catch(() => {})
}

onMounted(() => {
  if (datasetId.value) loadDataset(datasetId.value)
})
</script>

<template>
  <div class="dataset-query">
    <header class="query-header">
      <div class="query-header__brand">
        <el-tooltip content="返回数据集列表" placement="bottom">
          <el-button
            class="query-header__back"
            :icon="ElementPlusIcons.ArrowLeft"
            aria-label="返回数据集列表"
            @click="handleBack"
          />
        </el-tooltip>
        <div class="query-header__title">
          <h1>数据查询</h1>
          <p>{{ dataset?.description || '选择字段并组合筛选、排序规则' }}</p>
        </div>
      </div>
      <div class="query-header__picker">
        <span class="dataset-label">数据集</span>
        <form-select
          v-model="datasetId"
          v-model:selected="dataset"
          :callback="DatasetApi.list"
          placeholder="搜索并选择数据集"
          clearable
          class="dataset-select"
          @change="handleDatasetChange"
        />
        <template v-if="datasetName">
          <span class="dataset-meta">
            <el-icon><ElementPlusIcons.CollectionTag /></el-icon>
            {{ fields.length }} 个字段
          </span>
        </template>
      </div>
      <div class="query-header__actions">
        <el-button :icon="icon('Download')" :loading="exportLoading" :disabled="!datasetName" @click="handleExport">导出</el-button>
        <el-button type="primary" :icon="icon('Search')" :loading="loading" :disabled="!datasetName" @click="handleQuery(false)">执行查询</el-button>
      </div>
    </header>

    <el-splitter v-if="datasetName" class="query-body">
      <el-splitter-panel :size="280" :min="230" :max="400" collapsible>
        <aside class="field-panel">
          <div class="field-panel__head">
            <div>
              <strong>数据字段</strong>
              <span class="field-panel__count">{{ selectedFields.length }} / {{ fields.length }}</span>
            </div>
            <div class="field-panel__actions">
              <el-button text size="small" @click="invertFields">反选</el-button>
              <el-button
                text
                type="primary"
                size="small"
                class="field-panel__all"
                @click="selectAllFields(selectedFields.length !== fields.length)"
              >
                {{ selectedFields.length === fields.length ? '取消全选' : '全选' }}
              </el-button>
            </div>
          </div>
          <div class="field-panel__search">
            <el-input v-model="columnSearch" placeholder="搜索字段" clearable :prefix-icon="icon('Search')" />
          </div>
          <el-scrollbar class="field-panel__scroll">
            <div class="field-list">
              <button
                v-for="field in filteredFields"
                :key="field.name"
                type="button"
                class="field-item"
                :class="{ 'is-selected': selectedFields.includes(field.name) }"
                :aria-pressed="selectedFields.includes(field.name)"
                :title="field.comment || field.name"
                @click="toggleField(field.name)"
              >
                <span class="field-item__check">
                  <el-icon v-if="selectedFields.includes(field.name)"><ElementPlusIcons.Check /></el-icon>
                </span>
                <span class="field-item__kind" :class="`is-${fieldTone(field.type)}`">
                  <el-icon><component :is="icon(fieldIcon(field.type))" /></el-icon>
                </span>
                <span class="field-item__copy">
                  <span class="field-item__label">{{ field.title || field.name }}</span>
                  <small v-if="field.title && field.title !== field.name">{{ field.name }}</small>
                </span>
                <span class="field-item__type">{{ field.type || 'string' }}</span>
              </button>
              <el-empty
                v-if="!filteredFields.length"
                :description="columnSearch ? '没有匹配字段' : '该数据集暂无字段'"
                :image-size="54"
              />
            </div>
          </el-scrollbar>
          <div class="field-panel__footer">
            <span class="field-panel__selection">
              <el-icon><ElementPlusIcons.Select /></el-icon>
              已选择 {{ selectedFields.length }} 个字段
            </span>
            <el-button text size="small" @click="selectAllFields(false)">清空</el-button>
          </div>
        </aside>
      </el-splitter-panel>

      <el-splitter-panel>
        <section class="query-workbench">
          <div class="workbench-toolbar">
            <div class="query-tools">
              <button
                type="button"
                class="tool-chip"
                :class="{ 'is-active': activePanel === 'filter' }"
                @click="togglePanel('filter')"
              >
                <el-icon><ElementPlusIcons.Filter /></el-icon>
                <span>筛选</span>
                <em v-if="conditionCount">{{ conditionCount }}</em>
              </button>
              <TableSort
                v-model="sortValue"
                label="排序"
                tool
                size="small"
                :columns="sortColumns"
                :sortable="sortableFields"
                :loading="loading"
                @change="handleSortChange"
              />
              <button
                type="button"
                class="tool-chip"
                :class="{ 'is-active': activePanel === 'sql' }"
                @click="togglePanel('sql')"
              >
                <el-icon><ElementPlusIcons.Tickets /></el-icon>
                <span>SQL</span>
              </button>
            </div>
            <div class="result-tools">
              <span class="result-total">
                <strong>{{ pagination.total.toLocaleString() }}</strong> 条结果
              </span>
              <TableColumnSetting :key="resultVersion" v-model="resultColumns" :table="tableRef" :loading="loading" text />
              <el-tooltip content="刷新结果" placement="bottom">
                <el-button :icon="icon('RefreshRight')" circle :loading="loading" @click="handleQuery(true)" />
              </el-tooltip>
            </div>
          </div>

          <transition name="config-panel">
            <section v-if="activePanel !== 'none'" class="config-panel">
              <div class="config-panel__body">
                <template v-if="activePanel === 'filter'">
                  <div class="config-panel__head">
                    <div>
                      <strong>筛选条件</strong>
                      <span>支持嵌套且/或关系，拖动左侧手柄可调整条件顺序</span>
                    </div>
                    <div class="config-panel__actions">
                      <el-button text @click="activePanel = 'none'">收起</el-button>
                      <el-button type="primary" :icon="icon('Search')" @click="handleQuery(false)">应用筛选</el-button>
                    </div>
                  </div>
                  <div class="filter-composer">
                    <DataQueryFilter v-model="filterTree" :fields="filterFields" />
                  </div>
                </template>

                <template v-else-if="activePanel === 'sql'">
                  <div class="config-panel__head">
                    <div>
                      <strong>查询 SQL</strong>
                      <span>仅用于核对当前查询，实际查询仍受数据集权限校验</span>
                    </div>
                    <el-button
                      text
                      :icon="icon('CopyDocument')"
                      :disabled="!generatedSql"
                      @click="FormUtil.copyToClipboard(generatedSql).then(() => ElMessage.success('SQL 已复制')).catch(() => {})"
                    >
                      复制 SQL
                    </el-button>
                  </div>
                  <pre class="sql-preview">{{ generatedSql || '执行查询后在这里显示 SQL' }}</pre>
                </template>
              </div>
            </section>
          </transition>

          <div class="result-grid">
            <el-table
              v-if="!queryError"
              ref="tableRef"
              :data="rows"
              :border="true"
              stripe
              height="100%"
              size="small"
              table-layout="auto"
              v-loading="loading || datasetLoading"
              @row-dblclick="showRow"
            >
              <el-table-column
                v-for="column in visibleResultColumns"
                :key="column.prop"
                :prop="column.prop"
                :min-width="148"
                resizable
                show-overflow-tooltip
              >
                <template #header>
                  <div class="column-head">
                    <span>{{ column.label }}</span>
                    <small>{{ column.type }}</small>
                  </div>
                </template>
                <template #default="scope">
                  <span class="cell-value" :class="`is-${cellKind(scope.row[column.prop])}`">
                    {{ formatCell(scope.row[column.prop]) }}
                  </span>
                </template>
              </el-table-column>
              <template #empty>
                <div class="result-empty">
                  <el-icon><ElementPlusIcons.DataAnalysis /></el-icon>
                  <strong>{{ loading ? '正在查询数据' : '当前条件下暂无数据' }}</strong>
                  <span>{{ loading ? '请稍候，结果返回后将自动展示' : '可以调整筛选条件后重新查询' }}</span>
                </div>
              </template>
            </el-table>
            <div v-else class="query-error">
              <div class="query-error__head">
                <span class="query-error__icon"><el-icon><ElementPlusIcons.CircleCloseFilled /></el-icon></span>
                <div class="query-error__title">
                  <strong>查询失败</strong>
                  <span>{{ queryError }}</span>
                </div>
                <el-button
                  text
                  :icon="icon('CopyDocument')"
                  :disabled="!queryErrorCopyText"
                  @click="FormUtil.copyToClipboard(queryErrorCopyText).then(() => ElMessage.success('错误详情已复制')).catch(() => {})"
                >
                  复制详情
                </el-button>
              </div>
              <pre v-if="queryErrorDetail" class="query-error__detail">{{ queryErrorDetail }}</pre>
              <div v-else class="query-error__empty">服务端未返回更详细的错误信息，请检查查询条件后重试。</div>
            </div>
          </div>
          <footer class="result-footer">
            <span class="result-hint">
              双击行查看详情
            </span>
            <TablePagination v-model="pagination" :loading="loading" @change="handlePageChange" />
          </footer>
        </section>
      </el-splitter-panel>
    </el-splitter>

    <main v-else class="empty-workspace">
      <div class="empty-workspace__content">
        <span class="empty-workspace__icon"><el-icon><ElementPlusIcons.DataAnalysis /></el-icon></span>
        <h2>选择数据集开始查询</h2>
        <p>从顶部的数据集选择器中选择需要浏览的数据，随后可以自由选择字段、组合筛选条件并设置排序。</p>
        <div class="empty-workspace__features">
          <span><el-icon><ElementPlusIcons.Select /></el-icon>字段筛选</span>
          <span><el-icon><ElementPlusIcons.Filter /></el-icon>条件组合</span>
          <span><el-icon><ElementPlusIcons.Sort /></el-icon>多列排序</span>
          <span><el-icon><ElementPlusIcons.Download /></el-icon>结果导出</span>
        </div>
      </div>
    </main>

    <RowDataDetail
      v-model="queryDetailVisible"
      :row-index="queryDetailIndex"
      :rows="rows"
      :columns="visibleResultColumns"
      @changeRow="queryDetailIndex = $event"
    />
  </div>
</template>

<style lang="scss" scoped>
.dataset-query {
  --query-toolbar-height: 50px;
  --query-footer-height: 46px;
  --query-control-height: 30px;

  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  color: var(--el-text-color-primary);
  background: var(--fs-layout-background-color);
}

.query-header {
  flex: none;
  display: grid;
  grid-template-columns: minmax(210px, 1fr) minmax(320px, auto) minmax(210px, 1fr);
  align-items: center;
  gap: 20px;
  min-height: 64px;
  padding: 0 18px;
  border-bottom: 1px solid var(--el-border-color-light);
  background: var(--el-bg-color);
}

.query-header__brand,
.query-header__picker,
.query-header__actions {
  display: flex;
  align-items: center;
}

.query-header__brand {
  gap: 10px;
  min-width: 0;
}

.query-header__back {
  flex: none;
  display: inline-flex;
  width: var(--query-control-height);
  height: var(--query-control-height);
  align-items: center;
  justify-content: center;
  margin: 0;
  padding: 0;
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  color: var(--el-text-color-regular);
  background: var(--el-bg-color);

  &:hover {
    color: var(--el-color-primary);
    border-color: var(--el-color-primary-light-5);
    background: var(--el-color-primary-light-9);
  }

  :deep(.el-icon) {
    margin: 0;
    font-size: 15px;
  }
}

.query-header__title {
  min-width: 0;

  h1 {
    margin: 0;
    font-size: 16px;
    font-weight: 600;
    line-height: 22px;
  }

  p {
    margin: 1px 0 0;
    overflow: hidden;
    color: var(--el-text-color-secondary);
    font-size: 11px;
    line-height: 16px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.query-header__picker {
  gap: 8px;
  min-width: 0;
  min-height: 38px;
  padding: 3px 5px 3px 12px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: #f8fafc;
  transition: border-color 0.15s, box-shadow 0.15s;

  &:focus-within {
    border-color: var(--el-color-primary-light-5);
    box-shadow: 0 0 0 3px var(--el-color-primary-light-9);
  }
}

.dataset-label {
  flex: none;
  color: var(--el-text-color-regular);
  font-size: 12px;
}

.dataset-select {
  width: 300px;

  :deep(.el-select__wrapper) {
    min-height: 30px;
    box-shadow: none;
    background: transparent;
  }
}

.dataset-meta {
  display: inline-flex;
  flex: none;
  align-items: center;
  gap: 4px;
  padding: 3px 8px;
  border-radius: 999px;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  font-size: 11px;

  .el-icon {
    font-size: 13px;
  }
}

.query-header__actions {
  justify-content: flex-end;
  gap: 8px;

  .el-button {
    height: var(--query-control-height);
    margin-left: 0;
    border-radius: 6px;
  }
}

.query-body {
  flex: 1;
  min-height: 0;

  :deep(.el-splitter-panel) {
    overflow: hidden;
  }
}

.field-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  background: var(--el-bg-color);
}

.field-panel__head {
  display: flex;
  flex: none;
  align-items: center;
  justify-content: space-between;
  height: var(--query-toolbar-height);
  padding: 0 14px;
  border-bottom: 1px solid var(--el-border-color-extra-light);
  background: #fbfcfe;
  box-sizing: border-box;

  > div {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  strong {
    font-size: 13px;
    font-weight: 600;
  }
}

.field-panel__all {
  min-width: 62px;
  height: 26px;
  padding: 0 6px;
  font-size: 11px;
}

.field-panel__head > .field-panel__actions {
  display: flex;
  align-items: center;
  gap: 2px;

  .el-button {
    height: 26px;
    margin-left: 0;
    padding: 0 5px;
    font-size: 11px;
  }
}

.field-panel__count {
  padding: 1px 7px;
  border-radius: 999px;
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color-light);
  font-size: 10px;
  line-height: 18px;
}

.field-panel__search {
  flex: none;
  padding: 10px 12px;

  :deep(.el-input__wrapper) {
    min-height: var(--query-control-height);
    border-radius: 6px;
    box-shadow: 0 0 0 1px var(--el-border-color-light) inset;
    background: var(--el-fill-color-lighter);

    &.is-focus {
      box-shadow: 0 0 0 1px var(--el-color-primary) inset;
    }
  }
}

.field-panel__scroll {
  flex: 1;
  min-height: 0;

  :deep(.el-scrollbar__view) {
    padding: 2px 8px 10px;
  }
}

.field-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.field-item {
  display: flex;
  width: 100%;
  min-height: 36px;
  align-items: center;
  gap: 7px;
  padding: 3px 7px;
  border: 0;
  border-radius: 6px;
  color: var(--el-text-color-regular);
  background: transparent;
  font-family: inherit;
  text-align: left;
  cursor: pointer;
  transition: background-color 0.15s, color 0.15s, box-shadow 0.15s;

  &:hover {
    background: var(--el-fill-color-light);
  }

  &:focus-visible {
    outline: none;
    box-shadow: 0 0 0 2px var(--el-color-primary-light-7);
  }

  &.is-selected {
    color: var(--el-text-color-primary);
    background: var(--el-color-primary-light-9);
    box-shadow: inset 2px 0 0 var(--el-color-primary);
  }
}

.field-item__check {
  display: inline-flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: 15px;
  height: 15px;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  color: #fff;
  background: var(--el-bg-color);

  .is-selected & {
    border-color: var(--el-color-primary);
    background: var(--el-color-primary);
  }

  .el-icon {
    font-size: 11px;
  }
}

.field-item__kind {
  display: inline-flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 5px;
  font-size: 12px;
  background: var(--el-fill-color-light);

  &.is-number {
    color: #2f7ee6;
    background: #edf5ff;
  }

  &.is-date {
    color: #b26a00;
    background: #fff7e6;
  }

  &.is-boolean {
    color: #158860;
    background: #eafaf3;
  }
}

.field-item__copy {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;

  small {
    overflow: hidden;
    color: var(--el-text-color-placeholder);
    font-size: 10px;
    line-height: 13px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.field-item__label {
  overflow: hidden;
  font-size: 12px;
  line-height: 17px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.field-item__type {
  flex: none;
  max-width: 74px;
  overflow: hidden;
  color: var(--el-text-color-placeholder);
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 9px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.field-panel__footer {
  display: flex;
  flex: none;
  height: var(--query-footer-height);
  min-height: var(--query-footer-height);
  align-items: center;
  justify-content: space-between;
  padding: 0 8px 0 14px;
  border-top: 1px solid var(--el-border-color-extra-light);
  color: var(--el-text-color-secondary);
  background: #fbfcfe;
  font-size: 11px;
  box-sizing: border-box;
}

.field-panel__selection {
  display: inline-flex;
  align-items: center;
  gap: 5px;

  .el-icon {
    color: var(--el-color-primary);
    font-size: 13px;
  }
}

.query-workbench {
  position: relative;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-width: 0;
  min-height: 0;
  background: var(--el-bg-color);
}

.workbench-toolbar {
  display: flex;
  flex: none;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  height: var(--query-toolbar-height);
  padding: 0 14px;
  border-bottom: 1px solid var(--el-border-color-light);
  background: #fbfcfe;
  box-sizing: border-box;
}

.query-tools,
.result-tools {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tool-chip {
  display: inline-flex;
  height: var(--query-control-height);
  align-items: center;
  gap: 6px;
  padding: 0 10px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  color: var(--el-text-color-regular);
  background: var(--el-bg-color);
  font-family: inherit;
  font-size: 12px;
  cursor: pointer;
  transition: color 0.15s, border-color 0.15s, background-color 0.15s, box-shadow 0.15s;

  &:hover {
    color: var(--el-color-primary);
    border-color: var(--el-color-primary-light-5);
  }

  &.is-active {
    color: var(--el-color-primary);
    border-color: var(--el-color-primary-light-5);
    background: var(--el-color-primary-light-9);
    box-shadow: 0 0 0 2px var(--el-color-primary-light-9);
  }

  &:focus-visible {
    outline: none;
    box-shadow: 0 0 0 2px var(--el-color-primary-light-7);
  }

  em {
    display: inline-flex;
    min-width: 17px;
    height: 17px;
    align-items: center;
    justify-content: center;
    padding: 0 4px;
    border-radius: 999px;
    color: #fff;
    background: var(--el-color-primary);
    font-size: 9px;
    font-style: normal;
  }
}

.result-total {
  margin-right: 4px;
  color: var(--el-text-color-secondary);
  font-size: 11px;

  strong {
    color: var(--el-text-color-primary);
    font-size: 13px;
    font-weight: 600;
  }
}

.result-tools :deep(.el-button.is-circle) {
  width: var(--query-control-height);
  height: var(--query-control-height);
}

.result-tools {
  :deep(.el-button) {
    height: var(--query-control-height);
    margin-left: 0;
  }
}

.config-panel {
  display: flex;
  flex: none;
  max-height: 340px;
  flex-direction: column;
  overflow: hidden;
  border-bottom: 1px solid var(--el-border-color-light);
  background: #f8fafc;
}

.config-panel__body {
  flex: 1 1 auto;
  min-height: 0;
  max-height: 340px;
  overflow-x: hidden;
  overflow-y: auto;
  padding: 12px 16px;
  box-sizing: border-box;
  overscroll-behavior: contain;
}

.config-panel-enter-active,
.config-panel-leave-active {
  transition: opacity 0.12s ease;
}

.config-panel-enter-from,
.config-panel-leave-to {
  opacity: 0;
}

.config-panel__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 10px;

  > div:first-child {
    display: flex;
    align-items: baseline;
    gap: 8px;
  }

  strong {
    font-size: 12px;
    font-weight: 600;
  }

  span {
    color: var(--el-text-color-secondary);
    font-size: 11px;
  }
}

.config-panel__actions {
  display: flex;
  flex: none;
  align-items: center;
  gap: 6px;

  .el-button {
    margin-left: 0;
  }
}

.filter-composer {
  padding: 10px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 7px;
  background: var(--el-bg-color);

  :deep(.el-input__wrapper),
  :deep(.el-select__wrapper) {
    min-height: 26px;
  }

  :deep(.query-filter-toolbar) {
    padding-bottom: 8px;
    border-bottom: 1px dashed var(--el-border-color-lighter);
  }

  :deep(.query-filter-node) {
    border-radius: 5px;
  }

  :deep(.query-filter-node:not(.is-relation)) {
    padding-right: 6px;
    padding-left: 6px;
    background: #fbfcfe;
  }
}

.sql-preview {
  margin: 0;
  padding: 10px 12px;
  overflow: auto;
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  color: var(--el-text-color-regular);
  background: var(--el-bg-color);
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 11px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.result-grid {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;

  :deep(.el-table) {
    --el-table-header-bg-color: #f8fafc;
    --el-table-row-hover-bg-color: #f3f8ff;
    --el-table-border-color: var(--el-border-color-extra-light);
    height: 100%;
    font-size: 12px;
  }

  :deep(.el-table th.el-table__cell) {
    height: 38px;
    padding: 0;
    border-bottom: 1px solid var(--el-border-color-light);
  }

  :deep(.el-table td.el-table__cell) {
    height: 36px;
    padding: 0;
  }

  :deep(.el-table .cell) {
    padding-right: 12px;
    padding-left: 12px;
  }

  :deep(.el-table__inner-wrapper::before) {
    display: none;
  }
}

.query-error {
  display: flex;
  flex: 1;
  min-height: 0;
  flex-direction: column;
  margin: 16px;
  overflow: hidden;
  border: 1px solid var(--el-color-danger-light-7);
  border-radius: 8px;
  background: var(--el-color-danger-light-9);
}

.query-error__head {
  display: flex;
  flex: none;
  align-items: flex-start;
  gap: 10px;
  padding: 14px 16px;
  border-bottom: 1px solid var(--el-color-danger-light-8);

  > .el-button {
    flex: none;
    margin-left: auto;
  }
}

.query-error__icon {
  display: inline-flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  color: var(--el-color-danger);
  background: var(--el-bg-color);

  .el-icon {
    font-size: 16px;
  }
}

.query-error__title {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
  gap: 3px;

  strong {
    color: var(--el-color-danger);
    font-size: 13px;
    font-weight: 600;
  }

  span {
    color: var(--el-text-color-regular);
    font-size: 12px;
    line-height: 1.55;
  }
}

.query-error__detail {
  flex: 1;
  min-height: 0;
  margin: 0;
  padding: 14px 16px;
  overflow: auto;
  color: var(--el-text-color-regular);
  background: var(--el-bg-color);
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 11px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.query-error__empty {
  padding: 14px 16px;
  color: var(--el-text-color-secondary);
  background: var(--el-bg-color);
  font-size: 12px;
}

.column-head {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  line-height: 1.15;

  span {
    font-weight: 600;
    line-height: 16px;
  }

  small {
    color: var(--el-text-color-placeholder);
    font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
    font-size: 9px;
    font-weight: 400;
    line-height: 11px;
  }
}

.cell-value.is-null,
.cell-value.is-empty-string {
  display: inline-flex;
  min-height: 18px;
  align-items: center;
  padding: 0 6px;
  border-radius: 4px;
  font-family: ui-monospace, SFMono-Regular, Consolas, monospace;
  font-size: 10px;
  line-height: 18px;
  pointer-events: none;
  user-select: none;
  -webkit-user-select: none;
  white-space: nowrap;
}

.cell-value.is-null {
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color-light);
}

.cell-value.is-empty-string {
  color: var(--el-color-warning-dark-2);
  background: var(--el-color-warning-light-9);
  box-shadow: 0 0 0 1px var(--el-color-warning-light-7) inset;
}

.result-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 32px 0;
  color: var(--el-text-color-secondary);

  .el-icon {
    margin-bottom: 2px;
    color: var(--el-text-color-placeholder);
    font-size: 30px;
  }

  strong {
    color: var(--el-text-color-regular);
    font-size: 13px;
    font-weight: 500;
  }

  span {
    font-size: 11px;
  }
}

.result-footer {
  display: flex;
  flex: none;
  height: var(--query-footer-height);
  min-height: var(--query-footer-height);
  align-items: center;
  justify-content: space-between;
  padding: 0 12px 0 16px;
  border-top: 1px solid var(--el-border-color-light);
  background: var(--el-bg-color);
  box-sizing: border-box;

  :deep(.el-pagination) {
    --el-pagination-button-height: 26px;
    --el-pagination-button-width: 26px;
    height: 100%;
    align-items: center;
  }
}

.result-hint {
  color: var(--el-text-color-placeholder);
  font-size: 11px;
}

.empty-workspace {
  display: grid;
  flex: 1;
  min-height: 0;
  place-items: center;
  padding: 32px;
  background:
    radial-gradient(circle at 50% 35%, rgba(64, 158, 255, 0.08), transparent 28%),
    #f7f9fc;
}

.empty-workspace__content {
  max-width: 520px;
  text-align: center;

  h2 {
    margin: 18px 0 8px;
    font-size: 20px;
    font-weight: 600;
  }

  p {
    margin: 0;
    color: var(--el-text-color-secondary);
    font-size: 12px;
    line-height: 1.8;
  }
}

.empty-workspace__icon {
  display: inline-flex;
  width: 72px;
  height: 72px;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--el-color-primary-light-7);
  border-radius: 20px;
  color: var(--el-color-primary);
  background: linear-gradient(145deg, #fff, var(--el-color-primary-light-9));
  box-shadow: 0 12px 32px rgba(64, 158, 255, 0.12);

  .el-icon {
    font-size: 34px;
  }
}

.empty-workspace__features {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
  margin-top: 22px;

  span {
    display: inline-flex;
    height: 28px;
    align-items: center;
    gap: 5px;
    padding: 0 10px;
    border: 1px solid var(--el-border-color-light);
    border-radius: 999px;
    color: var(--el-text-color-regular);
    background: rgba(255, 255, 255, 0.8);
    font-size: 11px;
  }
}

@media (max-width: 1100px) {
  .query-header {
    grid-template-columns: auto minmax(300px, 1fr) auto;
  }

  .query-header__title p,
  .dataset-meta {
    display: none;
  }

  .config-panel__head span {
    display: none;
  }
}

@media (max-width: 820px) {
  .query-header {
    grid-template-columns: 1fr auto;
    gap: 10px;
    padding: 10px 12px;
  }

  .query-header__picker {
    grid-column: 1 / -1;
    grid-row: 2;
  }

  .dataset-select {
    flex: 1;
    width: auto;
  }

  .filter-composer :deep(.query-filter-line) {
    flex-wrap: wrap;
  }
}
</style>
