-- 用户库 v5 完整 schema（全新建库时执行）
-- 注意：基础设施表（schema_version、_integrity）不属于业务 schema，由 USER_META_TABLES 维护

CREATE TABLE IF NOT EXISTS projects (
  id TEXT PRIMARY KEY,
  model TEXT DEFAULT '',
  name TEXT NOT NULL,
  description TEXT DEFAULT '',
  icon TEXT DEFAULT '',
  env_vars TEXT DEFAULT '[]',
  tools TEXT DEFAULT '[]',
  workspace TEXT DEFAULT '[]',
  inputs TEXT DEFAULT '{}',
  sort INTEGER DEFAULT 0,
  created_at TEXT NOT NULL DEFAULT (datetime('now')),
  updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_projects_sort ON projects(sort DESC, updated_at DESC);

CREATE TABLE IF NOT EXISTS conversations (
  id TEXT PRIMARY KEY,
  title TEXT NOT NULL DEFAULT '',
  project_id TEXT DEFAULT '',
  created_at TEXT NOT NULL DEFAULT (datetime('now')),
  updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_conversations_project ON conversations(project_id, updated_at DESC);

CREATE TABLE IF NOT EXISTS messages (
  id TEXT PRIMARY KEY,
  conversation_id TEXT NOT NULL,
  role TEXT NOT NULL,
  content TEXT DEFAULT '',
  reasoning TEXT DEFAULT '',
  reference TEXT DEFAULT '',
  status TEXT DEFAULT '',
  created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS tasks (
  id TEXT PRIMARY KEY,
  project_id TEXT NOT NULL DEFAULT '',
  name TEXT NOT NULL,
  cron TEXT NOT NULL DEFAULT '',
  prompt TEXT NOT NULL DEFAULT '',
  inputs TEXT DEFAULT '{}',
  enabled INTEGER NOT NULL DEFAULT 1,
  last_run_at TEXT DEFAULT '',
  last_status TEXT DEFAULT '',
  last_message TEXT DEFAULT '',
  created_at TEXT NOT NULL DEFAULT (datetime('now')),
  updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_tasks_project ON tasks(project_id);

CREATE TABLE IF NOT EXISTS knowledge_bases (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  description TEXT DEFAULT '',
  segment_delimiter TEXT DEFAULT '',
  segment_length INTEGER DEFAULT 2000,
  chunk_length INTEGER DEFAULT 500,
  overlap INTEGER DEFAULT 50,
  recall_count INTEGER DEFAULT 20,
  score_threshold REAL DEFAULT 0.5,
  created_at TEXT NOT NULL DEFAULT (datetime('now')),
  updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS tools (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  type TEXT NOT NULL DEFAULT 'custom',
  description TEXT DEFAULT '',
  config TEXT DEFAULT '{}',
  created_at TEXT NOT NULL DEFAULT (datetime('now')),
  updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS skills (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  type TEXT NOT NULL DEFAULT 'installed',
  description TEXT DEFAULT '',
  installed_version TEXT DEFAULT '',
  latest_version TEXT DEFAULT '',
  created_at TEXT NOT NULL DEFAULT (datetime('now')),
  updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);
