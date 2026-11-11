import Db from '@renderer/core/Db'
import ApiUtil from '@renderer/utils/ApiUtil'

const SESSION_TABLE = 'agent_sessions'
const EXPERIENCE_TABLE = 'agent_experiences'
const MAX_SESSIONS_PER_CONVERSATION = 50
const MAX_EXPERIENCES_PER_AGENT = 100
const MAX_INTERACTION_ROUNDS = 25
const MAX_INTERACTION_CALLS = 100

/** 旧版本把这段内部控制文案写进了 error；界面据此识别并降级为普通状态。 */
export const WAITING_SETTLED_ERROR = '主对话已结束，等待用户状态已释放'
export const WAITING_SETTLED_TEXT = '等待已释放'

export type AgentKind = 'browser' | 'project_tools' | 'desktop'
export type AgentStatus =
  'queued' | 'running' | 'waiting_user' | 'completed' | 'failed' | 'canceled' | 'interrupted'

export interface AgentTurn {
  role: 'user' | 'assistant'
  content: string
}

export type AgentTraceStatus = 'success' | 'failed' | 'not_run'

/** 脱敏执行轨迹：只记录操作形状，不保存工具参数、返回正文或页面快照。 */
export interface AgentTraceEntry {
  round: number
  tool: string
  status: AgentTraceStatus
  durationMs: number
  at: string
  error?: string
}

export type AgentInteractionCallStatus = 'running' | 'success' | 'failed' | 'stopped' | 'not_run'

export interface AgentInteractionToolCall {
  id: string
  name: string
  args: string
  result: string
  status: AgentInteractionCallStatus
  durationMs: number
  error?: string
}

export interface AgentInteractionRound {
  content: string
  reasoning: string
  calls: AgentInteractionToolCall[]
}

/**
 * 一次委派的完整交互快照。参数与返回保留到可视化所需的上限，
 * 页面按任务增量刷新，最终仍以服务端和工具的真实状态为准。
 */
export interface AgentInteractionTask {
  id: string
  objective: string
  context: string
  status: AgentStatus
  startedAt: string
  updatedAt: string
  completedAt: string
  rounds: AgentInteractionRound[]
  error: string
}

export interface AgentSession {
  id: string
  projectId: string
  parentConversationId: string
  agent: AgentKind
  taskKey: string
  sessionMode: 'new' | 'auto' | 'reuse'
  reused: boolean
  title: string
  messages: AgentTurn[]
  summary: string
  trace: AgentTraceEntry[]
  status: AgentStatus
  error: string
  createdAt: string
  updatedAt: string
}

export interface AgentExperience {
  id: string
  projectId: string
  agent: AgentKind
  content: string
  sourceSessionId: string
  createdAt: string
}

function parseMessages(value: unknown): AgentTurn[] {
  if (typeof value !== 'string' || !value) return []
  try {
    const rows = JSON.parse(value)
    if (!Array.isArray(rows)) return []
    return rows
      .map((item) => ({
        role: item?.role === 'assistant' ? 'assistant' : 'user',
        content: String(item?.content ?? '')
      }))
      .filter((item) => item.content.trim()) as AgentTurn[]
  } catch {
    return []
  }
}

function parseTrace(value: unknown): AgentTraceEntry[] {
  if (typeof value !== 'string' || !value) return []
  try {
    const rows = JSON.parse(value)
    if (!Array.isArray(rows)) return []
    return rows
      .slice(-100)
      .map((item) => {
        const status = String(item?.status ?? '')
        return {
          round: Math.max(0, Number(item?.round ?? 0) || 0),
          tool: String(item?.tool ?? '').slice(0, 200),
          status: ('failed' === status || 'not_run' === status ? status : 'success') as
            'success' | 'failed' | 'not_run',
          durationMs: Math.max(0, Number(item?.durationMs ?? 0) || 0),
          at: String(item?.at ?? ''),
          ...(item?.error ? { error: String(item.error).slice(0, 1000) } : {})
        }
      })
      .filter((item) => item.tool)
  } catch {
    return []
  }
}

function interactionStatus(value: unknown): AgentStatus {
  const status = String(value ?? '')
  if (
    [
      'queued',
      'running',
      'waiting_user',
      'completed',
      'failed',
      'canceled',
      'interrupted'
    ].includes(status)
  ) {
    return status as AgentStatus
  }
  return 'failed'
}

