<script setup lang="ts">
/**
 * 基于 Trino Catalog / Schema 表元数据批量创建直连数据集。
 * 逐表读取字段注释并调用数据集保存接口，冲突项自动跳过，失败项保留在页面中供修改重试。
 */
import { computed, nextTick, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Close, DataLine, Refresh } from '@element-plus/icons-vue'
import DatasetApi from '@/api/bi/DatasetApi'
import OlapApi from '@/api/bi/OlapApi'
import RoleApi from '@/api/member/RoleApi'
import ApiUtil from '@/utils/ApiUtil'

type BatchStatus = 'pending' | 'running' | 'success' | 'failed' | 'skipped'
type ConflictMode = 'skip' | 'overwrite'

interface BatchTable {
  key: string
  table: string
  tableType: string
  datasetName: string
  manualName: boolean
  description: string
  sql: string
  selected: boolean
  status: BatchStatus
  message: string
  fieldsCount: number
  pks: string[]
}

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  refresh: []
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => {
    if (creating.value && !value) {
      ElMessage.warning('批量创建正在执行，请等待当前任务完成')
      return
    }
    emit('update:modelValue', value)
  },
})

const catalogs = ref<string[]>([])
const schemas = ref<string[]>([])
const catalog = ref('')
const schema = ref('')
const prefix = ref('')
const rows = ref<BatchTable[]>([])
const tableKeyword = ref('')
const catalogLoading = ref(false)
const schemaLoading = ref(false)
const tableLoading = ref(false)
const creating = ref(false)
const runningAction = ref<'' | 'create' | 'retry'>('')
const runTotal = ref(0)
const runFinished = ref(0)
const conflictMode = ref<ConflictMode>('skip')
const shared = ref({
  status: 1,
  roleIds: [] as number[],
  labels: [] as string[],
  sort: 0,
})
let initialized = false

const rowStatusOptions: Record<BatchStatus, { text: string, type: 'info' | 'primary' | 'success' | 'warning' | 'danger' }> = {
  pending: { text: '待创建', type: 'info' },
  running: { text: '创建中', type: 'primary' },
  success: { text: '已创建', type: 'success' },
  failed: { text: '失败', type: 'danger' },
  skipped: { text: '已跳过', type: 'warning' },
}
const datasetStatusOptions = [
  { value: 1, label: '启用' },
  { value: 2, label: '禁用' },
]
const conflictOptions: { value: ConflictMode, label: string, description: string }[] = [
  { value: 'skip', label: '自动跳过', description: '名称已存在时不修改源数据集' },
  { value: 'overwrite', label: '覆盖已有', description: '按名称更新现有数据集及其视图' },
]

