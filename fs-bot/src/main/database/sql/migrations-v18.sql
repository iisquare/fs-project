-- v17 -> v18：项目能力改为统一标识集合。
ALTER TABLE projects ADD COLUMN capabilities TEXT NOT NULL DEFAULT '[]';

UPDATE projects
SET capabilities = (
  SELECT json_group_array(value)
  FROM (
    SELECT 'sub_agent.project_tools' AS value WHERE project_tool_agent_enabled = 1
    UNION ALL
    SELECT 'sub_agent.browser' WHERE browser_agent_enabled = 1
    UNION ALL
    SELECT 'sub_agent.desktop' WHERE desktop_agent_enabled = 1
  )
);

ALTER TABLE projects DROP COLUMN browser_agent_enabled;
ALTER TABLE projects DROP COLUMN project_tool_agent_enabled;
ALTER TABLE projects DROP COLUMN desktop_agent_enabled;
