import Database from 'better-sqlite3-multiple-ciphers'
import { join, dirname } from 'path'
import { mkdirSync } from 'fs'
import { getLatestUserSchema } from './schema'
import { ensureUserSchema } from './migrations'
import { deriveHmacKey, computeTablesHash, verifyHash } from './integrity'
import { deriveKey } from './encryption'
import { is } from '@electron-toolkit/utils'
import type { DbDeleteBatchItem } from './types'

interface DbCtx {
  db: Database.Database
  hmacKey: Buffer
  encKey: Buffer
  dbPath: string
}

export class DatabaseManager {
  private static instance: DatabaseManager

  /** 数据变更后延迟重算完整性哈希的时间，避免每次写入都做全表哈希 */
  private static readonly HASH_FLUSH_DELAY = 1500

  private user: DbCtx | null = null
  private hashTimer: NodeJS.Timeout | null = null
  private hashDirty = false

  static getInstance(): DatabaseManager {
    if (!DatabaseManager.instance) {
      DatabaseManager.instance = new DatabaseManager()
    }
    return DatabaseManager.instance
  }

  get isReady(): boolean {
    return this.user !== null
  }

  /** 当前已打开用户库的 schema 版本；用户库尚未打开时为 null */
  get userSchemaVersion(): number | null {
    if (!this.user) return null
    const row = this.user.db
      .prepare('SELECT version FROM schema_version ORDER BY version DESC LIMIT 1')
      .get() as { version: number } | undefined
    return row?.version ?? 0
  }

  // ── Init user DB (per-user, called after login) ──

  /** runtimeDir 是运行目录（见 main/index.ts）：用户库放在它下面的 userdata/<userId>/ 里 */
  initUser(serial: string, userId: string, dbSecret: string, runtimeDir: string): void {
    this.closeUser()

    const userDir = join(runtimeDir, 'userdata', String(userId))
    const dbPath = join(userDir, 'fs-bot.db')

    this.user = this.openDb(dbPath, serial, dbSecret)
    console.log('[DB] User database ready for', serial)
  }

  private openDb(dbPath: string, ident: string, dbSecret: string): DbCtx {
    const hmacKey = deriveHmacKey(ident, dbSecret)
    const encKey = deriveKey(ident, dbSecret)

    mkdirSync(dirname(dbPath), { recursive: true })

    const db = new Database(dbPath)
    db.pragma("cipher='sqlcipher'")
    db.pragma('legacy=4')
    db.pragma(`key='${encKey.toString('hex')}'`)
    db.pragma('journal_mode = WAL')

    if (is.dev) {
      console.log(`[DB] Database: ${dbPath}`)
      console.log(`[DB] Cipher: sqlcipher | Legacy: 4 | Passphrase (hex): ${encKey.toString('hex')}`)
    }

    ensureUserSchema(db)

    const ctx: DbCtx = { db, hmacKey, encKey, dbPath }

    const hash = computeTablesHash(hmacKey, db, getLatestUserSchema())
    const integRow = db.prepare('SELECT hash FROM _integrity WHERE id = 1').get() as
      | { hash: string }
      | undefined
    if (integRow) {
      const ok = verifyHash(integRow.hash, hash)
      if (ok) {
        console.log('[DB] Integrity check passed')
      } else {
        console.warn('[DB] Integrity check FAILED - hash mismatch')
      }
    }
    this.updateHash(db, hash)

    return ctx
  }

  // ── Resolve DB context ──

  private ctx(): DbCtx {
    if (!this.user) throw new Error('User database not initialized')
    return this.user
  }

  // ── Generic CRUD ──

  select(
    table: string,
    where?: Record<string, unknown>,
    orderBy?: string,
    limit?: number
  ): { code: number; message: string; data: unknown[] } {
    const { db } = this.ctx()
    const { sql, params } = this.buildSelect(table, where, orderBy, limit)
    const rows = db.prepare(sql).all(...params) as Record<string, unknown>[]
    return { code: 0, message: 'Succeed!', data: rows }
  }

