import { markRaw, reactive, ref } from 'vue'
import { defineStore } from 'pinia'
import ConversationDb from '@renderer/db/ConversationDb'
import ModelChat, { type ModelChatMessage } from '@renderer/core/ModelChat'
import {
  buildTools,
  ToolDescriptionError,
  type ToolScope,
  type ToolSet
} from '@renderer/core/Toolbox'
import { clearTools } from '@renderer/core/ToolCache'
import { estimateJson, estimateTokens, MESSAGE_OVERHEAD } from '@renderer/core/Tokens'
import {
  beginAgentParentRun,
  cancelAgentRuns,
  resetAgentRuntime
} from '@renderer/core/AgentRuntime'
import { useProjectStore } from './project'
import type { Project } from '@renderer/db/ProjectDb'

/**
 * 会话运行态：按会话保存消息与流式连接。
 *
 * 会话按项目创建，项目选定的模型、系统提示词与思考强度决定每一轮怎么跑；
 * 会话历史由客户端带上、只落本地库（服务端不存会话）。
 *
 * 一轮对话是一段「模型 → 工具 → 模型」的循环（客户端驱动）：
 * 模型要调工具就调，本地工具（读文件 / 写文件 / 执行命令）由主进程按项目权限与工作区外访问策略执行，
 * 项目工具交由服务端执行；每次调用的参数与结果都进本轮的调用链，
 * 下一轮请求把完整调用链（assistant 的 tool_calls 与逐条 tool 返回）一并带上，模型据此续推。
 *
 * 「切换会话后保持后台运行」由这里保证：连接挂在 store 上，切换会话 / 返回上一页
 * 都不会断开；只有显式停止、退出登录或应用退出才会中断。运行时增量写在会话对象上，
 * 切回该会话即可看到完整结果。
 */

/** 一次工具调用：参数按分片累积，执行后补上结果与耗时 */
export interface ChatToolCall {
  id: string
  name: string
  /** 模型给出的参数（JSON 文本，流式期间是未完成的分片） */
  args: string
  status: 'running' | 'success' | 'failed' | 'stopped'
  result: string
  duration: number
  scope: ToolScope
  /** 展示名：本地工具即函数名，项目工具按类型给出中文名 */
  label: string
}

/** 一轮模型输出：正文、思考过程与这一轮发起的工具调用 */
export interface ChatRound {
  content: string
  reasoning: string
  calls: ChatToolCall[]
}

/** @ 引用：本地路径（文件或目录）与已安装技能，发送时把内容 / 清单补进这一轮的上下文 */
export interface ChatMention {
  type: 'file' | 'skill'
  name: string
  /** 文件取绝对路径，技能取技能目录 */
  path: string
  /** 输入框里 @ 后面那一段文本（文件是相对路径，技能是 skill:名称），用于回显与同步 */
  token?: string
}

export interface ChatMessage {
  id: string
  /** 父消息：'' 表示分支起点；同一条父消息下的多条子消息互为分支 */
  parentId: string
  role: string
  /** 用户消息是输入原文，助手消息是最终回复正文（不含工具调用过程） */
  content: string
  reasoning: string
  status: string
  createdAt: string
  /** 助手消息的工具调用链：持久化后按它为下一轮重建 tool_calls / tool 消息 */
  rounds: ChatRound[]
  /** 用户消息的 @ 引用 */
  mentions: ChatMention[]
}

export interface ChatSession {
  conversationId: string
  /** 会话所属项目：模型、工具与工作区权限都按它取 */
  projectId: string
  model: string
  systemPrompt: string
  /**
   * 长期记忆：项目配置里的那一格（编辑器里手写，模型也能用 memory 工具的 write 写进来），
   * 仅在 memoryEnabled 为 true 时随每轮请求下发（见 memoryBlock）。写入落在项目上、
   * 本项目所有会话共用，所以这里每轮都从项目重取一次，写进来的内容当轮就生效。
   */
  memory: string
  /** 是否启用客户端记忆：关闭时记忆内容不下发，模型也没有 memory 工具 */
  memoryEnabled: boolean
  thinkEffort: string
  messages: ChatMessage[]
  /** 当前分支尾：页面只渲染它沿 parentId 回溯到根的那条路径 */
  leafId: string
  running: boolean
  /**
   * 用户已按停止：在模型流之外（工具执行中、等审批中）按下的那一次。
   * 停止不能只断模型流——工具执行这一刻没有流可断，标记留在这里，
   * 调用循环每一步都看它，一看就按「已停止」收尾，不再往下接续。
   */
  stopping: boolean
  /**
   * 这一轮回复的标识：删除会话 / 清空消息时据此作废这一轮，
   * 生成结束不再把已完成的内容写回已经删掉的记录（见 discard 与 persist）。
   */
  activeReplyId: string
  loading: boolean
  error: string
  /** 提示（非错误）：这一轮被自动接续上限收尾等情况，界面上按警告色呈现 */
  notice: string
  /** 当前主轮次的取消令牌：子 Agent 委派只认这一轮，旧轮次的迟到调用无法复活 */
  agentRunToken: string
  /** 运行对象不参与响应式代理：连接这类宿主对象被代理后调用会报非法调用 */
  chat: ModelChat | null
  /** 本会话生效的工具定义：用量估算要把它算进去；完整工具集在所有会话间共用 */
  tools: unknown[] | null
  /**
   * 本机说明（主进程算好的一段文字：运行环境 + 工作区口径）：追加在项目系统提示词之后
   * 一起下发，用量估算也要算它。每轮重取，目录、主目录或访问策略改了当轮就生效。
   */
  workspacePrompt: string
  /**
   * 已发送用户消息的实测用量（词元数）：@ 引用的正文只在发送时读到，
   * 历史里只留着输入框那段文字，这里记下发送当时折算出的整条用量。
   */
  messageTokens: Record<string, number>
  loaded: boolean
}

/**
 * 一轮生成的结果怎么作废：
 * - delete：会话被删除，生成结束时再清一遍这个会话的残留记录；
 * - clear：消息被清空，生成结束时再清一遍消息并把分支尾收回起点。
 */
type DropMode = 'delete' | 'clear'

/** 项目口径：模型与生成参数都跟着会话所属项目走，打开会话与每轮发送都按它取 */
export interface ChatProfile {
  projectId: string
  model: string
  systemPrompt: string
  memory: string
  memoryEnabled: boolean
  thinkEffort: string
}

/** 从项目取本会话要用的口径：项目已被删除或字段为空时给安全的空值 */
export function chatProfile(project?: Project | null): ChatProfile {
  return {
    projectId: String(project?.id ?? ''),
    model: String(project?.model ?? ''),
    systemPrompt: String(project?.systemPrompt ?? ''),
    memory: String(project?.memory ?? ''),
    memoryEnabled: true === project?.memoryEnabled,
    thinkEffort: String(project?.thinkEffort ?? '')
  }
}

/**
 * 记忆块：项目配置里的长期记忆单独起一段、带标题下发，与系统提示词在视觉上分得开
 * （这段由用户手写，只加标题、不改写内容）。项目没启用记忆或内容为空时不占位置。
 */
function memoryBlock(memory: string, enabled: boolean): string {
  if (!enabled) return ''
  const text = String(memory ?? '').trim()
  return text ? `# 记忆\n${text}` : ''
}

/**
 * 下发给服务端的系统提示词：项目自己的提示词在前，接着是项目记忆，最后是主进程给的本机说明；
 * 三段都为空时不下发。服务端把这一整段作为首条 system 消息补在最前。
 */
function systemPromptOf(
  session: Pick<ChatSession, 'systemPrompt' | 'memory' | 'memoryEnabled' | 'workspacePrompt'>
): string {
  return [
    session.systemPrompt,
    memoryBlock(session.memory, session.memoryEnabled),
    session.workspacePrompt
  ]
    .map((text) => String(text ?? '').trim())
    .filter(Boolean)
    .join('\n\n')
}

