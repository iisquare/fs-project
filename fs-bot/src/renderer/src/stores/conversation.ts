import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import ApiUtil from '@renderer/utils/ApiUtil'
import ConversationDb from '@renderer/db/ConversationDb'
import type { Conversation } from '@renderer/db/ConversationDb'
import {
  chatProfile,
  chatTitle,
  onMessageLanded,
  onQueuedPending,
  onRunSettled,
  useChatStore,
  type ChatMention,
  type SendParams
} from './chat'
import { useProjectStore } from './project'
import type { Project } from '@renderer/db/ProjectDb'

/** 生成中提交、等这一轮收尾后再发的一条消息 */
export interface QueuedSend {
  id: string
  params: SendParams
}

/** 输入框里尚未发送的内容：按会话保存，切走再切回原样恢复 */
export interface ComposerDraft {
  text: string
  mentions: ChatMention[]
}

/**
 * 会话列表：会话与消息都只存在本地，按项目归属；换机器 / 重装后历史不跟随。
 *
 * 列表按会话最后一条消息的时间倒序展示与排序（今天 / 昨天 / 本周 / 更早也是按它分组），
 * 时间随消息落库推进：chat store 每落一条消息就报一次，这里随即把这条会话挪到最前面。
 *
 * 新建的会话先只是一条草稿：发出第一条消息时才落库进历史，
 * 所以点开项目后没说话就退出，历史列表与检索里都不会多出一条空的「新对话」。
 */
