/*
 Navicat Premium Data Transfer

 Source Server         : local-mysql
 Source Server Type    : MySQL
 Source Server Version : 80021
 Source Host           : localhost:3306
 Source Schema         : fs_project

 Target Server Type    : MySQL
 Target Server Version : 80021
 File Encoding         : 65001

 Date: 20/02/2023 10:20:37
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for fs_member_dictionary
-- ----------------------------
DROP TABLE IF EXISTS `fs_member_dictionary`;
CREATE TABLE `fs_member_dictionary`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `full_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `parent_id` int NOT NULL DEFAULT 0,
  `content` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `sort` tinyint NOT NULL DEFAULT 0,
  `status` tinyint NOT NULL DEFAULT 0,
  `description` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `created_time` bigint NOT NULL DEFAULT 0,
  `created_uid` int NOT NULL DEFAULT 0,
  `updated_time` bigint NOT NULL DEFAULT 0,
  `updated_uid` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_content`(`content` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 25 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of fs_member_dictionary
-- ----------------------------
INSERT INTO `fs_member_dictionary` VALUES (1, '性别', '性别', 0, 'gender', 0, 1, '', 1619512547886, 1, 1619512547886, 1);
INSERT INTO `fs_member_dictionary` VALUES (2, '男', '性别:男', 1, 'man', 0, 1, '', 1619512558937, 1, 1619512558937, 1);
INSERT INTO `fs_member_dictionary` VALUES (3, '女', '性别:女', 1, 'woman', 0, 1, '', 1619512570735, 1, 1619512570735, 1);
INSERT INTO `fs_member_dictionary` VALUES (4, '级联', '级联', 0, 'cascade', 0, 1, '', 1619512827457, 1, 1619512827457, 1);
INSERT INTO `fs_member_dictionary` VALUES (5, '一级-1', '级联:一级-1', 4, 'level-1', 0, 1, '', 1619512855045, 1, 1619512855045, 1);
INSERT INTO `fs_member_dictionary` VALUES (6, '一级-2', '级联:一级-2', 4, 'level-2', 0, 1, '', 1619512872071, 1, 1619512872071, 1);
INSERT INTO `fs_member_dictionary` VALUES (7, '一级-3', '级联:一级-3', 4, 'level-3', 0, 1, '', 1619512893508, 1, 1619512961411, 1);
INSERT INTO `fs_member_dictionary` VALUES (8, '二级-1-1', '级联:一级-1:二级-1-1', 5, 'level-1-1', 0, 1, '', 1619512990888, 1, 1619512990888, 1);
INSERT INTO `fs_member_dictionary` VALUES (9, '二级-3-1', '级联:一级-3:二级-3-1', 7, 'level-3-1', 0, 1, '', 1619513015148, 1, 1619513015148, 1);
INSERT INTO `fs_member_dictionary` VALUES (10, '二级-3-2', '级联:一级-3:二级-3-2', 7, 'level-3-2', 0, 1, '', 1619513023627, 1, 1619513023627, 1);
INSERT INTO `fs_member_dictionary` VALUES (11, '岗位职责', '岗位职责', 0, 'job-responsibility', 0, 1, '', 1622184066092, 1, 1628835455782, 1);
INSERT INTO `fs_member_dictionary` VALUES (12, '董事长', '岗位职责:董事长', 11, 'chairman', 0, 1, '', 1622184121813, 1, 1622184121813, 1);
INSERT INTO `fs_member_dictionary` VALUES (13, '总经理', '岗位职责:总经理', 11, 'president', 0, 1, '', 1622184318304, 1, 1622184318304, 1);
INSERT INTO `fs_member_dictionary` VALUES (14, '副总经理', '岗位职责:副总经理', 11, 'deputy-president', 0, 1, '', 1622184347379, 1, 1628835465073, 1);
INSERT INTO `fs_member_dictionary` VALUES (15, '主管', '岗位职责:主管', 11, 'executive', 0, 1, '', 1622184431053, 1, 1622184431053, 1);
INSERT INTO `fs_member_dictionary` VALUES (16, '职员', '岗位职责:职员', 11, 'staff', 0, 1, '', 1622184489471, 1, 1622184489471, 1);
INSERT INTO `fs_member_dictionary` VALUES (17, '审核标签', '审核标签', 0, 'audit-tag', 0, 1, '', 1628835395109, 1, 1628835477591, 1);
INSERT INTO `fs_member_dictionary` VALUES (18, '广告', '审核标签:广告', 17, 'advertise', 99, 1, '', 1628835501325, 1, 1628836122775, 1);
INSERT INTO `fs_member_dictionary` VALUES (19, '敏感词', '审核标签:敏感词', 17, 'sensitive', 19, 1, '', 1628835532602, 1, 1628836061905, 1);
INSERT INTO `fs_member_dictionary` VALUES (20, '色情', '审核标签:色情', 17, 'porn', 98, 1, '', 1628835543698, 1, 1628836135493, 1);
INSERT INTO `fs_member_dictionary` VALUES (21, '暴力', '审核标签:暴力', 17, 'violence', 97, 1, '', 1628835577084, 1, 1628836149133, 1);
INSERT INTO `fs_member_dictionary` VALUES (22, '违法', '审核标签:违法', 17, 'illegal', 22, 1, '', 1628835588090, 1, 1628836033275, 1);
INSERT INTO `fs_member_dictionary` VALUES (23, '辱骂', '审核标签:辱骂', 17, 'abuse', 23, 1, '', 1628835601620, 1, 1628836014104, 1);
INSERT INTO `fs_member_dictionary` VALUES (24, '其他', '审核标签:其他', 17, 'other', 4, 1, '', 1628835614279, 1, 1628836162399, 1);

-- ----------------------------
-- Table structure for fs_member_menu
-- ----------------------------
DROP TABLE IF EXISTS `fs_member_menu`;
CREATE TABLE `fs_member_menu`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `full_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `parent_id` int NOT NULL DEFAULT 0,
  `icon` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `url` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `target` varchar(8) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `sort` tinyint NOT NULL DEFAULT 0,
  `status` tinyint NOT NULL DEFAULT 0,
  `description` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `created_time` bigint NOT NULL DEFAULT 0,
  `created_uid` int NOT NULL DEFAULT 0,
  `updated_time` bigint NOT NULL DEFAULT 0,
  `updated_uid` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 130 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of fs_member_menu
-- ----------------------------
INSERT INTO `fs_member_menu` VALUES (1, '后台管理', '后台管理', 0, '', '', '', 0, 1, '', 0, 0, 1584601603608, 1);
INSERT INTO `fs_member_menu` VALUES (2, '后台首页', '后台管理:后台首页', 1, 'home', '/', '', 0, 1, '工作台、个人信息、修改密码', 1584599862538, 1, 1584603278377, 1);
INSERT INTO `fs_member_menu` VALUES (3, '仪表盘', '后台首页:仪表盘', 2, 'dashboard', '/dashboard', '', 0, 1, '', 1584599943309, 1, 1584601618327, 1);
INSERT INTO `fs_member_menu` VALUES (4, '工作台', '后台首页:仪表盘:工作台', 3, '', '/dashboard/workplace', '', 0, 1, '', 1584600104943, 1, 1584600307173, 1);
INSERT INTO `fs_member_menu` VALUES (5, '用户中心', '后台管理:用户中心', 1, 'user', '/member/index/index', '', 0, 1, '帐号、角色、资源、菜单、配置', 1584600249774, 1, 1584603317133, 1);
INSERT INTO `fs_member_menu` VALUES (6, '用户管理', '后台管理:用户中心:用户管理', 5, 'user', '/member/user', '', 0, 1, '', 1584600290726, 1, 1585384888303, 1);
INSERT INTO `fs_member_menu` VALUES (7, '用户列表', '后台管理:用户中心:用户管理:用户列表', 6, '', '/member/user/list', '', 0, 1, '', 1584600340060, 1, 1585384893566, 1);
INSERT INTO `fs_member_menu` VALUES (8, '角色管理', '后台管理:用户中心:角色管理', 5, 'team', '/member/role', '', 0, 1, '', 1584600399810, 1, 1585384902123, 1);
INSERT INTO `fs_member_menu` VALUES (9, '资源管理', '后台管理:用户中心:资源管理', 5, 'file-protect', '/member/resource', '', 0, 1, '', 1584600461951, 1, 1585384945709, 1);
INSERT INTO `fs_member_menu` VALUES (10, '菜单管理', '后台管理:用户中心:菜单管理', 5, 'link', '/member/menu', '', 0, 1, '', 1584600513666, 1, 1585384966047, 1);
INSERT INTO `fs_member_menu` VALUES (11, '配置管理', '后台管理:用户中心:配置管理', 5, 'setting', '/member/setting', '', 0, 1, '', 1584600543641, 1, 1585384981710, 1);
INSERT INTO `fs_member_menu` VALUES (12, '角色列表', '后台管理:用户中心:角色管理:角色列表', 8, '', '/member/role/list', '', 0, 1, '', 1584600568015, 1, 1585384939982, 1);
INSERT INTO `fs_member_menu` VALUES (13, '菜单列表', '后台管理:用户中心:菜单管理:菜单列表', 10, '', '/member/menu/list', '', 0, 1, '', 1584600581275, 1, 1585384970598, 1);
INSERT INTO `fs_member_menu` VALUES (14, '资源列表', '后台管理:用户中心:资源管理:资源列表', 9, '', '/member/resource/list', '', 0, 1, '', 1584600602600, 1, 1585384951171, 1);
INSERT INTO `fs_member_menu` VALUES (15, '树形资源', '后台管理:用户中心:资源管理:树形资源', 9, '', '/member/resource/tree', '', 0, 1, '', 1584600614965, 1, 1585384960607, 1);
INSERT INTO `fs_member_menu` VALUES (16, '树形菜单', '后台管理:用户中心:菜单管理:树形菜单', 10, '', '/member/menu/tree', '', 0, 1, '', 1584600858740, 1, 1585384975070, 1);
INSERT INTO `fs_member_menu` VALUES (17, '配置列表', '后台管理:用户中心:配置管理:配置列表', 11, '', '/member/setting/list', '', 0, 1, '', 1584600875558, 1, 1585384987639, 1);
INSERT INTO `fs_member_menu` VALUES (18, '个人中心', '后台首页:个人中心', 2, 'profile', '/account', '', 0, 1, '', 1584601522091, 1, 1584601635790, 1);
INSERT INTO `fs_member_menu` VALUES (19, '个人信息', '后台首页:个人中心:个人信息', 18, '', '/account/profile', '', 0, 1, '', 1584601559031, 1, 1584601559031, 1);
INSERT INTO `fs_member_menu` VALUES (20, '修改密码', '后台首页:个人中心:修改密码', 18, '', '/account/password', '', 0, 1, '', 1584601575033, 1, 1584601575033, 1);
INSERT INTO `fs_member_menu` VALUES (21, '网页爬虫', '后台管理:网页爬虫', 1, 'bug', '/spider/index/index', '', 0, 1, '节点信息、模板管理', 1585195171884, 1, 1585195171884, 1);
INSERT INTO `fs_member_menu` VALUES (22, '模板管理', '后台管理:网页爬虫:模板管理', 21, 'code', '/spider/template', '', 0, 1, '', 1585195263054, 1, 1585195263054, 1);
INSERT INTO `fs_member_menu` VALUES (23, '模板列表', '后台管理:网页爬虫:模板管理:模板列表', 22, '', '/spider/template/list', '', 0, 1, '', 1585195284371, 1, 1585195284371, 1);
INSERT INTO `fs_member_menu` VALUES (24, '节点管理', '后台管理:网页爬虫:节点管理', 21, 'cloud-server', '/spider/crawler', '', 0, 1, '', 1585195333832, 1, 1585195333832, 1);
INSERT INTO `fs_member_menu` VALUES (25, '节点面板', '后台管理:网页爬虫:节点管理:节点面板', 24, '', '/spider/crawler/dashboard', '', 0, 1, '', 1585195373342, 1, 1585195373342, 1);
INSERT INTO `fs_member_menu` VALUES (26, '商业智能', '后台管理:商业智能', 1, 'cluster', '/bi/index/index', '', 0, 1, '数据清洗、规则引擎、智能报表', 1585384043533, 1, 1642381343286, 1);
INSERT INTO `fs_member_menu` VALUES (27, '数据加工', '后台管理:商业智能:数据加工', 26, 'apartment', '/bi/diagram', '', 0, 1, '', 1585384219002, 1, 1634258659883, 1);
INSERT INTO `fs_member_menu` VALUES (28, '清洗规则', '后台管理:商业智能:数据加工:清洗规则', 27, '', '/bi/diagram/list', '', 0, 1, '', 1585384240550, 1, 1637656893932, 1);
INSERT INTO `fs_member_menu` VALUES (29, '数据管理', '后台管理:商业智能:数据管理', 26, 'dropbox', '/bi/data', '', 0, 1, '', 1585661766299, 1, 1634258058714, 1);
INSERT INTO `fs_member_menu` VALUES (30, '数据源', '后台管理:商业智能:数据管理:数据源', 29, '', '/bi/data/source', '', 0, 1, '', 1585661826570, 1, 1634258102486, 1);
INSERT INTO `fs_member_menu` VALUES (31, '数据集', '后台管理:商业智能:数据管理:数据集', 29, '', '/bi/data/dataset', '', 0, 1, '', 1585661841784, 1, 1634258119688, 1);
INSERT INTO `fs_member_menu` VALUES (32, '数据矩阵', '后台管理:商业智能:智能报表:数据矩阵', 37, '', '/bi/report/matrix', '', 0, 1, '', 1585661854477, 1, 1634258546289, 1);
INSERT INTO `fs_member_menu` VALUES (37, '智能报表', '后台管理:商业智能:智能报表', 26, 'radar-chart', '/bi/report', '', 0, 1, '', 1585662611501, 1, 1634258458541, 1);
INSERT INTO `fs_member_menu` VALUES (38, '数据报表', '后台管理:商业智能:智能报表:数据报表', 37, '', '/bi/report/visualize', '', 0, 1, '', 1585663056291, 1, 1637658388654, 1);
INSERT INTO `fs_member_menu` VALUES (39, '数据大屏', '后台管理:商业智能:智能报表:数据大屏', 37, '', '/auto/layout/list', '', 0, 1, '', 1585663079511, 1, 1662456614598, 1);
INSERT INTO `fs_member_menu` VALUES (40, '人脸识别', '后台管理:人脸识别', 1, 'smile', '/face/index/index', '', 0, 1, '人脸检测、人脸识别、检索对比', 1597297964946, 1, 1597298791858, 1);
INSERT INTO `fs_member_menu` VALUES (41, '分组管理', '后台管理:人脸识别:分组管理', 40, 'team', '/face/group', '', 0, 1, '', 1597297984692, 1, 1598260929886, 1);
INSERT INTO `fs_member_menu` VALUES (42, '人员管理', '后台管理:人脸识别:人员管理', 40, 'user', '/face/user', '', 0, 1, '', 1597298848332, 1, 1598260938414, 1);
INSERT INTO `fs_member_menu` VALUES (43, '分组列表', '后台管理:人脸识别:分组管理:分组列表', 41, '', '/face/group/list', '', 0, 1, '', 1597298889503, 1, 1597298889503, 1);
INSERT INTO `fs_member_menu` VALUES (44, '人员列表', '后台管理:人脸识别:人员管理:人员列表', 42, '', '/face/user/list', '', 0, 1, '', 1597298938906, 1, 1597298938906, 1);
INSERT INTO `fs_member_menu` VALUES (45, '人像管理', '后台管理:人脸识别:人像管理', 40, 'picture', '/face/photo', '', 0, 1, '', 1598324193930, 1, 1598324327966, 1);
INSERT INTO `fs_member_menu` VALUES (46, '人像列表', '后台管理:人脸识别:人像管理:人像列表', 45, '', '/face/photo/list', '', 0, 1, '', 1598324353459, 1, 1598324353459, 1);
INSERT INTO `fs_member_menu` VALUES (47, '控制面板', '后台管理:人脸识别:控制面板', 40, 'block', '/face/dashboard', '', 1, 1, '', 1598405239096, 1, 1598405239096, 1);
INSERT INTO `fs_member_menu` VALUES (48, '人脸对比', '后台管理:人脸识别:控制面板:人脸对比', 47, '', '/face/dashboard/compare', '', 0, 1, '', 1598405279275, 1, 1598405279275, 1);
INSERT INTO `fs_member_menu` VALUES (49, '人脸检索', '后台管理:人脸识别:控制面板:人脸检索', 47, '', '/face/dashboard/search', '', 0, 1, '', 1598405321956, 1, 1598405321956, 1);
INSERT INTO `fs_member_menu` VALUES (50, '人脸检测', '后台管理:人脸识别:控制面板:人脸检测', 47, '', '/face/dashboard/detect', '', 1, 1, '', 1598408189380, 1, 1598408189380, 1);
INSERT INTO `fs_member_menu` VALUES (51, '搜索引擎', '后台管理:搜索引擎', 1, 'search', '/lucene/index/index', '', 0, 1, '词典管理、索引示例、服务重载', 1600481187938, 1, 1600481496017, 1);
INSERT INTO `fs_member_menu` VALUES (52, 'Elasticsearch', '后台管理:搜索引擎:Elasticsearch', 51, 'deployment-unit', '/lucene/elasticsearch', '', 0, 1, '', 1600481694000, 1, 1600481730276, 1);
INSERT INTO `fs_member_menu` VALUES (53, '索引示例', '后台管理:搜索引擎:Elasticsearch:索引示例', 52, '', '/lucene/elasticsearch/demo', '', 0, 1, '', 1600481950656, 1, 1600481950656, 1);
INSERT INTO `fs_member_menu` VALUES (54, '词典管理', '后台管理:搜索引擎:Elasticsearch:词典管理', 52, '', '/lucene/elasticsearch/dict', '', 0, -1, '', 1600481985836, 1, 1607514726426, 1);
INSERT INTO `fs_member_menu` VALUES (55, '服务重载', '后台管理:搜索引擎:Elasticsearch:服务重载', 52, '', '/lucene/elasticsearch/reload', '', 0, 1, '', 1600482059381, 1, 1600482059381, 1);
INSERT INTO `fs_member_menu` VALUES (56, '词库管理', '后台管理:搜索引擎:词库管理', 51, 'gold', '/lucene/dictionary', '', 0, 1, '', 1607333548750, 1, 1607334213102, 1);
INSERT INTO `fs_member_menu` VALUES (57, '词库列表', '后台管理:搜索引擎:词库管理:词库列表', 56, '', '/lucene/dictionary/list', '', 0, 1, '', 1607333724524, 1, 1607333865742, 1);
INSERT INTO `fs_member_menu` VALUES (58, '服务管理', '后台管理:服务管理', 1, 'box-plot', '/server/index/index', '', 0, 1, '管理项目基础服务', 1611814694744, 1, 1611814694744, 1);
INSERT INTO `fs_member_menu` VALUES (59, '消息队列', '后台管理:服务管理:消息队列', 58, 'hourglass', '/server/rabbit', '', 0, 1, '', 1611814821145, 1, 1611814821145, 1);
INSERT INTO `fs_member_menu` VALUES (60, '控制面板', '后台管理:服务管理:消息队列:控制面板', 59, '', '/server/rabbit/dashboard', '', 0, 1, '', 1611814876795, 1, 1611814876795, 1);
INSERT INTO `fs_member_menu` VALUES (61, '在线办公', '后台管理:在线办公', 1, 'snippets', '/oa/index/index', '', 0, 1, '表单设计、流程设计、在线审批', 1618451824790, 1, 1642382345449, 1);
INSERT INTO `fs_member_menu` VALUES (62, '表单管理', '后台管理:在线办公:表单管理', 61, 'file-protect', '/oa/form', '', 30, 1, '', 1618451979842, 1, 1622708492969, 1);
INSERT INTO `fs_member_menu` VALUES (63, '表单模型', '后台管理:在线办公:表单管理:表单模型', 62, '', '/oa/form/frame', '', 0, 1, '', 1618452012679, 1, 1618452012679, 1);
INSERT INTO `fs_member_menu` VALUES (64, '托管数据', '后台管理:在线办公:表单管理:托管数据', 62, '', '/oa/form/data', '', 0, 1, '', 1618452012679, 1, 1618452012679, 1);
INSERT INTO `fs_member_menu` VALUES (65, '校验规则', '后台管理:在线办公:表单管理:校验规则', 62, '', '/oa/form/regular', '', 0, 1, '', 1618452012679, 1, 1618452012679, 1);
INSERT INTO `fs_member_menu` VALUES (66, '字典管理', '后台管理:用户中心:字典管理', 5, 'book', '/member/dictionary', '', 0, 1, '', 1619510811715, 1, 1619510811715, 1);
INSERT INTO `fs_member_menu` VALUES (67, '字典列表', '后台管理:用户中心:字典管理:字典列表', 66, '', '/member/dictionary/list', '', 0, 1, '', 1619510988399, 1, 1619510988399, 1);
INSERT INTO `fs_member_menu` VALUES (68, '树形字典', '后台管理:用户中心:字典管理:树形字典', 66, '', '/member/dictionary/tree', '', 0, 1, '', 1619511018156, 1, 1619511018156, 1);
INSERT INTO `fs_member_menu` VALUES (69, '演示实例', '后台管理:演示实例', 1, 'alert', '/demo/index/index', '', 0, 1, '基础组件、功能演示、示例代码', 1620998687126, 1, 1620998687126, 1);
INSERT INTO `fs_member_menu` VALUES (70, '数据表格', '后台管理:演示实例:数据表格', 69, 'table', '/demo/table', '', 0, 1, '', 1620998817281, 1, 1620998817281, 1);
INSERT INTO `fs_member_menu` VALUES (71, '合并单元格', '后台管理:演示实例:数据表格:合并单元格', 70, '', '/demo/table/merge', '', 0, 1, '', 1620998844844, 1, 1620998844844, 1);
INSERT INTO `fs_member_menu` VALUES (72, '工作流程', '后台管理:在线办公:工作流程', 61, 'branches', '/oa/workflow', '', 20, 1, '', 1618451979842, 1, 1622708507924, 1);
INSERT INTO `fs_member_menu` VALUES (73, '流程模型', '后台管理:在线办公:工作流程:流程模型', 72, '', '/oa/workflow/list', '', 0, 1, '', 1618452012679, 1, 1618452012679, 1);
INSERT INTO `fs_member_menu` VALUES (74, '流程部署', '后台管理:在线办公:工作流程:流程部署', 72, '', '/oa/workflow/deployment', '', 0, 1, '', 1622703834598, 1, 1622703834598, 1);
INSERT INTO `fs_member_menu` VALUES (75, '流程审批', '后台管理:在线办公:流程审批', 61, 'solution', '/oa/approve', '', 50, 1, '', 1622708586117, 1, 1622709216210, 1);
INSERT INTO `fs_member_menu` VALUES (76, '新建单据', '后台管理:在线办公:流程审批:新建单据', 75, '', '/oa/approve/workflow', '', 0, 1, '', 1622708678092, 1, 1622709225897, 1);
INSERT INTO `fs_member_menu` VALUES (77, '待签任务', '后台管理:在线办公:流程审批:待签任务', 75, '', '/oa/approve/candidate', '', 0, 1, '', 1624612909138, 1, 1624842860376, 1);
INSERT INTO `fs_member_menu` VALUES (78, '待办任务', '后台管理:在线办公:流程审批:待办任务', 75, '', '/oa/approve/assignee', '', 0, 1, '', 1624852957141, 1, 1624852957141, 1);
INSERT INTO `fs_member_menu` VALUES (79, '历史任务', '后台管理:在线办公:流程审批:历史任务', 75, '', '/oa/approve/history', '', 0, 1, '', 1624869272153, 1, 1624869272153, 1);
INSERT INTO `fs_member_menu` VALUES (80, '流程管理', '后台管理:在线办公:工作流程:流程管理', 72, '', '/oa/workflow/history', '', 0, 1, '', 1624973275460, 1, 1624973275460, 1);
INSERT INTO `fs_member_menu` VALUES (81, '内容管理', '后台管理:内容管理', 1, 'solution', '/cms/index/index', '', 0, 1, 'CMS内容管理系统', 1627262640332, 1, 1627262740224, 1);
INSERT INTO `fs_member_menu` VALUES (82, '系统设置', '后台管理:内容管理:系统设置', 81, 'setting', '/cms/setting', '', 0, 1, '', 1627262811964, 1, 1627285193931, 1);
INSERT INTO `fs_member_menu` VALUES (83, '基础信息', '后台管理:内容管理:系统设置:基础信息', 82, '', '/cms/setting/profile', '', 0, 1, '', 1627262893812, 1, 1627285199014, 1);
INSERT INTO `fs_member_menu` VALUES (84, '导航菜单', '后台管理:内容管理:系统设置:导航菜单', 82, '', '/cms/setting/menu', '', 0, 1, '', 1627263054790, 1, 1627285202965, 1);
INSERT INTO `fs_member_menu` VALUES (85, '友情链接', '后台管理:内容管理:系统设置:友情链接', 82, '', '/cms/setting/link', '', 0, 1, '', 1627263117475, 1, 1627285206628, 1);
INSERT INTO `fs_member_menu` VALUES (86, '通知公告', '后台管理:内容管理:系统设置:通知公告', 82, '', '/cms/setting/notice', '', 0, 1, '', 1627263143731, 1, 1627285209571, 1);
INSERT INTO `fs_member_menu` VALUES (87, '首页轮播', '后台管理:内容管理:系统设置:首页轮播', 82, '', '/cms/setting/carousel', '', 0, 1, '', 1627263318494, 1, 1627285212587, 1);
INSERT INTO `fs_member_menu` VALUES (88, '内容维护', '后台管理:内容管理:内容维护', 81, 'global', '/cms/site', '', 0, 1, '', 1627285310300, 1, 1627285429530, 1);
INSERT INTO `fs_member_menu` VALUES (89, '栏目管理', '后台管理:内容管理:内容维护:栏目管理', 88, '', '/cms/site/catalog', '', 0, 1, '', 1627285552334, 1, 1627285552334, 1);
INSERT INTO `fs_member_menu` VALUES (90, '文章管理', '后台管理:内容管理:内容维护:文章管理', 88, '', '/cms/site/article', '', 0, 1, '', 1627285573517, 1, 1627285573517, 1);
INSERT INTO `fs_member_menu` VALUES (91, '评论管理', '后台管理:内容管理:内容维护:评论管理', 88, '', '/cms/site/comment', '', 0, 1, '', 1627285597402, 1, 1627285597402, 1);
INSERT INTO `fs_member_menu` VALUES (92, '留言反馈', '后台管理:内容管理:内容维护:留言反馈', 88, '', '/cms/site/feedback', '', 0, 1, '', 1627285644951, 1, 1627285644951, 1);
INSERT INTO `fs_member_menu` VALUES (93, '发布文章', '后台管理:内容管理:内容维护:发布文章', 88, '', '/cms/site/editor', '', 100, 1, '', 1627286496327, 1, 1628843173856, 1);
INSERT INTO `fs_member_menu` VALUES (94, '标签管理', '后台管理:内容管理:内容维护:标签管理', 88, '', '/cms/site/tag', '', 0, 1, '', 1627286834091, 1, 1627286834091, 1);
INSERT INTO `fs_member_menu` VALUES (95, '引用管理', '后台管理:内容管理:内容维护:引用管理', 88, '', '/cms/site/cite', '', 0, 1, '', 1627377188353, 1, 1627377188353, 1);
INSERT INTO `fs_member_menu` VALUES (96, '文件存储', '后台管理:文件存储', 1, 'hdd', '/file/index/index', '', 0, 1, '文件存储、图库图床、对象存储', 1627548117479, 1, 1627548117479, 1);
INSERT INTO `fs_member_menu` VALUES (97, '存档管理', '后台管理:文件存储:存档管理', 96, 'file', '/file/archive', '', 0, 1, '', 1627548162634, 1, 1627548162634, 1);
INSERT INTO `fs_member_menu` VALUES (98, '文件列表', '后台管理:文件存储:存档管理:文件列表', 97, '', '/file/archive/list', '', 0, 1, '', 1627548198271, 1, 1627548198271, 1);
INSERT INTO `fs_member_menu` VALUES (99, '项目管理', '后台管理:项目管理', 1, 'project', '/auto/index/index', '', 0, 1, '脚手架、页面设计、应用设计', 1639719591630, 1, 1662455201977, 1);
INSERT INTO `fs_member_menu` VALUES (100, '页面设计', '后台管理:项目管理:页面设计', 99, 'read', '/auto/layout', '', 0, 1, '', 1639719989845, 1, 1662455350983, 1);
INSERT INTO `fs_member_menu` VALUES (101, '应用管理', '后台管理:项目管理:应用管理', 99, 'book', '/auto/app', '', 0, 1, '', 1639720019045, 1, 1662455223175, 1);
INSERT INTO `fs_member_menu` VALUES (102, '数据治理', '后台管理:数据治理', 1, 'medicine-box', '/govern/index/index', '', 0, 1, '数据接入、元数据、集成同步', 1642381520827, 1, 1642381520827, 1);
INSERT INTO `fs_member_menu` VALUES (103, '元数据', '后台管理:数据治理:元数据', 102, 'container', '/govern/meta', '', 0, 1, '', 1642381648881, 1, 1648436130377, 1);
INSERT INTO `fs_member_menu` VALUES (104, '定时任务', '后台管理:服务管理:定时任务', 58, 'clock-circle', '/server/cron', '', 0, 1, '', 1642571306245, 1, 1642571306245, 1);
INSERT INTO `fs_member_menu` VALUES (105, '工作节点', '后台管理:服务管理:定时任务:工作节点', 104, '', '/server/cron/node', '', 0, 1, '', 1642571336578, 1, 1642571336578, 1);
INSERT INTO `fs_member_menu` VALUES (106, '作业管理', '后台管理:服务管理:定时任务:作业管理', 104, '', '/server/cron/job', '', 0, 1, '', 1642571363823, 1, 1642571363823, 1);
INSERT INTO `fs_member_menu` VALUES (107, '在线打印', '后台管理:在线办公:在线打印', 61, 'printer', '/oa/print', '', 20, 1, '', 1646376598407, 1, 1646376598407, 1);
INSERT INTO `fs_member_menu` VALUES (108, '模板列表', '后台管理:在线办公:在线打印:模板列表', 107, '', '/oa/print/list', '', 0, 1, '', 1646376622053, 1, 1646376622053, 1);
INSERT INTO `fs_member_menu` VALUES (109, '作业调度', '后台管理:服务管理:定时任务:作业调度', 104, '', '/server/cron/trigger', '', 0, 1, '', 1647236774840, 1, 1647236774840, 1);
INSERT INTO `fs_member_menu` VALUES (110, '任务编排', '后台管理:服务管理:定时任务:任务编排', 104, '', '/server/cron/flow', '', 0, 1, '', 1647311512665, 1, 1647311512665, 1);
INSERT INTO `fs_member_menu` VALUES (111, '数据标准', '后台管理:数据治理:数据标准', 102, 'exception', '/govern/standard', '', 0, 1, '', 1648435605072, 1, 1648436213331, 1);
INSERT INTO `fs_member_menu` VALUES (112, '数据资产', '后台管理:数据治理:数据资产', 102, 'database', '/govern/asset', '', 0, 1, '', 1648435651055, 1, 1648436139625, 1);
INSERT INTO `fs_member_menu` VALUES (113, '数据质量', '后台管理:数据治理:数据质量', 102, 'file-protect', '/govern/quality', '', 0, 1, '', 1648435669817, 1, 1648436224359, 1);
INSERT INTO `fs_member_menu` VALUES (114, '数据安全', '后台管理:数据治理:数据安全', 102, 'safety-certificate', '/govern/security', '', 0, 1, '', 1648435775952, 1, 1648436303957, 1);
INSERT INTO `fs_member_menu` VALUES (115, '数据交换', '后台管理:数据治理:数据交换', 102, 'cloud-sync', '/govern/exchange', '', 0, 1, '', 1648436340888, 1, 1648867473179, 1);
INSERT INTO `fs_member_menu` VALUES (116, '模型管理', '后台管理:数据治理:元数据:模型管理', 103, '', '/govern/meta/model', '', 0, 1, '', 1648782739796, 1, 1649821115608, 1);
INSERT INTO `fs_member_menu` VALUES (117, '模型关系', '后台管理:数据治理:元数据:模型关系', 103, '', '/govern/meta/modelRelation', '', 0, 1, '', 1649856291456, 1, 1649856291456, 1);
INSERT INTO `fs_member_menu` VALUES (118, '数据地图', '后台管理:数据治理:元数据:数据地图', 103, '', '/govern/meta/map', '', 99, 1, '', 1650760956687, 1, 1650760956687, 1);
INSERT INTO `fs_member_menu` VALUES (119, '系统设置', '后台管理:数据治理:系统设置', 102, 'solution', '/govern/system', '', 0, 1, '', 1651288300961, 1, 1651288300961, 1);
INSERT INTO `fs_member_menu` VALUES (120, '数据源', '后台管理:数据治理:系统设置:数据源', 119, '', '/govern/system/source', '', 0, 1, '', 1651288324135, 1, 1651288324135, 1);
INSERT INTO `fs_member_menu` VALUES (121, '标准管理', '后台管理:数据治理:数据标准:标准管理', 111, '', '/govern/standard/list', '', 0, 1, '', 1651369141111, 1, 1651369141111, 1);
INSERT INTO `fs_member_menu` VALUES (122, '落地评估', '后台管理:数据治理:数据标准:落地评估', 111, '', '/govern/standard/assess', '', 0, 1, '', 1651369188363, 1, 1651369188363, 1);
INSERT INTO `fs_member_menu` VALUES (123, '质检分类', '后台管理:数据治理:数据质量:质检分类', 113, '', '/govern/quality/logic', '', 0, 1, '', 1651369714750, 1, 1676537824487, 1);
INSERT INTO `fs_member_menu` VALUES (124, '评估结果', '后台管理:数据治理:数据标准:评估结果', 111, '', '/govern/standard/log', '', 0, 1, '', 1652682411082, 1, 1652682411082, 1);
INSERT INTO `fs_member_menu` VALUES (125, '调度日志', '后台管理:服务管理:定时任务:调度日志', 104, '', '/server/cron/flowLog', '', 0, 1, '', 1658735846363, 1, 1658735846363, 1);
INSERT INTO `fs_member_menu` VALUES (126, '质检规则', '后台管理:数据治理:数据质量:质检规则', 113, '', '/govern/quality/rule', '', 0, 1, '', 1660203491808, 1, 1660203491808, 1);
INSERT INTO `fs_member_menu` VALUES (127, '质检方案', '后台管理:数据治理:数据质量:质检方案', 113, '', '/govern/quality/plan', '', 0, 1, '', 1660203503955, 1, 1660203503955, 1);
INSERT INTO `fs_member_menu` VALUES (128, '质检报告', '后台管理:数据治理:数据质量:质检报告', 113, '', '/govern/quality/log', '', 0, 1, '', 1660203570855, 1, 1660203570855, 1);
INSERT INTO `fs_member_menu` VALUES (129, '页面布局', '后台管理:项目管理:页面管理:页面布局', 100, '', '/auto/layout/list', '', 0, 1, '', 1662455306154, 1, 1662455306154, 1);

-- ----------------------------
-- Table structure for fs_member_relation
-- ----------------------------
DROP TABLE IF EXISTS `fs_member_relation`;
CREATE TABLE `fs_member_relation`  (
  `id` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `type` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `aid` int NOT NULL DEFAULT 0,
  `bid` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of fs_member_relation
-- ----------------------------
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_1', 'role_menu', 1, 1);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_10', 'role_menu', 1, 10);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_100', 'role_menu', 1, 100);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_101', 'role_menu', 1, 101);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_102', 'role_menu', 1, 102);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_103', 'role_menu', 1, 103);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_104', 'role_menu', 1, 104);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_105', 'role_menu', 1, 105);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_106', 'role_menu', 1, 106);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_107', 'role_menu', 1, 107);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_108', 'role_menu', 1, 108);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_109', 'role_menu', 1, 109);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_11', 'role_menu', 1, 11);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_110', 'role_menu', 1, 110);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_111', 'role_menu', 1, 111);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_112', 'role_menu', 1, 112);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_113', 'role_menu', 1, 113);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_114', 'role_menu', 1, 114);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_115', 'role_menu', 1, 115);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_116', 'role_menu', 1, 116);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_117', 'role_menu', 1, 117);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_118', 'role_menu', 1, 118);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_119', 'role_menu', 1, 119);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_12', 'role_menu', 1, 12);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_120', 'role_menu', 1, 120);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_121', 'role_menu', 1, 121);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_122', 'role_menu', 1, 122);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_123', 'role_menu', 1, 123);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_124', 'role_menu', 1, 124);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_125', 'role_menu', 1, 125);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_126', 'role_menu', 1, 126);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_127', 'role_menu', 1, 127);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_128', 'role_menu', 1, 128);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_129', 'role_menu', 1, 129);
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
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_24', 'role_menu', 1, 24);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_25', 'role_menu', 1, 25);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_26', 'role_menu', 1, 26);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_27', 'role_menu', 1, 27);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_28', 'role_menu', 1, 28);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_29', 'role_menu', 1, 29);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_3', 'role_menu', 1, 3);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_30', 'role_menu', 1, 30);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_31', 'role_menu', 1, 31);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_32', 'role_menu', 1, 32);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_33', 'role_menu', 1, 33);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_34', 'role_menu', 1, 34);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_35', 'role_menu', 1, 35);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_36', 'role_menu', 1, 36);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_37', 'role_menu', 1, 37);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_38', 'role_menu', 1, 38);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_39', 'role_menu', 1, 39);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_4', 'role_menu', 1, 4);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_40', 'role_menu', 1, 40);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_41', 'role_menu', 1, 41);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_42', 'role_menu', 1, 42);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_43', 'role_menu', 1, 43);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_44', 'role_menu', 1, 44);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_45', 'role_menu', 1, 45);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_46', 'role_menu', 1, 46);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_47', 'role_menu', 1, 47);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_48', 'role_menu', 1, 48);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_49', 'role_menu', 1, 49);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_5', 'role_menu', 1, 5);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_50', 'role_menu', 1, 50);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_51', 'role_menu', 1, 51);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_52', 'role_menu', 1, 52);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_53', 'role_menu', 1, 53);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_54', 'role_menu', 1, 54);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_55', 'role_menu', 1, 55);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_56', 'role_menu', 1, 56);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_57', 'role_menu', 1, 57);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_58', 'role_menu', 1, 58);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_59', 'role_menu', 1, 59);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_6', 'role_menu', 1, 6);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_60', 'role_menu', 1, 60);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_61', 'role_menu', 1, 61);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_62', 'role_menu', 1, 62);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_63', 'role_menu', 1, 63);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_64', 'role_menu', 1, 64);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_65', 'role_menu', 1, 65);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_66', 'role_menu', 1, 66);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_67', 'role_menu', 1, 67);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_68', 'role_menu', 1, 68);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_69', 'role_menu', 1, 69);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_7', 'role_menu', 1, 7);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_70', 'role_menu', 1, 70);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_71', 'role_menu', 1, 71);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_72', 'role_menu', 1, 72);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_73', 'role_menu', 1, 73);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_74', 'role_menu', 1, 74);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_75', 'role_menu', 1, 75);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_76', 'role_menu', 1, 76);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_77', 'role_menu', 1, 77);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_78', 'role_menu', 1, 78);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_79', 'role_menu', 1, 79);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_8', 'role_menu', 1, 8);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_80', 'role_menu', 1, 80);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_81', 'role_menu', 1, 81);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_82', 'role_menu', 1, 82);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_83', 'role_menu', 1, 83);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_84', 'role_menu', 1, 84);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_85', 'role_menu', 1, 85);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_86', 'role_menu', 1, 86);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_87', 'role_menu', 1, 87);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_88', 'role_menu', 1, 88);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_89', 'role_menu', 1, 89);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_9', 'role_menu', 1, 9);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_90', 'role_menu', 1, 90);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_91', 'role_menu', 1, 91);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_92', 'role_menu', 1, 92);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_93', 'role_menu', 1, 93);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_94', 'role_menu', 1, 94);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_95', 'role_menu', 1, 95);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_96', 'role_menu', 1, 96);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_97', 'role_menu', 1, 97);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_98', 'role_menu', 1, 98);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_99', 'role_menu', 1, 99);
INSERT INTO `fs_member_relation` VALUES ('role_menu_2_1', 'role_menu', 2, 1);
INSERT INTO `fs_member_relation` VALUES ('role_menu_2_2', 'role_menu', 2, 2);
INSERT INTO `fs_member_relation` VALUES ('role_menu_2_3', 'role_menu', 2, 3);
INSERT INTO `fs_member_relation` VALUES ('role_menu_3_1', 'role_menu', 3, 1);
INSERT INTO `fs_member_relation` VALUES ('role_menu_3_16', 'role_menu', 3, 16);
INSERT INTO `fs_member_relation` VALUES ('role_menu_3_18', 'role_menu', 3, 18);
INSERT INTO `fs_member_relation` VALUES ('role_menu_3_9', 'role_menu', 3, 9);
INSERT INTO `fs_member_relation` VALUES ('role_menu_4_16', 'role_menu', 4, 16);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_1', 'role_resource', 1, 1);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_10', 'role_resource', 1, 10);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_100', 'role_resource', 1, 100);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_101', 'role_resource', 1, 101);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_102', 'role_resource', 1, 102);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_103', 'role_resource', 1, 103);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_104', 'role_resource', 1, 104);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_105', 'role_resource', 1, 105);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_106', 'role_resource', 1, 106);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_107', 'role_resource', 1, 107);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_108', 'role_resource', 1, 108);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_109', 'role_resource', 1, 109);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_11', 'role_resource', 1, 11);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_110', 'role_resource', 1, 110);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_111', 'role_resource', 1, 111);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_112', 'role_resource', 1, 112);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_113', 'role_resource', 1, 113);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_114', 'role_resource', 1, 114);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_115', 'role_resource', 1, 115);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_116', 'role_resource', 1, 116);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_117', 'role_resource', 1, 117);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_118', 'role_resource', 1, 118);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_119', 'role_resource', 1, 119);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_12', 'role_resource', 1, 12);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_120', 'role_resource', 1, 120);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_121', 'role_resource', 1, 121);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_122', 'role_resource', 1, 122);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_123', 'role_resource', 1, 123);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_124', 'role_resource', 1, 124);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_125', 'role_resource', 1, 125);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_126', 'role_resource', 1, 126);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_127', 'role_resource', 1, 127);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_128', 'role_resource', 1, 128);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_129', 'role_resource', 1, 129);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_13', 'role_resource', 1, 13);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_130', 'role_resource', 1, 130);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_131', 'role_resource', 1, 131);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_132', 'role_resource', 1, 132);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_133', 'role_resource', 1, 133);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_134', 'role_resource', 1, 134);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_135', 'role_resource', 1, 135);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_136', 'role_resource', 1, 136);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_137', 'role_resource', 1, 137);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_138', 'role_resource', 1, 138);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_139', 'role_resource', 1, 139);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_14', 'role_resource', 1, 14);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_140', 'role_resource', 1, 140);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_141', 'role_resource', 1, 141);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_142', 'role_resource', 1, 142);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_143', 'role_resource', 1, 143);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_144', 'role_resource', 1, 144);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_145', 'role_resource', 1, 145);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_146', 'role_resource', 1, 146);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_147', 'role_resource', 1, 147);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_148', 'role_resource', 1, 148);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_149', 'role_resource', 1, 149);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_15', 'role_resource', 1, 15);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_150', 'role_resource', 1, 150);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_151', 'role_resource', 1, 151);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_152', 'role_resource', 1, 152);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_153', 'role_resource', 1, 153);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_154', 'role_resource', 1, 154);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_155', 'role_resource', 1, 155);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_156', 'role_resource', 1, 156);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_157', 'role_resource', 1, 157);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_158', 'role_resource', 1, 158);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_159', 'role_resource', 1, 159);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_16', 'role_resource', 1, 16);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_160', 'role_resource', 1, 160);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_161', 'role_resource', 1, 161);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_162', 'role_resource', 1, 162);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_163', 'role_resource', 1, 163);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_164', 'role_resource', 1, 164);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_165', 'role_resource', 1, 165);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_166', 'role_resource', 1, 166);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_167', 'role_resource', 1, 167);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_168', 'role_resource', 1, 168);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_169', 'role_resource', 1, 169);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_17', 'role_resource', 1, 17);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_170', 'role_resource', 1, 170);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_171', 'role_resource', 1, 171);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_172', 'role_resource', 1, 172);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_173', 'role_resource', 1, 173);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_174', 'role_resource', 1, 174);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_175', 'role_resource', 1, 175);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_176', 'role_resource', 1, 176);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_177', 'role_resource', 1, 177);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_178', 'role_resource', 1, 178);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_179', 'role_resource', 1, 179);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_18', 'role_resource', 1, 18);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_180', 'role_resource', 1, 180);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_181', 'role_resource', 1, 181);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_182', 'role_resource', 1, 182);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_183', 'role_resource', 1, 183);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_184', 'role_resource', 1, 184);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_185', 'role_resource', 1, 185);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_186', 'role_resource', 1, 186);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_187', 'role_resource', 1, 187);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_188', 'role_resource', 1, 188);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_189', 'role_resource', 1, 189);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_19', 'role_resource', 1, 19);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_190', 'role_resource', 1, 190);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_191', 'role_resource', 1, 191);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_192', 'role_resource', 1, 192);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_193', 'role_resource', 1, 193);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_194', 'role_resource', 1, 194);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_195', 'role_resource', 1, 195);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_196', 'role_resource', 1, 196);
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
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_59', 'role_resource', 1, 59);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_6', 'role_resource', 1, 6);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_60', 'role_resource', 1, 60);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_61', 'role_resource', 1, 61);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_62', 'role_resource', 1, 62);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_63', 'role_resource', 1, 63);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_64', 'role_resource', 1, 64);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_65', 'role_resource', 1, 65);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_66', 'role_resource', 1, 66);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_67', 'role_resource', 1, 67);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_68', 'role_resource', 1, 68);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_69', 'role_resource', 1, 69);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_7', 'role_resource', 1, 7);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_70', 'role_resource', 1, 70);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_71', 'role_resource', 1, 71);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_72', 'role_resource', 1, 72);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_73', 'role_resource', 1, 73);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_74', 'role_resource', 1, 74);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_75', 'role_resource', 1, 75);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_76', 'role_resource', 1, 76);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_77', 'role_resource', 1, 77);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_78', 'role_resource', 1, 78);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_79', 'role_resource', 1, 79);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_8', 'role_resource', 1, 8);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_80', 'role_resource', 1, 80);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_81', 'role_resource', 1, 81);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_82', 'role_resource', 1, 82);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_83', 'role_resource', 1, 83);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_84', 'role_resource', 1, 84);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_85', 'role_resource', 1, 85);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_86', 'role_resource', 1, 86);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_87', 'role_resource', 1, 87);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_88', 'role_resource', 1, 88);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_89', 'role_resource', 1, 89);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_9', 'role_resource', 1, 9);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_90', 'role_resource', 1, 90);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_91', 'role_resource', 1, 91);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_92', 'role_resource', 1, 92);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_93', 'role_resource', 1, 93);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_94', 'role_resource', 1, 94);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_95', 'role_resource', 1, 95);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_96', 'role_resource', 1, 96);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_97', 'role_resource', 1, 97);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_98', 'role_resource', 1, 98);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_99', 'role_resource', 1, 99);
INSERT INTO `fs_member_relation` VALUES ('role_resource_2_1', 'role_resource', 2, 1);
INSERT INTO `fs_member_relation` VALUES ('role_resource_2_2', 'role_resource', 2, 2);
INSERT INTO `fs_member_relation` VALUES ('role_resource_2_3', 'role_resource', 2, 3);
INSERT INTO `fs_member_relation` VALUES ('role_resource_2_4', 'role_resource', 2, 4);
INSERT INTO `fs_member_relation` VALUES ('role_resource_2_5', 'role_resource', 2, 5);
INSERT INTO `fs_member_relation` VALUES ('role_resource_2_6', 'role_resource', 2, 6);
INSERT INTO `fs_member_relation` VALUES ('role_resource_3_1', 'role_resource', 3, 1);
INSERT INTO `fs_member_relation` VALUES ('role_resource_3_2', 'role_resource', 3, 2);
INSERT INTO `fs_member_relation` VALUES ('role_resource_3_8', 'role_resource', 3, 8);
INSERT INTO `fs_member_relation` VALUES ('role_resource_4_8', 'role_resource', 4, 8);
INSERT INTO `fs_member_relation` VALUES ('user_role_1_1', 'user_role', 1, 1);
INSERT INTO `fs_member_relation` VALUES ('user_role_2_2', 'user_role', 2, 2);

-- ----------------------------
-- Table structure for fs_member_resource
-- ----------------------------
DROP TABLE IF EXISTS `fs_member_resource`;
CREATE TABLE `fs_member_resource`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `full_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `parent_id` int NOT NULL DEFAULT 0,
  `module` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `controller` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `action` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `sort` tinyint NOT NULL DEFAULT 0,
  `status` tinyint NOT NULL DEFAULT 0,
  `description` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `created_time` bigint NOT NULL DEFAULT 0,
  `created_uid` int NOT NULL DEFAULT 0,
  `updated_time` bigint NOT NULL DEFAULT 0,
  `updated_uid` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 197 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of fs_member_resource
-- ----------------------------
INSERT INTO `fs_member_resource` VALUES (1, '后台管理', '后台管理', 0, 'admin', '', '', 0, 1, '是否可访问后台', 1584534838948, 1, 1584535453295, 1);
INSERT INTO `fs_member_resource` VALUES (2, '用户中心', '用户中心', 0, 'member', '', '', 0, 1, '', 1528081670164, 1, 1584590104125, 1);
INSERT INTO `fs_member_resource` VALUES (3, '菜单', '用户中心:菜单', 2, 'member', 'menu', '', 0, 1, '', 1528081696180, 1, 1584590110517, 1);
INSERT INTO `fs_member_resource` VALUES (4, '资源', '用户中心:资源', 2, 'member', 'resource', '', 0, 1, '', 1528081721468, 1, 1584590139693, 1);
INSERT INTO `fs_member_resource` VALUES (5, '角色', '用户中心:角色', 2, 'member', 'role', '', 0, 1, '', 1528081764195, 1, 1584590159965, 1);
INSERT INTO `fs_member_resource` VALUES (6, '用户', '用户中心:用户', 2, 'member', 'user', '', 0, 1, '', 1528081790426, 1, 1584590183845, 1);
INSERT INTO `fs_member_resource` VALUES (7, '配置', '用户中心:配置', 2, 'member', 'setting', '', 0, 1, '', 1528081858994, 1, 1584590241289, 1);
INSERT INTO `fs_member_resource` VALUES (8, '添加', '用户中心:菜单:添加', 3, 'member', 'menu', 'add', 0, 1, '', 1528081911866, 1, 1584590115052, 1);
INSERT INTO `fs_member_resource` VALUES (9, '修改', '用户中心:菜单:修改', 3, 'member', 'menu', 'modify', 0, 1, '', 1528081955009, 1, 1584590127509, 1);
INSERT INTO `fs_member_resource` VALUES (10, '删除', '用户中心:菜单:删除', 3, 'member', 'menu', 'delete', 0, 1, '', 1528081985121, 1, 1584590135133, 1);
INSERT INTO `fs_member_resource` VALUES (11, '添加', '用户中心:资源:添加', 4, 'member', 'resource', 'add', 0, 1, '', 1528082024771, 1, 1584590144698, 1);
INSERT INTO `fs_member_resource` VALUES (12, '修改', '用户中心:资源:修改', 4, 'member', 'resource', 'modify', 0, 1, '', 1528082042903, 1, 1584590150312, 1);
INSERT INTO `fs_member_resource` VALUES (13, '删除', '用户中心:资源:删除', 4, 'member', 'resource', 'delete', 0, 1, '', 1528082084243, 1, 1584590155309, 1);
INSERT INTO `fs_member_resource` VALUES (14, '添加', '用户中心:角色:添加', 5, 'member', 'role', 'add', 0, 1, '', 1528082104404, 1, 1584590165347, 1);
INSERT INTO `fs_member_resource` VALUES (15, '修改', '用户中心:角色:修改', 5, 'member', 'role', 'modify', 0, 1, '', 1528082198733, 1, 1584590169694, 1);
INSERT INTO `fs_member_resource` VALUES (16, '删除', '用户中心:角色:删除', 5, 'member', 'role', 'delete', 0, 1, '', 1528082214265, 1, 1584590174794, 1);
INSERT INTO `fs_member_resource` VALUES (17, '菜单', '用户中心:角色:菜单', 5, 'member', 'role', 'menu', 0, 1, '', 1528082271647, 1, 1584590180046, 1);
INSERT INTO `fs_member_resource` VALUES (18, '资源', '用户中心:角色:资源', 5, 'member', 'role', 'resource', 0, 1, '', 1528082432853, 1, 1584590193894, 1);
INSERT INTO `fs_member_resource` VALUES (19, '添加', '用户中心:用户:添加', 6, 'member', 'user', 'add', 0, 1, '', 1528082465188, 1, 1584590202725, 1);
INSERT INTO `fs_member_resource` VALUES (20, '修改', '用户中心:用户:修改', 6, 'member', 'user', 'modify', 0, 1, '', 1528082492740, 1, 1584590224552, 1);
INSERT INTO `fs_member_resource` VALUES (21, '删除', '用户中心:用户:删除', 6, 'member', 'user', 'delete', 0, 1, '', 1528082514205, 1, 1584590230942, 1);
INSERT INTO `fs_member_resource` VALUES (22, '角色', '用户中心:用户:角色', 6, 'member', 'user', 'role', 0, 1, '', 1528082586393, 1, 1584590236190, 1);
INSERT INTO `fs_member_resource` VALUES (23, '添加', '用户中心:配置:添加', 7, 'member', 'setting', 'add', 0, 1, '', 1528082616922, 1, 1584590245882, 1);
INSERT INTO `fs_member_resource` VALUES (24, '修改', '用户中心:配置:修改', 7, 'member', 'setting', 'modify', 0, 1, '', 1528082642509, 1, 1584590249855, 1);
INSERT INTO `fs_member_resource` VALUES (25, '删除', '用户中心:配置:删除', 7, 'member', 'setting', 'delete', 0, 1, '', 1528082662922, 1, 1584590258533, 1);
INSERT INTO `fs_member_resource` VALUES (26, '网页爬虫', '网页爬虫', 0, 'spider', '', '', 0, 1, '', 1585195421330, 1, 1585195421330, 1);
INSERT INTO `fs_member_resource` VALUES (27, '模板', '网页爬虫:模板', 26, 'spider', 'template', '', 0, 1, '', 1585195446408, 1, 1585195446408, 1);
INSERT INTO `fs_member_resource` VALUES (28, '删除', '网页爬虫:模板:删除', 27, 'spider', 'template', 'delete', 0, 1, '', 1585195509597, 1, 1585195533509, 1);
INSERT INTO `fs_member_resource` VALUES (29, '修改', '网页爬虫:模板:修改', 27, 'spider', 'template', 'modify', 0, 1, '', 1585195523181, 1, 1585195523181, 1);
INSERT INTO `fs_member_resource` VALUES (30, '添加', '网页爬虫:模板:添加', 27, 'spider', 'template', 'add', 0, 1, '', 1585216567413, 1, 1585216567413, 1);
INSERT INTO `fs_member_resource` VALUES (31, '商业智能', '商业智能', 0, 'bi', '', '', 0, 1, '', 1585384071227, 1, 1631754471053, 1);
INSERT INTO `fs_member_resource` VALUES (32, '数据加工', '商业智能:数据加工', 31, 'bi', 'diagram', '', 0, 1, '', 1585384104596, 1, 1631754486815, 1);
INSERT INTO `fs_member_resource` VALUES (33, '添加', '商业智能:数据加工:添加', 32, 'bi', 'diagram', 'add', 0, 1, '', 1585384138739, 1, 1631754508516, 1);
INSERT INTO `fs_member_resource` VALUES (34, '修改', '商业智能:数据加工:修改', 32, 'bi', 'diagram', 'modify', 0, 1, '', 1585384152067, 1, 1631754517030, 1);
INSERT INTO `fs_member_resource` VALUES (35, '删除', '商业智能:数据加工:删除', 32, 'bi', 'diagram', 'delete', 0, 1, '', 1585384168964, 1, 1631754529772, 1);
INSERT INTO `fs_member_resource` VALUES (36, '数据源', '商业智能:数据源', 31, 'bi', 'source', '', 0, 1, '', 1585669452789, 1, 1634614816541, 1);
INSERT INTO `fs_member_resource` VALUES (37, '添加', '商业智能:数据源:添加', 36, 'bi', 'source', 'add', 0, 1, '', 1585669473979, 1, 1634614829860, 1);
INSERT INTO `fs_member_resource` VALUES (38, '修改', '商业智能:数据源:修改', 36, 'bi', 'source', 'modify', 0, 1, '', 1585669487729, 1, 1634614838196, 1);
INSERT INTO `fs_member_resource` VALUES (39, '删除', '商业智能:数据源:删除', 36, 'bi', 'source', 'delete', 0, 1, '', 1585669499607, 1, 1634614850447, 1);
INSERT INTO `fs_member_resource` VALUES (40, '数据集', '商业智能:数据集', 31, 'bi', 'dataset', '', 0, 1, '', 1585727898953, 1, 1634614884425, 1);
INSERT INTO `fs_member_resource` VALUES (41, '添加', '商业智能:数据集:添加', 40, 'bi', 'dataset', 'add', 0, 1, '', 1585727916428, 1, 1634614892519, 1);
INSERT INTO `fs_member_resource` VALUES (42, '修改', '商业智能:数据集:修改', 40, 'bi', 'dataset', 'modify', 0, 1, '', 1585727959930, 1, 1634614899268, 1);
INSERT INTO `fs_member_resource` VALUES (43, '删除', '商业智能:数据集:删除', 40, 'bi', 'dataset', 'delete', 0, 1, '', 1585727972170, 1, 1634614913436, 1);
INSERT INTO `fs_member_resource` VALUES (44, '人脸识别', '人脸识别', 0, 'face', '', '', 0, 1, '', 1597299393466, 1, 1597299393466, 1);
INSERT INTO `fs_member_resource` VALUES (45, '分组', '人脸识别:分组', 44, 'face', 'group', '', 0, 1, '', 1597299426164, 1, 1597299426164, 1);
INSERT INTO `fs_member_resource` VALUES (46, '人员', '人脸识别:人员', 44, 'face', 'user', '', 0, 1, '', 1597299441594, 1, 1597299441594, 1);
INSERT INTO `fs_member_resource` VALUES (47, '添加', '人脸识别:分组:添加', 45, 'face', 'group', 'add', 0, 1, '', 1597299463846, 1, 1597299463846, 1);
INSERT INTO `fs_member_resource` VALUES (48, '修改', '人脸识别:分组:修改', 45, 'face', 'group', 'modify', 0, 1, '', 1597299487386, 1, 1597299487386, 1);
INSERT INTO `fs_member_resource` VALUES (49, '删除', '人脸识别:分组:删除', 45, 'face', 'group', 'delete', 0, 1, '', 1597299507470, 1, 1597299507470, 1);
INSERT INTO `fs_member_resource` VALUES (50, '添加', '人脸识别:人员:添加', 46, 'face', 'user', 'add', 0, 1, '', 1597299532434, 1, 1597299532434, 1);
INSERT INTO `fs_member_resource` VALUES (51, '修改', '人脸识别:人员:修改', 46, 'face', 'user', 'modify', 0, 1, '', 1597299549779, 1, 1597299549779, 1);
INSERT INTO `fs_member_resource` VALUES (52, '删除', '人脸识别:人员:删除', 46, 'face', 'user', 'delete', 0, 1, '', 1597299561750, 1, 1597299561750, 1);
INSERT INTO `fs_member_resource` VALUES (53, '分组', '人脸识别:人员:分组', 46, 'face', 'user', 'group', 0, 1, '', 1598238834654, 1, 1598238834654, 1);
INSERT INTO `fs_member_resource` VALUES (54, '人像', '人脸识别:人像', 44, 'face', 'photo', '', 0, 1, '', 1598324393176, 1, 1598324393176, 1);
INSERT INTO `fs_member_resource` VALUES (55, '添加', '人脸识别:人像:添加', 54, 'face', 'photo', 'add', 0, 1, '', 1598324413298, 1, 1598324413298, 1);
INSERT INTO `fs_member_resource` VALUES (56, '修改', '人脸识别:人像:修改', 54, 'face', 'photo', 'modify', 0, 1, '', 1598324427504, 1, 1598324427504, 1);
INSERT INTO `fs_member_resource` VALUES (57, '删除', '人脸识别:人像:删除', 54, 'face', 'photo', 'delete', 0, 1, '', 1598324443162, 1, 1598324443162, 1);
INSERT INTO `fs_member_resource` VALUES (58, '搜索引擎', '搜索引擎', 0, 'lucene', '', '', 0, 1, '', 1607333327044, 1, 1607333327044, 1);
INSERT INTO `fs_member_resource` VALUES (59, '词库', '搜索引擎:词库', 58, 'lucene', 'dictionary', '', 0, 1, '', 1607333381289, 1, 1607333426485, 1);
INSERT INTO `fs_member_resource` VALUES (60, '添加', '搜索引擎:词库:添加', 59, 'lucene', 'dictionary', 'add', 0, 1, '', 1607333455146, 1, 1607333455146, 1);
INSERT INTO `fs_member_resource` VALUES (61, '修改', '搜索引擎:词库:修改', 59, 'lucene', 'dictionary', 'modify', 0, 1, '', 1607333470362, 1, 1607333470362, 1);
INSERT INTO `fs_member_resource` VALUES (62, '删除', '搜索引擎:词库:删除', 59, 'lucene', 'dictionary', 'delete', 0, 1, '', 1607333488826, 1, 1607333488826, 1);
INSERT INTO `fs_member_resource` VALUES (63, '在线办公', '在线办公', 0, 'oa', '', '', 0, 1, '', 1618446294833, 1, 1618446294833, 1);
INSERT INTO `fs_member_resource` VALUES (64, '表单模型', '在线办公:表单模型', 63, 'oa', 'formFrame', '', 0, 1, '', 1618446338026, 1, 1618446338026, 1);
INSERT INTO `fs_member_resource` VALUES (65, '添加', '在线办公:表单模型:添加', 64, 'oa', 'formFrame', 'add', 0, 1, '', 1618446365063, 1, 1618446365063, 1);
INSERT INTO `fs_member_resource` VALUES (66, '修改', '在线办公:表单模型:修改', 64, 'oa', 'formFrame', 'modify', 0, 1, '', 1618446377800, 1, 1618446377800, 1);
INSERT INTO `fs_member_resource` VALUES (67, '删除', '在线办公:表单模型:删除', 64, 'oa', 'formFrame', 'delete', 0, 1, '', 1618446390411, 1, 1618446390411, 1);
INSERT INTO `fs_member_resource` VALUES (68, '表单数据', '在线办公:表单数据', 63, 'oa', 'formData', '', 0, 1, '', 1618446338026, 1, 1618446338026, 1);
INSERT INTO `fs_member_resource` VALUES (69, '添加', '在线办公:表单数据:添加', 68, 'oa', 'formData', 'add', 0, 1, '', 1618446365063, 1, 1618446365063, 1);
INSERT INTO `fs_member_resource` VALUES (70, '修改', '在线办公:表单数据:修改', 68, 'oa', 'formData', 'modify', 0, 1, '', 1618446377800, 1, 1618446377800, 1);
INSERT INTO `fs_member_resource` VALUES (71, '删除', '在线办公:表单数据:删除', 68, 'oa', 'formData', 'delete', 0, 1, '', 1618446390411, 1, 1618446390411, 1);
INSERT INTO `fs_member_resource` VALUES (72, '校验规则', '在线办公:校验规则', 63, 'oa', 'formRegular', '', 0, 1, '', 1618446338026, 1, 1618446338026, 1);
INSERT INTO `fs_member_resource` VALUES (73, '添加', '在线办公:校验规则:添加', 72, 'oa', 'formRegular', 'add', 0, 1, '', 1618446365063, 1, 1618446365063, 1);
INSERT INTO `fs_member_resource` VALUES (74, '修改', '在线办公:校验规则:修改', 72, 'oa', 'formRegular', 'modify', 0, 1, '', 1618446377800, 1, 1618446377800, 1);
INSERT INTO `fs_member_resource` VALUES (75, '删除', '在线办公:校验规则:删除', 72, 'oa', 'formRegular', 'delete', 0, 1, '', 1618446390411, 1, 1618446390411, 1);
INSERT INTO `fs_member_resource` VALUES (76, '字典', '用户中心:字典', 2, 'member', 'dictionary', '', 0, 1, '', 1528081696180, 1, 1619512081161, 1);
INSERT INTO `fs_member_resource` VALUES (77, '添加', '用户中心:字典:添加', 76, 'member', 'dictionary', 'add', 0, 1, '', 1528081911866, 1, 1584590115052, 1);
INSERT INTO `fs_member_resource` VALUES (78, '修改', '用户中心:字典:修改', 76, 'member', 'dictionary', 'modify', 0, 1, '', 1528081955009, 1, 1584590127509, 1);
INSERT INTO `fs_member_resource` VALUES (79, '删除', '用户中心:字典:删除', 76, 'member', 'dictionary', 'delete', 0, 1, '', 1528081985121, 1, 1584590135133, 1);
INSERT INTO `fs_member_resource` VALUES (80, '工作流程', '在线办公:工作流程', 63, 'oa', 'workflow', '', 0, 1, '', 1618446338026, 1, 1618446338026, 1);
INSERT INTO `fs_member_resource` VALUES (81, '添加', '在线办公:工作流程:添加', 80, 'oa', 'workflow', 'add', 0, 1, '', 1618446365063, 1, 1618446365063, 1);
INSERT INTO `fs_member_resource` VALUES (82, '修改', '在线办公:工作流程:修改', 80, 'oa', 'workflow', 'modify', 0, 1, '', 1618446377800, 1, 1618446377800, 1);
INSERT INTO `fs_member_resource` VALUES (83, '删除', '在线办公:工作流程:删除', 80, 'oa', 'workflow', 'delete', 0, 1, '', 1618446390411, 1, 1618446390411, 1);
INSERT INTO `fs_member_resource` VALUES (84, '发布', '在线办公:工作流程:发布', 80, 'oa', 'workflow', 'publish', 0, 1, '', 1622633473668, 1, 1622633473668, 1);
INSERT INTO `fs_member_resource` VALUES (85, '检索部署', '在线办公:工作流程:检索部署', 80, 'oa', 'workflow', 'searchDeployment', 0, 1, '', 1622703892647, 1, 1622703990430, 1);
INSERT INTO `fs_member_resource` VALUES (86, '删除部署', '在线办公:工作流程:删除部署', 80, 'oa', 'workflow', 'deleteDeployment', 0, 1, '', 1622703934998, 1, 1622703973335, 1);
INSERT INTO `fs_member_resource` VALUES (87, '流程审批', '在线办公:流程审批', 63, 'oa', 'approve', '', 0, 1, '', 1622708972781, 1, 1622708972781, 1);
INSERT INTO `fs_member_resource` VALUES (88, '新建单据', '在线办公:流程审批:新建单据', 87, 'oa', 'approve', 'workflow', 0, 1, '', 1622709172273, 1, 1622709172273, 1);
INSERT INTO `fs_member_resource` VALUES (89, '检索历史', '在线办公:工作流程:检索历史', 80, 'oa', 'workflow', 'searchHistory', 0, 1, '', 1624973449494, 1, 1624973449494, 1);
INSERT INTO `fs_member_resource` VALUES (90, '流程详情', '在线办公:工作流程:流程详情', 80, 'oa', 'workflow', 'process', 0, 1, '', 1625030518787, 1, 1625030518787, 1);
INSERT INTO `fs_member_resource` VALUES (91, '撤销流程', '在线办公:工作流程:撤销流程', 80, 'oa', 'workflow', 'deleteProcessInstance', 0, 1, '', 1625033323075, 1, 1625033323075, 1);
INSERT INTO `fs_member_resource` VALUES (92, '删除流程', '在线办公:工作流程:删除流程', 80, 'oa', 'workflow', 'deleteHistoricProcessInstance', 0, 1, '', 1625033349461, 1, 1625033349461, 1);
INSERT INTO `fs_member_resource` VALUES (93, '挂起流程', '在线办公:工作流程:挂起流程', 80, 'oa', 'workflow', 'suspendProcessInstance', 0, 1, '', 1625040566076, 1, 1625040566076, 1);
INSERT INTO `fs_member_resource` VALUES (94, '激活流程', '在线办公:工作流程:激活流程', 80, 'oa', 'workflow', 'activateProcessInstance', 0, 1, '', 1625040586330, 1, 1625040586330, 1);
INSERT INTO `fs_member_resource` VALUES (95, '驳回流程', '在线办公:工作流程:驳回流程', 80, 'oa', 'workflow', 'reject', 0, 1, '', 1625122337903, 1, 1625122337903, 1);
INSERT INTO `fs_member_resource` VALUES (96, '内容管理', '内容管理', 0, 'cms', '', '', 0, 1, '', 1627462111659, 1, 1627462111659, 1);
INSERT INTO `fs_member_resource` VALUES (97, '配置', '内容管理:配置', 96, 'cms', 'setting', '', 0, 1, '', 1627462153168, 1, 1627462300400, 1);
INSERT INTO `fs_member_resource` VALUES (98, '载入', '内容管理:配置:载入', 97, 'cms', 'setting', 'load', 0, 1, '', 1627462649215, 1, 1627462649215, 1);
INSERT INTO `fs_member_resource` VALUES (99, '更改', '内容管理:配置:更改', 97, 'cms', 'setting', 'change', 0, 1, '', 1627462669393, 1, 1627462669393, 1);
INSERT INTO `fs_member_resource` VALUES (100, '文件存储', '文件存储', 0, 'file', '', '', 0, 1, '', 1627547763814, 1, 1627547763814, 1);
INSERT INTO `fs_member_resource` VALUES (101, '存档', '文件存储:存档', 100, 'file', 'archive', '', 0, 1, '', 1627547824301, 1, 1627547824301, 1);
INSERT INTO `fs_member_resource` VALUES (102, '更改', '文件存储:存档:更改', 101, 'file', 'archive', 'save', 0, 1, '', 1627547864010, 1, 1627547864010, 1);
INSERT INTO `fs_member_resource` VALUES (103, '删除', '文件存储:存档:删除', 101, 'file', 'archive', 'delete', 0, 1, '', 1627547881301, 1, 1627547881301, 1);
INSERT INTO `fs_member_resource` VALUES (104, '上传', '文件存储:存档:上传', 101, 'file', 'archive', 'upload', 0, 1, '', 1628069668474, 1, 1628069668474, 1);
INSERT INTO `fs_member_resource` VALUES (105, '下载', '文件存储:存档:下载', 101, 'file', 'archive', 'download', 0, 1, '', 1628069685308, 1, 1628069685308, 1);
INSERT INTO `fs_member_resource` VALUES (106, '栏目', '内容管理:栏目', 96, 'cms', 'catalog', '', 0, 1, '', 1628496213702, 1, 1628497159085, 1);
INSERT INTO `fs_member_resource` VALUES (107, '添加', '内容管理:栏目:添加', 106, 'cms', 'catalog', 'add', 0, 1, '', 1628496242603, 1, 1628497164867, 1);
INSERT INTO `fs_member_resource` VALUES (108, '修改', '内容管理:栏目:修改', 106, 'cms', 'catalog', 'modify', 0, 1, '', 1628496253964, 1, 1628497170688, 1);
INSERT INTO `fs_member_resource` VALUES (109, '删除', '内容管理:栏目:删除', 106, 'cms', 'catalog', 'delete', 0, 1, '', 1628496265504, 1, 1628497177984, 1);
INSERT INTO `fs_member_resource` VALUES (110, '文章', '内容管理:文章', 96, 'cms', 'article', '', 0, 1, '', 1628649304363, 1, 1628649304363, 1);
INSERT INTO `fs_member_resource` VALUES (111, '添加', '内容管理:文章:添加', 110, 'cms', 'article', 'add', 0, 1, '', 1628649318326, 1, 1628649318326, 1);
INSERT INTO `fs_member_resource` VALUES (112, '修改', '内容管理:文章:修改', 110, 'cms', 'article', 'modify', 0, 1, '', 1628649333630, 1, 1628649333630, 1);
INSERT INTO `fs_member_resource` VALUES (113, '删除', '内容管理:文章:删除', 110, 'cms', 'article', 'delete', 0, 1, '', 1628649349958, 1, 1628649349958, 1);
INSERT INTO `fs_member_resource` VALUES (114, '评论', '内容管理:评论', 96, 'cms', 'comment', '', 0, 1, '', 1628760363465, 1, 1628760363465, 1);
INSERT INTO `fs_member_resource` VALUES (115, '审核', '内容管理:评论:审核', 114, 'cms', 'comment', 'audit', 0, 1, '', 1628760381768, 1, 1628760381768, 1);
INSERT INTO `fs_member_resource` VALUES (116, '删除', '内容管理:评论:删除', 114, 'cms', 'comment', 'delete', 0, 1, '', 1628760391045, 1, 1628760391045, 1);
INSERT INTO `fs_member_resource` VALUES (117, '反馈', '内容管理:反馈', 96, 'cms', 'feedback', '', 0, 1, '', 1628838812386, 1, 1628838812386, 1);
INSERT INTO `fs_member_resource` VALUES (118, '审核', '内容管理:反馈:审核', 117, 'cms', 'feedback', 'audit', 0, 1, '', 1628838828851, 1, 1628838828851, 1);
INSERT INTO `fs_member_resource` VALUES (119, '删除', '内容管理:反馈:删除', 117, 'cms', 'feedback', 'delete', 0, 1, '', 1628838840154, 1, 1628838840154, 1);
INSERT INTO `fs_member_resource` VALUES (120, '标签', '内容管理:标签', 96, 'cms', 'tag', '', 0, 1, '', 1628841563164, 1, 1628841563164, 1);
INSERT INTO `fs_member_resource` VALUES (121, '引用', '内容管理:引用', 96, 'cms', 'cite', '', 0, 1, '', 1628841573794, 1, 1628841593925, 1);
INSERT INTO `fs_member_resource` VALUES (122, '添加', '内容管理:标签:添加', 120, 'cms', 'tag', 'add', 0, 1, '', 1628841604527, 1, 1628841604527, 1);
INSERT INTO `fs_member_resource` VALUES (123, '修改', '内容管理:标签:修改', 120, 'cms', 'tag', 'modify', 0, 1, '', 1628841615952, 1, 1628842280445, 1);
INSERT INTO `fs_member_resource` VALUES (124, '删除', '内容管理:标签:删除', 120, 'cms', 'tag', 'delete', 0, 1, '', 1628841629948, 1, 1628841629948, 1);
INSERT INTO `fs_member_resource` VALUES (125, '添加', '内容管理:引用:添加', 121, 'cms', 'cite', 'add', 0, 1, '', 1628841639288, 1, 1628841639288, 1);
INSERT INTO `fs_member_resource` VALUES (126, '修改', '内容管理:引用:修改', 121, 'cms', 'cite', 'modify', 0, 1, '', 1628841651912, 1, 1628841651912, 1);
INSERT INTO `fs_member_resource` VALUES (127, '删除', '内容管理:引用:删除', 121, 'cms', 'cite', 'delete', 0, 1, '', 1628841661763, 1, 1628841661763, 1);
INSERT INTO `fs_member_resource` VALUES (128, '结构', '商业智能:数据源:结构', 36, 'bi', 'source', 'schema', 0, 1, '', 1636543399133, 1, 1636543399133, 1);
INSERT INTO `fs_member_resource` VALUES (129, '检索', '商业智能:数据集:检索', 40, 'bi', 'dataset', 'search', 0, 1, '', 1637313631642, 1, 1637313631642, 1);
INSERT INTO `fs_member_resource` VALUES (130, '数据报表', '商业智能:数据报表', 31, 'bi', 'visualize', '', 0, 1, '', 1637658795954, 1, 1637658795954, 1);
INSERT INTO `fs_member_resource` VALUES (131, '添加', '商业智能:数据报表:添加', 130, 'bi', 'visualize', 'add', 0, 1, '', 1637658815910, 1, 1637658815910, 1);
INSERT INTO `fs_member_resource` VALUES (132, '修改', '商业智能:数据报表:修改', 130, 'bi', 'visualize', 'modify', 0, 1, '', 1637658833371, 1, 1637658833371, 1);
INSERT INTO `fs_member_resource` VALUES (133, '删除', '商业智能:数据报表:删除', 130, 'bi', 'visualize', 'delete', 0, 1, '', 1637658848750, 1, 1637658848750, 1);
INSERT INTO `fs_member_resource` VALUES (134, '页面设计', '项目管理:页面设计', 188, 'auto', 'layout', '', 0, 1, '', 1637658872174, 1, 1662455123717, 1);
INSERT INTO `fs_member_resource` VALUES (135, '添加', '项目管理:页面设计:添加', 134, 'auto', 'layout', 'add', 0, 1, '', 1637658885131, 1, 1662455137977, 1);
INSERT INTO `fs_member_resource` VALUES (136, '修改', '项目管理:页面设计:修改', 134, 'auto', 'layout', 'modify', 0, 1, '', 1637658899570, 1, 1662455152100, 1);
INSERT INTO `fs_member_resource` VALUES (137, '删除', '项目管理:页面设计:删除', 134, 'auto', 'layout', 'delete', 0, 1, '', 1637658911014, 1, 1662455164640, 1);
INSERT INTO `fs_member_resource` VALUES (138, '检索', '商业智能:数据报表:检索', 130, 'bi', 'visualize', 'search', 0, 1, '', 1637741322686, 1, 1637741322686, 1);
INSERT INTO `fs_member_resource` VALUES (139, '数据矩阵', '商业智能:数据矩阵', 31, 'bi', 'matrix', '', 0, 1, '', 1637886974028, 1, 1637886974028, 1);
INSERT INTO `fs_member_resource` VALUES (140, '添加', '商业智能:数据矩阵:添加', 139, 'bi', 'matrix', 'add', 0, 1, '', 1637886991599, 1, 1637886991599, 1);
INSERT INTO `fs_member_resource` VALUES (141, '修改', '商业智能:数据矩阵:修改', 139, 'bi', 'matrix', 'modify', 0, 1, '', 1637887005890, 1, 1637887005890, 1);
INSERT INTO `fs_member_resource` VALUES (142, '删除', '商业智能:数据矩阵:删除', 139, 'bi', 'matrix', 'delete', 0, 1, '', 1637887016065, 1, 1637887016065, 1);
INSERT INTO `fs_member_resource` VALUES (143, '检索', '商业智能:数据矩阵:检索', 139, 'bi', 'matrix', 'search', 0, 1, '', 1637887027205, 1, 1637887027205, 1);
INSERT INTO `fs_member_resource` VALUES (144, '在线打印', '在线办公:在线打印', 63, 'oa', 'print', '', 0, 1, '', 1646376682020, 1, 1646376682020, 1);
INSERT INTO `fs_member_resource` VALUES (145, '添加', '在线办公:在线打印:添加', 144, 'oa', 'print', 'add', 0, 1, '', 1646376706771, 1, 1646376706771, 1);
INSERT INTO `fs_member_resource` VALUES (146, '修改', '在线办公:在线打印:修改', 144, 'oa', 'print', 'modify', 0, 1, '', 1646376719565, 1, 1646376719565, 1);
INSERT INTO `fs_member_resource` VALUES (147, '删除', '在线办公:在线打印:删除', 144, 'oa', 'print', 'delete', 0, 1, '', 1646376730601, 1, 1646376730601, 1);
INSERT INTO `fs_member_resource` VALUES (148, '定时任务', '定时任务', 0, 'cron', '', '', 0, 1, '', 1646977729063, 1, 1646977729063, 1);
INSERT INTO `fs_member_resource` VALUES (149, '作业', '定时任务:作业', 148, 'cron', 'job', '', 0, 1, '', 1646977746619, 1, 1646977756985, 1);
INSERT INTO `fs_member_resource` VALUES (150, '添加', '定时任务:作业:添加', 149, 'cron', 'job', 'add', 0, 1, '', 1646977779484, 1, 1646977779484, 1);
INSERT INTO `fs_member_resource` VALUES (151, '修改', '定时任务:作业:修改', 149, 'cron', 'job', 'modify', 0, 1, '', 1646977789678, 1, 1646977789678, 1);
INSERT INTO `fs_member_resource` VALUES (152, '删除', '定时任务:作业:删除', 149, 'cron', 'job', 'delete', 0, 1, '', 1646977800237, 1, 1646977809644, 1);
INSERT INTO `fs_member_resource` VALUES (153, '流程', '定时任务:流程', 148, 'cron', 'flow', '', 0, 1, '', 1647311564613, 1, 1647311564613, 1);
INSERT INTO `fs_member_resource` VALUES (154, '添加', '定时任务:流程:添加', 153, 'cron', 'flow', 'add', 0, 1, '', 1647311575198, 1, 1647311575198, 1);
INSERT INTO `fs_member_resource` VALUES (155, '修改', '定时任务:流程:修改', 153, 'cron', 'flow', 'modify', 0, 1, '', 1647311596734, 1, 1647311885637, 1);
INSERT INTO `fs_member_resource` VALUES (156, '删除', '定时任务:流程:删除', 153, 'cron', 'flow', 'delete', 0, 1, '', 1647311896713, 1, 1647311896713, 1);
INSERT INTO `fs_member_resource` VALUES (157, '数据治理', '数据治理', 0, 'govern', '', '', 0, 1, '', 1648782552696, 1, 1648782565072, 1);
INSERT INTO `fs_member_resource` VALUES (158, '模型', '数据治理:模型', 157, 'govern', 'model', '', 0, 1, '', 1648782582351, 1, 1649821237598, 1);
INSERT INTO `fs_member_resource` VALUES (159, '添加', '数据治理:模型:添加', 158, 'govern', 'model', 'add', 0, 1, '', 1648782597292, 1, 1649821250741, 1);
INSERT INTO `fs_member_resource` VALUES (160, '修改', '数据治理:模型:修改', 158, 'govern', 'model', 'modify', 0, 1, '', 1648782610469, 1, 1649821275807, 1);
INSERT INTO `fs_member_resource` VALUES (161, '删除', '数据治理:模型:删除', 158, 'govern', 'model', 'delete', 0, 1, '', 1648782620556, 1, 1649821283653, 1);
INSERT INTO `fs_member_resource` VALUES (162, '模型关系', '数据治理:模型关系', 157, 'govern', 'modelRelation', '', 0, 1, '', 1649856184604, 1, 1649856184604, 1);
INSERT INTO `fs_member_resource` VALUES (163, '添加', '数据治理:模型关系:添加', 162, 'govern', 'modelRelation', 'add', 0, 1, '', 1649856205906, 1, 1649856205906, 1);
INSERT INTO `fs_member_resource` VALUES (164, '修改', '数据治理:模型关系:修改', 162, 'govern', 'modelRelation', 'modify', 0, 1, '', 1649856221977, 1, 1649856221977, 1);
INSERT INTO `fs_member_resource` VALUES (165, '删除', '数据治理:模型关系:删除', 162, 'govern', 'modelRelation', 'delete', 0, 1, '', 1649856238307, 1, 1649856238307, 1);
INSERT INTO `fs_member_resource` VALUES (166, '数据源', '数据治理:数据源', 157, 'govern', 'source', '', 0, 1, '', 1651288127259, 1, 1651288127259, 1);
INSERT INTO `fs_member_resource` VALUES (167, '添加', '数据治理:数据源:添加', 166, 'govern', 'source', 'add', 0, 1, '', 1651288142907, 1, 1651288142907, 1);
INSERT INTO `fs_member_resource` VALUES (168, '修改', '数据治理:数据源:修改', 166, 'govern', 'source', 'modify', 0, 1, '', 1651288155369, 1, 1651288155369, 1);
INSERT INTO `fs_member_resource` VALUES (169, '删除', '数据治理:数据源:删除', 166, 'govern', 'source', 'delete', 0, 1, '', 1651288169410, 1, 1651288169410, 1);
INSERT INTO `fs_member_resource` VALUES (170, '标准', '数据治理:标准', 157, 'govern', 'standard', '', 0, 1, '', 1651369369718, 1, 1651369369718, 1);
INSERT INTO `fs_member_resource` VALUES (171, '添加', '数据治理:标准:添加', 170, 'govern', 'standard', 'add', 0, 1, '', 1651369379754, 1, 1651369379754, 1);
INSERT INTO `fs_member_resource` VALUES (172, '修改', '数据治理:标准:修改', 170, 'govern', 'standard', 'modify', 0, 1, '', 1651369391457, 1, 1651369391457, 1);
INSERT INTO `fs_member_resource` VALUES (173, '删除', '数据治理:标准:删除', 170, 'govern', 'standard', 'delete', 0, 1, '', 1651369400862, 1, 1651369400862, 1);
INSERT INTO `fs_member_resource` VALUES (174, '落地评估', '数据治理:落地评估', 157, 'govern', 'assess', '', 0, 1, '', 1651369440554, 1, 1651369440554, 1);
INSERT INTO `fs_member_resource` VALUES (175, '添加', '数据治理:落地评估:添加', 174, 'govern', 'assess', 'add', 0, 1, '', 1651369470508, 1, 1651369470508, 1);
INSERT INTO `fs_member_resource` VALUES (176, '修改', '数据治理:落地评估:修改', 174, 'govern', 'assess', 'modify', 0, 1, '', 1651369483410, 1, 1651369483410, 1);
INSERT INTO `fs_member_resource` VALUES (177, '删除', '数据治理:落地评估:删除', 174, 'govern', 'assess', 'delete', 0, 1, '', 1651369492457, 1, 1651369492457, 1);
INSERT INTO `fs_member_resource` VALUES (178, '质检分类', '数据治理:质检分类', 157, 'govern', 'qualityLogic', '', 0, 1, '', 1651369515566, 1, 1676538075225, 1);
INSERT INTO `fs_member_resource` VALUES (179, '添加', '数据治理:质检分类:添加', 178, 'govern', 'qualityLogic', 'add', 0, 1, '', 1651369527926, 1, 1676538079211, 1);
INSERT INTO `fs_member_resource` VALUES (180, '修改', '数据治理:质检分类:修改', 178, 'govern', 'qualityLogic', 'modify', 0, 1, '', 1651369543376, 1, 1676538082296, 1);
INSERT INTO `fs_member_resource` VALUES (181, '删除', '数据治理:质检分类:删除', 178, 'govern', 'qualityLogic', 'delete', 0, 1, '', 1651369553877, 1, 1676538085082, 1);
INSERT INTO `fs_member_resource` VALUES (182, '质检规则', '数据治理:质检规则', 157, 'govern', 'qualityRule', '', 0, 1, '', 1651369579119, 1, 1676538112547, 1);
INSERT INTO `fs_member_resource` VALUES (183, '添加', '数据治理:质检规则:添加', 182, 'govern', 'qualityRule', 'add', 0, 1, '', 1651369589073, 1, 1676538118982, 1);
INSERT INTO `fs_member_resource` VALUES (184, '修改', '数据治理:质检规则:修改', 182, 'govern', 'qualityRule', 'modify', 0, 1, '', 1651369599391, 1, 1676538123271, 1);
INSERT INTO `fs_member_resource` VALUES (185, '删除', '数据治理:质检规则:删除', 182, 'govern', 'qualityRule', 'delete', 0, 1, '', 1651369607940, 1, 1676538128949, 1);
INSERT INTO `fs_member_resource` VALUES (186, '评估结果', '数据治理:落地评估:评估结果', 174, 'govern', 'assess', 'log', 0, 1, '', 1652748865504, 1, 1652748865504, 1);
INSERT INTO `fs_member_resource` VALUES (187, '删除日志', '数据治理:落地评估:删除日志', 174, 'govern', 'assess', 'clear', 0, 1, '', 1652748880199, 1, 1652748880199, 1);
INSERT INTO `fs_member_resource` VALUES (188, '项目管理', '项目管理', 0, 'auto', '', '', 0, 1, '', 1662455086472, 1, 1662455086472, 1);
INSERT INTO `fs_member_resource` VALUES (189, '质检方案', '数据治理:质检方案', 157, 'govern', 'qualityPlan', '', 0, 1, '', 1676855898630, 1, 1676855898630, 1);
INSERT INTO `fs_member_resource` VALUES (190, '质检报告', '数据治理:质检方案:质检报告', 189, 'govern', 'qualityPlan', 'log', 0, 1, '', 1676855950410, 1, 1676858921681, 1);
INSERT INTO `fs_member_resource` VALUES (191, '添加', '数据治理:质检方案:添加', 189, 'govern', 'qualityPlan', 'add', 0, 1, '', 1651369589073, 1, 1676538118982, 1);
INSERT INTO `fs_member_resource` VALUES (192, '修改', '数据治理:质检方案:修改', 189, 'govern', 'qualityPlan', 'modify', 0, 1, '', 1651369599391, 1, 1676538123271, 1);
INSERT INTO `fs_member_resource` VALUES (193, '删除', '数据治理:质检方案:删除', 189, 'govern', 'qualityPlan', 'delete', 0, 1, '', 1651369607940, 1, 1676538128949, 1);
INSERT INTO `fs_member_resource` VALUES (194, '删除日志', '数据治理:质检方案:删除日志', 189, 'govern', 'qualityPlan', 'clear', 0, 1, '', 1651369589073, 1, 1676858947132, 1);
INSERT INTO `fs_member_resource` VALUES (195, '应用设计', '项目管理:应用设计', 188, 'app', 'app', '', 0, 1, '', 1651369599391, 1, 1676859114485, 1);
INSERT INTO `fs_member_resource` VALUES (196, '管理', '项目管理:应用设计:管理', 195, 'auto', 'app', 'manage', 0, 1, '', 1651369607940, 1, 1676859157557, 1);

-- ----------------------------
-- Table structure for fs_member_role
-- ----------------------------
DROP TABLE IF EXISTS `fs_member_role`;
CREATE TABLE `fs_member_role`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `sort` tinyint NOT NULL DEFAULT 0,
  `status` tinyint NOT NULL DEFAULT 0,
  `description` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `created_time` bigint NOT NULL DEFAULT 0,
  `created_uid` int NOT NULL DEFAULT 0,
  `updated_time` bigint NOT NULL DEFAULT 0,
  `updated_uid` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of fs_member_role
-- ----------------------------
INSERT INTO `fs_member_role` VALUES (1, '后台管理', 0, 1, '', 1528081589495, 1, 1528266877684, 1);
INSERT INTO `fs_member_role` VALUES (2, '普通用户', 0, 1, '', 1528081606670, 1, 1528081606670, 1);
INSERT INTO `fs_member_role` VALUES (3, 'aaa', 0, -1, '', 1584514294259, 1, 1584515444090, 1);
INSERT INTO `fs_member_role` VALUES (4, 'aaaxx', 0, 2, '', 1584515454033, 1, 1584515461772, 1);

-- ----------------------------
-- Table structure for fs_member_setting
-- ----------------------------
DROP TABLE IF EXISTS `fs_member_setting`;
CREATE TABLE `fs_member_setting`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `type` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `content` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `sort` tinyint NOT NULL DEFAULT 0,
  `description` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `updated_time` bigint NOT NULL DEFAULT 0,
  `updated_uid` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uniq_type_name`(`type` ASC, `name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 42 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of fs_member_setting
-- ----------------------------
INSERT INTO `fs_member_setting` VALUES (1, 'defaultPassword', 'member', 'password', 1, '默认登录密码', 1627009079201, 1);
INSERT INTO `fs_member_setting` VALUES (3, 'menuParentId', 'admin', '1', 1, '管理后台菜单根节点ID', 1627009055076, 1);
INSERT INTO `fs_member_setting` VALUES (4, 'siteName', 'cms', 'IISquare.com', 0, '站点名称', 1627354687313, 1);
INSERT INTO `fs_member_setting` VALUES (5, 'keywords', 'cms', 'fs-project,iisquare,iisquare.com,开源,免费', 0, '关键词', 1627354693783, 1);
INSERT INTO `fs_member_setting` VALUES (6, 'description', 'cms', 'fs-project Copyright by IISquare.com', 0, '站点描述', 1627354700189, 1);
INSERT INTO `fs_member_setting` VALUES (7, 'commentDisabled', 'cms', 'false', 0, '禁用评论', 1627354707526, 1);
INSERT INTO `fs_member_setting` VALUES (8, 'homeTitle', 'cms', '个人博客', 0, '首页标题', 1627354713093, 1);
INSERT INTO `fs_member_setting` VALUES (29, 'menu', 'cms', '[{\"name\":\"首页\",\"title\":\"\",\"href\":\"/\",\"target\":\"\",\"disabled\":false,\"children\":[]},{\"name\":\"栏目一\",\"title\":\"\",\"href\":\"\",\"target\":\"\",\"disabled\":false,\"children\":[{\"name\":\"子栏目一\",\"title\":\"\",\"href\":\"\",\"target\":\"\",\"disabled\":false,\"children\":[]},{\"name\":\"子栏目二\",\"title\":\"\",\"href\":\"\",\"target\":\"\",\"disabled\":false,\"children\":[{\"name\":\"孙栏目一\",\"title\":\"\",\"href\":\"\",\"target\":\"\",\"disabled\":false,\"children\":[]},{\"name\":\"孙栏目二\",\"title\":\"\",\"href\":\"\",\"target\":\"\",\"disabled\":false,\"children\":[]}]},{\"name\":\"子栏目三\",\"title\":\"\",\"href\":\"\",\"target\":\"\",\"disabled\":false,\"children\":[]}]},{\"name\":\"栏目二\",\"title\":\"\",\"href\":\"\",\"target\":\"\",\"disabled\":false,\"children\":[]},{\"name\":\"栏目三\",\"title\":\"\",\"href\":\"\",\"target\":\"\",\"disabled\":false,\"children\":[]}]', 0, '导航菜单', 1627461740399, 1);
INSERT INTO `fs_member_setting` VALUES (39, 'link', 'cms', '[]', 0, '友情链接', 1628123781321, 1);
INSERT INTO `fs_member_setting` VALUES (40, 'notice', 'cms', '[]', 0, '通知公告', 1628123790033, 1);
INSERT INTO `fs_member_setting` VALUES (41, 'carousel', 'cms', '[]', 0, '首页轮播图', 1628123760775, 1);

-- ----------------------------
-- Table structure for fs_member_user
-- ----------------------------
DROP TABLE IF EXISTS `fs_member_user`;
CREATE TABLE `fs_member_user`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `serial` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `name` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `password` char(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `salt` char(4) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `sort` tinyint NOT NULL DEFAULT 0,
  `status` tinyint NOT NULL DEFAULT 0,
  `description` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `created_time` bigint NOT NULL DEFAULT 0,
  `created_ip` varchar(16) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `created_uid` int NOT NULL DEFAULT 0,
  `updated_time` bigint NOT NULL DEFAULT 0,
  `updated_uid` int NOT NULL DEFAULT 0,
  `logined_time` bigint NOT NULL DEFAULT 0,
  `logined_ip` varchar(16) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `locked_time` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `unq_serial`(`serial` ASC) USING BTREE,
  INDEX `idx_name`(`name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 25 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of fs_member_user
-- ----------------------------
INSERT INTO `fs_member_user` VALUES (1, 'admin', '管理员', 'fc7911b5108d30e087f8881b90368679', '5231', 0, 1, '', 1528081552985, '127.0.0.1', 1, 1528081552985, 1, 1676452330488, '127.0.0.1', 0);
INSERT INTO `fs_member_user` VALUES (2, 'test', '测试123', '4b361be828611add84453a24f39772a5', '0905', 0, 1, '', 1528081567988, '127.0.0.1', 1, 1542958281919, 1, 1528267171953, '127.0.0.1', 0);
INSERT INTO `fs_member_user` VALUES (3, '111', '111', '', '', 6, -1, '', 0, '', 0, 1658215959314, 1, 0, '', 0);
INSERT INTO `fs_member_user` VALUES (4, '222', '222', '', '', 0, 1, '', 0, '', 0, 1629363081609, 1, 0, '', 1630054276000);
INSERT INTO `fs_member_user` VALUES (5, '333', '333', '', '', 0, 1, '', 0, '', 0, 1606803617105, 1, 0, '', 0);
INSERT INTO `fs_member_user` VALUES (6, '444', '444', '', '', 0, 1, '', 0, '', 0, 0, 0, 0, '', 0);
INSERT INTO `fs_member_user` VALUES (7, '555', '555', '', '', 0, 1, '', 0, '', 0, 0, 0, 0, '', 0);
INSERT INTO `fs_member_user` VALUES (8, '666', '666', '', '', 0, 1, '', 0, '', 0, 0, 0, 0, '', 0);
INSERT INTO `fs_member_user` VALUES (9, '777', '777', '', '', 0, 1, '', 0, '', 0, 0, 0, 0, '', 0);
INSERT INTO `fs_member_user` VALUES (10, '888', '888', '', '', 0, 1, '', 0, '', 0, 0, 0, 0, '', 0);
INSERT INTO `fs_member_user` VALUES (11, '999', '999', '', '', 0, 1, '', 0, '', 0, 0, 0, 0, '', 0);
INSERT INTO `fs_member_user` VALUES (12, '124124', '124124', '', '', 0, 1, '', 0, '', 0, 0, 0, 0, '', 0);
INSERT INTO `fs_member_user` VALUES (13, '55555', '55555', '', '', 0, 1, '', 0, '', 0, 0, 0, 0, '', 0);
INSERT INTO `fs_member_user` VALUES (14, '777777', '777777', '', '', 0, 1, '', 0, '', 0, 0, 0, 0, '', 0);
INSERT INTO `fs_member_user` VALUES (15, '444444', '444444', '', '', 0, 0, ' ', 0, '', 0, 0, 0, 0, '', 0);
INSERT INTO `fs_member_user` VALUES (16, '90909', '9090909', '', '', 0, 0, '  ', 0, '', 0, 0, 0, 0, '', 0);
INSERT INTO `fs_member_user` VALUES (17, 'ttt', 'ttt', '31b063d036577dcf2f39b2859ffae762', '0639', 0, 1, '', 1645774049481, '127.0.0.1', 1, 1645774049481, 1, 0, '', 0);
INSERT INTO `fs_member_user` VALUES (18, 'gfdd', 'hfghfgh', 'b5fd16f3a2d74f05c8a1f5527cc206be', '1394', 0, 1, '', 1645774323385, '127.0.0.1', 1, 1645774323385, 1, 0, '', 0);
INSERT INTO `fs_member_user` VALUES (20, 'xxxx', 'xxx', '31b063d036577dcf2f39b2859ffae762', '0639', 0, 1, '', 1645774049481, '127.0.0.1', 1, 1645774049481, 1, 0, '', 0);
INSERT INTO `fs_member_user` VALUES (21, 'fsdf', 'sdgfsdg', '03789c4092774d1fb04fe87f1f9b4d93', '1284', 0, 1, '', 1645776653434, '127.0.0.1', 1, 1645776653434, 1, 0, '', 0);
INSERT INTO `fs_member_user` VALUES (22, 'sdg', 'sdg', '6f424ef6baddab83375741935b6d425a', '4932', 0, 1, '', 1645776911255, '127.0.0.1', 1, 1645776911255, 1, 0, '', 0);
INSERT INTO `fs_member_user` VALUES (23, 'sdg4', 'sdg4', 'fdb917c956cf2c96819ed4ddf3730816', '3605', 0, 1, '', 1645777390270, '127.0.0.1', 1, 1645777390270, 1, 0, '', 0);
INSERT INTO `fs_member_user` VALUES (24, 'xxsssxx', 'xsssxx', '31b063d036577dcf2f39b2859ffae762', '0639', 0, 1, '', 1645774049481, '127.0.0.1', 1, 1645774049481, 1, 0, '', 0);

SET FOREIGN_KEY_CHECKS = 1;
