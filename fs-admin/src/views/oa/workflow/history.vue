<script setup lang="ts">
import { onMounted, ref } from 'vue'
import type { FormInstance, TableInstance } from 'element-plus'
import RouteUtil from '@/utils/RouteUtil'
import { useRoute, useRouter } from 'vue-router'
import ApiUtil from '@/utils/ApiUtil'
import TableUtil from '@/utils/TableUtil'
import WorkflowApi from '@/api/oa/WorkflowApi'

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const activeTab = ref('running')
const columns = ref([
  { prop: 'id', label: '实例ID' },
  { prop: 'processName', label: '流程名称' },
  { prop: 'startTime', label: '开始时间' },
  { prop: 'endTime', label: '结束时间' },
  { prop: 'statusText', label: '状态' },
])
const rows = ref([])
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route, {}))
const pagination = ref(RouteUtil.pagination(filters.value))
const selection: any = ref([])

const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  const api = activeTab.value === 'running' ? WorkflowApi.searchHistory : WorkflowApi.searchHistory
  const params = Object.assign({}, filters.value, { finished: activeTab.value === 'completed' })
  api(params).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data.rows
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

onMounted(() => {
  handleRefresh(false, true)
})

const handleViewProcess = (scope: any, env: Event) => {
  RouteUtil.forward(route, router, env, {
    path: '/oa/approve/process',
    query: { id: scope.row.id }
  })
}

const handleSuspend = (scope: any) => {
  loading.value = true
  WorkflowApi.suspendProcessInstance({ id: scope.row.id }, { success: true }).then(() => {
    handleRefresh(false, true)
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

const handleActivate = (scope: any) => {
  loading.value = true
  WorkflowApi.activateProcessInstance({ id: scope.row.id }, { success: true }).then(() => {
    handleRefresh(false, true)
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

const handleDelete = () => {
  TableUtil.selection(selection.value).then((ids: any) => {
    loading.value = true
    const api = activeTab.value === 'running'
      ? WorkflowApi.deleteProcessInstance
      : WorkflowApi.deleteHistoricProcessInstance
    const promises = ids.map((id: any) => api({ id }))
    Promise.all(promises).then(() => {
      handleRefresh(false, true)
    }).catch(() => {}).finally(() => {
      loading.value = false
    })
  }).catch(() => {})
}

const handleTabChange = () => {
  handleRefresh(false, true)
}
</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-search" v-show="searchable">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="名称" prop="name">
        <el-input v-model="filters.name" clearable />
      </form-search-item>
      <form-search-item>
        <el-button type="primary" @click="handleRefresh(true, false)">查询</el-button>
        <el-button @click="filterRef?.resetFields()">重置</el-button>
      </form-search-item>
    </form-search>
  </el-card>
  <el-card :bordered="false" shadow="never" class="fs-table-card">
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="运行中" name="running" />
      <el-tab-pane label="已完成" name="completed" />
    </el-tabs>
    <div class="fs-table-toolbar flex-between">
      <el-space>
        <button-delete v-permit="'oa:workflow:delete'" :disabled="selection.length === 0" @click="handleDelete" />
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
      :row-key="(record: any) => record.id"
      :border="true"
      v-loading="loading"
      table-layout="auto"
      @selection-change="(newSelection: any) => selection = newSelection"
    >
      <el-table-column type="selection" />
      <TableColumn :columns="columns" />
      <el-table-column label="操作">
        <template #default="scope">
          <el-button link @click="(e: any) => handleViewProcess(scope, e)">查看</el-button>
          <el-button link v-if="activeTab === 'running'" @click="() => handleSuspend(scope)">挂起</el-button>
          <el-button link v-if="activeTab === 'running'" @click="() => handleActivate(scope)">激活</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" @change="handleRefresh(true, true)" />
  </el-card>
</template>

<style lang="scss" scoped>
</style>
