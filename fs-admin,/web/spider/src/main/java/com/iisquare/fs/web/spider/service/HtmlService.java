package com.iisquare.fs.web.spider.service;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class HtmlService {

    public Map<?, ?> taskStatus() {
        Map<String, String> status = new LinkedHashMap<>();
        status.put("queuing", "待处理");
        status.put("finished", "已完成");
        return status;
    }

    public Map<?, ?> pageType() {
        Map<String, String> types = new LinkedHashMap<>();
        types.put("list", "列表页");
        types.put("detail", "详情页");
        types.put("page_first", "第一页");
        types.put("page_next", "下一页");
        return types;
    }

    public Map<?, ?> responseStatus() {
        Map<String, String> status = new LinkedHashMap<>();
        status.put("timeout", "请求超时");
        status.put("502", "网关超时");
        status.put("404", "不存在");
        status.put("301", "永久定向");
        status.put("302", "临时定向");
        status.put("200", "正常响应");
        return status;
    }

}
