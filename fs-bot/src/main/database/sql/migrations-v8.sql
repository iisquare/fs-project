-- v7 -> v8：会话增加当前分支尾（leaf_id），消息增加父消息（parent_id）
-- 说明：消息按 parent_id 组成一棵消息树，会话的 leaf_id 指向当前分支尾，
-- 页面只渲染「分支尾沿 parent_id 回溯到根」的那条路径。
-- 编辑提问重新发送、重新生成回复都会新起一条分支（原分支的消息与调用链照旧保留），
-- 切换分支只改 leaf_id，不动消息本身。历史会话没有 leaf_id 时按入库顺序补成一条链。

ALTER TABLE conversations ADD COLUMN leaf_id TEXT DEFAULT '';

ALTER TABLE messages ADD COLUMN parent_id TEXT DEFAULT '';
