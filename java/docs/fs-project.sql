/*
 Navicat Premium Data Transfer

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 50729
 Source Host           : localhost:3306
 Source Schema         : fs-project

 Target Server Type    : MySQL
 Target Server Version : 50729
 File Encoding         : 65001

 Date: 14/03/2020 18:23:09
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for fs_member_menu
-- ----------------------------
DROP TABLE IF EXISTS `fs_member_menu`;
CREATE TABLE `fs_member_menu` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(32) NOT NULL DEFAULT '',
  `full_name` varchar(255) NOT NULL DEFAULT '',
  `parent_id` int(11) NOT NULL DEFAULT '0',
  `icon` varchar(64) NOT NULL DEFAULT '',
  `url` varchar(255) NOT NULL DEFAULT '',
  `target` varchar(8) NOT NULL DEFAULT '',
  `sort` tinyint(4) NOT NULL DEFAULT '0',
  `status` tinyint(4) NOT NULL DEFAULT '0',
  `description` text NOT NULL,
  `created_time` bigint(20) NOT NULL DEFAULT '0',
  `created_uid` int(11) NOT NULL DEFAULT '0',
  `updated_time` bigint(20) NOT NULL DEFAULT '0',
  `updated_uid` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of fs_member_menu
-- ----------------------------
BEGIN;
INSERT INTO `fs_member_menu` VALUES (1, '管理后台', '管理后台', 0, 'coffee', '', '', 0, 1, '', 1528082694531, 1, 1528082694531, 1);
INSERT INTO `fs_member_menu` VALUES (2, '用户中心', '管理后台:用户中心', 1, 'coffee', '/#/member/index/index', '', 0, 1, '', 1528082833701, 1, 1553161852203, 1);
INSERT INTO `fs_member_menu` VALUES (3, '用户管理', '用户管理', 2, 'coffee', '/user/index', '', 0, 1, '', 1528082886891, 1, 1528082886891, 1);
INSERT INTO `fs_member_menu` VALUES (4, '角色管理', '角色管理', 2, 'coffee', '/role/index', '', 0, 1, '', 1528082942723, 1, 1528082942723, 1);
INSERT INTO `fs_member_menu` VALUES (5, '配置管理', '配置管理', 2, 'coffee', '/setting/index', '', 0, 1, '', 1528083035344, 1, 1528083035344, 1);
INSERT INTO `fs_member_menu` VALUES (6, '菜单管理', '菜单管理', 2, 'coffee', '/menu/index', '', 0, 1, '', 1528083079182, 1, 1528083079182, 1);
INSERT INTO `fs_member_menu` VALUES (7, '资源管理', '资源管理', 2, 'coffee', '/resource/index', '', 0, 1, '', 1528083144253, 1, 1528083144253, 1);
INSERT INTO `fs_member_menu` VALUES (8, '数据计算', '数据计算', 1, 'coffee', '', '', 1, 1, '', 1530087300953, 1, 1553161848833, 1);
INSERT INTO `fs_member_menu` VALUES (9, '流程管理', '流程管理', 8, 'coffee', '/flow/index', '', 0, 1, '', 1530087389467, 1, 1530087389467, 1);
INSERT INTO `fs_member_menu` VALUES (10, '作业管理', '作业管理', 8, 'coffee', '/job/index', '', 0, 2, '', 1530087416459, 1, 1532067680468, 1);
INSERT INTO `fs_member_menu` VALUES (11, '业务报表', '业务报表', 1, 'coffee', '', '', 0, 2, '', 1530087517085, 1, 1553161856943, 1);
INSERT INTO `fs_member_menu` VALUES (12, '流程节点', '流程节点', 8, 'coffee', '/flowNode/index', '', 0, 1, '', 1530502987472, 1, 1530502987472, 1);
INSERT INTO `fs_member_menu` VALUES (13, '流程插件', '流程插件', 8, 'coffee', '/flowPlugin/index', '', 0, 1, '', 1531277525323, 1, 1531277525323, 1);
INSERT INTO `fs_member_menu` VALUES (14, '作业调度', '作业调度', 8, 'coffee', '/cron/index', '', 0, 1, '', 1531382291274, 1, 1531382332221, 1);
INSERT INTO `fs_member_menu` VALUES (15, '数据分析', '数据分析', 1, 'coffee', '', '', 2, 1, '', 1544491928382, 1, 1553161845649, 1);
INSERT INTO `fs_member_menu` VALUES (16, '数据报表', '数据报表', 1, 'coffee', '', '', 3, 1, '', 1544491964333, 1, 1553161841449, 1);
INSERT INTO `fs_member_menu` VALUES (17, '模型管理', '模型管理', 15, 'coffee', '/analysis/index', '', 0, 1, '', 1544493243839, 1, 1544577145273, 1);
INSERT INTO `fs_member_menu` VALUES (18, '报表管理', '报表管理', 16, 'coffee', '/report/index', '', 0, 1, '', 1544493672706, 1, 1544493672706, 1);
INSERT INTO `fs_member_menu` VALUES (19, '数据面板', '数据面板', 16, 'coffee', '/dashboard/index', '', 0, 1, '', 1544493724883, 1, 1544493724883, 1);
INSERT INTO `fs_member_menu` VALUES (20, '数据仓库', '数据仓库', 15, 'coffee', '/analysisNode/index', '', 0, 1, '', 1544503324527, 1, 1544503324527, 1);
INSERT INTO `fs_member_menu` VALUES (21, '网页爬虫', '网页爬虫', 1, 'coffee', '', '', 1, 1, '', 1557814673413, 1, 1557914109586, 1);
INSERT INTO `fs_member_menu` VALUES (22, '爬虫模板', '爬虫模板', 21, 'coffee', '/spider/index', '', 0, 1, '', 1557814732926, 1, 1557914095678, 1);
INSERT INTO `fs_member_menu` VALUES (23, '爬虫面板', '爬虫面板', 21, 'coffee', '/spider/dashboard', '', 0, 1, '', 1557914083549, 1, 1557914083549, 1);
COMMIT;

-- ----------------------------
-- Table structure for fs_member_relation
-- ----------------------------
DROP TABLE IF EXISTS `fs_member_relation`;
CREATE TABLE `fs_member_relation` (
  `id` varchar(64) NOT NULL,
  `type` varchar(32) NOT NULL DEFAULT '',
  `aid` int(11) NOT NULL DEFAULT '0',
  `bid` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of fs_member_relation
-- ----------------------------
BEGIN;
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_1', 'role_menu', 1, 1);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_10', 'role_menu', 1, 10);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_12', 'role_menu', 1, 12);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_13', 'role_menu', 1, 13);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_14', 'role_menu', 1, 14);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_15', 'role_menu', 1, 15);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_16', 'role_menu', 1, 16);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_17', 'role_menu', 1, 17);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_18', 'role_menu', 1, 18);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_19', 'role_menu', 1, 19);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_2', 'role_menu', 1, 2);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_20', 'role_menu', 1, 20);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_21', 'role_menu', 1, 21);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_22', 'role_menu', 1, 22);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_23', 'role_menu', 1, 23);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_3', 'role_menu', 1, 3);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_4', 'role_menu', 1, 4);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_5', 'role_menu', 1, 5);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_6', 'role_menu', 1, 6);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_7', 'role_menu', 1, 7);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_8', 'role_menu', 1, 8);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_9', 'role_menu', 1, 9);
INSERT INTO `fs_member_relation` VALUES ('role_menu_2_1', 'role_menu', 2, 1);
INSERT INTO `fs_member_relation` VALUES ('role_menu_2_2', 'role_menu', 2, 2);
INSERT INTO `fs_member_relation` VALUES ('role_menu_2_3', 'role_menu', 2, 3);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_1', 'role_resource', 1, 1);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_10', 'role_resource', 1, 10);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_11', 'role_resource', 1, 11);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_12', 'role_resource', 1, 12);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_13', 'role_resource', 1, 13);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_14', 'role_resource', 1, 14);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_15', 'role_resource', 1, 15);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_16', 'role_resource', 1, 16);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_17', 'role_resource', 1, 17);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_18', 'role_resource', 1, 18);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_19', 'role_resource', 1, 19);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_2', 'role_resource', 1, 2);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_20', 'role_resource', 1, 20);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_21', 'role_resource', 1, 21);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_22', 'role_resource', 1, 22);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_23', 'role_resource', 1, 23);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_24', 'role_resource', 1, 24);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_25', 'role_resource', 1, 25);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_26', 'role_resource', 1, 26);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_27', 'role_resource', 1, 27);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_28', 'role_resource', 1, 28);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_29', 'role_resource', 1, 29);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_3', 'role_resource', 1, 3);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_30', 'role_resource', 1, 30);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_31', 'role_resource', 1, 31);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_32', 'role_resource', 1, 32);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_33', 'role_resource', 1, 33);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_34', 'role_resource', 1, 34);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_35', 'role_resource', 1, 35);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_36', 'role_resource', 1, 36);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_37', 'role_resource', 1, 37);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_38', 'role_resource', 1, 38);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_39', 'role_resource', 1, 39);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_4', 'role_resource', 1, 4);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_40', 'role_resource', 1, 40);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_41', 'role_resource', 1, 41);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_42', 'role_resource', 1, 42);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_43', 'role_resource', 1, 43);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_44', 'role_resource', 1, 44);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_45', 'role_resource', 1, 45);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_46', 'role_resource', 1, 46);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_47', 'role_resource', 1, 47);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_48', 'role_resource', 1, 48);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_49', 'role_resource', 1, 49);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_5', 'role_resource', 1, 5);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_50', 'role_resource', 1, 50);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_51', 'role_resource', 1, 51);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_52', 'role_resource', 1, 52);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_53', 'role_resource', 1, 53);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_54', 'role_resource', 1, 54);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_55', 'role_resource', 1, 55);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_56', 'role_resource', 1, 56);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_57', 'role_resource', 1, 57);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_58', 'role_resource', 1, 58);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_6', 'role_resource', 1, 6);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_7', 'role_resource', 1, 7);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_8', 'role_resource', 1, 8);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_9', 'role_resource', 1, 9);
INSERT INTO `fs_member_relation` VALUES ('role_resource_2_1', 'role_resource', 2, 1);
INSERT INTO `fs_member_relation` VALUES ('role_resource_2_2', 'role_resource', 2, 2);
INSERT INTO `fs_member_relation` VALUES ('role_resource_2_3', 'role_resource', 2, 3);
INSERT INTO `fs_member_relation` VALUES ('role_resource_2_4', 'role_resource', 2, 4);
INSERT INTO `fs_member_relation` VALUES ('role_resource_2_5', 'role_resource', 2, 5);
INSERT INTO `fs_member_relation` VALUES ('role_resource_2_6', 'role_resource', 2, 6);
INSERT INTO `fs_member_relation` VALUES ('user_role_1_1', 'user_role', 1, 1);
INSERT INTO `fs_member_relation` VALUES ('user_role_2_2', 'user_role', 2, 2);
COMMIT;

-- ----------------------------
-- Table structure for fs_member_resource
-- ----------------------------
DROP TABLE IF EXISTS `fs_member_resource`;
CREATE TABLE `fs_member_resource` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(32) NOT NULL DEFAULT '',
  `full_name` varchar(255) NOT NULL DEFAULT '',
  `parent_id` int(11) NOT NULL DEFAULT '0',
  `module` varchar(64) NOT NULL DEFAULT '',
  `controller` varchar(64) NOT NULL DEFAULT '',
  `action` varchar(64) NOT NULL DEFAULT '',
  `sort` tinyint(4) NOT NULL DEFAULT '0',
  `status` tinyint(4) NOT NULL DEFAULT '0',
  `description` text NOT NULL,
  `created_time` bigint(20) NOT NULL DEFAULT '0',
  `created_uid` int(11) NOT NULL DEFAULT '0',
  `updated_time` bigint(20) NOT NULL DEFAULT '0',
  `updated_uid` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=59 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of fs_member_resource
-- ----------------------------
BEGIN;
INSERT INTO `fs_member_resource` VALUES (1, '后台', '后台', 0, 'admin', '', '', 0, 1, '', 1528081670164, 1, 1528081742458, 1);
INSERT INTO `fs_member_resource` VALUES (2, '后台-菜单', '后台-菜单', 1, 'member', 'menu', '', 0, 1, '', 1528081696180, 1, 1528082280902, 1);
INSERT INTO `fs_member_resource` VALUES (3, '后台-资源', '后台-资源', 1, 'member', 'resource', '', 0, 1, '', 1528081721468, 1, 1528082287209, 1);
INSERT INTO `fs_member_resource` VALUES (4, '后台-角色', '后台-角色', 1, 'member', 'role', '', 0, 1, '', 1528081764195, 1, 1528082294533, 1);
INSERT INTO `fs_member_resource` VALUES (5, '后台-用户', '后台-用户', 1, 'member', 'user', '', 0, 1, '', 1528081790426, 1, 1528082300610, 1);
INSERT INTO `fs_member_resource` VALUES (6, '后台-配置', '后台-配置', 1, 'admin', 'setting', '', 0, 1, '', 1528081858994, 1, 1528082308047, 1);
INSERT INTO `fs_member_resource` VALUES (7, '后台-菜单-添加', '后台-菜单-添加', 2, 'member', 'menu', 'add', 0, 1, '', 1528081911866, 1, 1528082318631, 1);
INSERT INTO `fs_member_resource` VALUES (8, '后台-菜单-修改', '后台-菜单-修改', 2, 'member', 'menu', 'modify', 0, 1, '', 1528081955009, 1, 1528082337498, 1);
INSERT INTO `fs_member_resource` VALUES (9, '后台-菜单-删除', '后台-菜单-删除', 2, 'member', 'menu', 'delete', 0, 1, '', 1528081985121, 1, 1528082346365, 1);
INSERT INTO `fs_member_resource` VALUES (10, '后台-资源-添加', '后台-资源-添加', 3, 'member', 'resource', 'add', 0, 1, '', 1528082024771, 1, 1528082355791, 1);
INSERT INTO `fs_member_resource` VALUES (11, '后台-资源-修改', '后台-资源-修改', 3, 'member', 'resource', 'modify', 0, 1, '', 1528082042903, 1, 1528082364006, 1);
INSERT INTO `fs_member_resource` VALUES (12, '后台-资源-删除', '后台-资源-删除', 3, 'member', 'resource', 'delete', 0, 1, '', 1528082084243, 1, 1528082374750, 1);
INSERT INTO `fs_member_resource` VALUES (13, '后台-角色-添加', '后台-角色-添加', 4, 'member', 'role', 'add', 0, 1, '', 1528082104404, 1, 1528082387074, 1);
INSERT INTO `fs_member_resource` VALUES (14, '后台-角色-修改', '后台-角色-修改', 4, 'member', 'role', 'modify', 0, 1, '', 1528082198733, 1, 1528082396288, 1);
INSERT INTO `fs_member_resource` VALUES (15, '后台-角色-删除', '后台-角色-删除', 4, 'member', 'role', 'delete', 0, 1, '', 1528082214265, 1, 1528082406423, 1);
INSERT INTO `fs_member_resource` VALUES (16, '后台-角色-菜单', '后台-角色-菜单', 4, 'member', 'role', 'menu', 0, 1, '', 1528082271647, 1, 1528082271647, 1);
INSERT INTO `fs_member_resource` VALUES (17, '后台-角色-资源', '后台-角色-资源', 4, 'member', 'role', 'resource', 0, 1, '', 1528082432853, 1, 1528082432853, 1);
INSERT INTO `fs_member_resource` VALUES (18, '后台-用户-添加', '后台-用户-添加', 5, 'member', 'user', 'add', 0, 1, '', 1528082465188, 1, 1528082465188, 1);
INSERT INTO `fs_member_resource` VALUES (19, '后台-用户-修改', '后台-用户-修改', 5, 'member', 'user', 'modify', 0, 1, '', 1528082492740, 1, 1528082492740, 1);
INSERT INTO `fs_member_resource` VALUES (20, '后台-用户-删除', '后台-用户-删除', 5, 'member', 'user', 'delete', 0, 1, '', 1528082514205, 1, 1528082514205, 1);
INSERT INTO `fs_member_resource` VALUES (21, '后台-用户-密码', '后台-用户-密码', 5, 'member', 'user', 'password', 0, 1, '', 1528082586393, 1, 1528082586393, 1);
INSERT INTO `fs_member_resource` VALUES (22, '后台-配置-添加', '后台-配置-添加', 6, 'admin', 'setting', 'add', 0, 1, '', 1528082616922, 1, 1528082616922, 1);
INSERT INTO `fs_member_resource` VALUES (23, '后台-配置-修改', '后台-配置-修改', 6, 'admin', 'setting', 'modify', 0, 1, '', 1528082642509, 1, 1528082642509, 1);
INSERT INTO `fs_member_resource` VALUES (24, '后台-配置-删除', '后台-配置-删除', 6, 'admin', 'setting', 'delete', 0, 1, '', 1528082662922, 1, 1528082662922, 1);
INSERT INTO `fs_member_resource` VALUES (25, '后台-流程', '后台-流程', 1, 'admin', 'flow', '', 0, 1, '', 1530087567492, 1, 1530099520508, 1);
INSERT INTO `fs_member_resource` VALUES (26, '后台-流程-添加', '后台-流程-添加', 25, 'admin', 'flow', 'add', 0, 1, '', 1530087595846, 1, 1530099527498, 1);
INSERT INTO `fs_member_resource` VALUES (27, '后台-流程-修改', '后台-流程-修改', 25, 'admin', 'flow', 'modify', 0, 1, '', 1530087616201, 1, 1530099532439, 1);
INSERT INTO `fs_member_resource` VALUES (28, '后台-流程-删除', '后台-流程-删除', 25, 'admin', 'flow', 'delete', 0, 1, '', 1530087635982, 1, 1530099538065, 1);
INSERT INTO `fs_member_resource` VALUES (29, '后台-流程-绘制', '后台-流程-绘制', 25, 'admin', 'flow', 'draw', 0, 1, '', 1530099561021, 1, 1530099561021, 1);
INSERT INTO `fs_member_resource` VALUES (30, '后台-流程节点', '后台-流程节点', 1, 'admin', 'flowNode', '', 0, 1, '', 1530502842330, 1, 1530502842330, 1);
INSERT INTO `fs_member_resource` VALUES (31, '后台-流程节点-添加', '后台-流程节点-添加', 30, 'admin', 'flowNode', 'add', 0, 1, '', 1530502873692, 1, 1530502873692, 1);
INSERT INTO `fs_member_resource` VALUES (32, '后台-流程节点-修改', '后台-流程节点-修改', 30, 'admin', 'flowNode', 'modify', 0, 1, '', 1530502892108, 1, 1530502935803, 1);
INSERT INTO `fs_member_resource` VALUES (33, '后台-流程节点-删除', '后台-流程节点-删除', 30, 'admin', 'flowNode', 'delete', 0, 1, '', 1530502920654, 1, 1530502920654, 1);
INSERT INTO `fs_member_resource` VALUES (34, '后台-流程插件', '后台-流程插件', 1, 'admin', 'flowPlugin', '', 0, 1, '', 1531277340702, 1, 1531277848905, 1);
INSERT INTO `fs_member_resource` VALUES (35, '后台-流程插件-添加', '后台-流程插件-添加', 34, 'admin', 'flowPlugin', 'add', 0, 1, '', 1531277388613, 1, 1531277855941, 1);
INSERT INTO `fs_member_resource` VALUES (36, '后台-流程插件-修改', '后台-流程插件-修改', 34, 'admin', 'flowPlugin', 'modify', 0, 1, '', 1531277429354, 1, 1531277429354, 1);
INSERT INTO `fs_member_resource` VALUES (37, '后台-流程插件-删除', '后台-流程插件-删除', 34, 'admin', 'flowPlugin', 'delete', 0, 1, '', 1531277455896, 1, 1531277455896, 1);
INSERT INTO `fs_member_resource` VALUES (38, '后台-作业调度', '后台-作业调度', 1, 'admin', 'cron', '', 0, 1, '', 1531382454935, 1, 1531382454935, 1);
INSERT INTO `fs_member_resource` VALUES (39, '后台-作业调度-添加', '后台-作业调度-添加', 38, 'admin', 'cron', 'add', 0, 1, '', 1531382530119, 1, 1531382947340, 1);
INSERT INTO `fs_member_resource` VALUES (40, '后台-作业调度-修改', '后台-作业调度-修改', 38, 'admin', 'cron', 'modify', 0, 1, '', 1531382557812, 1, 1531382985048, 1);
INSERT INTO `fs_member_resource` VALUES (41, '后台-作业调度-暂停', '后台-作业调度-暂停', 38, 'admin', 'cron', 'pause', 0, 1, '', 1531382585089, 1, 1531383003084, 1);
INSERT INTO `fs_member_resource` VALUES (42, '后台-作业调度-恢复', '后台-作业调度-恢复', 38, 'admin', 'cron', 'resume', 0, 1, '', 1531382626457, 1, 1531383034588, 1);
INSERT INTO `fs_member_resource` VALUES (43, '后台-作业调度-手动执行', '后台-作业调度-手动执行', 38, 'admin', 'cron', 'run', 0, 1, '', 1531388667272, 1, 1531476339629, 1);
INSERT INTO `fs_member_resource` VALUES (44, '后台-作业调度-删除', '后台-作业调度-删除', 38, 'admin', 'cron', 'delete', 0, 1, '', 1531476368990, 1, 1531476368990, 1);
INSERT INTO `fs_member_resource` VALUES (45, '后台-作业调度-开始调度', '后台-作业调度-开始调度', 38, 'admin', 'cron', 'start', 0, 1, '', 1531991548644, 1, 1531991548644, 1);
INSERT INTO `fs_member_resource` VALUES (46, '后台-作业调度-暂停调度', '后台-作业调度-暂停调度', 38, 'admin', 'cron', 'standby', 0, 1, '', 1531991578666, 1, 1531991578666, 1);
INSERT INTO `fs_member_resource` VALUES (47, '后台-分析模型', '后台-分析模型', 1, 'admin', 'analysis', '', 0, 1, '', 1544493315103, 1, 1544493338803, 1);
INSERT INTO `fs_member_resource` VALUES (48, '后台-分析模型-添加', '后台-分析模型-添加', 47, 'admin', 'analysis', 'add', 0, 1, '', 1544493361087, 1, 1544493361087, 1);
INSERT INTO `fs_member_resource` VALUES (49, '后台-分析模型-修改', '后台-分析模型-修改', 47, 'admin', 'analysis', 'modify', 0, 1, '', 1544493381218, 1, 1544493381218, 1);
INSERT INTO `fs_member_resource` VALUES (50, '后台-分析模型-删除', '后台-分析模型-删除', 47, 'admin', 'analysis', 'delete', 0, 1, '', 1544493395775, 1, 1544493395775, 1);
INSERT INTO `fs_member_resource` VALUES (51, '后台-数据仓库', '后台-数据仓库', 1, 'admin', 'analysisNode', '', 0, 1, '', 1544503360733, 1, 1544503360733, 1);
INSERT INTO `fs_member_resource` VALUES (52, '后台-数据仓库-添加', '后台-数据仓库-添加', 51, 'admin', 'analysisNode', 'add', 0, 1, '', 1544503379427, 1, 1544503379427, 1);
INSERT INTO `fs_member_resource` VALUES (53, '后台-数据仓库-修改', '后台-数据仓库-修改', 51, 'admin', 'analysisNode', 'modify', 0, 1, '', 1544503397055, 1, 1544503397055, 1);
INSERT INTO `fs_member_resource` VALUES (54, '后台-数据仓库-删除', '后台-数据仓库-删除', 51, 'admin', 'analysisNode', 'delete', 0, 1, '', 1544503415914, 1, 1544503415914, 1);
INSERT INTO `fs_member_resource` VALUES (55, '后台-爬虫模板', '后台-爬虫模板', 1, 'admin', 'spiderTemplate', '', 0, 1, '', 1557819021026, 1, 1557819021026, 1);
INSERT INTO `fs_member_resource` VALUES (56, '后台-爬虫模板-添加', '后台-爬虫模板-添加', 55, 'admin', 'spiderTemplate', 'add', 0, 1, '', 1557819047160, 1, 1557819047160, 1);
INSERT INTO `fs_member_resource` VALUES (57, '后台-爬虫模板-修改', '后台-爬虫模板-修改', 55, 'admin', 'spiderTemplate', 'modify', 0, 1, '', 1557819067213, 1, 1557819067213, 1);
INSERT INTO `fs_member_resource` VALUES (58, '后台-爬虫模板-删除', '后台-爬虫模板-删除', 55, 'admin', 'spiderTemplate', 'delete', 0, 1, '', 1557819081357, 1, 1557819081357, 1);
COMMIT;

-- ----------------------------
-- Table structure for fs_member_role
-- ----------------------------
DROP TABLE IF EXISTS `fs_member_role`;
CREATE TABLE `fs_member_role` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(32) NOT NULL DEFAULT '',
  `sort` tinyint(4) NOT NULL DEFAULT '0',
  `status` tinyint(4) NOT NULL DEFAULT '0',
  `description` text NOT NULL,
  `created_time` bigint(20) NOT NULL DEFAULT '0',
  `created_uid` int(11) NOT NULL DEFAULT '0',
  `updated_time` bigint(20) NOT NULL DEFAULT '0',
  `updated_uid` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of fs_member_role
-- ----------------------------
BEGIN;
INSERT INTO `fs_member_role` VALUES (1, '后台管理', 0, 1, '', 1528081589495, 1, 1528266877684, 1);
INSERT INTO `fs_member_role` VALUES (2, '普通用户', 0, 1, '', 1528081606670, 1, 1528081606670, 1);
COMMIT;

-- ----------------------------
-- Table structure for fs_member_setting
-- ----------------------------
DROP TABLE IF EXISTS `fs_member_setting`;
CREATE TABLE `fs_member_setting` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL DEFAULT '',
  `type` varchar(64) NOT NULL DEFAULT '',
  `content` text NOT NULL,
  `sort` tinyint(4) NOT NULL DEFAULT '0',
  `description` text NOT NULL,
  `updated_time` bigint(20) NOT NULL DEFAULT '0',
  `updated_uid` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of fs_member_setting
-- ----------------------------
BEGIN;
INSERT INTO `fs_member_setting` VALUES (1, 'default-password', 'member', 'password', 1, '', 1527731577113, 1);
INSERT INTO `fs_member_setting` VALUES (3, 'menu-parent-id', 'admin', '1', 1, '管理后台菜单根节点ID', 1532401344424, 1);
COMMIT;

-- ----------------------------
-- Table structure for fs_member_user
-- ----------------------------
DROP TABLE IF EXISTS `fs_member_user`;
CREATE TABLE `fs_member_user` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `serial` varchar(32) NOT NULL DEFAULT '',
  `name` varchar(32) NOT NULL DEFAULT '',
  `password` char(32) NOT NULL DEFAULT '',
  `salt` char(4) NOT NULL DEFAULT '',
  `sort` tinyint(4) NOT NULL DEFAULT '0',
  `status` tinyint(4) NOT NULL DEFAULT '0',
  `description` text NOT NULL,
  `created_time` bigint(20) NOT NULL DEFAULT '0',
  `created_ip` varchar(16) NOT NULL DEFAULT '',
  `created_uid` int(11) NOT NULL DEFAULT '0',
  `updated_time` bigint(20) NOT NULL DEFAULT '0',
  `updated_uid` int(11) NOT NULL DEFAULT '0',
  `logined_time` bigint(20) NOT NULL DEFAULT '0',
  `logined_ip` varchar(16) NOT NULL DEFAULT '',
  `locked_time` bigint(20) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `unq_serial` (`serial`),
  KEY `unq_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of fs_member_user
-- ----------------------------
BEGIN;
INSERT INTO `fs_member_user` VALUES (1, 'admin', '管理员', 'fc7911b5108d30e087f8881b90368679', '5231', 0, 1, '', 1528081552985, '127.0.0.1', 1, 1528081552985, 1, 1584177449798, '127.0.0.1', 0);
INSERT INTO `fs_member_user` VALUES (2, 'test', '测试123', '4b361be828611add84453a24f39772a5', '0905', 0, 1, '', 1528081567988, '127.0.0.1', 1, 1542958281919, 1, 1528267171953, '127.0.0.1', 0);
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
