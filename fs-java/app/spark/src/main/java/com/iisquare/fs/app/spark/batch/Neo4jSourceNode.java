package com.iisquare.fs.app.spark.batch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.app.spark.core.SparkRunner;
import com.iisquare.fs.app.spark.core.SparkTypes;
import com.iisquare.fs.app.spark.helper.Neo4jHelper;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.core.DAGType;
import com.iisquare.fs.base.dag.source.AbstractNeo4jSource;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.IsoDuration;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Point;
import org.neo4j.driver.types.Relationship;

import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Neo4j Cypher 批量读取。
 *
 * 当查询只有一个返回列时，节点/映射结果会展开为 Dataset 的顶层字段；
 * 多列结果按 Cypher 返回别名保留。调试模式会在末尾注入较小的 LIMIT。
 */
public class Neo4jSourceNode extends AbstractNeo4jSource<Dataset<Row>> {

    private static final Pattern TRAILING_NUMERIC_LIMIT =
            Pattern.compile("(?is)^(.*\\bLIMIT\\s+)(\\d+)\\s*$");
    private static final Pattern TRAILING_PARAMETER_LIMIT =
            Pattern.compile("(?is).*\\bLIMIT\\s+\\$\\w+\\s*$");

    private String cypher;
    private Map<String, Object> parameters;
    private Map<String, String> config;
    private int effectiveLimit;

    @Override
    public DAGType<Dataset<Row>> outputType() {
        return SparkTypes.DATASET;
    }

    @Override
    public boolean configure(JsonNode... configs) {
        if (!super.configure(configs)) return false;
        String uri = options.at("/uri").asText("");
        if (DPUtil.empty(uri)) return false;
        cypher = options.at("/cypher").asText("");
        if (DPUtil.empty(cypher)) return false;
        int nodeLimit = options.at("/limit").asInt(0);
        int debugLimit = options.at("/debugLimit").asInt(0);
        effectiveLimit = nodeLimit > 0 ? nodeLimit : 0;
        if (debugLimit > 0) {
            effectiveLimit = effectiveLimit > 0 ? Math.min(effectiveLimit, debugLimit) : debugLimit;
        }
        cypher = limitedCypher(cypher, effectiveLimit);
        parameters = new LinkedHashMap<>();
        JsonNode paramNode = options.get("params");
        if (paramNode != null && paramNode.isTextual()) paramNode = DPUtil.parseJSON(paramNode.asText());
        if (paramNode != null && paramNode.isObject()) {
            JsonNode source = paramNode;
            Map<?, ?> parsed = DPUtil.toJSON(source, Map.class);
            if (parsed != null) {
                for (Map.Entry<?, ?> entry : parsed.entrySet()) {
                    parameters.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
        }
        config = new LinkedHashMap<>();
        config.put("uri", uri);
        config.put("username", options.at("/username").asText(""));
        config.put("password", options.at("/password").asText(""));
        config.put("database", options.at("/database").asText(""));
        config.put("connectTimeout", options.at("/connectTimeout").asText("3000"));
        config.put("maxConnectionLifetime", options.at("/maxConnectionLifetime").asText("3600000"));
        config.put("maxSize", options.at("/maxSize").asText("20"));
        return true;
    }

    @Override
    public Dataset<Row> process() {
        SparkSession spark = runner(SparkRunner.class).session();
        List<String> lines = new ArrayList<>();
        try (Neo4jHelper helper = new Neo4jHelper(config);
             Session session = helper.session()) {
            Result result = session.run(cypher, parameters);
            while (result.hasNext()) {
                lines.add(toJson(result.next()));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Neo4j读取失败：" + e.getMessage(), e);
        }
        if (lines.isEmpty()) return spark.emptyDataFrame();
        return spark.read().json(spark.createDataset(lines, Encoders.STRING()));
    }

    private String toJson(Record record) {
        ObjectNode row = DPUtil.objectNode();
        if (record.size() == 1) {
            String key = record.keys().get(0);
            Object value = normalize(record.get(key).asObject());
            if (value instanceof Map<?, ?> map) {
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    row.set(String.valueOf(entry.getKey()), DPUtil.toJSON(normalize(entry.getValue())));
                }
            } else {
                row.set(key, DPUtil.toJSON(value));
            }
        } else {
            for (String key : record.keys()) {
                row.set(key, DPUtil.toJSON(normalize(record.get(key).asObject())));
            }
        }
        return DPUtil.stringify(row);
    }

    private Object normalize(Object value) {
        if (value == null) return null;
        if (value instanceof Node node) {
            Map<String, Object> result = objectMap(node.asMap());
            result.put("_elementId", node.elementId());
            List<String> labels = new ArrayList<>();
            node.labels().forEach(labels::add);
            result.put("_labels", labels);
            return result;
        }
        if (value instanceof Relationship relationship) {
            Map<String, Object> result = objectMap(relationship.asMap());
            result.put("_elementId", relationship.elementId());
            result.put("_type", relationship.type());
            result.put("_startNodeElementId", relationship.startNodeElementId());
            result.put("_endNodeElementId", relationship.endNodeElementId());
            return result;
        }
        if (value instanceof Path path) {
            Map<String, Object> result = new LinkedHashMap<>();
            List<Object> nodes = new ArrayList<>();
            path.nodes().forEach(node -> nodes.add(normalize(node)));
            List<Object> relationships = new ArrayList<>();
            path.relationships().forEach(relationship -> relationships.add(normalize(relationship)));
            result.put("nodes", nodes);
            result.put("relationships", relationships);
            result.put("length", path.length());
            result.put("start", normalize(path.start()));
            result.put("end", normalize(path.end()));
            return result;
        }
        if (value instanceof Point point) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("srid", point.srid());
            result.put("x", point.x());
            result.put("y", point.y());
            result.put("z", point.z());
            return result;
        }
        if (value instanceof IsoDuration || value instanceof TemporalAccessor) {
            return value.toString();
        }
        if (value instanceof byte[] bytes) {
            return Base64.getEncoder().encodeToString(bytes);
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> source = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                source.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return objectMap(source);
        }
        if (value instanceof Collection<?> collection) {
            List<Object> result = new ArrayList<>();
            for (Object item : collection) result.add(normalize(item));
            return result;
        }
        return value;
    }

    private Map<String, Object> objectMap(Map<String, Object> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            result.put(entry.getKey(), normalize(entry.getValue()));
        }
        return result;
    }

    static String limitedCypher(String query, int limit) {
        String result = DPUtil.trim(query);
        while (result.endsWith(";")) result = result.substring(0, result.length() - 1).trim();
        if (limit < 1 || DPUtil.empty(result)) return result;
        if (TRAILING_PARAMETER_LIMIT.matcher(result).matches()) return result;
        Matcher matcher = TRAILING_NUMERIC_LIMIT.matcher(result);
        if (matcher.matches()) {
            int existing = DPUtil.parseInt(matcher.group(2));
            int effective = existing > 0 ? Math.min(existing, limit) : limit;
            return matcher.group(1) + effective;
        }
        return result + " LIMIT " + limit;
    }
}
