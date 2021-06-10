/*
Navicat MySQL Data Transfer

Source Server         : yoyo_3306
Source Server Version : 50716
Source Host           : 192.168.0.146:13306
Source Database       : fs-project

Target Server Type    : MYSQL
Target Server Version : 50716
File Encoding         : 65001

Date: 2021-06-08 11:20:03
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for fs_oa_form_data
-- ----------------------------
DROP TABLE IF EXISTS `fs_oa_form_data`;
CREATE TABLE `fs_oa_form_data` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `frame_id` int(11) NOT NULL DEFAULT '0',
  `content` text NOT NULL,
  `bpm_instance` varchar(255) NOT NULL DEFAULT '',
  `bpm_status` varchar(255) NOT NULL DEFAULT '',
  `bpm_task` varchar(255) NOT NULL DEFAULT '',
  `bpm_identity` varchar(255) NOT NULL DEFAULT '',
  `created_time` bigint(20) NOT NULL DEFAULT '0',
  `created_uid` int(11) NOT NULL DEFAULT '0',
  `updated_time` bigint(20) NOT NULL DEFAULT '0',
  `updated_uid` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_frame_id` (`frame_id`) USING BTREE,
  KEY `idx_bpm_instance` (`bpm_instance`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for fs_oa_form_frame
-- ----------------------------
DROP TABLE IF EXISTS `fs_oa_form_frame`;
CREATE TABLE `fs_oa_form_frame` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL DEFAULT '',
  `content` text NOT NULL,
  `physical_table` varchar(255) NOT NULL DEFAULT '',
  `bpm_id` varchar(255) NOT NULL DEFAULT '',
  `sort` tinyint(4) NOT NULL DEFAULT '0',
  `status` tinyint(4) NOT NULL DEFAULT '0',
  `description` text NOT NULL,
  `created_time` bigint(20) NOT NULL DEFAULT '0',
  `created_uid` int(11) NOT NULL DEFAULT '0',
  `updated_time` bigint(20) NOT NULL DEFAULT '0',
  `updated_uid` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_name` (`name`) USING BTREE,
  KEY `idx_physical_table` (`physical_table`) USING BTREE,
  KEY `idx_bpm_id` (`bpm_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for fs_oa_form_index
-- ----------------------------
DROP TABLE IF EXISTS `fs_oa_form_index`;
CREATE TABLE `fs_oa_form_index` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `frame_id` int(11) NOT NULL DEFAULT '0',
  `data_id` int(11) NOT NULL DEFAULT '0',
  `path` varchar(255) NOT NULL DEFAULT '',
  `content` varchar(20000) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `idx_frame_id` (`frame_id`) USING BTREE,
  KEY `idx_data_id` (`data_id`) USING BTREE,
  KEY `idx_path` (`path`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for fs_oa_form_regular
-- ----------------------------
DROP TABLE IF EXISTS `fs_oa_form_regular`;
CREATE TABLE `fs_oa_form_regular` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL DEFAULT '',
  `label` varchar(255) NOT NULL DEFAULT '',
  `regex` varchar(255) NOT NULL DEFAULT '',
  `tooltip` varchar(255) NOT NULL DEFAULT '',
  `sort` tinyint(4) NOT NULL DEFAULT '0',
  `status` tinyint(4) NOT NULL DEFAULT '0',
  `description` text NOT NULL,
  `created_time` bigint(20) NOT NULL DEFAULT '0',
  `created_uid` int(11) NOT NULL DEFAULT '0',
  `updated_time` bigint(20) NOT NULL DEFAULT '0',
  `updated_uid` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_name` (`name`) USING BTREE,
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for fs_oa_workflow
-- ----------------------------
DROP TABLE IF EXISTS `fs_oa_workflow`;
CREATE TABLE `fs_oa_workflow` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL DEFAULT '',
  `content` text NOT NULL,
  `form_id` int(11) NOT NULL DEFAULT '0',
  `sort` tinyint(4) NOT NULL DEFAULT '0',
  `status` tinyint(4) NOT NULL DEFAULT '0',
  `description` text NOT NULL,
  `created_time` bigint(20) NOT NULL DEFAULT '0',
  `created_uid` int(11) NOT NULL DEFAULT '0',
  `updated_time` bigint(20) NOT NULL DEFAULT '0',
  `updated_uid` int(11) NOT NULL DEFAULT '0',
  `deployment_id` varchar(255) NOT NULL DEFAULT '',
  `deployment_uid` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_name` (`name`) USING BTREE,
  KEY `idx_form_id` (`form_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8;
