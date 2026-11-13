<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import {
  WAITING_SETTLED_ERROR,
  type AgentInteractionRound,
  type AgentInteractionTask,
  type AgentStatus
} from '@renderer/db/AgentDb'
import type { AgentRunView } from '@renderer/stores/agents'
import { useAgentStore } from '@renderer/stores/agents'
import type { ChatMessage as ChatMessageView, ChatRound, ChatToolCall } from '@renderer/stores/chat'
import ChatMessage from './ChatMessage.vue'

const props = defineProps<{
  modelValue: boolean
  sessionId: string
  parentConversationId: string
  projectId: string
  run: AgentRunView | null
  vertical?: boolean
  standalone?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const store = useAgentStore()
const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})
const tasks = ref<AgentInteractionTask[]>([])
const loading = ref(false)
const error = ref('')
const selectedTaskId = ref('')
const taskFilter = ref<'all' | 'tools' | 'issues'>('all')
const autoFollow = ref(true)
const AGENT_NAV_WIDTH_MIN = 180
const AGENT_NAV_WIDTH_MAX = 340
const AGENT_NAV_WIDTH_KEY = 'fs_agent_nav_width'

function initialNavWidth(): number {
  try {
    const value = Number(globalThis.localStorage?.getItem(AGENT_NAV_WIDTH_KEY))
    if (Number.isFinite(value) && value >= AGENT_NAV_WIDTH_MIN) {
      return Math.min(AGENT_NAV_WIDTH_MAX, value)
    }
  } catch {
    // 本地偏好不可用时使用默认宽度。
  }
  return 240
}

const navWidth = ref(initialNavWidth())

function setNavWidth(value: string | number): void {
  navWidth.value = Math.max(
    AGENT_NAV_WIDTH_MIN,
    Math.min(AGENT_NAV_WIDTH_MAX, Math.round(Number(value) || 0))
  )
}

function saveNavWidth(): void {
  try {
    globalThis.localStorage?.setItem(AGENT_NAV_WIDTH_KEY, String(navWidth.value))
  } catch {
    // 仅住本次运行即可。
  }
}

interface ScrollbarInstance {
  wrapRef?: HTMLElement | { value?: HTMLElement }
  setScrollTop: (top: number) => void
}

const detailScroll = ref<ScrollbarInstance>()
const filters: Array<{ value: 'all' | 'tools' | 'issues'; label: string }> = [
  { value: 'all', label: '全部' },
  { value: 'tools', label: '工具' },
  { value: 'issues', label: '异常' }
]

const labels: Record<AgentStatus, string> = {
  queued: '等待中',
  running: '运行中',
  waiting_user: '等待用户',
  completed: '已完成',
  failed: '失败',
  canceled: '已停止',
  interrupted: '已中断'
}

const types: Record<AgentStatus, 'success' | 'warning' | 'danger' | 'info' | 'primary'> = {
  queued: 'info',
  running: 'primary',
  waiting_user: 'warning',
  completed: 'success',
  failed: 'danger',
  canceled: 'info',
  interrupted: 'warning'
}

function hasIssues(task: AgentInteractionTask): boolean {
  return (
    ['failed', 'canceled', 'interrupted', 'waiting_user'].includes(task.status) ||
    task.rounds.some((round) =>
      round.calls.some((call) => ['failed', 'stopped', 'not_run'].includes(call.status))
    )
  )
}

const visibleTasks = computed(() => {
  if ('tools' === taskFilter.value) {
    return tasks.value.filter((task) => task.rounds.some((round) => round.calls.length))
  }
  if ('issues' === taskFilter.value) return tasks.value.filter(hasIssues)
  return tasks.value
})

