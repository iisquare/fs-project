import initSqlJs, { type Database } from 'sql.js'
import { afterEach, describe, expect, it } from 'vitest'
import schemaV15 from './sql/schema-v15.sql?raw'
import schemaV16 from './sql/schema-v16.sql?raw'
import migrationV16 from './sql/migrations-v16.sql?raw'

let db: Database | null = null

afterEach(() => {
  db?.close()
  db = null
})

describe('user schema v16', () => {
  it('adds interaction storage without touching existing sub-agent sessions', async () => {
    const SQL = await initSqlJs()
    db = new SQL.Database()
    db.exec(schemaV15)
    db.run(
      `INSERT INTO agent_sessions
       (id, project_id, parent_conversation_id, agent, title, messages, summary)
       VALUES (?, ?, ?, ?, ?, ?, ?)`,
      ['session-1', 'project-1', 'conversation-1', 'browser', '任务', '[]', '已有结果']
    )

    db.exec(migrationV16)

    expect(db.exec("SELECT summary FROM agent_sessions WHERE id = 'session-1'")[0].values).toEqual([
      ['已有结果']
    ])
    expect(() =>
      db!.exec(
        `INSERT INTO agent_interactions
         (id, session_id, project_id, parent_conversation_id, started_at, updated_at, payload)
         VALUES ('task-1', 'session-1', 'project-1', 'conversation-1', 'now', 'now', '{}')`
      )
    ).not.toThrow()
    db.run(
      `INSERT INTO agent_interactions
       (id, session_id, project_id, parent_conversation_id, started_at, updated_at, payload)
       VALUES (?, ?, ?, ?, ?, ?, ?)
       ON CONFLICT(id) DO UPDATE SET payload = excluded.payload, updated_at = excluded.updated_at`,
      ['task-1', 'session-1', 'project-1', 'conversation-1', 'now', 'later', '{"status":"running"}']
    )
    expect(
      db.exec("SELECT payload FROM agent_interactions WHERE id = 'task-1'")[0].values[0][0]
    ).toBe('{"status":"running"}')
  })

  it('creates the same interaction table as a fresh v16 database', async () => {
    const SQL = await initSqlJs()
    db = new SQL.Database()
    db.exec(schemaV15)
    db.exec(migrationV16)
    const migrated = db
      .exec('PRAGMA table_info(agent_interactions)')[0]
      .values.map((row) => String(row[1]))
    db.close()

    db = new SQL.Database()
    db.exec(schemaV16)
    const fresh = db
      .exec('PRAGMA table_info(agent_interactions)')[0]
      .values.map((row) => String(row[1]))
    expect(migrated).toEqual(fresh)
  })
})
