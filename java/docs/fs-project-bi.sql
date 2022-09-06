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

 Date: 06/09/2022 10:13:46
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for fs_bi_dataset
-- ----------------------------
DROP TABLE IF EXISTS `fs_bi_dataset`;
CREATE TABLE `fs_bi_dataset`  (
  `id` int(0) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '名称',
  `collection` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '数据集合名',
  `content` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '数据源配置',
  `sort` int(0) NOT NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint(0) NOT NULL DEFAULT 0 COMMENT '状态',
  `description` tinytext CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '描述',
  `created_uid` int(0) NOT NULL DEFAULT 0 COMMENT '创建者',
  `created_time` bigint(0) NOT NULL DEFAULT 0 COMMENT '创建时间',
  `updated_uid` int(0) NOT NULL DEFAULT 0 COMMENT '修改者',
  `updated_time` bigint(0) NOT NULL DEFAULT 0 COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for fs_bi_diagram
-- ----------------------------
DROP TABLE IF EXISTS `fs_bi_diagram`;
CREATE TABLE `fs_bi_diagram`  (
  `id` int(0) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '名称',
  `engine` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '计算引擎',
  `model` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '处理模式',
  `content` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '计算规则有向无环图',
  `sort` int(0) NOT NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint(0) NOT NULL DEFAULT 0 COMMENT '状态',
  `description` tinytext CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '描述',
  `created_uid` int(0) NOT NULL DEFAULT 0 COMMENT '创建者',
  `created_time` bigint(0) NOT NULL DEFAULT 0 COMMENT '创建时间',
  `updated_uid` int(0) NOT NULL DEFAULT 0 COMMENT '修改者',
  `updated_time` bigint(0) NOT NULL DEFAULT 0 COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_engine`(`engine`) USING BTREE,
  INDEX `idx_model`(`model`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for fs_bi_matrix
-- ----------------------------
DROP TABLE IF EXISTS `fs_bi_matrix`;
CREATE TABLE `fs_bi_matrix`  (
  `id` int(0) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '名称',
  `dataset_id` int(0) NOT NULL DEFAULT 0 COMMENT '引用数据集',
  `content` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '矩阵配置项',
  `sort` int(0) NOT NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint(0) NOT NULL DEFAULT 0 COMMENT '状态',
  `description` tinytext CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '描述',
  `created_uid` int(0) NOT NULL DEFAULT 0 COMMENT '创建者',
  `created_time` bigint(0) NOT NULL DEFAULT 0 COMMENT '创建时间',
  `updated_uid` int(0) NOT NULL DEFAULT 0 COMMENT '修改者',
  `updated_time` bigint(0) NOT NULL DEFAULT 0 COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_dataset_id`(`dataset_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for fs_bi_source
-- ----------------------------
DROP TABLE IF EXISTS `fs_bi_source`;
CREATE TABLE `fs_bi_source`  (
  `id` int(0) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '名称',
  `type` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '数据源类型',
  `content` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '数据源配置',
  `sort` int(0) NOT NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint(0) NOT NULL DEFAULT 0 COMMENT '状态',
  `description` tinytext CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '描述',
  `created_uid` int(0) NOT NULL DEFAULT 0 COMMENT '创建者',
  `created_time` bigint(0) NOT NULL DEFAULT 0 COMMENT '创建时间',
  `updated_uid` int(0) NOT NULL DEFAULT 0 COMMENT '修改者',
  `updated_time` bigint(0) NOT NULL DEFAULT 0 COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_type`(`type`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for fs_bi_visualize
-- ----------------------------
DROP TABLE IF EXISTS `fs_bi_visualize`;
CREATE TABLE `fs_bi_visualize`  (
  `id` int(0) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '名称',
  `type` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '类型',
  `dataset_id` int(0) NOT NULL DEFAULT 0 COMMENT '引用数据集',
  `content` text CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '报表配置项',
  `sort` int(0) NOT NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint(0) NOT NULL DEFAULT 0 COMMENT '状态',
  `description` tinytext CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '描述',
  `created_uid` int(0) NOT NULL DEFAULT 0 COMMENT '创建者',
  `created_time` bigint(0) NOT NULL DEFAULT 0 COMMENT '创建时间',
  `updated_uid` int(0) NOT NULL DEFAULT 0 COMMENT '修改者',
  `updated_time` bigint(0) NOT NULL DEFAULT 0 COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_dataset_id`(`dataset_id`) USING BTREE,
  INDEX `idx_type`(`type`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
