import initSqlJs, { type Database } from 'sql.js'
import { afterEach, describe, expect, it } from 'vitest'
import schemaV14 from './sql/schema-v14.sql?raw'
import schemaV15 from './sql/schema-v15.sql?raw'
import migrationV15 from './sql/migrations-v15.sql?raw'

let db: Database | null = null

afterEach(() => {
  db?.close()
  db = null
})

function columns(database: Database, table: string): string[] {
  return (database.exec(`PRAGMA table_info(${table})`)[0]?.values ?? []).map((row) =>
    String(row[1])
  )
}

describe('user schema v15', () => {
  it('migrates an existing v14 sub-agent row without losing data', async () => {
    const SQL = await initSqlJs()
    db = new SQL.Database()
    db.exec(schemaV14)
    db.run(
      `INSERT INTO agent_sessions
       (id, project_id, parent_conversation_id, agent, task_key, title, messages, summary)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
      ['agent-1', 'project-1', 'conversation-1', 'browser', 'order-1', '旧任务', '[]', '进度']
    )

    db.exec(migrationV15)

    const result = db.exec(
      `SELECT session_mode, reused, trace, summary
       FROM agent_sessions WHERE id = 'agent-1'`
    )[0]
    expect(result.values[0]).toEqual(['new', 0, '[]', '进度'])
  })

  it('keeps the migrated table shape aligned with a fresh v15 database', async () => {
    const SQL = await initSqlJs()
    db = new SQL.Database()
    db.exec(schemaV14)
    db.exec(migrationV15)
    const migrated = columns(db, 'agent_sessions')
    db.close()

    db = new SQL.Database()
    db.exec(schemaV15)
    expect(migrated).toEqual(columns(db, 'agent_sessions'))
  })
})