const selectedTask = computed(
  () => visibleTasks.value.find((task) => task.id === selectedTaskId.value) ?? null
)
const selectedTaskError = computed(() => {
  const value = String(selectedTask.value?.error ?? '').trim()
  return WAITING_SETTLED_ERROR === value ? '' : value
})
const currentStatus = computed<AgentStatus>(
  () => selectedTask.value?.status ?? props.run?.status ?? 'running'
)
const agentLabel = computed(() => {
  const agent = props.run?.agent
  if (!agent) return '子 Agent'
  if ('project_tools' === agent) return 'Project Tool Agent'
  if ('desktop' === agent) return 'Desktop Agent'
  return 'Browser Agent'
})
const currentAction = computed(() => {
  const task = selectedTask.value
  if (!task) return ''
  const runningCall = task.rounds
    .flatMap((round) => round.calls)
    .find((call) => 'running' === call.status)
  if (runningCall) return `第 ${task.rounds.length} 轮 · ${runningCall.name}`
  if ('running' === task.status || 'queued' === task.status) {
    return `第 ${task.rounds.length || 1} 轮 · 正在思考`
  }
  const calls = task.rounds.reduce((total, round) => total + round.calls.length, 0)
  return `${task.rounds.length} 轮${calls ? ` · ${calls} 次调用` : ''}`
})

const selectedMessages = computed<ChatMessageView[]>(() => {
  const task = selectedTask.value
  if (!task) return []
  const context = task.context.trim()
  const question = context ? `${task.objective}\n\n补充上下文：\n${context}` : task.objective
  const rounds: ChatRound[] = task.rounds.map((round) => ({
    content: round.content,
    reasoning: round.reasoning,
    calls: calls(round)
  }))
  const last = rounds.at(-1)
  const failed = 'failed' === task.status
  const stopped = 'canceled' === task.status || 'interrupted' === task.status
  return [
    {
      id: `${task.id}-question`,
      parentId: '',
      role: 'user',
      content: question,
      reasoning: '',
      status: 'done',
      createdAt: task.startedAt,
      rounds: [],
      mentions: []
    },
    {
      id: `${task.id}-assistant`,
      parentId: `${task.id}-question`,
      role: 'assistant',
      content: last?.content || task.error,
      reasoning: '',
      status:
        'running' === task.status || 'queued' === task.status
          ? 'running'
          : failed
            ? 'failed'
            : stopped
              ? 'stopped'
              : 'done',
      createdAt: task.completedAt || task.updatedAt || task.startedAt,
      rounds,
      mentions: []
    }
  ]
})

function mergeTask(task: AgentInteractionTask): void {
  const previousLatest = tasks.value.at(-1)?.id ?? ''
  const selectedWasLatest = !selectedTaskId.value || selectedTaskId.value === previousLatest
  const index = tasks.value.findIndex((item) => item.id === task.id)
  if (index >= 0) tasks.value[index] = task
  else tasks.value.push(task)
  tasks.value = [...tasks.value].sort(
    (left, right) => (Date.parse(left.startedAt) || 0) - (Date.parse(right.startedAt) || 0)
  )
  const latest = tasks.value.at(-1)
  if (!latest) return
  const nextTaskId = visibleTasks.value.at(-1)?.id ?? latest.id
  if (selectedWasLatest && selectedTaskId.value !== nextTaskId) {
    selectedTaskId.value = nextTaskId
    autoFollow.value = true
    followLatest(true)
  }
}

async function load(): Promise<void> {
  if (!visible.value || !props.sessionId) return
  loading.value = true
  error.value = ''
  autoFollow.value = true
  tasks.value = []
  selectedTaskId.value = ''
  try {
    tasks.value = await store.interactions(props.sessionId)
  } catch (reason) {
    error.value = reason instanceof Error ? reason.message : '执行明细读取失败'
  } finally {
    if (props.run?.interaction) mergeTask(props.run.interaction)
    loading.value = false
    if (!selectedTaskId.value && tasks.value.length) {
      selectedTaskId.value = visibleTasks.value.at(-1)?.id ?? tasks.value.at(-1)!.id
    }
    await nextTick()
    followLatest(true)
  }
}