  insert(
    table: string,
    data: Record<string, unknown>
  ): { code: number; message: string; data: unknown } {
    const { db } = this.ctx()
    const row = { ...data }
    const columns = Object.keys(row)
    const placeholders = columns.map(() => '?').join(', ')
    const values = columns.map((c) => row[c])

    db.prepare(`INSERT INTO ${table} (${columns.join(', ')}) VALUES (${placeholders})`).run(
      ...values
    )
    this.afterWrite()
    return { code: 0, message: 'Succeed!', data: { id: row['id'] ?? null } }
  }

  update(
    table: string,
    where: Record<string, unknown>,
    data: Record<string, unknown>
  ): { code: number; message: string; data: null } {
    const { db } = this.ctx()
    const { sql, params } = this.buildUpdate(table, where, data)
    db.prepare(sql).run(...params)
    this.afterWrite()
    return { code: 0, message: 'Succeed!', data: null }
  }

  upsert(
    table: string,
    data: Record<string, unknown>
  ): { code: number; message: string; data: unknown } {
    const { db } = this.ctx()
    const row = { ...data }
    const pkCols = ['id']
    const columns = Object.keys(row)
    const placeholders = columns.map(() => '?').join(', ')
    const values = columns.map((c) => row[c])

    const conflictTarget = pkCols.join(', ')
    const updateCols = columns.filter((c) => !pkCols.includes(c))
    const setClause =
      updateCols.length > 0
        ? ` DO UPDATE SET ${updateCols.map((c) => `${c} = excluded.${c}`).join(', ')}`
        : ''

    db.prepare(
      `INSERT INTO ${table} (${columns.join(', ')}) VALUES (${placeholders}) ON CONFLICT(${conflictTarget})${setClause}`
    ).run(...values)
    this.afterWrite()
    return { code: 0, message: 'Succeed!', data: { id: row['id'] ?? null } }
  }

  remove(
    table: string,
    where: Record<string, unknown>
  ): { code: number; message: string; data: null } {
    const { db } = this.ctx()
    const { sql, params } = this.buildDelete(table, where)
    db.prepare(sql).run(...params)
    this.afterWrite()
    return { code: 0, message: 'Succeed!', data: null }
  }

  /**
   * 在一个事务里执行多条删除（例如批量删除会话时连同消息一起清掉）。
   * 任意一条失败都会整体回滚，不会留下删了一半的会话。
   */
  removeBatch(deletes: DbDeleteBatchItem[]): {
    code: number
    message: string
    data: { changes: number }
  } {
    const { db } = this.ctx()
    const run = db.transaction((items: DbDeleteBatchItem[]) => {
      let changes = 0
      for (const item of items) {
        const { sql, params } = this.buildDelete(item.table, item.where, item.in)
        changes += db.prepare(sql).run(...params).changes
      }
      return changes
    })
    const changes = run(deletes)
    this.afterWrite()
    return { code: 0, message: 'Succeed!', data: { changes } }
  }

  /**
   * 关键词检索：columns 里的每一列按 LIKE 匹配（关键词参数化，
   * `%` / `_` / `\` 按字面量处理），可另加等值条件（where）、集合条件（in）与排序、条数上限。
   *
   * `mode` 是关键词的匹配口径（见 likePattern）：默认 `literal` 整段按字面量匹配（换行也算字面量），
   * `segments` 则按空白切成多段、各段按原顺序出现即算命中——从对话里整段粘来的多行片段
   * 与库里的缩进 / 换行（\n 与 \r\n）未必一样，字面量一条都命中不了时用它兜底。
   *
   * 供「历史对话检索」这类「按内容找行」的查询使用：表的行数可能有几千条，
   * 让主进程用一条 SQL 查完，比把每个会话的消息整段拉回渲染进程再过滤干净得多。
   */
  search(
    table: string,
    columns: string[],
    keyword: string,
    options: {
      where?: Record<string, unknown>
      in?: Record<string, unknown[]>
      orderBy?: string
      limit?: number
      mode?: 'literal' | 'segments'
    } = {}
  ): { code: number; message: string; data: unknown[] } {
    const { db } = this.ctx()
    const { sql, params } = this.buildSearch(table, columns, keyword, options)
    const rows = db.prepare(sql).all(...params) as Record<string, unknown>[]
    return { code: 0, message: 'Succeed!', data: rows }
  }

