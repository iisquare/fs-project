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

 Date: 29/08/2022 10:01:22
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for fs_govern_assess
-- ----------------------------
DROP TABLE IF EXISTS `fs_govern_assess`;
CREATE TABLE `fs_govern_assess`  (
  `id` int(0) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `source` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `content` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `sort` int(0) NOT NULL DEFAULT 0,
  `status` tinyint(0) NOT NULL DEFAULT 0,
  `description` tinytext CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `created_uid` int(0) NOT NULL DEFAULT 0,
  `created_time` bigint(0) NOT NULL DEFAULT 0,
  `updated_uid` int(0) NOT NULL DEFAULT 0,
  `updated_time` bigint(0) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for fs_govern_assess_log
-- ----------------------------
DROP TABLE IF EXISTS `fs_govern_assess_log`;
CREATE TABLE `fs_govern_assess_log`  (
  `id` int(0) NOT NULL AUTO_INCREMENT,
  `assess` int(0) NOT NULL DEFAULT 0,
  `standard` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `source` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `model` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `code` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `level` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `name` tinyint(0) NOT NULL DEFAULT 0,
  `type` tinyint(0) NOT NULL DEFAULT 0,
  `size` tinyint(0) NOT NULL DEFAULT 0,
  `digit` tinyint(0) NOT NULL DEFAULT 0,
  `nullable` tinyint(0) NOT NULL DEFAULT 0,
  `detail` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `check_time` bigint(0) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_assess`(`assess`) USING BTREE,
  INDEX `idx_standard`(`standard`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

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
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for fs_govern_quality_logic
-- ----------------------------
DROP TABLE IF EXISTS `fs_govern_quality_logic`;
CREATE TABLE `fs_govern_quality_logic`  (
  `catalog` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `code` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `path` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `mold` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `content` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `arg` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `suggest` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `sort` int(0) NOT NULL DEFAULT 0,
  `status` tinyint(0) NOT NULL DEFAULT 0,
  `description` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `created_uid` int(0) NOT NULL DEFAULT 0,
  `created_time` bigint(0) NOT NULL DEFAULT 0,
  `updated_uid` int(0) NOT NULL DEFAULT 0,
  `updated_time` bigint(0) NOT NULL DEFAULT 0,
  PRIMARY KEY (`catalog`, `code`) USING BTREE,
  UNIQUE INDEX `uniq_path`(`path`) USING BTREE,
  INDEX `idx_code`(`code`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for fs_govern_source
-- ----------------------------
DROP TABLE IF EXISTS `fs_govern_source`;
CREATE TABLE `fs_govern_source`  (
  `code` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `type` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `version` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `content` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `sort` int(0) NOT NULL DEFAULT 0,
  `status` tinyint(0) NOT NULL DEFAULT 0,
  `description` tinytext CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `created_uid` int(0) NOT NULL DEFAULT 0,
  `created_time` bigint(0) NOT NULL DEFAULT 0,
  `updated_uid` int(0) NOT NULL DEFAULT 0,
  `updated_time` bigint(0) NOT NULL DEFAULT 0,
  PRIMARY KEY (`code`) USING BTREE,
  INDEX `idx_type`(`type`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for fs_govern_standard
-- ----------------------------
DROP TABLE IF EXISTS `fs_govern_standard`;
CREATE TABLE `fs_govern_standard`  (
  `catalog` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `code` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `path` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `mold` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `another` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `flag` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `level` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `type` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `size` int(0) NOT NULL DEFAULT 0,
  `digit` int(0) NOT NULL DEFAULT 0,
  `nullable` int(0) NOT NULL DEFAULT 0,
  `sort` int(0) NOT NULL DEFAULT 0,
  `status` tinyint(0) NOT NULL DEFAULT 0,
  `description` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `created_uid` int(0) NOT NULL DEFAULT 0,
  `created_time` bigint(0) NOT NULL DEFAULT 0,
  `updated_uid` int(0) NOT NULL DEFAULT 0,
  `updated_time` bigint(0) NOT NULL DEFAULT 0,
  PRIMARY KEY (`catalog`, `code`) USING BTREE,
  UNIQUE INDEX `uniq_path`(`path`) USING BTREE,
  INDEX `idx_code`(`code`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
