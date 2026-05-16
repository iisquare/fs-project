package com.iisquare.fs.web.member.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.web.util.VerifyCodeUtil;
import com.iisquare.fs.web.member.core.RedisKey;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Service
public class CaptchaService {

    @Autowired
    StringRedisTemplate redis;

    public Map<String, Object> generate(HttpServletRequest request, HttpServletResponse response) {
        long time = DPUtil.parseLong(request.getParameter("time"));
        if (Math.abs(time - System.currentTimeMillis()) > 6000) {
            return ApiUtil.result(1001, "时间戳不同步", time);
        }

        String code = VerifyCodeUtil.generateVerifyCode(4);
        String base64;
        try {
            base64 = VerifyCodeUtil.outputImage(100, 40, code);
        } catch (IOException e) {
            return ApiUtil.result(1501, "生成验证码图形失败", e.getMessage());
        }
        String uuid = request.getParameter("uuid");
        if (!DPUtil.empty(uuid)) { // 若存在则尝试复用，避免浪费
            if (DPUtil.empty(redis.hasKey(RedisKey.captcha(uuid)))) {
                uuid = ""; // 不存在或已过期
            }
        }
        if (DPUtil.empty(uuid)) {
            uuid = UUID.randomUUID().toString();
        }
        ObjectNode captcha = DPUtil.objectNode();
        captcha.put("time", time);
        captcha.put("uuid", uuid);
        captcha.put("code", code);
        captcha.put("retry", DPUtil.parseInt(request.getParameter("retry"))); // 记录重试次数
        redis.opsForValue().set(RedisKey.captcha(uuid), captcha.toString(), Duration.ofMinutes(5));
        captcha.remove("code"); // 移除校验码
        captcha.put("base64", base64);
        return ApiUtil.result(0, null, captcha);
    }

    public Map<String, Object> verify(Map<String, Object> param) {
        String uuid = DPUtil.parseString(param.get("uuid"));
        if (DPUtil.empty(uuid)) {
            return ApiUtil.result(131001, "验证标识不能为空", uuid);
        }
        String code = DPUtil.parseString(param.get("code"));
        if (DPUtil.empty(code)) {
            return ApiUtil.result(131002, "验证码不能为空", code);
        }
        JsonNode captcha = DPUtil.parseJSON(redis.opsForValue().getAndDelete(RedisKey.captcha(uuid)));
        if (null == captcha) {
            return ApiUtil.result(131401, "验证码已过期", null);
        }
        if (code.equalsIgnoreCase(captcha.at("/code").asText())) {
            return ApiUtil.result(0, null, captcha);
        }
        return ApiUtil.result(131403, "验证码不正确", uuid);
    }

}
