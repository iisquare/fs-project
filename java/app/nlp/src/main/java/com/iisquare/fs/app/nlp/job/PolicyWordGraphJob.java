package com.iisquare.fs.app.nlp.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.app.nlp.helper.JDBCHelper;
import com.iisquare.fs.app.nlp.helper.Neo4jHelper;
import com.iisquare.fs.app.nlp.util.ConfigUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

public class PolicyWordGraphJob {
    public static void main(String[] args) throws Exception {
        JDBCHelper jdbc = new JDBCHelper(ConfigUtil.mysql());
        Neo4jHelper neo4j = new Neo4jHelper(ConfigUtil.neo4j());
        neo4j.run("MATCH (n:PolicyWord) DETACH DELETE n RETURN COUNT(n);");
        neo4j.run("CREATE CONSTRAINT uni_policy_word IF NOT EXISTS FOR (n:PolicyWord) REQUIRE n.name IS UNIQUE;");
        neo4j.run("CREATE INDEX idx_policy_sdp_doc_count IF NOT EXISTS FOR ()-[r:PolicySDP]-() ON (r.doc_count);");
        neo4j.run("CREATE INDEX idx_policy_sdp_dep_count IF NOT EXISTS FOR ()-[r:PolicySDP]-() ON (r.dep_count);");
        Statement statement = jdbc.connection().createStatement();
        statement.setFetchSize(100);
        ResultSet rs = statement.executeQuery("select base_id, sdp from fs_policy_sdp order by base_id asc");
        Map<String, Integer> deps = new LinkedHashMap<>(); // 依存计数
        Map<String, Set<String>> docs = new LinkedHashMap<>(); // 文档计数
        while (rs.next()) {
            String baseId = rs.getString(1);
            JsonNode node = DPUtil.parseJSON(rs.getString(2));
            node(neo4j, baseId, node, deps, docs);
        }
        ArrayNode edge = edge(neo4j, deps, docs);
        jdbc.update("truncate table fs_policy_edge");
        jdbc.batch("fs_policy_edge", edge, 200);
        FileUtil.close(rs, statement, jdbc, neo4j);
    }

    public static boolean bGenerate(String name, String tag) {
        List<String> stops = Arrays.asList(
                "e", // 叹词（啊）
                "o", // 拟声词（哈哈）
                "u", // 助词（的）
                "w", // 标点符号
                "m", // 数词
                "c", // 连词
                "q", // 量词
                "t", // 时间词
                "nr", // 人名
                "ns", // 地名
                "nt" // 机构团体
        );
        if (stops.contains(tag)) return false;
        if (name.contains(" ")) return false;
        if (name.contains("#")) return false;
        if (name.contains("&")) return false;
        return true;
    }

    public static boolean node(Neo4jHelper neo4j, String baseId, JsonNode node, Map<String, Integer> deps, Map<String, Set<String>> docs) {
        String name = node.at("/name").asText();
        String tag = node.at("/tag").asText();
        boolean bGenerate = bGenerate(name, tag);
        if (bGenerate) {
            neo4j.run("MERGE (n:PolicyWord { name: $name }) ON CREATE SET n.tag=$tag ON MATCH SET n.tag=$tag RETURN n;", new LinkedHashMap<String, Object>(){{
                put("name", name);
                put("tag", tag);
            }});
        }
        for (JsonNode children : node.withArray("children")) {
            boolean r = node(neo4j, baseId, children, deps, docs);
            if (bGenerate && r) {
                String cname = children.at("/name").asText();
                if (cname.equals(name)) continue; // 排除自依赖
                String ctag = children.at("/tag").asText();
                if (ctag.equals(tag)) continue; // 排除同性词关联
                String rel = String.format("%s->%s", cname, name);
                deps.put(rel, deps.getOrDefault(rel, 0) + 1);
                docs.computeIfAbsent(rel, k -> new HashSet<>()).add(baseId);
            }
        }
        return bGenerate;
    }

    public static ArrayNode edge(Neo4jHelper neo4j, Map<String, Integer> deps, Map<String, Set<String>> docs) {
        ArrayNode result = DPUtil.arrayNode();
        for (Map.Entry<String, Integer> entry : deps.entrySet()) {
            String key = entry.getKey();
            String[] keys = key.split("->");
            LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>() {{
                put("cname", keys[0]);
                put("pname", keys[1]);
                put("dep_count", entry.getValue());
                put("doc_count", docs.get(key).size());
            }};
            ObjectNode node = DPUtil.toJSON(map, ObjectNode.class);
            node.put("id", key);
            result.add(node);
            neo4j.run("MATCH (c:PolicyWord), (p:PolicyWord) WHERE c.name=$cname and p.name=$pname CREATE (c)-[r:PolicySDP { doc_count: $doc_count, dep_count: $dep_count }]->(p) RETURN r;", map);
        }
        return result;
    }

}
