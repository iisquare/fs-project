-- v1 -> v2：引入项目（projects）与定时任务（tasks），会话改为「项目 + 服务端会话」
-- 说明：v1 的 conversations.app_id 是本地应用标识（本地模拟应用已由服务端编排应用替代），
-- 升级时统一改挂项目与服务端会话，旧列一并删除

ALTER TABLE conversations ADD COLUMN project_id TEXT DEFAULT '';
ALTER TABLE conversations ADD COLUMN agentic_id INTEGER NOT NULL DEFAULT 0;
ALTER TABLE conversations ADD COLUMN chat_id INTEGER NOT NULL DEFAULT 0;
ALTER TABLE conversations DROP COLUMN app_id;

CREATE TABLE IF NOT EXISTS projects (
  id TEXT PRIMARY KEY,
  agentic_id INTEGER NOT NULL DEFAULT 0,
  name TEXT NOT NULL,
  description TEXT DEFAULT '',
  icon TEXT DEFAULT '',
  env_vars TEXT DEFAULT '[]',
  skills TEXT DEFAULT '[]',
  tools TEXT DEFAULT '[]',
  workspace TEXT DEFAULT '[]',
  inputs TEXT DEFAULT '{}',
  sort INTEGER DEFAULT 0,
  created_at TEXT NOT NULL DEFAULT (datetime('now')),
  updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_projects_sort ON projects(sort DESC, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_conversations_project ON conversations(project_id, updated_at DESC);

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

ALTER TABLE messages ADD COLUMN reasoning TEXT DEFAULT '';
ALTER TABLE messages ADD COLUMN reference TEXT DEFAULT '';
ALTER TABLE messages ADD COLUMN status TEXT DEFAULT '';
