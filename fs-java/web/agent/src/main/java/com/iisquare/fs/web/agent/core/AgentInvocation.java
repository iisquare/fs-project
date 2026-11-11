package com.iisquare.fs.web.agent.core;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 一次编排调用的生效身份与 LM 认证信息。
 *
 * @param uid      生效用户；API Key 开启用户代理时为会话用户，否则为密钥所属用户
 * @param identity 生效用户身份（含启用角色），用于编排授权角色校验
 * @param session  需要传给 LM 的成员会话；API Key 未开启用户代理时为空
 * @param apiKeyId agent 外部调用密钥标识；网页会话调用时为 0
 * @param callerType session / api_key / user_proxy
 * @param traceId 贯穿 agent 与 LM 调用记录的追踪标识
 */
public record AgentInvocation(int uid, JsonNode identity, String session, int apiKeyId,
                              String callerType, boolean userProxy,
                              String externalUserId, String traceId) {
}