/**
 * 取本机说明：主进程按这个项目当前的目录清单、主目录与访问策略（外加运行环境）现算一段文字。
 * 取不到（项目已删除、主进程未就绪）就不追加这一段——它只是给模型的补充说明，不挡对话。
 */
async function fetchWorkspacePrompt(projectId: string): Promise<string> {
  if (!projectId) return ''
  try {
    return String((await window.ipc.workspace.prompt(projectId)) ?? '')
  } catch {
    return ''
  }
}

export interface SendParams extends ChatProfile {
  conversationId: string
  query: string
  /** 输入框附件：本地文件 / 技能引用；粘贴截图会先保存为临时文件再按文件引用 */
  mentions?: ChatMention[]
  /** 接在哪条消息之后：不传表示接在当前分支尾，传 '' 表示从会话起点新起分支 */
  parentId?: string
  /** 重新生成：不重复落用户消息，只在 parentId 指向的提问下新增一条回复 */
  reuseQuestion?: boolean
  /** 定时任务等后台触发：不覆盖标题 */
  background?: boolean
}

/**
 * 会话标题：取这一轮的提问，只有 @ 引用时取引用名。
 * 只在会话第一次落库时用一次（见 conversation store 的 persist），
 * 之后各轮都不再动标题：改标题只发生在用户手动重命名时。
 * 后台触发（定时任务）与重新生成不动标题，给空串表示不改。
 */
export function chatTitle(params: SendParams): string {
  if (params.background || params.reuseQuestion) return ''
  const title = params.query || (params.mentions ?? []).map((item) => item.name).join(' ')
  return title.slice(0, 30) || '新对话'
}

/**
 * 消息落库通知：会话每落一条消息（用户提问、助手回复）就报一次落库时刻。
 * 会话列表据此把这条会话的时间与顺序推到当下——列表按最后一条消息的时间展示与排序，
 * 列表本身归 conversation store 管（它在这里注册），这里只管消息落地时报一声。
 */
let messageLanded: ((conversationId: string, at: string) => void) | null = null

export function onMessageLanded(hook: (conversationId: string, at: string) => void): void {
  messageLanded = hook
}

function notifyMessageLanded(conversationId: string, at: string): void {
  messageLanded?.(conversationId, at)
}

/**
 * 一轮真正收尾（落库 / 清理都结束）的通知：会话 store 据此发送排队中的下一条。
 * 不能只看「模型流结束」——那一轮还在落库时就把下一条塞进去，会把两条的叶子与时间写乱。
 */
let runSettled: ((conversationId: string) => void) | null = null

export function onRunSettled(hook: (conversationId: string) => void): void {
  runSettled = hook
}

function notifyRunSettled(conversationId: string): void {
  runSettled?.(conversationId)
}

/**
 * 这一轮是否出现了新排队消息：会话 store 管队列，chat store 在轮次边界问它。
 * 有队列时当前回复提前收尾，收尾后由会话 store 把整队合并成下一条发送。
 */
let queuedPending: ((conversationId: string) => boolean) | null = null

export function onQueuedPending(hook: (conversationId: string) => boolean): void {
  queuedPending = hook
}

function hasQueued(conversationId: string): boolean {
  return queuedPending?.(conversationId) === true
}

/**
 * 一批里最多的模型往返次数：跑满这么多轮而模型仍在请求工具时自动接续往下跑。
 * 接续不清空上下文，只在接续点随请求补一条提醒（见 continuePrompt），
 * 不再像过去那样把「跑满一批」直接当成这一轮对话的失败。
 *
 * 取 32 是照顾编码这类「读文件 → 改文件 → 跑命令 → 看输出」连着一长串的多步任务：
 * 一步一轮、每轮还常带多个工具调用，16 轮对稍大的改造（跨几个文件的改动 + 反复跑测试）
 * 就会在中途被收尾，得靠用户回一句「继续」才接得上；翻到 32 轮让一批基本能覆盖
 * 一次完整的「改到测试通过」。批越大，接续提醒越稀（提醒本身是防无效轮转的），
 * 所以别再往上堆——要更长就加接续次数，单批的粒度留着报警用。
 */
const MAX_ROUNDS = 32
/**
 * 自动接续的次数：模型陷入死循环（反复调同一个工具）时靠它兜住额度，
 * 到顶后做一次不带工具的收尾调用，让模型按已完成的进度作答，任务本身不中断。
 *
 * 4 次接续 = 最多 5 批 ≈ 160 轮工具往返，够长任务跑完；同时这仍是「死循环能拖多久」
 * 的硬上限，接续点每批提醒一次、共提醒 4 次，模型不改做法就会被收尾交卷
 * （收尾记 done 并提示「回复继续可接着做」，损失的是这轮的长度、不是已完成的成果）。
 */
const MAX_ROUNDS_CONTINUE = 4
/**
 * 一轮对话里模型调用的总轮数：工具往返 = 一批 MAX_ROUNDS 轮 × (1 + 接续次数)，
 * 另加最后 1 轮收尾调用（禁用工具、只出结论）。收尾不占用批次数，每一批都是完整的 MAX_ROUNDS 轮，
 * 否则「一批只有 1 轮、又不接续」这种极值会只剩一个收尾轮，一次工具都调不到。
 */
const MAX_ROUNDS_TOTAL = MAX_ROUNDS * (MAX_ROUNDS_CONTINUE + 1) + 1
/** 收尾调用的引导语：交代为什么不再给工具，并说明希望模型给出什么 */
const WRAPUP_PROMPT = `（系统提示：本轮对话已连续调用工具 ${MAX_ROUNDS_TOTAL - 1} 轮，达到上限，工具已禁用。请基于以上已经完成的工作直接给出最终答复：说明已完成的部分、尚未完成的部分，以及下一步建议。）`
/** 收尾后给用户的提示：说明这一轮是被上限收尾的，以及怎么接着做 */
const WRAPUP_NOTICE = `本轮已自动接续 ${MAX_ROUNDS_CONTINUE} 次（工具往返 ${MAX_ROUNDS_TOTAL - 1} 轮），达到上限后收尾：模型按当前进度作答，回复「继续」可接着做。`
/** 用户在本轮中途又发了消息：当前回复让位，队列合并成下一轮继续 */
const QUEUED_NOTICE = '收到新消息，当前回复已提前结束，合并后的新消息将继续发送。'
/**
 * 接续提示词：跑满一批 MAX_ROUNDS 轮而模型仍在请求工具时，随下一批的请求一起交给模型。
 * 讲清已经跑了多少轮、还剩多少轮，并点名无效轮转的风险（反复调同一个工具、同样的失败反复出现、
 * 拿不到新信息），让模型先自己纠正；不奏效才由总上限处的收尾调用兜底。
 * 提醒本身是链尾的 user 消息，比用户最初的提问更「新」，因此同时说明没有上述情况就照常推进，
 * 避免模型为了省轮数提前收尾。
 */
