package com.iisquare.fs.web.kg.service;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.neo4j.core.CQLBuilder;
import com.iisquare.fs.base.neo4j.util.Neo4jUtil;
import org.neo4j.driver.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Map;

@Service
public class Neo4jService {

    @Autowired
    protected Driver driver;

    public Map<String, Object> showIndex(Map<String, Object> param) {
        return showSchema("INDEXES", param);
    }

    public Map<String, Object> createIndex(Map<String, Object> param) {
        String indexType = DPUtil.parseString(param.get("indexType"));
        if (!Arrays.asList("", "RANGE", "TEXT", "POINT", "LOOKUP").contains(indexType)) {
            return ApiUtil.result(1001, "索引类型不合法", indexType);
        }
        String cql = "CREATE " + indexType + " INDEX ";
        String name = DPUtil.parseString(param.get("name"));
        if (!DPUtil.isMatcher("^[a-z_]+$", name)) {
            return ApiUtil.result(1002, "索引名称不合法", name);
        }
        cql += "`" + name + "`";
        if ("LOOKUP".equals(indexType)) {
            if ("NODE".equals(param.get("ontologyType"))) {
                cql += " FOR (n) ON EACH labels(n)";
            } else {
                cql += " FOR ()-[r]-() ON EACH type(r)";
            }
        } else {
            String label = DPUtil.parseString(param.get("label"));
            if (!DPUtil.isMatcher("^[a-zA-Z]+$", label)) {
                return ApiUtil.result(1003, "索引标签不合法", label);
            }
            if ("NODE".equals(param.get("ontologyType"))) {
                cql += " FOR (nor:`" + label + "`) ON (";
            } else {
                cql += " FOR ()-[nor:`" + label + "`]-() ON (";
            }
            String[] fields = DPUtil.parseStringList(param.get("fields")).toArray(new String[0]);
            if (fields.length == 0) {
                return ApiUtil.result(1004, "字段名称不能为空", fields);
            }
            for (int i = 0; i < fields.length; i++) {
                if (!DPUtil.isMatcher("^[a-zA-Z0-9_]+$", fields[i])) {
                    return ApiUtil.result(1005, "字段名称不合法", fields[i]);
                }
                fields[i] = "nor.`" + fields[i] + "`";
            }
            cql += DPUtil.implode(", ", fields) + ")";
        }
        try (Session session = driver.session()) {
            session.run(cql);
            return ApiUtil.result(0, null, cql);
        } catch (Exception e) {
            return ApiUtil.result(500, e.getMessage(), cql);
        }
    }

    public Map<String, Object> dropIndex(Map<String, Object> param) {
        return dropSchema("INDEX", param);
    }

    public Map<String, Object> showConstraint(Map<String, Object> param) {
        return showSchema("CONSTRAINTS", param);
    }

    public Map<String, Object> createConstraint(Map<String, Object> param) {
        String constraintType = DPUtil.parseString(param.get("constraintType"));
        if (DPUtil.empty(constraintType)) constraintType = "UNIQUE";
        String cql = "CREATE CONSTRAINT ";
        String name = DPUtil.parseString(param.get("name"));
        if (!DPUtil.isMatcher("^[a-z_]+$", name)) {
            return ApiUtil.result(1002, "约束名称不合法", name);
        }
        cql += "`" + name + "`";
        String label = DPUtil.parseString(param.get("label"));
        if (!DPUtil.isMatcher("^[a-zA-Z]+$", label)) {
            return ApiUtil.result(1003, "索引标签不合法", label);
        }
        if ("NODE".equals(param.get("ontologyType"))) {
            cql += " FOR (nor:`" + label + "`) REQUIRE (";
        } else {
            cql += " FOR ()-[nor:`" + label + "`]-() REQUIRE (";
        }
        String[] fields = DPUtil.parseStringList(param.get("fields")).toArray(new String[0]);
        if (fields.length == 0) {
            return ApiUtil.result(1004, "字段名称不能为空", fields);
        }
        for (int i = 0; i < fields.length; i++) {
            if (!DPUtil.isMatcher("^[a-zA-Z0-9_]+$", fields[i])) {
                return ApiUtil.result(1005, "字段名称不合法", fields[i]);
            }
            fields[i] = "nor.`" + fields[i] + "`";
        }
        cql += DPUtil.implode(", ", fields) + ")";
        switch (constraintType) {
            case "UNIQUE":
                cql += " IS UNIQUE";
                break;
            default:
                return ApiUtil.result(1001, "约束类型不合法", constraintType);
        }
        try (Session session = driver.session()) {
            session.run(cql);
            return ApiUtil.result(0, null, cql);
        } catch (Exception e) {
            return ApiUtil.result(500, e.getMessage(), cql);
        }
    }

    public Map<String, Object> dropConstraint(Map<String, Object> param) {
        return dropSchema("CONSTRAINT", param);
    }

    public Map<String, Object> dropSchema(String ic, Map<String, Object> param) {
        String name = DPUtil.parseString(param.get("name"));
        if (!DPUtil.isMatcher("^[a-z_]+$", name)) {
            return ApiUtil.result(1002, "名称不合法", name);
        }
        String cql = "DROP " + ic + " `" + name + "` IF EXISTS";
        try (Session session = driver.session()) {
            session.run(cql);
            return ApiUtil.result(0, null, cql);
        } catch (Exception e) {
            return ApiUtil.result(500, e.getMessage(), cql);
        }
    }

    public Map<String, Object> showSchema(String ic, Map<String, Object> param) {
        CQLBuilder builder = new CQLBuilder();
        String name = DPUtil.parseString(param.get("name"));
        if (!DPUtil.empty(name)) {
            builder.expression("name = $name").param("name", name);
        }
        String type = DPUtil.parseString(param.get("type"));
        if (!DPUtil.empty(type)) {
            builder.expression("(type = $type or entityType = $type)").param("type", type);
        }
        String label = DPUtil.parseString(param.get("label"));
        if (!DPUtil.empty(label)) {
            builder.expression("ANY(label in labelsOrTypes WHERE label = $label)").param("label", label);
        }
        String cql = "SHOW " + ic + " YIELD *" + builder.where();
        try (Session session = driver.session()) {
            ArrayNode result = Neo4jUtil.result2json(session.run(cql, builder.parameters()));
            return ApiUtil.result(0, null, result);
        } catch (Exception e) {
            return ApiUtil.result(500, e.getMessage(), cql);
        }
    }

}
