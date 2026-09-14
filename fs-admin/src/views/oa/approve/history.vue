<script setup lang="ts">
import { onMounted, ref } from 'vue'
import type { FormInstance, TableInstance } from 'element-plus'
import RouteUtil from '@/utils/RouteUtil'
import { useRoute, useRouter } from 'vue-router'
import ApiUtil from '@/utils/ApiUtil'
import ApproveApi from '@/api/oa/ApproveApi'
import WorkflowApi from '@/api/oa/WorkflowApi'
import UserApi from '@/api/member/UserApi'
import DateUtil from '@/utils/DateUtil'
import ProcessStatus from './ProcessStatus.vue'

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const columns = ref([
  { prop: 'id', label: 'ID' },
  { prop: 'deploymentInfo.name', label: '流程名称' },
  { prop: 'startUserInfo.name', label: '发起人', slot: 'submitter' },
  { prop: 'businessKey', label: '业务编号' },
  { prop: 'status', label: '状态', slot: 'status', width: 80, align: 'center' },
  { prop: 'startTime', label: '创建时间', formatter: DateUtil.render },
  { prop: 'endTime', label: '结束时间', formatter: DateUtil.render },
])
const rows = ref([])
const config = ref<any>({
  ready: false,
  finishStatus: {},
})
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route, {}))
const pagination = ref(RouteUtil.pagination(filters.value))

const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  ApproveApi.searchHistory(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data.rows
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

onMounted(() => {
  handleRefresh(false, true)
  WorkflowApi.config().then((result: any) => {
    Object.assign(config.value, { ready: true }, ApiUtil.data(result))
  }).catch(() => {})
})

const handleViewProcess = (scope: any, env: Event) => {
  RouteUtil.forward(route, router, env, {
    path: '/oa/approve/process',
    query: { processInstanceId: scope.row.id, taskId: '' }
  })
}
</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-search" v-show="searchable">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="发起人" prop="submitter">
        <form-select v-model="filters.submitter" :callback="UserApi.list" clearable placeholder="输入用户名称检索" />
      </form-search-item>
      <form-search-item label="流程名称" prop="deploymentId">
        <form-select v-model="filters.deploymentId" :callback="WorkflowApi.list" field-value="deploymentId" clearable placeholder="输入流程名称检索" />
      </form-search-item>
      <form-search-item label="完成状态" prop="finishStatus">
        <el-select v-model="filters.finishStatus" placeholder="请选择" clearable>
          <el-option v-for="(value, key) in config.finishStatus" :key="key" :value="key" :label="value" />
        </el-select>
      </form-search-item>
      <form-search-item>
        <el-button type="primary" @click="handleRefresh(true, false)" :loading="loading">查询</el-button>
        <el-button @click="filterRef?.resetFields()">重置</el-button>
      </form-search-item>
    </form-search>
  </el-card>
  <el-card :bordered="false" shadow="never" class="fs-table-card">
    <div class="fs-table-toolbar flex-end">
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
    >
      <TableColumn :columns="columns">
        <template #submitter="scope">
          {{ scope.row.startUserInfo?.name || scope.row.startUserIdName || scope.row.startUserId }}
        </template>
        <template #status="scope">
          <ProcessStatus :historic="scope.row" :runtime="scope.row.processInstanceInfo" />
        </template>
      </TableColumn>
      <el-table-column label="操作" width="90">
        <template #default="scope">
          <el-button link v-permit="['oa:workflow:', 'oa:approve:workflow']" @click="(e: any) => handleViewProcess(scope, e)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" @change="handleRefresh(true, true)" />
  </el-card>
</template>

<style lang="scss" scoped>
</style>
