<script setup lang="ts">
import { onMounted, ref } from 'vue';
import type { FormInstance, TableInstance } from 'element-plus';
import RouteUtil from '@/utils/RouteUtil'
import { useRoute, useRouter } from 'vue-router';
import UsageApi from '@/api/lm/UsageApi';
import DateUtil from '@/utils/DateUtil';
import TableUtil from '@/utils/TableUtil';
import UserApi from '@/api/member/UserApi';
import ModelApi from '@/api/lm/ModelApi';
import AuthApi from '@/api/lm/AuthApi';
import ProviderApi from '@/api/lm/ProviderApi';

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const columns = ref([
  { prop: 'id', label: 'ID' },
  { prop: 'uid', label: '用户ID', hide: true },
  { prop: 'uidUserInfo.name', label: '用户名称' },
  { prop: 'authInfo.name', label: '密钥', hide: true },
  { prop: 'type', label: '计费类型', hide: true },
  { prop: 'place', label: '调用服务' },
  { prop: 'creditAmount', label: '积分数量', hide: true },
  { prop: 'status', label: '执行状态', hide: true },
  { prop: 'providerInfo.name', label: '供应商', hide: true },
  { prop: 'modelInfo.name', label: '模型', hide: true },
  { prop: 'requestIp', label: 'IP', hide: true },
  { prop: 'requestStream', label: '流式', hide: true },
  { prop: 'requestSystem', label: '系统提示词', hide: true },
  { prop: 'requestUser', label: '用户提问' },
  { prop: 'requestPrompt', label: '上下文', hide: true },
  { prop: 'responseReason', label: '思考', hide: true },
  { prop: 'responseCompletion', label: '回答' },
  { prop: 'responseBody', label: '响应内容', hide: true },
  { prop: 'responseTool', label: '工具调用', hide: true },
  { prop: 'finishReason', label: '完成状态' },
  { prop: 'finishDetail', label: '详细原因', hide: true },
  { prop: 'usagePromptCachedTokens', label: '缓存Token', hide: true },
  { prop: 'usagePromptTokens', label: '输入Token', hide: true },
  { prop: 'usageCompletionTokens', label: '输出Token', hide: true },
  { prop: 'usageTotalTokens', label: '总Token', hide: true },
  { prop: 'auditReason', label: '审核', hide: true },
  { prop: 'auditDetail', label: '审核描述', hide: true },
  { prop: 'beginTime', label: '处理开始时间', formatter: DateUtil.render },
  { prop: 'endTime', label: '处理结束时间', formatter: DateUtil.render, hide: true },
  { prop: 'coastTotal', label: '整体耗时(ms)', hide: true },
  { prop: 'auditTime', label: '审核时间', formatter: DateUtil.render, hide: true },
  { prop: 'auditUid', label: '审核人员', hide: true },
])

