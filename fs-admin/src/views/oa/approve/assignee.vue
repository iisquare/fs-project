<script setup lang="ts">
import { onMounted, ref } from 'vue'
import type { FormInstance, TableInstance } from 'element-plus'
import { ElMessage } from 'element-plus'
import RouteUtil from '@/utils/RouteUtil'
import { useRoute, useRouter } from 'vue-router'
import ApproveApi from '@/api/oa/ApproveApi'
import UserApi from '@/api/member/UserApi'
import WorkflowApi from '@/api/oa/WorkflowApi'
import DateUtil from '@/utils/DateUtil'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
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
  { prop: 'claimTime', label: '签收时间', formatter: DateUtil.render },
])
const rows = ref([])
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route, {}))
const pagination = ref(RouteUtil.pagination(filters.value))
const revokeVisible = ref(false)
const revokeLoading = ref(false)
const revokeRow = ref<any>({})
const revokeReason = ref('')

const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  ApproveApi.searchAssignee(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data.rows
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

onMounted(() => {
  handleRefresh(false, true)
})

// 仅本人签收的任务可撤销
const revocable = (row: any) => {
  return Number.parseInt(row.assignee) === Number.parseInt((userStore as any)?.info?.id)
}

const handleView = (scope: any, env: Event) => {
  RouteUtil.forward(route, router, env, {
    path: '/oa/approve/process',
    query: { processInstanceId: scope.row.processInstanceId, taskId: scope.row.id }
  })
}

const handleTransact = (scope: any, env: Event) => {
  RouteUtil.forward(route, router, env, {
    path: '/oa/approve/transact',
    query: { taskId: scope.row.id }
  })
}

const handleRevoke = (scope: any) => {
  revokeRow.value = scope.row
  revokeReason.value = ''
  revokeVisible.value = true
}

const handleRevokeSubmit = () => {
  if (!revokeReason.value) {
    ElMessage.warning('请输入撤销原因')
    return
  }
  revokeLoading.value = true
  ApproveApi.revocation({
    processInstanceId: revokeRow.value.processInstanceId,
    taskId: revokeRow.value.id,
    reason: revokeReason.value,
  }, { success: true }).then(() => {
    revokeVisible.value = false
    handleRefresh(false, true)
  }).catch(() => {}).finally(() => {
    revokeLoading.value = false
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
      <el-table-column label="操作" width="160">
        <template #default="scope">
          <el-button link @click="(e: any) => handleView(scope, e)">查看</el-button>
          <el-button link v-permit="['oa:workflow:', 'oa:approve:workflow']" @click="(e: any) => handleTransact(scope, e)">办理</el-button>
          <el-button link type="danger" v-if="revocable(scope.row)" @click="() => handleRevoke(scope)">撤销</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" @change="handleRefresh(true, true)" />
  </el-card>

  <el-dialog v-model="revokeVisible" title="撤销流程" :close-on-click-modal="false" width="500">
    <el-form label-width="80px">
      <el-form-item label="任务ID">{{ revokeRow.id }}</el-form-item>
      <el-form-item label="撤销原因">
        <el-input type="textarea" v-model="revokeReason" :rows="3" placeholder="请输入撤销原因" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="revokeVisible = false">取消</el-button>
      <el-button type="primary" @click="handleRevokeSubmit" :loading="revokeLoading">确定</el-button>
    </template>
  </el-dialog>
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
