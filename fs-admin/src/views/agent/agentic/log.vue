<script setup lang="ts">
/**
 * 编排运行日志 - 记录每次调试运行与外部调用的执行情况：
 * 列表展示编排、来源、状态与耗时，详情里给出入参、回复内容、各节点执行步骤与失败原因，
 * 便于定位画布配置或运行时的问题。
 */
import { computed, onMounted, ref } from 'vue'
import type { FormInstance, TableInstance } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import AgenticApi from '@/api/agent/AgenticApi'
import AgentApiKeyApi from '@/api/agent/ApiKeyApi'
import UserApi from '@/api/member/UserApi'
import StepDetail from '@/components/Agentic/StepDetail.vue'
import ChatTextBlock from '@/components/Chat/ChatTextBlock.vue'
import ApiUtil from '@/utils/ApiUtil'
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
  { prop: 'agenticName', label: '编排名称' },
  { prop: 'inputs', label: '用户输入', minWidth: 220, showOverflowTooltip: true, slot: 'input' },
  { prop: 'chatId', label: '会话标识' },
  { prop: 'sourceText', label: '来源' },
  { prop: 'version', label: '版本' },
  { prop: 'statusText', label: '状态', slot: 'status' },
  { prop: 'duration', label: '耗时(毫秒)' },
  { prop: 'createdUid', label: '调用人', slot: 'createdUid' },
  { prop: 'callerTypeText', label: '调用方式' },
  { prop: 'apiKeyName', label: 'Agent API Key' },
  { prop: 'userProxy', label: '用户代理', slot: 'userProxy', hide: true },
  { prop: 'externalUserId', label: '外部用户', hide: true },
  { prop: 'traceId', label: '追踪标识', hide: true },
  { prop: 'error', label: '失败原因', hide: true, showOverflowTooltip: true },
  { prop: 'createdTime', label: '调用时间', formatter: DateUtil.render },
  { prop: 'ip', label: '来源IP', hide: true },
])
const config: any = ref({ sorts: {}, sources: {}, status: {} })
const rows = ref([])
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route, {
  advanced: false,
  deleted: 'without',
  agenticId: '',
  createdUid: '',
  apiKeyId: '',
}))
const pagination = ref(RouteUtil.pagination(filters.value))
const selection: any = ref([])

const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  AgenticApi.logList(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = ApiUtil.data(result)?.rows ?? []
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}
onMounted(() => {
  handleRefresh(false, true)
})

/* ------------------------------- 详情 ------------------------------- */

const infoVisible = ref(false)
const info: any = ref({})
const infoLoading = ref(false)
// 步骤表：节点、类型、状态与耗时
const steps = computed<any[]>(() => info.value?.steps ?? [])
const inputsText = computed(() => JSON.stringify(info.value?.inputs ?? {}, null, 2))
const outputsText = computed(() => JSON.stringify(info.value?.outputs ?? {}, null, 2))

const handleShow = (scope: any) => {
  infoVisible.value = true
  infoLoading.value = true
  info.value = {}
  AgenticApi.logInfo(scope.row.id).then((result: any) => {
    info.value = ApiUtil.data(result) ?? {}
  }).catch(() => {}).finally(() => {
    infoLoading.value = false
  })
}

const handleDelete = () => {
  TableUtil.selection(selection.value).then((ids: any) => {
    loading.value = true
    AgenticApi.logDelete(ids, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {
      loading.value = false
    })
  }).catch(() => {})
}

// 来源与状态文案：与后端字典一致（source：draft 调试运行 / published 外部调用）
const sourceText = (source: string) => 'published' === source ? '外部调用' : '调试运行'
const statusText = (status: number) => 2 === status ? '失败' : '成功'

