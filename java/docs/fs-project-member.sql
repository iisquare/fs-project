-- MySQL dump 10.13  Distrib 5.5.62, for Win64 (AMD64)
--
-- Host: localhost    Database: fs_project
-- ------------------------------------------------------
-- Server version	8.0.21

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `fs_member_menu`
--

DROP TABLE IF EXISTS `fs_member_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `fs_member_menu` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `full_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `application_id` int NOT NULL DEFAULT '0',
  `parent_id` int NOT NULL DEFAULT '0',
  `icon` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `url` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `target` varchar(8) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `sort` tinyint NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '0',
  `description` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `created_time` bigint NOT NULL DEFAULT '0',
  `created_uid` int NOT NULL DEFAULT '0',
  `updated_time` bigint NOT NULL DEFAULT '0',
  `updated_uid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_application_id` (`application_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=134 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fs_member_menu`
--

LOCK TABLES `fs_member_menu` WRITE;
/*!40000 ALTER TABLE `fs_member_menu` DISABLE KEYS */;
INSERT INTO `fs_member_menu` VALUES (3,'仪表盘','后台管理:仪表盘',1,0,'Briefcase','/dashboard','',0,1,'',1584599943309,1,1735873792352,1);
INSERT INTO `fs_member_menu` VALUES (4,'工作台','后台管理:仪表盘:工作台',1,3,'','/dashboard/workplace','',0,1,'',1584600104943,1,1735875975648,1);
INSERT INTO `fs_member_menu` VALUES (6,'用户管理','后台管理:用户中心:用户管理',2,0,'user','/member/user','',0,1,'',1584600290726,1,1585384888303,1);
INSERT INTO `fs_member_menu` VALUES (7,'用户列表','后台管理:用户中心:用户管理:用户列表',2,6,'','/member/user/list','',0,1,'',1584600340060,1,1585384893566,1);
INSERT INTO `fs_member_menu` VALUES (8,'角色管理','后台管理:用户中心:角色管理',2,0,'team','/member/role','',0,1,'',1584600399810,1,1585384902123,1);
INSERT INTO `fs_member_menu` VALUES (9,'资源管理','后台管理:用户中心:资源管理',2,0,'file-protect','/member/resource','',0,1,'',1584600461951,1,1585384945709,1);
INSERT INTO `fs_member_menu` VALUES (10,'菜单管理','后台管理:用户中心:菜单管理',2,0,'link','/member/menu','',0,1,'',1584600513666,1,1585384966047,1);
INSERT INTO `fs_member_menu` VALUES (11,'配置管理','后台管理:用户中心:配置管理',2,0,'setting','/member/setting','',0,1,'',1584600543641,1,1585384981710,1);
INSERT INTO `fs_member_menu` VALUES (12,'角色列表','后台管理:用户中心:角色管理:角色列表',2,8,'','/member/role/list','',0,1,'',1584600568015,1,1585384939982,1);
INSERT INTO `fs_member_menu` VALUES (13,'菜单列表','后台管理:用户中心:菜单管理:菜单列表',2,10,'','/member/menu/list','',0,1,'',1584600581275,1,1585384970598,1);
INSERT INTO `fs_member_menu` VALUES (14,'资源列表','后台管理:用户中心:资源管理:资源列表',2,9,'','/member/resource/list','',0,1,'',1584600602600,1,1585384951171,1);
INSERT INTO `fs_member_menu` VALUES (15,'树形资源','后台管理:用户中心:资源管理:树形资源',2,9,'','/member/resource/tree','',0,-1,'',1584600614965,1,1732157382299,1);
INSERT INTO `fs_member_menu` VALUES (16,'树形菜单','后台管理:用户中心:菜单管理:树形菜单',2,10,'','/member/menu/tree','',0,-1,'',1584600858740,1,1732157382299,1);
INSERT INTO `fs_member_menu` VALUES (17,'配置列表','后台管理:用户中心:配置管理:配置列表',2,11,'','/member/setting/list','',0,1,'',1584600875558,1,1585384987639,1);
INSERT INTO `fs_member_menu` VALUES (18,'个人中心','后台管理:个人中心',1,0,'UserFilled','/account','',0,1,'',1584601522091,1,1735874319316,1);
INSERT INTO `fs_member_menu` VALUES (19,'个人信息','后台首页:个人中心:个人信息',1,18,'','/account/profile','',0,1,'',1584601559031,1,1735874283677,1);
INSERT INTO `fs_member_menu` VALUES (20,'修改密码','后台首页:个人中心:修改密码',1,18,'','/account/password','',0,1,'',1584601575033,1,1584601575033,1);
INSERT INTO `fs_member_menu` VALUES (22,'模板管理','后台管理:网页爬虫:模板管理',26,0,'code','/spider/template','',0,1,'',1585195263054,1,1585195263054,1);
INSERT INTO `fs_member_menu` VALUES (23,'模板列表','后台管理:网页爬虫:模板管理:模板列表',26,22,'','/spider/template/list','',0,1,'',1585195284371,1,1585195284371,1);
INSERT INTO `fs_member_menu` VALUES (24,'节点管理','后台管理:网页爬虫:节点管理',26,0,'cloud-server','/spider/crawler','',0,1,'',1585195333832,1,1585195333832,1);
INSERT INTO `fs_member_menu` VALUES (25,'节点面板','后台管理:网页爬虫:节点管理:节点面板',26,24,'','/spider/crawler/dashboard','',0,1,'',1585195373342,1,1585195373342,1);
INSERT INTO `fs_member_menu` VALUES (27,'数据加工','后台管理:商业智能:数据加工',31,0,'apartment','/bi/diagram','',0,1,'',1585384219002,1,1634258659883,1);
INSERT INTO `fs_member_menu` VALUES (28,'清洗规则','后台管理:商业智能:数据加工:清洗规则',31,27,'','/bi/diagram/list','',0,1,'',1585384240550,1,1637656893932,1);
INSERT INTO `fs_member_menu` VALUES (29,'数据管理','后台管理:商业智能:数据管理',31,0,'dropbox','/bi/data','',0,1,'',1585661766299,1,1634258058714,1);
INSERT INTO `fs_member_menu` VALUES (30,'数据源','后台管理:商业智能:数据管理:数据源',31,29,'','/bi/data/source','',0,1,'',1585661826570,1,1634258102486,1);
INSERT INTO `fs_member_menu` VALUES (31,'数据集','后台管理:商业智能:数据管理:数据集',31,29,'','/bi/data/dataset','',0,1,'',1585661841784,1,1634258119688,1);
INSERT INTO `fs_member_menu` VALUES (32,'数据矩阵','后台管理:商业智能:智能报表:数据矩阵',31,37,'','/bi/report/matrix','',0,1,'',1585661854477,1,1634258546289,1);
INSERT INTO `fs_member_menu` VALUES (37,'智能报表','后台管理:商业智能:智能报表',31,0,'radar-chart','/bi/report','',0,1,'',1585662611501,1,1634258458541,1);
INSERT INTO `fs_member_menu` VALUES (38,'数据报表','后台管理:商业智能:智能报表:数据报表',31,37,'','/bi/report/visualize','',0,1,'',1585663056291,1,1637658388654,1);
INSERT INTO `fs_member_menu` VALUES (39,'数据大屏','后台管理:商业智能:智能报表:数据大屏',188,37,'','/auto/layout/list','',0,-1,'',1585663079511,1,1662456614598,1);
INSERT INTO `fs_member_menu` VALUES (41,'分组管理','后台管理:人脸识别:分组管理',44,0,'team','/face/group','',0,1,'',1597297984692,1,1598260929886,1);
INSERT INTO `fs_member_menu` VALUES (42,'人员管理','后台管理:人脸识别:人员管理',44,0,'user','/face/user','',0,1,'',1597298848332,1,1598260938414,1);
INSERT INTO `fs_member_menu` VALUES (43,'分组列表','后台管理:人脸识别:分组管理:分组列表',44,41,'','/face/group/list','',0,1,'',1597298889503,1,1597298889503,1);
INSERT INTO `fs_member_menu` VALUES (44,'人员列表','后台管理:人脸识别:人员管理:人员列表',44,42,'','/face/user/list','',0,1,'',1597298938906,1,1597298938906,1);
INSERT INTO `fs_member_menu` VALUES (45,'人像管理','后台管理:人脸识别:人像管理',44,0,'picture','/face/photo','',0,1,'',1598324193930,1,1598324327966,1);
INSERT INTO `fs_member_menu` VALUES (46,'人像列表','后台管理:人脸识别:人像管理:人像列表',44,45,'','/face/photo/list','',0,1,'',1598324353459,1,1598324353459,1);
INSERT INTO `fs_member_menu` VALUES (47,'控制面板','后台管理:人脸识别:控制面板',44,0,'block','/face/dashboard','',1,1,'',1598405239096,1,1598405239096,1);
INSERT INTO `fs_member_menu` VALUES (48,'人脸对比','后台管理:人脸识别:控制面板:人脸对比',44,47,'','/face/dashboard/compare','',0,1,'',1598405279275,1,1598405279275,1);
INSERT INTO `fs_member_menu` VALUES (49,'人脸检索','后台管理:人脸识别:控制面板:人脸检索',44,47,'','/face/dashboard/search','',0,1,'',1598405321956,1,1598405321956,1);
INSERT INTO `fs_member_menu` VALUES (50,'人脸检测','后台管理:人脸识别:控制面板:人脸检测',44,47,'','/face/dashboard/detect','',1,1,'',1598408189380,1,1598408189380,1);
INSERT INTO `fs_member_menu` VALUES (52,'Elasticsearch','服务管理:Elasticsearch',301,0,'deployment-unit','/server/elasticsearch','',0,1,'',1600481694000,1,1732160008175,1);
INSERT INTO `fs_member_menu` VALUES (53,'索引示例','服务管理:Elasticsearch:索引示例',301,52,'','/server/elasticsearch/demo','',0,1,'',1600481950656,1,1732160015732,1);
INSERT INTO `fs_member_menu` VALUES (54,'词典管理','后台管理:搜索引擎:Elasticsearch:词典管理',301,52,'','/lucene/elasticsearch/dict','',0,-1,'',1600481985836,1,1607514726426,1);
INSERT INTO `fs_member_menu` VALUES (55,'服务重载','服务管理:Elasticsearch:服务重载',301,52,'','/server/elasticsearch/reload','',0,1,'',1600482059381,1,1732160020099,1);
INSERT INTO `fs_member_menu` VALUES (56,'词库管理','后台管理:搜索引擎:词库管理',301,0,'gold','/lucene/dictionary','',0,-1,'',1607333548750,1,1732159996770,1);
INSERT INTO `fs_member_menu` VALUES (57,'词库列表','后台管理:搜索引擎:Elasticsearch:词库列表',301,52,'','/server/elasticsearch/dictionary','',0,1,'',1607333724524,1,1732159977181,1);
INSERT INTO `fs_member_menu` VALUES (59,'消息队列','后台管理:服务管理:消息队列',301,0,'hourglass','/server/rabbit','',0,1,'',1611814821145,1,1611814821145,1);
INSERT INTO `fs_member_menu` VALUES (60,'控制面板','后台管理:服务管理:消息队列:控制面板',301,59,'','/server/rabbit/dashboard','',0,1,'',1611814876795,1,1611814876795,1);
INSERT INTO `fs_member_menu` VALUES (62,'表单管理','后台管理:在线办公:表单管理',63,0,'file-protect','/oa/form','',30,1,'',1618451979842,1,1622708492969,1);
INSERT INTO `fs_member_menu` VALUES (63,'表单模型','后台管理:在线办公:表单管理:表单模型',63,62,'','/oa/form/frame','',0,1,'',1618452012679,1,1618452012679,1);
INSERT INTO `fs_member_menu` VALUES (64,'托管数据','后台管理:在线办公:表单管理:托管数据',63,62,'','/oa/form/data','',0,1,'',1618452012679,1,1618452012679,1);
INSERT INTO `fs_member_menu` VALUES (65,'校验规则','后台管理:在线办公:表单管理:校验规则',63,62,'','/oa/form/regular','',0,1,'',1618452012679,1,1618452012679,1);
INSERT INTO `fs_member_menu` VALUES (66,'字典管理','后台管理:用户中心:字典管理',2,0,'book','/member/dictionary','',0,1,'',1619510811715,1,1619510811715,1);
INSERT INTO `fs_member_menu` VALUES (67,'字典列表','后台管理:用户中心:字典管理:字典列表',2,66,'','/member/dictionary/list','',0,1,'',1619510988399,1,1619510988399,1);
INSERT INTO `fs_member_menu` VALUES (68,'树形字典','后台管理:用户中心:字典管理:树形字典',2,66,'','/member/dictionary/tree','',0,1,'',1619511018156,1,1619511018156,1);
INSERT INTO `fs_member_menu` VALUES (70,'数据表格','演示实例:数据表格',302,0,'Coin','/demo/table','',0,1,'',1620998817281,1,1736212018288,1);
INSERT INTO `fs_member_menu` VALUES (71,'合并单元格','后台管理:演示实例:数据表格:合并单元格',302,70,'','/demo/table/merge','',0,1,'',1620998844844,1,1620998844844,1);
INSERT INTO `fs_member_menu` VALUES (72,'工作流程','后台管理:在线办公:工作流程',63,0,'branches','/oa/workflow','',20,1,'',1618451979842,1,1622708507924,1);
INSERT INTO `fs_member_menu` VALUES (73,'流程模型','后台管理:在线办公:工作流程:流程模型',63,72,'','/oa/workflow/list','',0,1,'',1618452012679,1,1618452012679,1);
INSERT INTO `fs_member_menu` VALUES (74,'流程部署','后台管理:在线办公:工作流程:流程部署',63,72,'','/oa/workflow/deployment','',0,1,'',1622703834598,1,1622703834598,1);
INSERT INTO `fs_member_menu` VALUES (75,'流程审批','后台管理:在线办公:流程审批',63,0,'solution','/oa/approve','',50,1,'',1622708586117,1,1622709216210,1);
INSERT INTO `fs_member_menu` VALUES (76,'新建单据','后台管理:在线办公:流程审批:新建单据',63,75,'','/oa/approve/workflow','',0,1,'',1622708678092,1,1622709225897,1);
INSERT INTO `fs_member_menu` VALUES (77,'待签任务','后台管理:在线办公:流程审批:待签任务',63,75,'','/oa/approve/candidate','',0,1,'',1624612909138,1,1624842860376,1);
INSERT INTO `fs_member_menu` VALUES (78,'待办任务','后台管理:在线办公:流程审批:待办任务',63,75,'','/oa/approve/assignee','',0,1,'',1624852957141,1,1624852957141,1);
INSERT INTO `fs_member_menu` VALUES (79,'历史任务','后台管理:在线办公:流程审批:历史任务',63,75,'','/oa/approve/history','',0,1,'',1624869272153,1,1624869272153,1);
INSERT INTO `fs_member_menu` VALUES (80,'流程管理','后台管理:在线办公:工作流程:流程管理',63,72,'','/oa/workflow/history','',0,1,'',1624973275460,1,1624973275460,1);
INSERT INTO `fs_member_menu` VALUES (82,'系统设置','后台管理:内容管理:系统设置',96,0,'setting','/cms/setting','',0,1,'',1627262811964,1,1627285193931,1);
INSERT INTO `fs_member_menu` VALUES (83,'基础信息','后台管理:内容管理:系统设置:基础信息',96,82,'','/cms/setting/profile','',0,1,'',1627262893812,1,1627285199014,1);
INSERT INTO `fs_member_menu` VALUES (84,'导航菜单','后台管理:内容管理:系统设置:导航菜单',96,82,'','/cms/setting/menu','',0,1,'',1627263054790,1,1627285202965,1);
INSERT INTO `fs_member_menu` VALUES (85,'友情链接','后台管理:内容管理:系统设置:友情链接',96,82,'','/cms/setting/link','',0,1,'',1627263117475,1,1627285206628,1);
INSERT INTO `fs_member_menu` VALUES (86,'通知公告','后台管理:内容管理:系统设置:通知公告',96,82,'','/cms/setting/notice','',0,1,'',1627263143731,1,1627285209571,1);
INSERT INTO `fs_member_menu` VALUES (87,'首页轮播','后台管理:内容管理:系统设置:首页轮播',96,82,'','/cms/setting/carousel','',0,1,'',1627263318494,1,1627285212587,1);
INSERT INTO `fs_member_menu` VALUES (88,'内容维护','后台管理:内容管理:内容维护',96,0,'global','/cms/site','',0,1,'',1627285310300,1,1627285429530,1);
INSERT INTO `fs_member_menu` VALUES (89,'栏目管理','后台管理:内容管理:内容维护:栏目管理',96,88,'','/cms/site/catalog','',0,1,'',1627285552334,1,1627285552334,1);
INSERT INTO `fs_member_menu` VALUES (90,'文章管理','后台管理:内容管理:内容维护:文章管理',96,88,'','/cms/site/article','',0,1,'',1627285573517,1,1627285573517,1);
INSERT INTO `fs_member_menu` VALUES (91,'评论管理','后台管理:内容管理:内容维护:评论管理',96,88,'','/cms/site/comment','',0,1,'',1627285597402,1,1627285597402,1);
INSERT INTO `fs_member_menu` VALUES (92,'留言反馈','后台管理:内容管理:内容维护:留言反馈',96,88,'','/cms/site/feedback','',0,1,'',1627285644951,1,1627285644951,1);
INSERT INTO `fs_member_menu` VALUES (93,'发布文章','后台管理:内容管理:内容维护:发布文章',96,88,'','/cms/site/editor','',100,1,'',1627286496327,1,1628843173856,1);
INSERT INTO `fs_member_menu` VALUES (94,'标签管理','后台管理:内容管理:内容维护:标签管理',96,88,'','/cms/site/tag','',0,1,'',1627286834091,1,1627286834091,1);
INSERT INTO `fs_member_menu` VALUES (95,'引用管理','后台管理:内容管理:内容维护:引用管理',96,88,'','/cms/site/cite','',0,1,'',1627377188353,1,1627377188353,1);
INSERT INTO `fs_member_menu` VALUES (97,'文件管理','服务管理:文件管理',301,0,'file','/server/file','',0,1,'',1627548162634,1,1732160227988,1);
INSERT INTO `fs_member_menu` VALUES (98,'文件存档','服务管理:文件管理:文件存档',301,97,'','/server/file/archive','',0,1,'',1627548198271,1,1732160269651,1);
INSERT INTO `fs_member_menu` VALUES (100,'页面设计','后台管理:项目管理:页面设计',188,0,'read','/auto/layout','',0,-1,'',1639719989845,1,1662455350983,1);
INSERT INTO `fs_member_menu` VALUES (101,'应用管理','后台管理:项目管理:应用管理',188,0,'book','/auto/app','',0,-1,'',1639720019045,1,1662455223175,1);
INSERT INTO `fs_member_menu` VALUES (103,'元数据','后台管理:数据治理:元数据',157,0,'container','/govern/meta','',0,1,'',1642381648881,1,1648436130377,1);
INSERT INTO `fs_member_menu` VALUES (104,'定时任务','后台管理:服务管理:定时任务',301,0,'clock-circle','/server/cron','',0,1,'',1642571306245,1,1642571306245,1);
INSERT INTO `fs_member_menu` VALUES (105,'工作节点','后台管理:服务管理:定时任务:工作节点',301,104,'','/server/cron/node','',0,1,'',1642571336578,1,1642571336578,1);
INSERT INTO `fs_member_menu` VALUES (106,'作业管理','后台管理:服务管理:定时任务:作业管理',301,104,'','/server/cron/job','',0,1,'',1642571363823,1,1642571363823,1);
INSERT INTO `fs_member_menu` VALUES (107,'在线打印','后台管理:在线办公:在线打印',63,0,'printer','/oa/print','',20,1,'',1646376598407,1,1646376598407,1);
INSERT INTO `fs_member_menu` VALUES (108,'模板列表','后台管理:在线办公:在线打印:模板列表',63,107,'','/oa/print/list','',0,1,'',1646376622053,1,1646376622053,1);
INSERT INTO `fs_member_menu` VALUES (109,'作业调度','后台管理:服务管理:定时任务:作业调度',301,104,'','/server/cron/trigger','',0,1,'',1647236774840,1,1647236774840,1);
INSERT INTO `fs_member_menu` VALUES (110,'任务编排','后台管理:服务管理:定时任务:任务编排',301,104,'','/server/cron/flow','',0,1,'',1647311512665,1,1647311512665,1);
INSERT INTO `fs_member_menu` VALUES (111,'数据标准','后台管理:数据治理:数据标准',157,0,'exception','/govern/standard','',0,1,'',1648435605072,1,1648436213331,1);
INSERT INTO `fs_member_menu` VALUES (112,'数据资产','后台管理:数据治理:数据资产',157,0,'database','/govern/asset','',0,1,'',1648435651055,1,1648436139625,1);
INSERT INTO `fs_member_menu` VALUES (113,'数据质量','后台管理:数据治理:数据质量',157,0,'file-protect','/govern/quality','',0,1,'',1648435669817,1,1648436224359,1);
INSERT INTO `fs_member_menu` VALUES (114,'数据安全','后台管理:数据治理:数据安全',157,0,'safety-certificate','/govern/security','',0,1,'',1648435775952,1,1648436303957,1);
INSERT INTO `fs_member_menu` VALUES (115,'数据交换','后台管理:数据治理:数据交换',157,0,'cloud-sync','/govern/exchange','',0,1,'',1648436340888,1,1648867473179,1);
INSERT INTO `fs_member_menu` VALUES (116,'模型管理','后台管理:数据治理:元数据:模型管理',157,103,'','/govern/meta/model','',0,1,'',1648782739796,1,1649821115608,1);
INSERT INTO `fs_member_menu` VALUES (117,'模型关系','后台管理:数据治理:元数据:模型关系',157,103,'','/govern/meta/modelRelation','',0,1,'',1649856291456,1,1649856291456,1);
INSERT INTO `fs_member_menu` VALUES (118,'数据地图','后台管理:数据治理:元数据:数据地图',157,103,'','/govern/meta/map','',99,1,'',1650760956687,1,1650760956687,1);
INSERT INTO `fs_member_menu` VALUES (119,'系统设置','后台管理:数据治理:系统设置',157,0,'solution','/govern/system','',0,1,'',1651288300961,1,1651288300961,1);
INSERT INTO `fs_member_menu` VALUES (120,'数据源','后台管理:数据治理:系统设置:数据源',157,119,'','/govern/system/source','',0,1,'',1651288324135,1,1651288324135,1);
INSERT INTO `fs_member_menu` VALUES (121,'标准管理','后台管理:数据治理:数据标准:标准管理',157,111,'','/govern/standard/list','',0,1,'',1651369141111,1,1651369141111,1);
INSERT INTO `fs_member_menu` VALUES (122,'落地评估','后台管理:数据治理:数据标准:落地评估',157,111,'','/govern/standard/assess','',0,1,'',1651369188363,1,1651369188363,1);
INSERT INTO `fs_member_menu` VALUES (123,'质检分类','后台管理:数据治理:数据质量:质检分类',157,113,'','/govern/quality/logic','',0,1,'',1651369714750,1,1676537824487,1);
INSERT INTO `fs_member_menu` VALUES (124,'评估结果','后台管理:数据治理:数据标准:评估结果',157,111,'','/govern/standard/log','',0,1,'',1652682411082,1,1652682411082,1);
INSERT INTO `fs_member_menu` VALUES (125,'调度日志','后台管理:服务管理:定时任务:调度日志',301,104,'','/server/cron/flowLog','',0,1,'',1658735846363,1,1658735846363,1);
INSERT INTO `fs_member_menu` VALUES (126,'质检规则','后台管理:数据治理:数据质量:质检规则',157,113,'','/govern/quality/rule','',0,1,'',1660203491808,1,1660203491808,1);
INSERT INTO `fs_member_menu` VALUES (127,'质检方案','后台管理:数据治理:数据质量:质检方案',157,113,'','/govern/quality/plan','',0,1,'',1660203503955,1,1660203503955,1);
INSERT INTO `fs_member_menu` VALUES (128,'质检报告','后台管理:数据治理:数据质量:质检报告',157,113,'','/govern/quality/log','',0,1,'',1660203570855,1,1660203570855,1);
INSERT INTO `fs_member_menu` VALUES (129,'页面布局','后台管理:项目管理:页面管理:页面布局',188,100,'','/auto/layout/list','',0,-1,'',1662455306154,1,1662455306154,1);
INSERT INTO `fs_member_menu` VALUES (130,'数据交互','演示实例:数据交互',302,0,'Soccer','/demo/data','',0,1,'',1705915668277,1,1736212046223,1);
INSERT INTO `fs_member_menu` VALUES (131,'EventSource','后台管理:演示实例:数据交互:EventSource',302,130,'','/demo/data/sse','',0,1,'',1705915802879,1,1705915802879,1);
INSERT INTO `fs_member_menu` VALUES (132,'应用管理','后台管理:用户中心:应用管理',2,0,'appstore','/member/application','',0,1,'',1729752579015,1,1729752653095,1);
INSERT INTO `fs_member_menu` VALUES (133,'应用列表','后台管理:用户中心:应用管理:应用列表',2,132,'','/member/application/list','',0,1,'',1729752681106,1,1729752681106,1);
/*!40000 ALTER TABLE `fs_member_menu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `fs_member_role`
--

DROP TABLE IF EXISTS `fs_member_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `fs_member_role` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `sort` tinyint NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '0',
  `description` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `created_time` bigint NOT NULL DEFAULT '0',
  `created_uid` int NOT NULL DEFAULT '0',
  `updated_time` bigint NOT NULL DEFAULT '0',
  `updated_uid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fs_member_role`
--

LOCK TABLES `fs_member_role` WRITE;
/*!40000 ALTER TABLE `fs_member_role` DISABLE KEYS */;
INSERT INTO `fs_member_role` VALUES (1,'后台管理',0,1,'',1528081589495,1,1528266877684,1);
INSERT INTO `fs_member_role` VALUES (2,'普通用户',0,1,'',1528081606670,1,1528081606670,1);
INSERT INTO `fs_member_role` VALUES (3,'aaa',0,-1,'',1584514294259,1,1584515444090,1);
INSERT INTO `fs_member_role` VALUES (4,'aaaxx',0,2,'',1584515454033,1,1584515461772,1);
/*!40000 ALTER TABLE `fs_member_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `fs_member_setting`
--

DROP TABLE IF EXISTS `fs_member_setting`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `fs_member_setting` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `type` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `content` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `sort` tinyint NOT NULL DEFAULT '0',
  `description` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `updated_time` bigint NOT NULL DEFAULT '0',
  `updated_uid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uniq_type_name` (`type`,`name`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=42 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fs_member_setting`
--

LOCK TABLES `fs_member_setting` WRITE;
/*!40000 ALTER TABLE `fs_member_setting` DISABLE KEYS */;
INSERT INTO `fs_member_setting` VALUES (1,'defaultPassword','member','password',1,'默认登录密码',1627009079201,1);
INSERT INTO `fs_member_setting` VALUES (3,'menuParentId','admin','1',1,'管理后台菜单根节点ID',1627009055076,1);
INSERT INTO `fs_member_setting` VALUES (4,'siteName','cms','IISquare.com',0,'站点名称',1627354687313,1);
INSERT INTO `fs_member_setting` VALUES (5,'keywords','cms','fs-project,iisquare,iisquare.com,开源,免费',0,'关键词',1627354693783,1);
INSERT INTO `fs_member_setting` VALUES (6,'description','cms','fs-project Copyright by IISquare.com',0,'站点描述',1627354700189,1);
INSERT INTO `fs_member_setting` VALUES (7,'commentDisabled','cms','false',0,'禁用评论',1627354707526,1);
INSERT INTO `fs_member_setting` VALUES (8,'homeTitle','cms','个人博客',0,'首页标题',1627354713093,1);
INSERT INTO `fs_member_setting` VALUES (29,'menu','cms','[{\"name\":\"首页\",\"title\":\"\",\"href\":\"/\",\"target\":\"\",\"disabled\":false,\"children\":[]},{\"name\":\"栏目一\",\"title\":\"\",\"href\":\"\",\"target\":\"\",\"disabled\":false,\"children\":[{\"name\":\"子栏目一\",\"title\":\"\",\"href\":\"\",\"target\":\"\",\"disabled\":false,\"children\":[]},{\"name\":\"子栏目二\",\"title\":\"\",\"href\":\"\",\"target\":\"\",\"disabled\":false,\"children\":[{\"name\":\"孙栏目一\",\"title\":\"\",\"href\":\"\",\"target\":\"\",\"disabled\":false,\"children\":[]},{\"name\":\"孙栏目二\",\"title\":\"\",\"href\":\"\",\"target\":\"\",\"disabled\":false,\"children\":[]}]},{\"name\":\"子栏目三\",\"title\":\"\",\"href\":\"\",\"target\":\"\",\"disabled\":false,\"children\":[]}]},{\"name\":\"栏目二\",\"title\":\"\",\"href\":\"\",\"target\":\"\",\"disabled\":false,\"children\":[]},{\"name\":\"栏目三\",\"title\":\"\",\"href\":\"\",\"target\":\"\",\"disabled\":false,\"children\":[]}]',0,'导航菜单',1627461740399,1);
INSERT INTO `fs_member_setting` VALUES (39,'link','cms','[]',0,'友情链接',1628123781321,1);
INSERT INTO `fs_member_setting` VALUES (40,'notice','cms','[]',0,'通知公告',1628123790033,1);
INSERT INTO `fs_member_setting` VALUES (41,'carousel','cms','[]',0,'首页轮播图',1628123760775,1);
/*!40000 ALTER TABLE `fs_member_setting` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `fs_member_application`
--

DROP TABLE IF EXISTS `fs_member_application`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `fs_member_application` (
  `id` int NOT NULL AUTO_INCREMENT,
  `serial` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `icon` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `url` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `target` varchar(8) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `sort` tinyint NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '0',
  `description` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `created_time` bigint NOT NULL DEFAULT '0',
  `created_uid` int NOT NULL DEFAULT '0',
  `updated_time` bigint NOT NULL DEFAULT '0',
  `updated_uid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `unq_serial` (`serial`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=305 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fs_member_application`
--

LOCK TABLES `fs_member_application` WRITE;
/*!40000 ALTER TABLE `fs_member_application` DISABLE KEYS */;
INSERT INTO `fs_member_application` VALUES (1,'admin','后台管理','home','/','',0,1,'是否可访问后台',1528081670164,1,1584590104125,1);
INSERT INTO `fs_member_application` VALUES (2,'member','用户中心','User','/member/index/index','',0,1,'帐号、角色、资源、菜单、配置',1528081670164,1,1736157706223,1);
INSERT INTO `fs_member_application` VALUES (26,'spider','网页爬虫','bug','/spider/index/index','',0,1,'节点信息、模板管理',1585195421330,1,1585195421330,1);
INSERT INTO `fs_member_application` VALUES (31,'bi','商业智能','cluster','/bi/index/index','',0,1,'数据清洗、规则引擎、智能报表',1585384071227,1,1631754471053,1);
INSERT INTO `fs_member_application` VALUES (44,'face','人脸识别','smile','/face/index/index','',0,1,'人脸检测、人脸识别、检索对比',1597299393466,1,1597299393466,1);
INSERT INTO `fs_member_application` VALUES (58,'lucene','搜索引擎','','','',0,1,'',1607333327044,1,1732159710326,1);
INSERT INTO `fs_member_application` VALUES (63,'oa','在线办公','snippets','/oa/index/index','',0,1,'表单设计、流程设计、在线审批',1618446294833,1,1618446294833,1);
INSERT INTO `fs_member_application` VALUES (96,'cms','内容管理','solution','/cms/index/index','',0,1,'CMS内容管理系统',1627462111659,1,1627462111659,1);
INSERT INTO `fs_member_application` VALUES (100,'file','文件存储','','','',0,1,'文件存储、图库图床、对象存储',1627547763814,1,1732160136155,1);
INSERT INTO `fs_member_application` VALUES (148,'cron','定时任务','','','',0,1,'',1662455086472,1,1662455086472,1);
INSERT INTO `fs_member_application` VALUES (157,'govern','数据治理','medicine-box','/govern/index/index','',0,1,'数据接入、元数据、集成同步',1648782552696,1,1648782565072,1);
INSERT INTO `fs_member_application` VALUES (301,'server','服务管理','box-plot','/server/index/index','',0,1,'管理项目基础服务',1662455086472,1,1662455086472,1);
INSERT INTO `fs_member_application` VALUES (302,'demo','演示实例','Opportunity','/demo/index/index','',0,1,'基础组件、功能演示、示例代码',1662455086472,1,1736211445444,1);
INSERT INTO `fs_member_application` VALUES (304,'ai','人工智能','reconciliation','/ai/index/index','',0,1,'大模型、知识图谱、算法模型',1735010540766,1,1735010770702,1);
/*!40000 ALTER TABLE `fs_member_application` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `fs_member_dictionary`
--

DROP TABLE IF EXISTS `fs_member_dictionary`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `fs_member_dictionary` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `full_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `parent_id` int NOT NULL DEFAULT '0',
  `content` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `sort` tinyint NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '0',
  `description` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `created_time` bigint NOT NULL DEFAULT '0',
  `created_uid` int NOT NULL DEFAULT '0',
  `updated_time` bigint NOT NULL DEFAULT '0',
  `updated_uid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_content` (`content`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fs_member_dictionary`
--

LOCK TABLES `fs_member_dictionary` WRITE;
/*!40000 ALTER TABLE `fs_member_dictionary` DISABLE KEYS */;
INSERT INTO `fs_member_dictionary` VALUES (1,'性别','性别',0,'gender',0,1,'',1619512547886,1,1619512547886,1);
INSERT INTO `fs_member_dictionary` VALUES (2,'男','性别:男',1,'man',0,1,'',1619512558937,1,1619512558937,1);
INSERT INTO `fs_member_dictionary` VALUES (3,'女','性别:女',1,'woman',0,1,'',1619512570735,1,1619512570735,1);
INSERT INTO `fs_member_dictionary` VALUES (4,'级联','级联',0,'cascade',0,1,'',1619512827457,1,1619512827457,1);
INSERT INTO `fs_member_dictionary` VALUES (5,'一级-1','级联:一级-1',4,'level-1',0,1,'',1619512855045,1,1619512855045,1);
INSERT INTO `fs_member_dictionary` VALUES (6,'一级-2','级联:一级-2',4,'level-2',0,1,'',1619512872071,1,1619512872071,1);
INSERT INTO `fs_member_dictionary` VALUES (7,'一级-3','级联:一级-3',4,'level-3',0,1,'',1619512893508,1,1619512961411,1);
INSERT INTO `fs_member_dictionary` VALUES (8,'二级-1-1','级联:一级-1:二级-1-1',5,'level-1-1',0,1,'',1619512990888,1,1619512990888,1);
INSERT INTO `fs_member_dictionary` VALUES (9,'二级-3-1','级联:一级-3:二级-3-1',7,'level-3-1',0,1,'',1619513015148,1,1619513015148,1);
INSERT INTO `fs_member_dictionary` VALUES (10,'二级-3-2','级联:一级-3:二级-3-2',7,'level-3-2',0,1,'',1619513023627,1,1619513023627,1);
INSERT INTO `fs_member_dictionary` VALUES (11,'岗位职责','岗位职责',0,'job-responsibility',0,1,'',1622184066092,1,1628835455782,1);
INSERT INTO `fs_member_dictionary` VALUES (12,'董事长','岗位职责:董事长',11,'chairman',0,1,'',1622184121813,1,1622184121813,1);
INSERT INTO `fs_member_dictionary` VALUES (13,'总经理','岗位职责:总经理',11,'president',0,1,'',1622184318304,1,1622184318304,1);
INSERT INTO `fs_member_dictionary` VALUES (14,'副总经理','岗位职责:副总经理',11,'deputy-president',0,1,'',1622184347379,1,1628835465073,1);
INSERT INTO `fs_member_dictionary` VALUES (15,'主管','岗位职责:主管',11,'executive',0,1,'',1622184431053,1,1622184431053,1);
INSERT INTO `fs_member_dictionary` VALUES (16,'职员','岗位职责:职员',11,'staff',0,1,'',1622184489471,1,1622184489471,1);
INSERT INTO `fs_member_dictionary` VALUES (17,'审核标签','审核标签',0,'audit-tag',0,1,'',1628835395109,1,1628835477591,1);
INSERT INTO `fs_member_dictionary` VALUES (18,'广告','审核标签:广告',17,'advertise',99,1,'',1628835501325,1,1628836122775,1);
INSERT INTO `fs_member_dictionary` VALUES (19,'敏感词','审核标签:敏感词',17,'sensitive',19,1,'',1628835532602,1,1628836061905,1);
INSERT INTO `fs_member_dictionary` VALUES (20,'色情','审核标签:色情',17,'porn',98,1,'',1628835543698,1,1628836135493,1);
INSERT INTO `fs_member_dictionary` VALUES (21,'暴力','审核标签:暴力',17,'violence',97,1,'',1628835577084,1,1628836149133,1);
INSERT INTO `fs_member_dictionary` VALUES (22,'违法','审核标签:违法',17,'illegal',22,1,'',1628835588090,1,1628836033275,1);
INSERT INTO `fs_member_dictionary` VALUES (23,'辱骂','审核标签:辱骂',17,'abuse',23,1,'',1628835601620,1,1628836014104,1);
INSERT INTO `fs_member_dictionary` VALUES (24,'其他','审核标签:其他',17,'other',4,1,'',1628835614279,1,1628836162399,1);
/*!40000 ALTER TABLE `fs_member_dictionary` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `fs_member_relation`
--

DROP TABLE IF EXISTS `fs_member_relation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `fs_member_relation` (
  `id` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `type` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `aid` int NOT NULL DEFAULT '0',
  `bid` int NOT NULL DEFAULT '0',
  `cid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_type` (`type`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fs_member_relation`
--

LOCK TABLES `fs_member_relation` WRITE;
/*!40000 ALTER TABLE `fs_member_relation` DISABLE KEYS */;
INSERT INTO `fs_member_relation` VALUES ('role_application_1_100_0','role_application',1,100,0);
INSERT INTO `fs_member_relation` VALUES ('role_application_1_148_0','role_application',1,148,0);
INSERT INTO `fs_member_relation` VALUES ('role_application_1_157_0','role_application',1,157,0);
INSERT INTO `fs_member_relation` VALUES ('role_application_1_188_0','role_application',1,188,0);
INSERT INTO `fs_member_relation` VALUES ('role_application_1_1_0','role_application',1,1,0);
INSERT INTO `fs_member_relation` VALUES ('role_application_1_26_0','role_application',1,26,0);
INSERT INTO `fs_member_relation` VALUES ('role_application_1_2_0','role_application',1,2,0);
INSERT INTO `fs_member_relation` VALUES ('role_application_1_301_0','role_application',1,301,0);
INSERT INTO `fs_member_relation` VALUES ('role_application_1_302_0','role_application',1,302,0);
INSERT INTO `fs_member_relation` VALUES ('role_application_1_304_0','role_application',1,304,0);
INSERT INTO `fs_member_relation` VALUES ('role_application_1_31_0','role_application',1,31,0);
INSERT INTO `fs_member_relation` VALUES ('role_application_1_44_0','role_application',1,44,0);
INSERT INTO `fs_member_relation` VALUES ('role_application_1_58_0','role_application',1,58,0);
INSERT INTO `fs_member_relation` VALUES ('role_application_1_63_0','role_application',1,63,0);
INSERT INTO `fs_member_relation` VALUES ('role_application_1_96_0','role_application',1,96,0);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_103_157','role_menu',1,103,157);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_104_301','role_menu',1,104,301);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_105_301','role_menu',1,105,301);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_106_301','role_menu',1,106,301);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_107_63','role_menu',1,107,63);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_108_63','role_menu',1,108,63);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_109_301','role_menu',1,109,301);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_10_2','role_menu',1,10,2);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_110_301','role_menu',1,110,301);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_111_157','role_menu',1,111,157);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_112_157','role_menu',1,112,157);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_113_157','role_menu',1,113,157);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_114_157','role_menu',1,114,157);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_115_157','role_menu',1,115,157);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_116_157','role_menu',1,116,157);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_117_157','role_menu',1,117,157);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_118_157','role_menu',1,118,157);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_119_157','role_menu',1,119,157);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_11_2','role_menu',1,11,2);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_120_157','role_menu',1,120,157);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_121_157','role_menu',1,121,157);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_122_157','role_menu',1,122,157);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_123_157','role_menu',1,123,157);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_124_157','role_menu',1,124,157);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_125_301','role_menu',1,125,301);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_126_157','role_menu',1,126,157);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_127_157','role_menu',1,127,157);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_128_157','role_menu',1,128,157);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_12_2','role_menu',1,12,2);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_130_302','role_menu',1,130,302);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_131_302','role_menu',1,131,302);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_132_2','role_menu',1,132,2);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_133_2','role_menu',1,133,2);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_13_2','role_menu',1,13,2);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_14_2','role_menu',1,14,2);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_15_2','role_menu',1,15,2);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_16_2','role_menu',1,16,2);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_17_2','role_menu',1,17,2);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_18_1','role_menu',1,18,1);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_19_1','role_menu',1,19,1);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_20_1','role_menu',1,20,1);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_22_26','role_menu',1,22,26);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_23_26','role_menu',1,23,26);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_24_26','role_menu',1,24,26);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_25_26','role_menu',1,25,26);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_27_31','role_menu',1,27,31);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_28_31','role_menu',1,28,31);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_29_31','role_menu',1,29,31);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_30_31','role_menu',1,30,31);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_31_31','role_menu',1,31,31);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_32_31','role_menu',1,32,31);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_37_31','role_menu',1,37,31);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_38_31','role_menu',1,38,31);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_3_1','role_menu',1,3,1);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_41_44','role_menu',1,41,44);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_42_44','role_menu',1,42,44);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_43_44','role_menu',1,43,44);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_44_44','role_menu',1,44,44);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_45_44','role_menu',1,45,44);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_46_44','role_menu',1,46,44);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_47_44','role_menu',1,47,44);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_48_44','role_menu',1,48,44);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_49_44','role_menu',1,49,44);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_4_1','role_menu',1,4,1);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_50_44','role_menu',1,50,44);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_52_301','role_menu',1,52,301);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_52_58','role_menu',1,52,58);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_53_301','role_menu',1,53,301);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_53_58','role_menu',1,53,58);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_55_301','role_menu',1,55,301);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_55_58','role_menu',1,55,58);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_56_58','role_menu',1,56,58);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_57_301','role_menu',1,57,301);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_57_58','role_menu',1,57,58);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_59_301','role_menu',1,59,301);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_60_301','role_menu',1,60,301);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_62_63','role_menu',1,62,63);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_63_63','role_menu',1,63,63);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_64_63','role_menu',1,64,63);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_65_63','role_menu',1,65,63);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_66_2','role_menu',1,66,2);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_67_2','role_menu',1,67,2);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_68_2','role_menu',1,68,2);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_6_2','role_menu',1,6,2);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_70_302','role_menu',1,70,302);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_71_302','role_menu',1,71,302);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_72_63','role_menu',1,72,63);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_73_63','role_menu',1,73,63);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_74_63','role_menu',1,74,63);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_75_63','role_menu',1,75,63);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_76_63','role_menu',1,76,63);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_77_63','role_menu',1,77,63);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_78_63','role_menu',1,78,63);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_79_63','role_menu',1,79,63);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_7_2','role_menu',1,7,2);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_80_63','role_menu',1,80,63);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_82_96','role_menu',1,82,96);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_83_96','role_menu',1,83,96);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_84_96','role_menu',1,84,96);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_85_96','role_menu',1,85,96);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_86_96','role_menu',1,86,96);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_87_96','role_menu',1,87,96);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_88_96','role_menu',1,88,96);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_89_96','role_menu',1,89,96);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_8_2','role_menu',1,8,2);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_90_96','role_menu',1,90,96);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_91_96','role_menu',1,91,96);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_92_96','role_menu',1,92,96);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_93_96','role_menu',1,93,96);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_94_96','role_menu',1,94,96);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_95_96','role_menu',1,95,96);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_97_100','role_menu',1,97,100);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_97_301','role_menu',1,97,301);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_98_100','role_menu',1,98,100);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_98_301','role_menu',1,98,301);
INSERT INTO `fs_member_relation` VALUES ('role_menu_1_9_2','role_menu',1,9,2);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_101_100','role_resource',1,101,100);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_102_100','role_resource',1,102,100);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_103_100','role_resource',1,103,100);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_104_100','role_resource',1,104,100);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_105_100','role_resource',1,105,100);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_106_96','role_resource',1,106,96);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_107_96','role_resource',1,107,96);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_108_96','role_resource',1,108,96);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_109_96','role_resource',1,109,96);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_10_2','role_resource',1,10,2);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_110_96','role_resource',1,110,96);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_111_96','role_resource',1,111,96);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_112_96','role_resource',1,112,96);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_113_96','role_resource',1,113,96);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_114_96','role_resource',1,114,96);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_115_96','role_resource',1,115,96);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_116_96','role_resource',1,116,96);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_117_96','role_resource',1,117,96);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_118_96','role_resource',1,118,96);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_119_96','role_resource',1,119,96);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_11_2','role_resource',1,11,2);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_120_96','role_resource',1,120,96);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_121_96','role_resource',1,121,96);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_122_96','role_resource',1,122,96);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_123_96','role_resource',1,123,96);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_124_96','role_resource',1,124,96);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_125_96','role_resource',1,125,96);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_126_96','role_resource',1,126,96);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_127_96','role_resource',1,127,96);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_128_31','role_resource',1,128,31);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_129_31','role_resource',1,129,31);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_12_2','role_resource',1,12,2);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_130_31','role_resource',1,130,31);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_131_31','role_resource',1,131,31);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_132_31','role_resource',1,132,31);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_133_31','role_resource',1,133,31);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_138_31','role_resource',1,138,31);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_139_31','role_resource',1,139,31);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_13_2','role_resource',1,13,2);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_140_31','role_resource',1,140,31);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_141_31','role_resource',1,141,31);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_142_31','role_resource',1,142,31);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_143_31','role_resource',1,143,31);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_144_63','role_resource',1,144,63);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_145_63','role_resource',1,145,63);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_146_63','role_resource',1,146,63);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_147_63','role_resource',1,147,63);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_149_148','role_resource',1,149,148);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_14_2','role_resource',1,14,2);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_150_148','role_resource',1,150,148);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_151_148','role_resource',1,151,148);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_152_148','role_resource',1,152,148);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_153_148','role_resource',1,153,148);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_154_148','role_resource',1,154,148);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_155_148','role_resource',1,155,148);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_156_148','role_resource',1,156,148);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_158_157','role_resource',1,158,157);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_159_157','role_resource',1,159,157);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_15_2','role_resource',1,15,2);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_160_157','role_resource',1,160,157);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_161_157','role_resource',1,161,157);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_162_157','role_resource',1,162,157);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_163_157','role_resource',1,163,157);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_164_157','role_resource',1,164,157);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_165_157','role_resource',1,165,157);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_166_157','role_resource',1,166,157);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_167_157','role_resource',1,167,157);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_168_157','role_resource',1,168,157);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_169_157','role_resource',1,169,157);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_16_2','role_resource',1,16,2);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_170_157','role_resource',1,170,157);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_171_157','role_resource',1,171,157);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_172_157','role_resource',1,172,157);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_173_157','role_resource',1,173,157);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_174_157','role_resource',1,174,157);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_175_157','role_resource',1,175,157);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_176_157','role_resource',1,176,157);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_177_157','role_resource',1,177,157);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_178_157','role_resource',1,178,157);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_179_157','role_resource',1,179,157);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_17_2','role_resource',1,17,2);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_180_157','role_resource',1,180,157);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_181_157','role_resource',1,181,157);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_182_157','role_resource',1,182,157);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_183_157','role_resource',1,183,157);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_184_157','role_resource',1,184,157);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_185_157','role_resource',1,185,157);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_186_157','role_resource',1,186,157);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_187_157','role_resource',1,187,157);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_189_157','role_resource',1,189,157);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_18_2','role_resource',1,18,2);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_190_157','role_resource',1,190,157);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_191_157','role_resource',1,191,157);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_192_157','role_resource',1,192,157);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_193_157','role_resource',1,193,157);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_194_157','role_resource',1,194,157);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_197_2','role_resource',1,197,2);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_198_2','role_resource',1,198,2);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_199_2','role_resource',1,199,2);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_19_2','role_resource',1,19,2);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_200_2','role_resource',1,200,2);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_201_2','role_resource',1,201,2);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_202_2','role_resource',1,202,2);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_203_2','role_resource',1,203,2);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_20_2','role_resource',1,20,2);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_21_2','role_resource',1,21,2);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_22_2','role_resource',1,22,2);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_23_2','role_resource',1,23,2);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_24_2','role_resource',1,24,2);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_25_2','role_resource',1,25,2);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_27_26','role_resource',1,27,26);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_28_26','role_resource',1,28,26);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_29_26','role_resource',1,29,26);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_30_26','role_resource',1,30,26);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_32_31','role_resource',1,32,31);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_33_31','role_resource',1,33,31);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_34_31','role_resource',1,34,31);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_35_31','role_resource',1,35,31);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_36_31','role_resource',1,36,31);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_37_31','role_resource',1,37,31);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_38_31','role_resource',1,38,31);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_39_31','role_resource',1,39,31);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_3_2','role_resource',1,3,2);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_40_31','role_resource',1,40,31);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_41_31','role_resource',1,41,31);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_42_31','role_resource',1,42,31);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_43_31','role_resource',1,43,31);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_45_44','role_resource',1,45,44);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_46_44','role_resource',1,46,44);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_47_44','role_resource',1,47,44);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_48_44','role_resource',1,48,44);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_49_44','role_resource',1,49,44);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_4_2','role_resource',1,4,2);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_50_44','role_resource',1,50,44);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_51_44','role_resource',1,51,44);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_52_44','role_resource',1,52,44);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_53_44','role_resource',1,53,44);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_54_44','role_resource',1,54,44);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_55_44','role_resource',1,55,44);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_56_44','role_resource',1,56,44);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_57_44','role_resource',1,57,44);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_59_58','role_resource',1,59,58);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_5_2','role_resource',1,5,2);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_60_58','role_resource',1,60,58);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_61_58','role_resource',1,61,58);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_62_58','role_resource',1,62,58);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_64_63','role_resource',1,64,63);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_65_63','role_resource',1,65,63);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_66_63','role_resource',1,66,63);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_67_63','role_resource',1,67,63);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_68_63','role_resource',1,68,63);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_69_63','role_resource',1,69,63);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_6_2','role_resource',1,6,2);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_70_63','role_resource',1,70,63);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_71_63','role_resource',1,71,63);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_72_63','role_resource',1,72,63);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_73_63','role_resource',1,73,63);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_74_63','role_resource',1,74,63);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_75_63','role_resource',1,75,63);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_76_2','role_resource',1,76,2);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_77_2','role_resource',1,77,2);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_78_2','role_resource',1,78,2);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_79_2','role_resource',1,79,2);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_7_2','role_resource',1,7,2);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_80_63','role_resource',1,80,63);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_81_63','role_resource',1,81,63);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_82_63','role_resource',1,82,63);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_83_63','role_resource',1,83,63);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_84_63','role_resource',1,84,63);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_85_63','role_resource',1,85,63);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_86_63','role_resource',1,86,63);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_87_63','role_resource',1,87,63);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_88_63','role_resource',1,88,63);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_89_63','role_resource',1,89,63);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_8_2','role_resource',1,8,2);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_90_63','role_resource',1,90,63);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_91_63','role_resource',1,91,63);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_92_63','role_resource',1,92,63);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_93_63','role_resource',1,93,63);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_94_63','role_resource',1,94,63);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_95_63','role_resource',1,95,63);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_97_96','role_resource',1,97,96);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_98_96','role_resource',1,98,96);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_99_96','role_resource',1,99,96);
INSERT INTO `fs_member_relation` VALUES ('role_resource_1_9_2','role_resource',1,9,2);
INSERT INTO `fs_member_relation` VALUES ('user_role_1_1_0','user_role',1,1,0);
/*!40000 ALTER TABLE `fs_member_relation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `fs_member_resource`
--

DROP TABLE IF EXISTS `fs_member_resource`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `fs_member_resource` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `full_name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `application_id` int NOT NULL DEFAULT '0',
  `parent_id` int NOT NULL DEFAULT '0',
  `module` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `controller` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `action` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `sort` tinyint NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '0',
  `description` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `created_time` bigint NOT NULL DEFAULT '0',
  `created_uid` int NOT NULL DEFAULT '0',
  `updated_time` bigint NOT NULL DEFAULT '0',
  `updated_uid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_application_id` (`application_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=204 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fs_member_resource`
--

LOCK TABLES `fs_member_resource` WRITE;
/*!40000 ALTER TABLE `fs_member_resource` DISABLE KEYS */;
INSERT INTO `fs_member_resource` VALUES (3,'菜单','用户中心:菜单',2,0,'member','menu','',0,1,'',1528081696180,1,1584590110517,1);
INSERT INTO `fs_member_resource` VALUES (4,'资源','用户中心:资源',2,0,'member','resource','',0,1,'',1528081721468,1,1584590139693,1);
INSERT INTO `fs_member_resource` VALUES (5,'角色','用户中心:角色',2,0,'member','role','',0,1,'',1528081764195,1,1584590159965,1);
INSERT INTO `fs_member_resource` VALUES (6,'用户','用户中心:用户',2,0,'member','user','',0,1,'',1528081790426,1,1584590183845,1);
INSERT INTO `fs_member_resource` VALUES (7,'配置','用户中心:配置',2,0,'member','setting','',0,1,'',1528081858994,1,1584590241289,1);
INSERT INTO `fs_member_resource` VALUES (8,'添加','用户中心:菜单:添加',2,3,'member','menu','add',0,1,'',1528081911866,1,1584590115052,1);
INSERT INTO `fs_member_resource` VALUES (9,'修改','用户中心:菜单:修改',2,3,'member','menu','modify',0,1,'',1528081955009,1,1584590127509,1);
INSERT INTO `fs_member_resource` VALUES (10,'删除','用户中心:菜单:删除',2,3,'member','menu','delete',0,1,'',1528081985121,1,1584590135133,1);
INSERT INTO `fs_member_resource` VALUES (11,'添加','用户中心:资源:添加',2,4,'member','resource','add',0,1,'',1528082024771,1,1584590144698,1);
INSERT INTO `fs_member_resource` VALUES (12,'修改','用户中心:资源:修改',2,4,'member','resource','modify',0,1,'',1528082042903,1,1584590150312,1);
INSERT INTO `fs_member_resource` VALUES (13,'删除','用户中心:资源:删除',2,4,'member','resource','delete',0,1,'',1528082084243,1,1584590155309,1);
INSERT INTO `fs_member_resource` VALUES (14,'添加','用户中心:角色:添加',2,5,'member','role','add',0,1,'',1528082104404,1,1584590165347,1);
INSERT INTO `fs_member_resource` VALUES (15,'修改','用户中心:角色:修改',2,5,'member','role','modify',0,1,'',1528082198733,1,1584590169694,1);
INSERT INTO `fs_member_resource` VALUES (16,'删除','用户中心:角色:删除',2,5,'member','role','delete',0,1,'',1528082214265,1,1584590174794,1);
INSERT INTO `fs_member_resource` VALUES (17,'菜单','用户中心:角色:菜单',2,5,'member','role','menu',0,1,'',1528082271647,1,1584590180046,1);
INSERT INTO `fs_member_resource` VALUES (18,'资源','用户中心:角色:资源',2,5,'member','role','resource',0,1,'',1528082432853,1,1584590193894,1);
INSERT INTO `fs_member_resource` VALUES (19,'添加','用户中心:用户:添加',2,6,'member','user','add',0,1,'',1528082465188,1,1584590202725,1);
INSERT INTO `fs_member_resource` VALUES (20,'修改','用户中心:用户:修改',2,6,'member','user','modify',0,1,'',1528082492740,1,1584590224552,1);
INSERT INTO `fs_member_resource` VALUES (21,'删除','用户中心:用户:删除',2,6,'member','user','delete',0,1,'',1528082514205,1,1584590230942,1);
INSERT INTO `fs_member_resource` VALUES (22,'角色','用户中心:用户:角色',2,6,'member','user','role',0,1,'',1528082586393,1,1584590236190,1);
INSERT INTO `fs_member_resource` VALUES (23,'添加','用户中心:配置:添加',2,7,'member','setting','add',0,1,'',1528082616922,1,1584590245882,1);
INSERT INTO `fs_member_resource` VALUES (24,'修改','用户中心:配置:修改',2,7,'member','setting','modify',0,1,'',1528082642509,1,1584590249855,1);
INSERT INTO `fs_member_resource` VALUES (25,'删除','用户中心:配置:删除',2,7,'member','setting','delete',0,1,'',1528082662922,1,1584590258533,1);
INSERT INTO `fs_member_resource` VALUES (27,'模板','网页爬虫:模板',26,0,'spider','template','',0,1,'',1585195446408,1,1585195446408,1);
INSERT INTO `fs_member_resource` VALUES (28,'删除','网页爬虫:模板:删除',26,27,'spider','template','delete',0,1,'',1585195509597,1,1585195533509,1);
INSERT INTO `fs_member_resource` VALUES (29,'修改','网页爬虫:模板:修改',26,27,'spider','template','modify',0,1,'',1585195523181,1,1585195523181,1);
INSERT INTO `fs_member_resource` VALUES (30,'添加','网页爬虫:模板:添加',26,27,'spider','template','add',0,1,'',1585216567413,1,1585216567413,1);
INSERT INTO `fs_member_resource` VALUES (32,'数据加工','商业智能:数据加工',31,0,'bi','diagram','',0,1,'',1585384104596,1,1631754486815,1);
INSERT INTO `fs_member_resource` VALUES (33,'添加','商业智能:数据加工:添加',31,32,'bi','diagram','add',0,1,'',1585384138739,1,1631754508516,1);
INSERT INTO `fs_member_resource` VALUES (34,'修改','商业智能:数据加工:修改',31,32,'bi','diagram','modify',0,1,'',1585384152067,1,1631754517030,1);
INSERT INTO `fs_member_resource` VALUES (35,'删除','商业智能:数据加工:删除',31,32,'bi','diagram','delete',0,1,'',1585384168964,1,1631754529772,1);
INSERT INTO `fs_member_resource` VALUES (36,'数据源','商业智能:数据源',31,0,'bi','source','',0,1,'',1585669452789,1,1634614816541,1);
INSERT INTO `fs_member_resource` VALUES (37,'添加','商业智能:数据源:添加',31,36,'bi','source','add',0,1,'',1585669473979,1,1634614829860,1);
INSERT INTO `fs_member_resource` VALUES (38,'修改','商业智能:数据源:修改',31,36,'bi','source','modify',0,1,'',1585669487729,1,1634614838196,1);
INSERT INTO `fs_member_resource` VALUES (39,'删除','商业智能:数据源:删除',31,36,'bi','source','delete',0,1,'',1585669499607,1,1634614850447,1);
INSERT INTO `fs_member_resource` VALUES (40,'数据集','商业智能:数据集',31,0,'bi','dataset','',0,1,'',1585727898953,1,1634614884425,1);
INSERT INTO `fs_member_resource` VALUES (41,'添加','商业智能:数据集:添加',31,40,'bi','dataset','add',0,1,'',1585727916428,1,1634614892519,1);
INSERT INTO `fs_member_resource` VALUES (42,'修改','商业智能:数据集:修改',31,40,'bi','dataset','modify',0,1,'',1585727959930,1,1634614899268,1);
INSERT INTO `fs_member_resource` VALUES (43,'删除','商业智能:数据集:删除',31,40,'bi','dataset','delete',0,1,'',1585727972170,1,1634614913436,1);
INSERT INTO `fs_member_resource` VALUES (45,'分组','人脸识别:分组',44,0,'face','group','',0,1,'',1597299426164,1,1597299426164,1);
INSERT INTO `fs_member_resource` VALUES (46,'人员','人脸识别:人员',44,0,'face','user','',0,1,'',1597299441594,1,1597299441594,1);
INSERT INTO `fs_member_resource` VALUES (47,'添加','人脸识别:分组:添加',44,45,'face','group','add',0,1,'',1597299463846,1,1597299463846,1);
INSERT INTO `fs_member_resource` VALUES (48,'修改','人脸识别:分组:修改',44,45,'face','group','modify',0,1,'',1597299487386,1,1597299487386,1);
INSERT INTO `fs_member_resource` VALUES (49,'删除','人脸识别:分组:删除',44,45,'face','group','delete',0,1,'',1597299507470,1,1597299507470,1);
INSERT INTO `fs_member_resource` VALUES (50,'添加','人脸识别:人员:添加',44,46,'face','user','add',0,1,'',1597299532434,1,1597299532434,1);
INSERT INTO `fs_member_resource` VALUES (51,'修改','人脸识别:人员:修改',44,46,'face','user','modify',0,1,'',1597299549779,1,1597299549779,1);
INSERT INTO `fs_member_resource` VALUES (52,'删除','人脸识别:人员:删除',44,46,'face','user','delete',0,1,'',1597299561750,1,1597299561750,1);
INSERT INTO `fs_member_resource` VALUES (53,'分组','人脸识别:人员:分组',44,46,'face','user','group',0,1,'',1598238834654,1,1598238834654,1);
INSERT INTO `fs_member_resource` VALUES (54,'人像','人脸识别:人像',44,0,'face','photo','',0,1,'',1598324393176,1,1598324393176,1);
INSERT INTO `fs_member_resource` VALUES (55,'添加','人脸识别:人像:添加',44,54,'face','photo','add',0,1,'',1598324413298,1,1598324413298,1);
INSERT INTO `fs_member_resource` VALUES (56,'修改','人脸识别:人像:修改',44,54,'face','photo','modify',0,1,'',1598324427504,1,1598324427504,1);
INSERT INTO `fs_member_resource` VALUES (57,'删除','人脸识别:人像:删除',44,54,'face','photo','delete',0,1,'',1598324443162,1,1598324443162,1);
INSERT INTO `fs_member_resource` VALUES (59,'词库','搜索引擎:词库',58,0,'lucene','dictionary','',0,1,'',1607333381289,1,1607333426485,1);
INSERT INTO `fs_member_resource` VALUES (60,'添加','搜索引擎:词库:添加',58,59,'lucene','dictionary','add',0,1,'',1607333455146,1,1607333455146,1);
INSERT INTO `fs_member_resource` VALUES (61,'修改','搜索引擎:词库:修改',58,59,'lucene','dictionary','modify',0,1,'',1607333470362,1,1607333470362,1);
INSERT INTO `fs_member_resource` VALUES (62,'删除','搜索引擎:词库:删除',58,59,'lucene','dictionary','delete',0,1,'',1607333488826,1,1607333488826,1);
INSERT INTO `fs_member_resource` VALUES (64,'表单模型','在线办公:表单模型',63,0,'oa','formFrame','',0,1,'',1618446338026,1,1618446338026,1);
INSERT INTO `fs_member_resource` VALUES (65,'添加','在线办公:表单模型:添加',63,64,'oa','formFrame','add',0,1,'',1618446365063,1,1618446365063,1);
INSERT INTO `fs_member_resource` VALUES (66,'修改','在线办公:表单模型:修改',63,64,'oa','formFrame','modify',0,1,'',1618446377800,1,1618446377800,1);
INSERT INTO `fs_member_resource` VALUES (67,'删除','在线办公:表单模型:删除',63,64,'oa','formFrame','delete',0,1,'',1618446390411,1,1618446390411,1);
INSERT INTO `fs_member_resource` VALUES (68,'表单数据','在线办公:表单数据',63,0,'oa','formData','',0,1,'',1618446338026,1,1618446338026,1);
INSERT INTO `fs_member_resource` VALUES (69,'添加','在线办公:表单数据:添加',63,68,'oa','formData','add',0,1,'',1618446365063,1,1618446365063,1);
INSERT INTO `fs_member_resource` VALUES (70,'修改','在线办公:表单数据:修改',63,68,'oa','formData','modify',0,1,'',1618446377800,1,1618446377800,1);
INSERT INTO `fs_member_resource` VALUES (71,'删除','在线办公:表单数据:删除',63,68,'oa','formData','delete',0,1,'',1618446390411,1,1618446390411,1);
INSERT INTO `fs_member_resource` VALUES (72,'校验规则','在线办公:校验规则',63,0,'oa','formRegular','',0,1,'',1618446338026,1,1618446338026,1);
INSERT INTO `fs_member_resource` VALUES (73,'添加','在线办公:校验规则:添加',63,72,'oa','formRegular','add',0,1,'',1618446365063,1,1618446365063,1);
INSERT INTO `fs_member_resource` VALUES (74,'修改','在线办公:校验规则:修改',63,72,'oa','formRegular','modify',0,1,'',1618446377800,1,1618446377800,1);
INSERT INTO `fs_member_resource` VALUES (75,'删除','在线办公:校验规则:删除',63,72,'oa','formRegular','delete',0,1,'',1618446390411,1,1618446390411,1);
INSERT INTO `fs_member_resource` VALUES (76,'字典','用户中心:字典',2,0,'member','dictionary','',0,1,'',1528081696180,1,1619512081161,1);
INSERT INTO `fs_member_resource` VALUES (77,'添加','用户中心:字典:添加',2,76,'member','dictionary','add',0,1,'',1528081911866,1,1584590115052,1);
INSERT INTO `fs_member_resource` VALUES (78,'修改','用户中心:字典:修改',2,76,'member','dictionary','modify',0,1,'',1528081955009,1,1584590127509,1);
INSERT INTO `fs_member_resource` VALUES (79,'删除','用户中心:字典:删除',2,76,'member','dictionary','delete',0,1,'',1528081985121,1,1584590135133,1);
INSERT INTO `fs_member_resource` VALUES (80,'工作流程','在线办公:工作流程',63,0,'oa','workflow','',0,1,'',1618446338026,1,1618446338026,1);
INSERT INTO `fs_member_resource` VALUES (81,'添加','在线办公:工作流程:添加',63,80,'oa','workflow','add',0,1,'',1618446365063,1,1618446365063,1);
INSERT INTO `fs_member_resource` VALUES (82,'修改','在线办公:工作流程:修改',63,80,'oa','workflow','modify',0,1,'',1618446377800,1,1618446377800,1);
INSERT INTO `fs_member_resource` VALUES (83,'删除','在线办公:工作流程:删除',63,80,'oa','workflow','delete',0,1,'',1618446390411,1,1618446390411,1);
INSERT INTO `fs_member_resource` VALUES (84,'发布','在线办公:工作流程:发布',63,80,'oa','workflow','publish',0,1,'',1622633473668,1,1622633473668,1);
INSERT INTO `fs_member_resource` VALUES (85,'检索部署','在线办公:工作流程:检索部署',63,80,'oa','workflow','searchDeployment',0,1,'',1622703892647,1,1622703990430,1);
INSERT INTO `fs_member_resource` VALUES (86,'删除部署','在线办公:工作流程:删除部署',63,80,'oa','workflow','deleteDeployment',0,1,'',1622703934998,1,1622703973335,1);
INSERT INTO `fs_member_resource` VALUES (87,'流程审批','在线办公:流程审批',63,0,'oa','approve','',0,1,'',1622708972781,1,1622708972781,1);
INSERT INTO `fs_member_resource` VALUES (88,'新建单据','在线办公:流程审批:新建单据',63,87,'oa','approve','workflow',0,1,'',1622709172273,1,1622709172273,1);
INSERT INTO `fs_member_resource` VALUES (89,'检索历史','在线办公:工作流程:检索历史',63,80,'oa','workflow','searchHistory',0,1,'',1624973449494,1,1624973449494,1);
INSERT INTO `fs_member_resource` VALUES (90,'流程详情','在线办公:工作流程:流程详情',63,80,'oa','workflow','process',0,1,'',1625030518787,1,1625030518787,1);
INSERT INTO `fs_member_resource` VALUES (91,'撤销流程','在线办公:工作流程:撤销流程',63,80,'oa','workflow','deleteProcessInstance',0,1,'',1625033323075,1,1625033323075,1);
INSERT INTO `fs_member_resource` VALUES (92,'删除流程','在线办公:工作流程:删除流程',63,80,'oa','workflow','deleteHistoricProcessInstance',0,1,'',1625033349461,1,1625033349461,1);
INSERT INTO `fs_member_resource` VALUES (93,'挂起流程','在线办公:工作流程:挂起流程',63,80,'oa','workflow','suspendProcessInstance',0,1,'',1625040566076,1,1625040566076,1);
INSERT INTO `fs_member_resource` VALUES (94,'激活流程','在线办公:工作流程:激活流程',63,80,'oa','workflow','activateProcessInstance',0,1,'',1625040586330,1,1625040586330,1);
INSERT INTO `fs_member_resource` VALUES (95,'驳回流程','在线办公:工作流程:驳回流程',63,80,'oa','workflow','reject',0,1,'',1625122337903,1,1625122337903,1);
INSERT INTO `fs_member_resource` VALUES (97,'配置','内容管理:配置',96,0,'cms','setting','',0,1,'',1627462153168,1,1627462300400,1);
INSERT INTO `fs_member_resource` VALUES (98,'载入','内容管理:配置:载入',96,97,'cms','setting','load',0,1,'',1627462649215,1,1627462649215,1);
INSERT INTO `fs_member_resource` VALUES (99,'更改','内容管理:配置:更改',96,97,'cms','setting','change',0,1,'',1627462669393,1,1627462669393,1);
INSERT INTO `fs_member_resource` VALUES (101,'存档','文件存储:存档',100,0,'file','archive','',0,1,'',1627547824301,1,1627547824301,1);
INSERT INTO `fs_member_resource` VALUES (102,'更改','文件存储:存档:更改',100,101,'file','archive','save',0,1,'',1627547864010,1,1627547864010,1);
INSERT INTO `fs_member_resource` VALUES (103,'删除','文件存储:存档:删除',100,101,'file','archive','delete',0,1,'',1627547881301,1,1627547881301,1);
INSERT INTO `fs_member_resource` VALUES (104,'上传','文件存储:存档:上传',100,101,'file','archive','upload',0,1,'',1628069668474,1,1628069668474,1);
INSERT INTO `fs_member_resource` VALUES (105,'下载','文件存储:存档:下载',100,101,'file','archive','download',0,1,'',1628069685308,1,1628069685308,1);
INSERT INTO `fs_member_resource` VALUES (106,'栏目','内容管理:栏目',96,0,'cms','catalog','',0,1,'',1628496213702,1,1628497159085,1);
INSERT INTO `fs_member_resource` VALUES (107,'添加','内容管理:栏目:添加',96,106,'cms','catalog','add',0,1,'',1628496242603,1,1628497164867,1);
INSERT INTO `fs_member_resource` VALUES (108,'修改','内容管理:栏目:修改',96,106,'cms','catalog','modify',0,1,'',1628496253964,1,1628497170688,1);
INSERT INTO `fs_member_resource` VALUES (109,'删除','内容管理:栏目:删除',96,106,'cms','catalog','delete',0,1,'',1628496265504,1,1628497177984,1);
INSERT INTO `fs_member_resource` VALUES (110,'文章','内容管理:文章',96,0,'cms','article','',0,1,'',1628649304363,1,1628649304363,1);
INSERT INTO `fs_member_resource` VALUES (111,'添加','内容管理:文章:添加',96,110,'cms','article','add',0,1,'',1628649318326,1,1628649318326,1);
INSERT INTO `fs_member_resource` VALUES (112,'修改','内容管理:文章:修改',96,110,'cms','article','modify',0,1,'',1628649333630,1,1628649333630,1);
INSERT INTO `fs_member_resource` VALUES (113,'删除','内容管理:文章:删除',96,110,'cms','article','delete',0,1,'',1628649349958,1,1628649349958,1);
INSERT INTO `fs_member_resource` VALUES (114,'评论','内容管理:评论',96,0,'cms','comment','',0,1,'',1628760363465,1,1628760363465,1);
INSERT INTO `fs_member_resource` VALUES (115,'审核','内容管理:评论:审核',96,114,'cms','comment','audit',0,1,'',1628760381768,1,1628760381768,1);
INSERT INTO `fs_member_resource` VALUES (116,'删除','内容管理:评论:删除',96,114,'cms','comment','delete',0,1,'',1628760391045,1,1628760391045,1);
INSERT INTO `fs_member_resource` VALUES (117,'反馈','内容管理:反馈',96,0,'cms','feedback','',0,1,'',1628838812386,1,1628838812386,1);
INSERT INTO `fs_member_resource` VALUES (118,'审核','内容管理:反馈:审核',96,117,'cms','feedback','audit',0,1,'',1628838828851,1,1628838828851,1);
INSERT INTO `fs_member_resource` VALUES (119,'删除','内容管理:反馈:删除',96,117,'cms','feedback','delete',0,1,'',1628838840154,1,1628838840154,1);
INSERT INTO `fs_member_resource` VALUES (120,'标签','内容管理:标签',96,0,'cms','tag','',0,1,'',1628841563164,1,1628841563164,1);
INSERT INTO `fs_member_resource` VALUES (121,'引用','内容管理:引用',96,0,'cms','cite','',0,1,'',1628841573794,1,1628841593925,1);
INSERT INTO `fs_member_resource` VALUES (122,'添加','内容管理:标签:添加',96,120,'cms','tag','add',0,1,'',1628841604527,1,1628841604527,1);
INSERT INTO `fs_member_resource` VALUES (123,'修改','内容管理:标签:修改',96,120,'cms','tag','modify',0,1,'',1628841615952,1,1628842280445,1);
INSERT INTO `fs_member_resource` VALUES (124,'删除','内容管理:标签:删除',96,120,'cms','tag','delete',0,1,'',1628841629948,1,1628841629948,1);
INSERT INTO `fs_member_resource` VALUES (125,'添加','内容管理:引用:添加',96,121,'cms','cite','add',0,1,'',1628841639288,1,1628841639288,1);
INSERT INTO `fs_member_resource` VALUES (126,'修改','内容管理:引用:修改',96,121,'cms','cite','modify',0,1,'',1628841651912,1,1628841651912,1);
INSERT INTO `fs_member_resource` VALUES (127,'删除','内容管理:引用:删除',96,121,'cms','cite','delete',0,1,'',1628841661763,1,1628841661763,1);
INSERT INTO `fs_member_resource` VALUES (128,'结构','商业智能:数据源:结构',31,36,'bi','source','schema',0,1,'',1636543399133,1,1636543399133,1);
INSERT INTO `fs_member_resource` VALUES (129,'检索','商业智能:数据集:检索',31,40,'bi','dataset','search',0,1,'',1637313631642,1,1637313631642,1);
INSERT INTO `fs_member_resource` VALUES (130,'数据报表','商业智能:数据报表',31,0,'bi','visualize','',0,1,'',1637658795954,1,1637658795954,1);
INSERT INTO `fs_member_resource` VALUES (131,'添加','商业智能:数据报表:添加',31,130,'bi','visualize','add',0,1,'',1637658815910,1,1637658815910,1);
INSERT INTO `fs_member_resource` VALUES (132,'修改','商业智能:数据报表:修改',31,130,'bi','visualize','modify',0,1,'',1637658833371,1,1637658833371,1);
INSERT INTO `fs_member_resource` VALUES (133,'删除','商业智能:数据报表:删除',31,130,'bi','visualize','delete',0,1,'',1637658848750,1,1637658848750,1);
INSERT INTO `fs_member_resource` VALUES (134,'页面设计','项目管理:页面设计',188,0,'auto','layout','',0,-1,'',1637658872174,1,1662455123717,1);
INSERT INTO `fs_member_resource` VALUES (135,'添加','项目管理:页面设计:添加',188,134,'auto','layout','add',0,-1,'',1637658885131,1,1662455137977,1);
INSERT INTO `fs_member_resource` VALUES (136,'修改','项目管理:页面设计:修改',188,134,'auto','layout','modify',0,-1,'',1637658899570,1,1662455152100,1);
INSERT INTO `fs_member_resource` VALUES (137,'删除','项目管理:页面设计:删除',188,134,'auto','layout','delete',0,-1,'',1637658911014,1,1662455164640,1);
INSERT INTO `fs_member_resource` VALUES (138,'检索','商业智能:数据报表:检索',31,130,'bi','visualize','search',0,1,'',1637741322686,1,1637741322686,1);
INSERT INTO `fs_member_resource` VALUES (139,'数据矩阵','商业智能:数据矩阵',31,0,'bi','matrix','',0,1,'',1637886974028,1,1637886974028,1);
INSERT INTO `fs_member_resource` VALUES (140,'添加','商业智能:数据矩阵:添加',31,139,'bi','matrix','add',0,1,'',1637886991599,1,1637886991599,1);
INSERT INTO `fs_member_resource` VALUES (141,'修改','商业智能:数据矩阵:修改',31,139,'bi','matrix','modify',0,1,'',1637887005890,1,1637887005890,1);
INSERT INTO `fs_member_resource` VALUES (142,'删除','商业智能:数据矩阵:删除',31,139,'bi','matrix','delete',0,1,'',1637887016065,1,1637887016065,1);
INSERT INTO `fs_member_resource` VALUES (143,'检索','商业智能:数据矩阵:检索',31,139,'bi','matrix','search',0,1,'',1637887027205,1,1637887027205,1);
INSERT INTO `fs_member_resource` VALUES (144,'在线打印','在线办公:在线打印',63,0,'oa','print','',0,1,'',1646376682020,1,1646376682020,1);
INSERT INTO `fs_member_resource` VALUES (145,'添加','在线办公:在线打印:添加',63,144,'oa','print','add',0,1,'',1646376706771,1,1646376706771,1);
INSERT INTO `fs_member_resource` VALUES (146,'修改','在线办公:在线打印:修改',63,144,'oa','print','modify',0,1,'',1646376719565,1,1646376719565,1);
INSERT INTO `fs_member_resource` VALUES (147,'删除','在线办公:在线打印:删除',63,144,'oa','print','delete',0,1,'',1646376730601,1,1646376730601,1);
INSERT INTO `fs_member_resource` VALUES (149,'作业','定时任务:作业',148,0,'cron','job','',0,1,'',1646977746619,1,1646977756985,1);
INSERT INTO `fs_member_resource` VALUES (150,'添加','定时任务:作业:添加',148,149,'cron','job','add',0,1,'',1646977779484,1,1646977779484,1);
INSERT INTO `fs_member_resource` VALUES (151,'修改','定时任务:作业:修改',148,149,'cron','job','modify',0,1,'',1646977789678,1,1646977789678,1);
INSERT INTO `fs_member_resource` VALUES (152,'删除','定时任务:作业:删除',148,149,'cron','job','delete',0,1,'',1646977800237,1,1646977809644,1);
INSERT INTO `fs_member_resource` VALUES (153,'流程','定时任务:流程',148,0,'cron','flow','',0,1,'',1647311564613,1,1647311564613,1);
INSERT INTO `fs_member_resource` VALUES (154,'添加','定时任务:流程:添加',148,153,'cron','flow','add',0,1,'',1647311575198,1,1647311575198,1);
INSERT INTO `fs_member_resource` VALUES (155,'修改','定时任务:流程:修改',148,153,'cron','flow','modify',0,1,'',1647311596734,1,1647311885637,1);
INSERT INTO `fs_member_resource` VALUES (156,'删除','定时任务:流程:删除',148,153,'cron','flow','delete',0,1,'',1647311896713,1,1647311896713,1);
INSERT INTO `fs_member_resource` VALUES (158,'模型','数据治理:模型',157,0,'govern','model','',0,1,'',1648782582351,1,1649821237598,1);
INSERT INTO `fs_member_resource` VALUES (159,'添加','数据治理:模型:添加',157,158,'govern','model','add',0,1,'',1648782597292,1,1649821250741,1);
INSERT INTO `fs_member_resource` VALUES (160,'修改','数据治理:模型:修改',157,158,'govern','model','modify',0,1,'',1648782610469,1,1649821275807,1);
INSERT INTO `fs_member_resource` VALUES (161,'删除','数据治理:模型:删除',157,158,'govern','model','delete',0,1,'',1648782620556,1,1649821283653,1);
INSERT INTO `fs_member_resource` VALUES (162,'模型关系','数据治理:模型关系',157,0,'govern','modelRelation','',0,1,'',1649856184604,1,1649856184604,1);
INSERT INTO `fs_member_resource` VALUES (163,'添加','数据治理:模型关系:添加',157,162,'govern','modelRelation','add',0,1,'',1649856205906,1,1649856205906,1);
INSERT INTO `fs_member_resource` VALUES (164,'修改','数据治理:模型关系:修改',157,162,'govern','modelRelation','modify',0,1,'',1649856221977,1,1649856221977,1);
INSERT INTO `fs_member_resource` VALUES (165,'删除','数据治理:模型关系:删除',157,162,'govern','modelRelation','delete',0,1,'',1649856238307,1,1649856238307,1);
INSERT INTO `fs_member_resource` VALUES (166,'数据源','数据治理:数据源',157,0,'govern','source','',0,1,'',1651288127259,1,1651288127259,1);
INSERT INTO `fs_member_resource` VALUES (167,'添加','数据治理:数据源:添加',157,166,'govern','source','add',0,1,'',1651288142907,1,1651288142907,1);
INSERT INTO `fs_member_resource` VALUES (168,'修改','数据治理:数据源:修改',157,166,'govern','source','modify',0,1,'',1651288155369,1,1651288155369,1);
INSERT INTO `fs_member_resource` VALUES (169,'删除','数据治理:数据源:删除',157,166,'govern','source','delete',0,1,'',1651288169410,1,1651288169410,1);
INSERT INTO `fs_member_resource` VALUES (170,'标准','数据治理:标准',157,0,'govern','standard','',0,1,'',1651369369718,1,1651369369718,1);
INSERT INTO `fs_member_resource` VALUES (171,'添加','数据治理:标准:添加',157,170,'govern','standard','add',0,1,'',1651369379754,1,1651369379754,1);
INSERT INTO `fs_member_resource` VALUES (172,'修改','数据治理:标准:修改',157,170,'govern','standard','modify',0,1,'',1651369391457,1,1651369391457,1);
INSERT INTO `fs_member_resource` VALUES (173,'删除','数据治理:标准:删除',157,170,'govern','standard','delete',0,1,'',1651369400862,1,1651369400862,1);
INSERT INTO `fs_member_resource` VALUES (174,'落地评估','数据治理:落地评估',157,0,'govern','assess','',0,1,'',1651369440554,1,1651369440554,1);
INSERT INTO `fs_member_resource` VALUES (175,'添加','数据治理:落地评估:添加',157,174,'govern','assess','add',0,1,'',1651369470508,1,1651369470508,1);
INSERT INTO `fs_member_resource` VALUES (176,'修改','数据治理:落地评估:修改',157,174,'govern','assess','modify',0,1,'',1651369483410,1,1651369483410,1);
INSERT INTO `fs_member_resource` VALUES (177,'删除','数据治理:落地评估:删除',157,174,'govern','assess','delete',0,1,'',1651369492457,1,1651369492457,1);
INSERT INTO `fs_member_resource` VALUES (178,'质检分类','数据治理:质检分类',157,0,'govern','qualityLogic','',0,1,'',1651369515566,1,1676538075225,1);
INSERT INTO `fs_member_resource` VALUES (179,'添加','数据治理:质检分类:添加',157,178,'govern','qualityLogic','add',0,1,'',1651369527926,1,1676538079211,1);
INSERT INTO `fs_member_resource` VALUES (180,'修改','数据治理:质检分类:修改',157,178,'govern','qualityLogic','modify',0,1,'',1651369543376,1,1676538082296,1);
INSERT INTO `fs_member_resource` VALUES (181,'删除','数据治理:质检分类:删除',157,178,'govern','qualityLogic','delete',0,1,'',1651369553877,1,1676538085082,1);
INSERT INTO `fs_member_resource` VALUES (182,'质检规则','数据治理:质检规则',157,0,'govern','qualityRule','',0,1,'',1651369579119,1,1676538112547,1);
INSERT INTO `fs_member_resource` VALUES (183,'添加','数据治理:质检规则:添加',157,182,'govern','qualityRule','add',0,1,'',1651369589073,1,1676538118982,1);
INSERT INTO `fs_member_resource` VALUES (184,'修改','数据治理:质检规则:修改',157,182,'govern','qualityRule','modify',0,1,'',1651369599391,1,1676538123271,1);
INSERT INTO `fs_member_resource` VALUES (185,'删除','数据治理:质检规则:删除',157,182,'govern','qualityRule','delete',0,1,'',1651369607940,1,1676538128949,1);
INSERT INTO `fs_member_resource` VALUES (186,'评估结果','数据治理:落地评估:评估结果',157,174,'govern','assess','log',0,1,'',1652748865504,1,1652748865504,1);
INSERT INTO `fs_member_resource` VALUES (187,'删除日志','数据治理:落地评估:删除日志',157,174,'govern','assess','clear',0,1,'',1652748880199,1,1652748880199,1);
INSERT INTO `fs_member_resource` VALUES (189,'质检方案','数据治理:质检方案',157,0,'govern','qualityPlan','',0,1,'',1676855898630,1,1676855898630,1);
INSERT INTO `fs_member_resource` VALUES (190,'质检报告','数据治理:质检方案:质检报告',157,189,'govern','qualityPlan','log',0,1,'',1676855950410,1,1676858921681,1);
INSERT INTO `fs_member_resource` VALUES (191,'添加','数据治理:质检方案:添加',157,189,'govern','qualityPlan','add',0,1,'',1651369589073,1,1676538118982,1);
INSERT INTO `fs_member_resource` VALUES (192,'修改','数据治理:质检方案:修改',157,189,'govern','qualityPlan','modify',0,1,'',1651369599391,1,1676538123271,1);
INSERT INTO `fs_member_resource` VALUES (193,'删除','数据治理:质检方案:删除',157,189,'govern','qualityPlan','delete',0,1,'',1651369607940,1,1676538128949,1);
INSERT INTO `fs_member_resource` VALUES (194,'删除日志','数据治理:质检方案:删除日志',157,189,'govern','qualityPlan','clear',0,1,'',1651369589073,1,1676858947132,1);
INSERT INTO `fs_member_resource` VALUES (195,'应用设计','项目管理:应用设计',0,0,'app','app','',0,1,'',1651369599391,1,1676859114485,1);
INSERT INTO `fs_member_resource` VALUES (196,'管理','项目管理:应用设计:管理',188,195,'auto','app','manage',0,-1,'',1651369607940,1,1676859157557,1);
INSERT INTO `fs_member_resource` VALUES (197,'应用','用户中心:应用',2,0,'member','application','',0,1,'',1729752753541,1,1729752753541,1);
INSERT INTO `fs_member_resource` VALUES (198,'添加','用户中心:应用:添加',2,197,'member','application','add',0,1,'',1729752764491,1,1729752764491,1);
INSERT INTO `fs_member_resource` VALUES (199,'修改','用户中心:应用:修改',2,197,'member','application','modify',0,1,'',1729752780318,1,1729752780318,1);
INSERT INTO `fs_member_resource` VALUES (200,'删除','用户中心:应用:删除',2,197,'member','application','delete',0,1,'',1729752794411,1,1729752794411,1);
INSERT INTO `fs_member_resource` VALUES (201,'菜单','用户中心:应用:菜单',2,197,'member','application','menu',0,1,'',1730102855084,1,1730102855084,1);
INSERT INTO `fs_member_resource` VALUES (202,'资源','用户中心:应用:资源',2,197,'member','application','resource',0,1,'',1730102870872,1,1730102870872,1);
INSERT INTO `fs_member_resource` VALUES (203,'应用','用户中心:角色:应用',2,5,'member','role','application',0,1,'',1732097066641,1,1732097066641,1);
/*!40000 ALTER TABLE `fs_member_resource` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `fs_member_user`
--

DROP TABLE IF EXISTS `fs_member_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `fs_member_user` (
  `id` int NOT NULL AUTO_INCREMENT,
  `serial` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `name` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `password` char(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `salt` char(4) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `sort` tinyint NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '0',
  `description` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `created_time` bigint NOT NULL DEFAULT '0',
  `created_ip` varchar(16) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `created_uid` int NOT NULL DEFAULT '0',
  `updated_time` bigint NOT NULL DEFAULT '0',
  `updated_uid` int NOT NULL DEFAULT '0',
  `logined_time` bigint NOT NULL DEFAULT '0',
  `logined_ip` varchar(16) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `locked_time` bigint NOT NULL DEFAULT '0',
  `deleted_time` bigint NOT NULL DEFAULT '0',
  `deleted_uid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `unq_serial` (`serial`) USING BTREE,
  KEY `idx_name` (`name`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `fs_member_user`
--

LOCK TABLES `fs_member_user` WRITE;
/*!40000 ALTER TABLE `fs_member_user` DISABLE KEYS */;
INSERT INTO `fs_member_user` VALUES (1,'admin','管理员','4bd18037cb256efcc6bd6363c558e401','8395',0,1,'',1528081552985,'127.0.0.1',1,1528081552985,1,1736144038843,'127.0.0.1',0,0,0);
INSERT INTO `fs_member_user` VALUES (2,'test','测试123','4b361be828611add84453a24f39772a5','0905',0,1,'',1528081567988,'127.0.0.1',1,1542958281919,1,1528267171953,'127.0.0.1',0,0,0);
INSERT INTO `fs_member_user` VALUES (3,'111','111','','',6,-1,'',0,'',0,1658215959314,1,0,'',0,1735029204458,1);
INSERT INTO `fs_member_user` VALUES (4,'222','222','','',0,1,'',0,'',0,1629363081609,1,0,'',1630054276000,0,0);
INSERT INTO `fs_member_user` VALUES (5,'333','333','','',0,1,'',0,'',0,1606803617105,1,0,'',0,0,0);
INSERT INTO `fs_member_user` VALUES (6,'444','444','','',0,1,'',0,'',0,0,0,0,'',0,0,0);
INSERT INTO `fs_member_user` VALUES (7,'555','555','','',0,1,'',0,'',0,0,0,0,'',0,0,0);
INSERT INTO `fs_member_user` VALUES (8,'666','666','','',0,1,'',0,'',0,0,0,0,'',0,0,0);
INSERT INTO `fs_member_user` VALUES (9,'777','777','','',0,1,'',0,'',0,0,0,0,'',0,0,0);
INSERT INTO `fs_member_user` VALUES (10,'888','888','','',0,1,'',0,'',0,0,0,0,'',0,0,0);
INSERT INTO `fs_member_user` VALUES (11,'999','999','','',0,1,'',0,'',0,0,0,0,'',0,0,0);
INSERT INTO `fs_member_user` VALUES (12,'124124','124124','','',0,1,'',0,'',0,0,0,0,'',0,1735029204458,1);
INSERT INTO `fs_member_user` VALUES (13,'55555','55555','','',0,1,'',0,'',0,0,0,0,'',0,0,0);
INSERT INTO `fs_member_user` VALUES (14,'777777','777777','','',0,1,'',0,'',0,0,0,0,'',0,0,0);
INSERT INTO `fs_member_user` VALUES (15,'444444','444444','','',0,0,' ',0,'',0,0,0,0,'',0,0,0);
INSERT INTO `fs_member_user` VALUES (16,'90909','9090909','','',0,0,'  ',0,'',0,0,0,0,'',0,0,0);
INSERT INTO `fs_member_user` VALUES (17,'ttt','ttt','31b063d036577dcf2f39b2859ffae762','0639',0,1,'',1645774049481,'127.0.0.1',1,1645774049481,1,0,'',0,0,0);
INSERT INTO `fs_member_user` VALUES (18,'gfdd','hfghfgh','b5fd16f3a2d74f05c8a1f5527cc206be','1394',0,1,'',1645774323385,'127.0.0.1',1,1645774323385,1,0,'',0,0,0);
INSERT INTO `fs_member_user` VALUES (20,'xxxx','xxx','31b063d036577dcf2f39b2859ffae762','0639',0,1,'',1645774049481,'127.0.0.1',1,1645774049481,1,0,'',0,0,0);
INSERT INTO `fs_member_user` VALUES (21,'fsdf','sdgfsdg','03789c4092774d1fb04fe87f1f9b4d93','1284',0,1,'',1645776653434,'127.0.0.1',1,1645776653434,1,0,'',0,0,0);
INSERT INTO `fs_member_user` VALUES (22,'sdg','sdg','6f424ef6baddab83375741935b6d425a','4932',0,1,'',1645776911255,'127.0.0.1',1,1645776911255,1,0,'',0,0,0);
INSERT INTO `fs_member_user` VALUES (23,'sdg4','sdg4','fdb917c956cf2c96819ed4ddf3730816','3605',0,1,'',1645777390270,'127.0.0.1',1,1645777390270,1,0,'',0,0,0);
INSERT INTO `fs_member_user` VALUES (24,'xxsssxx','xsssxx','31b063d036577dcf2f39b2859ffae762','0639',0,1,'',1645774049481,'127.0.0.1',1,1645774049481,1,0,'',0,0,0);
/*!40000 ALTER TABLE `fs_member_user` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-01-07  9:09:12
