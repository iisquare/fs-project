/*
 Navicat Premium Data Transfer

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 80021
 Source Host           : localhost:3306
 Source Schema         : fs_project

 Target Server Type    : MySQL
 Target Server Version : 80021
 File Encoding         : 65001

 Date: 10/11/2021 16:11:20
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for fs_bi_dataset
-- ----------------------------
DROP TABLE IF EXISTS `fs_bi_dataset`;
CREATE TABLE `fs_bi_dataset`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '名称',
  `collection` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '数据集合名',
  `content` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '数据源配置',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态',
  `description` tinytext CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '描述',
  `created_uid` int NOT NULL DEFAULT 0 COMMENT '创建者',
  `created_time` bigint NOT NULL DEFAULT 0 COMMENT '创建时间',
  `updated_uid` int NOT NULL DEFAULT 0 COMMENT '修改者',
  `updated_time` bigint NOT NULL DEFAULT 0 COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '流程图信息' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of fs_bi_dataset
-- ----------------------------
INSERT INTO `fs_bi_dataset` VALUES (1, 'MySQL测试', '', '{}', 0, 1, 'sa', 1, 1634781824646, 1, 1636440862032);
INSERT INTO `fs_bi_dataset` VALUES (2, 'Mongo测试', '', '{}', 0, 1, '', 1, 1634781928370, 1, 1635323252448);

-- ----------------------------
-- Table structure for fs_bi_diagram
-- ----------------------------
DROP TABLE IF EXISTS `fs_bi_diagram`;
CREATE TABLE `fs_bi_diagram`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '名称',
  `engine` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '计算引擎',
  `model` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '处理模式',
  `content` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '计算规则有向无环图',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态',
  `description` tinytext CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '描述',
  `created_uid` int NOT NULL DEFAULT 0 COMMENT '创建者',
  `created_time` bigint NOT NULL DEFAULT 0 COMMENT '创建时间',
  `updated_uid` int NOT NULL DEFAULT 0 COMMENT '修改者',
  `updated_time` bigint NOT NULL DEFAULT 0 COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_engine`(`engine`) USING BTREE,
  INDEX `idx_model`(`model`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '流程图信息' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of fs_bi_diagram
-- ----------------------------
INSERT INTO `fs_bi_diagram` VALUES (2, 'Spark-离线批处理测试', 'spark', 'batch', '{\"canvas\":{\"width\":500,\"height\":500,\"top\":196.5},\"items\":[{\"id\":\"item_1\",\"name\":\"JDBC_1\",\"icon\":\"dagSource\",\"x\":140,\"y\":99,\"index\":1,\"type\":\"JDBCSource\",\"description\":\"JDBC输入\",\"options\":{\"driver\":\"com.mysql.jdbc.Driver\",\"url\":\"jdbc:mysql://localhost:3306/fs_project?characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true\",\"username\":\"root\",\"password\":\"admin888\",\"iterable\":false,\"partitionColumn\":\"\",\"lowerBound\":\"\",\"upperBound\":\"\",\"numPartitions\":0,\"fetchSize\":0,\"sql\":\"select * from fs_member_user limit 3\"},\"alias\":\"user\",\"kvConfigPrefix\":\"\"},{\"id\":\"item_10\",\"name\":\"Console_10\",\"icon\":\"dagSink\",\"x\":54,\"y\":298,\"index\":10,\"type\":\"ConsoleSink\",\"description\":\"Console输出\",\"options\":{\"echoConfig\":false,\"mode\":\"\"},\"kvConfigPrefix\":\"\"},{\"id\":\"item_11\",\"name\":\"Elasticsearch_11\",\"icon\":\"dagSink\",\"x\":324,\"y\":291,\"index\":11,\"type\":\"ElasticsearchSink\",\"description\":\"Elasticsearch输出\",\"options\":{\"servers\":\"127.0.0.1:9200\",\"username\":\"\",\"password\":\"\",\"collection\":\"fs_test\",\"batchSize\":200,\"flushInterval\":-1,\"idField\":\"\",\"tableField\":\"_table\",\"mode\":\"index\"},\"kvConfigPrefix\":\"\"}],\"relations\":[{\"sourceId\":\"item_1\",\"targetId\":\"item_10\",\"sourceAnchor\":\"BottomCenter\",\"targetAnchor\":\"TopCenter\"},{\"sourceId\":\"item_1\",\"targetId\":\"item_11\",\"sourceAnchor\":\"BottomCenter\",\"targetAnchor\":\"TopCenter\"}]}', 7, 1, '', 1, 1631674988145, 1, 1636512980348);
INSERT INTO `fs_bi_diagram` VALUES (3, 'Spark-在线流处理测试', 'spark', 'stream', '{\"canvas\":{\"width\":500,\"height\":500,\"top\":154.5},\"items\":[{\"id\":\"item_31\",\"name\":\"Console_31\",\"icon\":\"dagSink\",\"x\":240,\"y\":369,\"index\":31,\"type\":\"ConsoleSink\",\"description\":\"Console输出\",\"options\":{\"echoConfig\":true,\"mode\":\"line\"},\"kvConfigPrefix\":\"\"},{\"id\":\"item_34\",\"name\":\"JDBC_34\",\"icon\":\"dagSource\",\"x\":86,\"y\":209,\"index\":34,\"type\":\"JDBCSource\",\"description\":\"JDBC输入\",\"options\":{\"driver\":\"MySQL\",\"url\":\"jdbc:mysql://127.0.0.1:3306/fs-project?characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true\",\"username\":\"root\",\"password\":\"admin888\",\"iterable\":false,\"sql\":\"select * from fs_member_role\"},\"alias\":\"\",\"kvConfigPrefix\":\"\"},{\"id\":\"item_35\",\"name\":\"JSON_35\",\"icon\":\"dagConfig\",\"x\":337,\"y\":104,\"index\":35,\"type\":\"JSONConfig\",\"description\":\"JSON参数\",\"options\":{\"json\":\"{}\"}}],\"relations\":[{\"sourceId\":\"item_34\",\"targetId\":\"item_31\",\"sourceAnchor\":\"BottomCenter\",\"targetAnchor\":\"TopCenter\"},{\"sourceId\":\"item_35\",\"targetId\":\"item_31\",\"sourceAnchor\":\"BottomCenter\",\"targetAnchor\":\"TopCenter\"}]}', 8, 1, '', 1, 1631858852189, 1, 1634172510556);
INSERT INTO `fs_bi_diagram` VALUES (4, 'Flink-离线批处理测试', 'flink', 'batch', '{\"canvas\":{\"width\":500,\"height\":500,\"top\":154.5},\"items\":[{\"id\":\"item_1\",\"name\":\"Script_1\",\"icon\":\"dagScript\",\"x\":127,\"y\":148,\"index\":1,\"type\":\"ScriptTransform\",\"description\":\"逻辑脚本\",\"options\":{\"jarURI\":\"file:/D:/htdocs/fs-project-vip/java/plugins/flink/build/libs/fs-project-plugins-flink-0.0.1-SNAPSHOT.jar\",\"pkgClass\":\"com.iisquare.fs.plugins.flink.TestTransform\"},\"alias\":\"\",\"kvConfigPrefix\":\"\"},{\"id\":\"item_2\",\"name\":\"Console_2\",\"icon\":\"dagSink\",\"x\":186,\"y\":331,\"index\":2,\"type\":\"ConsoleSink\",\"description\":\"Console输出\",\"options\":{\"echoConfig\":false,\"mode\":\"line\"},\"kvConfigPrefix\":\"\"}],\"relations\":[{\"sourceId\":\"item_1\",\"targetId\":\"item_2\",\"sourceAnchor\":\"BottomCenter\",\"targetAnchor\":\"TopCenter\"}]}', 9, 1, '', 1, 1631858899715, 1, 1634172150472);
INSERT INTO `fs_bi_diagram` VALUES (5, 'Sample-访问日志处理', 'flink', 'stream', '{\"canvas\":{\"width\":500,\"height\":500,\"top\":63},\"items\":[{\"id\":\"item_1\",\"name\":\"Kafka_1\",\"icon\":\"dagSource\",\"x\":139,\"y\":76,\"index\":1,\"type\":\"KafkaSource\",\"description\":\"Kafka输入\",\"options\":{\"bootstrap\":\"kafka:9092\",\"zookeeper\":\"zookeeper:2181/kafka\",\"offset\":\"earliest\",\"group\":\"fs-bi\",\"topic\":\"fs-access-log\",\"commitInterval\":1000},\"alias\":\"\",\"kvConfigPrefix\":\"\"},{\"id\":\"item_2\",\"name\":\"Console_2\",\"icon\":\"dagSink\",\"x\":298,\"y\":370,\"index\":2,\"type\":\"ConsoleSink\",\"description\":\"Console输出\",\"options\":{\"echoConfig\":false,\"mode\":\"\"},\"kvConfigPrefix\":\"\"},{\"id\":\"item_3\",\"name\":\"Script_3\",\"icon\":\"dagScript\",\"x\":176,\"y\":182,\"index\":3,\"type\":\"ScriptTransform\",\"description\":\"逻辑脚本\",\"options\":{\"jarURI\":\"\",\"pkgClass\":\"com.iisquare.fs.app.flink.script.FAnalyseScript\"},\"alias\":\"\",\"kvConfigPrefix\":\"\"},{\"id\":\"item_4\",\"name\":\"Elasticsearch_4\",\"icon\":\"dagSink\",\"x\":33,\"y\":355,\"index\":4,\"type\":\"ElasticsearchSink\",\"description\":\"Elasticsearch输出\",\"options\":{\"cluster\":\"elasticsearch\",\"servers\":\"127.0.0.1:9200\",\"username\":\"\",\"password\":\"\",\"collection\":\"fs_access_log\",\"batchSize\":1,\"idField\":\"_id\",\"indexField\":\"_table\",\"tableField\":\"_table\",\"flushInterval\":-1},\"kvConfigPrefix\":\"\"}],\"relations\":[{\"sourceId\":\"item_1\",\"targetId\":\"item_3\",\"sourceAnchor\":\"BottomCenter\",\"targetAnchor\":\"TopCenter\"},{\"sourceId\":\"item_3\",\"targetId\":\"item_2\",\"sourceAnchor\":\"BottomCenter\",\"targetAnchor\":\"TopCenter\"},{\"sourceId\":\"item_3\",\"targetId\":\"item_4\",\"sourceAnchor\":\"BottomCenter\",\"targetAnchor\":\"TopCenter\"}]}', 0, 1, '', 1, 1631858913325, 1, 1634172133717);
INSERT INTO `fs_bi_diagram` VALUES (6, 'Sample-公共配置', 'spark', 'batch', '{\"canvas\":{\"width\":500,\"height\":500,\"top\":154.5},\"items\":[{\"id\":\"item_1\",\"name\":\"JSON_1\",\"icon\":\"dagConfig\",\"x\":178,\"y\":106,\"index\":1,\"type\":\"JSONConfig\",\"alias\":\"\",\"description\":\"JSON参数\",\"kvConfigPrefix\":\"\",\"options\":{\"json\":\"{\\\"date\\\":\\\"2021-03-30\\\"}\"}},{\"id\":\"item_2\",\"name\":\"Export_2\",\"icon\":\"dagConfig\",\"x\":158,\"y\":231,\"index\":2,\"type\":\"ExportConfig\",\"alias\":\"\",\"description\":\"导出当前规则\",\"kvConfigPrefix\":\"\",\"options\":{}}],\"relations\":[{\"sourceId\":\"item_1\",\"targetId\":\"item_2\",\"sourceAnchor\":\"BottomCenter\",\"targetAnchor\":\"TopCenter\"}]}', 0, 1, '', 1, 1632442644428, 1, 1634172140299);
INSERT INTO `fs_bi_diagram` VALUES (7, 'Flink-在线流处理测试', 'flink', 'stream', '{\"canvas\":{\"width\":500,\"height\":500,\"top\":154.5},\"items\":[{\"id\":\"item_1\",\"name\":\"Kafka_1\",\"icon\":\"dagSource\",\"x\":139,\"y\":76,\"index\":1,\"type\":\"KafkaSource\",\"description\":\"Kafka输入\",\"options\":{\"bootstrap\":\"kafka:9092\",\"zookeeper\":\"zookeeper:2181/kafka\",\"offset\":\"earliest\",\"group\":\"fs-bi\",\"topic\":\"fs-access-log\",\"commitInterval\":1000},\"alias\":\"\",\"kvConfigPrefix\":\"\"},{\"id\":\"item_2\",\"name\":\"Console_2\",\"icon\":\"dagSink\",\"x\":298,\"y\":370,\"index\":2,\"type\":\"ConsoleSink\",\"description\":\"Console输出\",\"options\":{\"echoConfig\":false,\"mode\":\"line\"},\"kvConfigPrefix\":\"\"},{\"id\":\"item_3\",\"name\":\"Script_3\",\"icon\":\"dagScript\",\"x\":176,\"y\":182,\"index\":3,\"type\":\"ScriptTransform\",\"description\":\"逻辑脚本\",\"options\":{\"jarURI\":\"file:/D:/htdocs/fs-project-vip/java/base/dag/build/libs/fs-project-base-dag-0.0.1-SNAPSHOT.jar\",\"pkgClass\":\"com.iisquare.fs.app.flink.script.FAnalyseScript\"},\"alias\":\"\",\"kvConfigPrefix\":\"\"},{\"id\":\"item_4\",\"name\":\"Elasticsearch_4\",\"icon\":\"dagSink\",\"x\":33,\"y\":355,\"index\":4,\"type\":\"ElasticsearchSink\",\"description\":\"Elasticsearch输出\",\"options\":{\"cluster\":\"elasticsearch\",\"servers\":\"127.0.0.1:9200\",\"username\":\"\",\"password\":\"\",\"collection\":\"fs_access_log\",\"batchSize\":1,\"idField\":\"_id\",\"indexField\":\"_table\",\"tableField\":\"_table\",\"flushInterval\":-1},\"kvConfigPrefix\":\"\"}],\"relations\":[{\"sourceId\":\"item_1\",\"targetId\":\"item_3\",\"sourceAnchor\":\"BottomCenter\",\"targetAnchor\":\"TopCenter\"},{\"sourceId\":\"item_3\",\"targetId\":\"item_2\",\"sourceAnchor\":\"BottomCenter\",\"targetAnchor\":\"TopCenter\"},{\"sourceId\":\"item_3\",\"targetId\":\"item_4\",\"sourceAnchor\":\"BottomCenter\",\"targetAnchor\":\"TopCenter\"}]}', 10, 1, '', 1, 1634027093441, 1, 1634172147120);
INSERT INTO `fs_bi_diagram` VALUES (8, 'Sample-字段转换', 'spark', 'batch', '{\"canvas\":{\"width\":500,\"height\":500,\"top\":154.5},\"items\":[{\"id\":\"item_1\",\"name\":\"JDBC_1\",\"icon\":\"dagSource\",\"x\":172,\"y\":86,\"index\":1,\"type\":\"JDBCSource\",\"description\":\"JDBC输入\",\"options\":{\"driver\":\"MySQL\",\"url\":\"jdbc:mysql://localhost:3306/fs_project?characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true\",\"username\":\"root\",\"password\":\"admin888\",\"iterable\":false,\"partitionColumn\":\"\",\"lowerBound\":\"\",\"upperBound\":\"\",\"numPartitions\":0,\"fetchSize\":0,\"sql\":\"select * from fs_member_user limit 3\"},\"alias\":\"\",\"kvConfigPrefix\":\"\"},{\"id\":\"item_2\",\"name\":\"Console_2\",\"icon\":\"dagSink\",\"x\":278,\"y\":385,\"index\":2,\"type\":\"ConsoleSink\",\"description\":\"Console输出\",\"options\":{\"echoConfig\":false,\"mode\":\"\"},\"kvConfigPrefix\":\"\"},{\"id\":\"item_3\",\"name\":\"Mongo_3\",\"icon\":\"dagSink\",\"x\":57,\"y\":416,\"index\":3,\"type\":\"MongoSink\",\"description\":\"Mongo输出\",\"options\":{\"hosts\":\"127.0.0.1:27017\",\"database\":\"fs_project\",\"username\":\"root\",\"password\":\"admin888\",\"collection\":\"fs.data\",\"batchSize\":0,\"replaceDocument\":false,\"forceInsert\":false},\"kvConfigPrefix\":\"\"},{\"id\":\"item_4\",\"name\":\"Convert_ID\",\"icon\":\"dagTransform\",\"x\":177,\"y\":203,\"index\":4,\"type\":\"ConvertTransform\",\"description\":\"字段转换\",\"options\":{\"mode\":\"KEEP_SOURCE\",\"items\":[{\"target\":\"_id\",\"source\":\"id\",\"clsType\":\"String\"}]},\"alias\":\"\",\"kvConfigPrefix\":\"\"},{\"id\":\"item_5\",\"name\":\"脱敏处理\",\"icon\":\"dagTransform\",\"x\":142,\"y\":297,\"index\":5,\"type\":\"ConvertTransform\",\"description\":\"字段转换\",\"options\":{\"mode\":\"REMOVE_TARGET\",\"items\":[{\"target\":\"password\",\"source\":\"\",\"clsType\":\"\"}]},\"alias\":\"\",\"kvConfigPrefix\":\"\"}],\"relations\":[{\"sourceId\":\"item_1\",\"targetId\":\"item_4\",\"sourceAnchor\":\"BottomCenter\",\"targetAnchor\":\"TopCenter\"},{\"sourceId\":\"item_4\",\"targetId\":\"item_5\",\"sourceAnchor\":\"BottomCenter\",\"targetAnchor\":\"TopCenter\"},{\"sourceId\":\"item_5\",\"targetId\":\"item_3\",\"sourceAnchor\":\"BottomCenter\",\"targetAnchor\":\"TopCenter\"},{\"sourceId\":\"item_5\",\"targetId\":\"item_2\",\"sourceAnchor\":\"BottomCenter\",\"targetAnchor\":\"TopCenter\"}]}', 0, 1, '', 1, 1634172188749, 1, 1634718737990);

-- ----------------------------
-- Table structure for fs_bi_source
-- ----------------------------
DROP TABLE IF EXISTS `fs_bi_source`;
CREATE TABLE `fs_bi_source`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '名称',
  `type` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '数据源类型',
  `content` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '数据源配置',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态',
  `description` tinytext CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '描述',
  `created_uid` int NOT NULL DEFAULT 0 COMMENT '创建者',
  `created_time` bigint NOT NULL DEFAULT 0 COMMENT '创建时间',
  `updated_uid` int NOT NULL DEFAULT 0 COMMENT '修改者',
  `updated_time` bigint NOT NULL DEFAULT 0 COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_type`(`type`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '流程图信息' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of fs_bi_source
-- ----------------------------
INSERT INTO `fs_bi_source` VALUES (1, 'MySQL项目库', 'MySQL', '{\"url\":\"jdbc:mysql://127.0.0.1:3306/fs_project?characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true\",\"username\":\"root\",\"password\":\"admin888\",\"sql\":\"select * from fs_member_user\"}', 0, 1, '', 1, 1634711509319, 1, 1635323213647);
INSERT INTO `fs_bi_source` VALUES (2, 'MySQL流程库', 'MySQL', '{\"url\":\"jdbc:mysql://127.0.0.1:3306/fs_workflow?characterEncoding=utf-8&useSSL=false&allowPublicKeyRetrieval=true\",\"username\":\"root\",\"password\":\"admin888\",\"sql\":\"select * from fs_member_role\"}', 0, 1, '', 1, 1634711556732, 1, 1635323219788);
INSERT INTO `fs_bi_source` VALUES (3, 'Mongo托管库', 'MongoDB', '{\"hosts\":\"127.0.0.1:27017\",\"database\":\"fs_project\",\"username\":\"root\",\"password\":\"admin888\"}', 0, 1, '', 1, 1634720050109, 1, 1635323225357);

SET FOREIGN_KEY_CHECKS = 1;
