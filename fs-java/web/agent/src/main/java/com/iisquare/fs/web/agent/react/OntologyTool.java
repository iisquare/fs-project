package com.iisquare.fs.web.agent.react;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.web.util.RpcUtil;
import com.iisquare.fs.web.agent.core.AgenticRuntime;
import com.iisquare.fs.web.core.rpc.KGRpc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识图谱（kind = ontology） - 内部调用：每个本体定义作为一个工具，支持两种动作（对应 /kg/retrieval/traverse 页面）：
 * - search：按实体类型 + 关键词检索实体数据；
 * - path：给出起点与终点（实体类型 + 名称关键词），推理两者之间的路径（起止点内部按关键词定位）。
 *
 * 本体内声明的实体类型作为参数枚举暴露给模型，模型无需知道图数据库结构。
 */
@Service
public class OntologyTool implements AgenticTool {

    @Autowired
    KGRpc kgRpc;

    @Override
    public String kind() {
        return "ontology";
    }

    @Override
    public ObjectNode definition(JsonNode item) {
        String name = item.at("/name").asText("");
        if (DPUtil.empty(name)) return null;
        int ontologyId = item.at("/ontologyId").asInt(0);
        if (ontologyId < 1) return null;
        JsonNode model = model(ontologyId);
        if (model.has("status") && 1 != model.at("/status").asInt(0)) return null;
        if (item.isObject()) {
            ObjectNode value = (ObjectNode) item;
            value.put("ontologyName", model.at("/name").asText(""));
            value.put("description", model.at("/description").asText(""));
        }
        ArrayNode params = DPUtil.arrayNode();
        ObjectNode action = AgenticTool.schema("string", "动作：search-按实体类型与关键词检索；path-推理起点与终点之间的路径");
        ArrayNode actions = DPUtil.arrayNode();
        actions.add("search");
        actions.add("path");
        action.set("enum", actions);
        params.add(AgenticTool.parameter("action", false, action));
        params.add(AgenticTool.parameter("entity", false, entitySchema(model, "检索的实体类型（search 用）")));
        params.add(AgenticTool.parameter("keyword", false, AgenticTool.schema("string", "检索关键词，按本体配置的名称字段模糊匹配（search 用）")));
        params.add(AgenticTool.parameter("fromEntity", false, entitySchema(model, "起点实体类型（path 用）")));
        params.add(AgenticTool.parameter("fromKeyword", false, AgenticTool.schema("string", "起点名称关键词（path 用）")));
        params.add(AgenticTool.parameter("toEntity", false, entitySchema(model, "终点实体类型（path 用）")));
        params.add(AgenticTool.parameter("toKeyword", false, AgenticTool.schema("string", "终点名称关键词（path 用）")));
        params.add(AgenticTool.parameter("maxDepth", false, AgenticTool.schema("integer", "路径最大深度，默认 3，最大 5（path 用）")));
        params.add(AgenticTool.parameter("limit", false, AgenticTool.schema("integer", "返回条数：search 默认 15、path 默认 5，最大 100")));
        return AgenticTool.definition(name, params, item);
    }

    @Override
    public Object invoke(AgenticRuntime runtime, JsonNode item, Map<String, Object> args,
                         Map<String, ObjectNode> outputs, Map<String, Object> variables) {
        int ontologyId = item.at("/ontologyId").asInt(0);
        if (ontologyId < 1) throw new IllegalStateException("本体工具未选择本体");
        JsonNode model = model(ontologyId);
        if (model.has("status") && 1 != model.at("/status").asInt(0)) {
            throw new IllegalStateException("知识图谱已停用：" + ontologyId);
        }
        String action = DPUtil.parseString(args.get("action"));
        if (!"path".equalsIgnoreCase(action) && !DPUtil.empty(args.get("fromKeyword"))) action = "path";
        return "path".equalsIgnoreCase(action) ? path(ontologyId, model, args) : search(ontologyId, model, args);
    }

    /** 实体检索：实体类型 + 关键词 */
    protected ObjectNode search(int ontologyId, JsonNode model, Map<String, Object> args) {
        String entity = DPUtil.parseString(args.get("entity"));
        if (DPUtil.empty(entity)) throw new IllegalStateException("本体工具缺少实体类型参数");
        JsonNode data = searchRows(ontologyId, entity, DPUtil.parseString(args.get("keyword")), limit(args, 15));
        ObjectNode value = DPUtil.objectNode();
        value.put("ontologyId", ontologyId);
        value.put("description", model.at("/description").asText(""));
        value.put("entity", entity);
        value.put("keyword", DPUtil.parseString(args.get("keyword")));
        value.put("total", data.at("/total").asLong(0));
        value.set("rows", data.at("/rows"));
        return value;
    }

