package com.iisquare.fs.app.nlp.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.app.nlp.bean.WordTitleNode;
import com.iisquare.fs.app.nlp.helper.JDBCHelper;
import com.iisquare.fs.app.nlp.helper.Neo4jHelper;
import com.iisquare.fs.app.nlp.util.ConfigUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;

import java.util.*;

public class WordPrettyJob {
    public static void main(String[] args) throws Exception {
        JDBCHelper jdbc = new JDBCHelper(ConfigUtil.mysql());
        Neo4jHelper neo4j = new Neo4jHelper(ConfigUtil.neo4j());
        neo4j.run("MATCH (n:Word) DETACH DELETE n RETURN COUNT(n);");
        neo4j.run("CREATE CONSTRAINT uni_word IF NOT EXISTS FOR (n:Word) REQUIRE n.name IS UNIQUE;");
        ObjectNode acc = DPUtil.array2object(jdbc.query("select * from fs_word_acc"), "name");
        ObjectNode gram = gram(jdbc.query("select * from fs_word_gram"));
        ArrayNode level = jdbc.query("select * from fs_word_level");
        for (JsonNode n : acc) {
            neo4j.run("CREATE (n:Word { name: $name, score: $score }) RETURN n;", new LinkedHashMap<String, Object>(){{
                put("name", n.at("/name").asText());
                put("score", n.at("/score").asDouble());
            }});
        }
        for (JsonNode n : level) {
            WordTitleNode node = DPUtil.toJSON(DPUtil.parseJSON(n.at("/level").asText()), WordTitleNode.class);
            edge(neo4j, acc, gram, n.at("/base_id").asText(), "", node);
        }
        FileUtil.close(jdbc, neo4j);
    }

    public static boolean edge(Neo4jHelper neo4j, ObjectNode acc, ObjectNode gram, String id, String parent, WordTitleNode node) {
        parent += "#" + node.text;
        Set<String> sp = new HashSet<>(DPUtil.toJSON(gram.withArray(parent), List.class));
        if (sp.size() == 0) return false;
        for (WordTitleNode child : node.children) {
            edge(neo4j, acc, gram, id, parent, child);
            String level = parent + "#" + child.text;
            for (JsonNode nc : gram.withArray(level)) {
                String c = nc.asText();
                if (!acc.has(c)) continue;
                for (String p : sp) {
                    if (p.equals(c)) continue;
                    neo4j.run("MATCH (c:Word), (p:Word) WHERE c.name=$c and p.name=$p CREATE (c)-[r:Policy { id: $id, level: $level }]->(p) RETURN r;", new LinkedHashMap<String, Object>(){{
                        put("c", c);
                        put("p", p);
                        put("id", id);
                        put("level", level);
                    }});
                }
            }
        }
        return true;
    }

    public static ObjectNode gram(JsonNode array) {
        ObjectNode nodes = DPUtil.objectNode();
        Iterator<JsonNode> iterator = array.iterator();
        while (iterator.hasNext()) {
            JsonNode node = iterator.next();
            String key = node.at("/level").asText("");
            nodes.withArray(key).add(node.at("/name").asText());
        }
        return nodes;
    }
}
