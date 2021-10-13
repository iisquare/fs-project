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
        ObjectNode node = DPUtil.objectNode();
        node.put("id", diagram.getId()).put("name", diagram.getName());
        node.put("engine", diagram.getEngine()).put("model", diagram.getModel());
        JsonNode content = DPUtil.parseJSON(diagram.getContent());
        if (null == content || !content.isObject()) content = DPUtil.objectNode();
        ArrayNode items = node.putArray("items");
        List<String> exportIds = new ArrayList<>();
        ObjectNode importIds = DPUtil.objectNode();
        ArrayNode jars = DPUtil.arrayNode();
        Iterator<JsonNode> iterator = content.at("/items").iterator();
        while (iterator.hasNext()) {
            JsonNode json = iterator.next();
            String nodeId = "dag_" + diagram.getId() + "_" + json.at("/id").asText();
            String type = json.at("/type").asText();
            JsonNode options = json.at("/options");
            if (!options.isObject()) options = DPUtil.objectNode();
            if ("ExportConfig".equals(type)) {
                exportIds.add(nodeId);
                continue;
            }
            if ("ImportConfig".equals(type)) {
                ObjectNode sub = diagram(DPUtil.parseInt(options.at("/id").asText()), cached);
                importIds.replace(nodeId, null == sub ? DPUtil.arrayNode() : sub.at("/exports"));
                items.addAll((ArrayNode) sub.at("/items"));
                continue;
            }
            if ("ScriptTransform".equals(type)) {
                String jar = options.at("/jarURI").asText();
                if (!DPUtil.empty(jar)) jars.add(jar);
            }
            ObjectNode item = items.addObject();
            item.put("id", nodeId);
            item.put("type", type);
            if (type.endsWith("Source") || type.endsWith("Transform")) {
                item.put("alias", json.at("/alias").asText());
            }
            if (type.endsWith("Source") || type.endsWith("Transform") || type.endsWith("Sink")) {
                item.put("kvConfigPrefix", json.at("/kvConfigPrefix").asText());
            }
            item.replace("options", options);
        }
        ArrayNode relations = node.putArray("relations");
        ArrayNode exports = DPUtil.arrayNode();
        iterator = content.at("/relations").iterator();
        while (iterator.hasNext()) {
            JsonNode json = iterator.next();
            String sourceId = "dag_" + diagram.getId() + "_" + json.at("/sourceId").asText();
            String targetId = "dag_" + diagram.getId() + "_" + json.at("/targetId").asText();
            if (exportIds.contains(targetId)) {
                exports.add(sourceId);
                continue;
            }
            if (importIds.has(sourceId)) {
                Iterator<JsonNode> it = importIds.get(sourceId).iterator();
                while (it.hasNext()) {
                    relations.addObject().put("sourceId", it.next().asText()).put("targetId", targetId);
                }
                continue;
            }
            relations.addObject().put("sourceId", sourceId).put("targetId", targetId);
        }
        node.replace("exports", exports);
        node.replace("jars", jars);
        if (null == cached) cached.replace(sid, node);
        return node;
    }

}
