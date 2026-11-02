package com.iisquare.fs.web.agent.react;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.agent.core.AgenticRuntime;
import com.iisquare.fs.web.agent.entity.Knowledge;
import com.iisquare.fs.web.agent.service.KnowledgeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库（kind = knowledge） - 内部调用：模型给出 query，按知识库召回并回填结果。
 * 召回结果按 recallScope 取结果集（粒度优先级：全文 document > 分段 segment > 分块 chunk）：
 * 分块召回只返回 chunks；分段、全文召回同时返回 chunks 与 segments，取更粗粒度的 segments。
 */
@Service
public class KnowledgeTool implements AgenticTool {

    @Autowired
    KnowledgeService knowledgeService;

    @Override
    public String kind() {
        return "knowledge";
    }

    @Override
    public ObjectNode definition(JsonNode item) {
        String name = item.at("/name").asText("");
        if (DPUtil.empty(name)) return null;
        int knowledgeId = item.at("/knowledgeId").asInt(0);
        Knowledge info = knowledgeId < 1 ? null : knowledgeService.info(knowledgeId);
        if (null == info || 1 != DPUtil.parseInt(info.getStatus())) return null;
        if (item.isObject()) {
            ObjectNode value = (ObjectNode) item;
            value.put("knowledgeName", DPUtil.parseString(info.getName()));
            value.put("description", DPUtil.parseString(info.getDescription()));
        }
        ArrayNode params = DPUtil.arrayNode();
        params.add(AgenticTool.parameter("query", true, AgenticTool.schema("string", "检索或提问的内容")));
        return AgenticTool.definition(name, params, item);
    }

    @Override
    public Object invoke(AgenticRuntime runtime, JsonNode item, Map<String, Object> args,
                         Map<String, ObjectNode> outputs, Map<String, Object> variables) {
        int knowledgeId = item.at("/knowledgeId").asInt(0);
        if (knowledgeId < 1) throw new IllegalStateException("知识库工具未选择知识库");
        Knowledge info = knowledgeService.info(knowledgeId);
        if (null == info || 1 != DPUtil.parseInt(info.getStatus())) {
            throw new IllegalStateException("知识库不存在或已停用：" + knowledgeId);
        }
        ObjectNode result = search(knowledgeId, DPUtil.parseString(args.get("query")));
        // 知识库描述只在实际调用时返回，避免在选择工具阶段占用上下文。
        result.put("description", DPUtil.parseString(info.getDescription()));
        return result;
    }

    /**
     * 知识检索：返回 { result: [...], text: "...", documents: [...] }
     * 知识库节点与「知识库工具」共用同一套召回解析
     */
    public ObjectNode search(int knowledgeId, String query) {
        Map<String, Object> param = new LinkedHashMap<>();
        param.put("id", knowledgeId);
        param.put("query", DPUtil.parseString(query));
        Map<?, ?> recalled = knowledgeService.recall(param);
        if (0 != DPUtil.parseInt(recalled.get("code"))) {
            throw new IllegalStateException(DPUtil.parseString(recalled.get("message")));
        }
        JsonNode data = DPUtil.toJSON(recalled.get("data"));
        ArrayNode list = DPUtil.arrayNode();
        List<String> texts = new ArrayList<>();
        // 文档名映射：命中结果带上来源文档名称，便于模型引用出处
        Map<String, String> documentNames = new LinkedHashMap<>();
        for (JsonNode document : data.at("/documents")) {
            documentNames.put(document.at("/id").asText(""), document.at("/name").asText(""));
        }
        for (JsonNode row : rows(data)) {
            if (!row.isObject()) continue;
            ObjectNode item = (ObjectNode) row;
            String name = documentNames.get(item.at("/documentId").asText(""));
            if (!DPUtil.empty(name)) item.put("documentName", name);
            list.add(item);
            String text = item.at("/content").asText("");
            if (DPUtil.empty(text)) text = item.at("/text").asText("");
            if (!DPUtil.empty(text)) texts.add(text);
        }
        ObjectNode result = DPUtil.objectNode();
        result.put("knowledgeId", knowledgeId);
        result.put("knowledgeName", data.at("/name").asText(""));
        result.set("result", list);
        result.put("text", DPUtil.implode("\n\n", texts));
        if (data.at("/documents").isArray() && !data.at("/documents").isEmpty()) {
            result.set("documents", data.at("/documents"));
        }
        return result;
    }

    /**
     * 召回结果集：粒度优先级为 全文(document) > 分段(segment) > 分块(chunk)。
     * 分块召回只返回 chunks，直接取 chunks；分段、全文召回两者都有，取更粗粒度的 segments；
     * 传入本身就是数组时原样返回（兼容旧实现）
     */
    protected JsonNode rows(JsonNode data) {
        if (null == data) return DPUtil.arrayNode();
        if (data.isArray()) return data;
        if (!data.isObject()) return DPUtil.arrayNode();
        String scope = DPUtil.parseString(data.at("/recallScope").asText("")).toLowerCase();
        JsonNode chunks = data.at("/chunks");
        JsonNode segments = data.at("/segments");
        if ("chunk".equals(scope)) {
            if (chunks.isArray() && !chunks.isEmpty()) return chunks;
            return segments.isArray() && !segments.isEmpty() ? segments : DPUtil.arrayNode();
        }
        if (segments.isArray() && !segments.isEmpty()) return segments;
        if (chunks.isArray() && !chunks.isEmpty()) return chunks;
        return DPUtil.arrayNode();
    }

}
