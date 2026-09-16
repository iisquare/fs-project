-- MySQL dump 10.13  Distrib 8.0.42, for Linux (x86_64)
--
-- Host: wsl    Database: fs_project
-- ------------------------------------------------------
-- Server version	9.5.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
SET @MYSQLDUMP_TEMP_LOG_BIN = @@SESSION.SQL_LOG_BIN;
SET @@SESSION.SQL_LOG_BIN= 0;

--
-- GTID state at the beginning of the backup 
--

SET @@GLOBAL.GTID_PURGED=/*!80000 '+'*/ '62aa6980-af0e-11f0-bf53-c2f7353079da:1-94994';

--
-- Table structure for table `fs_lm_auth`
--

DROP TABLE IF EXISTS `fs_lm_auth`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fs_lm_auth` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `uid` int NOT NULL DEFAULT '0',
  `secret` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `model_ids` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `status` varchar(16) NOT NULL DEFAULT '',
  `expired_time` bigint NOT NULL DEFAULT '0',
  `created_time` bigint NOT NULL DEFAULT '0',
  `created_uid` int NOT NULL DEFAULT '0',
  `updated_time` bigint NOT NULL DEFAULT '0',
  `updated_uid` int NOT NULL DEFAULT '0',
  `deleted_time` bigint NOT NULL DEFAULT '0',
  `deleted_uid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_secret` (`secret`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fs_lm_credit`
--

DROP TABLE IF EXISTS `fs_lm_credit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fs_lm_credit` (
  `uid` int NOT NULL,
  `remained` decimal(20,12) NOT NULL DEFAULT '0.000000000000',
  `consumed` decimal(20,12) NOT NULL DEFAULT '0.000000000000',
  `rate_ids` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `remind_enabled` tinyint NOT NULL DEFAULT '0',
  `remind_threshold` double NOT NULL DEFAULT '0',
  `sort` tinyint NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '0',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `published_time` bigint NOT NULL DEFAULT '0',
  `published_uid` int NOT NULL DEFAULT '0',
  `created_time` bigint NOT NULL DEFAULT '0',
  `created_uid` int NOT NULL DEFAULT '0',
  `updated_time` bigint NOT NULL DEFAULT '0',
  `updated_uid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fs_lm_model`
--

DROP TABLE IF EXISTS `fs_lm_model`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fs_lm_model` (
  `id` int NOT NULL AUTO_INCREMENT,
  `provider_id` int NOT NULL DEFAULT '0',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `alias` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `role_ids` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `explorable` tinyint NOT NULL DEFAULT '0',
  `all_visible` tinyint NOT NULL DEFAULT '0',
  `security_detectable` tinyint NOT NULL DEFAULT '0',
  `plan` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `sort` tinyint NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '0',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `created_time` bigint NOT NULL DEFAULT '0',
  `created_uid` int NOT NULL DEFAULT '0',
  `updated_time` bigint NOT NULL DEFAULT '0',
  `updated_uid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_provider_id` (`provider_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fs_lm_provider`
--

DROP TABLE IF EXISTS `fs_lm_provider`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fs_lm_provider` (
  `id` int NOT NULL AUTO_INCREMENT,
  `type` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `serial` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `endpoint` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `token` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `website` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `sort` tinyint NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '0',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `created_time` bigint NOT NULL DEFAULT '0',
  `created_uid` int NOT NULL DEFAULT '0',
  `updated_time` bigint NOT NULL DEFAULT '0',
  `updated_uid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `unq_serial` (`serial`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fs_lm_rate`
--

DROP TABLE IF EXISTS `fs_lm_rate`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fs_lm_rate` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `request_count` double NOT NULL DEFAULT '0',
  `request_interval` int NOT NULL DEFAULT '0',
  `token_count` double NOT NULL DEFAULT '0',
  `token_interval` int NOT NULL DEFAULT '0',
  `credit_count` double NOT NULL DEFAULT '0',
  `credit_interval` int NOT NULL DEFAULT '0',
  `sort` tinyint NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '0',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `published_time` bigint NOT NULL DEFAULT '0',
  `published_uid` int NOT NULL DEFAULT '0',
  `created_time` bigint NOT NULL DEFAULT '0',
  `created_uid` int NOT NULL DEFAULT '0',
  `updated_time` bigint NOT NULL DEFAULT '0',
  `updated_uid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fs_lm_sensitive`
--

DROP TABLE IF EXISTS `fs_lm_sensitive`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fs_lm_sensitive` (
  `id` int NOT NULL AUTO_INCREMENT,
  `content` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `risk` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `sort` tinyint NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '0',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `created_time` bigint NOT NULL DEFAULT '0',
  `created_uid` int NOT NULL DEFAULT '0',
  `updated_time` bigint NOT NULL DEFAULT '0',
  `updated_uid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_content` (`content`) USING BTREE,
  KEY `idx_risk` (`risk`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fs_lm_usage`
--

DROP TABLE IF EXISTS `fs_lm_usage`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fs_lm_usage` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `uid` int NOT NULL DEFAULT '0',
  `type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `place` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `credit_amount` decimal(20,12) NOT NULL DEFAULT '0.000000000000',
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '',
  `auth_id` int NOT NULL DEFAULT '0',
  `model_id` int NOT NULL DEFAULT '0',
  `provider_id` int NOT NULL DEFAULT '0',
  `request_ip` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `begin_time` bigint NOT NULL DEFAULT '0',
  `end_time` bigint NOT NULL DEFAULT '0',
  `coast_total` int NOT NULL DEFAULT '0',
  `request_header` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `request_body` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `request_stream` tinyint NOT NULL DEFAULT '0',
  `request_prompt` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `request_system` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `request_user` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `response_header` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `response_body` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `response_reason` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `response_completion` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `response_tool` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `finish_reason` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `finish_detail` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `usage_prompt_cached_tokens` int NOT NULL DEFAULT '0',
  `usage_prompt_tokens` int NOT NULL DEFAULT '0',
  `usage_completion_tokens` int NOT NULL DEFAULT '0',
  `usage_total_tokens` int NOT NULL DEFAULT '0',
  `audit_reason` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `audit_detail` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `audit_time` bigint NOT NULL DEFAULT '0',
  `audit_uid` int NOT NULL DEFAULT '0',
  `deleted_time` bigint NOT NULL DEFAULT '0',
  `deleted_uid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_uid` (`uid`) USING BTREE,
  KEY `idx_type` (`type`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_auth_id` (`auth_id`) USING BTREE,
  KEY `idx_model_id` (`model_id`) USING BTREE,
  KEY `idx_provider_id` (`provider_id`) USING BTREE,
  KEY `idx_request_ip` (`request_ip`) USING BTREE,
  KEY `idx_finish_reason` (`finish_reason`) USING BTREE,
  KEY `idx_deleted_time_begin_time` (`deleted_time`,`begin_time`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=433 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
SET @@SESSION.SQL_LOG_BIN = @MYSQLDUMP_TEMP_LOG_BIN;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-09-16 10:52:37
