-- v14 -> v15：保留子 Agent 的续接方式与脱敏执行轨迹

ALTER TABLE agent_sessions ADD COLUMN session_mode TEXT NOT NULL DEFAULT 'new';
ALTER TABLE agent_sessions ADD COLUMN reused INTEGER NOT NULL DEFAULT 0;
ALTER TABLE agent_sessions ADD COLUMN trace TEXT NOT NULL DEFAULT '[]';
