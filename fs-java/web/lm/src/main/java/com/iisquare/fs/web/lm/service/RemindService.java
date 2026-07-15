package com.iisquare.fs.web.lm.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.base.web.util.RpcUtil;
import com.iisquare.fs.web.core.rpc.MemberRpc;
import com.iisquare.fs.web.lm.core.RedisKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RemindService {

    @Autowired
    MemberRpc memberRpc;
    @Autowired
    StringRedisTemplate redis;
    @Value("${fs.lm.remind.type:}")
    private String type;
    @Value("${fs.lm.remind.secret:}")
    private String secret;
    public static final String HTML_AMOUNT;
    public static final String HTML_LIMITED;

    static {
        try (InputStream input = new ClassPathResource("/email/amount.html").getInputStream()) {
            HTML_AMOUNT = FileUtil.getContent(input, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (InputStream input = new ClassPathResource("/email/limited.html").getInputStream()) {
            HTML_LIMITED = FileUtil.getContent(input, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Object> amount(ObjectNode auth, double amount) {
        double remained = auth.at("/credit/remained").asDouble();
        boolean remindEnabled = auth.at("/credit/remindEnabled").asBoolean();
        double remindThreshold = auth.at("/credit/remindThreshold").asDouble();
        if (remained <= remindThreshold) {
            return ApiUtil.result(12002, "余额已小于预警阈值", remindThreshold);
        }
        if (remained - amount > remindThreshold) {
            return ApiUtil.result(12003, "余额未达到预警阈值", remindThreshold);
        }
        String key = RedisKey.remindAmount(auth.at("/uid").asInt());
        Boolean locked = redis.opsForValue().setIfAbsent(key, String.valueOf(remindThreshold), 30, TimeUnit.MINUTES);
        if (Boolean.FALSE.equals(locked)) {
            return ApiUtil.result(13002, "已执行过通知，半小时内不再重复处理", remindThreshold);
        }
        String name = auth.at("/identity/name").asText();
        String subject = "平方域积分不足提醒";
        Map<String, Object> result = message(subject, text("%s\n------\n用户：%s[%d]\n阈值：%f",
                subject, auth.at("/name").asText(), auth.at("/uid").asInt(), remindThreshold
        ).toString());
        if (remindEnabled) {
            String email = auth.at("/identity/email").asText();
            if (DPUtil.empty(email)) {
                return ApiUtil.result(13001, "未配置邮箱", email);
            }
            String html = HTML_AMOUNT.replace("{name}", name).replace("{threshold}", String.valueOf(remindThreshold));
            result = RpcUtil.result(memberRpc.email(email, subject, html));
            if (ApiUtil.failed(result)) {
                log.warn("usage remind send email failed: {}", DPUtil.stringify(result));
                redis.delete(key); // 发送失败，清理分布式锁
            }
        }
        return result;
    }

    public Map<String, Object> limited(ObjectNode auth, Map.Entry<String, String> entry) {
        boolean remindEnabled = auth.at("/credit/remindEnabled").asBoolean();
        String key = RedisKey.remindLimited(entry.getKey(), auth.at("/uid").asInt());
        Boolean locked = redis.opsForValue().setIfAbsent(key, entry.getKey(), 30, TimeUnit.MINUTES);
        if (Boolean.FALSE.equals(locked)) {
            return ApiUtil.result(13002, "已执行过通知，半小时内不再重复处理", entry.getKey());
        }
        String name = auth.at("/identity/name").asText();
        String subject = "平方域" + entry.getValue() + "间隔内用量超限提醒";
        Map<String, Object> result = message(subject, text("%s\n------\n用户：%s[%d]\n类型：%d",
                subject, auth.at("/name").asText(), auth.at("/uid").asInt(), entry.getValue()
        ).toString());
        if (remindEnabled) {
            String email = auth.at("/identity/email").asText();
            if (DPUtil.empty(email)) {
                return ApiUtil.result(13001, "未配置邮箱", email);
            }
            String html = HTML_LIMITED.replace("{name}", name).replace("{type}", entry.getValue());
            result = RpcUtil.result(memberRpc.email(email, subject, html));
            if (ApiUtil.failed(result)) {
                log.warn("usage limited send email failed: {}", DPUtil.stringify(result));
                redis.delete(key); // 发送失败，清理分布式锁
            }
        }
        return result;
    }

    public Map<String, Object> message(String subject, String content) {
        ObjectNode args = DPUtil.objectNode();
        args.put("recipient", secret);
        args.put("subject", subject);
        args.put("content", content);
        return switch (type) {
            case "wecom" -> RpcUtil.result(memberRpc.post("/rpc/wecom", args));
            case "dingtalk" -> RpcUtil.result(memberRpc.post("/rpc/dingtalk", args));
            default -> ApiUtil.result(17404, "配置异常", type);
        };
    }

    public ObjectNode text(String format, Object... args) {
        ObjectNode msg = DPUtil.objectNode();
        msg.put("msgtype", "text");
        ObjectNode text = msg.putObject("text");
        text.put("content", String.format(format, args));
        return msg;
    }

}
