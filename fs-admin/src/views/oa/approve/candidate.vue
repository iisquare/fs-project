<script setup lang="ts">
import { onMounted, ref } from 'vue'
import type { FormInstance, TableInstance } from 'element-plus'
import RouteUtil from '@/utils/RouteUtil'
import { useRoute, useRouter } from 'vue-router'
import ApproveApi from '@/api/oa/ApproveApi'
import UserApi from '@/api/member/UserApi'
import WorkflowApi from '@/api/oa/WorkflowApi'
import DateUtil from '@/utils/DateUtil'

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const columns = ref([
  { prop: 'id', label: 'ID' },
  { prop: 'deploymentInfo.name', label: '流程名称' },
  { prop: 'processInstanceInfo.startUserInfo.name', label: '发起人', slot: 'submitter' },
  { prop: 'name', label: '节点名称' },
  { prop: 'processInstanceInfo.businessKey', label: '业务编号' },
  { prop: 'createTime', label: '创建时间', formatter: DateUtil.render },
])
const rows = ref([])
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route, {}))
const pagination = ref(RouteUtil.pagination(filters.value))

const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  ApproveApi.searchCandidate(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data.rows
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

onMounted(() => {
  handleRefresh(false, true)
})

// 签收：办理时签收后直接进入任务办理页
const handleClaim = (scope: any, transact: boolean) => {
  loading.value = true
  ApproveApi.claim({ taskId: scope.row.id }, { success: true }).then(() => {
    if (transact) {
      router.push({ path: '/oa/approve/transact', query: { taskId: scope.row.id } })
      return true
    }
    handleRefresh(false, true)
    return true
  }).catch(() => {}).finally(() => {
    loading.value = false
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
      <el-table-column type="expand" width="45">
        <template #default="scope">
          <div class="fs-expand-panel">
            <span class="fs-expand-label">任务描述</span>
            <span class="fs-expand-content">{{ scope.row.description || '暂无描述' }}</span>
          </div>
        </template>
      </el-table-column>
      <TableColumn :columns="columns">
        <template #submitter="scope">
          {{ scope.row.processInstanceInfo?.startUserInfo?.name || scope.row.processInstanceInfo?.startUserIdName || scope.row.processInstanceInfo?.startUserId }}
        </template>
      </TableColumn>
      <el-table-column label="操作" width="140">
        <template #default="scope">
          <el-button link v-permit="['oa:workflow:', 'oa:approve:workflow']" @click="() => handleClaim(scope, false)">签收</el-button>
          <el-button link v-permit="['oa:workflow:', 'oa:approve:workflow']" @click="() => handleClaim(scope, true)">办理</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" @change="handleRefresh(true, true)" />
  </el-card>
</template>

<style lang="scss" scoped>
// 展开详情：去掉默认的 20px/50px 内边距，改为与表格内容对齐的浅底面板
:deep(.el-table__expanded-cell) {
  padding: 0;
  background: var(--el-fill-color-lighter);
}
.fs-expand-panel {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  font-size: 13px;
  line-height: 22px;
}
.fs-expand-label {
  flex: none;
  color: var(--el-text-color-secondary);
}
.fs-expand-content {
  flex: 1;
  min-width: 0;
  color: var(--el-text-color-regular);
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