function parseInteractionTask(value: unknown): AgentInteractionTask | null {
  if (typeof value !== 'string' || !value) return null
  try {
    const task: unknown = JSON.parse(value)
    if (!task || 'object' !== typeof task || Array.isArray(task)) return null
    const row = task as Record<string, unknown>
    const rounds = Array.isArray(row.rounds) ? row.rounds : []
    return {
      id: String(row.id ?? ''),
      objective: String(row.objective ?? ''),
      context: String(row.context ?? ''),
      status: interactionStatus(row.status),
      startedAt: String(row.startedAt ?? ''),
      updatedAt: String(row.updatedAt ?? ''),
      completedAt: String(row.completedAt ?? ''),
      error: String(row.error ?? ''),
      rounds: rounds.slice(-MAX_INTERACTION_ROUNDS).map((raw) => {
        const round = (raw ?? {}) as Record<string, unknown>
        const calls = Array.isArray(round.calls) ? round.calls : []
        return {
          content: String(round.content ?? ''),
          reasoning: String(round.reasoning ?? ''),
          calls: calls.slice(0, MAX_INTERACTION_CALLS).map((rawCall) => {
            const call = (rawCall ?? {}) as Record<string, unknown>
            const status = String(call.status ?? '')
            return {
              id: String(call.id ?? ''),
              name: String(call.name ?? ''),
              args: String(call.args ?? ''),
              result: String(call.result ?? ''),
              status: (['running', 'success', 'failed', 'stopped', 'not_run'].includes(status)
                ? status
                : 'failed') as AgentInteractionCallStatus,
              durationMs: Math.max(0, Number(call.durationMs ?? 0) || 0),
              ...(call.error ? { error: String(call.error) } : {})
            }
          })
        }
      })
    }
  } catch {
    return null
  }
}

function formatSession(row: Record<string, unknown>): AgentSession {
  const mode = String(row['session_mode'] ?? 'new')
  return {
    id: String(row['id'] ?? ''),
    projectId: String(row['project_id'] ?? ''),
    parentConversationId: String(row['parent_conversation_id'] ?? ''),
    agent: row['agent'] === 'project_tools' ? 'project_tools' : 'browser',
    taskKey: String(row['task_key'] ?? ''),
    sessionMode: 'auto' === mode || 'reuse' === mode ? mode : 'new',
    reused: 1 === Number(row['reused'] ?? 0) || true === row['reused'],
    title: String(row['title'] ?? ''),
    messages: parseMessages(row['messages']),
    summary: String(row['summary'] ?? ''),
    trace: parseTrace(row['trace']),
    status: String(row['status'] ?? '') as AgentStatus,
    error: String(row['error'] ?? ''),
    createdAt: String(row['created_at'] ?? ''),
    updatedAt: String(row['updated_at'] ?? '')
  }
}

function formatExperience(row: Record<string, unknown>): AgentExperience {
  return {
    id: String(row['id'] ?? ''),
    projectId: String(row['project_id'] ?? ''),
    agent: row['agent'] === 'project_tools' ? 'project_tools' : 'browser',
    content: String(row['content'] ?? ''),
    sourceSessionId: String(row['source_session_id'] ?? ''),
    createdAt: String(row['created_at'] ?? '')
  }
}

async function pruneSessions(parentConversationId: string): Promise<void> {
  const result = (await Db.select(
    SESSION_TABLE,
    { parent_conversation_id: parentConversationId },
    'updated_at DESC, created_at DESC',
    MAX_SESSIONS_PER_CONVERSATION + 10
  )) as { code: number; message: string; data: unknown[] }
  if (!ApiUtil.succeed(result)) return
  const ids = (ApiUtil.data(result) as Record<string, unknown>[])
    .slice(MAX_SESSIONS_PER_CONVERSATION)
    .map((row) => String(row['id'] ?? ''))
    .filter(Boolean)
  if (!ids.length) return
  await Db.removeBatch([
    { table: 'agent_interactions', in: { session_id: ids } },
    { table: SESSION_TABLE, in: { id: ids } }
  ])
}

async function pruneExperiences(projectId: string, agent: AgentKind): Promise<void> {
  const result = (await Db.select(
    EXPERIENCE_TABLE,
    { project_id: projectId, agent },
    'updated_at DESC, created_at DESC',
    MAX_EXPERIENCES_PER_AGENT + 10
  )) as { code: number; message: string; data: unknown[] }
  if (!ApiUtil.succeed(result)) return
  const ids = (ApiUtil.data(result) as Record<string, unknown>[])
    .slice(MAX_EXPERIENCES_PER_AGENT)
    .map((row) => String(row['id'] ?? ''))
    .filter(Boolean)
  if (!ids.length) return
  await Db.removeBatch([{ table: EXPERIENCE_TABLE, in: { id: ids } }])
}

