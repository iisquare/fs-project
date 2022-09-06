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

 Date: 06/09/2022 10:44:12
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for fs_auto_layout
-- ----------------------------
DROP TABLE IF EXISTS `fs_auto_layout`;
CREATE TABLE `fs_auto_layout`  (
  `id` int(0) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '名称',
  `content` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '配置',
  `sort` int(0) NOT NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint(0) NOT NULL DEFAULT 0 COMMENT '状态',
  `description` tinytext CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '描述',
  `created_uid` int(0) NOT NULL DEFAULT 0 COMMENT '创建者',
  `created_time` bigint(0) NOT NULL DEFAULT 0 COMMENT '创建时间',
  `updated_uid` int(0) NOT NULL DEFAULT 0 COMMENT '修改者',
  `updated_time` bigint(0) NOT NULL DEFAULT 0 COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
