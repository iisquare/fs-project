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

 Date: 25/04/2022 14:16:12
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for fs_govern_model_relation
-- ----------------------------
DROP TABLE IF EXISTS `fs_govern_model_relation`;
CREATE TABLE `fs_govern_model_relation`  (
  `id` int(0) NOT NULL AUTO_INCREMENT,
  `source_catalog` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `source_model` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `source_column` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `relation` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `target_catalog` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `target_model` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `target_column` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `description` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `created_uid` int(0) NOT NULL DEFAULT 0,
  `created_time` bigint(0) NOT NULL DEFAULT 0,
  `updated_uid` int(0) NOT NULL DEFAULT 0,
  `updated_time` bigint(0) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_source_catalog`(`source_catalog`) USING BTREE,
  INDEX `idx_source_column`(`source_column`) USING BTREE,
  INDEX `idx_target_catalog`(`target_catalog`) USING BTREE,
  INDEX `idx_target_column`(`target_column`) USING BTREE,
  INDEX `idx_relation`(`relation`) USING BTREE,
  INDEX `idx_source_model`(`source_model`) USING BTREE,
  INDEX `idx_target_model`(`target_model`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for fs_govern_model_column
-- ----------------------------
DROP TABLE IF EXISTS `fs_govern_model_column`;
CREATE TABLE `fs_govern_model_column`  (
  `catalog` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `model` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `code` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `type` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `size` int(0) NOT NULL DEFAULT 0,
  `digit` int(0) NOT NULL DEFAULT 0,
  `nullable` int(0) NOT NULL DEFAULT 0,
  `sort` int(0) NOT NULL DEFAULT 0,
  `description` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  PRIMARY KEY (`catalog`, `model`, `code`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for fs_govern_model
-- ----------------------------
DROP TABLE IF EXISTS `fs_govern_model`;
CREATE TABLE `fs_govern_model`  (
  `catalog` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `code` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `type` varchar(120) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `pk` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `sort` int(0) NOT NULL DEFAULT 0,
  `description` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `created_uid` int(0) NOT NULL DEFAULT 0,
  `created_time` bigint(0) NOT NULL DEFAULT 0,
  `updated_uid` int(0) NOT NULL DEFAULT 0,
  `updated_time` bigint(0) NOT NULL DEFAULT 0,
  PRIMARY KEY (`catalog`, `code`) USING BTREE,
  INDEX `idx_type`(`type`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
