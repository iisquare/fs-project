-- v5 -> v6：清掉本地不再使用的表
-- 说明：知识库、工具、技能以及技能包都在服务端维护，桌面端只在项目里记录引用了哪些
-- （projects.tools），技能包解压后直接落在本地 skills 目录；这三张表自服务端目录接管后
-- 就没有任何读写方，删掉以免误以为本地还有一份目录数据。

DROP TABLE IF EXISTS knowledge_bases;
DROP TABLE IF EXISTS tools;
DROP TABLE IF EXISTS skills;
