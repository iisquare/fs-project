package com.iisquare.fs.web.agent.react;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.web.util.RpcUtil;
import com.iisquare.fs.web.agent.core.AgenticRuntime;
import com.iisquare.fs.web.core.rpc.BIRpc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 数据主题（kind = theme） - 内部调用（对应 /bi/data/theme 配置的主题）：
 * - 不传 sql：返回主题的数据字典（数据集 + 字段 + 关联关系），供模型据此编写查询；
 * - 传 sql：按主题范围内已发布的数据集执行查询（表名用数据集名称），返回查询结果。
 *
 * 调用走 BI 服务 RPC，FeignInterceptor 会透传当前登录用户的 x-auth-token，
 * BI 侧据此替换与登录用户相关的变量。
 */
@Service
public class ThemeTool implements AgenticTool {

    @Autowired
    BIRpc biRpc;

    @Override
    public String kind() {
        return "theme";
    }

    @Override
    public ArrayNode definitions(JsonNode items) {
        Set<Integer> ids = new LinkedHashSet<>();
        for (JsonNode item : items) {
            int id = item.at("/themeId").asInt(0);
            if (id > 0) ids.add(id);
        }
        Map<Integer, JsonNode> themes = infos(ids);
        ArrayNode result = DPUtil.arrayNode();
        for (JsonNode item : items) {
            result.add(definition(item, themes.get(item.at("/themeId").asInt(0))));
        }
        return result;
    }

    @Override
    public ObjectNode definition(JsonNode item) {
        int themeId = item.at("/themeId").asInt(0);
        return definition(item, themeId < 1 ? null : infos(Set.of(themeId)).get(themeId));
    }

    protected ObjectNode definition(JsonNode item, JsonNode info) {
        String name = item.at("/name").asText("");
        if (DPUtil.empty(name)) return null;
        if (null == info) return null;
        if (info.has("status") && 1 != info.at("/status").asInt(0)) return null;
        // 取一次基础信息确认主题存在且当前用户可访问；类型说明由 ToolDescriptionService 统一生成，
        // 描述附在说明末尾，用服务端当前值而不是项目里的旧快照
        if (item.isObject()) {
            ObjectNode value = (ObjectNode) item;
            value.put("themeName", info.at("/name").asText(""));
            value.put("description", info.at("/description").asText(""));
        }
        ArrayNode params = DPUtil.arrayNode();
        params.add(AgenticTool.parameter("sql", false, AgenticTool.schema("string",
                "针对主题内数据集编写的查询 SQL（表名用数据集名称）；不传则返回主题的数据字典")));
        params.add(AgenticTool.parameter("limit", false, AgenticTool.schema("integer", "返回的最大行数，默认 100")));
        return AgenticTool.definition(name, params, item);
    }

    /** 一次取回多个主题基础信息，不加载数据集详情。 */
    protected Map<Integer, JsonNode> infos(Set<Integer> ids) {
        Map<Integer, JsonNode> result = new LinkedHashMap<>();
        if (ids.isEmpty()) return result;
        Map<String, Object> param = new LinkedHashMap<>();
        param.put("ids", ids);
        param.put("withDatasetInfo", false);
        JsonNode data = RpcUtil.data(biRpc.post("/dataTheme/infos", param), false);
        if (null == data) return result;
        for (JsonNode row : data.at("/rows")) {
            result.put(row.at("/id").asInt(0), row);
        }
        return result;
    }

    @Override
    public Object invoke(AgenticRuntime runtime, JsonNode item, Map<String, Object> args,
                         Map<String, ObjectNode> outputs, Map<String, Object> variables) {
        int themeId = item.at("/themeId").asInt(0);
        if (themeId < 1) throw new IllegalStateException("数据主题工具未选择主题");
        String sql = DPUtil.parseString(args.get("sql"));
        JsonNode info = available(themeId);
        if (DPUtil.empty(sql)) return dictionary(themeId, info);
        String themeName = info.at("/name").asText("");
        int limit = null == args.get("limit") || DPUtil.parseInt(args.get("limit")) < 1
                ? 100 : Math.min(DPUtil.parseInt(args.get("limit")), 10000);
        JsonNode data = queryData(themeId, themeName, sql, limit);
        ObjectNode value = DPUtil.objectNode();
        value.put("themeId", themeId);
        value.put("themeName", themeName);
        value.put("sql", sql);
        value.set("data", null == data ? DPUtil.objectNode() : data);
        return value;
    }

    /** 向 BI 发起一次数据主题范围内的 SQL 查询。 */
    protected JsonNode queryData(int themeId, String themeName, String sql, int limit) {
        Map<String, Object> param = new LinkedHashMap<>();
        param.put("sql", sql);
        param.put("themeId", themeId); // 供数据查询日志归类到所属数据主题
        param.put("themeName", themeName);
        param.put("limit", limit);
        Map<String, Object> result = RpcUtil.result(biRpc.get("/dataset/query", param));
        if (ApiUtil.failed(result)) {
            String message = ApiUtil.message(result);
            Object detail = ApiUtil.data(result, Object.class);
            if (null != detail) {
                String detailText = detail instanceof CharSequence
                        ? detail.toString() : DPUtil.stringify(detail);
                if (!DPUtil.empty(detailText)) message += "：" + detailText;
            }
            throw new IllegalStateException(DPUtil.empty(message) ? "数据主题查询失败" : message);
        }
        return DPUtil.toJSON(ApiUtil.data(result, Object.class));
    }

