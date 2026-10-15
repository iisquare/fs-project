import type Database from 'better-sqlite3-multiple-ciphers'
import v2 from './sql/migrations-v2.sql?raw'
import v3 from './sql/migrations-v3.sql?raw'
import v4 from './sql/migrations-v4.sql?raw'
import v5 from './sql/migrations-v5.sql?raw'
import v6 from './sql/migrations-v6.sql?raw'
import v7 from './sql/migrations-v7.sql?raw'
import v8 from './sql/migrations-v8.sql?raw'
import v9 from './sql/migrations-v9.sql?raw'
import v10 from './sql/migrations-v10.sql?raw'
import v11 from './sql/migrations-v11.sql?raw'
import { USER_META_TABLES, USER_SCHEMA_VERSION, getUserSchema } from './schema'

/**
 * 升级脚本注册表：键为「升级后的版本号」，值为 `sql/migrations-vN.sql` 的内容。
 *
 * v1 是新库初始 schema，不需要升级脚本；新增版本时在此登记，例如：
 *   import v2 from './sql/migrations-v2.sql?raw'
 *   const USER_MIGRATIONS: Record<number, string> = { 2: v2 }
 */
export const USER_MIGRATIONS: Record<number, string> = {
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

/**
 * 把用户库调整到目标 schema 版本。
 *
 * - 无版本记录（全新库，或早于版本管理的老库）：执行目标版本的完整 schema，老库靠 IF NOT EXISTS 补齐缺失表
 * - 已有版本记录：按版本号依次执行升级脚本，缺少任何一步都直接报错
 * - 版本高于应用（降级）：报错，避免用旧结构读写新数据
 *
 * 除基础设施表外的所有语句在同一个 SAVEPOINT 中执行，任一步失败整体回滚。
 */
export function ensureUserSchema(
  db: Database.Database,
  targetVersion: number = USER_SCHEMA_VERSION
): void {
  for (const sql of USER_META_TABLES) {
    db.exec(sql)
  }

  const current = getCurrentVersion(db)

  if (current > targetVersion) {
    throw new Error(
      `[DB] User DB version (v${current}) is newer than app version (v${targetVersion}). ` +
        `Downgrade is not supported. Please upgrade the application.`
    )
  }
  if (current === targetVersion) return

  const startTime = performance.now()
  db.exec('SAVEPOINT migration')
  try {
    if (current === 0) {
      console.log(`[DB] Creating user schema v${targetVersion}`)
      db.exec(getUserSchema(targetVersion))
      setVersion(db, targetVersion)
    } else {
      for (let version = current + 1; version <= targetVersion; version++) {
        const script = USER_MIGRATIONS[version]
        if (!script) {
          throw new Error(
            `[DB] Missing user migration script for v${version} ` +
              `(expected sql/migrations-v${version}.sql)`
          )
        }
        console.log(`[DB] Running user migration v${version}/${targetVersion}`)
        db.exec(script)
        setVersion(db, version)
      }
    }
    db.exec('RELEASE migration')

    const elapsed = Math.round(performance.now() - startTime)
    console.log(`[DB] User schema ready: v${current} -> v${targetVersion} (${elapsed}ms)`)
  } catch (e) {
    db.exec('ROLLBACK TO migration')
    throw e
  }
}

function getCurrentVersion(db: Database.Database): number {
  const row = db
    .prepare('SELECT version FROM schema_version ORDER BY version DESC LIMIT 1')
    .get() as { version: number } | undefined
  return row?.version ?? 0
}

function setVersion(db: Database.Database, version: number): void {
  db.prepare(
    `INSERT INTO schema_version (version, updated_at) VALUES (?, datetime('now'))
     ON CONFLICT(version) DO UPDATE SET updated_at = excluded.updated_at`
  ).run(version)
}
