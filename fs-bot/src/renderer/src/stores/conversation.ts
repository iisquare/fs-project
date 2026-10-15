import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import ApiUtil from '@renderer/utils/ApiUtil'
import ConversationDb from '@renderer/db/ConversationDb'
import type { Conversation } from '@renderer/db/ConversationDb'
import { chatProfile, chatTitle, useChatStore, type SendParams } from './chat'
import { useProjectStore } from './project'
import type { Project } from '@renderer/db/ProjectDb'

/**
 * 会话列表：会话与消息都只存在本地，按项目归属；换机器 / 重装后历史不跟随。
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
  const filterProjectId = ref('')

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

  async function fetchConversations(projectId = filterProjectId.value): Promise<Conversation[]> {
    loading.value = true
    try {
      const result: any = await ConversationDb.list(projectId)
      conversations.value = ApiUtil.succeed(result) ? (ApiUtil.data(result) as Conversation[]) : []
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
   * 落库失败就留着草稿，下一次发送再补——界面上这条会话照常能聊。
   */
  async function persist(id: string, title = ''): Promise<void> {
    const conversation = drafts.value[id]
    if (!conversation) return
    const name = title || conversation.title
    let stored = false
    try {
      const result: any = await ConversationDb.create({
        id: conversation.id,
        title: name,
        projectId: conversation.projectId,
        createdAt: conversation.createdAt
      })
      stored = ApiUtil.succeed(result)
    } catch {
      /* 落库失败：草稿保留，下一次发送再补 */
    }
    if (!stored) return
    delete drafts.value[id]
    conversation.title = name
    conversations.value = [conversation, ...conversations.value]
  }

  /**
   * 发送一轮：会话行先落库，再交给 chat store 把这一轮跑起来
   * （新会话的第一条消息就是它进历史的时机）。
   */
  async function send(params: SendParams): Promise<void> {
    await useChatStore().send(params, () => persist(params.conversationId, chatTitle(params)))
  }

  async function select(id: string): Promise<void> {
    // 上一个没发消息的草稿随着换会话丢掉：它还没进历史，切走就等于放弃
    if (currentId.value !== id) delete drafts.value[currentId.value]
    currentId.value = id
    const conversation = current.value
    if (!conversation) return
    // 模型与生成参数取会话所属项目：会话跟着项目走，和当前打开的是哪个会话无关
    const project = useProjectStore().find(conversation.projectId)
    // 分支尾一并交给 chat store：消息树按它决定当前展示哪条路径
    await useChatStore().open(conversation.id, chatProfile(project), conversation.leafId)
  }

  async function remove(id: string): Promise<void> {
    await ConversationDb.delete(id)
    if (currentId.value === id) currentId.value = ''
    await fetchConversations()
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

  /** 清空消息：会话保留，本地历史删除（消息为空后重复打开也不会再回填） */
  async function clearMessages(id: string): Promise<void> {
    await ConversationDb.clearMessages(id)
    const session = useChatStore().find(id)
    if (session) session.messages = []
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
    currentId.value = ''
    filterProjectId.value = ''
  }

  return {
    conversations,
    visible,
    current,
    currentId,
    loading,
    filterProjectId,
    fetchConversations,
    create,
    send,
    select,
    remove,
    rename,
    clearMessages,
    deselect,
    reset
  }
})
