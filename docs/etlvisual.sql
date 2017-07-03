/*
Navicat MySQL Data Transfer

Source Server         : localhost_3306
Source Server Version : 50624
Source Host           : localhost:3306
Source Database       : etlvisual

Target Server Type    : MYSQL
Target Server Version : 50624
File Encoding         : 65001

Date: 2017-07-03 09:12:46
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for t_menu
-- ----------------------------
DROP TABLE IF EXISTS `t_menu`;
CREATE TABLE `t_menu` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL DEFAULT '' COMMENT '名称',
  `parent_id` int(11) NOT NULL DEFAULT '0' COMMENT '父级',
  `module` varchar(255) NOT NULL DEFAULT '' COMMENT '模块',
  `url` varchar(255) NOT NULL DEFAULT '' COMMENT '链接地址',
  `target` varchar(255) NOT NULL DEFAULT '' COMMENT '打开方式',
  `icon` varchar(255) NOT NULL DEFAULT '' COMMENT '图标',
  `state` varchar(255) NOT NULL DEFAULT '' COMMENT '展开状态',
  `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '状态',
  `sort` int(11) NOT NULL DEFAULT '0' COMMENT '排序',
  `create_uid` int(11) NOT NULL DEFAULT '0' COMMENT '创建者',
  `create_time` bigint(20) NOT NULL DEFAULT '0' COMMENT '创建时间',
  `update_uid` int(11) NOT NULL DEFAULT '0' COMMENT '修改者',
  `update_time` bigint(20) NOT NULL DEFAULT '0' COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8 COMMENT='菜单信息表';

-- ----------------------------
-- Records of t_menu
-- ----------------------------
INSERT INTO `t_menu` VALUES ('1', '系统管理', '0', 'backend', '', '', 'fa fa-cogs', '', '1', '0', '0', '0', '0', '0');
INSERT INTO `t_menu` VALUES ('2', '用户管理', '1', 'backend', '', '', 'glyphicon glyphicon-user', '', '1', '0', '0', '0', '0', '0');
INSERT INTO `t_menu` VALUES ('3', '角色管理', '1', 'backend', '/role/index/', '', 'glyphicon glyphicon-road', '', '1', '0', '0', '0', '0', '0');
INSERT INTO `t_menu` VALUES ('4', '菜单管理', '1', 'backend', '', '', 'glyphicon glyphicon-indent-left', '', '1', '0', '0', '0', '0', '0');
INSERT INTO `t_menu` VALUES ('5', '资源管理', '1', 'backend', '', '', 'glyphicon glyphicon-inbox', '', '1', '0', '0', '0', '0', '0');
INSERT INTO `t_menu` VALUES ('6', '参数设置', '1', 'backend', '/setting/index/', '', 'fa fa-globe', '', '1', '0', '0', '0', '0', '0');
INSERT INTO `t_menu` VALUES ('7', '添加用户', '2', 'backend', '/user/edit/', '', '', '', '1', '0', '0', '0', '0', '0');
INSERT INTO `t_menu` VALUES ('8', '用户列表', '2', 'backend', '/user/index/', '', '', '', '1', '0', '0', '0', '0', '0');

-- ----------------------------
-- Table structure for t_relation
-- ----------------------------
DROP TABLE IF EXISTS `t_relation`;
CREATE TABLE `t_relation` (
  `type` varchar(255) NOT NULL DEFAULT '' COMMENT '类型',
  `aid` int(11) NOT NULL DEFAULT '0' COMMENT '主键',
  `bid` int(11) NOT NULL DEFAULT '0' COMMENT '主键',
  PRIMARY KEY (`type`,`aid`,`bid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='关联信息表';

-- ----------------------------
-- Records of t_relation
-- ----------------------------

-- ----------------------------
-- Table structure for t_resource
-- ----------------------------
DROP TABLE IF EXISTS `t_resource`;
CREATE TABLE `t_resource` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(255) NOT NULL DEFAULT '' COMMENT '名称',
  `parent_id` int(11) NOT NULL DEFAULT '0' COMMENT '父级',
  `module` varchar(255) NOT NULL DEFAULT '' COMMENT '模块',
  `controller` varchar(255) NOT NULL DEFAULT '' COMMENT '控制器',
  `action` varchar(255) NOT NULL DEFAULT '' COMMENT '活动',
  `operation` varchar(255) NOT NULL DEFAULT '' COMMENT '操作',
  `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '状态',
  `sort` int(11) NOT NULL DEFAULT '0' COMMENT '排序',
  `create_uid` int(11) NOT NULL DEFAULT '0' COMMENT '创建者',
  `create_time` bigint(20) NOT NULL DEFAULT '0' COMMENT '创建时间',
  `update_uid` int(11) NOT NULL DEFAULT '0' COMMENT '修改者',
  `update_time` bigint(20) NOT NULL DEFAULT '0' COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='资源信息表';

-- ----------------------------
-- Records of t_resource
-- ----------------------------

-- ----------------------------
-- Table structure for t_role
-- ----------------------------
DROP TABLE IF EXISTS `t_role`;
CREATE TABLE `t_role` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(255) NOT NULL DEFAULT '' COMMENT '名称',
  `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '状态',
  `sort` int(11) NOT NULL DEFAULT '0' COMMENT '排序',
  `description` text COMMENT '描述',
  `create_uid` int(11) NOT NULL DEFAULT '0' COMMENT '创建者',
  `create_time` bigint(20) NOT NULL DEFAULT '0' COMMENT '创建时间',
  `update_uid` int(11) NOT NULL DEFAULT '0' COMMENT '修改者',
  `update_time` bigint(20) NOT NULL DEFAULT '0' COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8 COMMENT='角色信息表';

-- ----------------------------
-- Records of t_role
-- ----------------------------
INSERT INTO `t_role` VALUES ('1', '系统管理员', '1', '1', '', '0', '1499044327196', '0', '1499044327196');

-- ----------------------------
-- Table structure for t_setting
-- ----------------------------
DROP TABLE IF EXISTS `t_setting`;
CREATE TABLE `t_setting` (
  `type` varchar(255) NOT NULL DEFAULT '' COMMENT '类型',
  `parameter` varchar(255) NOT NULL COMMENT '参数名',
  `name` varchar(255) NOT NULL DEFAULT '' COMMENT '名称',
  `content` text NOT NULL COMMENT '参数值',
  `sort` int(11) NOT NULL DEFAULT '0' COMMENT '排序',
  `description` text COMMENT '描述',
  `update_uid` int(11) NOT NULL DEFAULT '0' COMMENT '修改者',
  `update_time` bigint(20) NOT NULL DEFAULT '0' COMMENT '修改时间',
  PRIMARY KEY (`type`,`parameter`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='配置信息表';

-- ----------------------------
-- Records of t_setting
-- ----------------------------
INSERT INTO `t_setting` VALUES ('system', 'siteName', '系统名称', '数据报表调度平台', '0', '', '0', '0');

-- ----------------------------
-- Table structure for t_user
-- ----------------------------
DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(255) NOT NULL DEFAULT '' COMMENT '昵称',
  `username` varchar(255) NOT NULL DEFAULT '' COMMENT '账号',
  `password` varchar(64) NOT NULL DEFAULT '' COMMENT '密码',
  `salt` char(6) NOT NULL DEFAULT '' COMMENT '混淆码',
  `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '状态',
  `sort` int(11) NOT NULL DEFAULT '0' COMMENT '排序',
  `description` text COMMENT '描述',
  `create_uid` int(11) NOT NULL DEFAULT '0' COMMENT '创建者',
  `create_time` bigint(20) NOT NULL DEFAULT '0' COMMENT '创建时间',
  `update_uid` int(11) NOT NULL DEFAULT '0' COMMENT '修改者',
  `update_time` bigint(20) NOT NULL DEFAULT '0' COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='用户信息表';

-- ----------------------------
-- Records of t_user
-- ----------------------------