const quoteIdentifier = (value: any) => '"' + String(value ?? '').replace(/"/g, '""') + '"'
const qualifyTable = (...values: any[]) => values.map(quoteIdentifier).join('.')

/** 将表名转换为小写下划线形式，并规避首字符为数字的非法数据集名称。 */
const toSnake = (value: string) => {
  const result = String(value ?? '')
    .replace(/([a-z0-9])([A-Z])/g, '$1_$2')
    .replace(/([A-Z]+)([A-Z][a-z])/g, '$1_$2')
    .toLowerCase()
    .replace(/[^a-z0-9_]+/g, '_')
    .replace(/_+/g, '_')
  return /^[0-9]/.test(result) ? '_' + result : result
}

const defaultDatasetName = (table: string) => {
  const normalizedPrefix = toSnake(prefix.value)
  return normalizedPrefix + toSnake(table)
}

const filteredRows = computed(() => {
  const keyword = tableKeyword.value.trim().toLowerCase()
  if (!keyword) return rows.value
  return rows.value.filter((row) =>
    row.table.toLowerCase().includes(keyword)
    || row.datasetName.toLowerCase().includes(keyword)
    || row.description.toLowerCase().includes(keyword))
})

const counts = computed(() => {
  const result = { pending: 0, running: 0, success: 0, failed: 0, skipped: 0, selected: 0 }
  rows.value.forEach((row) => {
    result[row.status]++
    if (row.selected) result.selected++
  })
  return result
})

const progress = computed(() => runTotal.value
  ? Math.min(100, Math.round(runFinished.value / runTotal.value * 100))
  : 0)

const progressStatus = computed(() => {
  if (creating.value) return undefined
  if (counts.value.failed > 0) return 'exception'
  if (counts.value.skipped > 0) return 'warning'
  if (runTotal.value > 0 && runFinished.value >= runTotal.value) return 'success'
  return undefined
})

const conflictTip = computed(() => {
  const description = conflictOptions.find((item) => item.value === conflictMode.value)?.description || ''
  return conflictMode.value === 'overwrite' ? description + '；需要数据集修改权限' : description
})

const allFilteredSelected = computed(() =>
  filteredRows.value.length > 0 && filteredRows.value.every((row) => row.selected))

const pendingSelected = computed(() =>
  rows.value.filter((row) => row.selected && row.status === 'pending'))

const retryableSelected = computed(() =>
  rows.value.filter((row) => row.selected && (row.status === 'failed' || row.status === 'skipped')))

const loadCatalogs = () => {
  if (catalogLoading.value) return
  catalogLoading.value = true
  OlapApi.catalogs({}, { warning: false }).then((result: any) => {
    catalogs.value = ApiUtil.data(result)?.catalogs?.map((item: any) => item.name) || []
  }).catch(() => {}).finally(() => {
    catalogLoading.value = false
  })
}

const handleCatalogChange = () => {
  schema.value = ''
  schemas.value = []
  rows.value = []
  tableKeyword.value = ''
  if (!catalog.value) return
  schemaLoading.value = true
  OlapApi.schemas({ catalog: catalog.value }, { warning: false }).then((result: any) => {
    schemas.value = ApiUtil.data(result)?.schemas?.map((item: any) => item.name) || []
  }).catch(() => {}).finally(() => {
    schemaLoading.value = false
  })
}

const handleSchemaChange = () => {
  rows.value = []
  tableKeyword.value = ''
}

const loadTables = () => {
  if (tableLoading.value || creating.value) return
  if (!catalog.value || !schema.value) {
    ElMessage.warning('请先选择 Catalog 和 Schema')
    return
  }
  tableLoading.value = true
  rows.value = []
  runTotal.value = 0
  runFinished.value = 0
  OlapApi.tables({ catalog: catalog.value, schema: schema.value }, { warning: false }).then((result: any) => {
    const tables = ApiUtil.data(result)?.tables || []
    rows.value = tables.map((item: any, index: number) => {
      const sql = 'SELECT * FROM ' + qualifyTable(catalog.value, schema.value, item.name)
      return {
        key: catalog.value + '.' + schema.value + '.' + item.name + ':' + index,
        table: item.name,
        tableType: item.type || '',
        datasetName: defaultDatasetName(item.name),
        manualName: false,
        description: item.remark || '',
        sql,
        selected: false,
        status: 'pending',
        message: '',
        fieldsCount: 0,
        pks: [],
      } as BatchTable
    })
  }).catch(() => {}).finally(() => {
    tableLoading.value = false
  })
}

const toggleAllFiltered = () => {
  const selected = !allFilteredSelected.value
  filteredRows.value.forEach((row) => {
    if (!['success', 'running'].includes(row.status)) row.selected = selected
  })
}

const handleNameInput = (row: any) => {
  row.manualName = true
}

const resetName = (row: any) => {
  row.manualName = false
  row.datasetName = defaultDatasetName(row.table)
}

const fieldsOf = (columns: any[]) => columns.map((item: any) => ({
  name: item.name,
  title: item.name,
  type: item.type || '',
  comment: item.remark || '',
}))

const datasetPayload = (row: BatchTable, fields: any[], pks: string[], id = 0) => ({
  id: id > 0 ? id : undefined,
  name: String(row.datasetName).trim(),
  type: 'direct',
  expression: '',
  content: row.sql,
  pks: pks.slice(),
  partitions: [],
  fields,
  labels: shared.value.labels.slice(),
  roleIds: shared.value.roleIds.slice(),
  sort: Number(shared.value.sort) || 0,
  status: Number(shared.value.status) || 1,
  description: row.description || '',
})

const findExistingId = async (name: string) => {
  const result = await DatasetApi.byName(name, { warning: false, error: false, success: false })
  return Number(ApiUtil.data(result)?.id) || 0
}

/**
 * 按冲突策略保存单条数据集。
 * 覆盖模式先解析已有记录 ID；若并发过程中仍触发唯一键冲突，则重新解析后再更新一次。
 */
const saveDataset = async (row: BatchTable, fields: any[], pks: string[]) => {
  const name = String(row.datasetName).trim()
  if (conflictMode.value === 'skip') {
    await DatasetApi.save(datasetPayload(row, fields, pks), { warning: false, error: false, success: false })
    return false
  }
  let existingId = await findExistingId(name)
  try {
    await DatasetApi.save(datasetPayload(row, fields, pks, existingId), { warning: false, error: false, success: false })
    return existingId > 0
  } catch (error: any) {
    if (ApiUtil.code(error) !== 1501) throw error
    existingId = await findExistingId(name)
    if (!existingId) throw error
    await DatasetApi.save(datasetPayload(row, fields, pks, existingId), { warning: false, error: false, success: false })
    return true
  }
}

const createDatasets = async (targets: BatchTable[], action: 'create' | 'retry') => {
  if (creating.value) return
  if (!targets.length) {
    ElMessage.warning(action === 'retry' ? '没有勾选可重试的数据集' : '没有勾选待创建的数据集')
    return
  }
  creating.value = true
  runningAction.value = action
  runTotal.value = targets.length
  runFinished.value = 0
  let success = 0
  let skipped = 0
  let failed = 0
  try {
    for (const row of targets) {
      row.status = 'running'
      row.message = '正在读取字段及注释'
      await nextTick()
      try {
        if (!String(row.datasetName ?? '').trim()) {
          throw ApiUtil.result(1001, '数据集名称不能为空', row.datasetName)
        }
        const columnsResult = await OlapApi.columns({
          catalog: catalog.value,
          schema: schema.value,
          table: row.table,
        }, { warning: false })
        const metadata = ApiUtil.data(columnsResult) || {}
        const columns = metadata.columns || []
        if (!columns.length) throw ApiUtil.result(1004, '未读取到表字段', row.table)
        row.fieldsCount = columns.length
        row.pks = (metadata.pks || []).map((item: any) => String(item))
        row.message = conflictMode.value === 'overwrite'
          ? '正在检查名称冲突并更新数据集'
          : '正在创建直连数据集'
        await nextTick()
        const overwritten = await saveDataset(row, fieldsOf(columns), row.pks)
        row.status = 'success'
        row.message = overwritten ? '覆盖更新成功' : '创建成功'
        success++
      } catch (error: any) {
        if (ApiUtil.code(error) === 1501) {
          row.status = 'skipped'
          row.message = '名称已存在，已自动跳过'
          skipped++
        } else {
          row.status = 'failed'
          row.message = ApiUtil.message(error) || '创建失败'
          failed++
        }
      } finally {
        runFinished.value++
        await nextTick()
      }
    }
    if (success > 0) emit('refresh')
    const summary = `创建完成：成功 ${success} 个，跳过 ${skipped} 个，失败 ${failed} 个`
    if (failed > 0) {
      ElMessage.warning(summary + '。可修改失败项后重试')
    } else {
      ElMessage.success(summary)
    }
  } finally {
    creating.value = false
    runningAction.value = ''
  }
}

const handleCreate = () => createDatasets(pendingSelected.value, 'create')
const handleRetry = () => createDatasets(retryableSelected.value, 'retry')

const initialize = () => {
  catalog.value = ''
  schema.value = ''
  prefix.value = ''
  schemas.value = []
  rows.value = []
  tableKeyword.value = ''
  runTotal.value = 0
  runFinished.value = 0
  if (initialized && catalogs.value.length) return
  initialized = true
  loadCatalogs()
}

watch(() => props.modelValue, (value) => {
  if (value) initialize()
})

watch(prefix, () => {
  rows.value.forEach((row) => {
    if (row.status === 'pending' && !row.manualName) {
      row.datasetName = defaultDatasetName(row.table)
    }
  })
})
</script>

<template>
  <el-drawer
    v-model="visible"
    class="batch-drawer"
    :close-on-click-modal="false"
    :close-on-press-escape="!creating"
    :show-close="false"
    :destroy-on-close="false"
    size="88%"
  >
    <template #header="{ titleId, titleClass }">
      <div class="batch-header">
        <div class="batch-title">
          <span class="title-icon"><el-icon><DataLine /></el-icon></span>
          <div>
            <div class="title-line">
              <h4 :id="titleId" :class="titleClass">批量创建数据集</h4>
              <el-tag :type="creating ? 'warning' : 'info'" size="small" effect="dark">
                {{ creating ? '执行中' : '直连模式' }}
              </el-tag>
            </div>
          </div>
        </div>
        <el-button
          class="close-button"
          circle
          text
          :icon="Close"
          :disabled="creating"
          aria-label="关闭"
          @click="visible = false"
        />
      </div>
    </template>

    <div class="batch-content">
      <section class="batch-workspace">
        <div class="workspace-section">
          <el-form label-position="left" label-width="58px" @submit.prevent>
            <div class="source-grid">
              <el-form-item label="Catalog">
                <el-select
                  v-model="catalog"
                  :loading="catalogLoading"
                  :disabled="creating || tableLoading"
                  filterable
                  placeholder="选择 Catalog"
                  class="w-full"
                  @change="handleCatalogChange"
                >
                  <el-option v-for="item in catalogs" :key="item" :value="item" :label="item" />
                </el-select>
              </el-form-item>
              <el-form-item label="Schema">
                <el-select
                  v-model="schema"
                  :loading="schemaLoading"
                  :disabled="creating || tableLoading"
                  filterable
                  placeholder="选择 Schema"
                  class="w-full"
                  @change="handleSchemaChange"
                >
                  <el-option v-for="item in schemas" :key="item" :value="item" :label="item" />
                </el-select>
              </el-form-item>
              <el-form-item label="前缀">
                <el-input
                  v-model="prefix"
                  :disabled="creating || tableLoading"
                  placeholder="可选，例如 bi_"
                  title="前缀直接拼接表名，不会自动补下划线"
                  clearable
                />
              </el-form-item>
              <el-form-item label="表名筛选">
                <el-input v-model="tableKeyword" placeholder="输入表名或数据集名" clearable />
              </el-form-item>
              <el-form-item label="操作">
                <el-button
                  type="primary"
                  plain
                  class="w-full load-source-button"
                  :icon="Refresh"
                  :loading="tableLoading"
                  :disabled="creating"
                  @click="loadTables"
                >加载源表</el-button>
              </el-form-item>
            </div>
          </el-form>
        </div>

        <div class="workspace-divider" />

        <div class="workspace-section">
          <el-form label-position="left" label-width="58px" @submit.prevent>
            <div class="options-grid">
              <el-form-item label="状态">
                <el-select v-model="shared.status" :disabled="creating" class="w-full">
                  <el-option
                    v-for="item in datasetStatusOptions"
                    :key="item.value"
                    :value="item.value"
                    :label="item.label"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="授权角色">
                <form-select
                  v-model="shared.roleIds"
                  :callback="RoleApi.list"
                  :disabled="creating"
                  multiple
                  clearable
                  placeholder="留空表示不限制"
                />
              </el-form-item>
              <el-form-item label="标签">
                <el-select
                  v-model="shared.labels"
                  :disabled="creating"
                  multiple
                  filterable
                  allow-create
                  default-first-option
                  :reserve-keyword="false"
                  placeholder="输入后回车创建标签"
                  class="w-full"
                />
              </el-form-item>
              <el-form-item label="排序">
                <form-input-number v-model="shared.sort" :disabled="creating" />
              </el-form-item>
              <el-form-item label="冲突" class="conflict-field">
                <el-radio-group v-model="conflictMode" :disabled="creating" :title="conflictTip">
                  <el-radio-button v-for="item in conflictOptions" :key="item.value" :value="item.value">
                    {{ item.label }}
                  </el-radio-button>
                </el-radio-group>
              </el-form-item>
            </div>
          </el-form>
        </div>

        <div v-if="runTotal" class="progress-strip">
          <div class="progress-heading">
            <div>
              <strong>{{ creating ? '正在创建数据集' : '最近一次执行结果' }}</strong>
              <span>{{ runFinished }} / {{ runTotal }}</span>
            </div>
            <el-space :size="6">
              <el-tag type="success" size="small" effect="plain">成功 {{ counts.success }}</el-tag>
              <el-tag type="warning" size="small" effect="plain">跳过 {{ counts.skipped }}</el-tag>
              <el-tag type="danger" size="small" effect="plain">失败 {{ counts.failed }}</el-tag>
            </el-space>
          </div>
          <el-progress :percentage="progress" :status="progressStatus" :stroke-width="8" />
        </div>

        <div class="workspace-footer">
          <div class="selection-summary">
            <span class="summary-count">已选 {{ counts.selected }} 项</span>
            <span v-if="rows.length">共 {{ rows.length }} 张源表，待创建 {{ counts.pending }} 项</span>
            <span v-else>加载源表后即可开始创建</span>
          </div>
          <div class="footer-actions">
            <el-button
              :loading="runningAction === 'retry'"
              :disabled="creating || !retryableSelected.length"
              @click="handleRetry"
            >重试所选（{{ retryableSelected.length }}）</el-button>
            <el-button
              type="primary"
              :loading="runningAction === 'create'"
              :disabled="creating || !pendingSelected.length"
              @click="handleCreate"
            >创建所选（{{ pendingSelected.length }}）</el-button>
          </div>
        </div>
      </section>

      <section class="table-area">
        <div class="table-heading">
          <div>
            <strong>源表清单</strong>
            <span v-if="rows.length">{{ filteredRows.length }} / {{ rows.length }} 张表</span>
            <span v-else>尚未加载</span>
          </div>
          <el-checkbox
            v-if="rows.length"
            :model-value="allFilteredSelected"
            :indeterminate="filteredRows.some((row) => row.selected) && !allFilteredSelected"
            :disabled="creating"
            @change="toggleAllFiltered"
          >全选当前筛选</el-checkbox>
        </div>

        <div v-if="rows.length" class="table-shell">
          <el-table
            :data="filteredRows"
            row-key="key"
            border
            table-layout="fixed"
            height="100%"
            v-loading="tableLoading"
          >
            <el-table-column type="expand" width="44">
              <template #default="scope">
                <div class="expand-panel">
                  <div class="expand-meta">
                    <span>源表：{{ catalog }}.{{ schema }}.{{ scope.row.table }}</span>
                    <span>已读取字段：{{ scope.row.fieldsCount || '尚未读取' }}</span>
                  </div>
                  <el-input
                    v-model="scope.row.sql"
                    type="textarea"
                    :rows="4"
                    :disabled="creating || scope.row.status === 'success'"
                    placeholder="数据集查询语句"
                  />
                </div>
              </template>
            </el-table-column>
            <el-table-column width="58" align="center">
              <template #header><span>选择</span></template>
              <template #default="scope">
                <el-checkbox
                  v-model="scope.row.selected"
                  :disabled="creating || ['success', 'running'].includes(scope.row.status)"
                />
              </template>
            </el-table-column>
            <el-table-column prop="table" label="源表" min-width="150" show-overflow-tooltip>
              <template #default="scope">
                <div class="table-name">
                  <span>{{ scope.row.table }}</span>
                  <el-tag v-if="scope.row.tableType" size="small" effect="plain">{{ scope.row.tableType }}</el-tag>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="数据集名称" min-width="210">
              <template #default="scope">
                <div class="name-editor">
                  <el-input
                    v-model="scope.row.datasetName"
                    :disabled="creating || scope.row.status === 'success'"
                    @input="handleNameInput(scope.row)"
                  />
                  <el-button
                    v-if="scope.row.manualName && !['success', 'running'].includes(scope.row.status)"
                    link
                    type="primary"
                    :disabled="creating"
                    @click="resetName(scope.row)"
                  >重置</el-button>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="描述" min-width="180">
              <template #default="scope">
                <el-input
                  v-model="scope.row.description"
                  :disabled="creating || scope.row.status === 'success'"
                  placeholder="取自表注释"
                />
              </template>
            </el-table-column>
            <el-table-column label="主键" min-width="150">
              <template #default="scope">
                <el-space v-if="scope.row.pks.length" :size="4" wrap>
                  <el-tag v-for="item in scope.row.pks" :key="item" size="small" effect="plain">
                    {{ item }}
                  </el-tag>
                </el-space>
                <span v-else class="muted-text">
                  {{ scope.row.fieldsCount ? '未设置' : '创建后读取' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="140">
              <template #default="scope">
                <div class="status-cell">
                  <el-tag :type="rowStatusOptions[scope.row.status as BatchStatus].type" effect="plain">
                    {{ rowStatusOptions[scope.row.status as BatchStatus].text }}
                  </el-tag>
                  <span v-if="scope.row.message" :class="{ 'is-error': scope.row.status === 'failed' }">
                    {{ scope.row.message }}
                  </span>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div v-else class="empty-state">
          <el-empty
            :description="tableLoading ? '正在读取源表元数据' : '选择 Catalog 和 Schema，然后加载源表'"
            :image-size="86"
          />
        </div>
      </section>
    </div>
  </el-drawer>
</template>

<style lang="scss" scoped>
:global(.batch-drawer .el-drawer__header) {
  margin: 0;
  padding: 10px 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: var(--el-bg-color);
}

:global(.batch-drawer .el-drawer__body) {
  display: flex;
  padding: 0;
  min-height: 0;
  overflow: hidden;
  background:
    linear-gradient(180deg, var(--el-fill-color-lighter) 0, var(--el-fill-color-lighter) 100%);
}

.batch-content {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  min-height: 0;
  padding: 10px 12px 12px;
  overflow: hidden;
}

.batch-drawer {
  :deep(.el-drawer__header) {
    margin-bottom: 0;
  }
}

.batch-header {
  @include flex-between();
  gap: 16px;
  width: 100%;
  padding-right: 2px;
}

.batch-title {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;

  .title-icon {
    @include flex-center();
    flex: none;
    width: 30px;
    height: 30px;
    border-radius: 8px;
    color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
    font-size: 16px;
  }

  .title-line {
    display: flex;
    align-items: center;
    gap: 9px;

    h4 {
      margin: 0;
      font-size: 16px;
      line-height: 1.35;
    }
  }
}

.close-button {
  flex: none;
  color: var(--el-text-color-secondary);

  &:hover {
    color: var(--el-text-color-primary);
  }
}

.batch-workspace,
.table-area {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
  background: var(--el-bg-color);
  box-shadow: 0 2px 10px rgb(0 0 0 / 3%);
}

.batch-workspace {
  flex: none;
  margin-bottom: 10px;
  padding: 11px 14px 0;
  overflow: hidden;

  :deep(.el-form-item) {
    margin-bottom: 0;
  }

  :deep(.el-form-item__label) {
    align-items: center;
    padding: 0 8px 0 0;
    color: var(--el-text-color-regular);
    font-size: 11px;
    line-height: 32px;
  }

  :deep(.el-form-item__content) {
    min-width: 0;
  }
}

.workspace-section {
  padding-bottom: 6px;
}

.source-grid {
  display: grid;
  grid-template-columns: minmax(190px, 1fr) minmax(190px, 1fr) minmax(160px, 0.8fr) minmax(210px, 1.2fr) 190px;
  gap: 8px 10px;
  align-items: center;
}

.load-source-button {
  min-width: 0;
  padding-right: 16px;
  padding-left: 16px;
  white-space: nowrap;
}

.options-grid {
  display: grid;
  grid-template-columns: 150px minmax(240px, 1.2fr) minmax(200px, 1fr) 200px minmax(260px, 1.4fr);
  gap: 8px 10px;
  align-items: center;
}

.source-grid > .el-form-item,
.options-grid > .el-form-item {
  min-width: 0;
}

.options-grid {
  :deep(.el-input-number) {
    width: 100%;
    min-width: 0;
  }
}

.conflict-field {
  :deep(.el-form-item__content) {
    flex: 1;
  }

  :deep(.el-radio-group) {
    display: flex;
    width: 100%;
  }

  :deep(.el-radio-button) {
    flex: 1;
  }

  :deep(.el-radio-button__inner) {
    width: 100%;
    padding-right: 10px;
    padding-left: 10px;
  }
}

.workspace-divider {
  height: 1px;
  margin: 0 -14px 6px;
  background: var(--el-border-color-lighter);
}

.progress-strip {
  margin: 0 -14px;
  padding: 8px 14px;
  border-top: 1px solid var(--el-border-color-lighter);
  background: var(--el-color-primary-light-9);

  .progress-heading {
    @include flex-between();
    gap: 12px;
    margin-bottom: 5px;

    > div {
      display: flex;
      gap: 9px;
      align-items: baseline;
    }

    strong {
      font-size: 13px;
    }

    span {
      color: var(--el-text-color-secondary);
      font-size: 12px;
    }
  }

  :deep(.el-progress__text) {
    min-width: 38px;
    font-size: 12px !important;
  }
}

.workspace-footer {
  @include flex-between();
  gap: 16px;
  min-height: 44px;
  margin: 0 -14px;
  padding: 6px 14px;
  border-top: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-blank);

  .selection-summary {
    display: flex;
    align-items: center;
    gap: 10px;
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }

  .summary-count {
    padding: 4px 9px;
    border-radius: 999px;
    color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
    font-weight: 600;
  }

  .footer-actions {
    display: flex;
    gap: 8px;

    :deep(.el-button) {
      height: 30px;
      padding: 0 12px;
    }
  }
}

.table-area {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  padding: 10px 16px 14px;
}

.table-heading {
  @include flex-between();
  flex: none;
  gap: 16px;
  min-height: 26px;
  margin: 0 4px 8px;

  > div {
    display: flex;
    align-items: baseline;
    gap: 8px;
  }

  strong {
    font-size: 14px;
  }

  span {
    padding: 2px 8px;
    border-radius: 999px;
    color: var(--el-text-color-secondary);
    background: var(--el-fill-color-lighter);
    font-size: 11px;
  }
}

.table-shell {
  flex: 1;
  min-height: 0;
  margin: 0 4px;
  overflow: hidden;
  border-radius: 8px;

  :deep(.el-table) {
    --el-table-header-bg-color: var(--el-fill-color-light);
    --el-table-row-hover-bg-color: var(--el-color-primary-light-9);
  }

  :deep(.el-table th.el-table__cell) {
    color: var(--el-text-color-regular);
    font-size: 12px;
    font-weight: 600;
  }

  :deep(.el-table .cell) {
    padding-right: 10px;
    padding-left: 10px;
  }
}

.table-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.name-editor {
  display: flex;
  align-items: center;
  gap: 6px;

  :deep(.el-input) {
    min-width: 0;
  }
}

.muted-text {
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}

.status-cell {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 5px;

  span {
    color: var(--el-text-color-secondary);
    font-size: 12px;
    line-height: 1.35;

    &.is-error {
      color: var(--el-color-danger);
    }
  }
}

.expand-panel {
  padding: 12px 20px 16px 56px;
  background: var(--el-fill-color-lighter);

  .expand-meta {
    display: flex;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 10px;
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }
}

.empty-state {
  flex: 1;
  min-height: 220px;
  border: 1px dashed var(--el-border-color);
  border-radius: 9px;
  background:
    radial-gradient(circle at 50% 20%, var(--el-color-primary-light-9), transparent 42%),
    var(--el-fill-color-extra-light);
}

.w-full {
  width: 100%;
}

@media (max-width: 1280px) {
  .source-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .options-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .batch-content {
    padding: 12px;
  }

  .batch-workspace {
    padding: 15px 14px 0;
  }

  .source-grid,
  .options-grid {
    grid-template-columns: 1fr;
  }

  .workspace-divider,
  .progress-strip,
  .workspace-footer {
    margin-right: -14px;
    margin-left: -14px;
  }

  .progress-strip,
  .workspace-footer {
    padding-right: 14px;
    padding-left: 14px;
  }

  .workspace-footer {
    align-items: stretch;
    flex-direction: column;

    .footer-actions {
      display: grid;
      grid-template-columns: 1fr 1fr;
    }
  }

  .table-area {
    padding: 10px;
  }

  .table-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .expand-meta {
    flex-direction: column;
  }
}
</style>
