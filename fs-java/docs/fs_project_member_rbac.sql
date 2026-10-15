-- 用户中心 RBAC 初始化脚本
-- 目标：按「最小管理权限」生成 1 个内置 admin 用户 与 1 个后台管理角色
--
-- 依赖表：
--   fs_member_user             用户
--   fs_member_role             角色
--   fs_member_user_role        用户角色绑定
--   fs_member_role_application 角色应用授权
--   fs_member_role_menu        角色菜单授权
--   fs_member_role_resource    角色资源授权
--
-- 权限模型（见 RbacService.buildRolePermit）：
--   应用授权 → 应用级权限键 serial::（如 member::）
--   资源授权 → 资源级权限键 module:controller:action（如 member:user:add，
--             用户中心入口为 module:controller: 形式，如 member:user:）
--   菜单授权 → 菜单树，菜单所属应用必须同时被授权
--   角色未启用（status<>1）、应用未启用时授权不生效
--
-- 权限范围（最小管理权限）：
--   仅授予平台自身管理所需的应用
--     admin  后台管理（仪表盘、个人中心）
--     member 用户中心（账号、角色、菜单、资源、配置、字典、应用、数据权限等）
--   不授予任何业务应用（bi / cms / face / govern / lm / agent / oa / server / spider / kg ...）
--   如需进一步收紧，调整下方「2. 角色授权」中应用、菜单、资源的筛选条件即可
--
-- 执行说明：
--   1. 脚本可重复执行，已存在的用户、角色及授权关系不会重复写入；
--   2. 已存在的 admin 用户不会被覆盖，如需重置密码请自行修改；
--   3. fs_member_user.name 存在唯一约束，若昵称「管理员」已被占用请先调整 @user_name；
--   4. 初始密码建议首次登录后立即修改。
--   初始账号：admin / 123456（salt=fs01，密文 MD5(MD5('123456') + 'fs01') = 7e81e20376faea8494f6fb82a0f9e39c）

SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;

SET @now = UNIX_TIMESTAMP() * 1000;   -- 毫秒时间戳，与业务写入保持一致
SET @user_serial = 'admin';            -- 登录账号
SET @user_name = '管理员';              -- 昵称
SET @user_salt = 'fs01';               -- 4 位密码盐
SET @user_password = 'admin888';         -- 初始密码
SET @role_name = '后台管理';            -- 角色名称

--
-- 1. 内置用户与角色
--

-- 1.1 用户：账号 admin，密码 MD5(MD5(明文) + salt)，状态启用
--     等价于 UserService.password()：CodeUtil.md5(CodeUtil.md5(password) + salt)
SET @user_exists = (SELECT COUNT(*) FROM `fs_member_user` WHERE `serial` = @user_serial);
INSERT INTO `fs_member_user`
(`serial`, `name`, `password`, `email`, `phone`, `salt`, `sort`, `status`, `description`,
 `created_time`, `created_ip`, `created_uid`, `updated_time`, `updated_uid`,
 `login_time`, `login_ip`, `locked_time`, `deleted_time`, `deleted_uid`)
SELECT @user_serial, @user_name, MD5(CONCAT(MD5(@user_password), @user_salt)), '', '', @user_salt,
       0, 1, '内置管理员',
       @now, '', 0, @now, 0,
       0, '', 0, 0, 0
FROM DUAL
WHERE @user_exists = 0;

SET @user_id = (SELECT `id` FROM `fs_member_user` WHERE `serial` = @user_serial ORDER BY `id` LIMIT 1);

-- 1.2 角色：后台管理
SET @role_exists = (SELECT COUNT(*) FROM `fs_member_role` WHERE `name` = @role_name);
INSERT INTO `fs_member_role`
(`name`, `sort`, `status`, `description`, `created_time`, `created_uid`, `updated_time`, `updated_uid`)
SELECT @role_name, 0, 1, '内置后台管理角色，仅授予平台管理所需的最小权限',
       @now, 0, @now, 0
FROM DUAL
WHERE @role_exists = 0;

SET @role_id = (SELECT `id` FROM `fs_member_role` WHERE `name` = @role_name ORDER BY `id` LIMIT 1);

-- 1.3 用户角色绑定：admin -> 后台管理
INSERT IGNORE INTO `fs_member_user_role` (`user_id`, `role_id`, `created_time`, `created_uid`)
VALUES (@user_id, @role_id, @now, 0);

--
-- 2. 角色授权（最小管理权限）
--

-- 2.1 应用授权：后台管理（仪表盘、个人中心） + 用户中心（平台管理）
INSERT IGNORE INTO `fs_member_role_application` (`role_id`, `application_id`, `created_time`, `created_uid`)
SELECT @role_id, `id`, @now, 0
FROM `fs_member_application`
WHERE `status` = 1 AND `serial` IN ('admin', 'member');

-- 2.2 菜单授权：上述应用下的全部启用菜单
INSERT IGNORE INTO `fs_member_role_menu` (`role_id`, `menu_id`, `created_time`, `created_uid`)
SELECT @role_id, `id`, @now, 0
FROM `fs_member_menu`
WHERE `status` = 1 AND `application_id` IN (
    SELECT `id` FROM `fs_member_application`
    WHERE `status` = 1 AND `serial` IN ('admin', 'member')
);

-- 2.3 资源授权：上述应用下的全部启用资源
INSERT IGNORE INTO `fs_member_role_resource` (`role_id`, `resource_id`, `created_time`, `created_uid`)
SELECT @role_id, `id`, @now, 0
FROM `fs_member_resource`
WHERE `status` = 1 AND `application_id` IN (
    SELECT `id` FROM `fs_member_application`
    WHERE `status` = 1 AND `serial` IN ('admin', 'member')
);

--
-- 3. 执行结果校验
--

SELECT @user_id AS `user_id`, @role_id AS `role_id`;
SELECT `id`, `serial`, `name`, `status` FROM `fs_member_user` WHERE `id` = @user_id;
SELECT `id`, `name`, `status` FROM `fs_member_role` WHERE `id` = @role_id;
SELECT
    (SELECT COUNT(*) FROM `fs_member_user_role` WHERE `user_id` = @user_id AND `role_id` = @role_id) AS `user_role`,
    (SELECT COUNT(*) FROM `fs_member_role_application` WHERE `role_id` = @role_id) AS `role_application`,
    (SELECT COUNT(*) FROM `fs_member_role_menu` WHERE `role_id` = @role_id) AS `role_menu`,
    (SELECT COUNT(*) FROM `fs_member_role_resource` WHERE `role_id` = @role_id) AS `role_resource`;

--
-- 附：更严格的最小权限示例（如需「登录后仅可见仪表盘与个人中心」，改用本段并跳过 2.1~2.3）
--
-- -- 仅授权后台管理应用
-- INSERT IGNORE INTO `fs_member_role_application` (`role_id`, `application_id`, `created_time`, `created_uid`)
-- SELECT @role_id, `id`, @now, 0 FROM `fs_member_application`
-- WHERE `status` = 1 AND `serial` = 'admin';
--
-- -- 仅授权仪表盘（3）、工作台（4）、个人中心（18）、个人信息（19）、修改密码（20）
-- INSERT IGNORE INTO `fs_member_role_menu` (`role_id`, `menu_id`, `created_time`, `created_uid`)
-- SELECT @role_id, `id`, @now, 0 FROM `fs_member_menu`
-- WHERE `status` = 1 AND `id` IN (3, 4, 18, 19, 20);
--
-- 提示：授权变更后角色权限缓存（Redis）最长 30 分钟后自动重建，期间可调用 /rbac/resource 触发刷新
