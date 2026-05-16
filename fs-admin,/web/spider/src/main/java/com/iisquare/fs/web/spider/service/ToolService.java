package com.iisquare.fs.web.spider.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.spider.core.ZooKeeperClient;
import com.iisquare.fs.web.spider.parser.Parser;
import com.iisquare.fs.web.spider.parser.ParserFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ToolService {

    @Autowired
    NodeService nodeService;

    public Map<String, Object> state() {
        ZooKeeperClient zookeeper = nodeService.zookeeper();
        ObjectNode data = DPUtil.objectNode();
        data.putPOJO("participants", zookeeper.participants());
        data.putPOJO("spiders", zookeeper.spiders());
        data.putPOJO("crawlers", zookeeper.crawlers());
        return ApiUtil.result(0, null, data);
    }

    public Map<String, Object> parse(JsonNode json) {
        if (null == json) {
            return ApiUtil.result(1500, "请求参数解析失败", null);
        }
        String baseUri = json.at("/baseUri").asText();
        String expression = json.at("/expression").asText();
        if (DPUtil.empty(expression)) {
            return ApiUtil.result(1001, "表达式不能为空", json);
        }
        String html = json.at("/html").asText();
        if (DPUtil.empty(html)) {
            return ApiUtil.result(1002, "待解析数据不能为空", json);
        }
        ParserFactory factory = new ParserFactory();
        Parser parser;
        try {
            parser = factory.parser(null, expression);
        } catch (Exception e) {
            return ApiUtil.result(1501, "编译表达式失败", e.getMessage());
        }
        JsonNode data;
        try {
            data = parser.parse(html, baseUri);
        } catch (Exception e) {
            return ApiUtil.result(1502, "解析数据失败", e.getMessage());
        }
        return ApiUtil.result(0, null, data);
    }

}
