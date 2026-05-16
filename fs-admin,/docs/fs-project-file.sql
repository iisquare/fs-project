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

 Date: 29/07/2021 16:45:27
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for fs_file_archive
-- ----------------------------
DROP TABLE IF EXISTS `fs_file_archive`;
CREATE TABLE `fs_file_archive`  (
  `id` char(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `name` varchar(1000) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `bucket` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `dir` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `suffix` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `type` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `size` bigint NOT NULL DEFAULT 0,
  `digest` varchar(1024) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `hash` char(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '',
  `status` tinyint NOT NULL DEFAULT 0,
  `created_time` bigint NOT NULL DEFAULT 0,
  `created_uid` int NOT NULL DEFAULT 0,
  `updated_time` bigint NOT NULL DEFAULT 0,
  `updated_uid` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_name`(`name`) USING BTREE,
  INDEX `idx_form_id`(`bucket`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
