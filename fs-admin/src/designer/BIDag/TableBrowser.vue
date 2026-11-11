<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import DiagramApi from '@/api/bi/DiagramApi'
import ApiUtil from '@/utils/ApiUtil'

const visible = defineModel<boolean>({ default: false })
const props = withDefaults(defineProps<{
  datasourceId?: number | string
  database?: string
  selectedTable?: string
  sqlMode?: boolean
}>(), {
  datasourceId: undefined,
  database: '',
  selectedTable: '',
  sqlMode: false,
})
const emit = defineEmits<{
  select: [table: any, action: 'name' | 'query']
}>()

const keyword = ref('')
const loading = ref(false)
const columnsLoading = ref(false)
const message = ref('')
const tables = ref<any[]>([])
const selected = ref<any>(null)
const columns = ref<any[]>([])
const truncated = ref(false)
const resourceType = ref('TABLE')
const resourceLabel = ref('表')
const supportsColumns = ref(true)
const databaseEnumerationLimited = ref(false)
const databaseNames = ref<string[]>([])

const filteredTables = computed(() => {
  const value = keyword.value.trim().toLowerCase()
  if (!value) return tables.value
  return tables.value.filter((table: any) => {
    return [table.name, table.schema, table.catalog, table.qualifiedName, table.remark]
      .some(item => String(item ?? '').toLowerCase().includes(value))
  })
})

const tablePath = (table: any) => {
  if (!table) return ''
  return [table.catalog, table.schema, table.name]
    .filter((value: any, index: number, values: any[]) =>
      value && values.findIndex(item => String(item) === String(value)) === index)
    .join('.')
}

const resourceKey = (table: any) => tablePath(table) || table?.qualifiedName || table?.name || ''

const resourceTypeLabel = (type: any) => {
  const labels: Record<string, string> = {
    TABLE: '表',
    VIEW: '视图',
    COLLECTION: '集合',
    INDEX: '索引',
    TOPIC: '主题',
  }
  return labels[String(type ?? '').toUpperCase()] || type || resourceLabel.value
}

const dialogTitle = computed(() => props.sqlMode
  ? '获取表名并查看表结构'
  : `选择${resourceLabel.value}`)

const useLabel = computed(() => `使用此${resourceLabel.value}`)

const resourceLevels = (table: any) => {
  if (!table) return []
  return [
    { key: 'catalog', label: '数据库', value: table.catalog },
    { key: 'schema', label: '模式', value: table.schema },
  ].filter((item: any) => item.value)
}

const selectedLevels = computed(() => resourceLevels(selected.value))
const selectedTypeLabel = computed(() => selected.value ? resourceTypeLabel(selected.value.type) : '')

const databaseScopeText = computed(() => {
  if (props.database) return `数据库 ${props.database}`
  if (databaseNames.value.length > 1) return `全部数据库 · ${databaseNames.value.length}`
  if (databaseNames.value.length === 1) return `数据库 ${databaseNames.value[0]}`
  if (resourceType.value === 'INDEX') return '全部索引'
  if (resourceType.value === 'TOPIC') return '全部主题'
  return ''
})