watch(
  () => [props.modelValue, props.sessionId] as const,
  ([open]) => {
    if (open) void load()
    else {
      tasks.value = []
      selectedTaskId.value = ''
      taskFilter.value = 'all'
      autoFollow.value = true
      error.value = ''
    }
  },
  { immediate: true }
)

watch(taskFilter, () => {
  if (selectedTask.value) return
  const next = visibleTasks.value.at(-1)
  if (next) selectTask(next.id)
})

watch(
  () => props.run?.interaction,
  (task) => {
    if (!visible.value || !task) return
    mergeTask(task)
    if (selectedTaskId.value === task.id) followLatest()
  }
)

function selectTask(taskId: string): void {
  selectedTaskId.value = taskId
  autoFollow.value = true
  followLatest(true)
}

function followLatest(force = false): void {
  if (!force && !autoFollow.value) return
  const current = scrollWrap()
  if (!force && current && !isAtBottom(current)) {
    autoFollow.value = false
    return
  }
  void nextTick(() => {
    const wrap = scrollWrap()
    if (!wrap || !autoFollow.value) return
    detailScroll.value?.setScrollTop(wrap.scrollHeight)
  })
}

function onDetailScroll(): void {
  const wrap = scrollWrap()
  if (!wrap) return
  autoFollow.value = isAtBottom(wrap)
}

function isAtBottom(wrap: HTMLElement): boolean {
  return wrap.scrollHeight - wrap.scrollTop - wrap.clientHeight <= 24
}

function scrollWrap(): HTMLElement | undefined {
  const wrap = detailScroll.value?.wrapRef
  if (!wrap) return undefined
  return wrap instanceof HTMLElement ? wrap : wrap.value
}

function calls(round: AgentInteractionRound): ChatToolCall[] {
  return round.calls.map((call, index) => ({
    id: call.id || `call-${index}`,
    name: call.name,
    args: call.args,
    status:
      'success' === call.status
        ? 'success'
        : 'running' === call.status
          ? 'running'
          : 'failed' === call.status
            ? 'failed'
            : 'stopped',
    result: call.error && !call.result ? `调用失败：${call.error}` : call.result,
    duration: call.durationMs,
    scope: 'agent',
    label: call.name
  }))
}

function duration(task: AgentInteractionTask): string {
  const start = Date.parse(task.startedAt)
  const end = Date.parse(task.completedAt) || Date.now()
  if (!Number.isFinite(start)) return ''
  const seconds = Math.max(0, Math.floor((end - start) / 1000))
  if (seconds < 60) return `${seconds} 秒`
  return `${Math.floor(seconds / 60)} 分 ${seconds % 60} 秒`
}
</script>

