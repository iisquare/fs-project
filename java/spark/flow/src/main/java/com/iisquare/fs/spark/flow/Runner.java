package com.iisquare.fs.spark.flow;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.spark.util.SparkUtil;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.StructType;

import java.net.URL;
import java.util.*;

public class Runner {

    private Event event;

    public Runner(Event event) {
        this.event = event;
    }

    private boolean process(Map<String, Node> nodeMap) throws Exception {
        // 查找入度为零的全部节点
        List<Node> list = new ArrayList<>();
        for (Map.Entry<String, Node> entry : nodeMap.entrySet()) {
            Node node = entry.getValue();
            if(node.isReady()) continue;
            Set<Node> set = node.getSource();
            boolean sourceReady = true;
            for (Node source : set) {
                if(source.isDisabled() || source.isReady()) continue;
                sourceReady = false;
                break;
            }
            if(sourceReady) {
                list.add(node);
            }
        }
        // 执行任务
        for (Node node : list) {
            event.onNodeStart(node);
            Object result = node.execute();
            event.onNodeEnd(node, result);
            if(null != result) return false;
            node.setReady(true);
        }
        return !list.isEmpty();
    }

    public void execute(String appName, JsonNode flow) throws Exception {
        SparkSession session = SparkSession.builder().appName(appName).getOrCreate();
        // 配置环境参数
        String type = flow.get("type").asText();
        int parallelism = session.sparkContext().defaultParallelism();
        // 解析节点
        Iterator<JsonNode> it = flow.get("nodes").elements();
        Map<String, Node> nodes = new LinkedHashMap<>();
        while (it.hasNext()) {
            JsonNode item = it.next();
            StructType schema = new StructType();
            Map<String, String> reflect = new LinkedHashMap<>();
            Iterator<JsonNode> iterator = item.get("returns").elements();
            while (iterator.hasNext()) {
                JsonNode ret = iterator.next();
                String field = ret.get("field").asText();
                schema.add(field, SparkUtil.java2spark(ret.get("classname").asText()));
                reflect.put(field, ret.has("from") ? ret.get("from").asText() : field);
            }
            List<URL> urls = new ArrayList<>();
            Iterator<JsonNode> itplg = flow.get("plugins").get(item.get("plugin").asText()).elements();
            while (itplg.hasNext()) {
                urls.add(new URL(itplg.next().asText()));
            }
            Node node;
            if(urls.size() > 0) {
                ClassLoader classLoader = new ClassLoader(urls.toArray(new URL[urls.size()]), getClass().getClassLoader());
                node = (Node) classLoader.loadClass(item.get("classname").asText()).newInstance();
            } else {
                node = (Node) Class.forName(item.get("classname").asText()).newInstance();
            }
            String id = item.get("id").asText();
            int parallel = DPUtil.parseInt(item.at("/parallelism").asText(String.valueOf(parallelism)));;
            node.setSession(session);
            node.setType(type);
            node.setId(id);
            node.setProperties(DPUtil.stringify(item));
            node.setFlow(flow);
            node.setParallelism(parallel > 0 ? parallel : parallelism);
            node.setSchema(schema);
            node.setReflect(reflect);
            node.setDisabled(item.at("/disabled").asBoolean(false));
            nodes.put(id, node);
        }
        // 解析连线
        it = flow.get("connections").elements();
        while (it.hasNext()) {
            JsonNode item = it.next();
            Node source = nodes.get(item.get("sourceId").asText());
            Node target = nodes.get(item.get("targetId").asText());
            source.getTarget().add(target);
            target.getSource().add(source);
        }
        // 查找入度为零的节点并执行
        while(process(nodes)) {}
        session.close();
    }

}
