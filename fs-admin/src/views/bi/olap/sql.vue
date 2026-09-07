<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { ElMessage, ElSplitter, ElSplitterPanel } from 'element-plus';
import type { TableInstance } from 'element-plus';
import * as ElementPlusIcons from '@element-plus/icons-vue';
import TableColumnSetting from '@/components/Table/TableColumnSetting.vue';
import LayoutTabs from '@/components/Layout/LayoutTabs.vue';
import OlapApi from '@/api/bi/OlapApi';
import ApiUtil from '@/utils/ApiUtil';
import SqlUtil from '@/utils/SqlUtil';
import FormUtil from '@/utils/FormUtil';
import MenuUtil from '@/utils/MenuUtil';

const treeRef = ref()
const treeKey = ref(0)
const keyword = ref('')

const SQL_STORAGE_KEY = 'bi-olap-sql'

const sql = ref(sessionStorage.getItem(SQL_STORAGE_KEY) || '')
const limit = ref(20)
const timeout = ref(15)
const datasetMode = ref(false)
const executeLoading = ref(false)
const editorRef = ref()
const variableKeyword = ref('')
const config: any = ref({
  ready: false,
  variables: {},
})

const filteredVariables: any = computed(() => {
  const kw = variableKeyword.value.trim().toLowerCase()
  if (!kw) return Object.values(config.value.variables || {})
  return Object.values(config.value.variables || {}).filter((item: any) => (item.text + item.label + item.description).toLowerCase().includes(kw))
})
const insertVariable = (item: any) => {
  editorRef.value?.replaceSelection(item.text)
}

const hints = ref<any[]>([])
const hintKeys = new Set<string>()
const addHint = (text: string, displayText: string, className: string) => {
  if (hintKeys.has(displayText)) return
  hintKeys.add(displayText)
  hints.value.push({ text, displayText, className })
}

