package com.iisquare.fs.base.dag.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 一次 DAG 执行的报告：各节点的状态、输出、耗时与错误。
 *
 * 替代原先“执行完什么也拿不到、只能回头去引擎里捞”的做法，调试输出（preview）也统一收敛到这里。
 */
public class DAGExecution implements Serializable {

    public static class NodeExecution implements Serializable {

        private final String id;
        private final String name;
        private final String type;
        private final String outputType;
        private final long elapsedMs;
        private final Object result;
        private final JsonNode preview;
        private final String error;

        public NodeExecution(DAGNode<?> node, long elapsedMs, Object result, JsonNode preview, String error) {
            this.id = node.getId();
            this.name = node.getName();
            this.type = node.getType();
            this.outputType = node.outputType().label();
            this.elapsedMs = elapsedMs;
            this.result = result;
            this.preview = preview;
            this.error = error;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public String getOutputType() {
            return outputType;
        }

        public long getElapsedMs() {
            return elapsedMs;
        }

        public Object getResult() {
            return result;
        }

        public JsonNode getPreview() {
            return preview;
        }

        public String getError() {
            return error;
        }

        public boolean succeed() {
            return null == error;
        }
    }

    private final Map<String, NodeExecution> items = new LinkedHashMap<>();

    public void record(DAGNode<?> node, long elapsedMs, Object result, JsonNode preview, String error) {
        items.put(node.getId(), new NodeExecution(node, elapsedMs, result, preview, error));
    }

    public Map<String, NodeExecution> items() {
        return items;
    }

    public NodeExecution item(String id) {
        return items.get(id);
    }

    public Object result(String id) {
        NodeExecution item = items.get(id);
        return null == item ? null : item.getResult();
    }

    /**
     * 各节点输出的JSON预览，键为节点ID（键顺序与执行顺序一致）
     */
    public ObjectNode outputs() {
        ObjectNode result = DPUtil.objectNode();
        for (Map.Entry<String, NodeExecution> entry : items.entrySet()) {
            JsonNode preview = entry.getValue().getPreview();
            result.set(entry.getKey(), null == preview ? DPUtil.toJSON(null) : preview);
        }
        return result;
    }

    public boolean succeed() {
        for (NodeExecution item : items.values()) {
            if (!item.succeed()) return false;
        }
        return true;
    }

    public String failedNode() {
        for (NodeExecution item : items.values()) {
            if (!item.succeed()) return item.getId();
        }
        return null;
    }

}
