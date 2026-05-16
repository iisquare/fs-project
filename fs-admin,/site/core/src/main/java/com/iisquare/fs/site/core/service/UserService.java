package com.iisquare.fs.site.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.BeautifyUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.web.util.RpcUtil;
import com.iisquare.fs.web.core.rpc.MemberRpc;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class UserService {

    @Autowired
    MemberRpc memberRpc;

    public Map<String, Object> post(String uri, JsonNode params) {
        if (params.isObject()) {
            ((ObjectNode) params).put("module", "website");
        }
        return RpcUtil.result(memberRpc.post(uri, params));
    }

    public Map<String, Object> login(JsonNode params, HttpServletRequest request) {
        Map<String, Object> result = post("/user/login", params);
        if (ApiUtil.failed(result)) return result;
        JsonNode info = ApiUtil.data(result, JsonNode.class).at("/info");
        if (!info.isObject()) return result;
        ObjectNode node = (ObjectNode) info;
        if (node.has("email")) {
            node.put("email", BeautifyUtil.desensitizeEmail(info.at("/email").asText()));
        }
        if (node.has("phone")) {
            node.put("phone", BeautifyUtil.desensitizePhone(info.at("/phone").asText()));
        }
        return result;
    }

    public Map<String, Object> logout(JsonNode params, HttpServletRequest request) {
        return post("/user/logout", params);
    }

    public Map<String, Object> signup(JsonNode params, HttpServletRequest request) {
        return post("/user/signup", params);
    }

    public Map<String, Object> forgot(JsonNode params, HttpServletRequest request) {
        return post("/user/forgot", params);
    }

    public Map<String, Object> info(JsonNode params, HttpServletRequest request) {
        return login(params, request);
    }

    public JsonNode currentInfo(HttpServletRequest request) {
        Map<String, Object> result = post("/user/login", DPUtil.objectNode());
        if (ApiUtil.failed(result)) return DPUtil.objectNode();
        return ApiUtil.data(result, JsonNode.class).at("/info");
    }

}