    /** 主题数据字典：主题基础信息 + 数据集（名称、描述、字段）+ 关联关系 */
    public ObjectNode dictionary(int themeId) {
        return dictionary(themeId, available(themeId));
    }

    /** 使用已加载的主题详情生成数据字典，避免重复请求主题详情。 */
    protected ObjectNode dictionary(int themeId, JsonNode info) {
        ObjectNode result = DPUtil.objectNode();
        result.put("themeId", themeId);
        result.put("themeName", info.at("/name").asText(""));
        result.put("description", info.at("/description").asText(""));
        // 主题详情里带了数据集信息时直接取用，避免为名称和描述再发一次详情请求
        Map<String, JsonNode> datasetInfos = new LinkedHashMap<>();
        for (JsonNode dataset : info.at("/datasets")) {
            datasetInfos.put(dataset.at("/id").asText(""), dataset);
        }
        Set<Integer> datasetIds = new LinkedHashSet<>();
        for (JsonNode item : info.at("/content/datasetIds")) {
            int id = item.asInt(0);
            if (id > 0) datasetIds.add(id);
        }
        for (JsonNode item : info.at("/datasetIds")) {
            int id = item.asInt(0);
            if (id > 0) datasetIds.add(id);
        }
        Set<Integer> unresolved = new LinkedHashSet<>();
        for (Integer id : datasetIds) {
            if (columns(datasetInfos.get(String.valueOf(id))).isEmpty()) unresolved.add(id);
        }
        Map<String, JsonNode> batchColumns = unresolved.isEmpty()
                ? new LinkedHashMap<>() : columns(unresolved);
        ArrayNode datasets = result.putArray("datasets");
        for (Integer id : datasetIds) {
            ObjectNode dataset = datasets.addObject();
            dataset.put("id", id);
            JsonNode known = datasetInfos.get(String.valueOf(id));
            JsonNode loaded = batchColumns.get(String.valueOf(id));
            String name = null == known ? "" : known.at("/name").asText("");
            if (DPUtil.empty(name) && null != loaded) name = loaded.at("/name").asText("");
            dataset.put("name", DPUtil.empty(name) ? String.valueOf(id) : name);
            String description = null == known ? "" : known.at("/description").asText("");
            if (DPUtil.empty(description) && null != loaded) description = loaded.at("/description").asText("");
            dataset.put("description", description);
            ArrayNode columns = columns(known);
            if (columns.isEmpty() && null != loaded && loaded.at("/columns").isArray()) {
                columns = (ArrayNode) loaded.at("/columns").deepCopy();
            }
            dataset.set("columns", columns);
        }
        result.set("relations", info.at("/content/relations"));
        return result;
    }

    /** 校验主题存在且可用，查询只需要主题基础信息时也必须执行同一套校验。 */
    protected JsonNode available(int themeId) {
        return available(info(themeId), themeId);
    }

    /** 统一校验主题存在性与状态，调用方可以传入完整信息或基础信息。 */
    protected JsonNode available(JsonNode info, int themeId) {
        if (null == info) {
            throw new IllegalStateException("数据主题不存在或无权访问：" + themeId);
        }
        if (info.has("status") && 1 != info.at("/status").asInt(0)) {
            throw new IllegalStateException("数据主题已停用：" + themeId);
        }
        return info;
    }

    /** 主题详情已经批量带出字段配置时直接转换，只把字段为空的数据集交给批量接口解析。 */
    protected ArrayNode columns(JsonNode dataset) {
        ArrayNode result = DPUtil.arrayNode();
        if (null == dataset) return result;
        JsonNode fields = dataset.at("/fields");
        if (fields.isTextual()) fields = DPUtil.parseJSON(fields.asText(""));
        if (!fields.isArray()) return result;
        for (JsonNode field : fields) {
            String name = field.at("/name").asText("");
            if (DPUtil.empty(name)) continue;
            ObjectNode column = result.addObject();
            column.put("name", name);
            column.put("title", field.at("/title").asText(name));
            column.put("type", field.at("/type").asText(""));
            column.put("comment", field.at("/comment").asText(""));
        }
        return result;
    }

    /** 未维护字段的数据集一次批量解析，返回 id → 字段结果。 */
    protected Map<String, JsonNode> columns(Set<Integer> ids) {
        Map<String, JsonNode> result = new LinkedHashMap<>();
        if (ids.isEmpty()) return result;
        Map<String, Object> param = new LinkedHashMap<>();
        param.put("ids", ids);
        JsonNode data = RpcUtil.data(biRpc.post("/dataset/columnsBatch", param), false);
        if (null == data) return result;
        for (JsonNode row : data.at("/rows")) {
            result.put(row.at("/id").asText(""), row);
        }
        return result;
    }

    /** 当前主题详情：实际工具调用及数据字典都从 BI 服务实时读取，不在 Agent 进程内缓存。 */
    protected JsonNode info(int themeId) {
        Map<String, Object> param = new LinkedHashMap<>();
        param.put("id", themeId);
        JsonNode info = RpcUtil.data(biRpc.post("/dataTheme/info", param), false);
        if (null == info || info.isNull() || info.isMissingNode()) {
            return null;
        }
        return info;
    }

}
