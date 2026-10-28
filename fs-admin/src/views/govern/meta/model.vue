<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, TableInstance } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import ModelApi from '@/api/govern/ModelApi'
import ApiUtil from '@/utils/ApiUtil'
import RouteUtil from '@/utils/RouteUtil'
import TableUtil from '@/utils/TableUtil'
import GovernPageShell from '@/views/govern/components/GovernPageShell.vue'

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const rows = ref<any[]>([])
const selection = ref<any[]>([])
const filters = ref<any>(RouteUtil.query2filter(route, {
  advanced: false,
  catalog: String(route.query.catalog || '/'),
  page: 1,
  pageSize: 50,
}))
const pagination = ref<any>(RouteUtil.pagination(filters.value))
const filterRef = ref<FormInstance>()
const config = ref<any>({ types: {}, columnTypes: [] })
const columns = ref([
  { prop: 'code', label: '编码', minWidth: 150 },
  { prop: 'name', label: '名称', minWidth: 160 },
  { prop: 'type', label: '类型', width: 100, slot: 'type' },
  { prop: 'pk', label: '主键', minWidth: 160, formatter: (row: any) => row.pk || '暂无' },
  { prop: 'sort', label: '排序', width: 90 },
  { prop: 'updatedUidName', label: '修改者', width: 110 },
  { prop: 'updatedTime', label: '修改时间', minWidth: 165, formatter: (row: any) => RouteUtilDate(row.updatedTime) },
])

const RouteUtilDate = (value: any) => {
  if (!value) return '暂无'
  const date = new Date(Number(value))
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString('zh-CN', { hour12: false })
}

const breadcrumb = computed(() => {
  const parts = String(filters.value.catalog || '/').split('/').filter(Boolean)
  let catalog = '/'
  return parts.map((code: string) => {
    catalog += code + '/'
    return { code, catalog }
  })
})

