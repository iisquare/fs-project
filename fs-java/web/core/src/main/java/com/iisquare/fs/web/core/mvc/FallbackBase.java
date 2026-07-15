package com.iisquare.fs.web.core.mvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import feign.Request;
import feign.Response;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.*;

public abstract class FallbackBase implements RpcBase {

    @Override
    public Response get(String uri, Map param) {
        return fallback();
    }

    public Response get(String uri, Map param, Throwable cause) {
        return fallback(cause);
    }

    @Override
    public Response post(String uri, Map param) {
        return fallback();
    }

    public Response post(String uri, Map param, Throwable cause) {
        return fallback(cause);
    }

    @Override
    public Response post(String uri, JsonNode json) {
        return fallback();
    }

    public Response post(String uri, JsonNode json, Throwable cause) {
        return fallback(cause);
    }

    @Override
    public Response post(String uri, String body) {
        return fallback();
    }

    public Response post(String uri, String body, Throwable cause) {
        return fallback(cause);
    }

    @Override
    public Response form(String uri, Map param, MultipartFile... files) {
        return fallback();
    }

    public Response form(String uri, Map param, MultipartFile[] files, Throwable cause) {
        return fallback(cause);
    }

    public Response fallback() {
        String message = getClass().getSimpleName();
        String body = ApiUtil.echoResult(4501, message, null);
        return Response.builder()
                .status(200)
                .request(Request.create(Request.HttpMethod.POST, "/", Collections.emptyMap(), Request.Body.empty(), null))
                .headers(new HashMap<>())
                .body(body, StandardCharsets.UTF_8)
                .build();
    }

    public Response fallback(Throwable cause) {
        String message = getClass().getSimpleName();
        String data = null == cause ? null : cause.getMessage();
        String body = ApiUtil.echoResult(4502, message, data);
        return Response.builder()
                .status(200)
                .request(Request.create(Request.HttpMethod.POST, "/", Collections.emptyMap(), Request.Body.empty(), null))
                .headers(new HashMap<>())
                .body(body, StandardCharsets.UTF_8)
                .build();
    }

    public String fallbackString() {
        String message = getClass().getSimpleName();
        return ApiUtil.echoResult(4501, message, null);
    }

    public String fallbackString(Throwable cause) {
        String message = getClass().getSimpleName();
        String data = null == cause ? null : cause.getMessage();
        return ApiUtil.echoResult(4502, message, data);
    }

}
