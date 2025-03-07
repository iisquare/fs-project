package com.iisquare.fs.web.govern.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.elasticsearch.mvc.ElasticsearchBase;
import org.springframework.stereotype.Component;

import java.util.Iterator;

@Component
public class MetaES extends ElasticsearchBase {

    public MetaES() {
        this.collection = "fs_govern_meta";
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
        properties.putObject("code").put("type", "keyword") // 编码
                .put("index", true).put("store", true);
        properties.putObject("name").put("type", "keyword") // 名称
                .put("index", true).put("store", true);
        properties.putObject("mold").put("type", "keyword") // 类型[包、类、字段]
                .put("index", true).put("store", true);
        properties.putObject("description").put("type", "text") // 描述
                .put("index", true).put("store", false);

        properties.putObject("time").put("type", "long") // 索引时间
                .put("index", true).put("store", true);
        return source;
    }

    public ArrayNode formatModel(ArrayNode data, Long time) {
        ArrayNode result = DPUtil.arrayNode();
        Iterator<JsonNode> iterator = data.iterator();
        while (iterator.hasNext()) {
            JsonNode db = iterator.next();
            ObjectNode es = result.addObject();
            es.put("id", db.get("catalog").asText() + db.get("code").asText());
            es.put("code", db.get("code").asText());
            es.put("name", db.get("name").asText());
            es.put("mold", db.get("type").asText());
            es.put("description", db.get("description").asText());
            if (null != time) es.put("time", time);
        }
        return result;
    }

    public ArrayNode formatModelColumn(ArrayNode data, Long time) {
        ArrayNode result = DPUtil.arrayNode();
        Iterator<JsonNode> iterator = data.iterator();
        while (iterator.hasNext()) {
            JsonNode db = iterator.next();
            ObjectNode es = result.addObject();
            es.put("id", db.get("catalog").asText() + db.get("model").asText() + "/" + db.get("code").asText());
            es.put("code", db.get("code").asText());
            es.put("name", db.get("name").asText());
            es.put("mold", "column");
            es.put("description", db.get("description").asText());
            if (null != time) es.put("time", time);
        }
        return result;
    }

}
