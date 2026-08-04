<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { FormInstance, TableInstance } from 'element-plus'
import RouteUtil from '@/utils/RouteUtil'
import { useRoute } from 'vue-router'
import DatasetApi, { buildDatasetContent, parseDatasetContent } from '@/api/bi/DatasetApi'
import DatasourceApi from '@/api/bi/DatasourceApi'
import ApiUtil from '@/utils/ApiUtil'
import DateUtil from '@/utils/DateUtil'
import TableUtil from '@/utils/TableUtil'
import CodeEditor from '@/components/Editor/CodeEditor.vue'
import DataTable from '@/components/Data/DataTable.vue'

const route = useRoute()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const columns = ref([
  { prop: 'id', label: 'ID', width: '70' },
  { prop: 'name', label: '数据集名称', minWidth: '160' },
  { prop: 'sqlPreview', label: 'SQL 摘要', minWidth: '200' },
  { prop: 'statusText', label: '状态', width: '100' },
  { prop: 'updatedTime', label: '更新时间', width: '170' },
])
const config = ref({
  ready: false,
  status: {} as Record<string, string>,
})
const rows = ref([])
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route, { advanced: false }))
const pagination = ref(RouteUtil.pagination(filters.value))
const selection: any = ref([])

// 可用数据源列表（用于关联选择）
const datasources = ref<any[]>([])

const loadDatasources = () => {
  DatasourceApi.list({ page: 1, pageSize: 200, status: 1 }).then((result: any) => {
    const data = ApiUtil.data(result)
    datasources.value = data?.rows || data?.list || []
  }).catch(() => {})
}

function enrichRow(row: any) {
  const { sql } = parseDatasetContent(row.content)
  row.sqlPreview = sql ? sql.substring(0, 80) + (sql.length > 80 ? '...' : '') : '-'
}

const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  filter2query && RouteUtil.filter2query(route, undefined as any, filters.value)
  loading.value = true
  DatasetApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    const data = ApiUtil.data(result)
    const list = data?.rows || data?.list || []
    list.forEach(enrichRow)
    rows.value = list
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

onMounted(() => {
  handleRefresh(false, true)
  loadDatasources()
  DatasetApi.config().then(result => {
    Object.assign(config.value, { ready: true }, ApiUtil.data(result))
  }).catch(() => {})
})

// ==================== 表单 ====================
const infoVisible = ref(false)
const formVisible = ref(false)
const formLoading = ref(false)
const form: any = ref({})
const formRef: any = ref<FormInstance>()
const formSourceIds = ref<number[]>([])
const formColumns = ref<{ name: string; title: string; type: string }[]>([])
const formSql = ref('')
const columnTypes = ref(['String', 'Integer', 'Long', 'Double', 'Boolean', 'Date', 'Timestamp', 'Unknown'])
const columnEditable = ref(true)

const rules = {
  name: [{ required: true, message: '请输入数据集名称', trigger: 'blur' }],
}

const sourceIdOptions = computed(() => {
  return datasources.value.map((ds: any) => ({
    value: ds.id,
    label: `${ds.name} (${ds.typeText || ds.type})`,
  }))
})

const handleAdd = () => {
  form.value = { name: '', description: '' }
  formSourceIds.value = []
  formSql.value = ''
  formColumns.value = []
  formVisible.value = true
}

const handleShow = (scope: any) => {
  const row = scope.row
  DatasetApi.info(row.id).then((result: any) => {
    const info = ApiUtil.data(result)
    const { sourceIds, sql, table, collection } = parseDatasetContent(info.content)
    form.value = {
      id: info.id,
      name: info.name,
      status: info.status,
      statusText: info.statusText,
      description: info.description || '',
      collection: collection || '',
      createdTime: info.createdTime,
      updatedTime: info.updatedTime,
    }
    formSourceIds.value = sourceIds
    formSql.value = sql
    formColumns.value = table
    infoVisible.value = true
  }).catch(() => {})
}

const handleEdit = (scope: any) => {
  const row = scope.row
  loading.value = true
  DatasetApi.info(row.id).then((result: any) => {
    const info = ApiUtil.data(result)
    const { sourceIds, sql, table, collection } = parseDatasetContent(info.content)
    form.value = {
      id: info.id,
      name: info.name,
      status: info.status,
      description: info.description || '',
      collection: collection || '',
    }
    formSourceIds.value = sourceIds
    formSql.value = sql
    formColumns.value = table.map((col: any) => ({
      name: col.name || '',
      title: col.title || col.name || '',
      type: col.type || '',
    }))
    formVisible.value = true
  }).catch(() => {}).finally(() => { loading.value = false })
}

