-- 用户库 v15 完整 schema（全新建库时执行）
-- 注意：基础设施表（schema_version、_integrity）不属于业务 schema，由 USER_META_TABLES 维护

CREATE TABLE IF NOT EXISTS projects (
  id TEXT PRIMARY KEY,
  model TEXT DEFAULT '',
  context_length INTEGER DEFAULT 0,
  system_prompt TEXT DEFAULT '',
  memory TEXT DEFAULT '',
  memory_enabled INTEGER DEFAULT 0,
  think_effort TEXT DEFAULT '',
  name TEXT NOT NULL,
  description TEXT DEFAULT '',
  icon TEXT DEFAULT '',
  env_vars TEXT DEFAULT '[]',
  tools TEXT DEFAULT '[]',
  workspace TEXT DEFAULT '[]',
  inputs TEXT DEFAULT '{}',
  sort INTEGER DEFAULT 0,
  share_id INTEGER DEFAULT 0,
  share_name TEXT DEFAULT '',
  share_version TEXT DEFAULT '',
  created_at TEXT NOT NULL DEFAULT (datetime('now')),
  updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_projects_sort ON projects(sort DESC, updated_at DESC);

CREATE TABLE IF NOT EXISTS conversations (
  id TEXT PRIMARY KEY,
  title TEXT NOT NULL DEFAULT '',
  project_id TEXT DEFAULT '',
  leaf_id TEXT DEFAULT '',
  created_at TEXT NOT NULL DEFAULT (datetime('now')),
  updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_conversations_project ON conversations(project_id, updated_at DESC);

CREATE TABLE IF NOT EXISTS messages (
  id TEXT PRIMARY KEY,
  conversation_id TEXT NOT NULL,
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

-- messages 保存跨任务续接所需的高层问答；trace 只保存脱敏后的工具名、状态、
-- 轮次与耗时，不保存参数、页面快照或工具返回正文。
CREATE TABLE IF NOT EXISTS agent_sessions (
  id TEXT PRIMARY KEY,
  project_id TEXT NOT NULL DEFAULT '',
  parent_conversation_id TEXT NOT NULL DEFAULT '',
  agent TEXT NOT NULL DEFAULT '',
  task_key TEXT NOT NULL DEFAULT '',
  title TEXT NOT NULL DEFAULT '',
  messages TEXT NOT NULL DEFAULT '[]',
  summary TEXT DEFAULT '',
  status TEXT DEFAULT '',
  error TEXT DEFAULT '',
  created_at TEXT NOT NULL DEFAULT (datetime('now')),
  updated_at TEXT NOT NULL DEFAULT (datetime('now')),
  session_mode TEXT NOT NULL DEFAULT 'new',
  reused INTEGER NOT NULL DEFAULT 0,
  trace TEXT NOT NULL DEFAULT '[]'
);

CREATE INDEX IF NOT EXISTS idx_agent_sessions_project
  ON agent_sessions(project_id, agent, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_agent_sessions_parent
  ON agent_sessions(parent_conversation_id);
CREATE INDEX IF NOT EXISTS idx_agent_sessions_task_key
  ON agent_sessions(project_id, parent_conversation_id, agent, task_key, updated_at DESC);

CREATE TABLE IF NOT EXISTS agent_experiences (
  id TEXT PRIMARY KEY,
  project_id TEXT NOT NULL DEFAULT '',
  agent TEXT NOT NULL DEFAULT '',
  content TEXT NOT NULL DEFAULT '',
  source_session_id TEXT NOT NULL DEFAULT '',
  created_at TEXT NOT NULL DEFAULT (datetime('now')),
  updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_agent_experiences_project
  ON agent_experiences(project_id, agent, updated_at DESC);
