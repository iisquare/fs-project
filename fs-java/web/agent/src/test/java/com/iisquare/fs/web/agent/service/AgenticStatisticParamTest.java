package com.iisquare.fs.web.agent.service;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class AgenticStatisticParamTest {

    @Test
    public void apiKeyFilterAppliesToChatsAndLogs() {
        AgenticService service = new AgenticService();
        Map<String, Object> param = new HashMap<>();
        param.put("apiKeyId", 3);
        param.put("type", "published");
        param.put("callerType", "user_proxy");
        param.put("deleted", "without");

        Map<String, Object> chatParam = service.statisticParam(param, 100L, 200L, "type");
        String chatWhere = (String) chatParam.get("where");
        Assert.assertTrue(chatWhere.contains("EXISTS"));
        Assert.assertTrue(chatWhere.contains("statistic_log.api_key_id = #{apiKeyId}"));
        Assert.assertTrue(chatWhere.contains("statistic_log.created_time >= #{beginTime}"));
        Assert.assertTrue(chatWhere.contains("statistic_log.source = #{type}"));
        Assert.assertTrue(chatWhere.contains("statistic_log.caller_type = #{callerType}"));
        Assert.assertTrue(chatWhere.contains("statistic_log.deleted_time = 0"));
        Assert.assertEquals(3, chatParam.get("apiKeyId"));
        Assert.assertEquals("user_proxy", chatParam.get("callerType"));

        Map<String, Object> logParam = service.statisticParam(param, 100L, 200L, "source");
        String logWhere = (String) logParam.get("where");
        Assert.assertTrue(logWhere.contains("AND api_key_id = #{apiKeyId}"));
        Assert.assertTrue(logWhere.contains("AND caller_type = #{callerType}"));
        Assert.assertFalse(logWhere.contains("EXISTS"));
        Assert.assertEquals(3, logParam.get("apiKeyId"));
    }

    @Test
    public void callerTypeFilterAppliesWithoutApiKey() {
        AgenticService service = new AgenticService();
        Map<String, Object> param = new HashMap<>();
        param.put("callerType", "api_key");
        param.put("deleted", "without");

        Map<String, Object> chatParam = service.statisticParam(param, 100L, 200L, "type");
        String chatWhere = (String) chatParam.get("where");
        Assert.assertTrue(chatWhere.contains("EXISTS"));
        Assert.assertTrue(chatWhere.contains("statistic_log.caller_type = #{callerType}"));
        Assert.assertFalse(chatWhere.contains("#{apiKeyId}"));
        Assert.assertEquals("api_key", chatParam.get("callerType"));

        Map<String, Object> logParam = service.statisticParam(param, 100L, 200L, "source");
        String logWhere = (String) logParam.get("where");
        Assert.assertTrue(logWhere.contains("AND caller_type = #{callerType}"));
        Assert.assertFalse(logWhere.contains("#{apiKeyId}"));
    }

}
