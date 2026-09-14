-- 用户中心授权关系迁移脚本
-- 将授权关系由通用关联表 fs_member_relation（或用户、角色实体的授权字段）迁移为独立关联表：
--   fs_member_user_role        用户角色
--   fs_member_role_application 角色应用
--   fs_member_role_menu        角色菜单，菜单所属应用由 fs_member_menu.application_id 推导
--   fs_member_role_resource    角色资源，资源所属应用由 fs_member_resource.application_id 推导
-- 各关联表均以业务主键构成联合主键，并针对反向查询建立二级索引
-- 执行前请先备份数据库，执行完成后 fs_member_relation 及实体授权字段不再使用

SET SESSION group_concat_max_len = 102400;

--
-- 1. 建立关联表
--
CREATE TABLE IF NOT EXISTS `fs_member_user_role` (
  `user_id` int NOT NULL DEFAULT '0',
  `role_id` int NOT NULL DEFAULT '0',
  `created_time` bigint NOT NULL DEFAULT '0',
  `created_uid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`user_id`,`role_id`) USING BTREE,
  KEY `idx_role_id` (`role_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;

CREATE TABLE IF NOT EXISTS `fs_member_role_application` (
  `role_id` int NOT NULL DEFAULT '0',
  `application_id` int NOT NULL DEFAULT '0',
  `created_time` bigint NOT NULL DEFAULT '0',
  `created_uid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`role_id`,`application_id`) USING BTREE,
  KEY `idx_application_id` (`application_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;

CREATE TABLE IF NOT EXISTS `fs_member_role_menu` (
  `role_id` int NOT NULL DEFAULT '0',
  `menu_id` int NOT NULL DEFAULT '0',
  `created_time` bigint NOT NULL DEFAULT '0',
  `created_uid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`role_id`,`menu_id`) USING BTREE,
  KEY `idx_menu_id` (`menu_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;

CREATE TABLE IF NOT EXISTS `fs_member_role_resource` (
  `role_id` int NOT NULL DEFAULT '0',
  `resource_id` int NOT NULL DEFAULT '0',
  `created_time` bigint NOT NULL DEFAULT '0',
  `created_uid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`role_id`,`resource_id`) USING BTREE,
  KEY `idx_resource_id` (`resource_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;

--
-- 2. 迁移通用关联表数据
--
INSERT IGNORE INTO `fs_member_user_role` (`user_id`, `role_id`, `created_time`, `created_uid`)
SELECT `aid`, `bid`, UNIX_TIMESTAMP() * 1000, 0
FROM `fs_member_relation` WHERE `type` = 'user_role';

INSERT IGNORE INTO `fs_member_role_application` (`role_id`, `application_id`, `created_time`, `created_uid`)
SELECT `aid`, `bid`, UNIX_TIMESTAMP() * 1000, 0
FROM `fs_member_relation` WHERE `type` = 'role_application';

INSERT IGNORE INTO `fs_member_role_menu` (`role_id`, `menu_id`, `created_time`, `created_uid`)
SELECT `aid`, `bid`, UNIX_TIMESTAMP() * 1000, 0
FROM `fs_member_relation` WHERE `type` = 'role_menu';

INSERT IGNORE INTO `fs_member_role_resource` (`role_id`, `resource_id`, `created_time`, `created_uid`)
SELECT `aid`, `bid`, UNIX_TIMESTAMP() * 1000, 0
FROM `fs_member_relation` WHERE `type` = 'role_resource';

--
-- 3. 清理通用关联表
--
DROP TABLE IF EXISTS `fs_member_relation`;

--
-- 4. 清理实体授权字段
--
ALTER TABLE `fs_member_user` DROP COLUMN `role_ids`;
ALTER TABLE `fs_member_role`
  DROP COLUMN `application_ids`,
  DROP COLUMN `menu_ids`,
  DROP COLUMN `resource_ids`;

--
-- 附：若已执行过"授权标识维护在实体字段中"的临时版本，请先取消注释本段，再执行上面的清理
--
-- INSERT IGNORE INTO `fs_member_user_role` (`user_id`, `role_id`, `created_time`, `created_uid`)
-- SELECT t.`id`, j.`id`, UNIX_TIMESTAMP() * 1000, 0 FROM `fs_member_user` t
-- JOIN JSON_TABLE(CONCAT('["', REPLACE(t.`role_ids`, ',', '","'), '"]'),
--     '$[*]' COLUMNS(`id` INT PATH '$')) j WHERE t.`role_ids` <> '';
-- INSERT IGNORE INTO `fs_member_role_application` (`role_id`, `application_id`, `created_time`, `created_uid`)
-- SELECT t.`id`, j.`id`, UNIX_TIMESTAMP() * 1000, 0 FROM `fs_member_role` t
-- JOIN JSON_TABLE(CONCAT('["', REPLACE(t.`application_ids`, ',', '","'), '"]'),
--     '$[*]' COLUMNS(`id` INT PATH '$')) j WHERE t.`application_ids` <> '';
-- INSERT IGNORE INTO `fs_member_role_menu` (`role_id`, `menu_id`, `created_time`, `created_uid`)
-- SELECT t.`id`, j.`id`, UNIX_TIMESTAMP() * 1000, 0 FROM `fs_member_role` t
-- JOIN JSON_TABLE(CONCAT('["', REPLACE(t.`menu_ids`, ',', '","'), '"]'),
--     '$[*]' COLUMNS(`id` INT PATH '$')) j WHERE t.`menu_ids` <> '';
-- INSERT IGNORE INTO `fs_member_role_resource` (`role_id`, `resource_id`, `created_time`, `created_uid`)
-- SELECT t.`id`, j.`id`, UNIX_TIMESTAMP() * 1000, 0 FROM `fs_member_role` t
-- JOIN JSON_TABLE(CONCAT('["', REPLACE(t.`resource_ids`, ',', '","'), '"]'),
--     '$[*]' COLUMNS(`id` INT PATH '$')) j WHERE t.`resource_ids` <> '';

--
-- 附：关联表脏数据清理与唯一约束，存量环境建议一并执行
-- 唯一约束要求菜单在「同一应用 + 同一上级」下不同名、资源在「同一应用」下授权标识不重复
-- 执行前请先确认没有重复数据，否则唯一索引创建会失败
--
-- DELETE ur FROM `fs_member_user_role` ur LEFT JOIN `fs_member_user` u ON u.`id` = ur.`user_id` WHERE u.`id` IS NULL;
-- DELETE rm FROM `fs_member_role_menu` rm LEFT JOIN `fs_member_menu` m ON m.`id` = rm.`menu_id` WHERE m.`id` IS NULL;
-- DELETE rr FROM `fs_member_role_resource` rr LEFT JOIN `fs_member_resource` r ON r.`id` = rr.`resource_id` WHERE r.`id` IS NULL;
-- DELETE ra FROM `fs_member_role_application` ra LEFT JOIN `fs_member_application` a ON a.`id` = ra.`application_id` WHERE a.`id` IS NULL;
-- ALTER TABLE `fs_member_menu` ADD UNIQUE KEY `uniq_app_parent_name` (`application_id`, `parent_id`, `name`);
-- ALTER TABLE `fs_member_resource` ADD UNIQUE KEY `uniq_app_permit` (`application_id`, `module`, `controller`, `action`);
