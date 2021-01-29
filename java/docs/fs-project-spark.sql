/*
Navicat MySQL Data Transfer

Source Server         : localhost_3306
Source Server Version : 50716
Source Host           : localhost:3306
Source Database       : fs-project

Target Server Type    : MYSQL
Target Server Version : 50716
File Encoding         : 65001

Date: 2020-12-29 11:23:41
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for fs_spark_flow
-- ----------------------------
DROP TABLE IF EXISTS `fs_spark_flow`;
CREATE TABLE `fs_spark_flow` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(255) NOT NULL DEFAULT '' COMMENT '流程图',
  `type` varchar(32) NOT NULL DEFAULT '',
  `content` text NOT NULL COMMENT '流程图内容',
  `sort` int(11) NOT NULL DEFAULT '0' COMMENT '排序',
  `status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '状态',
  `description` tinytext NOT NULL COMMENT '描述',
  `created_uid` int(11) NOT NULL DEFAULT '0' COMMENT '创建者',
  `created_time` bigint(20) NOT NULL DEFAULT '0' COMMENT '创建时间',
  `updated_uid` int(11) NOT NULL DEFAULT '0' COMMENT '修改者',
  `updated_time` bigint(20) NOT NULL DEFAULT '0' COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='流程图信息';

-- ----------------------------
-- Table structure for fs_spark_node
-- ----------------------------
DROP TABLE IF EXISTS `fs_spark_node`;
CREATE TABLE `fs_spark_node` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(32) NOT NULL DEFAULT '',
  `full_name` varchar(255) NOT NULL DEFAULT '',
  `parent_id` int(11) NOT NULL DEFAULT '0',
  `type` varchar(32) NOT NULL DEFAULT '',
  `plugin` varchar(64) NOT NULL DEFAULT '',
  `icon` varchar(64) NOT NULL DEFAULT '',
  `state` varchar(8) NOT NULL DEFAULT '',
  `classname` varchar(255) NOT NULL DEFAULT '',
  `draggable` tinyint(4) NOT NULL DEFAULT '0',
  `properties` text NOT NULL,
  `returns` text NOT NULL,
  `sort` tinyint(4) NOT NULL DEFAULT '0',
  `status` tinyint(4) NOT NULL DEFAULT '0',
  `description` text NOT NULL,
  `created_time` bigint(20) NOT NULL DEFAULT '0',
  `created_uid` int(11) NOT NULL DEFAULT '0',
  `updated_time` bigint(20) NOT NULL DEFAULT '0',
  `updated_uid` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for fs_spark_plugin
-- ----------------------------
DROP TABLE IF EXISTS `fs_spark_plugin`;
CREATE TABLE `fs_spark_plugin` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(32) NOT NULL DEFAULT '',
  `version` varchar(32) NOT NULL DEFAULT '',
  `config` text NOT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '0',
  `description` text NOT NULL,
  `created_time` bigint(20) NOT NULL DEFAULT '0',
  `created_uid` int(11) NOT NULL DEFAULT '0',
  `updated_time` bigint(20) NOT NULL DEFAULT '0',
  `updated_uid` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
