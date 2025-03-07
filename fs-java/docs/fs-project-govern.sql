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

 Date: 20/02/2023 10:24:31
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for fs_govern_assess
-- ----------------------------
DROP TABLE IF EXISTS `fs_govern_assess`;
CREATE TABLE `fs_govern_assess`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `source` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `content` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `sort` int NOT NULL DEFAULT 0,
  `status` tinyint NOT NULL DEFAULT 0,
  `description` tinytext CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `created_uid` int NOT NULL DEFAULT 0,
  `created_time` bigint NOT NULL DEFAULT 0,
  `updated_uid` int NOT NULL DEFAULT 0,
  `updated_time` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for fs_govern_assess_log
-- ----------------------------
DROP TABLE IF EXISTS `fs_govern_assess_log`;
CREATE TABLE `fs_govern_assess_log`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `assess` int NOT NULL DEFAULT 0,
  `standard` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `source` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `model` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `code` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `level` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `name` tinyint NOT NULL DEFAULT 0,
  `type` tinyint NOT NULL DEFAULT 0,
  `size` tinyint NOT NULL DEFAULT 0,
  `digit` tinyint NOT NULL DEFAULT 0,
  `nullable` tinyint NOT NULL DEFAULT 0,
  `detail` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `check_time` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_assess`(`assess` ASC) USING BTREE,
  INDEX `idx_standard`(`standard` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

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
  `sort` int NOT NULL DEFAULT 0,
  `description` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `created_uid` int NOT NULL DEFAULT 0,
  `created_time` bigint NOT NULL DEFAULT 0,
  `updated_uid` int NOT NULL DEFAULT 0,
  `updated_time` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`catalog`, `code`) USING BTREE,
  INDEX `idx_type`(`type` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

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
  `size` int NOT NULL DEFAULT 0,
  `digit` int NOT NULL DEFAULT 0,
  `nullable` int NOT NULL DEFAULT 0,
  `sort` int NOT NULL DEFAULT 0,
  `description` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  PRIMARY KEY (`catalog`, `model`, `code`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for fs_govern_model_relation
-- ----------------------------
DROP TABLE IF EXISTS `fs_govern_model_relation`;
CREATE TABLE `fs_govern_model_relation`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `source_catalog` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `source_model` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `source_column` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `relation` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `target_catalog` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `target_model` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `target_column` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `description` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `created_uid` int NOT NULL DEFAULT 0,
  `created_time` bigint NOT NULL DEFAULT 0,
  `updated_uid` int NOT NULL DEFAULT 0,
  `updated_time` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_source_catalog`(`source_catalog` ASC) USING BTREE,
  INDEX `idx_source_column`(`source_column` ASC) USING BTREE,
  INDEX `idx_target_catalog`(`target_catalog` ASC) USING BTREE,
  INDEX `idx_target_column`(`target_column` ASC) USING BTREE,
  INDEX `idx_relation`(`relation` ASC) USING BTREE,
  INDEX `idx_source_model`(`source_model` ASC) USING BTREE,
  INDEX `idx_target_model`(`target_model` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for fs_govern_quality_log
-- ----------------------------
DROP TABLE IF EXISTS `fs_govern_quality_log`;
CREATE TABLE `fs_govern_quality_log`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `source` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `plan_id` int NOT NULL DEFAULT 0,
  `rule_id` int NOT NULL DEFAULT 0,
  `logic_id` int NOT NULL DEFAULT 0,
  `check_table` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `check_column` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `check_count` int NOT NULL DEFAULT 0,
  `hit_count` int NOT NULL DEFAULT 0,
  `reason` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL,
  `expression` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL,
  `state` mediumint NOT NULL DEFAULT 0,
  `created_time` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for fs_govern_quality_logic
-- ----------------------------
DROP TABLE IF EXISTS `fs_govern_quality_logic`;
CREATE TABLE `fs_govern_quality_logic`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `parent_id` int NOT NULL DEFAULT 0,
  `sort` tinyint NOT NULL DEFAULT 0,
  `status` tinyint NOT NULL DEFAULT 0,
  `description` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `created_time` bigint NOT NULL DEFAULT 0,
  `created_uid` int NOT NULL DEFAULT 0,
  `updated_time` bigint NOT NULL DEFAULT 0,
  `updated_uid` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for fs_govern_quality_plan
-- ----------------------------
DROP TABLE IF EXISTS `fs_govern_quality_plan`;
CREATE TABLE `fs_govern_quality_plan`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `source` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `content` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL,
  `sort` tinyint NOT NULL DEFAULT 0,
  `status` tinyint NOT NULL DEFAULT 0,
  `description` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `created_time` bigint NOT NULL DEFAULT 0,
  `created_uid` int NOT NULL DEFAULT 0,
  `updated_time` bigint NOT NULL DEFAULT 0,
  `updated_uid` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for fs_govern_quality_rule
-- ----------------------------
DROP TABLE IF EXISTS `fs_govern_quality_rule`;
CREATE TABLE `fs_govern_quality_rule`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `type` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `name` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `logic_id` int NOT NULL DEFAULT 0,
  `check_table` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `check_column` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `check_metric` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `check_where` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL,
  `check_group` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL,
  `refer_table` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `refer_column` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `refer_metric` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `refer_where` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL,
  `refer_group` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL,
  `content` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL,
  `suggest` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL,
  `sort` tinyint NOT NULL DEFAULT 0,
  `status` tinyint NOT NULL DEFAULT 0,
  `description` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `created_time` bigint NOT NULL DEFAULT 0,
  `created_uid` int NOT NULL DEFAULT 0,
  `updated_time` bigint NOT NULL DEFAULT 0,
  `updated_uid` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

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
  `sort` int NOT NULL DEFAULT 0,
  `status` tinyint NOT NULL DEFAULT 0,
  `description` tinytext CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `created_uid` int NOT NULL DEFAULT 0,
  `created_time` bigint NOT NULL DEFAULT 0,
  `updated_uid` int NOT NULL DEFAULT 0,
  `updated_time` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`code`) USING BTREE,
  INDEX `idx_type`(`type` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

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
  `size` int NOT NULL DEFAULT 0,
  `digit` int NOT NULL DEFAULT 0,
  `nullable` int NOT NULL DEFAULT 0,
  `sort` int NOT NULL DEFAULT 0,
  `status` tinyint NOT NULL DEFAULT 0,
  `description` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `created_uid` int NOT NULL DEFAULT 0,
  `created_time` bigint NOT NULL DEFAULT 0,
  `updated_uid` int NOT NULL DEFAULT 0,
  `updated_time` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`catalog`, `code`) USING BTREE,
  UNIQUE INDEX `uniq_path`(`path` ASC) USING BTREE,
  INDEX `idx_code`(`code` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;
