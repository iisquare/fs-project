-- v15 -> v16：子 Agent 的逐轮交互明细（正文、思考、工具参数与返回）

CREATE TABLE IF NOT EXISTS agent_interactions (
  id TEXT PRIMARY KEY,
  session_id TEXT NOT NULL,
  project_id TEXT NOT NULL DEFAULT '',
  parent_conversation_id TEXT NOT NULL DEFAULT '',
  started_at TEXT NOT NULL,
  updated_at TEXT NOT NULL,
  payload TEXT NOT NULL DEFAULT '{}'
);

CREATE INDEX IF NOT EXISTS idx_agent_interactions_session
  ON agent_interactions(session_id, started_at DESC);
CREATE INDEX IF NOT EXISTS idx_agent_interactions_parent
  ON agent_interactions(parent_conversation_id);
CREATE INDEX IF NOT EXISTS idx_agent_interactions_project
  ON agent_interactions(project_id);
