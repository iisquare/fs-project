<script setup lang="ts">
/**
 * 对话历史 - 编排发布后（以及设计器调试运行）的会话记录：
 * 按标题/类型检索会话，打开后查看完整消息、每轮运行的节点与工具调用明细；
 * 这里只做查看与追溯（不提供继续对话），继续对话在「流程对话」页进行。
 */
import { computed, onMounted, ref } from 'vue'
import type { FormInstance, TableInstance } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import AgenticApi from '@/api/agent/AgenticApi'
import useChatFeedback from '@/composables/useChatFeedback'
import useChatBranch from '@/composables/useChatBranch'
import UserApi from '@/api/member/UserApi'
import AgenticSteps from '@/components/Agentic/AgenticSteps.vue'
import AgenticTimeline from '@/components/Agentic/AgenticTimeline.vue'
import ChatElevator from '@/components/Chat/ChatElevator.vue'
import ChatMessage from '@/components/Chat/ChatMessage.vue'
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
  { prop: 'id', label: '会话标识' },
  { prop: 'title', label: '会话标题', slot: 'title' },
  { prop: 'agenticName', label: '所属编排' },
  { prop: 'typeText', label: '类型' },
  { prop: 'deletedText', label: '状态', slot: 'deletedText' },
  { prop: 'createdUid', label: '创建人', slot: 'createdUid' },
  { prop: 'updatedTime', label: '最后对话', formatter: (row: any) => DateUtil.format(row.updatedTime) },
  { prop: 'createdTime', label: '创建时间', formatter: (row: any) => DateUtil.format(row.createdTime) },
])
const rows = ref([])
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route, { advanced: false, deleted: 'without', agenticId: '', createdUid: '' }))
const pagination = ref(RouteUtil.pagination(filters.value))
const selection: any = ref([])

const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  AgenticApi.chatList(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = ApiUtil.data(result)?.rows ?? []
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}
onMounted(() => {
  handleRefresh(false, true)
})

/* ------------------------------- 会话详情与继续对话 ------------------------------- */

const infoVisible = ref(false)
const infoLoading = ref(false)
const info: any = ref({})
/**
 * 对话分支：消息按 parentId 组成消息树，本页只渲染会话当前分支那条路径，
 * 同一处有多条分支（编辑提问 / 重新生成产生）时按「◀ 2/3 ▶」回看（见 composables/useChatBranch）
 */
const branch = useChatBranch()
const messages = computed<any[]>(() => branch.visible.value)
/** 消息区滚动容器：电梯导航按它测量位置与滚动 */
const chatListRef = ref<HTMLDivElement>()
// 编排信息取自该会话的运行记录（展示所属应用与标识）
const runInfo = computed<any>(() => (info.value?.runs ?? []).length ? info.value.runs[0] : null)

const handleOpen = (scope: any) => {
  infoVisible.value = true
  loadInfo(scope.row.id)
}

const loadInfo = (id: any) => {
  infoLoading.value = true
  return AgenticApi.chatInfo(id).then((result: any) => {
    info.value = ApiUtil.data(result) ?? {}
    // 装载消息树：leafId 是会话当前分支尾，缺失时取最后一条消息
    branch.setMessages(info.value?.messages, info.value?.leafId)
  }).catch(() => {}).finally(() => {
    infoLoading.value = false
  })
}

const handleDelete = () => {
  TableUtil.selection(selection.value).then((ids: any) => {
    loading.value = true
    AgenticApi.chatDelete(ids, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {
      loading.value = false
    })
  }).catch(() => {})
}

/* ------------------------------- 消息反馈 ------------------------------- */

/**
 * 执行过程：消息带 logId 时按需拉取本轮运行日志的完整步骤
 * （节点输入/解析后入参/输出 + 工具调用参数与返回结果），用于定位与追踪问题
 */
const stepCache = ref<Record<string, any>>({})
const stepLoading = ref<Record<string, boolean>>({})
/** 本轮运行日志详情（步骤 + 运行入参 + 运行输出），与运行日志详情页保持同样的内容 */
const logOf = (item: any) => stepCache.value[String(item?.logId ?? '')] ?? null
const stepsOf = (item: any) => logOf(item)?.steps ?? []
const loadSteps = (item: any) => {
  const logId = String(item?.logId ?? '')
  if (!logId || stepCache.value[logId] || stepLoading.value[logId]) return
  stepLoading.value[logId] = true
  // 注意：logInfo 的第一个参数就是日志标识，不能再包一层 { id }
  AgenticApi.logInfo(Number(logId), { warning: false }).then((result: any) => {
    stepCache.value[logId] = ApiUtil.data(result) ?? {}
  }).catch(() => {}).finally(() => {
    stepLoading.value[logId] = false
  })
}