async function markInteractionsInterrupted(sessionId: string, at: string): Promise<void> {
  const result = (await Db.select('agent_interactions', { session_id: sessionId })) as {
    code: number
    message: string
    data: unknown[]
  }
  if (!ApiUtil.succeed(result)) return
  for (const row of ApiUtil.data(result) as Record<string, unknown>[]) {
    const task = parseInteractionTask(row['payload'])
    if (!task || ('queued' !== task.status && 'running' !== task.status)) continue
    task.status = 'interrupted'
    task.error = '应用上次退出时任务尚未完成'
    task.completedAt = at
    task.updatedAt = at
    await Db.update(
      'agent_interactions',
      { id: task.id },
      { updated_at: at, payload: JSON.stringify(task) }
    )
  }
}

async function settleInteractions(sessionId: string, at: string): Promise<void> {
  const result = (await Db.select('agent_interactions', { session_id: sessionId })) as {
    code: number
    message: string
    data: unknown[]
  }
  if (!ApiUtil.succeed(result)) return
  for (const row of ApiUtil.data(result) as Record<string, unknown>[]) {
    const task = parseInteractionTask(row['payload'])
    if (!task || !['queued', 'running', 'waiting_user'].includes(task.status)) continue
    task.status = 'interrupted'
    task.error = ''
    task.completedAt = at
    task.updatedAt = at
    await Db.update(
      'agent_interactions',
      { id: task.id },
      { updated_at: at, payload: JSON.stringify(task) }
    )
  }
}