</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-search" v-show="searchable">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="" prop="deleted">
        <form-deleted v-model="filters.deleted" @change="handleRefresh(true, false)" />
      </form-search-item>
      <!-- 编排筛选：下拉选择编排（按 agenticId 过滤，名称由后端填充展示） -->
      <form-search-item label="编排流程" prop="agenticId">
        <form-select
          v-model="filters.agenticId"
          clearable
          filterable
          :callback="AgenticApi.list"
          placeholder="请选择编排" />
      </form-search-item>
      <!-- 首行：删除状态（无标题）、调用人 + 操作按钮，其它条件展开后展示 -->
      <form-search-item label="调用人" prop="createdUid">
        <form-select v-model="filters.createdUid" clearable filterable :callback="UserApi.list" placeholder="请选择调用人" />
      </form-search-item>
      <form-search-item>
        <el-button type="primary" @click="handleRefresh(true, false)" :loading="loading">查询</el-button>
        <el-button @click="filterRef?.resetFields()">重置</el-button>
      </form-search-item>
      <form-search-item label="来源" prop="source">
        <el-select v-model="filters.source" placeholder="请选择" clearable>
          <el-option value="draft" label="调试运行" />
          <el-option value="published" label="外部调用" />
        </el-select>
      </form-search-item>
      <form-search-item label="状态" prop="status">
        <el-select v-model="filters.status" placeholder="请选择" clearable>
          <el-option :value="1" label="成功" />
          <el-option :value="2" label="失败" />
        </el-select>
      </form-search-item>
      <form-search-item label="调用方式" prop="callerType">
        <el-select v-model="filters.callerType" placeholder="请选择" clearable>
          <el-option value="session" label="登录会话" />
          <el-option value="api_key" label="API Key" />
          <el-option value="user_proxy" label="API Key + 用户代理" />
        </el-select>
      </form-search-item>
      <form-search-item label="Agent API Key" prop="apiKeyId">
        <form-select
          v-model="filters.apiKeyId"
          :callback="AgentApiKeyApi.list"
          clearable
          filterable
          placeholder="请选择 API Key"
        />
      </form-search-item>
      <form-search-item label="外部用户" prop="externalUserId">
        <el-input v-model="filters.externalUserId" clearable />
      </form-search-item>
      <form-search-item label="追踪标识" prop="traceId">
        <el-input v-model="filters.traceId" clearable />
      </form-search-item>
      <form-search-item label="创建开始时间" prop="createdTimeBegin">
        <form-date-picker v-model="filters.createdTimeBegin" placeholder="开始时间" />
      </form-search-item>
      <form-search-item label="创建结束时间" prop="createdTimeEnd">
        <form-date-picker v-model="filters.createdTimeEnd" placeholder="结束时间" />
      </form-search-item>
    </form-search>
  </el-card>
  <el-card :bordered="false" shadow="never" class="fs-table-card">
    <div class="fs-table-toolbar flex-between">
      <el-space>
        <button-delete v-permit="'agent:agentic:delete'" :disabled="selection.length === 0" @click="handleDelete" />
      </el-space>
      <el-space>
        <button-search @click="searchable = !searchable" />
        <button-refresh @click="handleRefresh(true, true)" :loading="loading" />
        <TableColumnSetting v-model="columns" :table="tableRef" :loading="loading" />
        <TableSort v-model="filters.sort" :columns="columns" :sortable="config.sorts" :loading="loading" @change="handleRefresh(true, true)" />
      </el-space>
    </div>
    <el-table
      ref="tableRef"
      :data="rows"
      :row-key="(record: any) => record.id"
      :border="true"
      v-loading="loading"
      table-layout="auto"
      @selection-change="(s: any) => selection = s">
      <el-table-column type="selection" />
      <TableColumn :columns="columns">
        <!-- 用户输入由后端从入参里抽好（inputsText，已截断成一行），列表接口不返回 inputs 大字段 -->
        <template #input="scope">{{ scope.row.inputsText }}</template>
        <template #createdUid="scope">{{ scope.row.createdUserInfo?.name || '系统' }}</template>
        <template #userProxy="scope">
          <el-tag :type="scope.row.userProxy ? 'warning' : 'info'" size="small" effect="plain">
            {{ scope.row.userProxy ? '是' : '否' }}
          </el-tag>
        </template>
        <template #status="scope">
          <el-tag :type="2 === scope.row.status ? 'danger' : 'success'" size="small" effect="plain">
            {{ statusText(scope.row.status) }}
          </el-tag>
        </template>
      </TableColumn>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button link @click="handleShow(scope)" v-permit="'agent:agentic:'">查看</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" :loading="loading" @change="handleRefresh(true, true)" />
  </el-card>
  <el-drawer v-model="infoVisible" :title="'运行日志 - ' + (info.id ?? '')" size="60%">
    <!-- v-loading 必须挂在元素上：el-drawer 的根是 Teleport，指令挂在组件上不会生效 -->
    <div class="log-detail" v-loading="infoLoading">
    <el-descriptions :column="2" label-width="100px" border>
      <el-descriptions-item label="编排">{{ info.agenticName }}</el-descriptions-item>
      <el-descriptions-item label="编排标识">{{ info.agenticId || '—' }}</el-descriptions-item>
      <el-descriptions-item label="会话标识">{{ info.chatId || '—' }}</el-descriptions-item>
      <el-descriptions-item label="来源">{{ sourceText(info.source) }}</el-descriptions-item>
      <el-descriptions-item label="版本">{{ info.version > 0 ? 'v' + info.version : '草稿' }}</el-descriptions-item>
      <el-descriptions-item label="状态">
        <el-tag :type="2 === info.status ? 'danger' : 'success'" size="small" effect="plain">{{ statusText(info.status) }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="耗时">{{ info.duration }} 毫秒</el-descriptions-item>
      <el-descriptions-item label="调用时间">{{ DateUtil.format(info.createdTime) }}</el-descriptions-item>
      <el-descriptions-item label="调用人">{{ info.createdUserInfo?.name || '系统' }}</el-descriptions-item>
      <el-descriptions-item label="调用方式">{{ info.callerTypeText }}</el-descriptions-item>
      <el-descriptions-item label="Agent API Key">{{ info.apiKeyName || (info.apiKeyId ? '#' + info.apiKeyId : '—') }}</el-descriptions-item>
      <el-descriptions-item label="API Key 属主">{{ info.apiKeyOwnerUidUserInfo?.name || (info.apiKeyOwnerUid ? '#' + info.apiKeyOwnerUid : '—') }}</el-descriptions-item>
      <el-descriptions-item label="用户代理">{{ info.userProxy ? '是' : '否' }}</el-descriptions-item>
      <el-descriptions-item label="外部用户">{{ info.externalUserId || '—' }}</el-descriptions-item>
      <el-descriptions-item label="追踪标识">{{ info.traceId || '—' }}</el-descriptions-item>
      <el-descriptions-item label="来源IP">{{ info.ip || '暂无' }}</el-descriptions-item>
    </el-descriptions>
    <el-alert class="log-alert" type="error" show-icon :closable="false" :title="info.error" v-if="info.error" />
    <!-- 回复内容：与对话历史一致，直接给出本轮给用户的最终回复 -->
    <template v-if="info.outputs?.answer">
      <el-divider>回复内容</el-divider>
      <ChatTextBlock :text="info.outputs.answer" />
    </template>
    <el-divider>运行入参</el-divider>
    <ChatTextBlock :text="inputsText" />
    <el-divider>执行步骤</el-divider>
    <el-table :data="steps" size="small" border>
      <!-- 展开行：节点输入、解析后入参与完整输出（日志保留完整内容） -->
      <el-table-column type="expand">
        <template #default="scope">
          <StepDetail :step="scope.row" />
        </template>
      </el-table-column>
      <el-table-column prop="name" label="节点" min-width="140" show-overflow-tooltip />
      <el-table-column prop="type" label="类型" width="140" />
      <el-table-column label="容器/迭代" width="110">
        <template #default="scope">
          <el-tag size="small" effect="plain" v-if="scope.row.container">第 {{ Number(scope.row.iteration ?? 0) + 1 }} 次</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="scope">
          <el-tag :type="2 === scope.row.status ? 'danger' : 'success'" size="small" effect="plain">
            {{ 2 === scope.row.status ? '失败' : '成功' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="duration" label="耗时(毫秒)" width="110" />
      <el-table-column prop="error" label="失败原因" show-overflow-tooltip />
    </el-table>
    <el-divider>运行输出</el-divider>
    <ChatTextBlock :text="outputsText" />
    </div>
  </el-drawer>
</template>

<style lang="scss" scoped>
.log-alert {
  margin-top: 12px;
}
/* 展开行里的分组标签：节点输入 / 解析后入参 / 节点输出 */
.step-detail {
  .step-label {
    margin: 6px 0 2px;
    font-size: 12px;
    color: var(--el-text-color-placeholder);
    &:first-child {
      margin-top: 0;
    }
  }
}
</style>