const refresh = (filter2query: boolean, keepPage = true) => {
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  if (!filters.value.catalog) filters.value.catalog = '/'
  if (filter2query) RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  ModelApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = ApiUtil.data(result)?.rows ?? []
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

const enter = (record?: any) => {
  const catalog = record?.type === 'catalog' ? `${record.catalog}${record.code}/` : '/'
  toCatalog(catalog)
}

const toCatalog = (catalog: string) => {
  filters.value = {
    page: 1,
    pageSize: pagination.value.pageSize,
    catalog,
  }
  refresh(true, false)
}

const editModel = (record: any) => {
  router.push({
    path: '/govern/meta/draw',
    query: { catalog: record.catalog, code: record.code, type: record.type },
  }).catch(() => {})
}

const createModel = () => {
  router.push({ path: '/govern/meta/draw', query: { catalog: filters.value.catalog, type: 'table' } }).catch(() => {})
}

const importModel = () => {
  router.push({ path: '/govern/meta/modelCompare', query: { catalog: filters.value.catalog } }).catch(() => {})
}

const handleAddCommand = (command: string) => {
  if (command === 'folder') return editFolder()
  if (command === 'import') return importModel()
  createModel()
}

const compareModel = (record: any) => {
  router.push({
    path: '/govern/meta/modelCompare',
    query: { catalog: record.catalog, code: record.code },
  }).catch(() => {})
}

const remove = () => {
  if (selection.value.length === 0) return
  const labels = selection.value.map((item: any) => `${item.catalog}${item.code}`).join('、')
  TableUtil.confirm(`确认删除 ${labels}？目录会连同其下模型一并删除。`).then(() => {
    loading.value = true
    Promise.all(selection.value.map((item: any) => ModelApi.delete({
      catalog: item.catalog,
      code: item.code,
    }, { warning: false }))).then(() => {
      refresh(false)
    }).catch(() => {
      loading.value = false
    })
  }).catch(() => {})
}

const folderVisible = ref(false)
const folderLoading = ref(false)
const folderFormRef = ref<FormInstance>()
const folderForm = ref<any>({})
const folderRules = {
  code: [{ required: true, message: '请输入包编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入包名称', trigger: 'blur' }],
}

const editFolder = (record?: any) => {
  folderForm.value = Object.assign({
    catalog: filters.value.catalog,
    type: 'catalog',
    sort: 0,
  }, record)
  folderVisible.value = true
}

const saveFolder = () => {
  folderFormRef.value?.validate((valid: boolean) => {
    if (!valid || folderLoading.value) return
    folderLoading.value = true
    ModelApi.save(folderForm.value, { success: true }).then(() => {
      folderVisible.value = false
      refresh(false)
    }).catch(() => {}).finally(() => {
      folderLoading.value = false
    })
  })
}

onMounted(() => {
  refresh(false)
  ModelApi.config({ warning: false }).then((result: any) => {
    config.value = ApiUtil.data(result) ?? config.value
  }).catch(() => {})
})
</script>

<template>
  <GovernPageShell title="模型管理" description="按包组织元数据模型，维护字段与主键，并进入模型关系分析">
  <el-card shadow="never" class="search-card" v-show="searchable">
    <el-breadcrumb separator="/" class="breadcrumb">
      <el-breadcrumb-item><el-button link @click="toCatalog('/')">全部模型</el-button></el-breadcrumb-item>
      <el-breadcrumb-item v-for="item in breadcrumb" :key="item.catalog">
        <el-button link @click="toCatalog(item.catalog)">{{ item.code }}</el-button>
      </el-breadcrumb-item>
    </el-breadcrumb>
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="编码" prop="code">
        <el-input v-model="filters.code" clearable />
      </form-search-item>
      <form-search-item label="名称" prop="name">
        <el-input v-model="filters.name" clearable />
      </form-search-item>
      <form-search-item label="类型" prop="type">
        <el-select v-model="filters.type" clearable placeholder="全部类型">
          <el-option v-for="(value, key) in config.types" :key="key" :label="`${key} - ${value}`" :value="key" />
        </el-select>
      </form-search-item>
      <form-search-item>
        <el-button type="primary" :loading="loading" @click="refresh(true, false)">查询</el-button>
        <el-button @click="filterRef?.resetFields()">重置</el-button>
      </form-search-item>
    </form-search>
  </el-card>

  <el-card shadow="never" class="fs-table-card">
    <div class="fs-table-toolbar flex-between">
      <el-space>
        <el-dropdown split type="success" trigger="click" v-permit="'govern:model:add'" @click="createModel" @command="handleAddCommand">
          <el-button type="success" :icon="Plus">新增</el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="table">新增类</el-dropdown-item>
              <el-dropdown-item command="folder">新增包</el-dropdown-item>
              <el-dropdown-item command="import" divided>导入模型</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <button-delete v-permit="'govern:model:delete'" :disabled="selection.length === 0" @click="remove" />
      </el-space>
      <el-space>
        <button-search @click="searchable = !searchable" />
        <button-refresh :loading="loading" @click="refresh(true)" />
        <TableColumnSetting v-model="columns" :table="tableRef" :loading="loading" />
      </el-space>
    </div>
    <el-table
      ref="tableRef"
      v-loading="loading"
      :data="rows"
      :row-key="(row: any) => `${row.catalog}${row.code}`"
      border
      @selection-change="(value: any[]) => selection = value"
    >
      <el-table-column type="selection" width="45" />
      <TableColumn :columns="columns">
        <template #type="scope">
          <el-tag :type="scope.row.type === 'catalog' ? 'warning' : 'primary'" effect="plain">
            {{ config.types?.[scope.row.type] || scope.row.type }}
          </el-tag>
        </template>
      </TableColumn>
      <el-table-column label="操作" width="210" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.type === 'catalog'" link @click="enter(row)">进入</el-button>
          <el-button v-if="row.type === 'catalog'" link v-permit="'govern:model:modify'" @click="editFolder(row)">编辑</el-button>
          <el-button v-if="row.type !== 'catalog'" link v-permit="'govern:model:modify'" @click="editModel(row)">编辑</el-button>
          <el-button v-if="row.type !== 'catalog'" link v-permit="'govern:model:modify'" @click="compareModel(row)">导入对比</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" :loading="loading" @change="refresh(true)" />
  </el-card>

  <el-dialog v-model="folderVisible" :title="folderForm.id || folderForm.code ? '编辑模型包' : '新增模型包'" width="520px">
    <el-form ref="folderFormRef" :model="folderForm" :rules="folderRules" label-width="92px">
      <el-form-item label="所属包">
        <el-input v-model="folderForm.catalog" disabled />
      </el-form-item>
      <el-form-item label="包编码" prop="code">
        <el-input v-model="folderForm.code" />
      </el-form-item>
      <el-form-item label="包名称" prop="name">
        <el-input v-model="folderForm.name" />
      </el-form-item>
      <el-form-item label="排序">
        <el-input-number v-model="folderForm.sort" :min="0" />
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="folderForm.description" type="textarea" :rows="3" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-space>
        <el-button type="primary" :loading="folderLoading" @click="saveFolder">保存</el-button>
        <el-button @click="folderVisible = false">取消</el-button>
      </el-space>
    </template>
  </el-dialog>
  </GovernPageShell>
</template>

<style scoped>
.breadcrumb { margin-bottom: 16px; }
</style>
