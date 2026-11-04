-- v12 -> v13：子 Agent 的任务会话与可复用经验

CREATE TABLE IF NOT EXISTS agent_sessions (
  id TEXT PRIMARY KEY,
  project_id TEXT NOT NULL DEFAULT '',
  parent_conversation_id TEXT NOT NULL DEFAULT '',
  agent TEXT NOT NULL DEFAULT '',
  title TEXT NOT NULL DEFAULT '',
  messages TEXT NOT NULL DEFAULT '[]',
  summary TEXT DEFAULT '',
  status TEXT DEFAULT '',
  error TEXT DEFAULT '',
  created_at TEXT NOT NULL DEFAULT (datetime('now')),
  updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_agent_sessions_project
  ON agent_sessions(project_id, agent, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_agent_sessions_parent
  ON agent_sessions(parent_conversation_id);

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