// 消息反馈：与流程对话、调试抽屉共用同一套提交逻辑（见 composables/useChatFeedback）
const { feeding, submit: handleFeedback } = useChatFeedback('该消息不支持反馈')

</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-search" v-show="searchable">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="" prop="deleted">
        <form-deleted v-model="filters.deleted" @change="handleRefresh(true, false)" />
      </form-search-item>
      <!-- 首行：删除状态（无标题）、流程、创建人 + 操作按钮，其它条件展开后展示 -->
      <form-search-item label="编排流程" prop="agenticId">
        <form-select v-model="filters.agenticId" clearable filterable :callback="AgenticApi.list" placeholder="请选择流程" />
      </form-search-item>
      <form-search-item label="创建人" prop="createdUid">
        <form-select v-model="filters.createdUid" clearable filterable :callback="UserApi.list" placeholder="请选择创建人" />
      </form-search-item>
      <form-search-item>
        <el-button type="primary" @click="handleRefresh(true, false)" :loading="loading">查询</el-button>
        <el-button @click="filterRef?.resetFields()">重置</el-button>
      </form-search-item>
      <form-search-item label="会话标题" prop="title">
        <el-input v-model="filters.title" clearable />
      </form-search-item>
      <form-search-item label="类型" prop="type">
        <el-select v-model="filters.type" placeholder="请选择" clearable>
          <el-option value="published" label="发布应用" />
          <el-option value="draft" label="调试运行" />
        </el-select>
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
        <template #deletedText="scope">
          <el-tag :type="scope.row.deletedTime > 0 ? 'info' : 'success'" size="small" effect="plain">
            {{ scope.row.deletedText }}
          </el-tag>
        </template>
        <template #title="scope">
          <el-button link type="primary" @click="handleOpen(scope)">{{ scope.row.title || '新会话' }}</el-button>
        </template>
        <template #createdUid="scope">{{ scope.row.createdUserInfo?.name || '系统' }}</template>
      </TableColumn>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button link @click="handleOpen(scope)" v-permit="'agent:agentic:'">打开</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" :loading="loading" @change="handleRefresh(true, true)" />
  </el-card>
  <!-- 会话详情：默认宽度给足（长消息、表格描述都需要横向空间），并支持拖拽左边缘调整宽度 -->
  <el-drawer
    class="chat-drawer"
    v-model="infoVisible"
    :title="'对话历史 - ' + (info.title || '')"
    size="72%"
    resizable>
    <!-- v-loading 必须挂在元素上：el-drawer 的根是 Teleport，指令挂在组件上不会生效 -->
    <div class="chat-panel" v-loading="infoLoading">
      <div class="chat-head">
        <el-descriptions :column="3" label-width="80px" border size="small">
          <el-descriptions-item label="会话标识">{{ info.id || '—' }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ info.typeText }}</el-descriptions-item>
          <el-descriptions-item label="创建人">{{ info.createdUserInfo?.name || '系统' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ DateUtil.format(info.createdTime) }}</el-descriptions-item>
          <el-descriptions-item label="最后对话">{{ DateUtil.format(info.updatedTime) }}</el-descriptions-item>
          <el-descriptions-item label="编排">{{ runInfo?.agenticName || '暂无运行记录' }}</el-descriptions-item>
          <el-descriptions-item label="编排标识">{{ runInfo?.agenticId || '—' }}</el-descriptions-item>
          <el-descriptions-item label="对话轮数">{{ (info.runs ?? []).length }}</el-descriptions-item>
          <el-descriptions-item label="消息条数">{{ messages.length }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <div class="chat-body">
        <div class="chat-list" ref="chatListRef">
          <ChatMessage
            :key="item.id"
            v-for="item in messages"
            :item="item"
            :avatar="false"
            :time="true"
            :disabled="feeding === item.id"
            :branch="branch.branchOf(item)"
            @switch="(step: number) => branch.switchBranch(item, step)"
            @submit="(payload: any) => handleFeedback(item, payload)">
            <!-- 执行过程（顶部）：与流程对话同一形态的轻量时间线，点开时才拉取日志 -->
            <template #steps>
              <AgenticTimeline
                v-if="'assistant' === item.role && item.logId"
                :steps="stepsOf(item)"
                :progress="item.progress"
                :rounds="item.rounds"
                :streaming="item.streaming"
                :loading="stepLoading[String(item.logId)]"
                :log-id="item.logId"
                @open="loadSteps(item)" />
            </template>
            <!-- 执行明细（底部）：逐节点完整内容，排查用 -->
            <template #extra>
              <AgenticSteps
                class="chat-steps"
                v-if="'assistant' === item.role && item.logId"
                :steps="stepsOf(item)"
                :title="`执行明细（${stepsOf(item).length} 个节点 · 日志 ${item.logId}）`"
                :loading="stepLoading[String(item.logId)]"
                @open="loadSteps(item)">
                <!-- 运行入参与运行输出：与运行日志详情页一致，排查时不必再跳页 -->
                <el-collapse class="chat-raw" v-if="logOf(item)">
                  <el-collapse-item title="运行入参">
                    <ChatTextBlock :value="logOf(item).inputs ?? {}" />
                  </el-collapse-item>
                  <el-collapse-item title="运行输出">
                    <ChatTextBlock :value="logOf(item).outputs ?? {}" />
                  </el-collapse-item>
                </el-collapse>
              </AgenticSteps>
            </template>
          </ChatMessage>
          <el-empty description="暂无对话内容" :image-size="60" v-if="!messages.length" />
        </div>
      <!-- 电梯导航：按用户消息生成右侧横条，点击定位到对应气泡 -->
      <ChatElevator
        :target="chatListRef"
        :revision="messages.length"
        selector="[data-chat-role='user']"
        text-selector="[data-chat-text]" />
      </div>
    </div>
  </el-drawer>
</template>

<style lang="scss" scoped>
/**
 * 对话内容：用户消息靠右、助手消息靠左，与设计器调试面板保持一致的观感
 */
@mixin chat-column {
  width: 100%;
  min-width: min(320px, 100%);
  max-width: 900px;
  margin-left: auto;
  margin-right: auto;
}
$chat-output-font: "PingFang SC", "Microsoft YaHei", "Segoe UI", sans-serif;
$chat-output-size: 13px;
$chat-output-line-height: 1.7;
.chat-panel {
  display: flex;
  flex-direction: column;
  flex: 1;
  gap: 8px;
  height: 100%;
  min-height: 0;
}
.chat-head {
  @include chat-column();
  flex: none;
}
/* 消息区外层：给右侧电梯导航提供定位基准（导航本身不随消息滚动） */
.chat-body {
  position: relative;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}
.chat-list {
  @include chat-column();
  /* 撑满输入区以上的剩余高度：输入区因此始终贴在容器最底部 */
  flex: 1;
  min-height: 0;
  padding: 8px;
  overflow: auto;
  border-radius: 4px;
  background: var(--el-fill-color-lighter);
  /* 消息行：结构与通用样式在 components/Chat/ChatMessage.vue，这里只保留对话历史自己的口径 */
  .chat-message {
    align-items: center;
    gap: 6px;
    /* 本页没有头像：用户消息靠右用行方向实现，不反向排列整行 */
    &.is-user {
      flex-direction: row;
      justify-content: flex-end;
    }
    & + .chat-message {
      margin-top: 12px;
    }
  }
  /* 气泡按内容收起、最宽 92%（本页无头像，不按头像竖线收） */
  :deep(.chat-bubble) {
    display: inline-block;
    max-width: 92%;
  }
  /* 用户消息：操作条与气泡同一个列容器，整块最宽 92%；气泡本身按容器满宽收起 */
  .chat-message.is-user :deep(.chat-main) {
    max-width: 92%;
  }
  .chat-message.is-user :deep(.chat-bubble) {
    max-width: 100%;
  }
  /* 助手输出与对话页共用一套紧凑字阶，长文和表格不会显得过度放大 */
  .chat-message.is-assistant {
    :deep(.chat-bubble) {
      padding: 10px 12px;
    }
    :deep(.chat-markdown) {
      display: block;
      color: var(--el-text-color-primary);
      font-family: $chat-output-font;
      font-size: $chat-output-size;
      line-height: $chat-output-line-height;
    }
    :deep(.chat-markdown .fs-markdown-preview),
    :deep(.chat-markdown .fs-markdown-preview-body.vditor-reset) {
      padding: 0;
      color: inherit;
      font-family: inherit;
      font-size: $chat-output-size;
      line-height: $chat-output-line-height;
    }
    :deep(.chat-markdown .fs-markdown-preview-body) {
      > :first-child {
        margin-top: 0 !important;
      }
      > :last-child {
        margin-bottom: 0 !important;
      }
      p {
        margin: 0 0 8px;
        line-height: inherit;
      }
      h1,
      h2,
      h3,
      h4,
      h5,
      h6 {
        margin: 12px 0 6px;
        font-weight: 600;
        line-height: 1.4;
      }
      h1 {
        font-size: 18px;
      }
      h2 {
        font-size: 16px;
      }
      h3 {
        font-size: 15px;
      }
      h4,
      h5,
      h6 {
        font-size: 13px;
      }
      ul,
      ol {
        margin: 0 0 8px;
        padding-left: 20px;
      }
      li {
        line-height: inherit;
      }
      li + li {
        margin-top: 3px;
      }
      li > p {
        margin: 0 0 3px;
      }
      blockquote {
        margin: 0 0 9px;
        padding: 6px 10px;
        border-left: 3px solid var(--el-color-primary-light-5);
        border-radius: 0 6px 6px 0;
        background: var(--el-fill-color-lighter);
        color: var(--el-text-color-secondary);
      }
      hr {
        margin: 14px 0;
      }
      a {
        color: var(--el-color-primary);
        text-decoration: none;
        &:hover {
          text-decoration: underline;
        }
      }
      img {
        border-radius: 6px;
      }
      code:not(.hljs):not(.highlight-chroma) {
        padding: 1px 4px;
        border-radius: 4px;
        background: var(--el-fill-color);
        font-size: 0.88em;
      }
      pre {
        margin: 10px 0;
        overflow: hidden;
        border: solid 1px var(--el-border-color-lighter);
        border-radius: 7px;
        background: var(--el-fill-color-light);
      }
      pre > code {
        padding: 10px 12px;
        background-color: transparent;
        background-image: none;
        font-size: 11.5px;
        line-height: 1.6;
      }
      table {
        margin-bottom: 10px;
        font-size: 12px;
        line-height: 1.6;
      }
      th,
      td {
        padding: 5px 8px;
      }
      .language-echarts {
        height: 300px !important;
        margin-bottom: 12px;
      }
    }
    :deep(.chat-reasoning-text) {
      font-size: 12px;
      line-height: 1.65;
    }
  }
  /* 折叠面板头：默认 48px 对气泡内的过程信息太高，压到 28px（与调试面板一致） */
  :deep(.chat-reasoning .el-collapse-item__header) {
    height: 28px;
  }
  /* 执行过程：逐节点展开完整明细，表格/长文本都在气泡内滚动 */
  .chat-steps {
    /* 折叠头/节点明细的样式随 AgenticSteps 组件，这里只留气泡内的间距 */
    margin: 8px 0 0;
    /* 运行入参 / 运行输出：内容与运行日志详情页一致 */
    .chat-raw {
      margin-top: 8px;
      &.el-collapse {
        border-top: none;
        border-bottom: none;
      }
      :deep(.el-collapse-item__header) {
        height: 28px;
        font-size: 12px;
        color: var(--el-text-color-secondary);
        border-bottom: none;
      }
      :deep(.el-collapse-item__wrap) {
        border-bottom: none;
      }
    }
  }

}
</style>

<style lang="scss">
/* 会话详情抽屉：拖拽调整宽度时的下限，避免被拖到看不清内容（拖拽条本身由组件提供） */
.chat-drawer {
  min-width: 560px;
}
/* 抽屉内容区铺满高度，消息列表撑开占满整个详情区（只做查看与追溯，没有输入区） */
.chat-drawer .el-drawer__body {
  display: flex;
  flex-direction: column;
  padding: 12px 16px 16px;
  overflow: hidden;
}
</style>