function continuePrompt(round: number): string {
  // 剩余的工具往返轮数：扣掉最后那一轮收尾（它不执行工具）
  const left = MAX_ROUNDS_TOTAL - round - 1
  return [
    `（系统提示：你已连续调用工具 ${round} 轮仍在继续，单轮对话的工具往返上限是 ${MAX_ROUNDS_TOTAL - 1} 轮，剩余 ${left} 轮，用完后还有一次不带工具的收尾。`,
    '请注意无效轮转的风险：如果发现自己在重复调用同一个工具、用同样的参数反复失败，或已经拿不到新信息，',
    '请立即改变做法——换参数或换工具、把多次调用合并成一次，或者基于已完成的工作直接给出结论并说明卡在哪里；',
    '没有上述情况就照常推进原计划，不要为了省轮数提前收尾。',
    '已经成功过的调用不要重复执行。',
    left <= MAX_ROUNDS ? '这是最后一次接续，用完即进入收尾（不再执行工具）。）' : '）'
  ].join('')
}
/** 单次工具返回进入调用链的长度上限：够模型判断，又不至于把上下文撑爆 */
const MAX_RESULT = 64 * 1024
/** @ 引用文件进入上下文的长度上限 */
const MAX_MENTION = 64 * 1024
/** 流式正文的检查点间隔：工具结果在边界强制落盘，正文最多损失一个很短的时间片 */
const STREAM_CHECKPOINT_MS = 750
/** 最终收尾写库失败后的自动重试间隔；期间会话保持 running，避免新任务覆盖未保存的结果 */
const FINAL_CHECKPOINT_RETRY_MS = 2_000
/** 正常退出时等待在途收尾的上限；主进程给渲染进程的总退出窗口是 3 秒 */
const EXIT_RUN_GRACE_MS = 1_500

interface ChatOutcome {
  aborted: boolean
  error: string
  toolCalls: ChatToolCall[]
}

interface ChatReference {
  rounds?: ChatRound[]
  mentions?: ChatMention[]
}

interface StreamToolFragment {
  index?: number
  id?: string
  function?: {
    name?: string
    arguments?: string
  }
}

interface ReplyCheckpoint {
  session: ChatSession
  reply: ChatMessage
  question: ChatMessage | null
  pending: boolean
  saving: Promise<void> | null
  timer: number | null
  retryTimer: number | null
  lastSavedAt: number
  finalizing: boolean
}

function truncate(text: string, limit: number): string {
  const value = String(text ?? '')
  return value.length > limit ? `${value.slice(0, limit)}\n…（已截断）` : value
}

function messageOf(error: unknown): string {
  const text = (error as { message?: unknown })?.message
  return String(text || error || '操作失败')
}

function settlePendingCalls(
  reply: ChatMessage,
  status: 'failed' | 'stopped',
  reason: string
): void {
  for (const round of reply.rounds) {
    for (const call of round.calls) {
      if ('running' !== call.status) continue
      call.status = status
      if (!call.result) call.result = reason
    }
  }
}

/** 参考的落地形状：助手消息存调用链，用户消息存 @ 引用 */
function parseReference(text: string): ChatReference {
  if (!text) return {}
  try {
    const value: unknown = JSON.parse(text)
    return value && 'object' === typeof value ? (value as ChatReference) : {}
  } catch {
    return {}
  }
}

/** 把内存消息转成库行；同一 id 的后续检查点会覆盖这一行 */
export function persistedMessage(message: ChatMessage): {
  id: string
  parentId: string
  role: string
  content: string
  reasoning: string
  reference: string
  status: string
  createdAt: string
} {
  const reference = message.rounds.length
    ? { rounds: message.rounds }
    : message.mentions.length
      ? { mentions: message.mentions }
      : null
  return {
    id: message.id,
    parentId: message.parentId,
    role: message.role,
    content: message.content,
    reasoning: message.reasoning,
    reference: reference ? JSON.stringify(reference) : '',
    status: message.status,
    createdAt: message.createdAt
  }
}

/**
 * 上次进程在生成中途退出时，库里会留下 running 检查点。
 * 恢复成「已中断」而不是继续显示成永远正在输出，同时保留已经落下的正文和工具结果。
 */
export function recoverInterrupted(messages: ChatMessage[]): boolean {
  let changed = false
  for (const message of messages) {
    if ('assistant' !== message.role) continue
    let messageChanged = false
    if ('running' === message.status) {
      message.status = 'stopped'
      messageChanged = true
    }
    for (const round of message.rounds ?? []) {
      for (const call of round.calls ?? []) {
        if ('running' !== call.status) continue
        call.status = 'stopped'
        if (!call.result) call.result = '客户端上次退出时该调用尚未完成'
        messageChanged = true
      }
    }
    changed ||= messageChanged
  }
  return changed
}