const rows = ref([])
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route, { advanced: false }))
const pagination = ref(RouteUtil.pagination(filters.value))
const selection = ref([])
const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  UsageApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data.rows
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}
onMounted(() => {
  handleRefresh(false, true)
})
const isAudit = ref(false)
const formVisible = ref(false)
const formLoading = ref(false)
const form: any = ref({})
const formRef: any = ref<FormInstance>()
const rules = ref({
  auditReason: [{ required: true, message: '请选择审核标签', trigger: 'change' }]
})
const handleShow = (scope: any) => {
  form.value = Object.assign({}, scope.row)
  isAudit.value = false
  formVisible.value = true
}
const handleEdit = (scope: any) => {
  form.value = Object.assign({}, scope.row)
  isAudit.value = true
  formVisible.value = true
}
const handleSubmit = () => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid || formLoading.value) return
    formLoading.value = true
    const param = {
      id: form.value.id,
      auditReason: form.value.auditReason,
      auditDetail: form.value.auditDetail,
    }
    UsageApi.audit(param, { success: true }).then(() => {
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
    UsageApi.delete(ids, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {})
  }).catch(() => {})
}
</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-search" v-show="searchable">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="用户" prop="uid">
        <form-select v-model="filters.uid" :callback="UserApi.list" clearable />
      </form-search-item>
      <form-search-item label="密钥" prop="authId">
          <form-select v-model="filters.authId" :callback="AuthApi.list" clearable />
        </form-search-item>
      <form-search-item label="计费类型" prop="type">
        <el-input v-model="filters.type" clearable />
      </form-search-item>
      <form-search-item label="执行状态" prop="status">
        <el-input v-model="filters.status" clearable />
      </form-search-item>
      <form-search-item label="调用服务" prop="place">
        <el-input v-model="filters.place" clearable />
      </form-search-item>
      <form-search-item label="提供商" prop="providerId">
        <form-select v-model="filters.providerId" :callback="ProviderApi.list" clearable />
      </form-search-item>
      <form-search-item label="调用模型" prop="modelId">
        <form-select v-model="filters.modelId" :callback="ModelApi.list" clearable />
      </form-search-item>
      <form-search-item>
        <el-button type="primary" @click="handleRefresh(true, false)">查询</el-button>
        <el-button @click="filterRef?.resetFields()">重置</el-button>
        <button-advanced v-model="filters.advanced" />
      </form-search-item>
      <template v-if="filters.advanced">
        <form-search-item label="请求内容" prop="requestPrompt">
          <el-input v-model="filters.requestPrompt" clearable />
        </form-search-item>
        <form-search-item label="响应内容" prop="responseCompletion">
          <el-input v-model="filters.responseCompletion" clearable />
        </form-search-item>
        <form-search-item label="结束原因" prop="finishReason">
          <el-input v-model="filters.finishReason" clearable />
        </form-search-item>
        <form-search-item label="详细原因" prop="finishDetail">
          <el-input v-model="filters.finishDetail" clearable />
        </form-search-item>
        <form-search-item label="处理开始时间" prop="beginTimeBegin">
          <form-date-picker v-model="filters.beginTimeBegin" placeholder="开始时间" />
        </form-search-item>
        <form-search-item label="处理结束时间" prop="beginTimeEnd">
          <form-date-picker v-model="filters.beginTimeEnd" placeholder="结束时间" />
        </form-search-item>
        <form-search-item label="完成开始时间" prop="endTimeBegin">
          <form-date-picker v-model="filters.endTimeBegin" placeholder="开始时间" />
        </form-search-item>
        <form-search-item label="完成结束时间" prop="endTimeEnd">
          <form-date-picker v-model="filters.endTimeEnd" placeholder="结束时间" />
        </form-search-item>
        <form-search-item label="请求IP" prop="requestIp">
          <el-input v-model="filters.requestIp" clearable />
        </form-search-item>
        <form-search-item label="审核标签" prop="auditReason">
          <el-input v-model="filters.auditReason" clearable />
        </form-search-item>
        <form-search-item label="审核开始时间" prop="auditTimeBegin">
          <form-date-picker v-model="filters.auditTimeBegin" placeholder="开始时间" />
        </form-search-item>
        <form-search-item label="审核结束时间" prop="auditTimeEnd">
          <form-date-picker v-model="filters.auditTimeEnd" placeholder="结束时间" />
        </form-search-item>
      </template>
    </form-search>
  </el-card>
  <el-card :bordered="false" shadow="never" class="fs-table-card">
    <div class="fs-table-toolbar flex-between">
      <el-space>
        <button-delete v-permit="'lm:usage:delete'" :disabled="selection.length === 0" @click="handleDelete" />
      </el-space>
      <el-space>
        <button-search @click="searchable = !searchable" />
        <button-refresh @click="handleRefresh(true, true)" :loading="loading" />
        <TableColumnSetting v-model="columns" :table="tableRef" />
        <TableSort v-model="filters.sort" :columns="columns" sortable="id,beginTime,endTime,coastTotal.desc,creditAmount" @change="handleRefresh(true, true)" />
      </el-space>
    </div>
    <el-table
      ref="tableRef"
      :data="rows"
      :row-key="record => record.id"
      :border="true"
      v-loading="loading"
      table-layout="auto"
      @selection-change="newSelection => selection = newSelection"
    >
      <el-table-column type="selection" />
      <TableColumn :columns="columns" />
      <el-table-column label="操作" width="110px">
        <template #default="scope">
          <el-button link @click="handleShow(scope)" v-permit="'lm:usage:'">查看</el-button>
          <el-button link @click="handleEdit(scope)" v-permit="'lm:usage:audit'">审核</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" @change="handleRefresh(true, true)" />
  </el-card>
  <el-drawer v-model="formVisible" :close-on-click-modal="false" :show-close="false" :destroy-on-close="true" size="80%">
    <template #header="{ close, titleId, titleClass }">
      <h4 :id="titleId" :class="titleClass">{{ '信息' + (isAudit ? ('审核 - ' + form.id) : '查看') }}</h4>
      <el-space>
        <el-button type="primary" @click="handleSubmit" :loading="formLoading" v-if="isAudit">确定</el-button>
        <el-button @click="close">取消</el-button>
      </el-space>
    </template>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="auto">
      <template v-if="isAudit">
        <el-form-item label="审核原因" prop="auditReason"><dictionary-select v-model="form.auditReason" dictionary="audit-tag" multiple filterable allow-create /></el-form-item>
        <el-form-item label="审核描述" prop="auditDetail"><el-input type="textarea" v-model="form.auditDetail" /></el-form-item>
      </template>
      <el-descriptions border>
        <el-descriptions-item label="用户ID">{{ form.uidUserInfo?.name }}[{{ form.uid }}]</el-descriptions-item>
        <el-descriptions-item label="密钥">{{ form.authInfo?.name }}</el-descriptions-item>
        <el-descriptions-item label="计费类型">{{ form.type }}</el-descriptions-item>
        <el-descriptions-item label="调用服务">{{ form.place }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ form.providerInfo?.name }}</el-descriptions-item>
        <el-descriptions-item label="模型">{{ form.modelInfo?.name }}</el-descriptions-item>
        <el-descriptions-item label="积分数量">{{ form.creditAmount }}</el-descriptions-item>
        <el-descriptions-item label="执行状态">{{ form.status }}</el-descriptions-item>
        <el-descriptions-item label="完成状态">{{ form.finishReason }}</el-descriptions-item>
        <el-descriptions-item label="处理开始时间">{{ DateUtil.format(form.beginTime) }}</el-descriptions-item>
        <el-descriptions-item label="处理结束时间">{{ DateUtil.format(form.endTime) }}</el-descriptions-item>
        <el-descriptions-item label="整体耗时(ms)">{{ form.coastTotal }}</el-descriptions-item>
        <el-descriptions-item label="客户端IP">{{ form.requestIp }}</el-descriptions-item>
        <el-descriptions-item label="流式输出">{{ form.requestStream }}</el-descriptions-item>
        <el-descriptions-item label="缓存Token">{{ form.usagePromptCachedTokens }}</el-descriptions-item>
        <el-descriptions-item label="输入Token">{{ form.usagePromptTokens }}</el-descriptions-item>
        <el-descriptions-item label="输出Token">{{ form.usageCompletionTokens }}</el-descriptions-item>
        <el-descriptions-item label="总Token">{{ form.usageTotalTokens }}</el-descriptions-item>
        <el-descriptions-item label="系统提示词" :span="3"><el-input type="textarea" v-model="form.requestSystem" :rows="3" /></el-descriptions-item>
        <el-descriptions-item label="用户提问" :span="3"><el-input type="textarea" v-model="form.requestUser" :rows="3" /></el-descriptions-item>
        <el-descriptions-item label="上下文" :span="3"><el-input type="textarea" v-model="form.requestPrompt" :rows="5" /></el-descriptions-item>
        <el-descriptions-item label="思考" :span="3"><el-input type="textarea" v-model="form.responseReason" :rows="5" /></el-descriptions-item>
        <el-descriptions-item label="回答" :span="3"><el-input type="textarea" v-model="form.responseCompletion" :rows="5" /></el-descriptions-item>
        <el-descriptions-item label="工具调用" :span="3"><el-input type="textarea" v-model="form.responseTool" :rows="3" /></el-descriptions-item>
        <el-descriptions-item label="请求参数" :span="3"><el-input type="textarea" v-model="form.requestBody" :rows="5" /></el-descriptions-item>
        <el-descriptions-item label="响应内容" :span="3"><el-input type="textarea" v-model="form.responseBody" :rows="5" /></el-descriptions-item>
        <el-descriptions-item label="详细原因" :span="3"><el-input type="textarea" v-model="form.finishDetail" :rows="3" /></el-descriptions-item>
        <el-descriptions-item label="审核标签">{{ form.auditReason }}</el-descriptions-item>
        <el-descriptions-item label="审核人员">{{ form.auditUserInfo?.name }}</el-descriptions-item>
        <el-descriptions-item label="审核时间">{{ DateUtil.format(form.auditTime) }}</el-descriptions-item>
        <el-descriptions-item label="审核描述" :span="3"><el-input type="textarea" v-model="form.auditDetail" :rows="3" /></el-descriptions-item>

      </el-descriptions>
    </el-form>
  </el-drawer>
</template>

<style lang="scss" scoped>
</style>
