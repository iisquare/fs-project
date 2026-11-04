<script setup lang="ts">
import { onMounted, ref } from 'vue'
import type { FormInstance, TableInstance } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import DashboardApi from '@/api/bi/DashboardApi'
import DashboardTemplateApi from '@/api/bi/DashboardTemplateApi'
import DateUtil from '@/utils/DateUtil'
import RouteUtil from '@/utils/RouteUtil'
import TableUtil from '@/utils/TableUtil'

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const columns = ref([
  { prop: 'id', label: 'ID' },
  { prop: 'name', label: '大屏名称' },
  {
    prop: 'templateName',
    label: '引用模板',
    formatter: (row: any, _column: any, value: any) => value || (row.templateId ? `#${row.templateId}` : '未引用模板'),
  },
  { prop: 'sort', label: '排序' },
  { prop: 'statusText', label: '状态' },
  { prop: 'description', label: '描述', hide: true },
  { prop: 'createdUidName', label: '创建者', hide: true },
  { prop: 'createdTime', label: '创建时间', hide: true, formatter: DateUtil.render },
  { prop: 'updatedTime', label: '修改时间', hide: true, formatter: DateUtil.render },
])
const rows = ref<any[]>([])
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route, { advanced: false }))
const pagination = ref(RouteUtil.pagination(filters.value))
const selection = ref<any[]>([])
const templateCallback = (params: any) => DashboardTemplateApi.list(params)

const handleRefresh = (toQuery: boolean, keepPage: boolean) => {
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  toQuery && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  DashboardApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data?.rows || []
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

onMounted(() => {
  handleRefresh(false, true)
})

const handleAdd = () => {
  router.push('/bi/design/dashboard').catch(() => {})
}
const handleDelete = () => {
  TableUtil.selection(selection.value).then((ids: any) => {
    loading.value = true
    DashboardApi.delete(ids, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {
      loading.value = false
    })
  }).catch(() => {})
}
const handleDesign = (row: any) => {
  router.push({ path: '/bi/design/dashboard', query: { id: row.id } }).catch(() => {})
}
const handlePreview = (row: any) => {
  const target = router.resolve({ path: '/bi/dashboard/view', query: { id: row.id } })
  window.open(target.href, '_blank')
}
const handleCreate = () => {
  handleAdd()
}
</script>

<template>
  <el-card v-show="searchable" :bordered="false" shadow="never" class="fs-table-search">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="名称" prop="name">
        <el-input v-model="filters.name" clearable placeholder="请输入大屏名称" />
      </form-search-item>
      <form-search-item label="引用模板" prop="templateId">
        <form-select
          v-model="filters.templateId"
          :callback="templateCallback"
          :page-size="20"
          clearable
          placeholder="搜索并选择模板" />
      </form-search-item>
      <form-search-item>
        <el-button type="primary" :loading="loading" @click="handleRefresh(true, false)">查询</el-button>
        <el-button @click="filterRef?.resetFields()">重置</el-button>
      </form-search-item>
    </form-search>
  </el-card>

  <el-card :bordered="false" shadow="never" class="fs-table-card">
    <div class="fs-table-toolbar flex-between">
      <el-space>
        <el-button v-permit="'bi:dashboard:add'" type="primary" :icon="Plus" @click="handleCreate">新建大屏</el-button>
        <button-delete v-permit="'bi:dashboard:delete'" :disabled="selection.length === 0" @click="handleDelete" />
      </el-space>
      <el-space>
        <button-search @click="searchable = !searchable" />
        <button-refresh :loading="loading" @click="handleRefresh(true, true)" />
        <TableColumnSetting v-model="columns" :table="tableRef" :loading="loading" />
      </el-space>
    </div>
    <el-table
      ref="tableRef"
      v-loading="loading"
      :data="rows"
      :row-key="(row: any) => row.id"
      table-layout="auto"
      @selection-change="(value: any[]) => selection = value">
      <el-table-column type="selection" />
      <TableColumn :columns="columns" />
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" v-permit="'bi:dashboard:modify'" @click="handleDesign(row)">设计</el-button>
          <el-button link v-permit="'bi:dashboard:'" @click="handlePreview(row)">预览</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" :loading="loading" @change="handleRefresh(true, true)" />
  </el-card>

</template>
