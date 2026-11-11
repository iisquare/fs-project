<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { AgentKind } from '@renderer/db/AgentDb'
import { markdownToPlainText } from '@renderer/core/MarkdownText'
import { useAgentStore, type AgentRunView } from '@renderer/stores/agents'

const props = defineProps<{
  conversationId: string
  projectId: string
}>()

const store = useAgentStore()
const activeStatuses = ['queued', 'running']
const runs = computed(() => store.forConversation(props.conversationId))
const live = computed(() =>
  runs.value.filter((run) => [...activeStatuses, 'waiting_user'].includes(run.status))
)
const history = computed(() =>
  runs.value.filter((run) => ![...activeStatuses, 'waiting_user'].includes(run.status))
)
const sections = computed(() =>
  [
    { key: 'live', title: '正在处理', runs: live.value },
    { key: 'history', title: '最近任务', runs: history.value }
  ].filter((section) => section.runs.length > 0)
)
const experienceAgents = computed<AgentKind[]>(() => [
  ...new Set(runs.value.map((run) => run.agent))
])
const labels: Record<string, string> = {
  queued: '等待中',
  running: '运行中',
  waiting_user: '等待用户',
  completed: '已完成',
  failed: '失败',
  canceled: '已停止',
  interrupted: '已中断'
}

const types: Record<string, 'success' | 'warning' | 'danger' | 'info' | 'primary'> = {
  queued: 'info',
  running: 'primary',
  waiting_user: 'warning',
  completed: 'success',
  failed: 'danger',
  canceled: 'info',
  interrupted: 'warning'
}

function agentName(run: AgentRunView): string {
  if ('browser' === run.agent) return 'Browser Agent'
  if ('desktop' === run.agent) return 'Desktop Agent'
  return 'Project Tool Agent'
}

function statusText(run: AgentRunView): string {
  if (run.stalled && 'running' === run.status) return '等待进展'
  return labels[run.status] || run.status
}

function messageText(run: AgentRunView): string {
  return markdownToPlainText(run.message)
}

function elapsed(run: AgentRunView): string {
  const seconds = Math.max(
    0,
    Math.floor(
      ((activeStatuses.includes(run.status) ? store.clock : Date.parse(run.lastActivityAt)) -
        (Date.parse(run.startedAt) || store.clock)) /
        1000
    )
  )
  if (seconds < 60) return `${seconds} 秒`
  const minutes = Math.floor(seconds / 60)
  if (minutes < 60) return `${minutes} 分 ${seconds % 60} 秒`
  return `${Math.floor(minutes / 60)} 时 ${minutes % 60} 分`
}

function canStop(run: AgentRunView): boolean {
  return run.managed && activeStatuses.includes(run.status)
}

function stopAgent(run: AgentRunView): void {
  if (!store.stop(run.sessionId)) return
  ElMessage.success(`${agentName(run)} 已停止`)
}

async function openDetail(run: AgentRunView): Promise<void> {
  try {
    await store.openInspector(run.sessionId)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '打开子 Agent 详情失败')
  }
}

async function clearExperience(agent: AgentKind): Promise<void> {
  if (!props.projectId) return
  const name =
    'browser' === agent
      ? 'Browser Agent'
      : 'desktop' === agent
        ? 'Desktop Agent'
        : 'Project Tool Agent'
  try {
    await ElMessageBox.confirm(
      `确认清空当前项目中 ${name} 沉淀的全部跨任务经验？子 Agent 会话仍会保留。`,
      '清空子 Agent 经验',
      { type: 'warning', confirmButtonText: '清空', cancelButtonText: '取消' }
    )
    await store.clearExperiences(props.projectId, agent)
    ElMessage.success(`已清空 ${name} 的跨任务经验`)
  } catch (error) {
    if (['cancel', 'close'].includes(String(error))) return
    ElMessage.error(error instanceof Error ? error.message : '经验清理失败')
  }
}

function load(): void {
  void store.loadConversation(props.conversationId)
}

onMounted(load)
watch(() => props.conversationId, load)
</script>

