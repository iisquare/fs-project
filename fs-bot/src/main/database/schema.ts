import v1 from './sql/schema-v1.sql?raw'
import v2 from './sql/schema-v2.sql?raw'
import v3 from './sql/schema-v3.sql?raw'
import v4 from './sql/schema-v4.sql?raw'
import v5 from './sql/schema-v5.sql?raw'
import v6 from './sql/schema-v6.sql?raw'
import v7 from './sql/schema-v7.sql?raw'
import v8 from './sql/schema-v8.sql?raw'
import v9 from './sql/schema-v9.sql?raw'
import v10 from './sql/schema-v10.sql?raw'
import v11 from './sql/schema-v11.sql?raw'

/**
 * 用户库 schema 版本管理。
 *
 * - `sql/schema-vN.sql`  该版本的完整 schema，用于全新建库
 * - `sql/migrations-vN.sql`  vN-1 → vN 的升级脚本，用于已有库升级
 *
 * 新增变更时两份文件都要加，然后在下方注册表里登记（详见 sql/README.md）。
 */

/**
 * 基础设施表：版本判定与完整性哈希依赖它们，任何版本都先建立
 * （不属于业务 schema，不参与版本文件与哈希计算）
 */
export const USER_META_TABLES: string[] = [
  `CREATE TABLE IF NOT EXISTS schema_version (
    version INTEGER PRIMARY KEY,
    updated_at TEXT NOT NULL DEFAULT (datetime('now'))
  )`,
  `CREATE TABLE IF NOT EXISTS _integrity (
    id INTEGER PRIMARY KEY,
    hash TEXT NOT NULL,
    updated_at TEXT NOT NULL DEFAULT (datetime('now'))
  )`
]

/** 各版本的完整 schema，键为版本号 */
export const USER_SCHEMAS: Record<number, string> = {
  1: v1,
  2: v2,
  3: v3,
  4: v4,
  5: v5,
  6: v6,
  7: v7,
  8: v8,
  9: v9,
  10: v10,
  11: v11
}

/** 应用当前使用的用户库版本：取已登记的最大版本号 */
export const USER_SCHEMA_VERSION = Math.max(
  ...Object.keys(USER_SCHEMAS).map((version) => Number(version))
)

export function getUserSchema(version: number): string {
  const schema = USER_SCHEMAS[version]
  if (!schema) {
    throw new Error(
      `[DB] Missing user schema file for v${version} (expected sql/schema-v${version}.sql)`
    )
  }
  return schema
}

/** 最新版本的完整 schema，用于完整性哈希计算与全新建库 */
export function getLatestUserSchema(): string {
  return getUserSchema(USER_SCHEMA_VERSION)
}
