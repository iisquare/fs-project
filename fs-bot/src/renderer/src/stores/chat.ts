import { markRaw, reactive, ref } from 'vue'
import { defineStore } from 'pinia'
import ConversationDb from '@renderer/db/ConversationDb'
import ModelChat, { type ModelChatMessage } from '@renderer/core/ModelChat'
import { buildTools, type ToolScope, type ToolSet } from '@renderer/core/Toolbox'
import { estimateJson, estimateTokens, MESSAGE_OVERHEAD } from '@renderer/core/Tokens'
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
  status: 'running' | 'success' | 'failed'
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
  /** 长期记忆：项目配置里手写的那段，随每轮请求下发（见 memoryBlock） */
  memory: string
  thinkEffort: string
  messages: ChatMessage[]
  /** 当前分支尾：页面只渲染它沿 parentId 回溯到根的那条路径 */
  leafId: string
  running: boolean
  loading: boolean
  error: string
  /** 提示（非错误）：这一轮被自动接续上限收尾等情况，界面上按警告色呈现 */
  notice: string
  /** 运行对象不参与响应式代理：连接这类宿主对象被代理后调用会报非法调用 */
  chat: ModelChat | null
  /** 本会话生效的工具定义：用量估算要把它算进去，打开会话时预取、每轮发送时按当次结果刷新 */
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

/** 项目口径：模型与生成参数都跟着会话所属项目走，打开会话与每轮发送都按它取 */
export interface ChatProfile {
  projectId: string
  model: string
  systemPrompt: string
  memory: string
  thinkEffort: string
}

/** 从项目取本会话要用的口径：项目已被删除或字段为空时给安全的空值 */
export function chatProfile(project?: Project | null): ChatProfile {
  return {
    projectId: String(project?.id ?? ''),
    model: String(project?.model ?? ''),
    systemPrompt: String(project?.systemPrompt ?? ''),
    memory: String(project?.memory ?? ''),
    thinkEffort: String(project?.thinkEffort ?? '')
  }
}

/**
 * 记忆块：项目配置里的长期记忆单独起一段、带标题下发，与系统提示词在视觉上分得开
 * （这段由用户手写，只加标题、不改写内容）。为空时不占位置。
 */
function memoryBlock(memory: string): string {
  const text = String(memory ?? '').trim()
  return text ? `# 记忆\n${text}` : ''
}

/**
 * 下发给服务端的系统提示词：项目自己的提示词在前，接着是项目记忆，最后是主进程给的本机说明；
 * 三段都为空时不下发。服务端把这一整段作为首条 system 消息补在最前。
 */
