import { ref } from 'vue'
import { defineStore } from 'pinia'
import AgentDb, {
  type AgentInteractionTask,
  type AgentKind,
  type AgentSession,
  type AgentStatus,
  WAITING_SETTLED_ERROR,
  WAITING_SETTLED_TEXT
} from '@renderer/db/AgentDb'
import {
  cancelAgentSession,
  onAgentRuntimeEvent,
  type AgentRuntimeEvent
} from '@renderer/core/AgentRuntime'

export interface AgentRunView {
  sessionId: string
  parentConversationId: string
  agent: AgentKind
  objective: string
  taskKey: string
  sessionMode: 'new' | 'auto' | 'reuse'
  reused: boolean
  status: AgentStatus
  round: number
  tool: string
  message: string
  startedAt: string
  lastActivityAt: string
  stalled: boolean
  traceCount: number
  /** 当前主进程推送的本次委派完整交互快照 */
  interaction: AgentInteractionTask | null
  /** 是否由当前渲染进程持有执行权；从数据库补回的运行态不能在本进程直接停止 */
  managed: boolean
}

const ACTIVE: AgentStatus[] = ['queued', 'running']
const STALE_AFTER = 2 * 60 * 1000
const MAX_REMEMBERED_RUNS = 200

export const useAgentStore = defineStore('agents', () => {
  const runs = ref<Record<string, AgentRunView>>({})
  const clock = ref(Date.now())
  let initialization: Promise<void> | null = null
  let unsubscribe: (() => void) | null = null
  let timer: ReturnType<typeof setInterval> | null = null
  let recoveryTick = 0
  const ignoredSessions = new Set<string>()

  function fromSession(session: AgentSession): AgentRunView {
    const lastTrace = session.trace.at(-1)
    const summary = String(session.summary ?? '')
      .replace(/\s+/g, ' ')
      .trim()
    const sessionError =
      WAITING_SETTLED_ERROR === session.error ? WAITING_SETTLED_TEXT : session.error
    return {
      sessionId: session.id,
      parentConversationId: session.parentConversationId,
      agent: session.agent,
      objective: session.title,
      taskKey: session.taskKey,
      sessionMode: session.sessionMode,
      reused: session.reused,
      status: session.status,
      round: 0,
      tool: lastTrace?.tool ?? '',
      message:
        sessionError ||
        ('waiting_user' === session.status ? '等待用户处理授权、登录或确认' : '') ||
        ('completed' === session.status && summary ? `最近结果：${summary.slice(0, 120)}` : '') ||
        (lastTrace
          ? `${lastTrace.tool}：${'failed' === lastTrace.status ? '失败' : '已完成'}`
          : ''),
      startedAt: session.createdAt,
      lastActivityAt: session.updatedAt,
      stalled: false,
      traceCount: session.trace.length,
      interaction: null,
      managed: false
    }
  }

  function apply(event: AgentRuntimeEvent): void {
    if (ignoredSessions.has(event.sessionId)) return
    const previous = runs.value[event.sessionId]
    runs.value[event.sessionId] = {
      sessionId: event.sessionId,
      parentConversationId: event.parentConversationId,
      agent: event.agent,
      objective: event.objective,
      taskKey: event.taskKey,
      sessionMode: event.sessionMode,
      reused: event.reused,
      status: event.status,
      round: event.round,
      tool: event.tool,
      message: event.message,
      startedAt: previous?.startedAt || event.at,
      lastActivityAt: event.at,
      stalled: false,
      traceCount: previous?.traceCount ?? 0,
      interaction: event.interaction ?? previous?.interaction ?? null,
      managed: true
    }
    if (event.interaction) {
      window.ipc.agent.publish(event.sessionId, event.interaction)
    }
    pruneRuns()
  }

  function pruneRuns(): void {
    const entries = Object.entries(runs.value)
    if (entries.length <= MAX_REMEMBERED_RUNS) return
    const keep = entries
      .sort((left, right) => {
        const leftActive = ACTIVE.includes(left[1].status)
        const rightActive = ACTIVE.includes(right[1].status)
        if (leftActive !== rightActive) return leftActive ? -1 : 1
        return (
          (Date.parse(right[1].lastActivityAt) || 0) - (Date.parse(left[1].lastActivityAt) || 0)
        )
      })
      .slice(0, MAX_REMEMBERED_RUNS)
    runs.value = Object.fromEntries(keep)
  }

  function refreshStale(now = Date.now()): void {
    clock.value = now
    for (const run of Object.values(runs.value)) {
      if (!ACTIVE.includes(run.status)) {
        run.stalled = false
        continue
      }
      run.stalled = now - (Date.parse(run.lastActivityAt) || now) > STALE_AFTER
    }
  }

  async function recoverInterrupted(): Promise<void> {
    const ids = await AgentDb.recoverInterrupted()
    const now = new Date().toISOString()
    for (const id of ids) {
      const run = runs.value[id]
      if (!run || !ACTIVE.includes(run.status)) continue
      run.status = 'interrupted'
      run.message = '应用上次退出时任务尚未完成'
      run.stalled = false
      run.lastActivityAt = now
    }
  }

  function initialize(): Promise<void> {
    if (initialization) return initialization
    const task = (async () => {
      unsubscribe = onAgentRuntimeEvent(apply)
      timer = setInterval(() => {
        refreshStale()
        recoveryTick++
        if (0 === recoveryTick % 6) void recoverInterrupted()
      }, 5000)
      await recoverInterrupted()
      // 应用启动时不存在正在运行的主对话；上次遗留的 waiting_user 只作为可续接历史，
      // 不再长期占用侧边栏和“正在处理”区域。
      await settleWaiting()
      const waiting = await AgentDb.waiting()
      for (const session of waiting) {
        if (!runs.value[session.id]) runs.value[session.id] = fromSession(session)
      }
    })()
    initialization = task
    void task.catch(() => {
      if (initialization !== task) return
      unsubscribe?.()
      unsubscribe = null
      if (timer) clearInterval(timer)
      timer = null
      recoveryTick = 0
      initialization = null
    })
    return task
  }

  async function loadConversation(conversationId: string): Promise<void> {
    if (!conversationId) return
    await initialize()
    const rows = await AgentDb.listByConversation(conversationId)
    for (const row of rows) {
      if (runs.value[row.id]) continue
      runs.value[row.id] = fromSession(row)
    }
    pruneRuns()
    refreshStale()
  }

  function forConversation(conversationId: string): AgentRunView[] {
    return Object.values(runs.value)
      .filter((run) => run.parentConversationId === conversationId)
      .sort(
        (left, right) =>
          (Date.parse(right.lastActivityAt) || 0) - (Date.parse(left.lastActivityAt) || 0)
      )
      .slice(0, 8)
  }

  function statusForConversation(conversationId: string): {
    active: number
    waiting: number
  } {
    let active = 0
    let waiting = 0
    for (const run of Object.values(runs.value)) {
      if (run.parentConversationId !== conversationId) continue
      if (ACTIVE.includes(run.status)) active++
      if ('waiting_user' === run.status) waiting++
    }
    return { active, waiting }
  }

  /** 主轮次结束时释放该对话尚未承接的等待状态，保留会话供显式 session_id 续接。 */
  async function settleWaiting(conversationId = ''): Promise<void> {
    const ids = await AgentDb.settleWaiting(conversationId)
    if (!ids.length) return
    const at = new Date().toISOString()
    for (const id of ids) {
      const run = runs.value[id]
      if (!run) continue
      run.status = 'interrupted'
      run.message = WAITING_SETTLED_TEXT
      run.lastActivityAt = at
      if (run.interaction) {
        run.interaction.status = 'interrupted'
        run.interaction.error = ''
        run.interaction.completedAt = at
        run.interaction.updatedAt = at
      }
    }
  }

  function stop(sessionId: string): boolean {
    const run = runs.value[sessionId]
    if (!run || !ACTIVE.includes(run.status)) return false
    const previousMessage = run.message
    run.message = '正在停止'
    if (cancelAgentSession(sessionId)) return true
    run.message = previousMessage
    return false
  }

  async function openInspector(sessionId: string): Promise<void> {
    const id = String(sessionId ?? '').trim()
    if (!id) return
    await window.ipc.agent.open(id)
  }

  function forgetConversation(conversationIds: string[]): void {
    const ids = new Set(conversationIds)
    if (!ids.size) return
    for (const [id, run] of Object.entries(runs.value)) {
      if (!ids.has(run.parentConversationId)) continue
      ignoredSessions.add(id)
      delete runs.value[id]
    }
  }

  async function clearExperiences(projectId: string, agent: AgentKind): Promise<void> {
    await AgentDb.clearExperiences(projectId, agent)
  }

  async function interactions(sessionId: string): Promise<AgentInteractionTask[]> {
    return AgentDb.interactions(sessionId)
  }

  function reset(): void {
    runs.value = {}
    ignoredSessions.clear()
    dispose()
  }

  function dispose(): void {
    unsubscribe?.()
    unsubscribe = null
    if (timer) clearInterval(timer)
    timer = null
    recoveryTick = 0
    initialization = null
  }

  return {
    runs,
    clock,
    initialize,
    loadConversation,
    forConversation,
    statusForConversation,
    settleWaiting,
    refreshStale,
    stop,
    openInspector,
    forgetConversation,
    clearExperiences,
    interactions,
    reset,
    dispose
  }
})
