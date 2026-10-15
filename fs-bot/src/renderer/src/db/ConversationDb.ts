import Db from '@renderer/core/Db'
import ApiUtil from '@renderer/utils/ApiUtil'

const TABLE = 'conversations'
const MSG_TABLE = 'messages'

/**
 * 本地会话表：会话与消息都只存在本地，按项目归属；断网或未登录时也能展示历史。
 */

export interface Conversation {
  id: string
  title: string
  projectId: string
  /** 当前分支尾：消息按 parentId 组成消息树，页面只渲染它回溯到根的那条路径 */
  leafId: string
  createdAt: string
  updatedAt: string
}

export interface CachedMessage {
  id: string
  conversationId: string
  /** 父消息：'' 表示分支起点 */
  parentId: string
  role: string
  content: string
  reasoning: string
  /** 附加信息：助手回复的工具调用链、用户消息的 @ 引用，按 JSON 文本存 */
  reference: string
  status: string
  createdAt: string
}

/**
 * 历史检索的一条命中：会话标题命中，或会话内某条消息命中。
 * 标题命中没有消息字段（messageId / role / createdAt 都是空串），content 给标题原文。
 */
export interface HistoryHit {
  conversationId: string
  conversationTitle: string
  projectId: string
  /** 会话最后更新时间 */
  updatedAt: string
  target: 'title' | 'content'
  messageId: string
  role: string
  createdAt: string
  /**
   * 这条消息在会话里的序号（1 起，按时间正序）与会话的消息总数，标题命中都是 0。
   * 读历史时 offset 给 messageIndex - 1 就能从这条附近接着读，不必从头翻。
   */
  messageIndex: number
  messageTotal: number
  /** 命中的完整内容（展示用的片段由调用方截取） */
  content: string
}

/** 一次检索最多翻多少个会话的消息：跨项目检索时兜住 IN 列表的长度 */
const MAX_SEARCH_CONVERSATIONS = 1000

function format(row: Record<string, any>): Conversation {
  return {
    id: String(row['id'] ?? ''),
    title: String(row['title'] ?? ''),
    projectId: String(row['project_id'] ?? ''),
    leafId: String(row['leaf_id'] ?? ''),
    createdAt: String(row['created_at'] ?? ''),
    updatedAt: String(row['updated_at'] ?? '')
  }
}

function formatMessage(row: Record<string, any>): CachedMessage {
  return {
    id: String(row['id'] ?? ''),
    conversationId: String(row['conversation_id'] ?? ''),
    parentId: String(row['parent_id'] ?? ''),
    role: String(row['role'] ?? ''),
    content: String(row['content'] ?? ''),
    reasoning: String(row['reasoning'] ?? ''),
    reference: String(row['reference'] ?? ''),
    status: String(row['status'] ?? ''),
    createdAt: String(row['created_at'] ?? '')
  }
}

