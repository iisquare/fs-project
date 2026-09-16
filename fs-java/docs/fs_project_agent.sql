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

SET @@GLOBAL.GTID_PURGED=/*!80000 '+'*/ '62aa6980-af0e-11f0-bf53-c2f7353079da:1-94877';

--
-- Table structure for table `fs_agent_agent`
--

DROP TABLE IF EXISTS `fs_agent_agent`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fs_agent_agent` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `model` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `token` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `system_prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `max_tokens` int NOT NULL DEFAULT '0',
  `temperature` float NOT NULL,
  `parameter` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `role_ids` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `sort` tinyint NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '0',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `created_time` bigint NOT NULL DEFAULT '0',
  `created_uid` int NOT NULL DEFAULT '0',
  `updated_time` bigint NOT NULL DEFAULT '0',
  `updated_uid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_name` (`name`) USING BTREE,
  KEY `idx_model` (`model`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fs_agent_agentic`
--

DROP TABLE IF EXISTS `fs_agent_agentic`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fs_agent_agentic` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `mode` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'workflow',
  `icon` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `tags` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `published_content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `published_version` int NOT NULL DEFAULT '0',
  `published_time` bigint NOT NULL DEFAULT '0',
  `published_uid` int NOT NULL DEFAULT '0',
  `sort` tinyint NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '0',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `created_time` bigint NOT NULL DEFAULT '0',
  `created_uid` int NOT NULL DEFAULT '0',
  `updated_time` bigint NOT NULL DEFAULT '0',
  `updated_uid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_name` (`name`) USING BTREE,
  KEY `idx_mode` (`mode`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fs_agent_knowledge`
--

DROP TABLE IF EXISTS `fs_agent_knowledge`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fs_agent_knowledge` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `embedding_model` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `rerank_model` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `top_k` int NOT NULL DEFAULT '0',
  `score` double NOT NULL DEFAULT '0',
  `recall_type` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `reranked` tinyint NOT NULL DEFAULT '0',
  `recall_scope` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `split_separator` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `split_segment_tokens` int NOT NULL DEFAULT '0',
  `split_chunk_tokens` int NOT NULL DEFAULT '0',
  `split_overlay_tokens` int NOT NULL DEFAULT '0',
  `labels` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `role_ids` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `sort` tinyint NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '0',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `created_time` bigint NOT NULL DEFAULT '0',
  `created_uid` int NOT NULL DEFAULT '0',
  `updated_time` bigint NOT NULL DEFAULT '0',
  `updated_uid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fs_agent_knowledge_chunk`
--

DROP TABLE IF EXISTS `fs_agent_knowledge_chunk`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fs_agent_knowledge_chunk` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `knowledge_id` int NOT NULL DEFAULT '0',
  `document_id` int NOT NULL DEFAULT '0',
  `segment_id` int NOT NULL DEFAULT '0',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `embedding` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `sort` tinyint NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '0',
  `created_time` bigint NOT NULL DEFAULT '0',
  `created_uid` int NOT NULL DEFAULT '0',
  `updated_time` bigint NOT NULL DEFAULT '0',
  `updated_uid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_document_id` (`document_id`) USING BTREE,
  KEY `idx_knowledge_id` (`knowledge_id`) USING BTREE,
  KEY `idx_segment_id` (`segment_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=879 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fs_agent_knowledge_document`
--

DROP TABLE IF EXISTS `fs_agent_knowledge_document`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fs_agent_knowledge_document` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `knowledge_id` int NOT NULL DEFAULT '0',
  `file_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `filepath` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `token_size` int NOT NULL DEFAULT '0',
  `metadata` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `sort` tinyint NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '0',
  `created_time` bigint NOT NULL DEFAULT '0',
  `created_uid` int NOT NULL DEFAULT '0',
  `updated_time` bigint NOT NULL DEFAULT '0',
  `updated_uid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_knowledge_id` (`knowledge_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=38 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fs_agent_knowledge_segment`
--

DROP TABLE IF EXISTS `fs_agent_knowledge_segment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fs_agent_knowledge_segment` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `knowledge_id` int NOT NULL DEFAULT '0',
  `document_id` int NOT NULL DEFAULT '0',
  `token_size` int NOT NULL DEFAULT '0',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `sort` tinyint NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '0',
  `created_time` bigint NOT NULL DEFAULT '0',
  `created_uid` int NOT NULL DEFAULT '0',
  `updated_time` bigint NOT NULL DEFAULT '0',
  `updated_uid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_knowledge_id` (`knowledge_id`) USING BTREE,
  KEY `idx_document_id` (`document_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=39 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fs_agent_skill`
--

DROP TABLE IF EXISTS `fs_agent_skill`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fs_agent_skill` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `type` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `labels` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `role_ids` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `sort` tinyint NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '0',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `created_time` bigint NOT NULL DEFAULT '0',
  `created_uid` int NOT NULL DEFAULT '0',
  `updated_time` bigint NOT NULL DEFAULT '0',
  `updated_uid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fs_agent_skill_version`
--

DROP TABLE IF EXISTS `fs_agent_skill_version`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fs_agent_skill_version` (
  `id` int NOT NULL AUTO_INCREMENT,
  `skill_id` int NOT NULL DEFAULT '0',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `file_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `filepath` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `file_size` bigint NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '0',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `created_time` bigint NOT NULL DEFAULT '0',
  `created_uid` int NOT NULL DEFAULT '0',
  `updated_time` bigint NOT NULL DEFAULT '0',
  `updated_uid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_skill_id` (`skill_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fs_agent_tool`
--

DROP TABLE IF EXISTS `fs_agent_tool`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fs_agent_tool` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `type` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `url` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `header` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `query` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `labels` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `role_ids` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `sort` tinyint NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '0',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `created_time` bigint NOT NULL DEFAULT '0',
  `created_uid` int NOT NULL DEFAULT '0',
  `updated_time` bigint NOT NULL DEFAULT '0',
  `updated_uid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fs_agent_knowledge_image` (
  `id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `knowledge_id` int NOT NULL DEFAULT '0',
  `document_id` int NOT NULL DEFAULT '0',
  `segment_id` int NOT NULL DEFAULT '0',
  `bucket` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `filepath` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `suffix` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `type` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `size` bigint NOT NULL DEFAULT '0',
  `alt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `page` int NOT NULL DEFAULT '0',
  `sort` tinyint NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '0',
  `created_time` bigint NOT NULL DEFAULT '0',
  `created_uid` int NOT NULL DEFAULT '0',
  `updated_time` bigint NOT NULL DEFAULT '0',
  `updated_uid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_knowledge_document` (`knowledge_id`,`document_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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

-- Dump completed on 2026-09-14 10:33:11
