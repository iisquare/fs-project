-- v13 -> v14：子 Agent 会话支持稳定的自动续接键

ALTER TABLE agent_sessions ADD COLUMN task_key TEXT NOT NULL DEFAULT '';

CREATE INDEX IF NOT EXISTS idx_agent_sessions_task_key
  ON agent_sessions(project_id, parent_conversation_id, agent, task_key, updated_at DESC);