const normalizeTableName = (value: any) => String(value ?? '').replace(/[`"]/g, '').trim().toLowerCase()

const loadColumns = async (table: any) => {
  if (!table || !(Number(props.datasourceId) > 0)) return
  selected.value = table
  if (!supportsColumns.value) {
    columns.value = []
    message.value = ''
    return
  }
  columnsLoading.value = true
  message.value = ''
  columns.value = []
  try {
    const result: any = await DiagramApi.columns({
      datasourceId: Number(props.datasourceId),
      table: table.qualifiedName || table.name,
    }, { success: false })
    const data = ApiUtil.data(result) ?? {}
    columns.value = Array.isArray(data.columns) ? data.columns : []
  } catch (error: any) {
    message.value = ApiUtil.message(error) || '读取表结构失败'
  } finally {
    columnsLoading.value = false
  }
}

const loadTables = async () => {
  if (!(Number(props.datasourceId) > 0)) {
    message.value = '请先选择数据源'
    return
  }
  loading.value = true
  message.value = ''
  databaseEnumerationLimited.value = false
  databaseNames.value = []
  tables.value = []
  selected.value = null
  columns.value = []
  try {
    const result: any = await DiagramApi.tables({
      datasourceId: Number(props.datasourceId),
      ...(props.database ? { database: props.database } : {}),
    }, { success: false })
    const data = ApiUtil.data(result) ?? {}
    resourceType.value = data.resourceType || 'TABLE'
    resourceLabel.value = data.resourceLabel || '表'
    supportsColumns.value = data.supportsColumns !== false
    databaseEnumerationLimited.value = resourceType.value === 'COLLECTION'
      && data.databaseConfigured === false
      && data.databaseEnumerationComplete === false
    databaseNames.value = Array.isArray(data.databaseNames)
      ? data.databaseNames.map((name: any) => String(name)).filter(Boolean)
      : []
    tables.value = Array.isArray(data.tables) ? data.tables : []
    truncated.value = !!data.truncated
    if (!tables.value.length) {
      message.value = `当前数据源下没有可读取的${resourceLabel.value}`
      return
    }
    const current = normalizeTableName(props.selectedTable)
    const matched = current
      ? tables.value.find((table: any) =>
          [resourceKey(table), table.qualifiedName, table.quotedName, table.value, table.name]
            .some(value => normalizeTableName(value) === current))
      : null
    await loadColumns(matched || tables.value[0])
  } catch (error: any) {
    message.value = ApiUtil.message(error) || '读取资源名称失败'
  } finally {
    loading.value = false
  }
}

const useTable = (action: 'name' | 'query') => {
  if (!selected.value) {
    ElMessage.warning(`请先选择一个${resourceLabel.value}`)
    return
  }
  emit('select', selected.value, action)
}

watch(visible, (value) => {
  if (!value) return
  keyword.value = ''
  truncated.value = false
  loadTables()
})
</script>

<template>
  <el-dialog
    v-model="visible"
    class="table-browser-dialog"
    :title="dialogTitle"
    width="min(980px, 96vw)"
    top="5vh"
    append-to-body
    destroy-on-close
    :close-on-click-modal="false"
  >
    <div class="table-browser">
      <section class="table-browser__tables">
        <header>
          <div class="table-browser__table-scope">
            <strong>{{ resourceLabel }}</strong>
            <small v-if="databaseScopeText">{{ databaseScopeText }}</small>
          </div>
          <span>{{ filteredTables.length }} 个</span>
        </header>
        <el-input
          v-model="keyword"
          class="table-browser__search"
          clearable
          :placeholder="`搜索${resourceLabel}、数据库、模式或备注`"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-alert
          v-if="truncated"
          class="table-browser__alert"
          :title="`${resourceLabel}数量较多，当前仅展示前 1000 个；未展示的内容可手动填写`"
          type="warning"
          :closable="false"
          show-icon
        />
        <el-alert
          v-if="databaseEnumerationLimited"
          class="table-browser__alert"
          title="当前 MongoDB 账号无法枚举全部数据库，仅展示已授权的可见库；请为连接账号授予 listDatabases 或 clusterMonitor 权限"
          type="warning"
          :closable="false"
          show-icon
        />
        <el-scrollbar class="table-browser__table-scroll">
          <div v-loading="loading" class="table-browser__table-list">
            <button
              v-for="table in filteredTables"
              :key="resourceKey(table)"
              type="button"
              class="table-browser__table-item"
              :class="{ 'is-selected': resourceKey(selected) === resourceKey(table) }"
              @click="loadColumns(table)"
            >
              <span class="table-browser__table-main">
                <span class="table-browser__table-name" :title="tablePath(table)">{{ table.name }}</span>
                <span class="table-browser__table-type">{{ resourceTypeLabel(table.type) }}</span>
              </span>
              <span v-if="resourceLevels(table).length || table.remark" class="table-browser__table-meta">
                <span
                  v-for="level in resourceLevels(table)"
                  :key="level.key"
                  class="table-browser__level-chip"
                >
                  <em>{{ level.label }}</em>{{ level.value }}
                </span>
                <small v-if="table.remark" :title="table.remark">{{ table.remark }}</small>
              </span>
              <span class="table-browser__table-check" aria-hidden="true">
                <span v-if="resourceKey(selected) === resourceKey(table)">✓</span>
              </span>
            </button>
            <el-empty
              v-if="!loading && !filteredTables.length"
              :description="keyword ? `没有匹配的${resourceLabel}` : (message || `暂无${resourceLabel}`)"
              :image-size="48"
            />
          </div>
        </el-scrollbar>
      </section>

      <section class="table-browser__columns">
        <header class="table-browser__detail-head">
          <div class="table-browser__detail-title">
            <strong :title="selected ? tablePath(selected) : ''">
              {{ selected ? selected.name : (supportsColumns ? '字段结构' : '资源信息') }}
            </strong>
            <span v-if="selected" class="table-browser__detail-type">{{ selectedTypeLabel }}</span>
            <small v-if="supportsColumns">
              {{ selected ? `${columns.length} 个字段` : `选择左侧${resourceLabel}查看字段` }}
            </small>
            <small v-else>
              {{ selected ? '当前资源类型不提供字段结构预览' : `选择左侧${resourceLabel}查看层级信息` }}
            </small>
            <el-button
              class="table-browser__detail-refresh"
              link
              :icon="Refresh"
              :loading="columnsLoading"
              :disabled="!selected"
              @click="loadColumns(selected)"
            >
              刷新
            </el-button>
          </div>
          <div v-if="selectedLevels.length" class="table-browser__detail-meta">
            <span v-for="level in selectedLevels" :key="level.key">
              <em>{{ level.label }}</em>{{ level.value }}
            </span>
          </div>
        </header>
        <div v-if="supportsColumns" class="table-browser__table-shell">
          <el-table
            v-loading="columnsLoading"
            :data="columns"
            height="100%"
            table-layout="fixed"
            size="small"
            border
            stripe
            empty-text="暂无字段"
          >
            <el-table-column prop="index" label="#" width="48" align="center" />
            <el-table-column prop="name" label="字段名" min-width="160" show-overflow-tooltip />
            <el-table-column prop="type" label="类型" width="130" show-overflow-tooltip />
            <el-table-column label="可空" width="64" align="center">
              <template #default="{ row }">{{ row.nullable ? '是' : '否' }}</template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
          </el-table>
        </div>
        <el-empty
          v-else
          class="table-browser__no-columns"
          :description="selected ? `${resourceLabel}暂不支持字段结构预览` : '请选择左侧资源'"
          :image-size="48"
        />
      </section>
    </div>

    <template #footer>
      <div class="table-browser__footer">
        <span v-if="message">{{ message }}</span>
        <div>
          <el-button @click="visible = false">取消</el-button>
          <el-button v-if="sqlMode" :disabled="!selected" @click="useTable('name')">插入表名</el-button>
          <el-button
            type="primary"
            :disabled="!selected"
            @click="useTable(sqlMode ? 'query' : 'name')"
          >
            {{ sqlMode ? '生成查询' : useLabel }}
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<style lang="scss" scoped>
.table-browser {
  display: grid;
  grid-template-columns: minmax(280px, 0.85fr) minmax(440px, 1.45fr);
  gap: 8px;
  height: min(66vh, 620px);
  min-height: 380px;

  &__tables,
  &__columns {
    min-width: 0;
    min-height: 0;
    padding: 8px;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 8px;
    background: var(--el-bg-color);
    display: flex;
    flex-direction: column;
    gap: 6px;
    overflow: hidden;
  }

  &__search {
    :deep(.el-input__wrapper) {
      background: var(--el-fill-color-extra-light);
      box-shadow: 0 0 0 1px var(--el-border-color-lighter) inset;
    }
  }

  &__tables > header {
    min-height: 28px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;

    strong {
      color: var(--el-text-color-primary);
      font-size: 13px;
    }

    span,
    small {
      color: var(--el-text-color-secondary);
      font-size: 12px;
    }

  }

  &__table-scope {
    min-width: 0;
    display: flex;
    align-items: center;
    gap: 6px;
  }

  &__tables > header &__table-scope small {
    max-width: 170px;
    padding: 0 5px;
    overflow: hidden;
    border-radius: 3px;
    color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
    font-size: 10px;
    line-height: 18px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__detail-head {
    min-height: 36px;
    flex-direction: column;
    align-items: stretch;
    justify-content: center;
    gap: 3px;
  }

  &__detail-title {
    min-width: 0;
    display: flex;
    align-items: center;
    gap: 6px;

    strong {
      min-width: 0;
      overflow: hidden;
      font-family: var(--el-font-family-mono, monospace);
      font-size: 13px;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    small {
      flex: none;
      font-size: 10px;
    }
  }

  &__detail-type {
    flex: none;
    padding: 0 5px;
    border-radius: 3px;
    color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
    font-size: 10px;
    line-height: 17px;
  }

  &__detail-refresh {
    flex: none;
    margin-left: auto;
  }

  &__detail-meta {
    min-width: 0;
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 3px 5px;

    > span {
      max-width: 100%;
      padding: 1px 5px;
      overflow: hidden;
      border-radius: 3px;
      color: var(--el-text-color-secondary);
      background: var(--el-fill-color-light);
      font-size: 10px;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    em {
      margin-right: 3px;
      color: var(--el-text-color-placeholder);
      font-style: normal;
    }
  }

  &__alert {
    margin: 0;
  }

  &__table-scroll,
  &__table-shell {
    flex: 1;
    min-height: 0;
    border-radius: 6px;
    background: var(--el-bg-color);
  }

  &__no-columns {
    flex: 1;
    min-height: 220px;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 6px;
    background: var(--el-bg-color);
  }

  &__table-list {
    min-height: 100%;
    padding: 0;
  }

  &__table-item {
    position: relative;
    width: 100%;
    display: flex;
    flex-direction: column;
    gap: 4px;
    margin-bottom: 0;
    padding: 6px 42px 6px 7px;
    border: 0;
    border-bottom: 1px solid var(--el-border-color-extra-light);
    border-radius: 4px;
    color: var(--el-text-color-regular);
    background: transparent;
    text-align: left;
    cursor: pointer;
    transition: border-color 0.15s, background-color 0.15s, box-shadow 0.15s;

    &:hover {
      background: var(--el-fill-color-light);
    }

    &.is-selected {
      color: var(--el-color-primary);
      border-color: transparent;
      background: var(--el-color-primary-light-9);
      box-shadow: inset 3px 0 0 var(--el-color-primary);
    }
  }

  &__table-main {
    min-width: 0;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
  }

  &__table-name {
    min-width: 0;
    overflow: hidden;
    color: var(--el-text-color-primary);
    font-family: var(--el-font-family-mono, monospace);
    font-size: 13px;
    font-weight: 500;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__table-type {
    flex: none;
    padding: 0 5px;
    border-radius: 3px;
    color: var(--el-text-color-secondary);
    background: var(--el-fill-color-dark);
    font-size: 10px;
    line-height: 18px;
  }

  &__table-meta {
    min-width: 0;
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 4px 6px;

    small {
      min-width: 0;
      overflow: hidden;
      color: var(--el-text-color-placeholder);
      font-size: 10px;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  &__level-chip {
    max-width: 100%;
    padding: 1px 5px;
    overflow: hidden;
    border-radius: 3px;
    color: var(--el-text-color-secondary);
    background: var(--el-fill-color);
    font-size: 10px;
    text-overflow: ellipsis;
    white-space: nowrap;

    em {
      margin-right: 3px;
      color: var(--el-text-color-placeholder);
      font-style: normal;
    }
  }

  &__table-check {
    position: absolute;
    top: 50%;
    right: 10px;
    width: 17px;
    height: 17px;
    border: 1px solid var(--el-border-color);
    border-radius: 50%;
    color: #fff;
    background: var(--el-bg-color);
    font-size: 11px;
    line-height: 17px;
    text-align: center;
    transform: translateY(-50%);

    .is-selected & {
      border-color: var(--el-color-primary);
      background: var(--el-color-primary);
    }
  }

  &__footer {
    min-height: 32px;
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 16px;

    > span {
      min-width: 0;
      margin-right: auto;
      overflow: hidden;
      color: var(--el-color-warning);
      font-size: 12px;
      text-align: left;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    > div {
      display: flex;
      flex: none;
      gap: 8px;

      :deep(.el-button) {
        margin-left: 0;
      }
    }
  }
}

:deep(.table-browser__table-shell .el-table) {
  height: 100%;
}

:deep(.table-browser__table-shell .el-table__header-wrapper) {
  flex: none;
}

@media (max-width: 760px) {
  .table-browser {
    height: min(72vh, 700px);
    grid-template-columns: 1fr;
    grid-template-rows: minmax(0, 1fr) minmax(0, 1fr);

    &__tables,
    &__columns {
      min-height: 0;
    }
  }
}
</style>

<style lang="scss">
.table-browser-dialog {
  overflow: hidden;
  border-radius: 10px;
  box-shadow: 0 18px 60px rgb(0 0 0 / 18%);

  .el-dialog__header {
    margin-right: 0;
    padding: 12px 16px 10px;
    border-bottom: 1px solid var(--el-border-color-lighter);
    background: var(--el-bg-color);
  }

  .el-dialog__title {
    color: var(--el-text-color-primary);
    font-size: 15px;
    font-weight: 600;
  }

  .el-dialog__headerbtn {
    top: 12px;
    right: 12px;
    width: 32px;
    height: 32px;
  }

  .el-dialog__body {
    padding: 8px 0;
    background: var(--el-bg-color);
  }

  .el-dialog__footer {
    padding: 10px 16px;
    border-top: 1px solid var(--el-border-color-lighter);
    background: var(--el-bg-color);
  }
}
</style>
