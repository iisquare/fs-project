<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { FormInstance, TableInstance } from 'element-plus'
import { ElMessage } from 'element-plus'
import RouteUtil from '@/utils/RouteUtil'
import { useRoute, useRouter } from 'vue-router'
import ApiUtil from '@/utils/ApiUtil'
import TableUtil from '@/utils/TableUtil'
import DateUtil from '@/utils/DateUtil'
import WorkflowApi from '@/api/oa/WorkflowApi'
import UserApi from '@/api/member/UserApi'
import ProcessStatus from '../approve/ProcessStatus.vue'

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
const config = ref<any>({
  ready: false,
  finishStatus: {},
  deleteStatus: {},
})
const rows = ref([])
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route, {}))
const pagination = ref(RouteUtil.pagination(filters.value))
const reasonVisible = ref(false)
const reasonLoading = ref(false)
const reasonAction = ref('reject')
const reasonRow = ref<any>({})
const reasonForm = ref<any>({ local: false, reason: '' })

const reasonTitle = computed(() => {
  return reasonAction.value === 'reject' ? '驳回单据' : '撤销单据'
})

const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  WorkflowApi.searchHistory(filters.value).then((result: any) => {
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
    query: { processInstanceId: scope.row.id }
  })
}

const handleToggle = (scope: any, active: boolean) => {
  const text = active ? '激活' : '挂起'
  TableUtil.confirm(`确认${text}流程吗？`).then(() => {
    loading.value = true
    const api = active ? WorkflowApi.activateProcessInstance : WorkflowApi.suspendProcessInstance
    api({ processInstanceId: scope.row.id }, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {
      loading.value = false
    })
  }).catch(() => {})
}

const handleReason = (scope: any, action: string) => {
  reasonAction.value = action
  reasonRow.value = scope.row
  reasonForm.value = { local: false, reason: '' }
  reasonVisible.value = true
}

const handleReasonSubmit = () => {
  if (!reasonForm.value.reason) {
    ElMessage.warning('请输入原因')
    return
  }
  reasonLoading.value = true
  const api = reasonAction.value === 'reject' ? WorkflowApi.reject : WorkflowApi.deleteProcessInstance
  api({
    processInstanceId: reasonRow.value.id,
    local: reasonForm.value.local,
    reason: reasonForm.value.reason,
  }, { success: true }).then(() => {
    reasonVisible.value = false
    handleRefresh(false, true)
  }).catch(() => {}).finally(() => {
    reasonLoading.value = false
  })
}

const handleRemove = (scope: any) => {
  TableUtil.confirm(`确认删除[ID=${scope.row.id}，业务编号=${scope.row.businessKey || '无'}]的流程吗？`).then(() => {
    loading.value = true
    WorkflowApi.deleteHistoricProcessInstance({ processInstanceId: scope.row.id }, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {
      loading.value = false
    })
  }).catch(() => {})
}
</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-search" v-show="searchable">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="流程名称" prop="deploymentId">
        <form-select v-model="filters.deploymentId" :callback="WorkflowApi.list" field-value="deploymentId" clearable placeholder="输入流程名称检索" />
      </form-search-item>
      <form-search-item label="发起人" prop="submitter">
        <form-select v-model="filters.submitter" :callback="UserApi.list" clearable placeholder="输入用户名称检索" />
      </form-search-item>
      <form-search-item label="参与人" prop="involvedUserId">
        <form-select v-model="filters.involvedUserId" :callback="UserApi.list" clearable placeholder="输入用户名称检索" />
      </form-search-item>
      <form-search-item label="完成状态" prop="finishStatus">
        <el-select v-model="filters.finishStatus" placeholder="请选择" clearable>
          <el-option v-for="(value, key) in config.finishStatus" :key="key" :value="key" :label="value" />
        </el-select>
      </form-search-item>
      <form-search-item label="删除状态" prop="deleteStatus">
        <el-select v-model="filters.deleteStatus" placeholder="请选择" clearable>
          <el-option v-for="(value, key) in config.deleteStatus" :key="key" :value="key" :label="value" />
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
      <el-table-column label="操作" width="130">
        <template #default="scope">
          <el-space>
            <el-button link v-permit="'oa:workflow:process'" @click="(e: any) => handleViewProcess(scope, e)">查看</el-button>
            <el-dropdown>
              <el-button link>更多</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="!scope.row.endTime" v-permit="'oa:workflow:reject'" @click="() => handleReason(scope, 'reject')">驳回</el-dropdown-item>
                  <el-dropdown-item v-if="!scope.row.endTime" v-permit="'oa:workflow:deleteProcessInstance'" @click="() => handleReason(scope, 'deleteProcessInstance')">撤销</el-dropdown-item>
                  <el-dropdown-item v-if="scope.row.processInstanceInfo?.id && !scope.row.processInstanceInfo?.isSuspended" v-permit="'oa:workflow:suspendProcessInstance'" @click="() => handleToggle(scope, false)">挂起</el-dropdown-item>
                  <el-dropdown-item v-if="scope.row.processInstanceInfo?.id && scope.row.processInstanceInfo?.isSuspended" v-permit="'oa:workflow:activateProcessInstance'" @click="() => handleToggle(scope, true)">激活</el-dropdown-item>
                  <el-dropdown-item v-if="scope.row.endTime" v-permit="'oa:workflow:deleteHistoricProcessInstance'" @click="() => handleRemove(scope)">删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </el-space>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" @change="handleRefresh(true, true)" />
  </el-card>

  <el-dialog v-model="reasonVisible" :title="reasonTitle" :close-on-click-modal="false" width="500">
    <el-form label-width="80px">
      <el-form-item label="流程实例">{{ reasonRow.id }}</el-form-item>
      <el-form-item label="原因">
        <el-input type="textarea" v-model="reasonForm.reason" :rows="3" placeholder="请输入原因" />
      </el-form-item>
      <el-form-item label="">
        <el-checkbox v-model="reasonForm.local">仅内部可见</el-checkbox>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="reasonVisible = false">取消</el-button>
      <el-button type="primary" @click="handleReasonSubmit" :loading="reasonLoading">确定</el-button>
    </template>
  </el-dialog>
</template>

<style lang="scss" scoped>
</style>