export const useConversationStore = defineStore('conversation', () => {
  const conversations = ref<Conversation[]>([])
  /** 未落库的新会话：按 id 存着，界面照常渲染，历史列表 / 检索里还没有它 */
  const drafts = ref<Record<string, Conversation>>({})
  const currentId = ref('')
  const loading = ref(false)
  /** 打开会话历史（本地库 → chat store）时的等待状态 */
  const opening = ref(false)
  const filterProjectId = ref('')
  /** 每条会话各自的待发送队列：只活在这次运行里，退出应用不保留 */
  const queued = ref<Record<string, QueuedSend[]>>({})
  const draining = new Set<string>()
  /** 每条会话尚未发送的输入：文字与 @ 引用一起保存，切换会话时原样恢复 */
  const composerDrafts = ref<Record<string, ComposerDraft>>({})

  // 会话落新消息就把列表里这条的时间推到当下：列表的展示、分组与排序都以它为准
  onMessageLanded(markActive)
  // 一轮真正收尾后再发下一条：模型流结束不等于落库结束，必须等 chat store 的收尾
  onRunSettled((conversationId) => {
    void drain(conversationId).catch((error) => {
      console.error('Queued send failed:', error)
    })
  })
  // 轮次循环里的插入点：chat store 在每次模型往返边界问这里有没有新消息
  onQueuedPending((conversationId) => queuedOf(conversationId).length > 0)

  /** 当前会话：历史里的那条，或还没落库的新会话 */
  const current = computed<Conversation | null>(() => {
    const id = currentId.value
    if (!id) return null
    return drafts.value[id] ?? conversations.value.find((item) => item.id === id) ?? null
  })

  const visible = computed(() =>
    filterProjectId.value
      ? conversations.value.filter((item) => item.projectId === filterProjectId.value)
      : conversations.value
  )

  /** 列表按最后一条消息的时间倒序：时间相同（同一毫秒）时保持原有先后 */
  function sortConversations(): void {
    conversations.value = [...conversations.value].sort(
      (left, right) => (Date.parse(right.updatedAt) || 0) - (Date.parse(left.updatedAt) || 0)
    )
  }

  /**
   * 会话落了新消息：把这条的时间推到当下并重排——老会话发完消息当即挪到「今天」最上面，
   * 不必等下次启动重新读库。时刻由 chat store 给出，与写进库里的那个值一致。
   */
  function markActive(id: string, at: string): void {
    const conversation = conversations.value.find((item) => item.id === id)
    if (!conversation || !at) return
    if (at > conversation.updatedAt) conversation.updatedAt = at
    sortConversations()
  }

  async function fetchConversations(projectId = filterProjectId.value): Promise<Conversation[]> {
    loading.value = true
    try {
      const result: any = await ConversationDb.list(projectId)
      if (!ApiUtil.succeed(result)) {
        throw new Error(String(ApiUtil.message(result) || '会话列表读取失败'))
      }
      conversations.value = ApiUtil.data(result) as Conversation[]
      sortConversations()
    } finally {
      loading.value = false
    }
    return conversations.value
  }

  /** 新建会话：先在内存里起一条草稿，发出第一条消息时才落库 */
  async function create(project: Project, title = '新对话'): Promise<Conversation> {
    const now = new Date().toISOString()
    const conversation: Conversation = {
      id: crypto.randomUUID(),
      title,
      projectId: project.id,
      leafId: '',
      createdAt: now,
      updatedAt: now
    }
    drafts.value[conversation.id] = conversation
    return conversation
  }

  /**
   * 草稿落库：把这条新会话写进历史列表，已落库的会话什么都不做。
   * 时间用这一轮发送的时刻（at）：列表按最后一条消息的时间展示与排序，与库里记的是同一个值。
   * 落库失败就留着草稿，下一次发送再补——界面上这条会话照常能聊。
   */
  async function persist(params: SendParams, at: string): Promise<void> {
    const conversation = drafts.value[params.conversationId]
    if (!conversation) return
    const name = chatTitle(params) || conversation.title
    let stored = false
    try {
      const result: any = await ConversationDb.create({
        id: conversation.id,
        title: name,
        projectId: conversation.projectId,
        createdAt: conversation.createdAt,
        updatedAt: at
      })
      stored = ApiUtil.succeed(result)
    } catch {
      /* 落库失败：草稿保留，下一次发送再补 */
    }
    if (!stored) return
    delete drafts.value[params.conversationId]
    conversation.title = name
    conversation.updatedAt = at
    conversations.value = [conversation, ...conversations.value]
    sortConversations()
  }

  /**
   * 保存一段会话的输入草稿：切会话、开新会话或页面卸载前调用。
   * 空内容就删掉这一格，不留下一条空草稿。
   */
  function saveComposerDraft(id: string, text: string, mentions: ChatMention[]): void {
    if (!id) return
    const content = String(text ?? '')
    const rows = (mentions ?? []).slice()
    if (!content && !rows.length) {
      delete composerDrafts.value[id]
      return
    }
    composerDrafts.value[id] = { text: content, mentions: rows }
  }

  function composerDraftOf(id: string): ComposerDraft {
    const saved = composerDrafts.value[id]
    return saved
      ? { text: saved.text, mentions: saved.mentions.slice() }
      : { text: '', mentions: [] }
  }

  function clearComposerDraft(id: string): void {
    delete composerDrafts.value[id]
  }

  function conversationExists(id: string): boolean {
    return !!drafts.value[id] || conversations.value.some((item) => item.id === id)
  }

  function queuedOf(id: string): QueuedSend[] {
    return queued.value[id] ?? []
  }

  function enqueue(params: SendParams, front = false): void {
    const rows = queued.value[params.conversationId] ?? []
    const item: QueuedSend = { id: crypto.randomUUID(), params }
    if (front) rows.unshift(item)
    else rows.push(item)
    queued.value[params.conversationId] = rows
  }

  /** 移除一条待发送消息（界面上的 ×）；队列空掉时把这一格一并摘掉 */
  function dequeue(id: string, itemId: string): void {
    const rows = queued.value[id]
    if (!rows?.length) return
    const index = rows.findIndex((item) => item.id === itemId)
    if (index < 0) return
    rows.splice(index, 1)
    if (!rows.length) delete queued.value[id]
  }

  function clearQueued(id: string): void {
    delete queued.value[id]
  }

  /** 合并 @ 引用：同一文件 / 技能只保留一份，顺序按先排队、后当前这条 */
  function mergeMentions(rows: SendParams[]): ChatMention[] {
    const merged: ChatMention[] = []
    const seen = new Set<string>()
    for (const row of rows) {
      for (const mention of row.mentions ?? []) {
        const key = `${mention.type}:${mention.token ?? mention.path}`
        if (seen.has(key)) continue
        seen.add(key)
        merged.push(mention)
      }
    }
    return merged
  }

  /** 按提交顺序把多条消息合成一条：正文空行分段，@ 引用去重，其余口径取最后一条 */
  function mergeParams(items: SendParams[]): SendParams {
    const base = items[items.length - 1]
    const query = items
      .map((item) => String(item.query ?? '').trim())
      .filter(Boolean)
      .join('\n\n')
    return { ...base, query, mentions: mergeMentions(items) }
  }

  /**
   * 真正发起一轮：会话行先落库，再交给 chat store 把这一轮跑起来
   * （新会话的第一条消息就是它进历史的时机，落库时刻从 chat store 传回来）。
   */
  async function dispatch(params: SendParams): Promise<boolean> {
    return useChatStore().send(params, (at) => persist(params, at))
  }

  /**
   * 一轮收尾后把队列一次性发出去：所有待发送消息按提交顺序合成一条，只发起一轮。
   * 这样用户不必等前一条回复完才能轮到下一条；下一条消息仍然等这一轮真正收尾后再合并发送。
   */
  async function drain(id: string): Promise<void> {
    if (draining.has(id)) return
    const rows = queued.value[id]
    if (!rows?.length || !conversationExists(id)) return
    if (useChatStore().find(id)?.running) return
    draining.add(id)
    try {
      const merged = mergeParams(rows.map((item) => item.params))
      delete queued.value[id]
      // 项目配置可能在排队期间改过：按发送时的最新模型与提示词下发，而不是沿用排队那一刻的旧值
      const project = useProjectStore().find(merged.projectId)
      const params = project ? { ...merged, ...chatProfile(project) } : merged
      try {
        const started = await dispatch(params)
        if (!started) {
          // 没发出去（例如项目模型被删）：合并后的消息放回队列，别吞掉用户输入
          enqueue(params)
        }
      } catch (error) {
        // 还没真正跑起来就失败（例如本地库写入异常）：放回队列，别把用户输入静默丢掉
        enqueue(params)
        throw error
      }
    } finally {
      draining.delete(id)
    }
  }

  /**
   * 发送一轮：生成中提交一律进入该会话的队列，不打断、不弹窗；
   * 当前轮次边界或整轮收尾后，把队列合并成一条继续发送。
   */
  async function send(params: SendParams): Promise<'sent' | 'queued'> {
    const chat = useChatStore()
    if (chat.find(params.conversationId)?.running) {
      enqueue(params)
      return 'queued'
    }
    await dispatch(params)
    return 'sent'
  }

  async function select(id: string): Promise<void> {
    // 上一个没发消息的草稿随着换会话丢掉：它还没进历史，切走就等于放弃
    if (currentId.value !== id) delete drafts.value[currentId.value]
    currentId.value = id
    let conversation = current.value
    if (!conversation) {
      // 列表只取最近一截：检索到的更早会话不在内存里，按标识补读这一条
      const found = await ConversationDb.find(id)
      if (!found) return
      conversations.value = [found, ...conversations.value]
      sortConversations()
      conversation = found
    }
    opening.value = true
    try {
      // 模型与生成参数取会话所属项目：会话跟着项目走，和当前打开的是哪个会话无关
      const project = useProjectStore().find(conversation.projectId)
      // 分支尾一并交给 chat store：消息树按它决定当前展示哪条路径
      await useChatStore().open(conversation.id, chatProfile(project), conversation.leafId)
    } finally {
      opening.value = false
    }
  }

  async function remove(id: string): Promise<void> {
    await removeMany([id])
  }

  /** 批量删除会话：先统一停止运行态，再由数据库层在一个事务里删消息与会话行 */
  async function removeMany(ids: string[]): Promise<void> {
    const targets = [...new Set(ids.map((id) => String(id ?? '')).filter(Boolean))]
    if (!targets.length) return

    const chat = useChatStore()
    for (const id of targets) {
      // 已经排队但还没发的消息跟着会话一起丢掉
      clearQueued(id)
      clearComposerDraft(id)
      delete drafts.value[id]
      // 先作废运行中的这一轮并收掉模型流 / 本地命令，再删库：
      // 删除后 generation 仍会走收尾，靠 chat store 的作废标记避免把回复重新写回来
      chat.discard(id, 'delete')
    }

    const result = await ConversationDb.deleteMany(targets)
    if (!ApiUtil.succeed(result)) {
      throw new Error(String(ApiUtil.message(result) || '删除会话失败'))
    }
    if (targets.includes(currentId.value)) currentId.value = ''
    await fetchConversations()
    // 页面可能在 currentId 变化时把输入写回草稿表，列表刷新后再清一次
    for (const id of targets) clearComposerDraft(id)
  }

  /**
   * 重命名会话：只改标题，不碰最近活动时间——历史按时间分组排序，
   * 改个名字不该把会话挪到「今天」去。标题定下后各轮对话都不再动它。
   */
  async function rename(id: string, title: string): Promise<void> {
    const name = title.trim()
    if (!name) return
    // 还没落库的新会话：只改内存里这条草稿，第一条消息照旧只落库不改名
    const draft = drafts.value[id]
    if (draft) {
      draft.title = name
      return
    }
    const conversation = conversations.value.find((item) => item.id === id)
    if (!conversation) return
    const result: any = await ConversationDb.rename(id, name)
    // 写库失败就保持原样：界面上不出现改了又弹回去的半吊子状态
    if (!ApiUtil.succeed(result)) return
    conversation.title = name
  }

  /**
   * 清空消息：会话保留，本地历史删除（消息为空后重复打开也不会再回填）。
   * 生成中调用会先停止这一轮；返回此前是否正在生成，供页面提示。
   */
  async function clearMessages(id: string): Promise<boolean> {
    // 清空是「把这段会话恢复成起点」：排队中的消息也不该在清空后自动发出去
    clearQueued(id)
    const running = useChatStore().discard(id, 'clear')
    await ConversationDb.clearMessages(id)
    await ConversationDb.setLeaf(id, '')
    return running
  }

  /** 取消选中：回到「选择项目」面板去开会话，已在跑的会话不受影响（运行态在 chat store 上） */
  function deselect(): void {
    // 没发消息的新会话跟着丢掉：它还没进历史，回面板就等于放弃这条草稿
    delete drafts.value[currentId.value]
    currentId.value = ''
  }

  function reset(): void {
    conversations.value = []
    drafts.value = {}
    queued.value = {}
    composerDrafts.value = {}
    draining.clear()
    currentId.value = ''
    filterProjectId.value = ''
  }

  return {
    conversations,
    visible,
    current,
    currentId,
    loading,
    opening,
    filterProjectId,
    fetchConversations,
    create,
    send,
    queuedOf,
    dequeue,
    saveComposerDraft,
    composerDraftOf,
    clearComposerDraft,
    select,
    remove,
    removeMany,
    rename,
    clearMessages,
    deselect,
    reset
  }
})