<template>
  <div class="agent-activity">
    <el-scrollbar class="agent-panel-scroll">
      <div class="agent-panel-body">
        <el-empty v-if="!runs.length" description="当前对话还没有子 Agent 会话" :image-size="54" />
        <section v-for="section in sections" :key="section.key" class="agent-section">
          <div class="agent-section-head">
            <span>{{ section.title }}</span>
            <span>{{ section.runs.length }} 个</span>
          </div>
          <div
            v-for="run in section.runs"
            :key="run.sessionId"
            class="agent-row"
            :class="[run.status, { stalled: run.stalled }]"
            :title="run.sessionId"
          >
            <div class="agent-row-head">
              <span class="agent-state" aria-hidden="true" />
              <strong>{{ agentName(run) }}</strong>
              <el-tag size="small" effect="plain" :type="types[run.status] || 'info'">
                {{ statusText(run) }}
              </el-tag>
              <span class="agent-time">{{ elapsed(run) }}</span>
              <div class="agent-actions">
                <el-button link type="primary" size="small" @click="openDetail(run)">
                  详情
                </el-button>
                <el-button
                  v-if="canStop(run)"
                  class="agent-stop"
                  link
                  type="danger"
                  size="small"
                  @click="stopAgent(run)"
                >
                  停止
                </el-button>
              </div>
            </div>
            <div class="agent-objective" :title="run.objective">{{ run.objective }}</div>
            <div class="agent-detail">
              <span v-if="run.round">第 {{ run.round }} 轮</span>
              <span v-if="run.tool">{{ run.tool }}</span>
              <span v-if="run.reused">已续接</span>
              <span v-if="run.traceCount">{{ run.traceCount }} 次工具调用</span>
              <span v-if="run.stalled">超过 2 分钟无进展</span>
              <span v-if="activeStatuses.includes(run.status) && !run.managed">等待恢复检测</span>
              <span v-if="run.message" class="agent-result" :title="messageText(run)">
                {{ messageText(run) }}
              </span>
            </div>
          </div>
        </section>
        <div v-if="experienceAgents.length" class="agent-memory">
          <div class="agent-memory-head">
            <span class="agent-memory-title">跨任务经验</span>
            <div class="agent-memory-actions">
              <button
                v-for="agent in experienceAgents"
                :key="agent"
                type="button"
                @click="clearExperience(agent)"
              >
                清空
                {{
                  'browser' === agent ? 'Browser' : 'desktop' === agent ? 'Desktop' : 'Project Tool'
                }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </el-scrollbar>
  </div>
</template>

<style lang="scss" scoped>
.agent-activity {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  container-type: inline-size;
}

.agent-panel-scroll {
  flex: 1;
  min-height: 0;

  :deep(> .el-scrollbar__wrap > .el-scrollbar__view) {
    padding: 10px 12px 16px;
  }
}

.agent-panel-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.agent-section {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.agent-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 3px;
  color: var(--el-text-color-placeholder);
  font-size: 11px;

  span:last-child {
    font-variant-numeric: tabular-nums;
  }
}

.agent-memory {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px 2px 0;
  border-top: 1px solid var(--el-border-color-lighter);
  color: var(--el-text-color-placeholder);
  font-size: 11px;
}

.agent-memory-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.agent-memory-title {
  flex: none;
  color: var(--el-text-color-secondary);
}

.agent-memory-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 4px 10px;

  button {
    padding: 0;
    border: none;
    background: transparent;
    color: var(--el-color-primary);
    font: inherit;
    cursor: pointer;

    &:hover {
      color: var(--el-color-danger);
    }
  }
}

.agent-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 8px 10px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  background: var(--el-bg-color);

  &:hover {
    border-color: var(--el-color-primary-light-7);
  }

  &.running,
  &.queued {
    border-color: var(--el-color-primary-light-7);
    background: linear-gradient(135deg, var(--el-color-primary-light-9), var(--el-bg-color));

    .agent-state {
      animation: agent-pulse 1s ease-in-out infinite;
    }
  }

  &.waiting_user {
    border-color: var(--el-color-warning-light-7);
    background: linear-gradient(135deg, var(--el-color-warning-light-9), var(--el-bg-color));
  }

  &.stalled {
    border-color: var(--el-color-warning-light-5);
  }
}

.agent-row-head {
  display: flex;
  align-items: center;
  gap: 5px;
  min-width: 0;

  strong {
    flex: 1;
    min-width: 0;
    overflow: hidden;
    color: var(--el-text-color-primary);
    font-size: 12px;
    font-weight: 600;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  :deep(.el-tag) {
    flex: none;
    height: 18px;
    padding: 0 5px;
    font-size: 10px;
  }
}

.agent-state {
  flex: none;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--el-text-color-placeholder);
}

.agent-row.running .agent-state,
.agent-row.queued .agent-state {
  background: var(--el-color-primary);
}

.agent-row.waiting_user .agent-state {
  background: var(--el-color-warning);
}

.agent-row.completed .agent-state {
  background: var(--el-color-success);
}

.agent-row.failed .agent-state {
  background: var(--el-color-danger);
}

.agent-time {
  margin-left: auto;
  flex: none;
  color: var(--el-text-color-placeholder);
  font-size: 10px;
  font-variant-numeric: tabular-nums;
}

.agent-objective {
  display: -webkit-box;
  overflow: hidden;
  color: var(--el-text-color-regular);
  font-size: 12px;
  line-height: 1.45;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.agent-detail {
  display: flex;
  align-items: center;
  gap: 4px 8px;
  min-width: 0;
  overflow: hidden;
  color: var(--el-text-color-placeholder);
  font-size: 11px;
  white-space: nowrap;

  > span {
    flex: none;
  }
}

.agent-result {
  flex: 1 !important;
  min-width: 60px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.agent-actions {
  display: flex;
  flex: none;
  align-items: center;
  gap: 0;
  margin-left: 2px;

  :deep(.el-button + .el-button) {
    margin-left: 0;
  }
}

.agent-stop {
  align-self: flex-start;
  flex: none;
}

@container (max-width: 300px) {
  .agent-time {
    display: none;
  }

  .agent-objective {
    -webkit-line-clamp: 1;
  }
}

@keyframes agent-pulse {
  0%,
  100% {
    opacity: 0.35;
  }

  50% {
    opacity: 1;
  }
}
</style>
