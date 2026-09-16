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
-- Table structure for table `fs_member_application`
--

DROP TABLE IF EXISTS `fs_member_application`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fs_member_application` (
  `id` int NOT NULL AUTO_INCREMENT,
  `serial` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `icon` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `target` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `sort` tinyint NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '0',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `created_time` bigint NOT NULL DEFAULT '0',
  `created_uid` int NOT NULL DEFAULT '0',
  `updated_time` bigint NOT NULL DEFAULT '0',
  `updated_uid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `unq_serial` (`serial`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=309 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fs_member_data`
--

DROP TABLE IF EXISTS `fs_member_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fs_member_data` (
  `id` int NOT NULL AUTO_INCREMENT,
  `serial` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `pks` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `fields` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `sort` tinyint NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '0',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `created_time` bigint NOT NULL DEFAULT '0',
  `created_uid` int NOT NULL DEFAULT '0',
  `updated_time` bigint NOT NULL DEFAULT '0',
  `updated_uid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `unq_serial` (`serial`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fs_member_data_log`
--

DROP TABLE IF EXISTS `fs_member_data_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fs_member_data_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `permits` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `server_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `client_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `request_url` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `request_ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '',
  `request_headers` longtext NOT NULL,
  `request_params` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `request_time` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fs_member_data_permit`
--

DROP TABLE IF EXISTS `fs_member_data_permit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fs_member_data_permit` (
  `id` int NOT NULL AUTO_INCREMENT,
  `data_id` int NOT NULL DEFAULT '0',
  `role_id` int NOT NULL DEFAULT '0',
  `filters` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `fields` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `sort` tinyint NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '0',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `created_time` bigint NOT NULL DEFAULT '0',
  `created_uid` int NOT NULL DEFAULT '0',
  `updated_time` bigint NOT NULL DEFAULT '0',
  `updated_uid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_data_id` (`data_id`) USING BTREE,
  KEY `idx_role_id` (`role_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fs_member_data_permit_log`
--

DROP TABLE IF EXISTS `fs_member_data_permit_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fs_member_data_permit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `log_id` bigint NOT NULL DEFAULT '0',
  `data_id` int NOT NULL DEFAULT '0',
  `data_serial` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `filters` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `fields` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fs_member_dictionary`
--

DROP TABLE IF EXISTS `fs_member_dictionary`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fs_member_dictionary` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `pinyin` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `ancestor_id` int NOT NULL DEFAULT '0',
  `parent_id` int NOT NULL DEFAULT '0',
  `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `leaf` tinyint NOT NULL DEFAULT '0',
  `sort` tinyint NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '0',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `created_time` bigint NOT NULL DEFAULT '0',
  `created_uid` int NOT NULL DEFAULT '0',
  `updated_time` bigint NOT NULL DEFAULT '0',
  `updated_uid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_content` (`content`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE,
  KEY `idx_ancestor_id` (`ancestor_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=10064 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fs_member_favorite`
--

DROP TABLE IF EXISTS `fs_member_favorite`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fs_member_favorite` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '',
  `type` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '',
  `content` longtext CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `sort` int NOT NULL DEFAULT '0',
  `sharable` tinyint NOT NULL DEFAULT '0',
  `description` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `created_uid` int NOT NULL DEFAULT '0',
  `created_time` bigint NOT NULL DEFAULT '0',
  `updated_uid` int NOT NULL DEFAULT '0',
  `updated_time` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb3;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fs_member_menu`
--

DROP TABLE IF EXISTS `fs_member_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fs_member_menu` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `full_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `application_id` int NOT NULL DEFAULT '0',
  `parent_id` int NOT NULL DEFAULT '0',
  `icon` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `target` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `sort` tinyint NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '0',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `created_time` bigint NOT NULL DEFAULT '0',
  `created_uid` int NOT NULL DEFAULT '0',
  `updated_time` bigint NOT NULL DEFAULT '0',
  `updated_uid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_application_id` (`application_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=414 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fs_member_message`
--

DROP TABLE IF EXISTS `fs_member_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fs_member_message` (
  `id` int NOT NULL AUTO_INCREMENT,
  `type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `recipient` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `subject` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `request_body` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `response_body` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `status` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `created_time` bigint NOT NULL DEFAULT '0',
  `deleted_time` bigint NOT NULL DEFAULT '0',
  `deleted_uid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_type` (`type`) USING BTREE,
  KEY `idx_recipient` (`recipient`) USING BTREE,
  KEY `idx_subject` (`subject`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fs_member_resource`
--

DROP TABLE IF EXISTS `fs_member_resource`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fs_member_resource` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `full_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `application_id` int NOT NULL DEFAULT '0',
  `parent_id` int NOT NULL DEFAULT '0',
  `module` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `controller` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `action` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `sort` tinyint NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '0',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `created_time` bigint NOT NULL DEFAULT '0',
  `created_uid` int NOT NULL DEFAULT '0',
  `updated_time` bigint NOT NULL DEFAULT '0',
  `updated_uid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_application_id` (`application_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=424 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fs_member_role`
--

DROP TABLE IF EXISTS `fs_member_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fs_member_role` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `sort` tinyint NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '0',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `created_time` bigint NOT NULL DEFAULT '0',
  `created_uid` int NOT NULL DEFAULT '0',
  `updated_time` bigint NOT NULL DEFAULT '0',
  `updated_uid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fs_member_role_application`
--

DROP TABLE IF EXISTS `fs_member_role_application`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fs_member_role_application` (
  `role_id` int NOT NULL DEFAULT '0',
  `application_id` int NOT NULL DEFAULT '0',
  `created_time` bigint NOT NULL DEFAULT '0',
  `created_uid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`role_id`,`application_id`) USING BTREE,
  KEY `idx_application_id` (`application_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fs_member_role_menu`
--

DROP TABLE IF EXISTS `fs_member_role_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fs_member_role_menu` (
  `role_id` int NOT NULL DEFAULT '0',
  `menu_id` int NOT NULL DEFAULT '0',
  `created_time` bigint NOT NULL DEFAULT '0',
  `created_uid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`role_id`,`menu_id`) USING BTREE,
  KEY `idx_menu_id` (`menu_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fs_member_role_resource`
--

DROP TABLE IF EXISTS `fs_member_role_resource`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fs_member_role_resource` (
  `role_id` int NOT NULL DEFAULT '0',
  `resource_id` int NOT NULL DEFAULT '0',
  `created_time` bigint NOT NULL DEFAULT '0',
  `created_uid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`role_id`,`resource_id`) USING BTREE,
  KEY `idx_resource_id` (`resource_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fs_member_setting`
--

DROP TABLE IF EXISTS `fs_member_setting`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fs_member_setting` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `sort` tinyint NOT NULL DEFAULT '0',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `updated_time` bigint NOT NULL DEFAULT '0',
  `updated_uid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uniq_type_name` (`type`,`name`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=42 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fs_member_user`
--

DROP TABLE IF EXISTS `fs_member_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fs_member_user` (
  `id` int NOT NULL AUTO_INCREMENT,
  `serial` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `password` char(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `phone` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `salt` char(4) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `sort` tinyint NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '0',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `created_time` bigint NOT NULL DEFAULT '0',
  `created_ip` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `created_uid` int NOT NULL DEFAULT '0',
  `updated_time` bigint NOT NULL DEFAULT '0',
  `updated_uid` int NOT NULL DEFAULT '0',
  `login_time` bigint NOT NULL DEFAULT '0',
  `login_ip` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `locked_time` bigint NOT NULL DEFAULT '0',
  `deleted_time` bigint NOT NULL DEFAULT '0',
  `deleted_uid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `unq_name` (`name`) USING BTREE,
  UNIQUE KEY `unq_serial` (`serial`),
  KEY `idx_email` (`email`) USING BTREE,
  KEY `idx_phone` (`phone`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=35 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fs_member_user_role`
--

DROP TABLE IF EXISTS `fs_member_user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fs_member_user_role` (
  `user_id` int NOT NULL DEFAULT '0',
  `role_id` int NOT NULL DEFAULT '0',
  `created_time` bigint NOT NULL DEFAULT '0',
  `created_uid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`user_id`,`role_id`) USING BTREE,
  KEY `idx_role_id` (`role_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;
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

-- Dump completed on 2026-09-16 10:52:34
