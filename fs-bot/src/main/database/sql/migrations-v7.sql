-- v6 -> v7：项目增加生成参数（系统提示词与思考强度）
-- 说明：两者和 model 一样属于项目口径，由项目保存、按会话所属项目下发；
-- 空值都表示不下发——系统提示词留空则只带会话消息，思考强度留空则由模型与网关取自己的默认值。

ALTER TABLE projects ADD COLUMN system_prompt TEXT DEFAULT '';
ALTER TABLE projects ADD COLUMN think_effort TEXT DEFAULT '';
