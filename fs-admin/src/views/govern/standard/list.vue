<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, TableInstance } from 'element-plus'
import { Download, Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import StandardApi from '@/api/govern/StandardApi'
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
const config = ref<any>({ molds: {}, flags: {}, levels: {}, status: {} })
const columns = ref([
  { prop: 'code', label: '编码', minWidth: 150 },
  { prop: 'name', label: '名称', minWidth: 170 },
  { prop: 'mold', label: '类型', width: 95, slot: 'mold' },
  { prop: 'type', label: '字段类型', width: 110 },
  { prop: 'size', label: '长度', width: 80 },
  { prop: 'digit', label: '精度', width: 80 },
  { prop: 'level', label: '预警等级', width: 100, slot: 'level', hide: true },
  { prop: 'statusText', label: '状态', width: 90, slot: 'status' },
  { prop: 'sort', label: '排序', width: 80 },
])

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
  StandardApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = ApiUtil.data(result)?.rows ?? []
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

const toCatalog = (catalog: string) => {
  filters.value = { page: 1, pageSize: pagination.value.pageSize, catalog }
  refresh(true, false)
}

const edit = (row?: any) => {
  router.push({
    path: '/govern/standard/draw',
    query: { catalog: row?.catalog || filters.value.catalog, code: row?.code, mode: row ? '' : '' },
  }).catch(() => {})
}

const pick = () => {
  router.push({ path: '/govern/standard/draw', query: { catalog: filters.value.catalog, mode: 'pick' } }).catch(() => {})
}

const handleAddCommand = (command: string) => {
  if (command === 'folder') return editFolder()
  if (command === 'pick') return pick()
  edit()
}

const remove = () => {
  if (selection.value.length === 0) return
  TableUtil.confirm(`确认删除所选 ${selection.value.length} 项标准？目录会连同下级标准一并删除。`).then(() => {
    loading.value = true
    Promise.all(selection.value.map((item: any) => StandardApi.delete({
      catalog: item.catalog,
      code: item.code,
    }, { warning: false }))).then(() => refresh(false)).catch(() => {
      loading.value = false
    })
  }).catch(() => {})
}

const exportStandards = () => {
  loading.value = true
  StandardApi.list(Object.assign({}, filters.value, { page: 1, pageSize: 500 }), { warning: false }).then((result: any) => {
    const data = ApiUtil.data(result)?.rows ?? []
    if (data.length === 0) {
      ElMessage.warning('当前筛选条件下没有可导出的标准')
      return
    }
    const headers = ['目录', '路径', '编码', '名称', '类型', '别名', '预警等级', '字段类型', '长度', '小数位', '允许为空', '状态', '描述']
    const escape = (value: any) => `"${String(value ?? '').replace(/"/g, '""')}"`
    const lines = data.map((row: any) => [
      row.catalog,
      row.path,
      row.code,
      row.name,
      config.value.molds?.[row.mold] || row.mold,
      row.another,
      config.value.levels?.[row.level] || row.level,
      row.type,
      row.size,
      row.digit,
      row.nullable ? '是' : '否',
      row.statusText || config.value.status?.[row.status],
      row.description,
    ].map(escape).join(','))
    const blob = new Blob(['\ufeff' + [headers.map(escape).join(','), ...lines].join('\r\n')], {
      type: 'text/csv;charset=utf-8',
    })
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = `数据标准_${filters.value.catalog.replace(/\//g, '_') || 'root'}.csv`
    link.click()
    URL.revokeObjectURL(link.href)
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

const folderVisible = ref(false)
const folderLoading = ref(false)
const folderRef = ref<FormInstance>()
const folder = ref<any>({})
const folderRules = {
  code: [{ required: true, message: '请输入目录编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入目录名称', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}

const editFolder = (row?: any) => {
  folder.value = Object.assign({
    catalog: filters.value.catalog,
    mold: 'catalog',
    status: '1',
    sort: 0,
  }, row, row?.status == null ? {} : { status: String(row.status) })
  folderVisible.value = true
}

const saveFolder = () => {
  folderRef.value?.validate((valid: boolean) => {
    if (!valid || folderLoading.value) return
    folderLoading.value = true
    StandardApi.save(folder.value, { success: true }).then(() => {
      folderVisible.value = false
      refresh(false)
    }).catch(() => {}).finally(() => {
      folderLoading.value = false
    })
  })
}

onMounted(() => {
  refresh(false)
  StandardApi.config({ warning: false }).then((result: any) => {
    config.value = ApiUtil.data(result) ?? config.value
  }).catch(() => {})
})
</script>

<template>
  <GovernPageShell title="数据标准" description="按业务目录维护字段标准，配置类型、长度、精度、可空和预警等级">
  <el-card shadow="never" class="search-card" v-show="searchable">
    <el-breadcrumb separator="/" class="breadcrumb">
      <el-breadcrumb-item><el-button link @click="toCatalog('/')">全部标准</el-button></el-breadcrumb-item>
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
      <form-search-item label="别名" prop="another">
        <el-input v-model="filters.another" clearable />
      </form-search-item>
      <form-search-item label="类型" prop="mold">
        <el-select v-model="filters.mold" clearable placeholder="全部类型">
          <el-option v-for="(value, key) in config.molds" :key="key" :label="value" :value="key" />
        </el-select>
      </form-search-item>
      <form-search-item>
        <el-button type="primary" :loading="loading" @click="refresh(true, false)">查询</el-button>
        <el-button @click="filterRef?.resetFields()">重置</el-button>
        <el-button v-permit="'govern:standard:add'" @click="pick">拾取字段</el-button>
      </form-search-item>
    </form-search>
  </el-card>

  <el-card shadow="never" class="fs-table-card">
    <div class="fs-table-toolbar flex-between">
      <el-space>
        <el-dropdown split type="success" trigger="click" v-permit="'govern:standard:add'" @click="edit()" @command="handleAddCommand">
          <el-button type="success" :icon="Plus">新增标准</el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="field">新增标准</el-dropdown-item>
              <el-dropdown-item command="folder">新增目录</el-dropdown-item>
              <el-dropdown-item command="pick" divided>从元数据拾取</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <button-delete v-permit="'govern:standard:delete'" :disabled="selection.length === 0" @click="remove" />
        <el-button :icon="Download" v-permit="'govern:standard:'" @click="exportStandards">导出标准</el-button>
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
        <template #mold="scope">
          <el-tag :type="scope.row.mold === 'catalog' ? 'warning' : 'primary'" effect="plain">
            {{ config.molds?.[scope.row.mold] || scope.row.mold }}
          </el-tag>
        </template>
        <template #level="scope">
          <el-tag :type="scope.row.level === 'force' ? 'danger' : scope.row.level === 'advise' ? 'warning' : 'info'" effect="plain">
            {{ config.levels?.[scope.row.level] || scope.row.level || '未设置' }}
          </el-tag>
        </template>
        <template #status="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" effect="plain">
            {{ scope.row.statusText || config.status?.[scope.row.status] }}
          </el-tag>
        </template>
      </TableColumn>
      <el-table-column label="字段属性" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.mold === 'catalog' ? row.path : `${row.type || '-'}(${row.size ?? '-'},${row.digit ?? '-'}) ${row.nullable ? '可空' : '非空'}` }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="130" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.mold === 'catalog'" link @click="toCatalog(`${row.catalog}${row.code}/`)">进入</el-button>
          <el-button v-if="row.mold === 'catalog'" link v-permit="'govern:standard:modify'" @click="editFolder(row)">编辑</el-button>
          <el-button v-if="row.mold !== 'catalog'" link v-permit="'govern:standard:modify'" @click="edit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" :loading="loading" @change="refresh(true)" />
  </el-card>

  <el-dialog v-model="folderVisible" :title="folder.code ? '编辑标准目录' : '新增标准目录'" width="520px">
    <el-form ref="folderRef" :model="folder" :rules="folderRules" label-width="90px">
      <el-form-item label="所属目录">
        <el-input v-model="folder.catalog" disabled />
      </el-form-item>
      <el-form-item label="目录编码" prop="code">
        <el-input v-model="folder.code" />
      </el-form-item>
      <el-form-item label="目录名称" prop="name">
        <el-input v-model="folder.name" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="folder.status">
          <el-option v-for="(value, key) in config.status" :key="key" :label="value" :value="key" />
        </el-select>
      </el-form-item>
      <el-form-item label="排序">
        <el-input-number v-model="folder.sort" :min="0" />
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="folder.description" type="textarea" :rows="3" />
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
