package com.iisquare.fs.base.neo4j.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.neo4j.util.Neo4jUtil;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;

import java.util.*;

public class CypherParameter {

    private int step;
    public static final String PARAM_PREFIX = "qp";
    private Map<String, Object> parameters = new LinkedHashMap<>();

    public CypherParameter() {
        this.step = 0;
    }

    public CypherParameter reset() {
        this.step = 0;
        this.parameters = new LinkedHashMap<>();
        return this;
    }

    public Value parameters() {
        List<Object> parameters = new ArrayList<>();
        for (Map.Entry<String, Object> entry : this.parameters.entrySet()) {
            parameters.add(entry.getKey());
            parameters.add(entry.getValue());
        }
        return Values.parameters(parameters.toArray(new Object[0]));
    }

    public String variable(Object value) {
        if(value instanceof JsonNode) {
            value = DPUtil.toJSON(value, Object.class);
        }
        String key = PARAM_PREFIX + step++;
        parameters.put(key, value);
        return "$" + key;
    }

    public String properties(JsonNode item) {
        JsonNode properties = item.at("/" + Neo4jUtil.FIELD_PROPERTIES);
        if (!properties.isObject() || properties.size() == 0) return "";
        Iterator<Map.Entry<String, JsonNode>> iterator = properties.fields();
        StringBuilder sb = new StringBuilder(" {");
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            sb.append(entry.getKey()).append(":").append(variable(entry.getValue()));
            if (iterator.hasNext()) sb.append(", ");
        }
        sb.append("}");
        return sb.toString();
    }

    public String labels(JsonNode item) {
        List<String> labels = Neo4jUtil.labels(item);
        if (labels.size() == 0) return "";
        return ":" + DPUtil.implode(":", labels.toArray(new String[0]));
    }

    public String type(JsonNode item) {
        String type = item.at("/" + Neo4jUtil.FIELD_TYPE).asText();
        if (DPUtil.empty(type)) return type;
        Neo4jUtil.assertSafe(type);
        return ":" + type;
    }

    public String where(String field, JsonNode item) {
        JsonNode properties = item.at("/" + Neo4jUtil.FIELD_PROPERTIES);
        if (!properties.isObject() || properties.size() == 0) return "";
        return extract(field, properties, " AND ");
    }

    public String extract(String field, JsonNode properties, String glue) {
        List<String> list = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> iterator = properties.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            list.add(String.format("%s.%s=%s", field, entry.getKey(), variable(entry.getValue())));
        }
        return DPUtil.implode(glue, list.toArray(new String[0]));
    }

    public String in(Object... values) {
        List<String> list = new ArrayList<>();
        for (Object value : values) {
            list.add(variable(value));
        }
        return DPUtil.implode(", ", list.toArray(new String[0]));
    }

}