<template>
  <section v-if="visible" class="agent-inspector-panel" :class="{ vertical, standalone }">
    <header class="compact-head">
      <span class="agent-mark" aria-hidden="true">
        {{
          run?.agent
            ? 'project_tools' === run.agent
              ? 'P'
              : 'desktop' === run.agent
                ? 'D'
                : 'B'
            : 'A'
        }}
      </span>
      <strong>{{ agentLabel }}</strong>
      <el-tag size="small" effect="plain" :type="types[currentStatus] || 'info'">
        {{ labels[currentStatus] || currentStatus }}
      </el-tag>
      <span v-if="currentAction" class="compact-action">{{ currentAction }}</span>
      <span v-if="run" class="compact-session">
        {{ run.reused ? '已续接' : '新会话' }}
      </span>
      <span class="compact-spacer" />
      <el-button link :loading="loading" @click="load">刷新</el-button>
    </header>

    <el-alert
      v-if="error"
      class="detail-error"
      :title="error"
      type="error"
      show-icon
      :closable="false"
    />

    <el-splitter v-loading="loading" class="agent-splitter" @resize-end="saveNavWidth">
      <el-splitter-panel
        :size="navWidth"
        :min="AGENT_NAV_WIDTH_MIN"
        :max="AGENT_NAV_WIDTH_MAX"
        @update:size="setNavWidth"
      >
        <aside class="task-nav">
          <div class="task-filters">
            <button
              v-for="item in filters"
              :key="item.value"
              type="button"
              :class="{ active: taskFilter === item.value }"
              @click="taskFilter = item.value"
            >
              {{ item.label }}
            </button>
            <button type="button" class="task-filter-refresh" title="刷新执行明细" @click="load">
              ↻
            </button>
          </div>
          <el-scrollbar class="task-nav-scroll">
            <button
              v-for="(task, taskIndex) in visibleTasks"
              :key="task.id"
              type="button"
              class="task-nav-item"
              :class="[task.status, { active: selectedTaskId === task.id }]"
              @click="selectTask(task.id)"
            >
              <span class="task-nav-index">{{ taskIndex + 1 }}</span>
              <span class="task-nav-main">
                <strong>{{ task.objective }}</strong>
                <span>{{ labels[task.status] || task.status }} · {{ duration(task) }}</span>
              </span>
              <span class="task-nav-dot" aria-hidden="true" />
            </button>
            <el-empty
              v-if="!visibleTasks.length && !loading"
              description="当前筛选没有任务"
              :image-size="54"
            />
          </el-scrollbar>
        </aside>
      </el-splitter-panel>

      <el-splitter-panel :min="340">
        <main class="task-content">
          <template v-if="selectedTask">
            <el-button
              v-if="'running' === selectedTask.status && !autoFollow"
              class="follow-latest"
              size="small"
              type="primary"
              plain
              @click="selectTask(selectedTask.id)"
            >
              回到最新
            </el-button>
            <el-scrollbar ref="detailScroll" class="task-scroll" @scroll="onDetailScroll">
              <div class="task-detail">
                <div class="task-meta">
                  <el-tag size="small" effect="plain" :type="types[selectedTask.status] || 'info'">
                    {{ labels[selectedTask.status] || selectedTask.status }}
                  </el-tag>
                  <span>{{ duration(selectedTask) }}</span>
                  <span>{{ selectedTask.rounds.length }} 轮模型调用</span>
                  <span>
                    {{
                      selectedTask.rounds.reduce((total, round) => total + round.calls.length, 0)
                    }}
                    次工具调用
                  </span>
                </div>
                <div v-if="selectedTaskError" class="task-error-note" role="status">
                  {{ selectedTaskError }}
                </div>
                <div class="agent-message-column">
                  <ChatMessage
                    v-for="message in selectedMessages"
                    :key="message.id"
                    :message="message"
                    :project-id="projectId"
                    :conversation-id="parentConversationId"
                  />
                </div>
              </div>
            </el-scrollbar>
          </template>
          <el-empty v-else description="还没有可展示的执行明细" />
        </main>
      </el-splitter-panel>
    </el-splitter>
  </section>
</template>

<style lang="scss" scoped>
.agent-inspector-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-width: 0;
  overflow: hidden;
  border-left: 1px solid var(--el-border-color-lighter);
  background: var(--el-bg-color);
  container-type: inline-size;

  &.vertical {
    border-top: 1px solid var(--el-border-color-lighter);
    border-left: none;
  }

  &.standalone {
    border: none;
  }
}

.compact-head {
  display: flex;
  flex: none;
  align-items: center;
  gap: 7px;
  min-height: 40px;
  padding: 6px 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: var(--el-bg-color);

  strong {
    flex: none;
    color: var(--el-text-color-primary);
    font-size: 12px;
    font-weight: 600;
  }
}

.agent-mark {
  display: inline-flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 7px;
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  font-size: 10px;
  font-weight: 700;
}

