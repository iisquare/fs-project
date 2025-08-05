package com.iisquare.fs.base.neo4j.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.neo4j.driver.internal.value.*;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Relationship;
import org.neo4j.driver.util.Pair;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Neo4jUtil {

    public static final String FIELD_IDENTITY = "identity";
    public static final String FIELD_LABELS = "labels";
    public static final String FIELD_PROPERTIES = "properties";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_START_NODE_ID = "start";
    public static final String FIELD_END_NODE_ID = "end";

    public static List<String> labels(JsonNode node) {
        List<String> result = new ArrayList<>();
        Iterator<JsonNode> iterator = node.at("/" + FIELD_LABELS).iterator();
        while (iterator.hasNext()) {
            String label = iterator.next().asText();
            assertSafe(label);
            result.add(label);
        }
        return result;
    }

    /**
     * 节点格式示例：
     * {
     *     "identity": 0,
     *     "labels": [
     *         "Person"
     *     ],
     *     "properties": {
     *         "name": "John"
     *     }
     * }
     */
    public static ObjectNode node2json(Node node) {
        ObjectNode result = DPUtil.objectNode();
        result.put(FIELD_IDENTITY, node.id());
        ArrayNode labels = result.putArray(FIELD_LABELS);
        Iterator<String> iterator = node.labels().iterator();
        while (iterator.hasNext()) {
            labels.add(iterator.next());
        }
        result.replace(FIELD_PROPERTIES, DPUtil.toJSON(node.asMap()));
        return result;
    }

    /**
     * 关系格式示例：
     * {
     *     "identity": 8,
     *     "start": 17,
     *     "end": 18,
     *     "type": "RL",
     *     "properties": {
     *         "title": "Teach"
     *     }
     * }
     */
    public static ObjectNode relationship2json(Relationship relationship) {
        ObjectNode result = DPUtil.objectNode();
        result.put(FIELD_IDENTITY, relationship.id());
        result.put(FIELD_TYPE, relationship.type());
        result.put(FIELD_START_NODE_ID, relationship.startNodeId());
        result.put(FIELD_END_NODE_ID, relationship.endNodeId());
        result.replace(FIELD_PROPERTIES, DPUtil.toJSON(relationship.asMap()));
        return result;
    }

    public static JsonNode path2json(Path path) {
        ObjectNode result = DPUtil.objectNode();
        result.replace("start", node2json(path.start()));
        result.replace("end", node2json(path.end()));
        ArrayNode relationships = result.putArray("relationships");
        Iterator<Relationship> iterator = path.relationships().iterator();
        while (iterator.hasNext()) {
            relationships.add(relationship2json(iterator.next()));
        }
        result.put("length", path.length());
        return result;
    }

    public static JsonNode value2json(Object value) {
        if (value instanceof NullValue) return DPUtil.toJSON(null);
        if (value instanceof BooleanValue) return DPUtil.toJSON(((BooleanValue) value).asBoolean());
        if (value instanceof FloatValue) return DPUtil.toJSON(((FloatValue) value).asFloat());
        if (value instanceof IntegerValue) return DPUtil.toJSON(((IntegerValue) value).asInt());
        if (value instanceof MapValue) return DPUtil.toJSON(((MapValue) value).asMap());
        if (value instanceof StringValue) return DPUtil.toJSON(((StringValue) value).asString());
        if (value instanceof Node) return node2json((Node) value);
        if (value instanceof NodeValue) return node2json(((NodeValue) value).asNode());
        if (value instanceof Relationship) return relationship2json((Relationship) value);
        if (value instanceof RelationshipValue) return relationship2json(((RelationshipValue) value).asRelationship());
        if (value instanceof Path) return path2json((Path) value);
        if (value instanceof PathValue) return path2json(((PathValue) value).asPath());
        if (value instanceof DateTimeValue) return DPUtil.toJSON(((DateTimeValue) value).asZonedDateTime().toEpochSecond());
        if (value instanceof ListValue) {
            ArrayNode list = DPUtil.arrayNode();
            for (Object item : ((ListValue) value).asList()) {
                list.add(value2json(item));
            }
            return list;
        }
        return DPUtil.toJSON(value);
    }

    public static ObjectNode record2json(Record record) {
        ObjectNode result = DPUtil.objectNode();
        for (Pair<String, Value> pair : record.fields()) {
            result.replace(pair.key(), value2json(pair.value()));
        }
        return result;
    }

    public static ArrayNode result2json(Result result) {
        ArrayNode data = DPUtil.arrayNode();
        for (Record record : result.list()) {
            data.add(record2json(record));
        }
        return data;
    }

    public static void print(Result result) {
        int index = 0;
        for (Record record : result.list()) {
            System.out.println(String.format("Result Record %d\n--------------------", index++));
            for (Pair<String, Value> pair : record.fields()) {
                System.out.println(String.format("Field Key: %s", pair.key()));
                System.out.println(String.format("Field Value: %s", value2json(pair.value())));
            }
        }
    }

    public static boolean isSafe(String str) {
        return str.matches("^[a-zA-Z][a-zA-Z0-9]*$");
    }

    public static void assertSafe(String str) {
        if (!isSafe(str)) throw new RuntimeException("neo4j assert not safe:" + str);
    }

    public static ObjectNode mergePath(ArrayNode paths, String key) {
        ObjectNode result = DPUtil.objectNode();
        ObjectNode nodes = result.putObject("nodes");
        ObjectNode relations = result.putObject("relations");
        Iterator<JsonNode> iterator = paths.iterator();
        while (iterator.hasNext()) {
            JsonNode path = iterator.next().at("/" + key);
            nodes.replace(path.at("/start/identity").asText(), path.at("/start"));
            nodes.replace(path.at("/end/identity").asText(), path.at("/end"));
            Iterator<JsonNode> it = path.at("/relationships").iterator();
            while (it.hasNext()) {
                JsonNode relationship = it.next();
                relations.replace(relationship.at("/identity").asText(), relationship);
            }
        }
        return result;
    }

    public static ObjectNode singleRelationship(Result result) {
        if (!result.hasNext()) return DPUtil.objectNode();
        return Neo4jUtil.relationship2json(result.single().get(0).asRelationship());
    }

    public static ObjectNode singleNode(Result result) {
        if (!result.hasNext()) return DPUtil.objectNode();
        return Neo4jUtil.node2json(result.single().get(0).asNode());
    }

    public static long singleLong(Result result) {
        if (!result.hasNext()) return -1;
        return result.single().get(0).asLong();
    }

    public static Value parameters(Map<String, Object> args) {
        List<Object> parameters = new ArrayList<>();
        for (Map.Entry<String, Object> entry : args.entrySet()) {
            parameters.add(entry.getKey());
            parameters.add(entry.getValue());
        }
        return Values.parameters(parameters.toArray(new Object[0]));
    }

}
