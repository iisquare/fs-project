package com.iisquare.fs.base.neo4j.mvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.neo4j.core.CypherParameter;
import com.iisquare.fs.base.neo4j.util.Neo4jUtil;
import org.neo4j.driver.Session;

public abstract class Neo4jRelationshipBase extends Neo4jBase {

    protected String TYPE_NAME = getClass().getSimpleName();

    public String typeName() {
        return TYPE_NAME;
    }

    public void index(String property) {
        String cql = "CREATE INDEX rel_index_%s_%s IF NOT EXISTS FOR ()-[r:%s]-() ON (r.%s)";
        run(String.format(cql, DPUtil.addUnderscores(TYPE_NAME), property, TYPE_NAME, property), null);
    }

    public ObjectNode save(JsonNode a, JsonNode r, JsonNode b) {
        CypherParameter parameter = new CypherParameter();
        StringBuilder sb = new StringBuilder("MATCH (a");
        sb.append(parameter.labels(a)).append(parameter.properties(a)).append("), (b");
        sb.append(parameter.labels(b)).append(parameter.properties(b)).append(") MERGE (a)-[r:");
        sb.append(TYPE_NAME).append(parameter.properties(r)).append("]->(b) RETURN r");
        try (Session session = driver.session()) {
            return Neo4jUtil.singleRelationship(session.run(sb.toString(), parameter.parameters()));
        }
    }

    public ObjectNode save(JsonNode arb) {
        return save(arb.at("/a"), arb.at("/r"), arb.at("/b"));
    }

    public long delete(JsonNode properties) {
        ObjectNode r = DPUtil.objectNode();
        r.put(Neo4jUtil.FIELD_TYPE, TYPE_NAME);
        r.replace(Neo4jUtil.FIELD_PROPERTIES, properties);
        return relationshipDelete(r);
    }

}
