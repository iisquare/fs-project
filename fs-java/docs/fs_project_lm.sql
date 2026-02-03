-- MySQL dump 10.13  Distrib 8.0.42, for Linux (x86_64)
--
-- Host: wsl    Database: fs_project
-- ------------------------------------------------------
-- Server version	8.0.41

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

--
-- Table structure for table `fs_lm_agent`
--

DROP TABLE IF EXISTS `fs_lm_agent`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fs_lm_agent` (
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
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fs_lm_client`
--

DROP TABLE IF EXISTS `fs_lm_client`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fs_lm_client` (
  `id` int NOT NULL AUTO_INCREMENT,
  `token` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `sort` tinyint NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '0',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `created_time` bigint NOT NULL DEFAULT '0',
  `created_uid` int NOT NULL DEFAULT '0',
  `updated_time` bigint NOT NULL DEFAULT '0',
  `updated_uid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `unq_token` (`token`),
  KEY `idx_name` (`name`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fs_lm_client_endpoint`
--

DROP TABLE IF EXISTS `fs_lm_client_endpoint`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fs_lm_client_endpoint` (
  `id` int NOT NULL AUTO_INCREMENT,
  `client_id` int NOT NULL DEFAULT '0',
  `server_id` int NOT NULL DEFAULT '0',
  `parallel_by_client` int NOT NULL DEFAULT '0',
  `checkable` tinyint NOT NULL DEFAULT '0',
  `sort` tinyint NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '0',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `created_time` bigint NOT NULL DEFAULT '0',
  `created_uid` int NOT NULL DEFAULT '0',
  `updated_time` bigint NOT NULL DEFAULT '0',
  `updated_uid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fs_lm_knowledge`
--

DROP TABLE IF EXISTS `fs_lm_knowledge`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fs_lm_knowledge` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `embedding_id` int NOT NULL DEFAULT '0',
  `reranker_id` int NOT NULL DEFAULT '0',
  `top_k` int NOT NULL DEFAULT '0',
  `score` float NOT NULL DEFAULT '0',
  `split_type` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `split_separator` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `split_segment_tokens` int NOT NULL DEFAULT '0',
  `split_chunk_tokens` int NOT NULL DEFAULT '0',
  `split_overlay_tokens` int NOT NULL DEFAULT '0',
  `sort` tinyint NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '0',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `created_time` bigint NOT NULL DEFAULT '0',
  `created_uid` int NOT NULL DEFAULT '0',
  `updated_time` bigint NOT NULL DEFAULT '0',
  `updated_uid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fs_lm_knowledge_chunk`
--

DROP TABLE IF EXISTS `fs_lm_knowledge_chunk`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fs_lm_knowledge_chunk` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fs_lm_knowledge_document`
--

DROP TABLE IF EXISTS `fs_lm_knowledge_document`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fs_lm_knowledge_document` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `knowledge_id` int NOT NULL DEFAULT '0',
  `filepath` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `sort` tinyint NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '0',
  `created_time` bigint NOT NULL DEFAULT '0',
  `created_uid` int NOT NULL DEFAULT '0',
  `updated_time` bigint NOT NULL DEFAULT '0',
  `updated_uid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_knowledge_id` (`knowledge_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fs_lm_knowledge_segment`
--

DROP TABLE IF EXISTS `fs_lm_knowledge_segment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fs_lm_knowledge_segment` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `knowledge_id` int NOT NULL DEFAULT '0',
  `document_id` int NOT NULL DEFAULT '0',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fs_lm_log`
--

DROP TABLE IF EXISTS `fs_lm_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fs_lm_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `client_id` int NOT NULL DEFAULT '0',
  `client_endpoint_id` int NOT NULL DEFAULT '0',
  `server_id` int NOT NULL DEFAULT '0',
  `server_endpoint_id` int NOT NULL DEFAULT '0',
  `request_body` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `request_ip` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `request_stream` tinyint NOT NULL DEFAULT '0',
  `request_prompt` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `response_body` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `response_completion` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `finish_reason` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `finish_detail` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `usage_prompt_tokens` int NOT NULL DEFAULT '0',
  `usage_completion_tokens` int NOT NULL DEFAULT '0',
  `usage_total_tokens` int NOT NULL DEFAULT '0',
  `audit_reason` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `audit_detail` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `begin_time` bigint NOT NULL DEFAULT '0',
  `request_time` bigint NOT NULL DEFAULT '0',
  `waiting_time` bigint NOT NULL DEFAULT '0',
  `response_time` bigint NOT NULL DEFAULT '0',
  `end_time` bigint NOT NULL DEFAULT '0',
  `audit_time` bigint NOT NULL DEFAULT '0',
  `audit_uid` int NOT NULL DEFAULT '0',
  `deleted_time` bigint NOT NULL DEFAULT '0',
  `deleted_uid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_client_id` (`client_id`) USING BTREE,
  KEY `idx_client_endpoint_id` (`client_endpoint_id`) USING BTREE,
  KEY `idx_server_id` (`server_id`) USING BTREE,
  KEY `idx_server_endpoint_id` (`server_endpoint_id`) USING BTREE,
  KEY `idx_request_ip` (`request_ip`) USING BTREE,
  KEY `idx_request_stream` (`request_stream`) USING BTREE,
  KEY `idx_finish_reason` (`finish_reason`) USING BTREE,
  KEY `idx_usage_prompt_tokens` (`usage_prompt_tokens`) USING BTREE,
  KEY `idx_usage_completion_tokens` (`usage_completion_tokens`) USING BTREE,
  KEY `idx_usage_total_tokens` (`usage_total_tokens`) USING BTREE,
  KEY `idx_audit_reason` (`audit_reason`) USING BTREE,
  KEY `idx_begin_time` (`begin_time`) USING BTREE,
  KEY `idx_request_time` (`request_time`) USING BTREE,
  KEY `idx_waiting_time` (`waiting_time`) USING BTREE,
  KEY `idx_response_time` (`response_time`) USING BTREE,
  KEY `idx_end_time` (`end_time`) USING BTREE,
  KEY `idx_audit_time` (`audit_time`) USING BTREE,
  KEY `idx_audit_uid` (`audit_uid`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=322 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fs_lm_mcp`
--

DROP TABLE IF EXISTS `fs_lm_mcp`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fs_lm_mcp` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
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
-- Table structure for table `fs_lm_model`
--

DROP TABLE IF EXISTS `fs_lm_model`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fs_lm_model` (
  `id` int NOT NULL AUTO_INCREMENT,
  `provider_id` int NOT NULL DEFAULT '0',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
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
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fs_lm_provider`
--

DROP TABLE IF EXISTS `fs_lm_provider`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fs_lm_provider` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `endpoint` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `token` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
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
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fs_lm_server`
--

DROP TABLE IF EXISTS `fs_lm_server`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fs_lm_server` (
  `id` int NOT NULL AUTO_INCREMENT,
  `model` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `sort` tinyint NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '0',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `created_time` bigint NOT NULL DEFAULT '0',
  `created_uid` int NOT NULL DEFAULT '0',
  `updated_time` bigint NOT NULL DEFAULT '0',
  `updated_uid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `unq_model` (`model`) USING BTREE,
  KEY `idx_name` (`name`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fs_lm_server_endpoint`
--

DROP TABLE IF EXISTS `fs_lm_server_endpoint`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fs_lm_server_endpoint` (
  `id` int NOT NULL AUTO_INCREMENT,
  `server_id` int NOT NULL DEFAULT '0',
  `url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `model` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `token` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `parallel_by_server` int NOT NULL DEFAULT '0',
  `sort` tinyint NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '0',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `created_time` bigint NOT NULL DEFAULT '0',
  `created_uid` int NOT NULL DEFAULT '0',
  `updated_time` bigint NOT NULL DEFAULT '0',
  `updated_uid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `fs_lm_tool`
--

DROP TABLE IF EXISTS `fs_lm_tool`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `fs_lm_tool` (
  `name` varbinary(255) NOT NULL DEFAULT '',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `status` tinyint NOT NULL DEFAULT '0',
  `created_time` bigint NOT NULL DEFAULT '0',
  `created_uid` int NOT NULL DEFAULT '0',
  `updated_time` bigint NOT NULL DEFAULT '0',
  `updated_uid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-09-06 11:27:31
