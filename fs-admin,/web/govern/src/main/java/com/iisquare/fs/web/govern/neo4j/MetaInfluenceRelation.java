package com.iisquare.fs.web.govern.neo4j;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.neo4j.core.CypherParameter;
import com.iisquare.fs.base.neo4j.mvc.Neo4jRelationshipBase;
import com.iisquare.fs.base.neo4j.util.Neo4jUtil;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MetaInfluenceRelation extends Neo4jRelationshipBase {

    @Autowired
    private MetaInfluenceNode metaInfluenceNode;

    public MetaInfluenceRelation() {
        this.TYPE_NAME = "GovernInfluence";
    }

    public ObjectNode formatColumnRelation(JsonNode db, Long time) {
        ObjectNode result = DPUtil.objectNode();
        ObjectNode a = result.putObject("a");
        ObjectNode r = result.putObject("r");
        ObjectNode b = result.putObject("b");
        String aid = db.get("source_catalog").asText() + db.get("source_model").asText() + "/" + db.get("source_column").asText();
        String bid = db.get("target_catalog").asText() + db.get("target_model").asText() + "/" + db.get("target_column").asText();

        a.putArray(Neo4jUtil.FIELD_LABELS).add(metaInfluenceNode.labelName());
        ObjectNode properties = a.putObject(Neo4jUtil.FIELD_PROPERTIES).put(metaInfluenceNode.idName(), aid);
        if (null != time) properties.put("time", time);

        b.putArray(Neo4jUtil.FIELD_LABELS).add(metaInfluenceNode.labelName());
        properties = b.putObject(Neo4jUtil.FIELD_PROPERTIES).put(metaInfluenceNode.idName(), bid);
        if (null != time) properties.put("time", time);

        properties = r.putObject(Neo4jUtil.FIELD_PROPERTIES);
        properties.replace("source_catalog", db.get("source_catalog"));
        properties.replace("source_model", db.get("source_model"));
        properties.replace("source_column", db.get("source_column"));
        properties.replace("relation", db.get("relation"));
        properties.replace("target_catalog", db.get("target_catalog"));
        properties.replace("target_model", db.get("target_model"));
        properties.replace("target_column", db.get("target_column"));
        if (null != time) properties.put("time", time);
        return result;
    }

    public long deleteWithTime(Long time) {
        String cql = String.format("MATCH ()-[r:%s]->() WHERE r.time <> $time DELETE r RETURN COUNT(r)", TYPE_NAME);
        try (Session session = driver.session()) {
            Result result = session.run(cql, Values.parameters("time", time));
            return result.single().get(0).asLong();
        }
    }

    public ArrayNode influence(String catalog, String model, String code, int minHops, int maxHops) {
        CypherParameter parameter = new CypherParameter();
        StringBuilder sb = new StringBuilder("MATCH p=(a:");
        sb.append(metaInfluenceNode.labelName()).append(")-[r:").append(TYPE_NAME);
        sb.append("*").append(minHops).append("..").append(maxHops).append("]->(b:");
        sb.append(metaInfluenceNode.labelName()).append(") WHERE ");
        if (DPUtil.empty(catalog)) catalog = "/";
        List<String> a = new ArrayList<>();
        List<String> b = new ArrayList<>();
        a.add(String.format("a.catalog=%s", parameter.variable(catalog)));
        b.add(String.format("b.catalog=%s", parameter.variable(catalog)));
        if (!DPUtil.empty(model)) {
            a.add(String.format("a.model=%s", parameter.variable(model)));
            b.add(String.format("b.model=%s", parameter.variable(model)));
        }
        if (!DPUtil.empty(code)) {
            a.add(String.format("a.code=%s", parameter.variable(code)));
            b.add(String.format("b.code=%s", parameter.variable(code)));
        }
        sb.append(String.format("(%s) OR (%s)", DPUtil.implode(" AND ", a), DPUtil.implode(" AND ", b)));
        sb.append(" RETURN distinct p");
        try (Session session = driver.session()) {
            Result result = session.run(sb.toString(), parameter.parameters());
            return Neo4jUtil.result2json(result);
        }
    }

}
