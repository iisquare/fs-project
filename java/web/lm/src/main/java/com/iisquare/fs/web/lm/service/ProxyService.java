package com.iisquare.fs.web.lm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.lm.core.RedisKey;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

@Service
public class ProxyService extends ServiceBase implements MessageListener, InitializingBean {

    @Autowired
    SensitiveService sensitiveService;
    @Autowired
    private StringRedisTemplate redis;

    @Override
    public void afterPropertiesSet() throws Exception {
        sensitiveService.rebuild();
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String topic = new String(message.getChannel(), StandardCharsets.UTF_8);
        if (!RedisKey.channel().equals(topic)) return;
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        JsonNode json = DPUtil.parseJSON(body);
        if (null == json) return;
        String type = json.at("/type").asText("");
        switch (type) {
            case "sensitive":
                sensitiveService.rebuild();
                return;
            case "model":
                return;
        }
    }

    public Map<String, Object> notice(Map<String, Object> param) {
        String type = DPUtil.parseString(param.get("type"));
        if (!Arrays.asList("sensitive", "model").contains(type)) {
            return ApiUtil.result(1001, "类型异常", type);
        }
        ObjectNode notice = DPUtil.objectNode();
        notice.put("type", type);
        notice.put("time", System.currentTimeMillis());
        redis.convertAndSend(RedisKey.channel(), DPUtil.stringify(notice));
        return ApiUtil.result(0, null, notice);
    }
}
