/*
Navicat MySQL Data Transfer

Source Server         : localhost_3306
Source Server Version : 50624
Source Host           : localhost:3306
Source Database       : etlvisual

Target Server Type    : MYSQL
Target Server Version : 50624
File Encoding         : 65001

Date: 2017-07-05 16:13:27
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for t_flow
-- ----------------------------
DROP TABLE IF EXISTS `t_flow`;
CREATE TABLE `t_flow` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(255) NOT NULL DEFAULT '' COMMENT '流程图',
  `content` text NOT NULL COMMENT '流程图内容',
  `sort` int(11) NOT NULL DEFAULT '0' COMMENT '排序',
  `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '状态',
  `description` tinytext NOT NULL COMMENT '描述',
  `create_uid` int(11) NOT NULL DEFAULT '0' COMMENT '创建者',
  `create_time` bigint(20) NOT NULL DEFAULT '0' COMMENT '创建时间',
  `update_uid` int(11) NOT NULL DEFAULT '0' COMMENT '修改者',
  `update_time` bigint(20) NOT NULL DEFAULT '0' COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8 COMMENT='流程图信息';

-- ----------------------------
-- Records of t_flow
-- ----------------------------
INSERT INTO `t_flow` VALUES ('1', '测试', '{\"width\":700,\"height\":500,\"nodes\":{\"flowChartItem0\":{\"id\":\"flowChartItem0\",\"index\":0,\"top\":86,\"left\":79,\"width\":109,\"height\":24,\"parent\":\"com.iisquare.etl.spark.plugins.core.ReadMySQLNode\",\"property\":[{\"name\":\"节点\",\"key\":\"node\",\"value\":\"flowChartItem0\",\"group\":\"基础信息\"},{\"name\":\"别名\",\"key\":\"alias\",\"value\":\"member\",\"group\":\"基础信息\",\"editor\":\"text\"},{\"name\":\"名称\",\"key\":\"text\",\"value\":\"读取member表\",\"group\":\"基础信息\",\"editor\":\"text\"},{\"name\":\"备注\",\"key\":\"description\",\"value\":\"\",\"group\":\"基础信息\",\"editor\":\"textarea\"},{\"name\":\"主机\",\"key\":\"host\",\"value\":\"10.207.9.216\",\"group\":\"连接设置\",\"editor\":\"text\"},{\"name\":\"端口\",\"key\":\"port\",\"value\":\"3306\",\"group\":\"连接设置\",\"editor\":\"text\"},{\"name\":\"账号\",\"key\":\"username\",\"value\":\"root\",\"group\":\"连接设置\",\"editor\":\"text\"},{\"name\":\"密码\",\"key\":\"password\",\"value\":\"admin888\",\"group\":\"连接设置\",\"editor\":\"text\"},{\"name\":\"数据库\",\"key\":\"database\",\"value\":\"test\",\"group\":\"连接设置\",\"editor\":\"text\"},{\"name\":\"编码\",\"key\":\"charset\",\"value\":\"utf-8\",\"group\":\"连接设置\",\"editor\":\"text\"},{\"name\":\"分区数量\",\"key\":\"numPartitions\",\"value\":\"1\",\"group\":\"分布式\",\"editor\":\"numberbox\"},{\"name\":\"读取下限\",\"key\":\"lowerBound\",\"value\":\"0\",\"group\":\"分布式\",\"editor\":\"numberbox\"},{\"name\":\"读取上限\",\"key\":\"upperBound\",\"value\":\"2147483647\",\"group\":\"分布式\",\"editor\":\"numberbox\"},{\"name\":\"SQL\",\"key\":\"sql\",\"value\":\"select * from member where ?<=userID and userID <= ?\",\"group\":\"数据查询\",\"editor\":\"textarea\"}]},\"flowChartItem1\":{\"id\":\"flowChartItem1\",\"index\":1,\"top\":308,\"left\":58,\"width\":132,\"height\":24,\"parent\":\"com.iisquare.etl.spark.plugins.core.ReadMySQLNode\",\"property\":[{\"name\":\"节点\",\"key\":\"node\",\"value\":\"flowChartItem1\",\"group\":\"基础信息\"},{\"name\":\"别名\",\"key\":\"alias\",\"value\":\"membertype\",\"group\":\"基础信息\",\"editor\":\"text\"},{\"name\":\"名称\",\"key\":\"text\",\"value\":\"读取membertype表\",\"group\":\"基础信息\",\"editor\":\"text\"},{\"name\":\"备注\",\"key\":\"description\",\"value\":\"\",\"group\":\"基础信息\",\"editor\":\"textarea\"},{\"name\":\"主机\",\"key\":\"host\",\"value\":\"10.207.9.216\",\"group\":\"连接设置\",\"editor\":\"text\"},{\"name\":\"端口\",\"key\":\"port\",\"value\":\"3306\",\"group\":\"连接设置\",\"editor\":\"text\"},{\"name\":\"账号\",\"key\":\"username\",\"value\":\"root\",\"group\":\"连接设置\",\"editor\":\"text\"},{\"name\":\"密码\",\"key\":\"password\",\"value\":\"root\",\"group\":\"连接设置\",\"editor\":\"text\"},{\"name\":\"数据库\",\"key\":\"database\",\"value\":\"test\",\"group\":\"连接设置\",\"editor\":\"text\"},{\"name\":\"编码\",\"key\":\"charset\",\"value\":\"utf-8\",\"group\":\"连接设置\",\"editor\":\"text\"},{\"name\":\"分区数量\",\"key\":\"numPartitions\",\"value\":\"1\",\"group\":\"分布式\",\"editor\":\"numberbox\"},{\"name\":\"读取下限\",\"key\":\"lowerBound\",\"value\":\"0\",\"group\":\"分布式\",\"editor\":\"numberbox\"},{\"name\":\"读取上限\",\"key\":\"upperBound\",\"value\":\"2147483647\",\"group\":\"分布式\",\"editor\":\"numberbox\"},{\"name\":\"SQL\",\"key\":\"sql\",\"value\":\"select * from membertype where ?<=typeNO and typeNO <= ?\",\"group\":\"数据查询\",\"editor\":\"textarea\"}]},\"flowChartItem2\":{\"id\":\"flowChartItem2\",\"index\":2,\"top\":201,\"left\":258,\"width\":89,\"height\":24,\"parent\":\"com.iisquare.etl.spark.plugins.core.CalculateSQLNode\",\"property\":[{\"name\":\"节点\",\"key\":\"node\",\"value\":\"flowChartItem2\",\"group\":\"基础信息\"},{\"name\":\"别名\",\"key\":\"alias\",\"value\":\"\",\"group\":\"基础信息\",\"editor\":\"text\"},{\"name\":\"名称\",\"key\":\"text\",\"value\":\"SQL查询[2] \",\"group\":\"基础信息\",\"editor\":\"text\"},{\"name\":\"备注\",\"key\":\"description\",\"value\":\"\",\"group\":\"基础信息\",\"editor\":\"textarea\"},{\"name\":\"SQL\",\"key\":\"sql\",\"value\":\"select userID,discount from member,membertype where member.memberType=membertype.typeName\",\"group\":\"数据查询\",\"editor\":\"textarea\"}]},\"flowChartItem3\":{\"id\":\"flowChartItem3\",\"index\":3,\"top\":204,\"left\":517,\"width\":111,\"height\":24,\"parent\":\"com.iisquare.etl.spark.plugins.core.WriteConsoleNode\",\"property\":[{\"name\":\"节点\",\"key\":\"node\",\"value\":\"flowChartItem3\",\"group\":\"基础信息\"},{\"name\":\"别名\",\"key\":\"alias\",\"value\":\"\",\"group\":\"基础信息\",\"editor\":\"text\"},{\"name\":\"名称\",\"key\":\"text\",\"value\":\"Console打印[3] \",\"group\":\"基础信息\",\"editor\":\"text\"},{\"name\":\"备注\",\"key\":\"description\",\"value\":\"\",\"group\":\"基础信息\",\"editor\":\"textarea\"}]}},\"connections\":[{\"sourceId\":\"flowChartItem0\",\"targetId\":\"flowChartItem2\",\"sourceAnchor\":\"RightMiddle\",\"targetAnchor\":\"TopCenter\"},{\"sourceId\":\"flowChartItem1\",\"targetId\":\"flowChartItem2\",\"sourceAnchor\":\"RightMiddle\",\"targetAnchor\":\"BottomCenter\"},{\"sourceId\":\"flowChartItem2\",\"targetId\":\"flowChartItem3\",\"sourceAnchor\":\"RightMiddle\",\"targetAnchor\":\"LeftMiddle\"}]}', '1', '1', '', '1', '1499239563418', '1', '1499242355815');

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
  `pattern` tinytext NOT NULL COMMENT '通配符',
  `target` varchar(255) NOT NULL DEFAULT '' COMMENT '打开方式',
  `icon` varchar(255) NOT NULL DEFAULT '' COMMENT '图标',
  `state` varchar(255) NOT NULL DEFAULT '' COMMENT '展开状态',
  `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '状态',
  `sort` int(11) NOT NULL DEFAULT '0' COMMENT '排序',
  `description` tinytext NOT NULL COMMENT '描述',
  `create_uid` int(11) NOT NULL DEFAULT '0' COMMENT '创建者',
  `create_time` bigint(20) NOT NULL DEFAULT '0' COMMENT '创建时间',
  `update_uid` int(11) NOT NULL DEFAULT '0' COMMENT '修改者',
  `update_time` bigint(20) NOT NULL DEFAULT '0' COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8 COMMENT='菜单信息表';

-- ----------------------------
-- Records of t_menu
-- ----------------------------
INSERT INTO `t_menu` VALUES ('1', '系统管理', '0', 'backend', '', '', '', 'fa fa-cogs', '', '1', '90', '', '0', '0', '1', '1499226980981');
INSERT INTO `t_menu` VALUES ('2', '用户管理', '1', 'backend', '', '', '', 'glyphicon glyphicon-user', '', '1', '0', '', '0', '0', '0', '0');
INSERT INTO `t_menu` VALUES ('3', '角色管理', '1', 'backend', '/role/index/', '', '', 'glyphicon glyphicon-road', '', '1', '0', '', '0', '0', '0', '0');
INSERT INTO `t_menu` VALUES ('4', '菜单管理', '1', 'backend', '', '', '', 'glyphicon glyphicon-indent-left', '', '1', '0', '', '0', '0', '0', '0');
INSERT INTO `t_menu` VALUES ('5', '资源管理', '1', 'backend', '', '', '', 'glyphicon glyphicon-inbox', '', '1', '0', '', '0', '0', '0', '0');
INSERT INTO `t_menu` VALUES ('6', '参数设置', '1', 'backend', '/setting/index/', '', '', 'fa fa-globe', '', '1', '0', '', '0', '0', '0', '0');
INSERT INTO `t_menu` VALUES ('7', '添加用户', '2', 'backend', '/user/edit/', '/user/edit/.*', '', '', '', '1', '0', '', '0', '0', '1', '1499159216539');
INSERT INTO `t_menu` VALUES ('8', '用户列表', '2', 'backend', '/user/index/', '', '', '', '', '1', '0', '', '0', '0', '0', '0');
INSERT INTO `t_menu` VALUES ('9', '添加菜单', '4', 'backend', '/menu/edit/', '', '', '', '', '1', '0', '', '0', '0', '0', '0');
INSERT INTO `t_menu` VALUES ('10', '菜单列表', '4', 'backend', '/menu/index/', '', '', '', '', '1', '0', '', '0', '0', '0', '0');
INSERT INTO `t_menu` VALUES ('12', '添加资源', '5', 'backend', '/resource/edit/', '', '', '', '', '1', '0', '', '0', '1499064520519', '0', '1499064520519');
INSERT INTO `t_menu` VALUES ('13', '资源列表', '5', 'backend', '/resource/index/', '', '', '', '', '1', '0', '', '0', '1499064552744', '0', '1499064770069');
INSERT INTO `t_menu` VALUES ('14', '数据调度', '0', 'backend', '', '', '', 'glyphicon glyphicon-cloud', '', '1', '30', '', '1', '1499226748886', '1', '1499233819612');
INSERT INTO `t_menu` VALUES ('15', '业务报表', '0', 'backend', '', '', '', 'glyphicon glyphicon-stats', '', '1', '50', '', '1', '1499226798815', '1', '1499233798447');
INSERT INTO `t_menu` VALUES ('16', '基础信息', '0', 'backend', '', '', '', 'glyphicon glyphicon-blackboard', '', '1', '10', '', '1', '1499226843799', '1', '1499233771276');
INSERT INTO `t_menu` VALUES ('17', '流程图', '14', 'backend', '/flow/index/', '', '', 'glyphicon glyphicon-retweet', '', '1', '0', '', '1', '1499234440664', '1', '1499234440664');

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
INSERT INTO `t_relation` VALUES ('role_menu', '1', '1');
INSERT INTO `t_relation` VALUES ('role_menu', '1', '2');
INSERT INTO `t_relation` VALUES ('role_menu', '1', '3');
INSERT INTO `t_relation` VALUES ('role_menu', '1', '4');
INSERT INTO `t_relation` VALUES ('role_menu', '1', '5');
INSERT INTO `t_relation` VALUES ('role_menu', '1', '6');
INSERT INTO `t_relation` VALUES ('role_menu', '1', '7');
INSERT INTO `t_relation` VALUES ('role_menu', '1', '8');
INSERT INTO `t_relation` VALUES ('role_menu', '1', '9');
INSERT INTO `t_relation` VALUES ('role_menu', '1', '10');
INSERT INTO `t_relation` VALUES ('role_menu', '1', '12');
INSERT INTO `t_relation` VALUES ('role_menu', '1', '13');
INSERT INTO `t_relation` VALUES ('role_resource', '1', '1');

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
  `description` tinytext NOT NULL COMMENT '描述',
  `create_uid` int(11) NOT NULL DEFAULT '0' COMMENT '创建者',
  `create_time` bigint(20) NOT NULL DEFAULT '0' COMMENT '创建时间',
  `update_uid` int(11) NOT NULL DEFAULT '0' COMMENT '修改者',
  `update_time` bigint(20) NOT NULL DEFAULT '0' COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8 COMMENT='资源信息表';

-- ----------------------------
-- Records of t_resource
-- ----------------------------
INSERT INTO `t_resource` VALUES ('1', '用户登录', '0', 'backend', 'user', 'login', '', '-1', '0', '', '0', '1499067915669', '0', '1499067915669');
INSERT INTO `t_resource` VALUES ('2', '系统首页', '0', 'backend', 'index', 'index', '', '0', '0', '', '0', '0', '0', '0');

-- ----------------------------
-- Table structure for t_role
-- ----------------------------
DROP TABLE IF EXISTS `t_role`;
CREATE TABLE `t_role` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(255) NOT NULL DEFAULT '' COMMENT '名称',
  `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '状态',
  `sort` int(11) NOT NULL DEFAULT '0' COMMENT '排序',
  `description` tinytext COMMENT '描述',
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
  `description` tinytext COMMENT '描述',
  `update_uid` int(11) NOT NULL DEFAULT '0' COMMENT '修改者',
  `update_time` bigint(20) NOT NULL DEFAULT '0' COMMENT '修改时间',
  PRIMARY KEY (`type`,`parameter`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='配置信息表';

-- ----------------------------
-- Records of t_setting
-- ----------------------------
INSERT INTO `t_setting` VALUES ('system', 'defaultPassword', '用户默认密码', 'admin888', '0', '创建用户时，若密码项留空，则采用该值作为用户默认密码', '0', '1499050437900');
INSERT INTO `t_setting` VALUES ('system', 'permitAll', '开放全部权限', '1', '0', '慎用该配置，若值不为空则系统权限验证全部失效', '1', '1499155751703');
INSERT INTO `t_setting` VALUES ('system', 'siteName', '系统名称', '数据报表调度平台', '0', '通过修改该值用来更换系统显示标题和名称', '0', '1499050481885');

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
  `description` text NOT NULL COMMENT '描述',
  `active_ip` varchar(255) NOT NULL DEFAULT '' COMMENT '最后登录IP',
  `active_time` bigint(20) NOT NULL DEFAULT '0' COMMENT '最后登录时间',
  `create_uid` int(11) NOT NULL DEFAULT '0' COMMENT '创建者',
  `create_time` bigint(20) NOT NULL DEFAULT '0' COMMENT '创建时间',
  `update_uid` int(11) NOT NULL DEFAULT '0' COMMENT '修改者',
  `update_time` bigint(20) NOT NULL DEFAULT '0' COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8 COMMENT='用户信息表';

-- ----------------------------
-- Records of t_user
-- ----------------------------
INSERT INTO `t_user` VALUES ('1', '系统管理员', 'admin', 'd53333790b3613e28e8f9f645cfecb9b', '390455', '1', '1', '', '127.0.0.1', '1499242196770', '0', '1499049919402', '0', '1499050329037');
INSERT INTO `t_user` VALUES ('2', 'test', 'test', 'ce0f14b2f013ff077c5685b21b69a186', '157585', '1', '0', '', '', '0', '0', '1499050281568', '0', '1499050281568');
