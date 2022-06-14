package com.iisquare.fs.base.dag.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;

import java.io.Serializable;
import java.util.*;

public abstract class DAGRunner implements Serializable {

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
            if (Arrays.asList("SubprocessLayout").contains(type)) continue;
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
            DAGNode source = items.get(item.get("source").asText());
            DAGNode target = items.get(item.get("target").asText());
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

    public JsonNode edges(JsonNode nodes, JsonNode edges) {
        /**
         * 查找所有子流程
         * {
         *     processId: {
         *         children: {
         *             childId: {}
         *         },
         *         incoming: { // edge.target
         *             childId: {}
         *         },
         *         outgoing: { // edge.source
         *             childId: {}
         *         }
         *     }
         * }
         */
        ObjectNode processes = DPUtil.objectNode();
        Iterator<JsonNode> iterator = nodes.iterator();
        while (iterator.hasNext()) {
            JsonNode node = iterator.next();
            if (!"SubprocessLayout".equals(node.at("/type").asText())) continue;
            ObjectNode process = processes.putObject(node.at("/id").asText());
            process.putObject("children"); // 全部子节点
            process.putObject("incoming"); // 入度为零的节点
            process.putObject("outgoing"); // 出度为零的节点
        }
        /**
         * 查找子流程内的节点
         */
        iterator = nodes.iterator();
        while (iterator.hasNext()) {
            JsonNode node = iterator.next();
            String parent = node.at("/parent").asText();
            if (!processes.has(parent)) continue;
            ObjectNode children = (ObjectNode) processes.at("/" + parent + "/children");
            children.putObject(node.at("/id").asText());
        }
        /**
         * 出入节点索引
         * sources -> {
         *     sourceId: [{}]
         * }
         * targets -> {
         *     targetId: [{}]
         * }
         */
        ObjectNode sources = DPUtil.objectNode();
        ObjectNode targets = DPUtil.objectNode();
        iterator = edges.iterator();
        while (iterator.hasNext()) {
            JsonNode edge = iterator.next();
            String source = edge.at("/source").asText();
            String target = edge.at("/target").asText();
            (sources.has(source) ? (ArrayNode) sources.get(source) : sources.putArray(source)).add(edge);
            (targets.has(target) ? (ArrayNode) targets.get(target) : targets.putArray(target)).add(edge);
        }
        /**
         * 查找子流程内出入度为零的节点
         */
        iterator = processes.iterator();
        while (iterator.hasNext()) {
            JsonNode process = iterator.next();
            ObjectNode incoming = (ObjectNode) process.at("/incoming");
            ObjectNode outgoing = (ObjectNode) process.at("/outgoing");
            Iterator<Map.Entry<String, JsonNode>> it = process.at("/children").fields();
            while (it.hasNext()) {
                String key = it.next().getKey();
                if (!sources.has(key)) outgoing.putObject(key);
                if (!targets.has(key)) incoming.putObject(key);
            }
        }
        /**
         * 展开子流程节点
         */
        ArrayNode result = DPUtil.arrayNode();
        iterator = edges.iterator();
        while (iterator.hasNext()) {
            JsonNode edge = iterator.next();
            String source = edge.at("/source").asText();
            String target = edge.at("/target").asText();
            Iterator<Map.Entry<String, JsonNode>> sit = null; // 来源 -> processes[processId].outgoing
            Iterator<Map.Entry<String, JsonNode>> tit = null; // 目标 -> processes[processId].incoming
            sit = (processes.has(source) ? processes.get(source).get("outgoing") : DPUtil.objectNode().put(source, "")).fields();
            tit = (processes.has(target) ? processes.get(target).get("incoming") : DPUtil.objectNode().put(target, "")).fields();
            while (sit.hasNext()) {
                String sk = sit.next().getKey();
                while (tit.hasNext()) {
                    String tk = tit.next().getKey();
                    result.addObject().put("source", sk).put("target", tk);
                }
            }
        }
        return result;
    }

    public void execute() throws Exception {
        Map<String, DAGNode> items = items(diagram.at("/nodes"));
        JsonNode edges = edges(diagram.at("/nodes"), diagram.at("/edges"));
        Map<String, DAGNode> nodes = configure(relations(items, edges));
        // 查找入度为零的节点并执行
        while(recursive(nodes)) {}
    }

}
