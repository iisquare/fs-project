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

 Date: 04/03/2022 14:36:37
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for fs_oa_form_frame
-- ----------------------------
DROP TABLE IF EXISTS `fs_oa_form_frame`;
CREATE TABLE `fs_oa_form_frame`  (
  `id` int(0) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `storage` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `content` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `sort` tinyint(0) NOT NULL DEFAULT 0,
  `status` tinyint(0) NOT NULL DEFAULT 0,
  `description` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `created_time` bigint(0) NOT NULL DEFAULT 0,
  `created_uid` int(0) NOT NULL DEFAULT 0,
  `updated_time` bigint(0) NOT NULL DEFAULT 0,
  `updated_uid` int(0) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_name`(`name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for fs_oa_form_regular
-- ----------------------------
DROP TABLE IF EXISTS `fs_oa_form_regular`;
CREATE TABLE `fs_oa_form_regular`  (
  `id` int(0) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `label` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `regex` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `tooltip` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `sort` tinyint(0) NOT NULL DEFAULT 0,
  `status` tinyint(0) NOT NULL DEFAULT 0,
  `description` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `created_time` bigint(0) NOT NULL DEFAULT 0,
  `created_uid` int(0) NOT NULL DEFAULT 0,
  `updated_time` bigint(0) NOT NULL DEFAULT 0,
  `updated_uid` int(0) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_name`(`name`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for fs_oa_print
-- ----------------------------
DROP TABLE IF EXISTS `fs_oa_print`;
CREATE TABLE `fs_oa_print`  (
  `id` int(0) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `json` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `html` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `state` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `content` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `sort` tinyint(0) NOT NULL DEFAULT 0,
  `status` tinyint(0) NOT NULL DEFAULT 0,
  `description` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `created_time` bigint(0) NOT NULL DEFAULT 0,
  `created_uid` int(0) NOT NULL DEFAULT 0,
  `updated_time` bigint(0) NOT NULL DEFAULT 0,
  `updated_uid` int(0) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_name`(`name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for fs_oa_workflow
-- ----------------------------
DROP TABLE IF EXISTS `fs_oa_workflow`;
CREATE TABLE `fs_oa_workflow`  (
  `id` int(0) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `content` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `form_id` int(0) NOT NULL DEFAULT 0,
  `sort` tinyint(0) NOT NULL DEFAULT 0,
  `status` tinyint(0) NOT NULL DEFAULT 0,
  `description` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `created_time` bigint(0) NOT NULL DEFAULT 0,
  `created_uid` int(0) NOT NULL DEFAULT 0,
  `updated_time` bigint(0) NOT NULL DEFAULT 0,
  `updated_uid` int(0) NOT NULL DEFAULT 0,
  `deployment_id` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `deployment_uid` int(0) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_name`(`name`) USING BTREE,
  INDEX `idx_form_id`(`form_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