function systemPromptOf(
  session: Pick<ChatSession, 'systemPrompt' | 'memory' | 'workspacePrompt'>
): string {
  return [session.systemPrompt, memoryBlock(session.memory), session.workspacePrompt]
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
  /** @ 引用：本地文件与技能 */
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
 * 一批里最多的模型往返次数：跑满这么多轮而模型仍在请求工具时自动接续往下跑。
 * 接续不清空上下文，只在接续点随请求补一条提醒（见 continuePrompt），
 * 不再像过去那样把「跑满 16 轮」直接当成这一轮对话的失败。
 */
const MAX_ROUNDS = 16
/**
 * 自动接续的次数：模型陷入死循环（反复调同一个工具）时靠它兜住额度，
 * 到顶后做一次不带工具的收尾调用，让模型按已完成的进度作答，任务本身不中断。
 */
const MAX_ROUNDS_CONTINUE = 3
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

interface ChatOutcome {
  aborted: boolean
  error: string
  toolCalls: ChatToolCall[]
}

function truncate(text: string, limit: number): string {
  const value = String(text ?? '')
  return value.length > limit ? `${value.slice(0, limit)}\n…（已截断）` : value
}

function messageOf(error: unknown): string {
  const text = (error as any)?.message
  return String(text || error || '操作失败')
}

/** 参考的落地形状：助手消息存调用链，用户消息存 @ 引用 */
function parseReference(text: string): any {
  if (!text) return {}
  try {
    const value = JSON.parse(text)
    return value && 'object' === typeof value ? value : {}
  } catch {
    return {}
  }
}

export const useChatStore = defineStore('chat', () => {
  const sessions = ref<Record<string, ChatSession>>({})

  function ensure(conversationId: string, profile: Partial<ChatProfile> = {}): ChatSession {
    if (!sessions.value[conversationId]) {
      sessions.value[conversationId] = {
        conversationId,
        projectId: profile.projectId ?? '',
        model: profile.model ?? '',
        systemPrompt: profile.systemPrompt ?? '',
        memory: profile.memory ?? '',
        thinkEffort: profile.thinkEffort ?? '',
        messages: [],
        leafId: '',
        running: false,
        loading: false,
        error: '',
        notice: '',
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
    if (profile.thinkEffort !== undefined) session.thinkEffort = profile.thinkEffort
    return session
  }

  function find(conversationId: string): ChatSession | null {
    return sessions.value[conversationId] ?? null
  }

  /**
   * 预取工具定义与本机说明：两者都参与用量估算，等发送时再取就晚了。
   * 定义由 Toolbox 按项目工具清单缓存，重复调用只是取缓存；取不到就不算这一块。
   */
  async function warmup(session: ChatSession): Promise<void> {
    if (!session.tools) {
      try {
        const project = useProjectStore().find(session.projectId)
        session.tools = markRaw((await buildTools(project ?? null)).definitions)
      } catch {
        // 工具定义取不到（接口不通、权限不足）时不影响对话，只是估算里少算这块
      }
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
      if (!session.messages.length) {
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
      // 分支尾：库里记着就用它；v8 之前的历史会话没有记录，
      // 按入库顺序把消息串成一条链（线性历史就是一条链），并把叶子补写进库
      const stored = leafId && session.messages.some((item) => item.id === leafId) ? leafId : ''
      if (stored) {
        session.leafId = stored
      } else if (session.messages.length) {
        session.messages.forEach((item, index) => {
          item.parentId = index > 0 ? session.messages[index - 1].id : ''
        })
        session.leafId = session.messages[session.messages.length - 1].id
        void ConversationDb.setLeaf(conversationId, session.leafId)
      }
      session.loaded = true
    }
    // 工具定义要参与用量估算（不少项目挂了几只工具），打开会话时先取一次
    void warmup(session)
    return session
  }

  /**
   * 发送一轮：先把用户消息与待填的回复上屏，再由调用循环逐段填充。
   *
   * onStart 在这一轮真的发得出去、消息落库之前调用：新会话借此先把自己的会话行写进库
   * （还没落库的会话不在历史里，发送是它进历史的时机）。
   */
  async function send(params: SendParams, onStart?: () => Promise<void>): Promise<void> {
    const session = ensure(params.conversationId, params)
    if (session.running) return
    if (!session.model) {
      session.error = '该项目没有选择模型，请先在项目管理里配置'
      return
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
    if (question) session.messages.push(question)
    session.messages.push(reply)
    // 本轮回复即新的分支尾：重新生成时上一次的输出随之让位（消息本身仍保留，可切回）
    session.leafId = reply.id
    session.error = ''
    session.notice = ''
    session.running = true
    // 会话行先于消息落库：新会话在这里进历史
    if (onStart) await onStart()
    void ConversationDb.setLeaf(params.conversationId, reply.id)
    if (question) {
      void ConversationDb.appendMessage(params.conversationId, {
        id: question.id,
        parentId: question.parentId,
        role: 'user',
        content: params.query,
        reference: question.mentions.length ? JSON.stringify({ mentions: question.mentions }) : '',
        status: 'done',
        createdAt: now
      })
    }
    void run(session, reply)
  }

  /**
   * 调用循环：模型给出工具调用就执行，把结果接回上下文再让模型继续，直到模型给出最终回复。
   * 跑满一批 MAX_ROUNDS 轮而模型仍在请求工具时自动接续下一批：上下文不清空，
   * 只在接续点随请求补一条「提防无效轮转」的提醒（continuePrompt），让模型先自己纠正；
   * 接续 MAX_ROUNDS_CONTINUE 次后再做一次不带工具的收尾调用（额外一轮，不占批次）：
   * 按已完成的进度作答，而不是判失败。
   * 每轮的思考过程、正文与工具调用都实时写在会话对象上，界面据此即时展示。
   */
  async function run(session: ChatSession, reply: ChatMessage): Promise<void> {
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
      if ('user' === item.role) chain.push({ role: 'user', content: await userContent(session, item) })
      else chain.push(...assistantChain(item))
    }
    try {
      // 接续提醒在链里的下标：只保留最新一条（换新的之前先把上一条撤掉），避免链里堆起多条催促
      let hintIndex = -1
      for (let index = 0; index < MAX_ROUNDS_TOTAL; index++) {
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
        if (outcome.aborted) {
          reply.status = 'stopped'
          break
        }
        if (outcome.error) {
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
        for (const call of outcome.toolCalls) {
          await execute(call, tools, session)
          chain.push({
            role: 'tool',
            toolCallId: call.id,
            name: call.name,
            content: truncate(call.result, MAX_RESULT)
          })
        }
      }
    } catch (error) {
      reply.status = 'failed'
      session.error = messageOf(error)
      if (!reply.content) reply.content = session.error
    } finally {
      session.running = false
      session.chat = null
      void persist(session, reply)
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
  function mergeCalls(round: ChatRound, fragments: any[], tools: ToolSet): void {
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
   */
  async function execute(call: ChatToolCall, tools: ToolSet, session: ChatSession): Promise<void> {
    call.status = 'running'
    const started = Date.now()
    try {
      call.result = await tools.invoke(
        call.name,
        parseArgs(call.args),
        session.projectId,
        session.conversationId
      )
      call.status = 'success'
      if (!call.result) call.result = '（无返回内容）'
    } catch (error) {
      call.result = `调用失败：${messageOf(error)}`
      call.status = 'failed'
    }
    call.duration = Date.now() - started
  }

  /** 模型给出的参数：空串按空对象处理，解析失败直接作为调用失败回到模型 */
  function parseArgs(text: string): any {
    const value = String(text ?? '').trim()
    if (!value) return {}
    try {
      return JSON.parse(value)
    } catch {
      throw new Error(`参数不是合法的 JSON：${truncate(value, 200)}`)
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
        total += estimateTokens(row.content ?? '') + MESSAGE_OVERHEAD
        for (const call of row.toolCalls ?? []) total += estimateTokens(call.function.arguments)
      }
    }
    if (String(draft ?? '').trim()) total += estimateTokens(draft) + MESSAGE_OVERHEAD
    if (session.tools?.length) total += estimateJson(session.tools)
    return total
  }

  /** 落库：正文与最终思考过程各归各的列，工具调用链与 @ 引用进 reference */
  async function persist(session: ChatSession, reply: ChatMessage): Promise<void> {
    await ConversationDb.appendMessage(session.conversationId, {
      id: reply.id,
      parentId: reply.parentId,
      role: reply.role,
      content: reply.content,
      reasoning: reply.reasoning,
      reference: reply.rounds.length ? JSON.stringify({ rounds: reply.rounds }) : '',
      status: reply.status,
      createdAt: reply.createdAt
    })
    await ConversationDb.setLeaf(session.conversationId, session.leafId)
    await ConversationDb.touch(session.conversationId)
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
      thinkEffort: session.thinkEffort,
      query: question.content,
      mentions: question.mentions ?? [],
      parentId: question.id,
      reuseQuestion: true
    })
  }

  /** 停止生成：保留已上屏内容，标记为已停止 */
  function stop(conversationId: string): void {
    find(conversationId)?.chat?.stop()
  }

  /** 退出登录：断开所有连接，清空运行态 */
  function reset(): void {
    for (const session of Object.values(sessions.value)) {
      session.chat?.stop()
    }
    sessions.value = {}
  }

  return {
    sessions,
    ensure,
    find,
    open,
    send,
    stop,
    reset,
    visible,
    contextUsage,
    branchOf,
    switchBranch,
    edit,
    regenerate
  }
})
