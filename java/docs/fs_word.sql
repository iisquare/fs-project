/*
 Navicat Premium Data Transfer

 Source Server         : local-mysql
 Source Server Type    : MySQL
 Source Server Version : 80021
 Source Host           : localhost:3306
 Source Schema         : fs_test

 Target Server Type    : MySQL
 Target Server Version : 80021
 File Encoding         : 65001

 Date: 13/09/2023 10:28:39
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for fs_word_acc
-- ----------------------------
DROP TABLE IF EXISTS `fs_word_acc`;
CREATE TABLE `fs_word_acc`  (
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `score` double(10, 6) NOT NULL DEFAULT 0.000000,
  `word_count` bigint NOT NULL DEFAULT 0,
  `under_left_count` bigint NOT NULL DEFAULT 0,
  `over_left_count` bigint NOT NULL DEFAULT 0,
  `under_right_count` bigint NOT NULL DEFAULT 0,
  `over_right_count` bigint NOT NULL DEFAULT 0,
  `total_count` bigint NOT NULL DEFAULT 0,
  INDEX `idx_name`(`name` ASC) USING BTREE,
  INDEX `idx_score`(`score` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for fs_word_gram
-- ----------------------------
DROP TABLE IF EXISTS `fs_word_gram`;
CREATE TABLE `fs_word_gram`  (
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `score` double(10, 6) NOT NULL DEFAULT 0.000000,
  `title` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '',
  `level` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  INDEX `idx_name`(`name` ASC) USING BTREE,
  INDEX `idx_score`(`score` ASC) USING BTREE,
  INDEX `idx_title`(`title` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for fs_word_level
-- ----------------------------
DROP TABLE IF EXISTS `fs_word_level`;
CREATE TABLE `fs_word_level`  (
  `base_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `level` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL,
  PRIMARY KEY (`base_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