.compact-action,
.compact-session {
  overflow: hidden;
  color: var(--el-text-color-secondary);
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.compact-action {
  max-width: 240px;
  padding-left: 7px;
  border-left: 1px solid var(--el-border-color);
}

.compact-session {
  flex: none;
  color: var(--el-text-color-placeholder);
}

.compact-spacer {
  flex: 1;
}

.detail-error {
  flex: none;
  margin: 10px 16px 0;
  width: auto;
}

.task-error-note {
  padding: 2px 1px;
  color: var(--el-color-danger);
  font-size: 12px;
  line-height: 1.6;
}

.agent-splitter {
  flex: 1;
  min-height: 0;

  :deep(.el-splitter-panel) {
    height: 100%;
    overflow: hidden;
  }
}

.task-nav {
  height: 100%;
  min-width: 0;
  overflow: hidden;
  background: var(--el-fill-color-lighter);
}

.task-filters {
  display: flex;
  gap: 2px;
  padding: 10px;

  button {
    flex: 1;
    padding: 5px 0;
    border: none;
    border-radius: 5px;
    background: transparent;
    color: var(--el-text-color-secondary);
    font: inherit;
    font-size: 11px;
    cursor: pointer;

    &.active {
      background: var(--el-bg-color);
      color: var(--el-color-primary);
      box-shadow: 0 1px 3px rgb(0 0 0 / 6%);
    }

    &.task-filter-refresh {
      flex: 0 0 24px;
      font-size: 15px;
      line-height: 1;
    }
  }
}

.task-nav-scroll {
  height: calc(100% - 43px);
}

.task-nav-item {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: 8px;
  width: 100%;
  padding: 10px;
  border: 1px solid transparent;
  border-radius: 12px;
  background: transparent;
  text-align: left;
  cursor: pointer;

  &:hover {
    border-color: var(--el-border-color-lighter);
  }

  &.active {
    border-color: transparent;
    background: var(--el-bg-color);
  }

  &.active::before {
    position: absolute;
    inset: 10px auto 10px 0;
    width: 3px;
    border-radius: 0 3px 3px 0;
    background: var(--el-color-primary);
    content: '';
  }
}

.task-nav-scroll :deep(.el-scrollbar__view) {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 2px 8px 8px;
}

.task-nav-index {
  flex: none;
  color: var(--el-text-color-placeholder);
  font-size: 10px;
  line-height: 18px;
}

.task-nav-main {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  gap: 4px;

  strong {
    display: -webkit-box;
    overflow: hidden;
    color: var(--el-text-color-regular);
    font-size: 12px;
    font-weight: 500;
    line-height: 18px;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
  }

  span {
    overflow: hidden;
    color: var(--el-text-color-placeholder);
    font-size: 10px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.task-nav-dot {
  flex: none;
  width: 6px;
  height: 6px;
  margin-top: 6px;
  border-radius: 50%;
  background: var(--el-text-color-placeholder);

  .task-nav-item.running & {
    background: var(--el-color-primary);
  }

  .task-nav-item.waiting_user &,
  .task-nav-item.interrupted & {
    background: var(--el-color-warning);
  }

  .task-nav-item.failed & {
    background: var(--el-color-danger);
  }

  .task-nav-item.completed & {
    background: var(--el-color-success);
  }
}

.task-content {
  position: relative;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-width: 0;
  overflow: hidden;
}

.follow-latest {
  position: absolute;
  z-index: 2;
  top: 10px;
  right: 16px;
  box-shadow: 0 3px 12px rgb(0 0 0 / 10%);
}

.task-scroll {
  flex: 1;
  min-height: 0;
}

.task-detail {
  width: 100%;
  max-width: 820px;
  margin: 0 auto;
  padding: 14px 18px 28px;
}

.task-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 12px;
  margin: 0 0 10px;
  color: var(--el-text-color-placeholder);
  font-size: 11px;
}

.agent-message-column {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

@container (max-width: 480px) {
  .compact-session {
    display: none;
  }

  .compact-action {
    max-width: 120px;
  }
}
</style>
