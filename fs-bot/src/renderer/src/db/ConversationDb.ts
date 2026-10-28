import Db from '@renderer/core/Db'
import ApiUtil from '@renderer/utils/ApiUtil'

const TABLE = 'conversations'
const MSG_TABLE = 'messages'

/** 左侧会话列表最多取最近多少条：条数再多也只加载这一截，避免整表读回内存 */
export const CONVERSATION_LIST_LIMIT = 50

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
  /** 最后一条消息的时间：列表按它展示与排序（提问与回复落库时各写一次，见 touch） */
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
  async list(
    projectId = '',
    limit = CONVERSATION_LIST_LIMIT
  ): Promise<{ code: number; message: string; data: Conversation[] }> {
    const where = projectId ? { project_id: projectId } : undefined
    const result: any = await Db.select(TABLE, where, 'updated_at DESC', limit)
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
    updatedAt?: string
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
      // 新会话的第一条消息随会话行一起落库，最后消息的时间就是这一轮发送的时刻
      updated_at: data.updatedAt || now
    }
    const result: any = await Db.insert(TABLE, row)
    return ApiUtil.result(ApiUtil.code(result) ?? 0, ApiUtil.message(result), format(row)) as any
  },

  /** 重命名：只改标题——最近活动时间跟着消息走，改名不该把会话在历史里换位置 */
  async rename(id: string, title: string) {
    return Db.update(TABLE, { id }, { title })
  },

  /** 记录最后一条消息的时间：提问与回复落库时各写一次，列表按它展示与排序 */
  async touch(id: string, at = new Date().toISOString()) {
    return Db.update(TABLE, { id }, { updated_at: at })
  },

  /**
   * 记录当前分支尾：切换分支、新一轮回复落库后各写一次。
   * 不碰最后消息的时间——分支指针跟着消息走，切分支 / 补写旧历史的叶子
   * 不该把一段很久没说话的会话挪到「今天」。
   */
  async setLeaf(id: string, leafId: string) {
    return Db.update(TABLE, { id }, { leaf_id: leafId })
  },

  async delete(id: string) {
    return this.deleteMany([id])
  },

  /** 批量删除会话：消息与会话行放在同一个事务里，要么全删掉，要么原样保留 */
  async deleteMany(ids: string[]) {
    const unique = [...new Set(ids.map((id) => String(id ?? '')).filter(Boolean))]
    if (!unique.length) return ApiUtil.result(0, 'Succeed!', { changes: 0 })
    return Db.removeBatch([
      { table: MSG_TABLE, in: { conversation_id: unique } },
      { table: TABLE, in: { id: unique } }
    ])
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
   * 关键词先按字面量整段匹配（`%` / `_` / `\` 按字面量转义，换行也是字面量）；
   * 一条都没命中、而关键词又能按空白切成多段时，再按「各段按原顺序出现」查一次
   * （`segments` 口径）：模型常把多行片段整段粘过来，库里那段的换行与缩进未必与它一致，
   * 逐段匹配才兜得住——顺序仍然要求保持，不是把关键词拆成几个词各查各的。
   *
   * 消息表没有项目列，所以先按项目取会话清单，再用 conversation_id IN (...) 一次查完；
   * 会话数按上限截断（跨项目检索时兜住 IN 列表的长度）。
   */
  async search(
    keyword: string,
    projectId = '',
    limit = 10
  ): Promise<{ code: number; message: string; data: HistoryHit[] }> {
    const text = String(keyword ?? '')
      .replace(/\r\n?/g, '\n')
      .trim()
    const size = Math.max(1, Math.min(30, Math.floor(Number(limit) || 10)))
    if (!text) return ApiUtil.result(0, 'Succeed!', [] as HistoryHit[]) as any

    const listed: any = await Db.select(
      TABLE,
      projectId ? { project_id: projectId } : undefined,
      'updated_at DESC'
    )
    const conversations = ApiUtil.succeed(listed) ? (ApiUtil.data(listed) as any[]).map(format) : []
    const byId = new Map(conversations.map((item) => [item.id, item]))

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

    /** 按给定口径查一遍：标题一路、消息正文一路，标题命中排在前面 */
    const collect = async (mode: 'literal' | 'segments'): Promise<HistoryHit[]> => {
      const hits: HistoryHit[] = []
      const titles: any = await Db.search(TABLE, ['title'], text, {
        ...(projectId ? { where: { project_id: projectId } } : {}),
        mode,
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

      const ids = conversations
        .slice(0, MAX_SEARCH_CONVERSATIONS)
        .map((item) => item.id)
        .filter(Boolean)
      if (ids.length && hits.length < size) {
        const body: any = await Db.search(MSG_TABLE, ['content'], text, {
          in: { conversation_id: ids },
          mode,
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
      return hits
    }

    let hits = await collect('literal')
    // 字面量一条都没命中、关键词又能切成多段时，再按「各段按原顺序出现」查一次
    // （整段粘来的多行片段、换行与缩进对不上的情况靠这一趟兜住）
    if (!hits.length && text.split(/\s+/).filter(Boolean).length > 1) {
      hits = await collect('segments')
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
