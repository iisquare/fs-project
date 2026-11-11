package com.iisquare.fs.app.spark.batch;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.app.spark.core.SparkTypes;
import com.iisquare.fs.app.spark.helper.Neo4jHelper;
import com.iisquare.fs.app.spark.util.SparkUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.core.DAGType;
import com.iisquare.fs.base.dag.sink.AbstractNeo4jSink;
import org.apache.spark.api.java.function.ForeachPartitionFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

import java.lang.reflect.Array;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 通过 Cypher UNWIND 分批写入 Neo4j。
 *
 * 写入语句需使用 $rows 接收当前批次，如：
 * UNWIND $rows AS row MERGE (n:Person {id: row.id}) SET n += row
 */
public class Neo4jSinkNode extends AbstractNeo4jSink<Void> {

    private String cypher;
    private int batchSize;
    private Map<String, String> config;
    private Set<String> requiredFields;
    private Set<String> allowedFields;

    @Override
    public DAGType<Void> outputType() {
        return SparkTypes.VOID;
    }

    @Override
    public boolean configure(JsonNode... configs) {
        if (!super.configure(configs)) return false;
        String uri = options.at("/uri").asText("");
        if (DPUtil.empty(uri)) return false;
        cypher = options.at("/cypher").asText("");
        if (DPUtil.empty(cypher)) return false;
        batchSize = Math.max(1, options.at("/batchSize").asInt(500));
        config = new LinkedHashMap<>();
        config.put("uri", uri);
        config.put("username", options.at("/username").asText(""));
        config.put("password", options.at("/password").asText(""));
        config.put("database", options.at("/database").asText(""));
        config.put("connectTimeout", options.at("/connectTimeout").asText("3000"));
        config.put("maxConnectionLifetime", options.at("/maxConnectionLifetime").asText("3600000"));
        config.put("maxSize", options.at("/maxSize").asText("20"));
        requiredFields = new LinkedHashSet<>();
        allowedFields = new LinkedHashSet<>();
        JsonNode required = options.get("requiredFields");
        if (required != null && required.isArray()) {
            for (JsonNode item : required) {
                if (!DPUtil.empty(item.asText(""))) requiredFields.add(item.asText());
            }
        }
        JsonNode allowed = options.get("allowedFields");
        if (allowed != null && allowed.isArray()) {
            for (JsonNode item : allowed) {
                if (!DPUtil.empty(item.asText(""))) allowedFields.add(item.asText());
            }
        }
        return true;
    }

    @Override
    public Void process() {
        Dataset<Row> dataset = SparkUtil.union(inputs(SparkTypes.DATASET));
        Set<String> columns = new LinkedHashSet<>(List.of(dataset.columns()));
        for (String field : requiredFields) {
            if (!columns.contains(field)) {
                throw new IllegalStateException("Neo4j写入缺少本体必填字段：" + field);
            }
        }
        String statement = cypher;
        int size = batchSize;
        Map<String, String> settings = config;
        Set<String> allowed = allowedFields;
        dataset.foreachPartition((ForeachPartitionFunction<Row>) rows -> {
            try (Neo4jHelper helper = new Neo4jHelper(settings)) {
                List<Map<String, Object>> batch = new ArrayList<>(size);
                while (rows.hasNext()) {
                    Map<String, Object> row = rowMap(rows.next());
                    if (!allowed.isEmpty()) row.keySet().retainAll(allowed);
                    batch.add(row);
                    if (batch.size() >= size) {
                        helper.execute(statement, Map.of("rows", batch));
                        batch = new ArrayList<>(size);
                    }
                }
                if (!batch.isEmpty()) helper.execute(statement, Map.of("rows", batch));
            }
        });
        return null;
    }

    private static Map<String, Object> rowMap(Row row) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int index = 0; index < row.size(); index++) {
            result.put(row.schema().fields()[index].name(), parameter(row.get(index)));
        }
        return result;
    }

    private static Object parameter(Object value) {
        if (value == null) return null;
        if (value instanceof Row row) return rowMap(row);
        if (value instanceof Timestamp timestamp) return timestamp.toInstant().toString();
        if (value instanceof java.sql.Date date) return date.toLocalDate().toString();
        if (value instanceof Date date) return date.toInstant().toString();
        if (value instanceof Instant) return value.toString();
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                result.put(String.valueOf(entry.getKey()), parameter(entry.getValue()));
            }
            return result;
        }
        if (value instanceof Collection<?> collection) {
            List<Object> result = new ArrayList<>(collection.size());
            for (Object item : collection) result.add(parameter(item));
            return result;
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            List<Object> result = new ArrayList<>(length);
            for (int index = 0; index < length; index++) result.add(parameter(Array.get(value, index)));
            return result;
        }
        return value;
    }
}
