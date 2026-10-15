package com.iisquare.fs.base.dag.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;

import java.io.Serializable;
import java.util.*;

/**
 * DAG 执行器：负责图构建、校验、配置与调度。
 *
 * 相比旧实现的关键变化：
 * 1. 节点注册表使用 Class&lt;? extends DAGNode&lt;?&gt;&gt;，去掉裸类型强转；
 * 2. 执行前统一校验（未知节点类型、重复ID、悬空边、循环依赖），错误信息带节点上下文；
 * 3. 用 Kahn 拓扑排序一次得到执行顺序并检测环，替代多轮扫描与深度兜底；
 * 4. execute() 返回 DAGExecution（各节点状态、输出、耗时、预览），不再“执行完什么也拿不到”。
 */
public abstract class DAGRunner implements Serializable {

    protected JsonNode diagram;
    protected Map<String, Class<? extends DAGNode<?>>> nodes;
    protected String debugId = null; // 调试目标节点，为空时执行全图

    public DAGRunner(JsonNode diagram, Map<String, Class<? extends DAGNode<?>>> nodes) {
        this.diagram = diagram;
        this.nodes = nodes;
    }

    /**
     * 开启节点调试：仅执行至指定节点为止，并收集各节点输出结果
     */
    public DAGRunner debug(String nodeId) {
        this.debugId = nodeId;
        return this;
    }

    public boolean isDebug() {
        return !DPUtil.empty(debugId);
    }

    public String debugId() {
        return debugId;
    }

    /**
     * 将配置转换为DAGNode对象
     */
    public Map<String, DAGNode<?>> items(JsonNode items) {
        Map<String, DAGNode<?>> result = new LinkedHashMap<>();
        if (null == items || !items.isArray()) return result;
        for (JsonNode item : items) {
            String id = item.at("/id").asText();
            if (DPUtil.empty(id)) throw new DAGException("节点缺少id：" + DPUtil.stringify(item));
            if (result.containsKey(id)) throw new DAGException("节点id重复：" + id);
            String type = item.at("/type").asText();
            if (Arrays.asList("SubprocessLayout", "GroupLayout").contains(type)) continue;
            Class<? extends DAGNode<?>> classType = this.nodes.get(type);
            if (null == classType) {
                throw new DAGException("未知节点类型：" + type + "，可用类型："
                        + DPUtil.implode(", ", new TreeSet<>(this.nodes.keySet())));
            }
            DAGNode<?> node = newInstance(classType);
            node.setId(id);
            node.setName(item.at("/name").asText(id));
            node.setParallelism(Math.max(1, item.at("/parallelism").asInt(1)));
            String kvConfigPrefix = item.at("/kvConfigPrefix").asText();
            if (node instanceof DAGSource) {
                ((DAGSource<?>) node).setKvConfigPrefix(kvConfigPrefix);
            } else if (node instanceof DAGTransform) {
                ((DAGTransform<?>) node).setKvConfigPrefix(kvConfigPrefix);
            } else if (node instanceof DAGSink) {
                ((DAGSink<?>) node).setKvConfigPrefix(kvConfigPrefix);
            }
            JsonNode options = item.at("/options");
            node.setOptions(options.isObject() ? options : DPUtil.objectNode());
            node.setRunner(this);
            result.put(node.getId(), node);
        }
        return result;
    }

    private DAGNode<?> newInstance(Class<? extends DAGNode<?>> classType) {
        try {
            return classType.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new DAGException("实例化节点失败：" + classType.getName() + "，" + e.getMessage());
        }
    }

    /**
     * 建立DAGNode之间的关联关系
     */
    public Map<String, DAGNode<?>> relations(Map<String, DAGNode<?>> items, JsonNode relations) {
        if (null == relations || !relations.isArray()) return items;
        for (JsonNode item : relations) {
            String sourceId = item.at("/source").asText();
            String targetId = item.at("/target").asText();
            DAGNode<?> source = items.get(sourceId);
            DAGNode<?> target = items.get(targetId);
            if (null == source || null == target) {
                throw new DAGException("连线的节点不存在：" + sourceId + " -> " + targetId);
            }
            source.getTargets().add(target);
            target.getSources().add(source);
        }
        return items;
    }

    /**
     * Kahn 拓扑排序：同时完成环检测，保证执行顺序与上游顺序确定
     */
    public List<DAGNode<?>> topological(Map<String, DAGNode<?>> items) {
        Map<String, Integer> indegree = new LinkedHashMap<>();
        for (Map.Entry<String, DAGNode<?>> entry : items.entrySet()) {
            int count = 0;
            for (DAGNode<?> source : entry.getValue().getSources()) {
                if (items.containsKey(source.getId())) count++;
            }
            indegree.put(entry.getKey(), count);
        }
        Deque<String> ready = new ArrayDeque<>();
        for (Map.Entry<String, Integer> entry : indegree.entrySet()) {
            if (0 == entry.getValue()) ready.add(entry.getKey());
        }
        List<DAGNode<?>> result = new ArrayList<>();
        while (!ready.isEmpty()) {
            DAGNode<?> node = items.get(ready.poll());
            result.add(node);
            for (DAGNode<?> target : node.getTargets()) {
                Integer value = indegree.get(target.getId());
                if (null == value) continue;
                value = value - 1;
                indegree.put(target.getId(), value);
                if (0 == value) ready.add(target.getId());
            }
        }
        if (result.size() != items.size()) {
            throw new DAGException("检测到循环依赖：" + cycle(items, indegree));
        }
        return result;
    }

