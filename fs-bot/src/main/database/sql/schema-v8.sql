-- 用户库 v8 完整 schema（全新建库时执行）
-- 注意：基础设施表（schema_version、_integrity）不属于业务 schema，由 USER_META_TABLES 维护

CREATE TABLE IF NOT EXISTS projects (
  id TEXT PRIMARY KEY,
  model TEXT DEFAULT '',
  system_prompt TEXT DEFAULT '',
  think_effort TEXT DEFAULT '',
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
  -- 当前分支尾：消息按 parent_id 组成消息树，这里记页面正在看的那条路径的末端
  leaf_id TEXT DEFAULT '',
  created_at TEXT NOT NULL DEFAULT (datetime('now')),
  updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_conversations_project ON conversations(project_id, updated_at DESC);

CREATE TABLE IF NOT EXISTS messages (
  id TEXT PRIMARY KEY,
  conversation_id TEXT NOT NULL,
  -- 父消息：'' 表示分支起点；同一条父消息下的多条子消息互为分支
  parent_id TEXT DEFAULT '',
  role TEXT NOT NULL,
  content TEXT DEFAULT '',
  reasoning TEXT DEFAULT '',
  reference TEXT DEFAULT '',
  status TEXT DEFAULT '',
  created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_messages_parent ON messages(conversation_id, parent_id);

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
