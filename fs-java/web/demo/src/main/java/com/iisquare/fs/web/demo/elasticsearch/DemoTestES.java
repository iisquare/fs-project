package com.iisquare.fs.web.demo.elasticsearch;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.elasticsearch.mvc.ElasticsearchBase;
import org.springframework.stereotype.Component;

@Component
public class DemoTestES extends ElasticsearchBase {

    public DemoTestES() {
        this.collection = "fs_demo_test";
    }

    @Override
    public String id(ObjectNode source) {
        return source.get("id").asText();
    }

    @Override
    protected ObjectNode mapping() {
        ObjectNode source = DPUtil.objectNode();
        source.putObject("_source").put("enabled", true);
        source.putObject("_all").put("enabled", false);
        ObjectNode properties = source.putObject("properties");
        properties.putObject("id").put("type", "keyword") // 唯一标识
                .put("index", true).put("store", true);
        properties.putObject("name_text").put("type", "text") // 名称
                .put("index", true).put("store", true);
        properties.putObject("name_keyword").put("type", "keyword") // 名称
                .put("index", true).put("store", true);
        properties.putObject("name_wildcard").put("type", "wildcard") // 名称
                .put("index", true).put("store", false);
        return source;
    }

}
