/*
 Navicat Premium Data Transfer

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 50716
 Source Host           : localhost:3306
 Source Schema         : fs-project

 Target Server Type    : MySQL
 Target Server Version : 50716
 File Encoding         : 65001

 Date: 07/12/2020 15:55:55
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for fs_lucene_dictionary
-- ----------------------------
DROP TABLE IF EXISTS `fs_lucene_dictionary`;
CREATE TABLE `fs_lucene_dictionary`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `catalogue` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `content` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `source` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `created_time` bigint(20) NOT NULL DEFAULT 0,
  `created_uid` int(11) NOT NULL DEFAULT 0,
  `updated_time` bigint(20) NOT NULL DEFAULT 0,
  `updated_uid` int(11) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_catalogue`(`catalogue`) USING BTREE,
  INDEX `idx_type`(`type`) USING BTREE,
  INDEX `idx_source`(`source`) USING BTREE,
  INDEX `idx_created_time`(`created_time`) USING BTREE,
  INDEX `idx_updated_time`(`updated_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