const handleSubmit = () => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid || formLoading.value) return
    formLoading.value = true
    const table = formColumns.value
      .filter(c => c.name)
      .map(c => ({ name: c.name, title: c.title || c.name, type: c.type || 'String', format: '', enabled: true }))
    const content = buildDatasetContent({
      sourceIds: formSourceIds.value,
      sql: formSql.value,
      table,
      collection: form.value.collection || '',
    })
    const params: any = {
      name: form.value.name,
      content,
      description: form.value.description,
    }
    if (form.value.id) {
      params.id = form.value.id
    } else {
      params.status = 1
    }
    DatasetApi.save(params, { success: true }).then(() => {
      handleRefresh(false, true)
      formVisible.value = false
    }).catch(() => {}).finally(() => {
      formLoading.value = false
    })
  })
}

const handleDelete = () => {
  TableUtil.selection(selection.value).then((ids: any) => {
    loading.value = true
    DatasetApi.delete(ids, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {
      loading.value = false
    })
  }).catch(() => {})
}

// ==================== SQL Schema & Preview ====================
const schemaVisible = ref(false)
const schemaLoading = ref(false)
const schemaDatasetId = ref(0)
const schemaDatasetName = ref('')
const schemaColumns = ref<any[]>([])

const handleOpenSchema = (scope: any) => {
  schemaDatasetId.value = scope.row.id
  schemaDatasetName.value = scope.row.name
  schemaColumns.value = []
  schemaVisible.value = true
  schemaLoading.value = true
  DatasetApi.sqlSchema(scope.row.id).then((result: any) => {
    const data = ApiUtil.data(result)
    if (data && typeof data === 'object') {
      if (Array.isArray(data)) {
        schemaColumns.value = data
      } else {
        schemaColumns.value = Object.entries(data).map(([name, info]: [string, any]) => ({
          name,
          type: info?.type || '',
          format: info?.format || '',
        }))
      }
    }
  }).catch(() => {}).finally(() => {
    schemaLoading.value = false
  })
}

const previewVisible = ref(false)
const previewLoading = ref(false)
const previewDatasetId = ref(0)
const previewDatasetName = ref('')
const previewColumns = ref<string[]>([])
const previewRows = ref<any[]>([])
const previewLimit = ref(100)

const handleOpenPreview = (scope: any) => {
  previewDatasetId.value = scope.row.id
  previewDatasetName.value = scope.row.name
  previewColumns.value = []
  previewRows.value = []
  previewLimit.value = 100
  previewVisible.value = true
  loadPreview()
}

const loadPreview = () => {
  previewLoading.value = true
  DatasetApi.sqlPreview(previewDatasetId.value, previewLimit.value).then((result: any) => {
    const data = ApiUtil.data(result)
    if (data) {
      if (data.columns) {
        previewColumns.value = data.columns.map((c: any) => typeof c === 'string' ? c : c.name)
      }
      previewRows.value = data.rows || data || []
      if (previewRows.value.length > 0 && previewColumns.value.length === 0) {
        previewColumns.value = Object.keys(previewRows.value[0])
      }
    }
  }).catch(() => {}).finally(() => {
    previewLoading.value = false
  })
}
</script>