  /**
   * 写入后只打脏标记：连续写入（例如一次会话里连发多条消息）只在空闲后合并算一次哈希。
   * 进程退出前会通过 close()/flushHash() 落盘；若被强杀，下次启动会打印一次
   * “Integrity check FAILED” 警告并自动重新计算，不影响数据本身。
   */
  private afterWrite(): void {
    this.hashDirty = true
    if (this.hashTimer) clearTimeout(this.hashTimer)
    this.hashTimer = setTimeout(() => {
      this.hashTimer = null
      this.flushHash()
    }, DatabaseManager.HASH_FLUSH_DELAY)
  }

  /** 立即落盘待更新的完整性哈希（切换用户、退出应用前调用） */
  flushHash(): void {
    if (this.hashTimer) {
      clearTimeout(this.hashTimer)
      this.hashTimer = null
    }
    if (!this.hashDirty || !this.user) {
      this.hashDirty = false
      return
    }
    const ctx = this.user
    const hash = computeTablesHash(ctx.hmacKey, ctx.db, getLatestUserSchema())
    this.updateHash(ctx.db, hash)
    this.hashDirty = false
  }

  private updateHash(db: Database.Database, hash: string): void {
    db.prepare(
      `INSERT INTO _integrity (id, hash, updated_at)
       VALUES (1, ?, datetime('now'))
       ON CONFLICT(id) DO UPDATE SET hash = excluded.hash, updated_at = excluded.updated_at`
    ).run(hash)
  }

  // ── Close ──

  closeUser(): void {
    if (!this.user) return
    this.flushHash()
    this.user.db.close()
    this.user = null
  }

  close(): void {
    this.closeUser()
  }

  // ── Internal helpers ──

  private buildSelect(
    table: string,
    where?: Record<string, unknown>,
    orderBy?: string,
    limit?: number
  ): { sql: string; params: unknown[] } {
    let sql = `SELECT * FROM ${table}`
    const params: unknown[] = []
    if (where && Object.keys(where).length > 0) {
      const clauses = Object.keys(where).map((col) => `${col} = ?`)
      sql += ` WHERE ${clauses.join(' AND ')}`
      params.push(...Object.values(where))
    }
    if (orderBy) {
      sql += ` ORDER BY ${orderBy}`
    }
    const size = Math.floor(Number(limit))
    if (Number.isFinite(size) && size > 0) {
      sql += ' LIMIT ?'
      params.push(size)
    }
    return { sql, params }
  }

  private buildUpdate(
    table: string,
    where: Record<string, unknown>,
    data: Record<string, unknown>
  ): { sql: string; params: unknown[] } {
    const setClauses: string[] = []
    const setParams: unknown[] = []
    for (const [col, val] of Object.entries(data)) {
      setClauses.push(`${col} = ?`)
      setParams.push(val)
    }
    const whereClauses: string[] = []
    const whereParams: unknown[] = []
    for (const [col, val] of Object.entries(where)) {
      whereClauses.push(`${col} = ?`)
      whereParams.push(val)
    }
    return {
      sql: `UPDATE ${table} SET ${setClauses.join(', ')} WHERE ${whereClauses.join(' AND ')}`,
      params: [...setParams, ...whereParams]
    }
  }