const quoteIdentifier = (value: any) => '"' + String(value ?? '').replace(/"/g, '""') + '"'
const qualifyTable = (...values: any[]) => values.map(quoteIdentifier).join('.')

const nodeIcon = (kind: string) => {
  if (kind === 'catalog') return ElementPlusIcons.FolderOpened
  if (kind === 'schema') return ElementPlusIcons.Folder
  if (kind === 'table') return ElementPlusIcons.Grid
  return ElementPlusIcons.Memo
}

const loadNode = (node: any, resolve: any) => {
  if (node.level === 0) {
    OlapApi.catalogs().then((result: any) => {
      resolve(((result.data || {}).catalogs || []).map((item: any) => ({
        key: 'catalog-' + item.name,
        kind: 'catalog',
        name: item.name,
      })))
    }).catch(() => resolve([]))
  } else if (node.data.kind === 'catalog') {
    OlapApi.schemas({ catalog: node.data.name }).then((result: any) => {
      resolve(((result.data || {}).schemas || []).map((item: any) => ({
        key: 'schema-' + node.data.name + '-' + item.name,
        kind: 'schema',
        catalog: node.data.name,
        name: item.name,
      })))
    }).catch(() => resolve([]))
  } else if (node.data.kind === 'schema') {
    OlapApi.tables({ catalog: node.data.catalog, schema: node.data.name }).then((result: any) => {
      const tables = ((result.data || {}).tables || []).map((item: any) => ({
        key: 'table-' + node.data.catalog + '-' + node.data.name + '-' + item.name,
        kind: 'table',
        catalog: node.data.catalog,
        schema: node.data.name,
        name: item.name,
        tableType: item.type,
        remark: item.remark,
      }))
      tables.forEach((item: any) => addHint(
        qualifyTable(node.data.catalog, node.data.name, item.name),
        node.data.catalog + '.' + node.data.name + '.' + item.name,
        'fs-sql-hint-table',
      ))
      resolve(tables)
    }).catch(() => resolve([]))
  } else if (node.data.kind === 'table') {
    OlapApi.columns({ catalog: node.data.catalog, schema: node.data.schema, table: node.data.name }).then((result: any) => {
      const data = result.data || {}
      const columns = (data.columns || [])
        .map((item: any) => ({
          key: 'column-' + node.data.catalog + '-' + node.data.schema + '-' + node.data.name + '-' + item.name,
          kind: 'column',
          catalog: node.data.catalog,
          schema: node.data.schema,
          table: node.data.name,
          name: item.name,
          dataType: item.type,
          nullable: item.nullable,
          remark: item.remark,
          index: item.index,
          leaf: true,
        }))
      columns.forEach((item: any) => addHint(item.name, item.name, 'fs-sql-hint-column'))
      resolve(columns)
    }).catch(() => resolve([]))
  } else {
    resolve([])
  }
}

const filterNode = (value: string, data: any) => {
  if (!value) return true
  return (data.name || '').toLowerCase().includes(value.toLowerCase())
}

const handleRefreshTree = () => {
  hints.value = []
  hintKeys.clear()
  keyword.value = ''
  treeKey.value++
}

watch(keyword, (value) => {
  treeRef.value?.filter(value)
})

const isArrayType = (type?: string) => {
  return !!type && /^array\s*[<(]/i.test(type.trim())
}

const buildSelectFields = (columns: { name: string; type?: string }[]) => {
  const parts: string[] = []
  columns.forEach((column) => {
    const { name, type } = column
    if (isArrayType(type)) {
      parts.push(`array_join(${quoteIdentifier(name)}, ',') AS ${quoteIdentifier(name)}`)
    } else {
      parts.push(quoteIdentifier(name))
    }
  })
  return parts.length ? parts.join(', ') : '*'
}

const loadColumns = async (data: any): Promise<{ name: string; type?: string }[]> => {
  const node = treeRef.value?.getNode(data)
  const children = (node?.childNodes || []).map((n: any) => n.data).filter((n: any) => n && n.name)
  if (children.length) {
    return children.map((n: any) => ({ name: n.name, type: n.dataType }))
  }
  try {
    const result: any = await OlapApi.columns({ catalog: data.catalog, schema: data.schema, table: data.name })
    const res = result.data || {}
    return (res.columns || [])
      .map((item: any) => ({ name: item.name, type: item.type }))
      .filter((column: any) => column.name)
  } catch (e) {
    return []
  }
}

const selectText = (columns: { name: string; type?: string }[], data: any) => {
  return 'SELECT ' + buildSelectFields(columns) + ' FROM ' + qualifyTable(data.catalog, data.schema, data.name)
}

const insertName = (data: any) => {
  const text = data.kind === 'table'
    ? qualifyTable(data.catalog, data.schema, data.name)
    : data.kind === 'column'
      ? data.name
      : quoteIdentifier(data.name)
  editorRef.value?.replaceSelection(text)
}

const generateSql = async (data: any) => {
  if (data.kind === 'table') {
    const columns = await loadColumns(data)
    const text = selectText(columns, data)
    if (!sql.value.trim()) {
      sql.value = text
    } else {
      editorRef.value?.replaceSelection(text)
    }
  } else if (data.kind === 'column') {
    const text = selectText([{ name: data.name, type: data.dataType }], data)
    if (!sql.value.trim()) {
      sql.value = text
    } else {
      editorRef.value?.replaceSelection(text)
    }
  }
}

const refreshNode = (data: any) => {
  const node = treeRef.value?.getNode(data)
  const target = data.kind === 'column' ? node?.parent : node
  if (!target) return
  target.loaded = false
  target.loading = false
  target.childNodes = []
  target.loadData(() => {
    ElMessage.success('节点已刷新')
  })
}

const handleTreeContextMenu = (event: Event, data: any) => {
  if (!data) return
  treeRef.value?.setCurrentNode(data)
  const menus: any[] = [
    { key: 'refresh', icon: 'Refresh', title: '刷新节点' },
    { key: 'insert', icon: 'DocumentAdd', title: '插入名称' },
  ]
  if (data.kind === 'table' || data.kind === 'column') {
    menus.push({ key: 'generate', icon: 'MagicStick', title: '生成SQL' })
  }
  MenuUtil.context(event, menus, (menu: any) => {
    switch (menu.key) {
      case 'refresh':
        return refreshNode(data)
      case 'insert':
        return insertName(data)
      case 'generate':
        return generateSql(data)
      default:
        return false
    }
  })
}

const results = ref<any[]>([])
const activeResult = ref<any>(null)
let resultId = 0

const tableRef = ref<TableInstance>()
const resultKeyword = ref('')
const showSql = ref(false)

const columns = computed({
  get: () => activeResult.value?.columns || [],
  set: (value: any[]) => {
    if (activeResult.value) activeResult.value.columns = value
  },
})

const visibleColumns = computed(() => columns.value.filter((col: any) => !col.hide))

const filteredRows = computed(() => {
  const result = activeResult.value
  if (!result) return []
  const kw = resultKeyword.value.trim().toLowerCase()
  if (!kw) return result.rows
  return result.rows.filter((row: any) =>
    result.columns.some((col: any) => {
      const value = row[col.prop]
      return value !== null && value !== undefined && String(value).toLowerCase().includes(kw)
    })
  )
})

const buildColumns = (cols: any[]) => (cols || []).map((col: any) => ({
  prop: col.name,
  label: col.name,
  type: col.type,
}))

const handleFillSql = () => {
  if (!activeResult.value) return
  sql.value = activeResult.value.sql
}

const exportLoading = ref(false)

const handleExport = (command: string | number | object) => {
  if (!activeResult.value || exportLoading.value) return
  const all = command === 'all'
  exportLoading.value = true
  const url = activeResult.value.dataset
    ? import.meta.env.VITE_APP_API_URL + '/bi/olap/datasetQuery'
    : import.meta.env.VITE_APP_API_URL + '/bi/olap/query'
  const params: any = {
    sql: activeResult.value.sql,
    limit: all ? 10000 : activeResult.value.limit,
    explain: activeResult.value.explain,
    download: true,
  }
  FormUtil.download(url, params)
  ElMessage.success('导出成功')
  setTimeout(() => {
    exportLoading.value = false
  }, 1000)
}

watch(activeResult, () => {
  resultKeyword.value = ''
})

const tabLabel = (tab: any) => {
  const prefix = tab.explain ? '执行计划' : '结果'
  if (tab.error) return prefix + tab.id + ' · 失败'
  return prefix + tab.id + ' · ' + tab.rows.length + ' 行'
}

const handleBeautify = () => {
  try {
    const selected = editorRef.value?.getSelection() || ''
    if (selected.trim()) {
      const formatted = SqlUtil.format(selected, 'trino')
      if (formatted !== selected) editorRef.value?.replaceSelection(formatted)
    } else {
      const formatted = SqlUtil.format(sql.value, 'trino')
      if (formatted !== sql.value) sql.value = formatted
    }
  } catch (e: any) {
    ElMessage.error('SQL 美化失败：' + (e?.message || e))
  }
}

const runQuery = (explain: boolean, selection: boolean) => {
  const selected = selection ? editorRef.value?.getSelection() : ''
  const querySql = (selected && selected.trim()) || sql.value
  if (!querySql.trim()) {
    ElMessage.warning('请输入 SQL 语句')
    return
  }
  if (executeLoading.value) return
  executeLoading.value = true
  const started = Date.now()
  const queryLimit = limit.value
  const queryTimeout = timeout.value
  const request = datasetMode.value ? OlapApi.datasetQuery : OlapApi.query
  request({ sql: querySql, limit: queryLimit, timeout: queryTimeout, explain }).then((result: any) => {
    const data = ApiUtil.data(result) || {}
    const tab: any = {
      id: ++resultId,
      dataset: datasetMode.value,
      sql: data.sql || querySql,
      limit: queryLimit,
      timeout: queryTimeout,
      explain,
      columns: buildColumns(data.columns),
      rows: data.rows || [],
      error: '',
      elapsed: Date.now() - started,
    }
    results.value.push(tab)
    activeResult.value = tab
  }).catch((result: any) => {
    const tab: any = {
      id: ++resultId,
      dataset: datasetMode.value,
      sql: querySql,
      limit: queryLimit,
      timeout: queryTimeout,
      explain,
      columns: [],
      rows: [],
      error: ApiUtil.message(result),
      errorDetail: ApiUtil.data(result),
      elapsed: Date.now() - started,
    }
    results.value.push(tab)
    activeResult.value = tab
  }).finally(() => {
    executeLoading.value = false
  })
}

const handleKeydown = (e: KeyboardEvent) => {
  if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
    e.preventDefault()
    runQuery(false, true)
  }
}

const formatCell = (value: any) => {
  if (value === null || value === undefined) return ''
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

const rowDetailVisible = ref(false)
const rowDetail = ref<any>(null)

const handleRowClick = (row: any) => {
  rowDetail.value = row
  rowDetailVisible.value = true
}

watch(sql, (value) => {
  sessionStorage.setItem(SQL_STORAGE_KEY, value)
})

onMounted(() => {
  document.addEventListener('keydown', handleKeydown)
  OlapApi.config().then((result: any) => {
    Object.assign(config.value, { ready: true }, ApiUtil.data(result))
  }).catch(() => {})
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<template>
  <div class="olap-sql">
    <el-splitter layout="horizontal">
      <el-splitter-panel :size="280" :min="180" :max="520" collapsible>
        <div class="olap-sql__aside">
          <div class="olap-sql__aside-toolbar">
            <el-input v-model="keyword" placeholder="搜索数据对象" clearable :prefix-icon="ElementPlusIcons.Search" />
            <el-button :icon="ElementPlusIcons.Refresh" title="刷新" @click="handleRefreshTree" />
          </div>
          <div class="olap-sql__aside-body">
            <el-scrollbar class="olap-sql__aside-scrollbar">
              <el-tree
                :key="treeKey"
                ref="treeRef"
                class="olap-sql__tree"
                node-key="key"
                :props="{ isLeaf: 'leaf' }"
                lazy
                :load="loadNode"
                :filter-node-method="filterNode"
                :expand-on-click-node="false"
                highlight-current
                @node-contextmenu="handleTreeContextMenu"
              >
                <template #default="{ data }">
                  <span class="olap-sql__tree-node">
                    <el-icon class="olap-sql__tree-node-icon"><component :is="nodeIcon(data.kind)" /></el-icon>
                    <span class="olap-sql__tree-node-label">{{ data.name }}</span>
                    <span v-if="data.kind === 'column'" class="olap-sql__tree-node-type">{{ data.dataType }}</span>
                    <span v-else-if="data.kind === 'table'" class="olap-sql__tree-node-type">{{ data.tableType }}</span>
                  </span>
                </template>
              </el-tree>
            </el-scrollbar>
          </div>
        </div>
      </el-splitter-panel>
      <el-splitter-panel>
        <el-splitter layout="vertical">
          <el-splitter-panel :size="300" :min="120">
            <div class="olap-sql__editor">
              <div class="olap-sql__editor-toolbar flex-between">
                <el-space>
                  <el-button type="primary" :icon="ElementPlusIcons.VideoPlay" :loading="executeLoading" @click="runQuery(false, false)" text>执行查询</el-button>
                  <el-button :icon="ElementPlusIcons.Select" :loading="executeLoading" @click="runQuery(false, true)" text>执行选中</el-button>
                  <el-button :icon="ElementPlusIcons.DataAnalysis" :loading="executeLoading" @click="runQuery(true, true)" text>执行计划</el-button>
                  <el-button :icon="ElementPlusIcons.MagicStick" @click="handleBeautify" text>美化</el-button>
                  <el-button :icon="ElementPlusIcons.Delete" @click="sql = ''" text>清空</el-button>
                  <el-popover placement="bottom-start" :width="260" trigger="click" :popper-style="{ padding: '12px' }">
                    <template #reference>
                      <el-button :icon="ElementPlusIcons.PriceTag" text>变量</el-button>
                    </template>
                    <el-input v-model="variableKeyword" placeholder="搜索变量" clearable size="small" :prefix-icon="ElementPlusIcons.Search" />
                    <el-scrollbar max-height="240px" class="olap-sql__variable-list">
                      <div v-for="item in filteredVariables" key="item.text" class="olap-sql__variable-item" @click="insertVariable(item)">
                        <div class="olap-sql__variable-title">
                          <span class="olap-sql__variable-text">{{ item.text }}</span>
                          <span class="olap-sql__variable-label">{{ item.label }}</span>
                        </div>
                        <div class="olap-sql__variable-desc">{{ item.description }}</div>
                      </div>
                    </el-scrollbar>
                  </el-popover>
                  <data-favorite v-model="sql" type="sql" text @apply="(v: any) => sql = v" v-permit="'member:favorite:'" />
                </el-space>
                <el-space>
                  <el-tooltip content="Ctrl + Enter 查询" placement="bottom">
                    <span class="olap-sql__shortcut">Ctrl+Enter</span>
                  </el-tooltip>
                  <el-popover placement="bottom-end" :width="240" trigger="click" :popper-style="{ padding: '12px 16px' }">
                    <template #reference>
                      <el-button :icon="ElementPlusIcons.Setting" text>配置</el-button>
                    </template>
                    <el-form label-width="70px" size="small" class="olap-sql__config-form" @submit.prevent>
                      <el-form-item label="运行模式">
                        <el-checkbox v-model="datasetMode">仅检索数据集</el-checkbox>
                      </el-form-item>
                      <el-form-item label="记录条数">
                        <form-input-number v-model="limit" :min="1" :max="100000" style="width: 100%" />
                      </el-form-item>
                      <el-form-item label="执行超时">
                        <form-input-number v-model="timeout" :min="0" :max="3600" style="width: 100%" />
                      </el-form-item>
                    </el-form>
                  </el-popover>
                </el-space>
              </div>
              <div class="olap-sql__editor-body">
                <code-editor ref="editorRef" v-model="sql" mode="sql" :hints="hints" fill />
              </div>
            </div>
          </el-splitter-panel>
          <el-splitter-panel>
            <div class="olap-sql__result">
              <div class="olap-sql__result-tabs" v-if="results.length">
                <layout-tabs v-model="activeResult" :tabs="results" :label="tabLabel" />
              </div>
              <div class="olap-sql__result-body">
                <template v-if="activeResult && !activeResult.error">
                  <div class="olap-sql__result-meta flex-between">
                    <el-space :size="16">
                      <el-switch v-model="showSql" inline-prompt active-text="展示SQL" inactive-text="隐藏SQL" />
                      <span>共 {{ activeResult.rows.length }} 行</span>
                      <span>耗时 {{ activeResult.elapsed }} ms</span>
                      <span>LIMIT {{ activeResult.limit }}</span>
                    </el-space>
                    <el-space>
                      <el-input
                        v-model="resultKeyword"
                        placeholder="过滤结果"
                        clearable
                        size="small"
                        :prefix-icon="ElementPlusIcons.Search"
                        style="width: 180px"
                      />
                      <el-button size="small" :icon="ElementPlusIcons.Back" @click="handleFillSql" text>回填SQL</el-button>
                      <el-dropdown trigger="click" @command="handleExport">
                        <el-button size="small" :icon="ElementPlusIcons.Download" :loading="exportLoading" text>导出</el-button>
                        <template #dropdown>
                          <el-dropdown-menu>
                            <el-dropdown-item command="current">导出当前结果</el-dropdown-item>
                            <el-dropdown-item command="all">导出全部结果</el-dropdown-item>
                          </el-dropdown-menu>
                        </template>
                      </el-dropdown>
                      <TableColumnSetting v-model="columns" :table="tableRef" :key="activeResult.id" text />
                    </el-space>
                  </div>
                </template>
                <div class="olap-sql__result-table">
                  <el-alert
                    v-if="activeResult && activeResult.error"
                    :title="activeResult.error"
                    :description="activeResult.errorDetail"
                    type="error"
                    show-icon
                    :closable="false"
                  />
                  <div v-if="activeResult && (showSql || activeResult.error)" class="olap-sql__result-sql">{{ activeResult.sql }}</div>
                  <el-table
                    ref="tableRef"
                    v-if="activeResult && !activeResult.error"
                    :class="{ 'olap-sql__table--flush': !showSql }"
                    :data="filteredRows"
                    border
                    size="small"
                    :empty-text="resultKeyword ? '无匹配数据' : '暂无数据'"
                    @row-dblclick="handleRowClick"
                  >
                    <el-table-column
                      v-for="col in visibleColumns"
                      :key="col.prop"
                      :prop="col.prop"
                      :fixed="col.fixed"
                      :min-width="140"
                      :show-overflow-tooltip="!activeResult.explain"
                    >
                      <template #header>
                        <div class="olap-sql__cell-head">
                          <span class="olap-sql__cell-head-name">{{ col.label }}</span>
                          <span class="olap-sql__cell-head-type">{{ col.type }}</span>
                        </div>
                      </template>
                      <template #default="scope">
                        <span :class="{ 'olap-sql__cell-explain': activeResult.explain }">{{ formatCell(scope.row[col.prop]) }}</span>
                      </template>
                    </el-table-column>
                  </el-table>
                  <el-empty v-if="!activeResult" description="在编辑器中输入 SQL 后点击「查询」查看结果" />
                </div>
              </div>
            </div>
          </el-splitter-panel>
        </el-splitter>
      </el-splitter-panel>
    </el-splitter>
    <el-drawer v-model="rowDetailVisible" title="行数据" size="50%" append-to-body>
      <el-descriptions v-if="rowDetail" :column="1" border size="small">
        <el-descriptions-item v-for="col in visibleColumns" :key="col.prop" :label="col.label">
          <span class="olap-sql__cell-value">{{ formatCell(rowDetail[col.prop]) }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<style lang="scss" scoped>
.olap-sql {
  height: 100%;
  overflow: hidden;

  :deep(.el-splitter-panel) {
    overflow: hidden;
  }
}

.olap-sql__aside {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.olap-sql__aside-toolbar {
  flex: none;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  border-bottom: 1px solid var(--el-border-color-light);
}

.olap-sql__aside-body {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.olap-sql__aside-scrollbar {
  height: 100%;

  :deep(.el-scrollbar__view) {
    padding: 8px;
  }
}

.olap-sql__tree {
  :deep(.el-tree-node__content) {
    height: 28px;
  }
}

.olap-sql__tree-node {
  display: flex;
  align-items: center;
  gap: 4px;
  width: 100%;
  min-width: 0;
}

.olap-sql__tree-node-icon {
  flex: none;
  color: var(--el-color-primary);
}

.olap-sql__tree-node-label {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.olap-sql__tree-node-type {
  flex: none;
  max-width: 45%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.olap-sql__editor {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.olap-sql__editor-toolbar {
  flex: none;
  padding: 6px 10px;
  border-bottom: 1px solid var(--el-border-color-light);
}

.olap-sql__editor-body {
  flex: 1;
  min-height: 0;
}

.olap-sql__variable-list {
  margin-top: 8px;

  :deep(.el-scrollbar__view) {
    padding-right: 4px;
  }
}

.olap-sql__variable-item {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 8px 10px;
  border-radius: 4px;
  cursor: pointer;

  &:hover {
    background: var(--el-fill-color-light);
  }
}

.olap-sql__variable-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  line-height: 1.2;
}

.olap-sql__variable-text {
  flex: none;
  font-family: monospace;
  font-size: 13px;
  color: var(--el-color-primary);
}

.olap-sql__variable-label {
  flex: none;
  font-size: 13px;
  color: var(--el-text-color-primary);
}

.olap-sql__variable-desc {
  font-size: 12px;
  line-height: 1.4;
  color: var(--el-text-color-secondary);
}

.olap-sql__config-form {
  :deep(.el-form-item) {
    margin-bottom: 12px;

    &:last-child {
      margin-bottom: 0;
    }
  }
}

.olap-sql__limit-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.olap-sql__shortcut {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  cursor: default;
}

.olap-sql__result {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
  margin-top: 5px;
}

.olap-sql__result-tabs {
  flex: none;
}

.olap-sql__result-body {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.olap-sql__result-meta {
  flex: none;
  position: sticky;
  top: 0;
  z-index: 10;
  background: var(--el-bg-color);
  padding: 6px 12px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.olap-sql__result-meta-sql-switch {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.olap-sql__result-sql {
  flex: none;
  padding: 6px 12px;
  font-family: monospace;
  font-size: 12px;
  color: var(--el-text-color-regular);
  white-space: pre-wrap;
  word-break: break-all;
}

.olap-sql__result-table {
  width: 100%;
  padding-bottom: 3px;
}

.olap-sql__table--flush {
  margin-top: -1px;
}

.olap-sql__cell-head {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.olap-sql__cell-head-name {
  font-weight: 500;
}

.olap-sql__cell-head-type {
  font-size: 12px;
  font-weight: normal;
  color: var(--el-text-color-secondary);
}

.olap-sql__cell-explain {
  white-space: pre-wrap;
  word-break: break-all;
}

.olap-sql__cell-value {
  white-space: pre-wrap;
  word-break: break-all;
}

.olap-sql__result-table {
  :deep(.el-table) {
    overflow: visible;
  }

  :deep(.el-table__header-wrapper) {
    position: sticky;
    top: 45px;
    z-index: 5;
    background: var(--el-table-header-bg-color);
  }

  :deep(.el-table__row) {
    cursor: pointer;
  }
}
</style>