export default {
  async get(id: string): Promise<AgentSession | null> {
    if (!id) return null
    const result = (await Db.select(SESSION_TABLE, { id })) as {
      code: number
      message: string
      data: unknown[]
    }
    if (!ApiUtil.succeed(result)) return null
    const row = (ApiUtil.data(result) as Record<string, unknown>[])[0]
    return row ? formatSession(row) : null
  },

  async create(data: {
    id: string
    projectId: string
    parentConversationId: string
    agent: AgentKind
    taskKey?: string
    sessionMode?: 'new' | 'auto' | 'reuse'
    reused?: boolean
    title: string
    messages?: AgentTurn[]
    trace?: AgentTraceEntry[]
  }): Promise<void> {
    const now = new Date().toISOString()
    const result = (await Db.insert(SESSION_TABLE, {
      id: data.id,
      project_id: data.projectId,
      parent_conversation_id: data.parentConversationId,
      agent: data.agent,
      task_key: data.taskKey ?? '',
      session_mode: data.sessionMode ?? 'new',
      reused: data.reused ? 1 : 0,
      title: data.title,
      messages: JSON.stringify(data.messages ?? []),
      summary: '',
      trace: JSON.stringify(data.trace ?? []),
      status: 'queued',
      error: '',
      created_at: now,
      updated_at: now
    })) as { code: number; message: string; data: unknown }
    if (!ApiUtil.succeed(result))
      throw new Error(ApiUtil.message(result) || '子 Agent 会话创建失败')
    await pruneSessions(data.parentConversationId).catch((error) => {
      console.warn('[AgentDb] Failed to prune old sub-agent sessions:', error)
    })
  },

  async update(
    id: string,
    data: {
      messages?: AgentTurn[]
      summary?: string
      trace?: AgentTraceEntry[]
      sessionMode?: 'new' | 'auto' | 'reuse'
      reused?: boolean
      status?: AgentStatus
      error?: string
    }
  ): Promise<void> {
    const row: Record<string, unknown> = { updated_at: new Date().toISOString() }
    if (data.messages !== undefined) row['messages'] = JSON.stringify(data.messages)
    if (data.summary !== undefined) row['summary'] = data.summary
    if (data.trace !== undefined) row['trace'] = JSON.stringify(data.trace.slice(-100))
    if (data.sessionMode !== undefined) row['session_mode'] = data.sessionMode
    if (data.reused !== undefined) row['reused'] = data.reused ? 1 : 0
    if (data.status !== undefined) row['status'] = data.status
    if (data.error !== undefined) row['error'] = data.error
    const result = (await Db.update(SESSION_TABLE, { id }, row)) as {
      code: number
      message: string
      data: unknown
    }
    if (!ApiUtil.succeed(result))
      throw new Error(ApiUtil.message(result) || '子 Agent 会话保存失败')
  },

  /** 打开详情时按需读取逐次委派，避免会话列表反复搬运大段正文与工具返回。 */
  async interactions(sessionId: string, limit = 8): Promise<AgentInteractionTask[]> {
    if (!sessionId) return []
    const result = (await Db.select(
      'agent_interactions',
      { session_id: sessionId },
      'started_at DESC',
      Math.max(1, Math.min(20, limit))
    )) as { code: number; message: string; data: unknown[] }
    if (!ApiUtil.succeed(result)) {
      throw new Error(ApiUtil.message(result) || '子 Agent 交互明细读取失败')
    }
    return (ApiUtil.data(result) as Record<string, unknown>[])
      .map((row) => parseInteractionTask(row['payload']))
      .filter((task): task is AgentInteractionTask => !!task)
      .reverse()
  },

  async saveInteraction(
    sessionId: string,
    projectId: string,
    parentConversationId: string,
    task: AgentInteractionTask
  ): Promise<void> {
    const result = (await Db.upsert('agent_interactions', {
      id: task.id,
      session_id: sessionId,
      project_id: projectId,
      parent_conversation_id: parentConversationId,
      started_at: task.startedAt,
      updated_at: task.updatedAt,
      payload: JSON.stringify(task)
    })) as { code: number; message: string; data: unknown }
    if (!ApiUtil.succeed(result)) {
      throw new Error(ApiUtil.message(result) || '子 Agent 交互明细保存失败')
    }
  },

  /** 同一项目、同一专业 Agent 最近沉淀的少量经验，按时间倒序 */
  async experiences(projectId: string, agent: AgentKind, limit = 5): Promise<AgentExperience[]> {
    if (!projectId) return []
    const result = (await Db.select(
      EXPERIENCE_TABLE,
      { project_id: projectId, agent },
      'updated_at DESC, created_at DESC',
      Math.max(1, Math.min(10, limit))
    )) as { code: number; message: string; data: unknown[] }
    if (!ApiUtil.succeed(result)) return []
    return (ApiUtil.data(result) as Record<string, unknown>[]).map(formatExperience)
  },

  /** 当前父会话最近一次同类子 Agent 会话，用于显式或保守自动续接 */
  async latest(
    projectId: string,
    parentConversationId: string,
    agent: AgentKind,
    taskKey = ''
  ): Promise<AgentSession | null> {
    if (!projectId || !parentConversationId) return null
    const where: Record<string, unknown> = {
      project_id: projectId,
      parent_conversation_id: parentConversationId,
      agent
    }
    if (taskKey) where['task_key'] = taskKey
    const result = (await Db.select(
      SESSION_TABLE,
      where,
      'updated_at DESC, created_at DESC',
      1
    )) as {
      code: number
      message: string
      data: unknown[]
    }
    if (!ApiUtil.succeed(result)) return null
    const row = (ApiUtil.data(result) as Record<string, unknown>[])[0]
    return row ? formatSession(row) : null
  },

  /** 当前对话的子 Agent 会话，供状态面板在页面重新打开后补回历史 */
  async listByConversation(parentConversationId: string, limit = 20): Promise<AgentSession[]> {
    if (!parentConversationId) return []
    const result = (await Db.select(
      SESSION_TABLE,
      { parent_conversation_id: parentConversationId },
      'updated_at DESC, created_at DESC',
      Math.max(1, Math.min(50, limit))
    )) as { code: number; message: string; data: unknown[] }
    if (!ApiUtil.succeed(result)) return []
    return (ApiUtil.data(result) as Record<string, unknown>[]).map(formatSession)
  },

  /** 全部等待用户处理的会话：登录后先载入，侧边栏无需逐个打开对话就能提示 */
  async waiting(limit = 50): Promise<AgentSession[]> {
    const result = (await Db.select(
      SESSION_TABLE,
      { status: 'waiting_user' },
      'updated_at DESC, created_at DESC',
      Math.max(1, Math.min(100, limit))
    )) as { code: number; message: string; data: unknown[] }
    if (!ApiUtil.succeed(result)) return []
    return (ApiUtil.data(result) as Record<string, unknown>[]).map(formatSession)
  },

  /**
   * 主轮次结束或应用重新启动后，把不再有活动主对话承接的 waiting_user 收口为 interrupted。
   * 会话本身和历史保留，显式 session_id 仍可续接；只是不再占用“正在处理”和侧边栏等待状态。
   */
  async settleWaiting(parentConversationId = ''): Promise<string[]> {
    const where: Record<string, unknown> = { status: 'waiting_user' }
    if (parentConversationId) where['parent_conversation_id'] = parentConversationId
    const listed = (await Db.select(
      SESSION_TABLE,
      where,
      'updated_at DESC, created_at DESC',
      200
    )) as {
      code: number
      message: string
      data: unknown[]
    }
    if (!ApiUtil.succeed(listed)) return []
    const now = new Date().toISOString()
    const ids: string[] = []
    for (const row of ApiUtil.data(listed) as Record<string, unknown>[]) {
      const id = String(row['id'] ?? '')
      if (!id) continue
      const updated = (await Db.update(
        SESSION_TABLE,
        { id },
        { status: 'interrupted', error: '', updated_at: now }
      )) as { code: number; message: string; data: unknown }
      if (!ApiUtil.succeed(updated)) continue
      await settleInteractions(id, now).catch((error) => {
        console.warn('[AgentDb] Failed to settle waiting interaction:', error)
      })
      ids.push(id)
    }
    return ids
  },

  /** 活动执行的心跳：只刷新时间，不改状态；多实例启动时据此避免误判其他进程的任务 */
  async touch(id: string): Promise<void> {
    const result = (await Db.update(
      SESSION_TABLE,
      { id },
      { updated_at: new Date().toISOString() }
    )) as { code: number; message: string; data: unknown }
    if (!ApiUtil.succeed(result))
      throw new Error(ApiUtil.message(result) || '子 Agent 心跳写入失败')
  },

  /** 超过心跳时限的 running / queued 会话标记为 interrupted */
  async recoverInterrupted(maxAgeMs = 2 * 60 * 1000): Promise<string[]> {
    const now = new Date().toISOString()
    const changed: string[] = []
    for (const status of ['running', 'queued'] as const) {
      const listed = (await Db.select(SESSION_TABLE, { status }, 'updated_at ASC', 200)) as {
        code: number
        message: string
        data: unknown[]
      }
      if (!ApiUtil.succeed(listed)) continue
      for (const row of ApiUtil.data(listed) as Record<string, unknown>[]) {
        const updated = Date.parse(String(row['updated_at'] ?? ''))
        if (Number.isFinite(updated) && Date.now() - updated < maxAgeMs) continue
        const result = (await Db.update(
          SESSION_TABLE,
          { id: String(row['id'] ?? '') },
          {
            status: 'interrupted',
            error: '应用上次退出时任务尚未完成',
            updated_at: now
          }
        )) as { code: number; message: string; data: unknown }
        if (!ApiUtil.succeed(result)) continue
        await markInteractionsInterrupted(String(row['id'] ?? ''), now).catch(() => undefined)
        changed.push(String(row['id'] ?? ''))
      }
    }
    return changed.filter(Boolean)
  },

  async remember(data: {
    projectId: string
    agent: AgentKind
    content: string
    sourceSessionId: string
  }): Promise<void> {
    const content = String(data.content ?? '').trim()
    if (!content || !data.projectId) return
    const now = new Date().toISOString()
    const existing = (await Db.select(
      EXPERIENCE_TABLE,
      { project_id: data.projectId, agent: data.agent, content },
      'updated_at DESC',
      1
    )) as { code: number; message: string; data: unknown[] }
    const existingId = ApiUtil.succeed(existing)
      ? String((ApiUtil.data(existing) as Record<string, unknown>[])[0]?.['id'] ?? '')
      : ''
    if (existingId) {
      const updated = (await Db.update(
        EXPERIENCE_TABLE,
        { id: existingId },
        { source_session_id: data.sourceSessionId, updated_at: now }
      )) as { code: number; message: string; data: unknown }
      if (!ApiUtil.succeed(updated)) {
        throw new Error(ApiUtil.message(updated) || '子 Agent 经验更新失败')
      }
      return
    }
    const result = (await Db.insert(EXPERIENCE_TABLE, {
      id: crypto.randomUUID(),
      project_id: data.projectId,
      agent: data.agent,
      content,
      source_session_id: data.sourceSessionId,
      created_at: now,
      updated_at: now
    })) as { code: number; message: string; data: unknown }
    if (!ApiUtil.succeed(result))
      throw new Error(ApiUtil.message(result) || '子 Agent 经验保存失败')
    await pruneExperiences(data.projectId, data.agent).catch((error) => {
      console.warn('[AgentDb] Failed to prune old sub-agent experiences:', error)
    })
  },

  async clearExperiences(projectId: string, agent: AgentKind): Promise<void> {
    if (!projectId) return
    const result = (await Db.remove(EXPERIENCE_TABLE, {
      project_id: projectId,
      agent
    })) as { code: number; message: string; data: unknown }
    if (!ApiUtil.succeed(result))
      throw new Error(ApiUtil.message(result) || '子 Agent 经验清理失败')
  }
}