  private buildDelete(
    table: string,
    where: Record<string, unknown> = {},
    inFilter: Record<string, unknown[]> = {}
  ): { sql: string; params: unknown[] } {
    const whereClauses: string[] = []
    const whereParams: unknown[] = []
    for (const [col, val] of Object.entries(where)) {
      whereClauses.push(`${col} = ?`)
      whereParams.push(val)
    }
    for (const [col, values] of Object.entries(inFilter)) {
      const list = Array.isArray(values) ? values : []
      if (!list.length) {
        whereClauses.push('0')
        continue
      }
      whereClauses.push(`${col} IN (${list.map(() => '?').join(', ')})`)
      whereParams.push(...list)
    }
    if (!whereClauses.length) whereClauses.push('0')
    return {
      sql: `DELETE FROM ${table} WHERE ${whereClauses.join(' AND ')}`,
      params: whereParams
    }
  }

  /**
   * 组装关键词检索语句：列名与表名沿用既有口径直接拼接（渲染进程本来就传表名），
   * 关键词与条件值一律走占位符（关键词的口径见 likePattern）。集合条件为空时直接判为不命中（WHERE 0），
   * 免得拼出 `IN ()` 这种非法语句。
   */
  private buildSearch(
    table: string,
    columns: string[],
    keyword: string,
    options: {
      where?: Record<string, unknown>
      in?: Record<string, unknown[]>
      orderBy?: string
      limit?: number
      mode?: 'literal' | 'segments'
    }
  ): { sql: string; params: unknown[] } {
    const text = String(keyword ?? '').trim()
    if (!text) throw new Error('检索关键词为空')
    const cols = (columns ?? []).map((column) => String(column ?? '').trim()).filter(Boolean)
    if (!cols.length) throw new Error('未指定检索列')

    const like = likePattern(text, options.mode)
    const clauses = cols.map((column) => `${column} LIKE ? ESCAPE '\\'`)
    const params: unknown[] = cols.map(() => like)

    for (const [col, val] of Object.entries(options.where ?? {})) {
      clauses.push(`${col} = ?`)
      params.push(val)
    }
    for (const [col, values] of Object.entries(options.in ?? {})) {
      const list = Array.isArray(values) ? values : []
      if (!list.length) return { sql: `SELECT * FROM ${table} WHERE 0`, params: [] }
      clauses.push(`${col} IN (${list.map(() => '?').join(', ')})`)
      params.push(...list)
    }

    let sql = `SELECT * FROM ${table} WHERE ${clauses.join(' AND ')}`
    if (options.orderBy) sql += ` ORDER BY ${options.orderBy}`
    const limit = Math.max(0, Math.floor(Number(options.limit ?? 0)) || 0)
    if (limit > 0) sql += ` LIMIT ${limit}`
    return { sql, params }
  }
}

/**
 * 关键词按空白切段：空格、制表、换行（\n 与 \r\n 都算）一律当分隔，
 * 空段丢掉。粘过来的多行片段靠它还原成「按原顺序出现的几段」
 * （库里那段的缩进与换行未必与粘来的那份一致，逐段匹配才兜得住）。
 */
export function keywordSegments(text: string): string[] {
  return String(text ?? '')
    .split(/\s+/)
    .filter(Boolean)
}

/**
 * 关键词 → LIKE 模式：
 * - `literal`：整段按字面量（`%` / `_` / `\` 转义，换行也是字面量）；
 * - `segments`：切成多段后按原顺序串起来（`%段1%段2%`），段与段之间允许任意字符——
 *   多段之间隔着换行、缩进或别的文字都算命中；切不出两段时退回字面量口径。
 */
function likePattern(text: string, mode = 'literal'): string {
  if ('segments' === mode) {
    const parts = keywordSegments(text).map(escapeLike)
    if (parts.length > 1) return `%${parts.join('%')}%`
  }
  return `%${escapeLike(String(text ?? '').trim())}%`
}

/** LIKE 的通配符转义：关键词按字面量匹配，`\` 自身也要先转义 */
function escapeLike(text: string): string {
  return text.replace(/[\\%_]/g, (char) => `\\${char}`)
}
