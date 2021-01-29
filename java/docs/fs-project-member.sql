/*
Navicat MySQL Data Transfer

Source Server         : localhost_3306
Source Server Version : 50716
Source Host           : localhost:3306
Source Database       : fs-project

Target Server Type    : MYSQL
Target Server Version : 50716
File Encoding         : 65001

Date: 2021-01-29 08:09:38
*/

SET FOREIGN_KEY_CHECKS=0;

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
) ENGINE=InnoDB AUTO_INCREMENT=61 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of fs_member_menu
-- ----------------------------
INSERT INTO `fs_member_menu` VALUES ('1', '后台管理', '后台管理', '0', '', '', '', '0', '1', '', '0', '0', '1584601603608', '1');
INSERT INTO `fs_member_menu` VALUES ('2', '后台首页', '后台管理:后台首页', '1', 'home', '/', '', '0', '1', '工作台、个人信息、修改密码', '1584599862538', '1', '1584603278377', '1');
INSERT INTO `fs_member_menu` VALUES ('3', '仪表盘', '后台首页:仪表盘', '2', 'dashboard', '/dashboard', '', '0', '1', '', '1584599943309', '1', '1584601618327', '1');
INSERT INTO `fs_member_menu` VALUES ('4', '工作台', '后台首页:仪表盘:工作台', '3', '', '/dashboard/workplace', '', '0', '1', '', '1584600104943', '1', '1584600307173', '1');
INSERT INTO `fs_member_menu` VALUES ('5', '用户中心', '后台管理:用户中心', '1', 'user', '/member/index/index', '', '0', '1', '帐号、角色、资源、菜单、配置', '1584600249774', '1', '1584603317133', '1');
INSERT INTO `fs_member_menu` VALUES ('6', '用户管理', '后台管理:用户中心:用户管理', '5', 'user', '/member/user', '', '0', '1', '', '1584600290726', '1', '1585384888303', '1');
INSERT INTO `fs_member_menu` VALUES ('7', '用户列表', '后台管理:用户中心:用户管理:用户列表', '6', '', '/member/user/list', '', '0', '1', '', '1584600340060', '1', '1585384893566', '1');
INSERT INTO `fs_member_menu` VALUES ('8', '角色管理', '后台管理:用户中心:角色管理', '5', 'team', '/member/role', '', '0', '1', '', '1584600399810', '1', '1585384902123', '1');
INSERT INTO `fs_member_menu` VALUES ('9', '资源管理', '后台管理:用户中心:资源管理', '5', 'file-protect', '/member/resource', '', '0', '1', '', '1584600461951', '1', '1585384945709', '1');
INSERT INTO `fs_member_menu` VALUES ('10', '菜单管理', '后台管理:用户中心:菜单管理', '5', 'link', '/member/menu', '', '0', '1', '', '1584600513666', '1', '1585384966047', '1');
INSERT INTO `fs_member_menu` VALUES ('11', '配置管理', '后台管理:用户中心:配置管理', '5', 'setting', '/member/setting', '', '0', '1', '', '1584600543641', '1', '1585384981710', '1');
INSERT INTO `fs_member_menu` VALUES ('12', '角色列表', '后台管理:用户中心:角色管理:角色列表', '8', '', '/member/role/list', '', '0', '1', '', '1584600568015', '1', '1585384939982', '1');
INSERT INTO `fs_member_menu` VALUES ('13', '菜单列表', '后台管理:用户中心:菜单管理:菜单列表', '10', '', '/member/menu/list', '', '0', '1', '', '1584600581275', '1', '1585384970598', '1');
INSERT INTO `fs_member_menu` VALUES ('14', '资源列表', '后台管理:用户中心:资源管理:资源列表', '9', '', '/member/resource/list', '', '0', '1', '', '1584600602600', '1', '1585384951171', '1');
INSERT INTO `fs_member_menu` VALUES ('15', '树形资源', '后台管理:用户中心:资源管理:树形资源', '9', '', '/member/resource/tree', '', '0', '1', '', '1584600614965', '1', '1585384960607', '1');
INSERT INTO `fs_member_menu` VALUES ('16', '树形菜单', '后台管理:用户中心:菜单管理:树形菜单', '10', '', '/member/menu/tree', '', '0', '1', '', '1584600858740', '1', '1585384975070', '1');
INSERT INTO `fs_member_menu` VALUES ('17', '配置列表', '后台管理:用户中心:配置管理:配置列表', '11', '', '/member/setting/list', '', '0', '1', '', '1584600875558', '1', '1585384987639', '1');
INSERT INTO `fs_member_menu` VALUES ('18', '个人中心', '后台首页:个人中心', '2', 'profile', '/account', '', '0', '1', '', '1584601522091', '1', '1584601635790', '1');
INSERT INTO `fs_member_menu` VALUES ('19', '个人信息', '后台首页:个人中心:个人信息', '18', '', '/account/profile', '', '0', '1', '', '1584601559031', '1', '1584601559031', '1');
INSERT INTO `fs_member_menu` VALUES ('20', '修改密码', '后台首页:个人中心:修改密码', '18', '', '/account/password', '', '0', '1', '', '1584601575033', '1', '1584601575033', '1');
INSERT INTO `fs_member_menu` VALUES ('21', '网页爬虫', '后台管理:网页爬虫', '1', 'bug', '/spider/index/index', '', '0', '1', '节点信息、模板管理', '1585195171884', '1', '1585195171884', '1');
INSERT INTO `fs_member_menu` VALUES ('22', '模板管理', '后台管理:网页爬虫:模板管理', '21', 'code', '/spider/template', '', '0', '1', '', '1585195263054', '1', '1585195263054', '1');
INSERT INTO `fs_member_menu` VALUES ('23', '模板列表', '后台管理:网页爬虫:模板管理:模板列表', '22', '', '/spider/template/list', '', '0', '1', '', '1585195284371', '1', '1585195284371', '1');
INSERT INTO `fs_member_menu` VALUES ('24', '节点管理', '后台管理:网页爬虫:节点管理', '21', 'cloud-server', '/spider/crawler', '', '0', '1', '', '1585195333832', '1', '1585195333832', '1');
INSERT INTO `fs_member_menu` VALUES ('25', '节点面板', '后台管理:网页爬虫:节点管理:节点面板', '24', '', '/spider/crawler/dashboard', '', '0', '1', '', '1585195373342', '1', '1585195373342', '1');
INSERT INTO `fs_member_menu` VALUES ('26', '数据计算', '后台管理:数据计算', '1', 'cluster', '/spark/index/index', '', '0', '1', 'Spark插件、计算流程、离线分析', '1585384043533', '1', '1585384305065', '1');
INSERT INTO `fs_member_menu` VALUES ('27', '插件管理', '后台管理:数据计算:插件管理', '26', 'dropbox', '/spark/plugin', '', '0', '1', '', '1585384219002', '1', '1585384219002', '1');
INSERT INTO `fs_member_menu` VALUES ('28', '插件列表', '后台管理:数据计算:插件管理:插件列表', '27', '', '/spark/plugin/list', '', '0', '1', '', '1585384240550', '1', '1585384240550', '1');
INSERT INTO `fs_member_menu` VALUES ('29', '流程管理', '后台管理:数据计算:流程管理', '26', 'deployment-unit', '/spark/flow', '', '0', '1', '', '1585661766299', '1', '1585661985644', '1');
INSERT INTO `fs_member_menu` VALUES ('30', '流程列表', '后台管理:数据计算:流程管理:流程列表', '29', '', '/spark/flow/list', '', '0', '1', '', '1585661826570', '1', '1585661826570', '1');
INSERT INTO `fs_member_menu` VALUES ('31', '节点列表', '后台管理:数据计算:流程管理:节点列表', '29', '', '/spark/node/list', '', '0', '1', '', '1585661841784', '1', '1585661841784', '1');
INSERT INTO `fs_member_menu` VALUES ('32', '树形节点', '后台管理:数据计算:流程管理:树形节点', '29', '', '/spark/node/tree', '', '0', '1', '', '1585661854477', '1', '1585661854477', '1');
INSERT INTO `fs_member_menu` VALUES ('37', '辅助工具', '后台管理:数据计算:辅助工具', '26', 'rocket', '/spark/tool', '', '0', '1', '', '1585662611501', '1', '1610410293382', '1');
INSERT INTO `fs_member_menu` VALUES ('38', '属性编辑器', '后台管理:数据计算:辅助工具:属性编辑器', '37', '', '/spark/tool/property', '', '0', '1', '', '1585663056291', '1', '1610410337234', '1');
INSERT INTO `fs_member_menu` VALUES ('39', '字段编辑器', '后台管理:数据计算:辅助工具:字段编辑器', '37', '', '/spark/tool/field', '', '0', '1', '', '1585663079511', '1', '1610410326558', '1');
INSERT INTO `fs_member_menu` VALUES ('40', '人脸识别', '后台管理:人脸识别', '1', 'smile', '/face/index/index', '', '0', '1', '人脸检测、人脸识别、检索对比', '1597297964946', '1', '1597298791858', '1');
INSERT INTO `fs_member_menu` VALUES ('41', '分组管理', '后台管理:人脸识别:分组管理', '40', 'team', '/face/group', '', '0', '1', '', '1597297984692', '1', '1598260929886', '1');
INSERT INTO `fs_member_menu` VALUES ('42', '人员管理', '后台管理:人脸识别:人员管理', '40', 'user', '/face/user', '', '0', '1', '', '1597298848332', '1', '1598260938414', '1');
INSERT INTO `fs_member_menu` VALUES ('43', '分组列表', '后台管理:人脸识别:分组管理:分组列表', '41', '', '/face/group/list', '', '0', '1', '', '1597298889503', '1', '1597298889503', '1');
INSERT INTO `fs_member_menu` VALUES ('44', '人员列表', '后台管理:人脸识别:人员管理:人员列表', '42', '', '/face/user/list', '', '0', '1', '', '1597298938906', '1', '1597298938906', '1');
INSERT INTO `fs_member_menu` VALUES ('45', '人像管理', '后台管理:人脸识别:人像管理', '40', 'picture', '/face/photo', '', '0', '1', '', '1598324193930', '1', '1598324327966', '1');
INSERT INTO `fs_member_menu` VALUES ('46', '人像列表', '后台管理:人脸识别:人像管理:人像列表', '45', '', '/face/photo/list', '', '0', '1', '', '1598324353459', '1', '1598324353459', '1');
INSERT INTO `fs_member_menu` VALUES ('47', '控制面板', '后台管理:人脸识别:控制面板', '40', 'block', '/face/dashboard', '', '1', '1', '', '1598405239096', '1', '1598405239096', '1');
INSERT INTO `fs_member_menu` VALUES ('48', '人脸对比', '后台管理:人脸识别:控制面板:人脸对比', '47', '', '/face/dashboard/compare', '', '0', '1', '', '1598405279275', '1', '1598405279275', '1');
INSERT INTO `fs_member_menu` VALUES ('49', '人脸检索', '后台管理:人脸识别:控制面板:人脸检索', '47', '', '/face/dashboard/search', '', '0', '1', '', '1598405321956', '1', '1598405321956', '1');
INSERT INTO `fs_member_menu` VALUES ('50', '人脸检测', '后台管理:人脸识别:控制面板:人脸检测', '47', '', '/face/dashboard/detect', '', '1', '1', '', '1598408189380', '1', '1598408189380', '1');
INSERT INTO `fs_member_menu` VALUES ('51', '搜索引擎', '后台管理:搜索引擎', '1', 'search', '/lucene/index/index', '', '0', '1', '词典管理、索引示例、服务重载', '1600481187938', '1', '1600481496017', '1');
INSERT INTO `fs_member_menu` VALUES ('52', 'Elasticsearch', '后台管理:搜索引擎:Elasticsearch', '51', 'deployment-unit', '/lucene/elasticsearch', '', '0', '1', '', '1600481694000', '1', '1600481730276', '1');
INSERT INTO `fs_member_menu` VALUES ('53', '索引示例', '后台管理:搜索引擎:Elasticsearch:索引示例', '52', '', '/lucene/elasticsearch/demo', '', '0', '1', '', '1600481950656', '1', '1600481950656', '1');
INSERT INTO `fs_member_menu` VALUES ('54', '词典管理', '后台管理:搜索引擎:Elasticsearch:词典管理', '52', '', '/lucene/elasticsearch/dict', '', '0', '-1', '', '1600481985836', '1', '1607514726426', '1');
INSERT INTO `fs_member_menu` VALUES ('55', '服务重载', '后台管理:搜索引擎:Elasticsearch:服务重载', '52', '', '/lucene/elasticsearch/reload', '', '0', '1', '', '1600482059381', '1', '1600482059381', '1');
INSERT INTO `fs_member_menu` VALUES ('56', '词库管理', '后台管理:搜索引擎:词库管理', '51', 'gold', '/lucene/dictionary', '', '0', '1', '', '1607333548750', '1', '1607334213102', '1');
INSERT INTO `fs_member_menu` VALUES ('57', '词库列表', '后台管理:搜索引擎:词库管理:词库列表', '56', '', '/lucene/dictionary/list', '', '0', '1', '', '1607333724524', '1', '1607333865742', '1');
INSERT INTO `fs_member_menu` VALUES ('58', '服务管理', '后台管理:服务管理', '1', 'box-plot', '/server/index/index', '', '0', '1', '管理项目基础服务', '1611814694744', '1', '1611814694744', '1');
INSERT INTO `fs_member_menu` VALUES ('59', '消息队列', '后台管理:服务管理:消息队列', '58', 'hourglass', '/server/rabbit', '', '0', '1', '', '1611814821145', '1', '1611814821145', '1');
INSERT INTO `fs_member_menu` VALUES ('60', '控制面板', '后台管理:服务管理:消息队列:控制面板', '59', '', '/server/rabbit/dashboard', '', '0', '1', '', '1611814876795', '1', '1611814876795', '1');

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
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_1', 'role_menu', '1', '1');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_10', 'role_menu', '1', '10');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_11', 'role_menu', '1', '11');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_12', 'role_menu', '1', '12');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_13', 'role_menu', '1', '13');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_14', 'role_menu', '1', '14');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_15', 'role_menu', '1', '15');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_16', 'role_menu', '1', '16');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_17', 'role_menu', '1', '17');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_18', 'role_menu', '1', '18');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_19', 'role_menu', '1', '19');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_2', 'role_menu', '1', '2');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_20', 'role_menu', '1', '20');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_21', 'role_menu', '1', '21');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_22', 'role_menu', '1', '22');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_23', 'role_menu', '1', '23');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_24', 'role_menu', '1', '24');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_25', 'role_menu', '1', '25');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_26', 'role_menu', '1', '26');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_27', 'role_menu', '1', '27');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_28', 'role_menu', '1', '28');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_29', 'role_menu', '1', '29');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_3', 'role_menu', '1', '3');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_30', 'role_menu', '1', '30');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_31', 'role_menu', '1', '31');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_32', 'role_menu', '1', '32');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_33', 'role_menu', '1', '33');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_34', 'role_menu', '1', '34');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_35', 'role_menu', '1', '35');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_36', 'role_menu', '1', '36');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_37', 'role_menu', '1', '37');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_38', 'role_menu', '1', '38');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_39', 'role_menu', '1', '39');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_4', 'role_menu', '1', '4');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_40', 'role_menu', '1', '40');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_41', 'role_menu', '1', '41');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_42', 'role_menu', '1', '42');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_43', 'role_menu', '1', '43');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_44', 'role_menu', '1', '44');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_45', 'role_menu', '1', '45');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_46', 'role_menu', '1', '46');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_47', 'role_menu', '1', '47');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_48', 'role_menu', '1', '48');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_49', 'role_menu', '1', '49');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_5', 'role_menu', '1', '5');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_50', 'role_menu', '1', '50');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_51', 'role_menu', '1', '51');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_52', 'role_menu', '1', '52');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_53', 'role_menu', '1', '53');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_54', 'role_menu', '1', '54');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_55', 'role_menu', '1', '55');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_56', 'role_menu', '1', '56');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_57', 'role_menu', '1', '57');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_58', 'role_menu', '1', '58');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_59', 'role_menu', '1', '59');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_6', 'role_menu', '1', '6');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_60', 'role_menu', '1', '60');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_7', 'role_menu', '1', '7');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_8', 'role_menu', '1', '8');
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_9', 'role_menu', '1', '9');
INSERT INTO `fs_member_relation` VALUES ('role_menu_2_1', 'role_menu', '2', '1');
INSERT INTO `fs_member_relation` VALUES ('role_menu_2_2', 'role_menu', '2', '2');
INSERT INTO `fs_member_relation` VALUES ('role_menu_2_3', 'role_menu', '2', '3');
INSERT INTO `fs_member_relation` VALUES ('role_menu_3_1', 'role_menu', '3', '1');
INSERT INTO `fs_member_relation` VALUES ('role_menu_3_16', 'role_menu', '3', '16');
INSERT INTO `fs_member_relation` VALUES ('role_menu_3_18', 'role_menu', '3', '18');
INSERT INTO `fs_member_relation` VALUES ('role_menu_3_9', 'role_menu', '3', '9');
INSERT INTO `fs_member_relation` VALUES ('role_menu_4_16', 'role_menu', '4', '16');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_1', 'role_resource', '1', '1');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_10', 'role_resource', '1', '10');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_11', 'role_resource', '1', '11');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_12', 'role_resource', '1', '12');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_13', 'role_resource', '1', '13');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_14', 'role_resource', '1', '14');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_15', 'role_resource', '1', '15');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_16', 'role_resource', '1', '16');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_17', 'role_resource', '1', '17');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_18', 'role_resource', '1', '18');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_19', 'role_resource', '1', '19');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_2', 'role_resource', '1', '2');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_20', 'role_resource', '1', '20');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_21', 'role_resource', '1', '21');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_22', 'role_resource', '1', '22');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_23', 'role_resource', '1', '23');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_24', 'role_resource', '1', '24');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_25', 'role_resource', '1', '25');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_26', 'role_resource', '1', '26');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_27', 'role_resource', '1', '27');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_28', 'role_resource', '1', '28');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_29', 'role_resource', '1', '29');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_3', 'role_resource', '1', '3');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_30', 'role_resource', '1', '30');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_31', 'role_resource', '1', '31');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_32', 'role_resource', '1', '32');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_33', 'role_resource', '1', '33');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_34', 'role_resource', '1', '34');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_35', 'role_resource', '1', '35');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_36', 'role_resource', '1', '36');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_37', 'role_resource', '1', '37');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_38', 'role_resource', '1', '38');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_39', 'role_resource', '1', '39');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_4', 'role_resource', '1', '4');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_40', 'role_resource', '1', '40');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_41', 'role_resource', '1', '41');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_42', 'role_resource', '1', '42');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_43', 'role_resource', '1', '43');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_44', 'role_resource', '1', '44');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_45', 'role_resource', '1', '45');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_46', 'role_resource', '1', '46');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_47', 'role_resource', '1', '47');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_48', 'role_resource', '1', '48');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_49', 'role_resource', '1', '49');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_5', 'role_resource', '1', '5');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_50', 'role_resource', '1', '50');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_51', 'role_resource', '1', '51');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_52', 'role_resource', '1', '52');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_53', 'role_resource', '1', '53');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_54', 'role_resource', '1', '54');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_55', 'role_resource', '1', '55');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_56', 'role_resource', '1', '56');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_57', 'role_resource', '1', '57');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_58', 'role_resource', '1', '58');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_59', 'role_resource', '1', '59');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_6', 'role_resource', '1', '6');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_60', 'role_resource', '1', '60');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_61', 'role_resource', '1', '61');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_62', 'role_resource', '1', '62');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_7', 'role_resource', '1', '7');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_8', 'role_resource', '1', '8');
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_9', 'role_resource', '1', '9');
INSERT INTO `fs_member_relation` VALUES ('role_resource_2_1', 'role_resource', '2', '1');
INSERT INTO `fs_member_relation` VALUES ('role_resource_2_2', 'role_resource', '2', '2');
INSERT INTO `fs_member_relation` VALUES ('role_resource_2_3', 'role_resource', '2', '3');
INSERT INTO `fs_member_relation` VALUES ('role_resource_2_4', 'role_resource', '2', '4');
INSERT INTO `fs_member_relation` VALUES ('role_resource_2_5', 'role_resource', '2', '5');
INSERT INTO `fs_member_relation` VALUES ('role_resource_2_6', 'role_resource', '2', '6');
INSERT INTO `fs_member_relation` VALUES ('role_resource_3_1', 'role_resource', '3', '1');
INSERT INTO `fs_member_relation` VALUES ('role_resource_3_2', 'role_resource', '3', '2');
INSERT INTO `fs_member_relation` VALUES ('role_resource_3_8', 'role_resource', '3', '8');
INSERT INTO `fs_member_relation` VALUES ('role_resource_4_8', 'role_resource', '4', '8');
INSERT INTO `fs_member_relation` VALUES ('user_role_1_1', 'user_role', '1', '1');
INSERT INTO `fs_member_relation` VALUES ('user_role_2_2', 'user_role', '2', '2');

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
) ENGINE=InnoDB AUTO_INCREMENT=63 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of fs_member_resource
-- ----------------------------
INSERT INTO `fs_member_resource` VALUES ('1', '后台管理', '后台管理', '0', 'admin', '', '', '0', '1', '是否可访问后台', '1584534838948', '1', '1584535453295', '1');
INSERT INTO `fs_member_resource` VALUES ('2', '用户中心', '用户中心', '0', 'member', '', '', '0', '1', '', '1528081670164', '1', '1584590104125', '1');
INSERT INTO `fs_member_resource` VALUES ('3', '菜单', '用户中心:菜单', '2', 'member', 'menu', '', '0', '1', '', '1528081696180', '1', '1584590110517', '1');
INSERT INTO `fs_member_resource` VALUES ('4', '资源', '用户中心:资源', '2', 'member', 'resource', '', '0', '1', '', '1528081721468', '1', '1584590139693', '1');
INSERT INTO `fs_member_resource` VALUES ('5', '角色', '用户中心:角色', '2', 'member', 'role', '', '0', '1', '', '1528081764195', '1', '1584590159965', '1');
INSERT INTO `fs_member_resource` VALUES ('6', '用户', '用户中心:用户', '2', 'member', 'user', '', '0', '1', '', '1528081790426', '1', '1584590183845', '1');
INSERT INTO `fs_member_resource` VALUES ('7', '配置', '用户中心:配置', '2', 'member', 'setting', '', '0', '1', '', '1528081858994', '1', '1584590241289', '1');
INSERT INTO `fs_member_resource` VALUES ('8', '添加', '用户中心:菜单:添加', '3', 'member', 'menu', 'add', '0', '1', '', '1528081911866', '1', '1584590115052', '1');
INSERT INTO `fs_member_resource` VALUES ('9', '修改', '用户中心:菜单:修改', '3', 'member', 'menu', 'modify', '0', '1', '', '1528081955009', '1', '1584590127509', '1');
INSERT INTO `fs_member_resource` VALUES ('10', '删除', '用户中心:菜单:删除', '3', 'member', 'menu', 'delete', '0', '1', '', '1528081985121', '1', '1584590135133', '1');
INSERT INTO `fs_member_resource` VALUES ('11', '添加', '用户中心:资源:添加', '4', 'member', 'resource', 'add', '0', '1', '', '1528082024771', '1', '1584590144698', '1');
INSERT INTO `fs_member_resource` VALUES ('12', '修改', '用户中心:资源:修改', '4', 'member', 'resource', 'modify', '0', '1', '', '1528082042903', '1', '1584590150312', '1');
INSERT INTO `fs_member_resource` VALUES ('13', '删除', '用户中心:资源:删除', '4', 'member', 'resource', 'delete', '0', '1', '', '1528082084243', '1', '1584590155309', '1');
INSERT INTO `fs_member_resource` VALUES ('14', '添加', '用户中心:角色:添加', '5', 'member', 'role', 'add', '0', '1', '', '1528082104404', '1', '1584590165347', '1');
INSERT INTO `fs_member_resource` VALUES ('15', '修改', '用户中心:角色:修改', '5', 'member', 'role', 'modify', '0', '1', '', '1528082198733', '1', '1584590169694', '1');
INSERT INTO `fs_member_resource` VALUES ('16', '删除', '用户中心:角色:删除', '5', 'member', 'role', 'delete', '0', '1', '', '1528082214265', '1', '1584590174794', '1');
INSERT INTO `fs_member_resource` VALUES ('17', '菜单', '用户中心:角色:菜单', '5', 'member', 'role', 'menu', '0', '1', '', '1528082271647', '1', '1584590180046', '1');
INSERT INTO `fs_member_resource` VALUES ('18', '资源', '用户中心:角色:资源', '5', 'member', 'role', 'resource', '0', '1', '', '1528082432853', '1', '1584590193894', '1');
INSERT INTO `fs_member_resource` VALUES ('19', '添加', '用户中心:用户:添加', '6', 'member', 'user', 'add', '0', '1', '', '1528082465188', '1', '1584590202725', '1');
INSERT INTO `fs_member_resource` VALUES ('20', '修改', '用户中心:用户:修改', '6', 'member', 'user', 'modify', '0', '1', '', '1528082492740', '1', '1584590224552', '1');
INSERT INTO `fs_member_resource` VALUES ('21', '删除', '用户中心:用户:删除', '6', 'member', 'user', 'delete', '0', '1', '', '1528082514205', '1', '1584590230942', '1');
INSERT INTO `fs_member_resource` VALUES ('22', '角色', '用户中心:用户:角色', '6', 'member', 'user', 'role', '0', '1', '', '1528082586393', '1', '1584590236190', '1');
INSERT INTO `fs_member_resource` VALUES ('23', '添加', '用户中心:配置:添加', '7', 'member', 'setting', 'add', '0', '1', '', '1528082616922', '1', '1584590245882', '1');
INSERT INTO `fs_member_resource` VALUES ('24', '修改', '用户中心:配置:修改', '7', 'member', 'setting', 'modify', '0', '1', '', '1528082642509', '1', '1584590249855', '1');
INSERT INTO `fs_member_resource` VALUES ('25', '删除', '用户中心:配置:删除', '7', 'member', 'setting', 'delete', '0', '1', '', '1528082662922', '1', '1584590258533', '1');
INSERT INTO `fs_member_resource` VALUES ('26', '网页爬虫', '网页爬虫', '0', 'spider', '', '', '0', '1', '', '1585195421330', '1', '1585195421330', '1');
INSERT INTO `fs_member_resource` VALUES ('27', '模板', '网页爬虫:模板', '26', 'spider', 'template', '', '0', '1', '', '1585195446408', '1', '1585195446408', '1');
INSERT INTO `fs_member_resource` VALUES ('28', '删除', '网页爬虫:模板:删除', '27', 'spider', 'template', 'delete', '0', '1', '', '1585195509597', '1', '1585195533509', '1');
INSERT INTO `fs_member_resource` VALUES ('29', '修改', '网页爬虫:模板:修改', '27', 'spider', 'template', 'modify', '0', '1', '', '1585195523181', '1', '1585195523181', '1');
INSERT INTO `fs_member_resource` VALUES ('30', '添加', '网页爬虫:模板:添加', '27', 'spider', 'template', 'add', '0', '1', '', '1585216567413', '1', '1585216567413', '1');
INSERT INTO `fs_member_resource` VALUES ('31', '数据计算', '数据计算', '0', 'spark', '', '', '0', '1', '', '1585384071227', '1', '1585384071227', '1');
INSERT INTO `fs_member_resource` VALUES ('32', '插件', '数据计算:插件', '31', 'spark', 'plugin', '', '0', '1', '', '1585384104596', '1', '1585384120688', '1');
INSERT INTO `fs_member_resource` VALUES ('33', '添加', '数据计算:插件:添加', '32', 'spark', 'plugin', 'add', '0', '1', '', '1585384138739', '1', '1585384138739', '1');
INSERT INTO `fs_member_resource` VALUES ('34', '修改', '数据计算:插件:修改', '32', 'spark', 'plugin', 'modify', '0', '1', '', '1585384152067', '1', '1585384152067', '1');
INSERT INTO `fs_member_resource` VALUES ('35', '删除', '数据计算:插件:删除', '32', 'spark', 'plugin', 'delete', '0', '1', '', '1585384168964', '1', '1585384168964', '1');
INSERT INTO `fs_member_resource` VALUES ('36', '流程节点', '数据计算:流程节点', '31', 'spark', 'node', '', '0', '1', '', '1585669452789', '1', '1585727887482', '1');
INSERT INTO `fs_member_resource` VALUES ('37', '添加', '数据计算:流程节点:添加', '36', 'spark', 'node', 'add', '0', '1', '', '1585669473979', '1', '1585669473979', '1');
INSERT INTO `fs_member_resource` VALUES ('38', '修改', '数据计算:流程节点:修改', '36', 'spark', 'node', 'modify', '0', '1', '', '1585669487729', '1', '1585669487729', '1');
INSERT INTO `fs_member_resource` VALUES ('39', '删除', '数据计算:流程节点:删除', '36', 'spark', 'node', 'delete', '0', '1', '', '1585669499607', '1', '1585669499607', '1');
INSERT INTO `fs_member_resource` VALUES ('40', '流程', '数据计算:流程', '31', 'spark', 'flow', '', '0', '1', '', '1585727898953', '1', '1585727898953', '1');
INSERT INTO `fs_member_resource` VALUES ('41', '添加', '数据计算:流程:添加', '40', 'spark', 'flow', 'add', '0', '1', '', '1585727916428', '1', '1585727941798', '1');
INSERT INTO `fs_member_resource` VALUES ('42', '修改', '数据计算:流程:修改', '40', 'spark', 'flow', 'modify', '0', '1', '', '1585727959930', '1', '1585727959930', '1');
INSERT INTO `fs_member_resource` VALUES ('43', '删除', '数据计算:流程:删除', '40', 'spark', 'flow', 'delete', '0', '1', '', '1585727972170', '1', '1585727972170', '1');
INSERT INTO `fs_member_resource` VALUES ('44', '人脸识别', '人脸识别', '0', 'face', '', '', '0', '1', '', '1597299393466', '1', '1597299393466', '1');
INSERT INTO `fs_member_resource` VALUES ('45', '分组', '人脸识别:分组', '44', 'face', 'group', '', '0', '1', '', '1597299426164', '1', '1597299426164', '1');
INSERT INTO `fs_member_resource` VALUES ('46', '人员', '人脸识别:人员', '44', 'face', 'user', '', '0', '1', '', '1597299441594', '1', '1597299441594', '1');
INSERT INTO `fs_member_resource` VALUES ('47', '添加', '人脸识别:分组:添加', '45', 'face', 'group', 'add', '0', '1', '', '1597299463846', '1', '1597299463846', '1');
INSERT INTO `fs_member_resource` VALUES ('48', '修改', '人脸识别:分组:修改', '45', 'face', 'group', 'modify', '0', '1', '', '1597299487386', '1', '1597299487386', '1');
INSERT INTO `fs_member_resource` VALUES ('49', '删除', '人脸识别:分组:删除', '45', 'face', 'group', 'delete', '0', '1', '', '1597299507470', '1', '1597299507470', '1');
INSERT INTO `fs_member_resource` VALUES ('50', '添加', '人脸识别:人员:添加', '46', 'face', 'user', 'add', '0', '1', '', '1597299532434', '1', '1597299532434', '1');
INSERT INTO `fs_member_resource` VALUES ('51', '修改', '人脸识别:人员:修改', '46', 'face', 'user', 'modify', '0', '1', '', '1597299549779', '1', '1597299549779', '1');
INSERT INTO `fs_member_resource` VALUES ('52', '删除', '人脸识别:人员:删除', '46', 'face', 'user', 'delete', '0', '1', '', '1597299561750', '1', '1597299561750', '1');
INSERT INTO `fs_member_resource` VALUES ('53', '分组', '人脸识别:人员:分组', '46', 'face', 'user', 'group', '0', '1', '', '1598238834654', '1', '1598238834654', '1');
INSERT INTO `fs_member_resource` VALUES ('54', '人像', '人脸识别:人像', '44', 'face', 'photo', '', '0', '1', '', '1598324393176', '1', '1598324393176', '1');
INSERT INTO `fs_member_resource` VALUES ('55', '添加', '人脸识别:人像:添加', '54', 'face', 'photo', 'add', '0', '1', '', '1598324413298', '1', '1598324413298', '1');
INSERT INTO `fs_member_resource` VALUES ('56', '修改', '人脸识别:人像:修改', '54', 'face', 'photo', 'modify', '0', '1', '', '1598324427504', '1', '1598324427504', '1');
INSERT INTO `fs_member_resource` VALUES ('57', '删除', '人脸识别:人像:删除', '54', 'face', 'photo', 'delete', '0', '1', '', '1598324443162', '1', '1598324443162', '1');
INSERT INTO `fs_member_resource` VALUES ('58', '搜索引擎', '搜索引擎', '0', 'lucene', '', '', '0', '1', '', '1607333327044', '1', '1607333327044', '1');
INSERT INTO `fs_member_resource` VALUES ('59', '词库', '搜索引擎:词库', '58', 'lucene', 'dictionary', '', '0', '1', '', '1607333381289', '1', '1607333426485', '1');
INSERT INTO `fs_member_resource` VALUES ('60', '添加', '搜索引擎:词库:添加', '59', 'lucene', 'dictionary', 'add', '0', '1', '', '1607333455146', '1', '1607333455146', '1');
INSERT INTO `fs_member_resource` VALUES ('61', '修改', '搜索引擎:词库:修改', '59', 'lucene', 'dictionary', 'modify', '0', '1', '', '1607333470362', '1', '1607333470362', '1');
INSERT INTO `fs_member_resource` VALUES ('62', '删除', '搜索引擎:词库:删除', '59', 'lucene', 'dictionary', 'delete', '0', '1', '', '1607333488826', '1', '1607333488826', '1');

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
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of fs_member_role
-- ----------------------------
INSERT INTO `fs_member_role` VALUES ('1', '后台管理', '0', '1', '', '1528081589495', '1', '1528266877684', '1');
INSERT INTO `fs_member_role` VALUES ('2', '普通用户', '0', '1', '', '1528081606670', '1', '1528081606670', '1');
INSERT INTO `fs_member_role` VALUES ('3', 'aaa', '0', '-1', '', '1584514294259', '1', '1584515444090', '1');
INSERT INTO `fs_member_role` VALUES ('4', 'aaaxx', '0', '2', '', '1584515454033', '1', '1584515461772', '1');

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
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of fs_member_setting
-- ----------------------------
INSERT INTO `fs_member_setting` VALUES ('1', 'default-password', 'member', 'password', '1', '默认登录密码', '1584530675522', '1');
INSERT INTO `fs_member_setting` VALUES ('3', 'menu-parent-id', 'admin', '1', '1', '管理后台菜单根节点ID', '1532401344424', '1');
INSERT INTO `fs_member_setting` VALUES ('4', 'test', 'test', 'aaa', '0', '', '1596167128860', '1');

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
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of fs_member_user
-- ----------------------------
INSERT INTO `fs_member_user` VALUES ('1', 'admin', '管理员', 'fc7911b5108d30e087f8881b90368679', '5231', '0', '1', '', '1528081552985', '127.0.0.1', '1', '1528081552985', '1', '1611812584629', '127.0.0.1', '0');
INSERT INTO `fs_member_user` VALUES ('2', 'test', '测试123', '4b361be828611add84453a24f39772a5', '0905', '0', '1', '', '1528081567988', '127.0.0.1', '1', '1542958281919', '1', '1528267171953', '127.0.0.1', '0');
INSERT INTO `fs_member_user` VALUES ('3', '111', '111', '', '', '6', '-1', '', '0', '', '0', '1584360531961', '1', '0', '', '0');
INSERT INTO `fs_member_user` VALUES ('4', '222', '222', '', '', '0', '1', '', '0', '', '0', '0', '0', '0', '', '0');
INSERT INTO `fs_member_user` VALUES ('5', '333', '333', '', '', '0', '1', '', '0', '', '0', '1606803617105', '1', '0', '', '0');
INSERT INTO `fs_member_user` VALUES ('6', '444', '444', '', '', '0', '1', '', '0', '', '0', '0', '0', '0', '', '0');
INSERT INTO `fs_member_user` VALUES ('7', '555', '555', '', '', '0', '1', '', '0', '', '0', '0', '0', '0', '', '0');
INSERT INTO `fs_member_user` VALUES ('8', '666', '666', '', '', '0', '1', '', '0', '', '0', '0', '0', '0', '', '0');
INSERT INTO `fs_member_user` VALUES ('9', '777', '777', '', '', '0', '1', '', '0', '', '0', '0', '0', '0', '', '0');
INSERT INTO `fs_member_user` VALUES ('10', '888', '888', '', '', '0', '1', '', '0', '', '0', '0', '0', '0', '', '0');
INSERT INTO `fs_member_user` VALUES ('11', '999', '999', '', '', '0', '1', '', '0', '', '0', '0', '0', '0', '', '0');
INSERT INTO `fs_member_user` VALUES ('12', '124124', '124124', '', '', '0', '1', '', '0', '', '0', '0', '0', '0', '', '0');
INSERT INTO `fs_member_user` VALUES ('13', '55555', '55555', '', '', '0', '1', '', '0', '', '0', '0', '0', '0', '', '0');
INSERT INTO `fs_member_user` VALUES ('14', '777777', '777777', '', '', '0', '1', '', '0', '', '0', '0', '0', '0', '', '0');
INSERT INTO `fs_member_user` VALUES ('15', '444444', '444444', '', '', '0', '0', ' ', '0', '', '0', '0', '0', '0', '', '0');
INSERT INTO `fs_member_user` VALUES ('16', '90909', '9090909', '', '', '0', '0', '  ', '0', '', '0', '0', '0', '0', '', '0');
