import { createHmac, createHash, timingSafeEqual } from 'crypto'
import type Database from 'better-sqlite3-multiple-ciphers'

export function deriveHmacKey(ident: string, dbSecret: string): Buffer {
  return createHash('sha256')
    .update(ident + ':' + dbSecret + ':hmac')
    .digest()
}

/**
 * 计算业务表行数据的哈希，用于检测数据被外部改动。
 * ddl 为完整 schema 脚本（可能包含多张表），基础设施表不参与计算。
 */
export function computeTablesHash(key: Buffer, db: Database.Database, ddl: string): string {
  const tables = extractTableNames(ddl)
  const parts: string[] = []
  for (const table of tables) {
    const rows = db.prepare(`SELECT * FROM "${table}" ORDER BY 1`).all() as Record<string, unknown>[]
    const serialized = rows.map((r) => JSON.stringify(r)).join(';')
    parts.push(`${table}:${serialized}`)
  }
  const message = parts.join('|')
  return createHmac('sha256', key).update(message).digest('hex')
}

function extractTableNames(ddl: string): string[] {
  const tablePattern = /CREATE TABLE IF NOT EXISTS (\w+)/g
  return [...ddl.matchAll(tablePattern)]
    .map((match) => match[1])
    .filter((table) => table !== '_integrity' && table !== 'schema_version')
}

export function verifyHash(storedHash: string, computedHash: string): boolean {
  if (storedHash.length !== computedHash.length) return false
  return timingSafeEqual(Buffer.from(storedHash, 'hex'), Buffer.from(computedHash, 'hex'))
}
