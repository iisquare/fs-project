package com.iisquare.fs.base.neo4j.core;

import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.neo4j.util.Neo4jUtil;
import org.neo4j.driver.Value;

import java.util.*;

public class CQLBuilder {

    private List<String> where = new ArrayList<>();
    private Map<String, Object> params = new LinkedHashMap<>();

    public CQLBuilder() {

    }

    public CQLBuilder expression(String where) {
        this.where.add(where);
        return this;
    }

    public CQLBuilder param(String key, Object value) {
        params.put(key, value);
        return this;
    }

    public String where() {
        if (where.isEmpty()) {
            return "";
        } else {
            return " where " + DPUtil.implode(" AND ", where);
        }
    }

    public Value parameters() {
        return Neo4jUtil.parameters(params);
    }

    public CQLBuilder reset() {
        where = new ArrayList<>();
        params = new LinkedHashMap<>();
        return this;
    }

}
