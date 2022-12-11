package com.iisquare.fs.base.neo4j.mvc;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.neo4j.core.CypherParameter;
import com.iisquare.fs.base.neo4j.util.Neo4jUtil;
import org.neo4j.driver.Session;

public abstract class Neo4jNodeBase extends Neo4jBase {

    protected String ID_NAME = "id";
    protected String LABEL_NAME = getClass().getSimpleName();

    public String idName() {
        return ID_NAME;
    }

    public String labelName() {
        return LABEL_NAME;
    }

    public void index(String property) {
        String cql = "CREATE INDEX node_index_%s_%s IF NOT EXISTS FOR (n:%s) ON (n.%s)";
        run(String.format(cql, DPUtil.addUnderscores(LABEL_NAME), property, LABEL_NAME, property), null);
    }

    public void unique() {
        String cql = "CREATE CONSTRAINT constraint_%s IF NOT EXISTS FOR (n:%s) REQUIRE n.%s IS UNIQUE";
        run(String.format(cql, DPUtil.addUnderscores(LABEL_NAME), LABEL_NAME, ID_NAME), null);
    }

    public String id(ObjectNode properties) {
        return properties.at("/" + ID_NAME).asText("");
    }

    public ObjectNode save(ObjectNode properties, ObjectNode replace) {
        CypherParameter parameter = new CypherParameter();
        StringBuilder sb = new StringBuilder("MERGE (n:");
        sb.append(LABEL_NAME).append(" {").append(ID_NAME).append(":").append(parameter.variable(id(properties)));
        properties.remove(ID_NAME);
        sb.append("}) ON CREATE SET ").append(parameter.extract("n", properties, ", "));
        if (null != replace) {
            replace.remove(ID_NAME);
            sb.append(" ON MATCH SET ").append(parameter.extract("n", replace, ", "));
        }
        sb.append(" RETURN n");
        try (Session session = driver.session()) {
            return Neo4jUtil.singleNode(session.run(sb.toString(), parameter.parameters()));
        }
    }

    public ObjectNode save(ObjectNode properties) {
        return save(properties, properties);
    }

    public long delete(ObjectNode properties, boolean withDetach) {
        ObjectNode node = DPUtil.objectNode();
        node.putArray(Neo4jUtil.FIELD_LABELS).add(LABEL_NAME);
        node.replace(Neo4jUtil.FIELD_PROPERTIES, properties);
        return nodeDelete(node, withDetach);
    }

    public long deleteById(boolean withDetach, String... ids) {
        if (ids.length == 0) return 0;
        CypherParameter parameter = new CypherParameter();
        StringBuilder sb = new StringBuilder("MATCH (n) WHERE n.");
        sb.append(ID_NAME).append(" IN [").append(parameter.in(ids)).append("]");
        if (withDetach) sb.append(" DETACH");
        sb.append(" DELETE n RETURN COUNT(n)");
        try (Session session = driver.session()) {
            return Neo4jUtil.singleLong(session.run(sb.toString()));
        }
    }

    public long deleteAll() {
        String cql = String.format("MATCH (n:%s) DETACH DELETE n RETURN COUNT(n)", LABEL_NAME);
        try (Session session = driver.session()) {
            return Neo4jUtil.singleLong(session.run(cql));
        }
    }

}