<template>
  <!-- ==================== 搜索栏 ==================== -->
  <el-card :bordered="false" shadow="never" class="fs-table-search" v-show="searchable">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="名称" prop="name">
        <el-input v-model="filters.name" clearable placeholder="输入名称搜索" />
      </form-search-item>
      <form-search-item label="ID" prop="id">
        <el-input v-model="filters.id" clearable placeholder="按 ID 搜索" />
      </form-search-item>
      <form-search-item label="状态" prop="status">
        <el-select v-model="filters.status" placeholder="全部状态" clearable>
          <el-option v-for="(v, k) in config.status" :key="k" :value="k" :label="v" />
        </el-select>
      </form-search-item>
      <form-search-item>
        <el-button type="primary" @click="handleRefresh(true, false)" :loading="loading">查询</el-button>
        <el-button @click="filterRef?.resetFields()">重置</el-button>
      </form-search-item>
    </form-search>
  </el-card>

  <!-- ==================== 表格 ==================== -->
  <el-card :bordered="false" shadow="never" class="fs-table-card">
    <div class="fs-table-toolbar flex-between">
      <el-space>
        <button-add v-permit="'bi:dataset:add'" @click="handleAdd" />
        <button-delete v-permit="'bi:dataset:delete'" :disabled="selection.length === 0" @click="handleDelete" />
      </el-space>
      <el-space>
        <button-search @click="searchable = !searchable" />
        <button-refresh @click="handleRefresh(true, true)" :loading="loading" />
        <TableColumnSetting v-model="columns" :table="tableRef" />
      </el-space>
    </div>
    <el-table
      ref="tableRef"
      :data="rows"
      :row-key="(r: any) => r.id"
      :border="true"
      v-loading="loading"
      table-layout="auto"
      @selection-change="(s: any) => selection = s"
    >
      <el-table-column type="selection" width="42" />
      <TableColumn :columns="columns">
        <template #name="scope">
          <span class="cell-name">{{ scope.row.name }}</span>
        </template>
        <template #sqlPreview="scope">
          <code class="cell-sql">{{ scope.row.sqlPreview }}</code>
        </template>
        <template #statusText="scope">
          <span class="cell-status" :class="`cell-status--${scope.row.status}`">
            {{ scope.row.statusText || config.status?.[scope.row.status] || '未知' }}
          </span>
        </template>
        <template #updatedTime="scope">
          {{ DateUtil.format(scope.row.updatedTime) }}
        </template>
      </TableColumn>
      <el-table-column label="操作" width="220">
        <template #default="scope">
          <el-button link type="primary" @click="handleShow(scope)" v-permit="'bi:dataset:'">查看</el-button>
          <el-button link type="primary" @click="handleEdit(scope)" v-permit="'bi:dataset:modify'">编辑</el-button>
          <el-button link type="primary" @click="handleOpenSchema(scope)">结构</el-button>
          <el-button link type="primary" @click="handleOpenPreview(scope)">预览</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" @change="handleRefresh(true, true)" />
  </el-card>

  <!-- ==================== 查看抽屉 ==================== -->
  <el-drawer v-model="infoVisible" title="数据集详情" size="680px">
    <template v-if="form.id">
      <div class="info-hero">
        <span class="info-hero__name">{{ form.name }}</span>
        <span class="info-dot-status" :class="`info-dot-status--${form.status}`">
          {{ form.statusText || config.status?.[form.status] || '未知' }}
        </span>
      </div>

      <div class="info-section">
        <div class="info-section__title">基本信息</div>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="ID">{{ form.id }}</el-descriptions-item>
          <el-descriptions-item label="数据集名称">{{ form.name }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ form.description || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <div class="info-section" v-if="formSourceIds.length">
        <div class="info-section__title">关联数据源</div>
        <el-tag v-for="sid in formSourceIds" :key="sid" style="margin-right:8px;margin-bottom:4px" type="info">
          ds_{{ sid }}
        </el-tag>
      </div>

      <div class="info-section" v-if="formSql">
        <div class="info-section__title">查询 SQL</div>
        <CodeEditor v-model="formSql" :height="200" mode="sql" />
      </div>

      <div class="info-section" v-if="formColumns.length">
        <div class="info-section__title">字段定义</div>
        <DataTable v-model="formColumns" v-model:types="columnTypes" />
      </div>

      <div class="info-section">
        <div class="info-section__title">元信息</div>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="创建时间">{{ DateUtil.format(form.createdTime) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ DateUtil.format(form.updatedTime) }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </template>
  </el-drawer>

  <!-- ==================== 编辑/新增抽屉 ==================== -->
  <el-drawer
    v-model="formVisible"
    :close-on-click-modal="false"
    :show-close="false"
    :destroy-on-close="true"
    size="760px"
  >
    <template #header="{ close, titleId, titleClass }">
      <h4 :id="titleId" :class="titleClass" style="margin:0;font-size:16px">
        {{ form.id ? '编辑数据集' : '创建数据集' }}
      </h4>
      <el-space>
        <el-button type="primary" @click="handleSubmit" :loading="formLoading">保存</el-button>
        <el-button @click="close">取消</el-button>
      </el-space>
    </template>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="ds-form">

      <div class="ds-block">
        <div class="ds-block__title">
          <span class="ds-block__bar"></span>
          基本信息
        </div>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="例如：用户订单统计" maxlength="60" show-word-limit />
        </el-form-item>
        <el-form-item label="描述">
          <el-input type="textarea" v-model="form.description" placeholder="数据集的用途或描述" :rows="2" />
        </el-form-item>
      </div>

      <div class="ds-block">
        <div class="ds-block__title">
          <span class="ds-block__bar"></span>
          关联数据源
        </div>
        <el-form-item label="数据源">
          <el-select
            v-model="formSourceIds"
            multiple
            filterable
            placeholder="选择要关联的数据源（可在 SQL 中通过 ds_{id} 引用）"
            style="width:100%"
          >
            <el-option
              v-for="opt in sourceIdOptions"
              :key="opt.value"
              :value="opt.value"
              :label="opt.label"
            />
          </el-select>
        </el-form-item>
        <div v-if="formSourceIds.length" style="margin-top:-8px;margin-bottom:12px;color:var(--el-text-color-secondary);font-size:12px">
          引用方式：
          <el-tag v-for="sid in formSourceIds" :key="sid" size="small" style="margin-right:4px" type="info">
            ds_{{ sid }}
          </el-tag>
        </div>
      </div>

      <div class="ds-block">
        <div class="ds-block__title">
          <span class="ds-block__bar"></span>
          SQL 查询
        </div>
        <el-form-item label="SQL">
          <CodeEditor v-model="formSql" :height="220" mode="sql" />
        </el-form-item>
        <div style="margin-top:-8px;color:var(--el-text-color-secondary);font-size:12px">
          使用 <code>ds_{id}</code> 引用已关联的数据源表，例如：<code>SELECT * FROM ds_1.users</code>
        </div>
      </div>

      <div class="ds-block">
        <div class="ds-block__title">
          <span class="ds-block__bar"></span>
          字段定义
          <span class="ds-block__sub">可选，用于声明输出字段的元数据</span>
        </div>
        <DataTable v-model="formColumns" v-model:types="columnTypes" v-model:editable="columnEditable" />
      </div>

    </el-form>
  </el-drawer>

  <!-- ==================== 结构查看对话框 ==================== -->
  <el-dialog v-model="schemaVisible" :title="`SQL 结构 - ${schemaDatasetName}`" width="700px" destroy-on-close>
    <div v-loading="schemaLoading">
      <el-table :data="schemaColumns" max-height="450" border size="small" v-if="schemaColumns.length">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="name" label="字段名" min-width="180" />
        <el-table-column prop="type" label="类型" width="160" />
        <el-table-column prop="format" label="格式" min-width="120" />
      </el-table>
      <el-empty v-if="!schemaLoading && schemaColumns.length === 0" description="无法解析 SQL 结构" />
    </div>
  </el-dialog>

  <!-- ==================== 数据预览对话框 ==================== -->
  <el-dialog v-model="previewVisible" :title="`数据预览 - ${previewDatasetName}`" width="1000px" destroy-on-close>
    <div v-loading="previewLoading">
      <div style="margin-bottom:12px;display:flex;align-items:center;gap:12px">
        <span>返回行数：</span>
        <el-input-number v-model="previewLimit" :min="1" :max="500" @change="loadPreview" />
        <el-button @click="loadPreview">刷新</el-button>
      </div>
      <el-table
        v-if="previewRows.length"
        :data="previewRows"
        max-height="450"
        border
        size="small"
        style="width:100%;overflow-x:auto"
      >
        <el-table-column
          v-for="col in previewColumns"
          :key="col"
          :prop="col"
          :label="col"
          :width="Math.max(120, Math.min(200, 1800 / previewColumns.length))"
          show-overflow-tooltip
        />
      </el-table>
      <el-empty v-if="!previewLoading && previewRows.length === 0" description="无数据或 SQL 执行失败" />
    </div>
  </el-dialog>
</template>

<style lang="scss" scoped>
.cell-name { font-weight: 500; }

.cell-sql {
  font-family: 'SF Mono', 'Cascadia Code', 'Consolas', monospace;
  font-size: 12px;
  background: var(--el-fill-color-light);
  padding: 2px 6px;
  border-radius: 3px;
  word-break: break-all;
}

.cell-status {
  font-size: 13px;
  &::before {
    content: '';
    display: inline-block;
    width: 6px; height: 6px;
    border-radius: 50%;
    margin-right: 6px;
    vertical-align: middle;
    margin-top: -1px;
  }
  &--1::before { background: var(--el-color-success); }
  &--2::before { background: var(--el-color-warning); }
  &---1::before { background: var(--el-color-danger); }
}

.info-hero {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 24px; padding-bottom: 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  &__name { font-size: 18px; font-weight: 600; color: var(--el-text-color-primary); }
}

.info-dot-status {
  font-size: 13px; flex-shrink: 0;
  &::before {
    content: '';
    display: inline-block;
    width: 7px; height: 7px;
    border-radius: 50%;
    margin-right: 6px;
    vertical-align: middle; margin-top: -1px;
  }
  &--1 { color: var(--el-color-success); &::before { background: var(--el-color-success); } }
  &--2 { color: var(--el-color-warning); &::before { background: var(--el-color-warning); } }
  &---1 { color: var(--el-color-danger); &::before { background: var(--el-color-danger); } }
}

.info-section {
  margin-bottom: 20px;
  &__title { font-size: 14px; font-weight: 600; color: var(--el-text-color-primary); margin-bottom: 10px; }
}

.ds-form {
  padding: 4px 0 32px;
  :deep(.el-form-item) { margin-bottom: 18px; }
}

.ds-block {
  margin-bottom: 28px;
  &__title {
    display: flex; align-items: center; gap: 10px;
    margin-bottom: 20px;
    font-size: 15px; font-weight: 600;
    color: var(--el-text-color-primary);
  }
  &__bar { width: 3px; height: 18px; border-radius: 2px; flex-shrink: 0; background: var(--el-color-primary); }
  &__sub { font-weight: 400; font-size: 12px; color: var(--el-text-color-placeholder); }
}
</style>
