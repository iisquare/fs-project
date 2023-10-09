package com.iisquare.fs.app.nlp.job;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.app.nlp.helper.JDBCHelper;
import com.iisquare.fs.app.nlp.helper.Neo4jHelper;
import com.iisquare.fs.app.nlp.util.ConfigUtil;
import com.iisquare.fs.app.nlp.util.WordUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

public class PolicyPhraseGraphJob {
    public static void main(String[] args) throws Exception {
        JDBCHelper jdbc = new JDBCHelper(ConfigUtil.mysql());
        Neo4jHelper neo4j = new Neo4jHelper(ConfigUtil.neo4j());
        neo4j.run("MATCH (n:PolicyPhrase) DETACH DELETE n RETURN COUNT(n);");
        neo4j.run("CREATE CONSTRAINT uni_policy_phrase IF NOT EXISTS FOR (n:PolicyPhrase) REQUIRE n.name IS UNIQUE;");
        Statement statement = jdbc.connection().createStatement();
        statement.setFetchSize(100);
        ResultSet rs = statement.executeQuery("select * from fs_policy_phrase order by base_id asc");
        Map<String, Integer> deps = new LinkedHashMap<>(); // 依赖计数
        while (rs.next()) {
            String title = rs.getString(2);
            String[] keywords = DPUtil.explode(",", rs.getString(3));
            String[] phrases = DPUtil.explode(",", rs.getString(4));
            node(neo4j, title, keywords, phrases, deps);
        }
        ArrayNode edge = edge(neo4j, deps);
        jdbc.update("truncate table fs_policy_edge");
        jdbc.batch("fs_policy_edge", edge, 200);
        FileUtil.close(rs, statement, jdbc, neo4j);
    }

    public static boolean node(Neo4jHelper neo4j, String title, String[] keywords, String[] phrases, Map<String, Integer> deps) {
        HashSet<String> words = new HashSet<>();
        words.addAll(Arrays.asList(keywords));
        words.addAll(Arrays.asList(phrases));
        List<String> ts = new ArrayList<>();
        for (String word : words) {
            if (title.contains(word)) {
                ts.add(word);
            }
        }
        ts = WordUtil.combine(ts);
        words.clear();
        words.addAll(ts);
        words.addAll(Arrays.asList(phrases));
        for (String word : words) {
            neo4j.run("MERGE (n:PolicyPhrase { name: $name }) RETURN n;", new LinkedHashMap<String, Object>(){{
                put("name", word);
            }});
        }
        for (String p : ts) {
            for (String c : phrases) {
                if (p.equals(c)) continue;
                String rel = String.format("%s->%s", c, p);
                deps.put(rel, deps.getOrDefault(rel, 0) + 1);
            }
        }
        return true;
    }

    public static ArrayNode edge(Neo4jHelper neo4j, Map<String, Integer> deps) {
        ArrayNode result = DPUtil.arrayNode();
        for (Map.Entry<String, Integer> entry : deps.entrySet()) {
            String key = entry.getKey();
            String[] keys = key.split("->");
            LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>() {{
                put("cname", keys[0]);
                put("pname", keys[1]);
                put("dep_count", entry.getValue());
            }};
            ObjectNode node = DPUtil.toJSON(map, ObjectNode.class);
            node.put("id", key);
            result.add(node);
            neo4j.run("MATCH (c:PolicyPhrase), (p:PolicyPhrase) WHERE c.name=$cname and p.name=$pname CREATE (c)-[r:PolicyTC { dep_count: $dep_count }]->(p) RETURN r;", map);
        }
        return result;
    }

}
