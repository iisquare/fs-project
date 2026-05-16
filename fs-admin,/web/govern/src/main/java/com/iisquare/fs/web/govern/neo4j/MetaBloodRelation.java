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

@Component
public class MetaBloodRelation extends Neo4jRelationshipBase {

    @Autowired
    private MetaBloodNode metaBloodNode;

    public MetaBloodRelation() {
        this.TYPE_NAME = "GovernBlood";
    }

    public ObjectNode formatModelRelation(JsonNode db, Long time) {
        ObjectNode result = DPUtil.objectNode();
        ObjectNode a = result.putObject("a");
        ObjectNode r = result.putObject("r");
        ObjectNode b = result.putObject("b");
        String aid = db.get("source_catalog").asText() + db.get("source_model").asText();
        String bid = db.get("target_catalog").asText() + db.get("target_model").asText();

        a.putArray(Neo4jUtil.FIELD_LABELS).add(metaBloodNode.labelName());
        ObjectNode properties = a.putObject(Neo4jUtil.FIELD_PROPERTIES).put(metaBloodNode.idName(), aid);
        if (null != time) properties.put("time", time);

        b.putArray(Neo4jUtil.FIELD_LABELS).add(metaBloodNode.labelName());
        properties = b.putObject(Neo4jUtil.FIELD_PROPERTIES).put(metaBloodNode.idName(), bid);
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

    public ArrayNode blood(String catalog, String code, int minHops, int maxHops) {
        CypherParameter parameter = new CypherParameter();
        StringBuilder sb = new StringBuilder("MATCH p=(a:");
        sb.append(metaBloodNode.labelName()).append(")-[r:").append(TYPE_NAME);
        sb.append("*").append(minHops).append("..").append(maxHops).append("]->(b:");
        sb.append(metaBloodNode.labelName()).append(") WHERE ");
        if (DPUtil.empty(catalog)) catalog = "/";
        if (DPUtil.empty(code)) {
            sb.append("a.catalog=").append(parameter.variable(catalog));
            sb.append(" OR b.catalog=").append(parameter.variable(catalog));
        } else {
            String id = catalog + code;
            sb.append("a.id=").append(parameter.variable(id));
            sb.append(" OR b.id=").append(parameter.variable(id));
        }
        sb.append(" RETURN distinct p");
        try (Session session = driver.session()) {
            Result result = session.run(sb.toString(), parameter.parameters());
            return Neo4jUtil.result2json(result);
        }
    }

}