    /**
     * 两点路径推理：起点与终点按「实体类型 + 名称关键词」定位后调用图路径接口，
     * 返回命中路径（含节点与关系明细，以及可读的路径描述文本）
     */
    protected ObjectNode path(int ontologyId, JsonNode model, Map<String, Object> args) {
        String fromEntity = DPUtil.parseString(args.get("fromEntity"));
        String toEntity = DPUtil.parseString(args.get("toEntity"));
        if (DPUtil.empty(fromEntity) || DPUtil.empty(toEntity)) {
            throw new IllegalStateException("路径推理需要提供起点与终点的实体类型（fromEntity / toEntity）");
        }
        ObjectNode from = locate(ontologyId, model, fromEntity, DPUtil.parseString(args.get("fromKeyword")));
        if (null == from) throw new IllegalStateException("未找到起点数据：" + DPUtil.parseString(args.get("fromKeyword")));
        ObjectNode to = locate(ontologyId, model, toEntity, DPUtil.parseString(args.get("toKeyword")));
        if (null == to) throw new IllegalStateException("未找到终点数据：" + DPUtil.parseString(args.get("toKeyword")));
        Map<String, Object> param = new LinkedHashMap<>();
        param.put("ontologyId", ontologyId);
        param.put("fromEntity", fromEntity);
        param.put("fromId", from.at("/id").asText(""));
        param.put("toEntity", toEntity);
        param.put("toId", to.at("/id").asText(""));
        int maxDepth = DPUtil.parseInt(args.get("maxDepth"));
        param.put("maxDepth", maxDepth < 1 ? 3 : Math.min(maxDepth, 5));
        param.put("limit", limit(args, 5));
        JsonNode data = RpcUtil.data(kgRpc.post("/graph/paths", param), false);
        if (null == data || data.isNull() || data.isMissingNode()) {
            throw new IllegalStateException("路径推理无返回：" + fromEntity + " → " + toEntity);
        }
        ObjectNode value = DPUtil.objectNode();
        value.put("ontologyId", ontologyId);
        value.put("description", model.at("/description").asText(""));
        value.put("action", "path");
        value.set("from", from);
        value.set("to", to);
        value.put("maxDepth", param.get("maxDepth").toString());
        value.put("truncated", data.at("/truncated").asBoolean(false));
        value.set("paths", data.at("/paths"));
        value.set("nodes", data.at("/nodes"));
        value.set("relationships", data.at("/relationships"));
        // 可读路径：A -[关系]-> B -[关系]-> C，便于模型直接引用
        value.set("text", pathTexts(model, data));
        return value;
    }

    /** 按实体类型 + 关键词取出第一条命中，作为路径起点/终点 */
    protected ObjectNode locate(int ontologyId, JsonNode model, String entity, String keyword) {
        JsonNode data = searchRows(ontologyId, entity, keyword, 1);
        JsonNode row = data.at("/rows/0");
        if (!row.isObject()) return null;
        JsonNode def = entityDef(model, entity);
        String primary = def.at("/primaryField").asText("id");
        JsonNode properties = row.at("/properties");
        ObjectNode value = DPUtil.objectNode();
        value.put("entity", entity);
        value.put("id", properties.at("/" + primary).asText(""));
        value.put("caption", caption(def, properties, value.at("/id").asText("")));
        value.set("properties", properties);
        return value;
    }

    /** 实体检索：实体类型 label + 关键词 */
    protected JsonNode searchRows(int ontologyId, String entity, String keyword, int pageSize) {
        Map<String, Object> param = new LinkedHashMap<>();
        param.put("ontologyId", ontologyId);
        param.put("entity", entity);
        param.put("keyword", keyword);
        param.put("page", 1);
        param.put("pageSize", pageSize);
        JsonNode data = RpcUtil.data(kgRpc.post("/graph/search", param), false);
        if (null == data || data.isNull() || data.isMissingNode()) {
            throw new IllegalStateException("本体检索无返回：" + entity);
        }
        return data;
    }