    /**
     * 从剩余节点中回溯出一条环路，便于定位问题
     */
    private String cycle(Map<String, DAGNode<?>> items, Map<String, Integer> indegree) {
        for (Map.Entry<String, Integer> entry : indegree.entrySet()) {
            if (entry.getValue() <= 0) continue;
            List<String> path = new ArrayList<>();
            Set<String> visited = new LinkedHashSet<>();
            DAGNode<?> node = items.get(entry.getKey());
            while (null != node && !visited.contains(node.getId())) {
                visited.add(node.getId());
                path.add(node.getId());
                DAGNode<?> next = null;
                for (DAGNode<?> target : node.getTargets()) {
                    if (items.containsKey(target.getId()) && indegree.getOrDefault(target.getId(), 0) > 0) {
                        next = target;
                        break;
                    }
                }
                node = next;
            }
            if (null != node) path.add(node.getId());
            if (!path.isEmpty()) return DPUtil.implode(" -> ", path);
        }
        return "unknown";
    }

    /**
     * 执行DAGConfig配置，并返回非配置节点
     */
    public Map<String, DAGNode<?>> configure(Map<String, DAGNode<?>> items) {
        Map<String, DAGNode<?>> result = new LinkedHashMap<>();
        for (Map.Entry<String, DAGNode<?>> entry : items.entrySet()) {
            if (entry.getValue() instanceof DAGConfig) continue;
            result.put(entry.getKey(), configure(entry.getValue()));
        }
        for (DAGNode<?> node : result.values()) {
            node.getSources().removeIf(source -> source instanceof DAGConfig);
            node.getTargets().removeIf(target -> target instanceof DAGConfig);
        }
        return result;
    }

    public DAGNode<?> configure(DAGNode<?> node) {
        if (node.isConfigured()) return node;
        node.setState(DAGNode.State.CONFIGURED); // 先置位，避免重复配置
        List<JsonNode> configs = new ArrayList<>();
        for (DAGNode<?> source : node.getSources()) {
            configure(source);
            if (!(source instanceof DAGConfig)) continue;
            configs.add(((DAGConfig) source).result());
        }
        if (!node.configure(configs.toArray(new JsonNode[0]))) {
            node.setState(DAGNode.State.FAILED);
            throw new DAGException(node, "CONFIGURE", "节点配置失败", null);
        }
        if ((node instanceof DAGConfig) && !node.isProcessed()) {
            try {
                node.setResult(((DAGConfig) node).process());
                node.setState(DAGNode.State.SUCCEEDED);
            } catch (Exception e) {
                node.setState(DAGNode.State.FAILED);
                throw new DAGException(node, "CONFIGURE", "配置节点执行失败：" + e.getMessage(), e);
            }
        }
        return node;
    }

    /**
     * 将节点输出转换为JSON，子类可覆写以支持引擎特有的数据集预览
     */
    protected JsonNode describe(DAGNode<?> node) {
        return describeValue(node.result());
    }

    protected JsonNode describeValue(Object result) {
        if (null == result) return DPUtil.toJSON(null);
        if (result instanceof JsonNode) return (JsonNode) result;
        if (result instanceof String || result instanceof Number || result instanceof Boolean) {
            return DPUtil.toJSON(result);
        }
        ObjectNode node = DPUtil.objectNode();
        node.put("class", result.getClass().getName());
        node.put("preview", DPUtil.parseString(result));
        return node;
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

    /**
     * 构建并执行整张图，返回执行报告；失败时抛出携带已执行状态的 DAGException
     */
    public DAGExecution execute() {
        Map<String, DAGNode<?>> items = items(diagram.at("/nodes"));
        JsonNode edges = edges(diagram.at("/nodes"), diagram.at("/edges"));
        Map<String, DAGNode<?>> nodes = relations(items, edges);
        if (isDebug() && !nodes.containsKey(debugId)) {
            throw new DAGException("调试目标节点不存在：" + debugId);
        }
        List<DAGNode<?>> order = topological(nodes);
        configure(nodes);
        DAGExecution execution = new DAGExecution();
        for (DAGNode<?> node : order) {
            long start = System.currentTimeMillis();
            // 配置节点已在 configure() 阶段执行完毕，此处只调度数据节点，避免重复执行配置节点
            if (node instanceof DAGConfig) continue;
            node.setState(DAGNode.State.RUNNING);
            try {
                node.setResult(node.process());
                node.setState(DAGNode.State.SUCCEEDED);
            } catch (Exception e) {
                node.setState(DAGNode.State.FAILED);
                String message;
                if (e instanceof DAGException) {
                    // 已是带上下文的DAG异常，复用其原始描述，避免前缀重复
                    message = ((DAGException) e).detail();
                } else {
                    message = DPUtil.empty(e.getMessage()) ? e.getClass().getName() : e.getMessage();
                }
                execution.record(node, System.currentTimeMillis() - start, null, null, message);
                throw new DAGException(execution, node, "RUN", message, e);
            }
            JsonNode preview = null;
            if (isDebug()) {
                try {
                    preview = describe(node);
                } catch (Exception e) {
                    preview = DPUtil.toJSON(ApiUtil.getStackTrace(e));
                }
            }
            execution.record(node, System.currentTimeMillis() - start, node.result(), preview, null);
            // 调试模式：目标节点执行完毕后立即终止，不再执行下游节点
            if (isDebug() && node.getId().equals(debugId)) break;
        }
        return execution;
    }

}
