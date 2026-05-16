package com.iisquare.fs.base.neo4j;

import com.iisquare.fs.base.neo4j.util.Neo4jUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.driver.*;

import java.util.concurrent.TimeUnit;

public class CypherTests {

    private Driver driver;

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
    public void multiLabelTest() {
        // 标签不可作为参数变量
        try (Session session = driver.session()) {
            Result result = session.run("CREATE (a:Person:Phd {name: $name, title: $title}) return a",
                    Values.parameters("name", "Arthur001", "title", "King001"));
            System.out.println(Neo4jUtil.node2json(result.single().get(0).asNode()));
        }
    }

    @Test
    public void createTest() {
        // 关系不支持多个标签
        try (Session session = driver.session()) {
            Result result = session.run("CREATE (a0:Person {name: $name0}), (a1:Person {name: $name1})" +
                            ", (a0)-[r:Teach {name: $name2}]->(a1) return a0, a1, r",
                    Values.parameters("name0", "A", "name1", "B", "name2", "R"));
            Neo4jUtil.print(result);
        }
    }

}
