-- v16 -> v17：三类子 Agent 独立开关，旧项目默认全部关闭
ALTER TABLE projects ADD COLUMN browser_agent_enabled INTEGER NOT NULL DEFAULT 0;
ALTER TABLE projects ADD COLUMN project_tool_agent_enabled INTEGER NOT NULL DEFAULT 0;
ALTER TABLE projects ADD COLUMN desktop_agent_enabled INTEGER NOT NULL DEFAULT 0;
