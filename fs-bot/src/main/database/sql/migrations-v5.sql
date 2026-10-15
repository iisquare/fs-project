-- v4 -> v5：项目不再绑定编排应用，会话也不再对应服务端会话
-- 说明：项目的能力清单统一在 tools（工具）里按类型引用，编排应用只是其中一种类型；
-- 会话与消息只存在本地，不再有服务端会话标识。以下几列自此没有读写方，一并删除。

ALTER TABLE projects DROP COLUMN agentic_id;
ALTER TABLE projects DROP COLUMN skills;
ALTER TABLE projects DROP COLUMN knowledge;
ALTER TABLE conversations DROP COLUMN agentic_id;
ALTER TABLE conversations DROP COLUMN chat_id;
