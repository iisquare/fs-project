<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, TableInstance } from 'element-plus'
import SourceApi from '@/api/govern/SourceApi'
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
const filters = ref<any>(RouteUtil.query2filter(route, { advanced: false, page: 1, pageSize: 20 }))
const pagination = ref<any>(RouteUtil.pagination(filters.value))
const filterRef = ref<FormInstance>()
const config = ref<any>({ status: { 1: '启用', 2: '禁用' } })
const sourceTypes: Record<string, string> = { mysql: 'MySQL' }
const columns = ref([
  { prop: 'code', label: '编码', minWidth: 140 },
  { prop: 'name', label: '名称', minWidth: 160 },
  { prop: 'type', label: '类型', width: 110, slot: 'type' },
  { prop: 'version', label: '版本', width: 110 },
  { prop: 'sort', label: '排序', width: 90 },
  { prop: 'statusText', label: '状态', width: 95, slot: 'status' },
  { prop: 'updatedUidName', label: '修改者', width: 110, hide: true },
  { prop: 'updatedTime', label: '修改时间', minWidth: 165, hide: true, formatter: (row: any) => new Date(Number(row.updatedTime || 0)).toLocaleString('zh-CN', { hour12: false }) },
])

const refresh = (filter2query: boolean, keepPage = true) => {
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  if (filter2query) RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  SourceApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = ApiUtil.data(result)?.rows ?? []
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

const edit = (row?: any) => {
  router.push({ path: '/govern/system/sourceModel', query: row?.code ? { code: row.code } : {} }).catch(() => {})
}

const remove = () => {
  if (selection.value.length === 0) return
  TableUtil.confirm(`确认删除所选 ${selection.value.length} 个数据源？`).then(() => {
    loading.value = true
    Promise.all(selection.value.map((item: any) => SourceApi.delete({ code: item.code }, { warning: false }))).then(() => {
      refresh(false)
    }).catch(() => {
      loading.value = false
    })
  }).catch(() => {})
}

onMounted(() => {
  refresh(false)
  SourceApi.config({ warning: false }).then((result: any) => {
    config.value = ApiUtil.data(result) ?? config.value
  }).catch(() => {})
})
</script>

<template>
  <GovernPageShell title="数据源" description="维护标准落地评估与质量检查所使用的数据库连接">
  <el-card shadow="never" class="search-card" v-show="searchable">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="编码" prop="code">
        <el-input v-model="filters.code" clearable />
      </form-search-item>
      <form-search-item label="名称" prop="name">
        <el-input v-model="filters.name" clearable />
      </form-search-item>
      <form-search-item label="类型" prop="type">
        <el-select v-model="filters.type" clearable placeholder="全部类型">
          <el-option v-for="(value, key) in sourceTypes" :key="key" :label="value" :value="key" />
        </el-select>
      </form-search-item>
      <form-search-item label="版本" prop="version">
        <el-input v-model="filters.version" clearable />
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
        <button-add v-permit="'govern:source:add'" @click="edit()" />
        <button-delete v-permit="'govern:source:delete'" :disabled="selection.length === 0" @click="remove" />
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
      row-key="code"
      border
      @selection-change="(value: any[]) => selection = value"
    >
      <el-table-column type="selection" width="45" />
      <TableColumn :columns="columns">
        <template #type="scope">
          <el-tag effect="plain">{{ sourceTypes[scope.row.type] || scope.row.type }}</el-tag>
        </template>
        <template #status="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" effect="plain">
            {{ scope.row.statusText || config.status?.[scope.row.status] }}
          </el-tag>
        </template>
      </TableColumn>
      <el-table-column label="描述" prop="description" min-width="180" show-overflow-tooltip />
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button link v-permit="'govern:source:modify'" @click="edit(row)">配置</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" :loading="loading" @change="refresh(true)" />
  </el-card>
  </GovernPageShell>
</template>
