-- 用户库 v12 完整 schema（全新建库时执行）
-- 注意：基础设施表（schema_version、_integrity）不属于业务 schema，由 USER_META_TABLES 维护

CREATE TABLE IF NOT EXISTS projects (
  id TEXT PRIMARY KEY,
  model TEXT DEFAULT '',
  -- 手工指定的上下文窗口（词元数）：0 表示按服务端在模型清单里声明的窗口
  context_length INTEGER DEFAULT 0,
  system_prompt TEXT DEFAULT '',
  -- 长期记忆：与系统提示词分开存放的一段文字，仅在 memory_enabled 为 1 时随每轮请求下发
  -- （编辑器里手写，模型也能用 memory 工具的 write 写；见 chat store 的 memoryBlock）
  memory TEXT DEFAULT '',
  -- 是否启用客户端记忆：默认关闭；关闭时记忆内容不下发，memory 工具也不下发给模型
  memory_enabled INTEGER DEFAULT 0,
  think_effort TEXT DEFAULT '',
  name TEXT NOT NULL,
  description TEXT DEFAULT '',
  icon TEXT DEFAULT '',
  env_vars TEXT DEFAULT '[]',
  tools TEXT DEFAULT '[]',
  workspace TEXT DEFAULT '[]',
  inputs TEXT DEFAULT '{}',
  sort INTEGER DEFAULT 0,
  -- 分享去向：服务端分享项目标识、名称与最新发布的版本号，0 / 空串表示未分享
  share_id INTEGER DEFAULT 0,
  share_name TEXT DEFAULT '',
  share_version TEXT DEFAULT '',
  created_at TEXT NOT NULL DEFAULT (datetime('now')),
  updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_projects_sort ON projects(sort DESC, updated_at DESC);

CREATE TABLE IF NOT EXISTS conversations (
  id TEXT PRIMARY KEY,
  title TEXT NOT NULL DEFAULT '',
  project_id TEXT DEFAULT '',
  -- 当前分支尾：消息按 parent_id 组成消息树，这里记页面正在看的那条路径的末端
  leaf_id TEXT DEFAULT '',
  created_at TEXT NOT NULL DEFAULT (datetime('now')),
  updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_conversations_project ON conversations(project_id, updated_at DESC);

CREATE TABLE IF NOT EXISTS messages (
  id TEXT PRIMARY KEY,
  conversation_id TEXT NOT NULL,
  -- 父消息：'' 表示分支起点；同一条父消息下的多条子消息互为分支
  parent_id TEXT DEFAULT '',
  role TEXT NOT NULL,
  content TEXT DEFAULT '',
  reasoning TEXT DEFAULT '',
  reference TEXT DEFAULT '',
  status TEXT DEFAULT '',
  created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_messages_parent ON messages(conversation_id, parent_id);

CREATE TABLE IF NOT EXISTS tasks (
  id TEXT PRIMARY KEY,
  project_id TEXT NOT NULL DEFAULT '',
  name TEXT NOT NULL,
  cron TEXT NOT NULL DEFAULT '',
  prompt TEXT NOT NULL DEFAULT '',
  inputs TEXT DEFAULT '{}',
  enabled INTEGER NOT NULL DEFAULT 1,
  last_run_at TEXT DEFAULT '',
  last_status TEXT DEFAULT '',
  last_message TEXT DEFAULT '',
  created_at TEXT NOT NULL DEFAULT (datetime('now')),
  updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_tasks_project ON tasks(project_id);
