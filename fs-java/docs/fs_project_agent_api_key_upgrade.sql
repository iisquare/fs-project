-- Agent API Key 增量升级（执行一次）

CREATE TABLE IF NOT EXISTS `fs_agent_api_key` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '',
  `uid` int NOT NULL DEFAULT '0' COMMENT '密钥所属用户',
  `secret` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '密钥',
  `agentic_ids` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '允许调用的编排流程',
  `proxyable` tinyint NOT NULL DEFAULT '0' COMMENT '是否接受用户会话并按会话用户记账',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1-启用，2-禁用',
  `expired_time` bigint NOT NULL DEFAULT '0',
  `last_used_time` bigint NOT NULL DEFAULT '0',
  `created_time` bigint NOT NULL DEFAULT '0',
  `created_uid` int NOT NULL DEFAULT '0',
  `updated_time` bigint NOT NULL DEFAULT '0',
  `updated_uid` int NOT NULL DEFAULT '0',
  `deleted_time` bigint NOT NULL DEFAULT '0',
  `deleted_uid` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniq_secret` (`secret`) USING BTREE,
  KEY `idx_uid` (`uid`) USING BTREE,
  KEY `idx_last_used_time` (`last_used_time`) USING BTREE,
  KEY `idx_deleted_time` (`deleted_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE `fs_agent_agentic_log`
  ADD COLUMN `api_key_id` int NOT NULL DEFAULT '0'
    COMMENT 'agent 外部调用密钥' AFTER `ip`,
  ADD COLUMN `caller_type` varchar(32) NOT NULL DEFAULT 'session'
    COMMENT '调用来源：session/api_key/user_proxy' AFTER `api_key_id`,
  ADD COLUMN `user_proxy` tinyint NOT NULL DEFAULT '0'
    COMMENT '是否使用用户会话代理' AFTER `caller_type`,
  ADD COLUMN `external_user_id` varchar(128) NOT NULL DEFAULT ''
    COMMENT '外部系统终端用户标识，仅审计' AFTER `user_proxy`,
  ADD COLUMN `trace_id` varchar(128) NOT NULL DEFAULT ''
    COMMENT '调用链追踪标识' AFTER `external_user_id`;

ALTER TABLE `fs_agent_agentic_log`
  ADD KEY `idx_api_key_id` (`api_key_id`) USING BTREE,
  ADD KEY `idx_trace_id` (`trace_id`) USING BTREE,
  ADD KEY `idx_external_user_id` (`external_user_id`) USING BTREE;

ALTER TABLE `fs_lm_usage`
  ADD COLUMN `trace_id` varchar(128) NOT NULL DEFAULT ''
    COMMENT '调用链追踪标识' AFTER `auth_id`,
  ADD KEY `idx_trace_id` (`trace_id`) USING BTREE;
