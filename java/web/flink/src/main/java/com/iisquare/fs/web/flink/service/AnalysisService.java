package com.iisquare.fs.web.flink.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.flink.dao.AnalysisDao;
import com.iisquare.fs.web.flink.dao.AnalysisNodeDao;
import com.iisquare.fs.web.flink.entity.Analysis;
import com.iisquare.fs.web.flink.entity.AnalysisNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AnalysisService extends ServiceBase {

    @Autowired
    private AnalysisDao analysisDao;
    @Autowired
    private AnalysisNodeDao analysisNodeDao;
    @Autowired
    private FlowService flowService;

    public Analysis analysisInfo(Integer id) {
        if(null == id || id < 1) return null;
        Optional<Analysis> info = analysisDao.findById(id);
        return info.isPresent() ? info.get() : null;
    }

    private ObjectNode modelNode(Analysis info) {
        ObjectNode node = DPUtil.objectNode();
        node.put("id", "OLAPAnalysis" + info.getId());
        node.put("alias", "");
        node.put("type", "system");
        node.put("plugin", "olap");
        node.put("classname", "com.iisquare.fs.flink.plugins.olap.node.AnalysisNode");
        node.put("parallelism", "");
        node.replace("info", DPUtil.convertJSON(info));
        node.put("timezone", "GMT+8");
        node.putObject("returns");
        return node;
    }

    private ObjectNode sinkNode() {
        ObjectNode node = DPUtil.objectNode();
        node.put("id", "OLAPSink");
        node.put("alias", "");
        node.put("type", "node");
        node.put("plugin", "core");
        node.put("classname", "com.iisquare.fs.flink.plugins.core.node.HBaseSinkNode");
        node.put("kvConfigPrefix", "spring.data.hbase");
        node.put("parallelism", "");
        node.put("quorum", "kvConfig:zookeeper.quorum");
        node.put("znode", "kvConfig:znode.parent");
        node.put("tableName", "fs_analysis_result");
        node.put("idField", "_id");
        node.put("tableField", "_table");
        node.putObject("returns");
        return node;
    }

    public ObjectNode parse(Analysis info, String logId, Object ext) {
        if(null == info) return null;
        JsonNode analysis = DPUtil.parseJSON(info.getContent());
        if(null == analysis) return null;
        ObjectNode result = DPUtil.objectNode();
        result.put("id", info.getId());
        result.put("name", info.getName());
        result.put("type", "batch"); // 固定使用批处理
        result.put("logId", logId);
        String appname = "analysis-" + info.getName() + "-" + info.getId();
        if(!DPUtil.empty(logId)) appname += "-" + logId;
        result.put("appname", appname);
        // 解析数据仓库
        Set<Integer> tableIds = new HashSet<>();
        Iterator<JsonNode> tables = analysis.get("tables").elements();
        while (tables.hasNext()) {
            tableIds.add(tables.next().get("from").asInt());
        }
        if (tableIds.size() < 1) return null;
        List<AnalysisNode> analysisNodes = analysisNodeDao.findAllById(tableIds);
        if(analysisNodes.size() < 1) return null;
        JsonNode kvConfig = DPUtil.parseJSON(DPUtil.stringify(flowService.kvConfig()));
        ArrayNode nodes = result.putArray("nodes"); // 流程节点
        ObjectNode modelNode = modelNode(info); // 模型节点
        nodes.add(modelNode);
        String modelNodeId = modelNode.get("id").asText();
        ObjectNode sinkNode = sinkNode();   // 输出节点
        flowService.kvReplace(kvConfig, sinkNode);
        nodes.add(sinkNode);
        ObjectNode plugins = result.putObject("plugins"); // 插件集合
        plugins.putArray("core");
        ArrayNode connections = result.putArray("connections"); // 节点连线
        for (AnalysisNode analysisNode : analysisNodes) {
            String warehousePrefix = "warehouse" + analysisNode.getId() + "-";
            JsonNode warehouse = DPUtil.parseJSON(analysisNode.getContent());
            Iterator<Map.Entry<String, JsonNode>> iNodes = warehouse.get("nodes").fields();
            while (iNodes.hasNext()) {
                Map.Entry<String, JsonNode> iNode = iNodes.next();
                ObjectNode node = DPUtil.objectNode();
                // 配置项
                Iterator<JsonNode> iterator = iNode.getValue().get("property").elements();
                while (iterator.hasNext()) {
                    JsonNode prop = iterator.next();
                    node.put(prop.get("key").asText(), prop.get("value").asText());
                }
                // 采用配置中心参数替换配置项
                flowService.kvReplace(kvConfig, node);
                // 返回字段
                JsonNode returns = DPUtil.parseJSON(node.get("returns").asText());
                if(null == returns) returns = DPUtil.objectNode();
                node.replace("returns", returns);
                plugins.putArray(node.get("plugin").asText());
                // 替换配置参数
                node.put("id", warehousePrefix + node.get("id").asText());
                if (node.get("classname").asText().endsWith(".plugins.olap.node.AnchorNode")) {
                    node.put("warehouse", analysisNode.getId());
                    // 连接数据仓库锚点
                    ObjectNode connection = DPUtil.objectNode();
                    connection.put("sourceId", node.get("id").asText());
                    connection.put("targetId", modelNodeId);
                    connection.put("sourceAnchor", "RightMiddle");
                    connection.put("targetAnchor", "LeftMiddle");
                    connections.add(connection);
                }
                nodes.add(node);
            }
            // 替换连接点
            Iterator<JsonNode> iConnections = warehouse.get("connections").elements();
            while (iConnections.hasNext()) {
                JsonNode iConnection = iConnections.next();
                ObjectNode connection = DPUtil.objectNode();
                connection.put("sourceId", warehousePrefix + iConnection.get("sourceId").asText());
                connection.put("targetId", warehousePrefix + iConnection.get("targetId").asText());
                connection.put("sourceAnchor", iConnection.get("sourceAnchor").asText());
                connection.put("targetAnchor", iConnection.get("targetAnchor").asText());
                connections.add(connection);
            }
        }
        // 连接输出锚点
        ObjectNode sinkConnection = DPUtil.objectNode();
        sinkConnection.put("sourceId", modelNodeId);
        sinkConnection.put("targetId", sinkNode.get("id").asText());
        sinkConnection.put("sourceAnchor", "LeftMiddle");
        sinkConnection.put("targetAnchor", "RightMiddle");
        connections.add(sinkConnection);
        return flowService.extend(result, ext);
    }

}
