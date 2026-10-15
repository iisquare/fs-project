import Database from 'better-sqlite3-multiple-ciphers'
import { join, dirname } from 'path'
import { mkdirSync } from 'fs'
import { getLatestUserSchema } from './schema'
import { ensureUserSchema } from './migrations'
import { deriveHmacKey, computeTablesHash, verifyHash } from './integrity'
import { deriveKey } from './encryption'
import { is } from '@electron-toolkit/utils'

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

  // ── Init user DB (per-user, called after login) ──

  initUser(serial: string, userId: string, dbSecret: string, installPath: string): void {
    this.closeUser()

    const userDir = join(installPath, 'userdata', String(userId))
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
    orderBy?: string
  ): { code: number; message: string; data: unknown[] } {
    const { db } = this.ctx()
    const { sql, params } = this.buildSelect(table, where, orderBy)
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

    db.prepare(
      `INSERT INTO ${table} (${columns.join(', ')}) VALUES (${placeholders})`
    ).run(...values)
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
   * 关键词检索：columns 里的每一列按 LIKE 匹配（关键词参数化，
   * `%` 与 `_` 按字面量处理），可另加等值条件（where）、集合条件（in）与排序、条数上限。
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
    orderBy?: string
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
    where: Record<string, unknown>
  ): { sql: string; params: unknown[] } {
    const whereClauses: string[] = []
    const whereParams: unknown[] = []
    for (const [col, val] of Object.entries(where)) {
      whereClauses.push(`${col} = ?`)
      whereParams.push(val)
    }
    return {
      sql: `DELETE FROM ${table} WHERE ${whereClauses.join(' AND ')}`,
      params: whereParams
    }
  }

  /**
   * 组装关键词检索语句：列名与表名沿用既有口径直接拼接（渲染进程本来就传表名），
   * 关键词与条件值一律走占位符。集合条件为空时直接判为不命中（WHERE 0），
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
    }
  ): { sql: string; params: unknown[] } {
    const text = String(keyword ?? '').trim()
    if (!text) throw new Error('检索关键词为空')
    const cols = (columns ?? []).map((column) => String(column ?? '').trim()).filter(Boolean)
    if (!cols.length) throw new Error('未指定检索列')

    const like = `%${escapeLike(text)}%`
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

/** LIKE 的通配符转义：关键词按字面量匹配，`\` 自身也要先转义 */
function escapeLike(text: string): string {
  return text.replace(/[\\%_]/g, (char) => `\\${char}`)
}
