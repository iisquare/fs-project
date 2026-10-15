-- v9 -> v10：项目增加「分享去向」三个字段（share_id / share_name / share_version）
-- 说明：项目可以发布到服务端分享（见 agent 模块的项目分享）。发布后把「分享项目标识 + 名称 + 最新版本号」
-- 记在本地，用于卡片上回显「已分享 v1.0」、再次分享时默认选中同一个分享项目。
-- 服务端才是分享内容的权威存储，这里只是本地便签：清空 / 丢失都不影响服务端上的版本记录。
-- 老库的项目一律补 0 / 空串：升级后行为与升级前一致（未分享）。

ALTER TABLE projects ADD COLUMN share_id INTEGER DEFAULT 0;
ALTER TABLE projects ADD COLUMN share_name TEXT DEFAULT '';
ALTER TABLE projects ADD COLUMN share_version TEXT DEFAULT '';
