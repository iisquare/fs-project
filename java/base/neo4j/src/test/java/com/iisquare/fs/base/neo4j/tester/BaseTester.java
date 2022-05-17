package com.iisquare.fs.base.neo4j.tester;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.neo4j.mvc.Neo4jBase;
import com.iisquare.fs.base.neo4j.util.Neo4jUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.GraphDatabase;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class BaseTester extends Neo4jBase {

    @Before
    public void open() {
        String uri = "neo4j://127.0.0.1:7687";
        String username = "neo4j";
        String password = "admin888";
        Config.ConfigBuilder builder = Config.builder();
        builder.withMaxConnectionPoolSize(30);
        builder.withConnectionTimeout(1000, TimeUnit.MILLISECONDS);
        this.driver = GraphDatabase.driver(uri, AuthTokens.basic(username, password), builder.build());
    }

    @After
    public void close() {
        if (null != driver) driver.close();
    }

    @Test
    public void nodeCreateTest() {
        ObjectNode node = DPUtil.objectNode();
        node.putArray(Neo4jUtil.FIELD_LABELS).add("Test");
        node.putObject(Neo4jUtil.FIELD_PROPERTIES).put("name", getClass().getSimpleName());
        ObjectNode result = nodeCreate(node);
        System.out.println(result);
    }

    @Test
    public void nodeDeleteTest() {
        ObjectNode node = DPUtil.objectNode();
        node.putArray(Neo4jUtil.FIELD_LABELS).add("Test");
        node.putObject(Neo4jUtil.FIELD_PROPERTIES).put("name", getClass().getSimpleName());
        long result = nodeDelete(node, true);
        System.out.println(result);
    }

    @Test
    public void deleteByIdentityTest() {
        long result = deleteNodeByIdentity(true, 0L, 1L, 2L, 3L);
        System.out.println(result);
    }

    @Test
    public void splitTest() {
        String str = "a->b";
        System.out.println(Arrays.toString(str.split("->")));
    }

    @Test
    public void batchCreateTest() {
        ObjectNode items = DPUtil.objectNode();
        ObjectNode a = items.putObject("a");
        a.putArray(Neo4jUtil.FIELD_LABELS).add("Test");
        a.putObject(Neo4jUtil.FIELD_PROPERTIES).put("name", "A");
        ObjectNode b = items.putObject("b");
        b.putArray(Neo4jUtil.FIELD_LABELS).add("Test");
        b.putObject(Neo4jUtil.FIELD_PROPERTIES).put("name", "B");
        ObjectNode r = items.putObject("r");
        r.put(Neo4jUtil.FIELD_TYPE, "Test");
        r.putObject(Neo4jUtil.FIELD_PROPERTIES).put("name", "R");
        ObjectNode result = batchCreate(items);
        System.out.println(result);
    }

}
