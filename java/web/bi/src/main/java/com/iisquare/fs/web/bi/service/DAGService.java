package com.iisquare.fs.web.bi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.jpa.util.JPAUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.bi.dao.DiagramDao;
import com.iisquare.fs.web.bi.entity.Diagram;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
public class DAGService extends ServiceBase {

    @Autowired
    private DiagramDao diagramDao;

    public ObjectNode diagram(Integer id, ObjectNode cached) {
        if(null == id || id < 1) return null;
        String sid = String.valueOf(id);
        if (null != cached && cached.has(sid)) return (ObjectNode) cached.get(sid);
        Diagram diagram = JPAUtil.findById(diagramDao, id, Diagram.class);
        if (null == diagram || 1 != diagram.getStatus()) {
            if (null != cached) cached.replace(sid, null);
            return null;
        }
        ObjectNode result = DPUtil.objectNode();
        result.put("id", diagram.getId()).put("name", diagram.getName());
        result.put("engine", diagram.getEngine()).put("model", diagram.getModel());
        JsonNode content = DPUtil.parseJSON(diagram.getContent());
        if (null == content || !content.isObject()) content = DPUtil.objectNode();
        ArrayNode nodes = result.putArray("nodes");
        ArrayNode edges = result.putArray("edges");
        List<String> exportIds = new ArrayList<>();
        ObjectNode importIds = DPUtil.objectNode();
        ArrayNode jars = DPUtil.arrayNode();
        ArrayNode exports = DPUtil.arrayNode();
        Iterator<JsonNode> iterator = content.at("/cells").iterator();
        while (iterator.hasNext()) {
            JsonNode json = iterator.next();
            String shape = json.at("/shape").asText("");
            if ("flow-edge".equals(shape)) {
                String source = "dag_" + diagram.getId() + "_" + json.at("/source/cell").asText();
                String target = "dag_" + diagram.getId() + "_" + json.at("/target/cell").asText();
                if (exportIds.contains(target)) {
                    exports.add(target);
                    continue;
                }
                if (importIds.has(source)) {
                    Iterator<JsonNode> it = importIds.get(source).iterator();
                    while (it.hasNext()) {
                        edges.addObject().put("source", it.next().asText()).put("target", target);
                    }
                    continue;
                }
                edges.addObject().put("source", source).put("target", target);
                continue;
            }
            String nodeId = "dag_" + diagram.getId() + "_" + json.at("/id").asText();
            String type = json.at("/data/type").asText();
            JsonNode options = json.at("/data/options");
            if (!options.isObject()) options = DPUtil.objectNode();
            if ("GroupLayout".equals(type)) continue;
            if ("ExportConfig".equals(type)) {
                exportIds.add(nodeId);
                continue;
            }
            if ("ImportConfig".equals(type)) {
                ObjectNode sub = diagram(DPUtil.parseInt(options.at("/id").asText()), cached);
                importIds.replace(nodeId, null == sub ? DPUtil.arrayNode() : sub.at("/exports"));
                nodes.addAll((ArrayNode) sub.at("/nodes"));
                jars.addAll((ArrayNode) sub.at("/jars"));
                continue;
            }
            if ("ScriptTransform".equals(type)) {
                String jar = options.at("/jarURI").asText();
                if (!DPUtil.empty(jar)) jars.add(jar);
            }
            ObjectNode node = nodes.addObject();
            node.put("id", nodeId);
            node.put("type", type);
            String name = json.at("/data/name").asText();
            if (DPUtil.empty(name)) {
                name = String.format("data_%d_node_%d", diagram.getId(), json.at("/data/index").asInt());
            }
            node.put("name", name);
            node.put("parallelism", json.at("/data/parallelism").asInt());
            String parent = json.at("/parent").asText();
            node.put("parent", DPUtil.empty(parent) ? "" : ("dag_" + diagram.getId() + "_" + parent));
            if (type.endsWith("Source") || type.endsWith("Transform") || type.endsWith("Sink")) {
                node.put("kvConfigPrefix", json.at("/data/kvConfigPrefix").asText());
            }
            node.replace("options", options);
        }
        result.replace("exports", exports);
        result.replace("jars", jars);
        if (null == cached) cached.replace(sid, result);
        return result;
    }

}