    /** 路径描述文本：按路径里的节点与关系顺序拼成「A -[关系]-> B」形式 */
    protected ArrayNode pathTexts(JsonNode model, JsonNode data) {
        Map<String, String> captionFields = new LinkedHashMap<>();
        for (JsonNode entity : model.at("/entities")) {
            captionFields.put(entity.at("/label").asText(""),
                    entity.at("/captionField").asText("name"));
        }
        Map<String, JsonNode> nodes = new LinkedHashMap<>();
        for (JsonNode node : data.at("/nodes")) nodes.put(node.at("/elementId").asText(""), node);
        Map<String, JsonNode> relationships = new LinkedHashMap<>();
        for (JsonNode row : data.at("/relationships")) relationships.put(row.at("/elementId").asText(""), row);
        ArrayNode texts = DPUtil.arrayNode();
        for (JsonNode path : data.at("/paths")) {
            List<String> parts = new ArrayList<>();
            JsonNode nodeIds = path.at("/nodes");
            JsonNode relationshipIds = path.at("/relationships");
            for (int index = 0; index < nodeIds.size(); index++) {
                JsonNode node = nodes.get(nodeIds.get(index).asText(""));
                parts.add(nodeCaption(model, captionFields, node, nodeIds.get(index).asText("")));
                if (index >= relationshipIds.size()) continue;
                JsonNode relationship = relationships.get(relationshipIds.get(index).asText(""));
                parts.add("-[ " + relationship.at("/type").asText("关联") + " ]->");
            }
            texts.add(DPUtil.implode(" ", parts));
        }
        return texts;
    }

    /** 节点展示名：优先取本体里配置的名称字段 */
    protected String nodeCaption(JsonNode model, Map<String, String> captionFields, JsonNode node, String fallback) {
        if (null == node) return fallback;
        JsonNode properties = node.at("/properties");
        List<String> fields = new ArrayList<>();
        for (JsonNode label : node.at("/labels")) {
            String field = captionFields.get(label.asText(""));
            if (!DPUtil.empty(field)) fields.add(field);
        }
        fields.add("name");
        fields.add("title");
        for (String field : fields) {
            String value = properties.at("/" + field).asText("");
            if (!DPUtil.empty(value)) return value;
        }
        return fallback;
    }

    /** 实体展示名：取本体配置的名称字段，其次回落到实体 label */
    protected String caption(JsonNode def, JsonNode properties, String fallback) {
        List<String> fields = new ArrayList<>();
        String field = def.at("/captionField").asText("");
        if (!DPUtil.empty(field)) fields.add(field);
        fields.add("name");
        fields.add("title");
        for (String item : fields) {
            String value = properties.at("/" + item).asText("");
            if (!DPUtil.empty(value)) return value;
        }
        return fallback;
    }

    /** 本体模型里的实体定义 */
    protected JsonNode entityDef(JsonNode model, String label) {
        for (JsonNode entity : model.at("/entities")) {
            if (label.equals(entity.at("/label").asText(""))) return entity;
        }
        return DPUtil.objectNode();
    }

    protected int limit(Map<String, Object> args, int def) {
        int limit = DPUtil.parseInt(args.get("limit"));
        return limit < 1 ? def : Math.min(limit, 100);
    }

    /** 实体类型参数：用本体里声明的实体 label 作为枚举，展示名写进说明，模型据此选择 */
    protected ObjectNode entitySchema(JsonNode model, String prefix) {
        ObjectNode schema = AgenticTool.schema("string", prefix);
        ArrayNode enums = DPUtil.arrayNode();
        List<String> labels = new ArrayList<>();
        for (JsonNode entity : model.at("/entities")) {
            String label = entity.at("/label").asText("");
            if (DPUtil.empty(label)) continue;
            enums.add(label);
            String title = entity.at("/name").asText(label);
            labels.add(title.equals(label) ? label : title + "（" + label + "）");
        }
        if (!enums.isEmpty()) {
            schema.set("enum", enums);
            schema.put("description", prefix + "：" + DPUtil.implode("、", labels));
        }
        return schema;
    }

    /** 本体模型：实体与关系定义（KG 侧带缓存） */
    protected JsonNode model(int ontologyId) {
        Map<String, Object> param = new LinkedHashMap<>();
        param.put("id", ontologyId);
        JsonNode data = RpcUtil.data(kgRpc.post("/ontology/model", param), false);
        if (null == data || data.isNull() || data.isMissingNode()) {
            throw new IllegalStateException("本体不存在或无权访问：" + ontologyId);
        }
        return data;
    }

}
