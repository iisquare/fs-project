export default {
  async initUser(serial: string, userId: string, dbSecret: string) {
    return window.ipc.db.initUser(serial, userId, dbSecret)
  },

  async select(table: string, where?: Record<string, unknown>, orderBy?: string, limit?: number) {
    return window.ipc.db.select(table, where, orderBy, limit)
  },

  async insert(table: string, data: Record<string, unknown>) {
    return window.ipc.db.insert(table, data)
  },

  async upsert(table: string, data: Record<string, unknown>) {
    return window.ipc.db.upsert(table, data)
  },

  /** 一次事务保存消息与分支尾：对话检查点不会出现“消息写了、分支没切过去”的半状态 */
  async saveMessages(
    conversationId: string,
    messages: Array<Record<string, unknown>>,
    leafId: string,
    updatedAt?: string
  ) {
    return window.ipc.db.saveMessages(conversationId, messages, leafId, updatedAt)
  },

  async update(table: string, where: Record<string, unknown>, data: Record<string, unknown>) {
    return window.ipc.db.update(table, where, data)
  },

  async remove(table: string, where: Record<string, unknown>) {
    return window.ipc.db.delete(table, where)
  },

  async removeBatch(
    deletes: Array<{
      table: string
      where?: Record<string, unknown>
      in?: Record<string, unknown[]>
    }>
  ) {
    return window.ipc.db.deleteBatch(deletes)
  },

  /**
   * 关键词检索：columns 逐列按 LIKE 匹配，另有等值（where）与集合（in）条件。
   * mode 给 `segments` 时按空白切成多段、各段按原顺序出现即算命中（多行片段的兜底口径）。
   */
  async search(
    table: string,
    columns: string[],
    keyword: string,
    options?: {
      where?: Record<string, unknown>
      in?: Record<string, unknown[]>
      orderBy?: string
      limit?: number
      mode?: 'literal' | 'segments'
    }
  ) {
    return window.ipc.db.search(table, columns, keyword, options)
  }
}
