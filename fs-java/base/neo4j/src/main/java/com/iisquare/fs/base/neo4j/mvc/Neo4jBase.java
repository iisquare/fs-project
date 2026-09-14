package com.iisquare.fs.base.neo4j.mvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.neo4j.core.CypherParameter;
import com.iisquare.fs.base.neo4j.util.Neo4jUtil;
import org.neo4j.driver.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class Neo4jBase {

    /**
     * 批量创建语句中引用同一语句内局部变量的字段名，注意它不是图数据库的元素标识
     */
    protected static final String FIELD_VARIABLE_START = "start";
    protected static final String FIELD_VARIABLE_END = "end";

    @Autowired
    protected Driver driver;

    /**
     * 按元素标识删除节点
     *
     * elementId 仅用于数据排查与会话内的元素定位，不能作为业务标识使用；
     * 业务数据请按本体（或业务）定义的主键删除。
     */
    public long deleteNodeByElementId(boolean withDetach, String... ids) {
        if (null == ids || ids.length == 0) return 0;
        String cql = "MATCH (n) WHERE elementId(n) IN $ids" + (withDetach ? " DETACH" : "")
                + " DELETE n RETURN COUNT(n)";
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("ids", Arrays.asList(ids));
        try (Session session = driver.session()) {
            return Neo4jUtil.singleLong(session.run(cql, Neo4jUtil.parameters(parameters)));
        }
    }

    /**
     * 按元素标识删除关系
     *
     * elementId 仅用于数据排查与会话内的元素定位，不能作为业务标识使用；
     * 业务数据请按本体（或业务）定义的主键删除。
     */
    public long deleteRelationshipByElementId(String... ids) {
        if (null == ids || ids.length == 0) return 0;
        String cql = "MATCH ()-[r]->() WHERE elementId(r) IN $ids DELETE r RETURN COUNT(r)";
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("ids", Arrays.asList(ids));
        try (Session session = driver.session()) {
            return Neo4jUtil.singleLong(session.run(cql, Neo4jUtil.parameters(parameters)));
        }
    }

    /**
     * 创建节点
     */
    protected ObjectNode nodeCreate(ObjectNode node) {
        CypherParameter parameter = new CypherParameter();
        StringBuilder sb = new StringBuilder("CREATE (n");
        sb.append(parameter.labels(node));
        sb.append(parameter.properties(node)).append(") RETURN n");
        try (Session session = driver.session()) {
            return Neo4jUtil.singleNode(session.run(sb.toString(), parameter.parameters()));
        }
    }

    /**
     * 删除节点
     */
    protected long nodeDelete(ObjectNode node, boolean withDetach) {
        CypherParameter parameter = new CypherParameter();
        StringBuilder sb = new StringBuilder("MATCH (n");
        sb.append(parameter.labels(node));
        sb.append(parameter.properties(node)).append(")");
        if (withDetach) sb.append(" DETACH");
        sb.append(" DELETE n RETURN COUNT(n)");
        try (Session session = driver.session()) {
            return Neo4jUtil.singleLong(session.run(sb.toString(), parameter.parameters()));
        }
    }

    /**
     * 根据查询条件创建关系
     */
    protected ObjectNode relationshipCreate(ObjectNode a, ObjectNode r, ObjectNode b) {
        CypherParameter parameter = new CypherParameter();
        StringBuilder sb = new StringBuilder("MATCH (a");
        sb.append(parameter.labels(a)).append("), (b").append(parameter.labels(b)).append(")");
        String wa = parameter.where("a", a);
        String wb = parameter.where("b", b);
        if (!DPUtil.empty(wa) || !DPUtil.empty(wa)) {
            sb.append(" WHERE ").append(wa);
            if (!DPUtil.empty(wa)) sb.append(" AND ");
            sb.append(wb);
        }
        sb.append(" CREATE (a)-[r").append(parameter.type(r)).append(parameter.properties(r)).append("]->(b)");
        sb.append(" RETURN r");
        try (Session session = driver.session()) {
            return Neo4jUtil.singleRelationship(session.run(sb.toString(), parameter.parameters()));
        }
    }

    /**
     * 根据两端元素的 elementId 创建关系
     *
     * elementId 仅用于数据排查与会话内的元素定位，业务数据请按主键匹配两端节点。
     */
    protected ObjectNode relationshipCreate(ObjectNode r) {
        CypherParameter parameter = new CypherParameter();
        StringBuilder sb = new StringBuilder("MATCH (a), (b)");
        sb.append(" WHERE elementId(a) = ").append(parameter.variable(r.at("/" + Neo4jUtil.FIELD_START_ELEMENT_ID).asText("")));
        sb.append(" AND elementId(b) = ").append(parameter.variable(r.at("/" + Neo4jUtil.FIELD_END_ELEMENT_ID).asText("")));
        sb.append(" CREATE (a)-[r").append(parameter.type(r)).append(parameter.properties(r)).append("]->(b)");
        sb.append(" RETURN r");
        try (Session session = driver.session()) {
            return Neo4jUtil.singleRelationship(session.run(sb.toString(), parameter.parameters()));
        }
    }

    /**
     * 根据类型和属性删除关系
     */
    protected long relationshipDelete(ObjectNode r) {
        CypherParameter parameter = new CypherParameter();
        StringBuilder sb = new StringBuilder("MATCH ()-[r");
        sb.append(parameter.type(r));
        sb.append(parameter.properties(r)).append("]->()").append(" DELETE r RETURN COUNT(r)");
        try (Session session = driver.session()) {
            return Neo4jUtil.singleLong(session.run(sb.toString(), parameter.parameters()));
        }
    }

    /**
     * 批量创建节点和关系
     * {
     *     "a": Node,
     *     "b": Node,
     *     "r": { // Relationship
     *         start: "a", // 同一语句内的局部变量名，不是 elementId
     *         end: "b"
     *     }
     * }
     */
    protected ObjectNode batchCreate(ObjectNode items) {
        CypherParameter parameter = new CypherParameter();
        StringBuilder sb = new StringBuilder("CREATE ");
        List<String> keys = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> iterator = items.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            String key = entry.getKey();
            Neo4jUtil.assertSafe(key);
            keys.add(key);
            JsonNode item = entry.getValue();
            if (item.has(Neo4jUtil.FIELD_TYPE)) {
                sb.append("(").append(item.at("/" + FIELD_VARIABLE_START).asText()).append(")").append("-[");
                sb.append(key).append(parameter.type(item)).append(parameter.properties(item));
                sb.append("]->(").append(item.at("/" + FIELD_VARIABLE_END).asText()).append(")");
            } else {
                sb.append("(").append(key).append(parameter.labels(item));
                sb.append(parameter.properties(item)).append(")");
            }
            if (iterator.hasNext()) sb.append(", ");
        }
        sb.append(" RETURN ").append(DPUtil.implode(", ", keys.toArray(new String[0])));
        try (Session session = driver.session()) {
            Result result = session.run(sb.toString(), parameter.parameters());
            return Neo4jUtil.record2json(result.single());
        }
    }

    public void run(String query, Value parameters) {
        if (null == parameters) parameters = Values.EmptyMap;
        try (Session session = driver.session()) {
            session.run(query, parameters);
        }
    }

}