export default {
  async list(projectId = ''): Promise<{ code: number; message: string; data: Conversation[] }> {
    const where = projectId ? { project_id: projectId } : undefined
    const result: any = await Db.select(TABLE, where, 'updated_at DESC')
    return ApiUtil.result(
      ApiUtil.code(result) ?? 0,
      ApiUtil.message(result),
      (ApiUtil.data(result) as any[]).map(format)
    )
  },

  /**
   * 新建会话行：id 与 createdAt 由调用方给——新会话先以草稿活在内存里，
   * 发出第一条消息时才落库，id 提前定下这一轮的消息才接得上去。
   */
  async create(data: {
    id?: string
    title?: string
    projectId?: string
    createdAt?: string
  }): Promise<{
    code: number
    message: string
    data: Conversation
  }> {
    const now = new Date().toISOString()
    const id = data.id || crypto.randomUUID()
    const row = {
      id,
      title: data.title || '新对话',
      project_id: data.projectId || '',
      created_at: data.createdAt || now,
      updated_at: now
    }
    const result: any = await Db.insert(TABLE, row)
    return ApiUtil.result(ApiUtil.code(result) ?? 0, ApiUtil.message(result), format(row)) as any
  },

  /** 重命名：只改标题——最近活动时间跟着消息走，改名不该把会话在历史里换位置 */
  async rename(id: string, title: string) {
    return Db.update(TABLE, { id }, { title })
  },

  async touch(id: string) {
    return Db.update(TABLE, { id }, { updated_at: new Date().toISOString() })
  },

  /** 记录当前分支尾：切换分支、新一轮回复落库后各写一次 */
  async setLeaf(id: string, leafId: string) {
    return Db.update(TABLE, { id }, { leaf_id: leafId, updated_at: new Date().toISOString() })
  },

  async delete(id: string) {
    await Db.remove(MSG_TABLE, { conversation_id: id })
    return Db.remove(TABLE, { id })
  },

  /** 清空消息：会话保留，历史只删本地那几条消息记录 */
  async clearMessages(id: string) {
    return Db.remove(MSG_TABLE, { conversation_id: id })
  },

  /** 按标识取一条会话：读历史会话时用来回显标题与归属项目 */
  async find(id: string): Promise<Conversation | null> {
    const result: any = await Db.select(TABLE, { id })
    if (!ApiUtil.succeed(result)) return null
    const rows = ApiUtil.data(result) as any[]
    return rows.length ? format(rows[0]) : null
  },

  /**
   * 历史检索（记忆）：会话标题与消息正文两路都查，标题命中排在前面。
   * projectId 给空串表示不限项目（模型侧默认只搜当前对话所属的项目）。
   *
   * 消息表没有项目列，所以先按项目取会话清单，再用 conversation_id IN (...) 一次查完；
   * 会话数按上限截断（跨项目检索时兜住 IN 列表的长度）。
   */
  async search(
    keyword: string,
    projectId = '',
    limit = 10
  ): Promise<{ code: number; message: string; data: HistoryHit[] }> {
    const text = String(keyword ?? '').trim()
    const size = Math.max(1, Math.min(30, Math.floor(Number(limit) || 10)))
    if (!text) return ApiUtil.result(0, 'Succeed!', [] as HistoryHit[]) as any

    const listed: any = await Db.select(
      TABLE,
      projectId ? { project_id: projectId } : undefined,
      'updated_at DESC'
    )
    const conversations = ApiUtil.succeed(listed) ? (ApiUtil.data(listed) as any[]).map(format) : []
    const byId = new Map(conversations.map((item) => [item.id, item]))

    const hits: HistoryHit[] = []
    const titles: any = await Db.search(TABLE, ['title'], text, {
      ...(projectId ? { where: { project_id: projectId } } : {}),
      orderBy: 'updated_at DESC',
      limit: size
    })
    if (ApiUtil.succeed(titles)) {
      for (const row of ApiUtil.data(titles) as any[]) {
        const item = format(row)
        hits.push({
          conversationId: item.id,
          conversationTitle: item.title,
          projectId: item.projectId,
          updatedAt: item.updatedAt,
          target: 'title',
          messageId: '',
          role: '',
          createdAt: '',
          messageIndex: 0,
          messageTotal: 0,
          content: item.title
        })
      }
    }

    /** 命中消息的序号：按会话取一次消息清单（同一个会话只取一次），在内存里数出位置 */
    const cache = new Map<string, CachedMessage[]>()
    const locate = async (
      conversationId: string,
      messageId: string
    ): Promise<{ messageIndex: number; messageTotal: number }> => {
      let rows = cache.get(conversationId)
      if (!rows) {
        const listed: any = await Db.select(
          MSG_TABLE,
          { conversation_id: conversationId },
          'created_at ASC'
        )
        rows = ApiUtil.succeed(listed) ? (ApiUtil.data(listed) as any[]).map(formatMessage) : []
        cache.set(conversationId, rows)
      }
      const index = rows.findIndex((item) => item.id === messageId)
      return { messageIndex: index < 0 ? 0 : index + 1, messageTotal: rows.length }
    }

    const ids = conversations
      .slice(0, MAX_SEARCH_CONVERSATIONS)
      .map((item) => item.id)
      .filter(Boolean)
    if (ids.length && hits.length < size) {
      const body: any = await Db.search(MSG_TABLE, ['content'], text, {
        in: { conversation_id: ids },
        orderBy: 'created_at DESC',
        limit: size
      })
      if (ApiUtil.succeed(body)) {
        for (const row of ApiUtil.data(body) as any[]) {
          const message = formatMessage(row)
          const conversation = byId.get(message.conversationId)
          const position = await locate(message.conversationId, message.id)
          hits.push({
            conversationId: message.conversationId,
            conversationTitle: conversation?.title ?? '',
            projectId: conversation?.projectId ?? projectId,
            updatedAt: conversation?.updatedAt ?? '',
            target: 'content',
            messageId: message.id,
            role: message.role,
            createdAt: message.createdAt,
            messageIndex: position.messageIndex,
            messageTotal: position.messageTotal,
            content: message.content
          })
        }
      }
    }

    return ApiUtil.result(0, 'Succeed!', hits.slice(0, size)) as any
  },
  async messages(conversationId: string): Promise<CachedMessage[]> {
    const result: any = await Db.select(
      MSG_TABLE,
      { conversation_id: conversationId },
      'created_at ASC'
    )
    if (!ApiUtil.succeed(result)) return []
    return (ApiUtil.data(result) as any[]).map(formatMessage)
  },

  async appendMessage(conversationId: string, message: Partial<CachedMessage>) {
    return Db.insert(MSG_TABLE, {
      id: message.id || crypto.randomUUID(),
      conversation_id: conversationId,
      parent_id: message.parentId ?? '',
      role: message.role || 'assistant',
      content: message.content ?? '',
      reasoning: message.reasoning ?? '',
      reference: message.reference ?? '',
      status: message.status ?? '',
      created_at: message.createdAt || new Date().toISOString()
    })
  }
}
