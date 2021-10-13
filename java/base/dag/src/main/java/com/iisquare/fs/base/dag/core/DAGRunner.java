package com.iisquare.fs.base.dag.core;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;

public abstract class DAGRunner {

    protected JsonNode diagram;
    protected Map<String, Class<DAGNode>> nodes;
    public static final int MAX_ROUTE_SIZE = 100;

    public DAGRunner(JsonNode diagram, Map<String, Class<DAGNode>> nodes) {
        this.diagram = diagram;
        this.nodes = nodes;
    }

    /**
     * 将配置转换为DAGNode对象
     */
    public Map<String, DAGNode> items(JsonNode items) throws Exception {
        Map<String, DAGNode> result = new LinkedHashMap<>();
        Iterator<JsonNode> iterator = items.iterator();
        while (iterator.hasNext()) {
            JsonNode item = iterator.next();
            String type = item.at("/type").asText();
            DAGNode node = this.nodes.get(type).newInstance();
            node.setId(item.at("/id").asText());
            if (node instanceof DAGSource) {
                ((DAGSource) node).setAlias(item.at("/alias").asText());
                ((DAGSource) node).setKvConfigPrefix(item.at("/kvConfigPrefix").asText());
            } else if (node instanceof DAGTransform) {
                ((DAGTransform) node).setAlias(item.at("/alias").asText());
                ((DAGTransform) node).setKvConfigPrefix(item.at("/kvConfigPrefix").asText());
            } else if (node instanceof DAGSink) {
                ((DAGSink) node).setKvConfigPrefix(item.at("/kvConfigPrefix").asText());
            }
            node.setOptions(item.at("/options"));
            node.setRunner(this);
            result.put(node.getId(), node);
        }
        return result;
    }

    /**
     * 建立DAGNode之间的关联关系
     */
    public Map<String, DAGNode> relations(Map<String, DAGNode> items, JsonNode relations) throws Exception {
        Iterator<JsonNode> iterator = relations.iterator();
        while (iterator.hasNext()) {
            JsonNode item = iterator.next();
            DAGNode source = items.get(item.get("sourceId").asText());
            DAGNode target = items.get(item.get("targetId").asText());
            source.getTargets().add(target);
            target.getSources().add(source);
        }
        return items;
    }

    /**
     * 执行DAGConfig配置，并返回非配置节点
     */
    public Map<String, DAGNode> configure(Map<String, DAGNode> items) throws Exception {
        Map<String, DAGNode> nodes = new LinkedHashMap<>();
        for (Map.Entry<String, DAGNode> entry : items.entrySet()) {
            DAGNode node = entry.getValue();
            if (node instanceof DAGConfig) continue;
            nodes.put(entry.getKey(), configure(node, 0));
        }
        for (Map.Entry<String, DAGNode> entry : nodes.entrySet()) {
            DAGNode node = entry.getValue();
            node.setSources(removeConfigNode(node.getSources()));
            node.setTargets(removeConfigNode(node.getTargets()));
        }
        return nodes;
    }

    public Set<DAGNode> removeConfigNode(Set<DAGNode> nodes) {
        Set<DAGNode> result = new HashSet<>();
        for (DAGNode node : nodes) {
            if (node instanceof DAGConfig) continue;
            result.add(node);
        }
        return result;
    }

    public DAGNode configure(DAGNode node, int routeSize) throws Exception {
        if (node.isConfigured()) return node;
        if (++routeSize > MAX_ROUTE_SIZE) {
            throw new RuntimeException("configure route exhausted!");
        }
        List<JsonNode> configs = new ArrayList<>();
        for (DAGNode source : node.getSources()) {
            configure(source, routeSize);
            if (!(source instanceof DAGConfig)) continue;
            configs.add(source.result(JsonNode.class));
        }
        node.configure(configs.toArray(new JsonNode[0]));
        node.setConfigured(true);
        if ((node instanceof DAGConfig) && !node.isProcessed()) {
            node.setResult(node.process());
            node.setProcessed(true);
        }
        return node;
    }

    private boolean recursive(Map<String, DAGNode> nodeMap) throws Exception {
        // 查找入度为零的全部节点
        List<DAGNode> list = new ArrayList<>();
        for (Map.Entry<String, DAGNode> entry : nodeMap.entrySet()) {
            DAGNode node = entry.getValue();
            if(node.isProcessed()) continue;
            Set<DAGNode> set = node.getSources();
            boolean isReady = true; // 上级节点是否准备就绪
            for (DAGNode source : set) {
                if(source.isProcessed()) continue;
                isReady = false;
                break;
            }
            if(isReady) list.add(node);
        }
        // 执行任务
        for (DAGNode node : list) {
            node.setResult(node.process());
            node.setProcessed(true);
        }
        return !list.isEmpty();
    }

    public void execute() throws Exception {
        Map<String, DAGNode> items = items(diagram.at("/items"));
        Map<String, DAGNode> nodes = configure(relations(items, diagram.at("/relations")));
        // 查找入度为零的节点并执行
        while(recursive(nodes)) {}
    }

}
