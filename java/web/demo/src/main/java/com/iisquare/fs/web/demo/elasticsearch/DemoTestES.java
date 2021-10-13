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
        properties.putObject("name").put("type", "keyword") // 名称
                .put("index", true).put("store", true);
        properties.putObject("full").put("type", "keyword") // 长文本
                .put("index", true).put("store", true)
                .put("ignore_above", 65535);
        properties.putObject("content").put("type", "text") // 内容
                .put("index", true).put("store", false);
        properties.putObject("time").put("type", "long") // 日期
                .put("index", true).put("store", true);

        properties.putObject("tag_a").put("type", "keyword") // 标签A
                .put("index", true).put("store", true);
        properties.putObject("tag_b").put("type", "keyword") // 标签B
                .put("index", true).put("store", true);

        // 拷贝字段
        properties.putObject("t_a").put("type", "keyword")
                .put("index", true).put("store", true)
                .put("copy_to", "tag_a");
        properties.putObject("t_ab").put("type", "keyword")
                .put("index", true).put("store", true)
                .putArray("copy_to").add("tag_a").add("tag_b");

        return source;
    }

}
