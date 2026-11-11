package com.iisquare.fs.app.flink.serialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import org.apache.flink.api.common.functions.MapFunction;

import java.nio.charset.StandardCharsets;

/**
 * 将 Kafka 源输出或普通字符串统一为 Kafka 消息体。
 */
public class KafkaMessageMapFunction implements MapFunction<Object, String> {

    private final String messageField;

    public KafkaMessageMapFunction(String messageField) {
        this.messageField = messageField;
    }

    @Override
    public String map(Object value) {
        if (value == null) return "";
        if (value instanceof byte[] bytes) return new String(bytes, StandardCharsets.UTF_8);
        if (value instanceof JsonNode json) {
            if (!DPUtil.empty(messageField) && json.has(messageField)) {
                return json.get(messageField).asText("");
            }
            return DPUtil.stringify(json);
        }
        if (value instanceof String text) return text;
        String json = DPUtil.stringify(value);
        return DPUtil.empty(json) ? String.valueOf(value) : json;
    }
}
