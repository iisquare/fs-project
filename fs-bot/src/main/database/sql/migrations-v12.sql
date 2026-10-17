-- v11 -> v12：项目增加「启用客户端记忆」开关
-- 说明：默认关闭，升级后老项目的记忆内容不再下发、模型也拿不到 memory 工具；
-- 需要长期记忆的项目在项目编辑器里打开「启用记忆」即可恢复原有行为。

ALTER TABLE projects ADD COLUMN memory_enabled INTEGER DEFAULT 0;
