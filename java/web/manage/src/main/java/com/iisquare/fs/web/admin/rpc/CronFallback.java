package com.iisquare.fs.web.admin.rpc;

import com.iisquare.fs.base.core.util.ApiUtil;
import org.springframework.stereotype.Component;
import org.springframework.ui.ModelMap;

import java.util.Map;

@Component
public class CronFallback implements CronRpc {

    @Override
    public String list(Map<String, Object> params) {
        return fallback();
    }

    @Override
    public String map(Map<String, Object> params) {
        return fallback();
    }

    @Override
    public String add(Map<String, Object> params) {
        return fallback();
    }

    @Override
    public String modify(Map<String, Object> params) {
        return fallback();
    }

    @Override
    public String pause(Map<String, Object> params) {
        return fallback();
    }

    @Override
    public String resume(Map<String, Object> params) {
        return fallback();
    }

    @Override
    public String run(Map<String, Object> params) {
        return fallback();
    }

    @Override
    public String delete(Map<String, Object> params) {
        return fallback();
    }

    @Override
    public String logs(Map<String, Object> params) {
        return fallback();
    }

    @Override
    public String log(Map<String, Object> params) {
        return fallback();
    }

    @Override
    public String config(ModelMap model) {
        return fallback();
    }

    @Override
    public String serviceInfo() {
        return fallback();
    }

    @Override
    public String start(Map<String, Object> params) {
        return fallback();
    }

    @Override
    public String standby(Map<String, Object> params) {
        return fallback();
    }

    @Override
    public String isStandby(Map<String, Object> params) {
        return fallback();
    }

    @Override
    public String isValidCron(Map<String, Object> params) {
        return fallback();
    }

    private String fallback() {
        return ApiUtil.echoResult(500, "调用quartz服务失败", null);
    }
}
