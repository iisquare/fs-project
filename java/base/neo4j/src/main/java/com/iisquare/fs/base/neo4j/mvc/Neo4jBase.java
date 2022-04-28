package com.iisquare.fs.base.neo4j.mvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.neo4j.core.CypherParameter;
import com.iisquare.fs.base.neo4j.util.Neo4jUtil;
import org.neo4j.driver.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public abstract class Neo4jBase {

    @Autowired
    protected Driver driver;

    /**
     * 根据主键删除节点，节点与关系的主键相互独立非互斥
     * 节点：(n:Label1:Label2:Label2 { k1: v1, k2: v2 })
     */
    public long deleteNodeByIdentity(boolean withDetach, Long... ids) {
        if (ids.length == 0) return 0;
        StringBuilder sb = new StringBuilder("MATCH (n) WHERE id(n) IN [");
        sb.append(DPUtil.implode(", ", ids)).append("]");
        if (withDetach) sb.append(" DETACH");
        sb.append(" DELETE n RETURN COUNT(n)");
        try (Session session = driver.session()) {
            return Neo4jUtil.singleLong(session.run(sb.toString()));
        }
    }

    /**
     * 根据主键删除关系，节点与关系的主键相互独立非互斥
     * 关系：()-[r:Type { k1: v1, k2: v2 }]->()
     */
    public long deleteRelationshipByIdentity(Long... ids) {
        if (ids.length == 0) return 0;
        StringBuilder sb = new StringBuilder("MATCH ()-[r]->() WHERE id(r) IN [");
        sb.append(DPUtil.implode(", ", ids)).append("]");
        sb.append(" DELETE r RETURN COUNT(r)");
        try (Session session = driver.session()) {
            return Neo4jUtil.singleLong(session.run(sb.toString()));
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
     * 根据关联主键创建关系
     */
    protected ObjectNode relationshipCreate(ObjectNode r) {
        CypherParameter parameter = new CypherParameter();
        StringBuilder sb = new StringBuilder("MATCH (a), (b)");
        sb.append(" WHERE id(a)=").append(r.at("/" + Neo4jUtil.FIELD_START_NODE_ID).asLong(-1));
        sb.append(" AND id(b)=").append(r.at("/" + Neo4jUtil.FIELD_END_NODE_ID).asLong(-1));
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
     *         start: "a",
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
                sb.append("(").append(item.at("/" + Neo4jUtil.FIELD_START_NODE_ID).asText()).append(")").append("-[");
                sb.append(key).append(parameter.type(item)).append(parameter.properties(item));
                sb.append("]->(").append(item.at("/" + Neo4jUtil.FIELD_END_NODE_ID).asText()).append(")");
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