export const useChatStore = defineStore('chat', () => {
  const sessions = ref<Record<string, ChatSession>>({})
  /**
   * 已被删除 / 清空、但生成尚未收尾的回复：按回复标识记着怎么收口。
   * 删除与落库是两条异步链，只在删除那一刻检查「还在不在跑」挡不住已经开始的落库，
   * 所以 persist 的每个写入点都会再看一眼这里，作废后把刚写进去的记录再清掉。
   */
  const dropped = new Map<string, DropMode>()
  /** 仍在生成或尚未完成最终落库的回复；退出、清空与删除都要先处理这里的检查点 */
  const checkpoints = new Map<string, ReplyCheckpoint>()
  /** 发送入口到首批检查点写完之间的任务；退出登录也要等它们，避免初始化完成后又在旧库里启动模型 */
  const initialTasks = new Map<string, Promise<void>>()
  /** 所有已启动的生成任务：退出登录时等它们停止，避免旧账号的收尾写进下一个账号的库 */
  const runTasks = new Map<string, Promise<void>>()

  function ensure(conversationId: string, profile: Partial<ChatProfile> = {}): ChatSession {
    if (!sessions.value[conversationId]) {
      sessions.value[conversationId] = {
        conversationId,
        projectId: profile.projectId ?? '',
        model: profile.model ?? '',
        systemPrompt: profile.systemPrompt ?? '',
        memory: profile.memory ?? '',
        memoryEnabled: profile.memoryEnabled ?? false,
        thinkEffort: profile.thinkEffort ?? '',
        messages: [],
        leafId: '',
        running: false,
        stopping: false,
        activeReplyId: '',
        loading: false,
        error: '',
        notice: '',
        agentRunToken: '',
        chat: null,
        tools: null,
        workspacePrompt: '',
        messageTokens: {},
        loaded: false
      }
    }
    const session = sessions.value[conversationId]
    // 项目口径跟着会话走：项目被删除时保留原来的模型，其余以项目当前配置为准
    if (profile.projectId && profile.projectId !== session.projectId) {
      session.projectId = profile.projectId
      // 换了项目就换了工具清单与工作区说明：都重新取，用量估算按新的一份算
      session.tools = null
      session.workspacePrompt = ''
    }
    if (profile.model) session.model = profile.model
    if (profile.systemPrompt !== undefined) session.systemPrompt = profile.systemPrompt
    if (profile.memory !== undefined) session.memory = profile.memory
    if (profile.memoryEnabled !== undefined) {
      // 记忆开关改变工具清单：清掉预取的定义，用量圆环与下一次发送都按新的一份算
      if (profile.memoryEnabled !== session.memoryEnabled) {
        session.tools = null
      }
      session.memoryEnabled = profile.memoryEnabled
    }
    if (profile.thinkEffort !== undefined) session.thinkEffort = profile.thinkEffort
    return session
  }

  function find(conversationId: string): ChatSession | null {
    return sessions.value[conversationId] ?? null
  }

  /**
   * 预取工具定义与本机说明：两者都参与用量估算，等发送时再取就晚了。
   * 工具集由所有会话共用；这里同步当前共享定义，缓存未刷新时不会重复请求服务端。
   */
  async function warmup(session: ChatSession): Promise<void> {
    try {
      const project = useProjectStore().find(session.projectId)
      session.tools = markRaw((await buildTools(project ?? null)).definitions)
    } catch (error) {
      session.tools = null
      if (error instanceof ToolDescriptionError) session.error = error.message
      // 其它工具服务异常不挡会话展示，发送前会再校验一次
    }
    session.workspacePrompt = await fetchWorkspacePrompt(session.projectId)
  }

  /** 打开会话：历史只来自本地库（服务端不存会话），没有则从缓存上屏 */
  async function open(
    conversationId: string,
    profile: Partial<ChatProfile> = {},
    leafId = ''
  ): Promise<ChatSession> {
    const session = ensure(conversationId, profile)
    if (!session.loaded) {
      const cached = await ConversationDb.messages(conversationId)
      const restored = !session.messages.length
      if (restored) {
        session.messages = cached.map((item) => {
          const reference = parseReference(item.reference)
          return reactive<ChatMessage>({
            id: item.id,
            parentId: item.parentId,
            role: item.role,
            content: item.content,
            reasoning: item.reasoning,
            status: item.status || 'done',
            createdAt: item.createdAt,
            rounds: Array.isArray(reference.rounds) ? reference.rounds : [],
            mentions: Array.isArray(reference.mentions) ? reference.mentions : []
          })
        })
      }
      // 只在从库里装载时推断“上次进程中断”；后台任务可能已有内存消息，不能把正在跑的轮次误标成中断。
      const recovered = restored ? recoverInterrupted(session.messages) : false
      // 分支尾：库里记着就用它；v8 之前的历史会话没有记录，
      // 按入库顺序把消息串成一条链（线性历史就是一条链），并把叶子补写进库
      const stored = leafId && session.messages.some((item) => item.id === leafId) ? leafId : ''
      let rebuiltLinear = false
      if (stored) {
        session.leafId = stored
      } else if (session.messages.length) {
        session.messages.forEach((item, index) => {
          item.parentId = index > 0 ? session.messages[index - 1].id : ''
        })
        session.leafId = session.messages[session.messages.length - 1].id
        rebuiltLinear = true
      }
      if ((recovered || rebuiltLinear) && session.messages.length) {
        await ConversationDb.saveMessages(
          conversationId,
          session.messages.map(persistedMessage),
          session.leafId
        ).catch((error) => {
          session.error = `历史恢复状态暂未保存：${messageOf(error)}`
        })
      }
      session.loaded = true
    }
    // 工具定义要参与用量估算（不少项目挂了几只工具），打开会话时先取一次
    void warmup(session)
    return session
  }

  /**
   * 写一次检查点。消息与分支尾在一个主进程事务里提交：
   * 工具刚执行完、模型还在下一轮思考时退出，已经拿到的输出和结果仍能在重启后恢复。
   */
  async function writeCheckpoint(entry: ReplyCheckpoint, updatedAt?: string): Promise<void> {
    if (await dropIfNeeded(entry.session, entry.reply)) return
    const rows = entry.question
      ? [persistedMessage(entry.question), persistedMessage(entry.reply)]
      : [persistedMessage(entry.reply)]
    await ConversationDb.saveMessages(
      entry.session.conversationId,
      rows,
      entry.session.leafId,
      updatedAt
    )
    entry.lastSavedAt = Date.now()
  }

  /** 串行刷新；保存期间又有增量时，保存完继续追到最新状态 */
  async function flushCheckpoint(entry: ReplyCheckpoint, updatedAt?: string): Promise<void> {
    if (entry.saving) {
      entry.pending = true
      await entry.saving
      if (entry.pending) await flushCheckpoint(entry, updatedAt)
      return
    }
    if (!entry.pending) return

    entry.pending = false
    entry.saving = writeCheckpoint(entry, updatedAt)
    try {
      await entry.saving
    } catch (error) {
      entry.pending = true
      throw error
    } finally {
      entry.saving = null
    }
    // 保存期间继续流式输出时，再排一个短定时器追到最新内容。
    if (entry.pending && !entry.finalizing) scheduleCheckpoint(entry)
  }

  /** 流式内容按短时间片保存；定时器失败时保留 dirty 标记，下一个边界会重试 */
  function scheduleCheckpoint(entry: ReplyCheckpoint): void {
    entry.pending = true
    if (entry.timer !== null || entry.saving) return
    const delay = Math.max(0, STREAM_CHECKPOINT_MS - (Date.now() - entry.lastSavedAt))
    entry.timer = window.setTimeout(() => {
      entry.timer = null
      void flushCheckpoint(entry).catch((error) => {
        console.warn('Chat checkpoint delayed:', error)
      })
    }, delay)
  }

  function checkpointNow(replyId: string): Promise<void> {
    const entry = checkpoints.get(replyId)
    if (!entry) return Promise.resolve()
    entry.pending = true
    if (entry.timer !== null) {
      window.clearTimeout(entry.timer)
      entry.timer = null
    }
    return flushCheckpoint(entry)
  }

  function dropCheckpoint(entry: ReplyCheckpoint): void {
    if (entry.timer !== null) window.clearTimeout(entry.timer)
    if (entry.retryTimer !== null) window.clearTimeout(entry.retryTimer)
    checkpoints.delete(entry.reply.id)
    dropped.delete(entry.reply.id)
    if (entry.session.activeReplyId === entry.reply.id) entry.session.activeReplyId = ''
  }

  /** 最终检查点：消息、分支尾和会话最后活动时间一次提交，成功后这一轮才算真正收尾 */
  async function completeCheckpoint(entry: ReplyCheckpoint): Promise<void> {
    if (entry.finalizing || !checkpoints.has(entry.reply.id)) return
    entry.finalizing = true
    entry.pending = true
    if (entry.timer !== null) window.clearTimeout(entry.timer)
    if (entry.retryTimer !== null) window.clearTimeout(entry.retryTimer)
    entry.timer = null
    entry.retryTimer = null
    try {
      if (await dropIfNeeded(entry.session, entry.reply)) {
        entry.session.running = false
        notifyRunSettled(entry.session.conversationId)
        dropCheckpoint(entry)
        return
      }
      await flushCheckpoint(entry)
      if (await dropIfNeeded(entry.session, entry.reply)) {
        entry.session.running = false
        notifyRunSettled(entry.session.conversationId)
        dropCheckpoint(entry)
        return
      }
      const at = new Date().toISOString()
      const rows = entry.question
        ? [persistedMessage(entry.question), persistedMessage(entry.reply)]
        : [persistedMessage(entry.reply)]
      await ConversationDb.saveMessages(
        entry.session.conversationId,
        rows,
        entry.session.leafId,
        at
      )
      if (await dropIfNeeded(entry.session, entry.reply)) {
        entry.session.running = false
        notifyRunSettled(entry.session.conversationId)
        dropCheckpoint(entry)
        return
      }
      entry.lastSavedAt = Date.now()
      if (entry.session.error.startsWith('执行结果尚未保存')) entry.session.error = ''
      entry.session.running = false
      notifyMessageLanded(entry.session.conversationId, at)
      notifyRunSettled(entry.session.conversationId)
      dropCheckpoint(entry)
    } catch (error) {
      const reason = messageOf(error)
      entry.session.error = `执行结果尚未保存，正在自动重试：${reason}`
      console.error('Failed to save chat checkpoint:', error)
      entry.finalizing = false
      entry.retryTimer = window.setTimeout(() => {
        entry.retryTimer = null
        void completeCheckpoint(entry)
      }, FINAL_CHECKPOINT_RETRY_MS)
      return
    } finally {
      entry.finalizing = false
    }
  }

  /**
   * 退出/切换账号前的最后一道屏障：先停流和本地工具，再把所有未收尾结果强制写成「已中断」。
   * 正常收尾仍由 completeCheckpoint 负责；这里只处理进程即将消失时的快照。
   */
  async function flushBeforeExit(): Promise<void> {
    for (const session of Object.values(sessions.value)) halt(session)

    const waitFor = async (tasks: Promise<void>[]): Promise<void> => {
      if (!tasks.length) return
      let timer: number | null = null
      await Promise.race([
        Promise.allSettled(tasks),
        new Promise<void>((resolve) => {
          timer = window.setTimeout(resolve, EXIT_RUN_GRACE_MS / 2)
        })
      ])
      if (timer !== null) window.clearTimeout(timer)
    }
    await waitFor([...initialTasks.values()])
    await waitFor([...runTasks.values()])

    await Promise.all(
      [...checkpoints.values()].map(async (entry) => {
        if (entry.timer !== null) window.clearTimeout(entry.timer)
        if (entry.retryTimer !== null) window.clearTimeout(entry.retryTimer)
        entry.timer = null
        entry.retryTimer = null
        if ('running' === entry.reply.status) entry.reply.status = 'stopped'
        for (const round of entry.reply.rounds) {
          for (const call of round.calls) {
            if ('running' !== call.status) continue
            call.status = 'stopped'
            if (!call.result) call.result = '客户端退出时该调用尚未完成'
          }
        }
        entry.pending = true
        try {
          await flushCheckpoint(entry)
        } catch (error) {
          entry.session.error = `退出前保存执行结果失败：${messageOf(error)}`
          console.error('Failed to flush chat checkpoint before exit:', error)
        }
      })
    )
  }

  /**
   * 发送一轮：先把用户消息与待填的回复上屏，再由调用循环逐段填充。
   *
   * onStart 在这一轮真的发得出去、消息落库之前调用：新会话借此先把自己的会话行写进库
   * （还没落库的会话不在历史里，发送是它进历史的时机），参数是这一轮用的落库时刻——
   * 会话行与列表都按它记「最后一条消息的时间」，两边不会差出第二个值。
   */
  async function send(
    params: SendParams,
    onStart?: (at: string) => Promise<void>
  ): Promise<boolean> {
    const session = ensure(params.conversationId, params)
    if (session.running) return false
    if (!session.model) {
      session.error = '该项目没有选择模型，请先在项目管理里配置'
      return false
    }
    // 工具装配先于用户消息和模型调用：说明缺失时保持草稿不动，直接拒绝本轮发送
    const project = useProjectStore().find(session.projectId)
    try {
      session.tools = markRaw((await buildTools(project ?? null)).definitions)
      session.error = ''
    } catch (error) {
      session.tools = null
      session.error = messageOf(error)
      return false
    }
    // 分支位置：不传就是接在当前分支尾；重新生成时 parentId 指向那条提问
    const parentId = params.parentId === undefined ? session.leafId : String(params.parentId ?? '')
    const reuseQuestion = true === params.reuseQuestion
    const now = new Date().toISOString()
    const question = reuseQuestion
      ? null
      : reactive<ChatMessage>({
          id: crypto.randomUUID(),
          parentId,
          role: 'user',
          content: params.query,
          reasoning: '',
          status: 'done',
          createdAt: now,
          rounds: [],
          mentions: (params.mentions ?? []).slice()
        })
    const reply = reactive<ChatMessage>({
      id: crypto.randomUUID(),
      parentId: question ? question.id : parentId,
      role: 'assistant',
      content: '',
      reasoning: '',
      status: 'running',
      createdAt: now,
      rounds: [],
      mentions: []
    })
    const previousLeaf = session.leafId
    if (question) session.messages.push(question)
    session.messages.push(reply)
    // 本轮回复即新的分支尾：重新生成时上一次的输出随之让位（消息本身仍保留，可切回）
    session.leafId = reply.id
    // 删除 / 清空要能精确作废这一轮：记到会话上，直到这一轮真正落库结束
    session.activeReplyId = reply.id
    session.error = ''
    session.notice = ''
    session.agentRunToken = beginAgentParentRun(session.conversationId)
    // 新一轮从「没被停止」起步：上一轮按过停止的标记不能带进来，否则这一轮发不出去就被收尾
    session.stopping = false
    session.running = true
    const entry: ReplyCheckpoint = {
      session,
      reply,
      question,
      pending: true,
      saving: null,
      timer: null,
      retryTimer: null,
      lastSavedAt: 0,
      finalizing: false
    }
    checkpoints.set(reply.id, entry)
    let releaseInitial!: () => void
    const initialTask = new Promise<void>((resolve) => {
      releaseInitial = resolve
    })
    initialTasks.set(reply.id, initialTask)
    try {
      // 会话行先于消息落库：新会话在这里进历史
      if (onStart) await onStart(now)
      // 问题和空的运行中回复一起原子落盘；此后模型和工具产生的每个结果都可覆盖这条检查点。
      await flushCheckpoint(entry, now)
      notifyMessageLanded(params.conversationId, now)
    } catch (error) {
      halt(session)
      dropCheckpoint(entry)
      session.messages = session.messages.filter(
        (message) => message.id !== reply.id && message.id !== question?.id
      )
      session.leafId = previousLeaf
      session.activeReplyId = ''
      session.running = false
      session.error = `消息未发送：执行结果无法写入本地库（${messageOf(error)}）`
      releaseInitial()
      initialTasks.delete(reply.id)
      return false
    }
    const task = run(session, reply, entry)
    runTasks.set(reply.id, task)
    releaseInitial()
    initialTasks.delete(reply.id)
    void task
      .catch((error) => {
        reply.status = 'failed'
        session.error = messageOf(error)
      })
      .finally(() => {
        if (runTasks.get(reply.id) === task) runTasks.delete(reply.id)
      })
    return true
  }

  /**
   * 调用循环：模型给出工具调用就执行，把结果接回上下文再让模型继续，直到模型给出最终回复。
   * 跑满一批 MAX_ROUNDS 轮而模型仍在请求工具时自动接续下一批：上下文不清空，
   * 只在接续点随请求补一条「提防无效轮转」的提醒（continuePrompt），让模型先自己纠正；
   * 接续 MAX_ROUNDS_CONTINUE 次后再做一次不带工具的收尾调用（额外一轮，不占批次）：
   * 按已完成的进度作答，而不是判失败。
   * 每轮的思考过程、正文与工具调用都实时写在会话对象上，界面据此即时展示。
   */
  async function run(
    session: ChatSession,
    reply: ChatMessage,
    checkpoint: ReplyCheckpoint
  ): Promise<void> {
    try {
      const project = useProjectStore().find(session.projectId)
      const tools = await buildTools(project ?? null)
      // 这一轮实际下发的定义就是估算用的那一份（圆环的用量与请求口径保持一致）
      session.tools = markRaw(tools.definitions)
      // 本机说明同样每轮重取：目录、主目录或策略刚改过，这一轮就用新的
      session.workspacePrompt = await fetchWorkspacePrompt(session.projectId)
      // 历史：当前分支路径上除本轮回复之外的全部消息（本轮提问已在路径尾）
      const history = visible(session).filter((item) => item !== reply)
      const chain: ModelChatMessage[] = []
      for (const item of history) {
        if ('user' === item.role)
          chain.push({ role: 'user', content: await userContent(session, item) })
        else chain.push(...assistantChain(item))
      }
      // 接续提醒在链里的下标：只保留最新一条（换新的之前先把上一条撤掉），避免链里堆起多条催促
      let hintIndex = -1
      for (let index = 0; index < MAX_ROUNDS_TOTAL; index++) {
        // 长期记忆每轮重取：memory 工具的 write 刚落进项目，从这一轮起就随请求下发
        // （记忆按项目存、本项目所有会话共用；其余口径仍按会话打开时的那份项目配置走）
        const fresh = useProjectStore().find(session.projectId)
        if (fresh) {
          session.memory = fresh.memory
          session.memoryEnabled = fresh.memoryEnabled
        }
        // 停止落在这一轮之前（工具执行中、等审批中按下的那一次）：不再发起模型调用
        if (session.stopping) {
          reply.status = 'stopped'
          break
        }
        // 上一轮工具已跑完、用户又发了消息：不再发起下一次模型调用，让队列接管下一轮
        if (index > 0 && hasQueued(session.conversationId)) {
          reply.status = 'stopped'
          session.notice = QUEUED_NOTICE
          break
        }
        // 收尾轮：工具往返跑满后再加的一轮，不再给工具，改为引导模型给结论
        const closing = index === MAX_ROUNDS_TOTAL - 1
        const round = reactive<ChatRound>({ content: '', reasoning: '', calls: [] })
        reply.rounds.push(round)
        if (closing) {
          // 收尾轮：撤掉还在链上的接续提醒，换成收尾引导语（两条并存只会互相打架）
          if (hintIndex >= 0) chain.splice(hintIndex, 1)
          hintIndex = -1
          chain.push({ role: 'user', content: WRAPUP_PROMPT })
        }
        // 接续点：跑满一批仍在请求工具，随下一批把「提防无效轮转」的提醒一并交给模型
        else if (index > 0 && 0 === index % MAX_ROUNDS) {
          if (hintIndex >= 0) chain.splice(hintIndex, 1)
          chain.push({ role: 'user', content: continuePrompt(index) })
          hintIndex = chain.length - 1
        }
        const outcome = await chatOnce(session, reply, round, chain, tools, closing)
        // 模型这一轮已经收尾（包括正文与 tool_calls）：先把检查点推平，再执行工具。
        await checkpointNow(reply.id)
        if (outcome.aborted) {
          settlePendingCalls(reply, 'stopped', '已停止，调用未执行')
          reply.status = 'stopped'
          break
        }
        // 模型流刚收尾那一刻按下的停止：这一轮不再往下推
        if (session.stopping) {
          settlePendingCalls(reply, 'stopped', '已停止，调用未执行')
          reply.status = 'stopped'
          break
        }
        if (outcome.error) {
          settlePendingCalls(reply, 'failed', `模型调用中断，未执行：${outcome.error}`)
          session.error = outcome.error
          reply.status = 'failed'
          if (!reply.content) reply.content = outcome.error
          break
        }
        // 正文取最近一个真有输出的轮次：纯工具调用的一轮不该把上文抹掉
        if (round.content) reply.content = round.content
        reply.reasoning = round.reasoning
        if (!outcome.toolCalls.length) {
          reply.status = 'done'
          if (closing) session.notice = WRAPUP_NOTICE
          break
        }
        // 模型这一轮还要调工具，但用户已经发了新消息：本轮工具不再执行，先把队列送进去
        if (hasQueued(session.conversationId)) {
          outcome.toolCalls.forEach((call) => {
            if ('running' === call.status) {
              call.status = 'stopped'
              call.result = '收到新消息，本轮未执行'
            }
          })
          reply.status = 'stopped'
          session.notice = QUEUED_NOTICE
          break
        }
        // 收尾轮没下发工具定义，模型正常不会再要工具；万一要了也不再执行，按已有内容收尾
        if (closing) {
          outcome.toolCalls.forEach((call) => {
            call.status = 'failed'
            call.result = '已达单轮工具调用上限，未执行'
          })
          reply.status = 'done'
          session.notice = WRAPUP_NOTICE
          break
        }
        // 调用链续写：先补这一轮的助手消息（含 tool_calls），再逐条补工具返回
        chain.push({
          role: 'assistant',
          content: round.content,
          toolCalls: outcome.toolCalls.map((call) => ({
            id: call.id,
            type: 'function',
            function: { name: call.name, arguments: call.args }
          }))
        })
        // 多个专业子 Agent 是彼此独立的委派，可以并行；只要混入本地工具就保持串行，
        // 避免文件、命令或浏览器资源在缺少资源锁时并发。
        const parallelAgents =
          outcome.toolCalls.length > 1 && outcome.toolCalls.every((call) => 'agent' === call.scope)
        if (parallelAgents) {
          await Promise.all(
            outcome.toolCalls.map(async (call) => {
              await execute(call, tools, session)
              // 并行委派各自完成就立即落盘，不能等最慢的一只返回后才保存其他结果。
              await checkpointNow(reply.id)
            })
          )
        } else {
          for (const call of outcome.toolCalls) {
            // 停止落在同一轮的多次调用之间：这一轮剩下的调用不再执行
            if (session.stopping) {
              if ('running' === call.status) {
                call.status = 'stopped'
                call.result = '已停止，未执行'
              }
              continue
            }
            await execute(call, tools, session)
            // 同一轮的多次串行调用逐次检查点，后续调用失败或进程退出不会带走前面的结果。
            await checkpointNow(reply.id)
          }
        }
        for (const call of outcome.toolCalls) {
          chain.push({
            role: 'tool',
            toolCallId: call.id,
            name: call.name,
            content: truncate(call.result || '调用未完成', MAX_RESULT)
          })
        }
        // 工具跑完再看一眼：本地命令与等审批的那一步都可能刚被停止收掉
        if (session.stopping) {
          reply.status = 'stopped'
          break
        }
      }
    } catch (error) {
      settlePendingCalls(reply, 'failed', `本轮异常中断，未执行：${messageOf(error)}`)
      reply.status = 'failed'
      session.error = messageOf(error)
      if (!reply.content) reply.content = session.error
    } finally {
      session.chat = null
      // 收尾与最终检查点都属于这一轮；完成前 running 保持为真，队列不会抢跑。
      await completeCheckpoint(checkpoint)
    }
  }

  /**
   * 一次模型调用：增量写进当前轮次，返回这一轮的完整结果。
   *
   * 正文增量同时写回消息本身：时间线不渲染末轮的正文（避免与气泡里的正文重复），
   * 这一处赋值就是正文逐字上屏的来源；本轮结束时调用方再落一次值，与这里一致。
   * closing 为收尾调用：不下发工具定义，模型只能给结论（调用链与工具清单保持原样）。
   */
  function chatOnce(
    session: ChatSession,
    reply: ChatMessage,
    round: ChatRound,
    chain: ModelChatMessage[],
    tools: ToolSet,
    closing = false
  ): Promise<ChatOutcome> {
    return new Promise<ChatOutcome>((settle) => {
      let failure = ''
      const chat = markRaw(new ModelChat())
      session.chat = chat
      chat.start({
        model: session.model,
        systemPrompt: systemPromptOf(session),
        thinkEffort: session.thinkEffort,
        messages: chain,
        tools: closing ? [] : tools.definitions,
        onDelta: (delta) => {
          if (delta.content) {
            round.content += delta.content
            reply.content = round.content
          }
          if (delta.reasoning) round.reasoning += delta.reasoning
          if (delta.toolCalls?.length) mergeCalls(round, delta.toolCalls, tools)
          const checkpoint = checkpoints.get(reply.id)
          if (checkpoint) scheduleCheckpoint(checkpoint)
        },
        onError: (message) => {
          failure = message
        },
        onClose: ({ aborted, error }) => {
          session.chat = null
          const calls = round.calls.filter(Boolean)
          // 个别网关不推 id：补一个本地标识，调用链里的 tool 返回才有东西可对应
          calls.forEach((call, index) => {
            if (!call.id) call.id = `call_${Date.now()}_${index}`
          })
          settle({ aborted, error: error || failure, toolCalls: calls })
        }
      })
    })
  }

  /** 工具调用分片合并：按 index 拼接 id、函数名与参数增量（流式期间就能看到调的是什么） */
  function mergeCalls(round: ChatRound, fragments: StreamToolFragment[], tools: ToolSet): void {
    for (const fragment of fragments) {
      const index = Math.max(0, Number(fragment?.index ?? 0) || 0)
      if (!round.calls[index]) {
        round.calls[index] = reactive<ChatToolCall>({
          id: '',
          name: '',
          args: '',
          status: 'running',
          result: '',
          duration: 0,
          scope: 'local',
          label: ''
        })
      }
      const call = round.calls[index]
      if (fragment?.id) call.id = String(fragment.id)
      const fn = fragment?.function ?? {}
      if (fn.name) call.name = String(fn.name)
      if (fn.arguments) call.args += String(fn.arguments)
      const info = tools.catalog[call.name]
      call.scope = info?.scope ?? 'local'
      call.label = info?.label ?? call.name
    }
  }

  /**
   * 执行一次工具调用：本地工具走主进程，项目工具走服务端；失败同样进调用链，模型可据此修正。
   * 会话标识随调用一起走：本地工具需要审批时，申请据此交回本段对话（各对话互不串门）。
   *
   * 这段会话刚被停止时不执行（同一轮里排在后面的调用直接按「已停止」记账）；
   * 执行期间被停止的，这一次也不按失败算——本地命令已被主进程收掉、申请按拒绝处理，
   * 返回原文里带着「已停止」的原因，这里只把状态从红改成「已停止」。
   */
  async function execute(call: ChatToolCall, tools: ToolSet, session: ChatSession): Promise<void> {
    if (session.stopping) {
      call.status = 'stopped'
      call.result = '已停止，未执行'
      return
    }
    call.status = 'running'
    const started = Date.now()
    try {
      const result = await tools.invoke(
        call.name,
        parseArgs(call.args),
        session.projectId,
        session.conversationId,
        session.agentRunToken
      )
      call.result = 'string' === typeof result ? result : result.content
      call.status = 'success'
      if (!call.result) call.result = '（无返回内容）'
    } catch (error) {
      const message = messageOf(error)
      // 停止期间收尾的调用不按失败记账：原因（「命令已被停止」等）就在返回原文里，
      // 再加「调用失败」会读成命令自己跑砸了——这一格按「已停止」呈现
      call.result = session.stopping ? message : `调用失败：${message}`
      call.status = 'failed'
    }
    call.duration = Date.now() - started
    if (session.stopping) call.status = 'stopped'
  }

  /** 模型给出的参数：空串按空对象处理，解析失败直接作为调用失败回到模型 */
  function parseArgs(text: string): Record<string, unknown> {
    const value = String(text ?? '').trim()
    if (!value) return {}
    try {
      const parsed: unknown = JSON.parse(value)
      if (!parsed || 'object' !== typeof parsed || Array.isArray(parsed)) {
        throw new Error('参数必须是 JSON 对象')
      }
      return parsed as Record<string, unknown>
    } catch {
      throw new Error(`参数不是合法的 JSON 对象：${truncate(value, 200)}`)
    }
  }

  /**
   * 用户消息进入模型上下文的内容：输入原文 + @ 引用的文件与技能正文。
   * 引用每次都按引用重新读取（文件可能已被改动），读不到就把原因写进上下文，模型据此说明。
   * 折算出的用量顺手记在会话上：引用正文只在发送时读到，界面上的用量圆环过后还能按它算历史。
   */
  async function userContent(session: ChatSession, message: ChatMessage): Promise<string> {
    if (!message.mentions?.length) {
      session.messageTokens[message.id] = estimateTokens(message.content)
      return message.content
    }
    const parts = [message.content]
    for (const mention of message.mentions) {
      if ('file' === mention.type) {
        // 路径引用不区分文件与目录：主进程按实际类型给内容或清单，读不到就把原因写进上下文
        try {
          const target = await window.ipc.workspace.reference(
            session.projectId,
            mention.path,
            session.conversationId
          )
          const body = truncate(target.text, MAX_MENTION) || '（空目录或空文件）'
          parts.push(
            target.dir
              ? `【引用文件夹 ${target.path}】\n${body}`
              : `【引用文件 ${target.path}】\n\`\`\`\n${body}\n\`\`\``
          )
        } catch (error) {
          parts.push(`【引用文件 ${mention.path}】读取失败：${messageOf(error)}`)
        }
        continue
      }
      try {
        const skill = await window.ipc.skill.read(mention.name)
        parts.push(`【引用技能 ${mention.name}】\n${truncate(skill.content, MAX_MENTION)}`)
      } catch (error) {
        parts.push(`【引用技能 ${mention.name}】读取失败：${messageOf(error)}`)
      }
    }
    const content = parts.filter((item) => String(item ?? '').trim()).join('\n\n')
    session.messageTokens[message.id] = estimateTokens(content)
    return content
  }

  /** 助手消息还原成模型上下文：逐轮给出 assistant(tool_calls) 与对应的 tool 返回 */
  function assistantChain(message: ChatMessage): ModelChatMessage[] {
    const rows: ModelChatMessage[] = []
    for (const round of message.rounds ?? []) {
      // 中断的轮次可能只落了半条调用（没有 id / 没有结果）：这类调用不还原，
      // 否则会出现「有 tool_calls 却没有对应 tool 返回」的残缺报文，模型侧直接报错
      const calls = (round.calls ?? []).filter((call) => call && call.id)
      if (calls.length) {
        rows.push({
          role: 'assistant',
          content: round.content,
          toolCalls: calls.map((call) => ({
            id: call.id,
            type: 'function',
            function: { name: call.name, arguments: call.args }
          }))
        })
        for (const call of calls) {
          rows.push({
            role: 'tool',
            toolCallId: call.id,
            name: call.name,
            content: truncate(call.result || '（调用未完成）', MAX_RESULT)
          })
        }
        continue
      }
      if (round.content) rows.push({ role: 'assistant', content: round.content })
    }
    if (!rows.length && message.content) rows.push({ role: 'assistant', content: message.content })
    return rows
  }

  /**
   * 上下文用量（词元数，估算）：下一轮请求会带上的全部内容，口径与 `run()` 组装报文一致——
   * 系统提示词（含追加的本机说明）+ 当前分支（提问含 @ 引用正文、助手轮次的工具调用与返回）
   * + 工具定义 + 待发内容。
   *
   * 界面拿它画输入框旁的圆环：`used / 窗口大小`。@ 引用的正文只在发送时读到，
   * 已经发出去的那些按发送当时记下的折算结果计；还没发出去的（输入框里的 @）只按文字计。
   * 客户端没有分词器，这是按字符折算的估算值，只作体量提示。
   */
  function contextUsage(session: ChatSession, draft = ''): number {
    let total = 0
    // 系统提示词与工作区说明在服务端合成同一条 system 消息，这里也按合并后的一段算
    const systemPrompt = systemPromptOf(session)
    if (systemPrompt) total += estimateTokens(systemPrompt) + MESSAGE_OVERHEAD
    for (const item of visible(session)) {
      if ('user' === item.role) {
        const measured = session.messageTokens[item.id]
        total += (measured ?? estimateTokens(item.content)) + MESSAGE_OVERHEAD
        continue
      }
      for (const row of assistantChain(item)) {
        const content = Array.isArray(row.content)
          ? row.content
              .map((part) => ('text' === part.type ? part.text : ''))
              .filter(Boolean)
              .join('\n')
          : String(row.content ?? '')
        total += estimateTokens(content) + MESSAGE_OVERHEAD
        for (const call of row.toolCalls ?? []) total += estimateTokens(call.function.arguments)
      }
    }
    if (String(draft ?? '').trim()) total += estimateTokens(draft) + MESSAGE_OVERHEAD
    if (session.tools?.length) total += estimateJson(session.tools)
    return total
  }

  /**
   * 按作废方式清掉这一轮写进去的记录：
   * 删除会话连消息一起删；清空消息只删消息，并把分支尾收回起点。
   * 两类操作都是幂等的，生成结束时再执行一次即可兜住删除瞬间仍在途的写入。
   */
  async function dropRecords(conversationId: string, mode: DropMode): Promise<void> {
    if ('delete' === mode) {
      await ConversationDb.delete(conversationId)
      return
    }
    await ConversationDb.clearMessages(conversationId)
    await ConversationDb.setLeaf(conversationId, '')
  }

  /** 这一轮是否已被删除 / 清空：是就再清一遍，返回 true 让调用方停止后续写入 */
  async function dropIfNeeded(session: ChatSession, reply: ChatMessage): Promise<boolean> {
    const mode = dropped.get(reply.id)
    if (!mode) return false
    await dropRecords(session.conversationId, mode)
    return true
  }

  /* ------------------------------- 对话分支 ------------------------------- */

  /** 子消息：同一条父消息下的消息互为分支（按时间正序，同一时刻按入库顺序） */
  function children(session: ChatSession): Record<string, ChatMessage[]> {
    const order = new Map<string, number>()
    session.messages.forEach((item, index) => order.set(item.id, index))
    const map: Record<string, ChatMessage[]> = {}
    session.messages.forEach((item) => {
      const key = item.parentId || ''
      if (!map[key]) map[key] = []
      map[key].push(item)
    })
    Object.values(map).forEach((rows) =>
      rows.sort((a, b) => {
        const left = Date.parse(a.createdAt) || 0
        const right = Date.parse(b.createdAt) || 0
        if (left !== right) return left - right
        return (order.get(a.id) ?? 0) - (order.get(b.id) ?? 0)
      })
    )
    return map
  }

  /** 当前分支路径：从分支尾沿 parentId 回溯到根，按时间正序 */
  function visible(session: ChatSession): ChatMessage[] {
    const byId = new Map(session.messages.map((item) => [item.id, item]))
    let current = session.leafId ? byId.get(session.leafId) : undefined
    if (!current) current = session.messages[session.messages.length - 1]
    const path: ChatMessage[] = []
    const seen = new Set<string>()
    while (current && !seen.has(current.id)) {
      seen.add(current.id)
      path.unshift(current)
      current = current.parentId ? byId.get(current.parentId) : undefined
    }
    return path.length ? path : session.messages.slice()
  }

  /** 某条分支的末端：一直取最新的子消息，直到没有后续 */
  function tailOf(session: ChatSession, id: string): string {
    const map = children(session)
    let current = id
    const seen = new Set<string>()
    while (current && !seen.has(current)) {
      seen.add(current)
      const rows = map[current] ?? []
      if (!rows.length) return current
      current = rows[rows.length - 1].id
    }
    return current
  }

  /** 消息的分支位置：同父节点下排第几（1 起）；只有一条时不显示切换 */
  function branchOf(
    session: ChatSession,
    message: ChatMessage
  ): { index: number; count: number } | null {
    const rows = children(session)[message.parentId || ''] ?? []
    if (rows.length < 2) return null
    const index = rows.findIndex((item) => item.id === message.id)
    return index < 0 ? null : { index: index + 1, count: rows.length }
  }

  /** 切换分支：按兄弟节点前后移动，并把分支尾设到那条分支的末端 */
  function switchBranch(session: ChatSession, message: ChatMessage, step: number): void {
    const rows = children(session)[message.parentId || ''] ?? []
    const index = rows.findIndex((item) => item.id === message.id)
    if (index < 0 || rows.length < 2) return
    const next = rows[(index + step + rows.length) % rows.length]
    if (!next) return
    session.leafId = tailOf(session, next.id)
    void ConversationDb.setLeaf(session.conversationId, session.leafId)
  }

  /** 编辑提问后重新发送：新提问接在被编辑消息的父节点下，形成新分支 */
  function edit(session: ChatSession, message: ChatMessage, content: string): void {
    const text = String(content ?? '').trim()
    if (!text) return
    void send({
      conversationId: session.conversationId,
      projectId: session.projectId,
      model: session.model,
      systemPrompt: session.systemPrompt,
      memory: session.memory,
      memoryEnabled: session.memoryEnabled,
      thinkEffort: session.thinkEffort,
      query: text,
      mentions: message.mentions ?? [],
      parentId: message.parentId
    })
  }

  /** 重新生成：不重复落用户消息，只在同一条提问下新增一条回复 */
  function regenerate(session: ChatSession, message: ChatMessage): void {
    const question = session.messages.find((item) => item.id === message.parentId)
    if (!question) {
      session.error = '找不到该回复对应的提问，无法重新生成'
      return
    }
    void send({
      conversationId: session.conversationId,
      projectId: session.projectId,
      model: session.model,
      systemPrompt: session.systemPrompt,
      memory: session.memory,
      memoryEnabled: session.memoryEnabled,
      thinkEffort: session.thinkEffort,
      query: question.content,
      mentions: question.mentions ?? [],
      parentId: question.id,
      reuseQuestion: true
    })
  }

  /**
   * 停止生成：保留已上屏内容，标记为已停止。
   *
   * 断模型流只是其中一半。工具执行这一刻没有流可断（本轮模型流收尾时 session.chat 已置空），
   * 只断流的话这次调用会一直挂到它自己结束——本地命令尤其明显：它跑在主进程里，
   * 断流管不着，会一直跑到超时，跑完还把结果交回调用循环，整轮对话接着往下走。
   * 所以这里同时做两件事：标记 stopping 让调用循环按「已停止」收尾，
   * 并让主进程收掉这段会话还挂着的本地活儿（等答复的申请、正在跑的命令）。
   */
  function halt(session: ChatSession): boolean {
    // 停止链会同时碰模型流、子 Agent 与主进程任务；重复触发只会造成重复取消请求。
    if (session.stopping) return false
    session.stopping = true
    // 子 Agent 与主轮次共用这枚 token：先同步作废 token 并中止已启动的子 Agent，
    // 再断开主模型流，避免主流的 close 回调先推进到下一轮委派。
    cancelAgentRuns(session.conversationId, session.agentRunToken, true)
    session.chat?.stop()
    void window.ipc.workspace.cancel(session.conversationId)
    return true
  }

  function stop(conversationId: string): boolean {
    const session = find(conversationId)
    if (!session?.running || session.stopping) return false
    return halt(session)
  }

  /**
   * 删除会话 / 清空消息时作废当前这一轮：
   * 先收掉模型流与本地命令（等审批的申请也一并关掉），再给这一轮记上收口方式。
   * 删除把运行态从 store 里摘掉；清空保留会话本身，只清消息与分支尾。
   * 返回这一轮此前是否正在生成，调用方据此给出「已停止并清空」之类的提示。
   */
  function discard(conversationId: string, mode: DropMode): boolean {
    const session = find(conversationId)
    if (!session) return false
    const running = session.running
    if (session.activeReplyId) dropped.set(session.activeReplyId, mode)
    if (running) halt(session)
    if ('delete' === mode) {
      delete sessions.value[conversationId]
    } else {
      session.messages = []
      session.leafId = ''
      session.error = ''
      session.notice = ''
    }
    return running
  }

  /**
   * 退出登录：先停止并等当前收尾完成，再强制保存剩余检查点。
   * 主进程随后才会切库；不能让旧会话的异步收尾跨到下一条账号的数据库。
   */
  async function reset(): Promise<void> {
    for (const session of Object.values(sessions.value)) {
      // 退出登录同样不该把命令留在后台跑：那一轮已经不会再有人看结果了
      halt(session)
    }
    await Promise.allSettled([...initialTasks.values()])
    await Promise.allSettled([...runTasks.values()])
    await flushBeforeExit()
    sessions.value = {}
    clearTools()
    resetAgentRuntime()
  }

  return {
    sessions,
    ensure,
    find,
    open,
    send,
    stop,
    discard,
    reset,
    flushBeforeExit,
    visible,
    contextUsage,
    branchOf,
    switchBranch,
    edit,
    regenerate
  }
})
