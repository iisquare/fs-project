/*
 Navicat Premium Data Transfer

 Source Server         : yoyo_3306
 Source Server Type    : MySQL
 Source Server Version : 80021
 Source Host           : 192.168.0.146:13306
 Source Schema         : fs-project

 Target Server Type    : MySQL
 Target Server Version : 80021
 File Encoding         : 65001

 Date: 16/09/2021 09:01:58
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

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
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '流程图信息' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of fs_bi_diagram
-- ----------------------------
INSERT INTO `fs_bi_diagram` VALUES (2, 'test', '', 'batch', '', 0, 1, '', 1, 1631674988145, 1, 1631674988145);

SET FOREIGN_KEY_CHECKS = 1;
