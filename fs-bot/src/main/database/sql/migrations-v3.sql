-- v2 -> v3：项目增加知识库引用（knowledge）
-- 说明：知识库本体（文档、分段、检索配置）由服务端维护，桌面端只在项目里记录引用了哪些，
-- 与 skills / tools 同样以 JSON 数组存 id 与名称；本地 knowledge_bases 表自此不再使用。

ALTER TABLE projects ADD COLUMN knowledge TEXT DEFAULT '[]';
