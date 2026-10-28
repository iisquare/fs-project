package com.iisquare.fs.web.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.HttpUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.agent.dao.ToolDao;
import com.iisquare.fs.web.agent.dao.ToolMethodDao;
import com.iisquare.fs.web.agent.entity.Tool;
import com.iisquare.fs.web.agent.entity.ToolMethod;
import com.iisquare.fs.web.agent.mcp.McpHttpClient;
import com.iisquare.fs.web.agent.mcp.ToolSessionSupport;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 工具方法：由工具配置解析而来，落库缓存，供页面展示、编排引用与运行调用。
 *
 * - 解析：schema 类型用 swagger-parser 完整解析 OpenAPI（JSON / YAML），mcp 类型取同步结果里的 tools；
 * - 落库：按 toolId + name upsert，本次未出现的方法标记 present=0（软失效），人工改过的 status/sort 不覆盖；
 * - 参数：只落方法级参数（OpenAPI 的 parameters 与 requestBody、MCP 的 inputSchema），
 *   工具级 url / header / query 只在调用时使用，不进入参数与模型 tool 定义。
 */
@Service
public class ToolMethodService extends JPAServiceBase {

    private static final Logger logger = LoggerFactory.getLogger(ToolMethodService.class);

    /**
     * 工具方法调用超时：工具是编排里的外部依赖（HTTP 接口、文件服务、BI 查询等），
     * 卡住时应当尽快失败并交给模型重试，不能拖着整轮运行；单位毫秒
     */
    @Value("${fs.agent.tool.connectTimeout:10000}")
    private int toolConnectTimeout;
    @Value("${fs.agent.tool.readTimeout:60000}")
    private int toolReadTimeout;

    @Autowired
    ToolDao toolDao;
    @Autowired
    ToolMethodDao toolMethodDao;
    @Autowired
    ToolSessionSupport toolSessionSupport;

    @Override
    public Map<String, String> sorts() {
        Map<String, String> sorts = new LinkedHashMap<>();
        sorts.put("sort", "asc");
        sorts.put("id", "asc");
        return sorts;
    }

    public Map<Integer, String> status() {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "启用");
        status.put(2, "停用");
        return status;
    }

    /* ------------------------------- 解析 ------------------------------- */

    /**
     * 工具配置 → 方法清单（不落库）
     * @param type    工具类型：schema / mcp
     * @param content 配置信息：OpenAPI 文档 或 MCP 同步结果
     */
    public List<ToolMethod> parse(String type, String content) {
        if (DPUtil.empty(type)) throw new IllegalArgumentException("工具类型异常");
        if (DPUtil.empty(content)) throw new IllegalArgumentException("配置信息为空");
        List<ToolMethod> methods = "mcp".equals(type) ? parseMcp(content) : parseSchema(content);
        // 方法名唯一：同名时补序号，避免唯一键冲突
        Set<String> used = new LinkedHashSet<>();
        int sort = 0;
        for (ToolMethod method : methods) {
            method.setName(methodName(method.getOriginName(), used));
            method.setSort(++sort);
            method.setStatus(1);
            method.setPresent(1);
        }
        return methods;
    }

    protected List<ToolMethod> parseSchema(String content) {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(content, null, options);
        OpenAPI openAPI = result.getOpenAPI();
        if (null == openAPI) {
            throw new IllegalArgumentException("OpenAPI 解析失败：" + message(result.getMessages()));
        }
        List<ToolMethod> methods = new ArrayList<>();
        Paths paths = openAPI.getPaths();
        if (null == paths) return methods;
        for (Map.Entry<String, PathItem> entry : paths.entrySet()) {
            PathItem item = entry.getValue();
            if (null == item || null == item.readOperationsMap()) continue;
            for (Map.Entry<PathItem.HttpMethod, Operation> operationEntry : item.readOperationsMap().entrySet()) {
                methods.add(parseOperation(openAPI, entry.getKey(), operationEntry.getKey().name(), operationEntry.getValue()));
            }
        }
        return methods;
    }

    protected ToolMethod parseOperation(OpenAPI openAPI, String path, String verb, Operation operation) {
        if (null == operation) return null;
        List<Map<String, Object>> params = new ArrayList<>();
        if (null != operation.getParameters()) {
            for (Parameter parameter : operation.getParameters()) {
                params.add(parameter(parameter.getName(), parameter.getSchema(), parameter.getRequired(),
                        parameter.getDescription(), parameter.getIn()));
            }
        }
        if (null != operation.getRequestBody()) {
            Schema<?> schema = requestBodySchema(operation.getRequestBody().getContent());
            List<Map<String, Object>> fields = bodyFields(schema);
            if (fields.isEmpty()) {
                // 非对象请求体（原始字符串、数组等）无法拍平，保留单个 body 参数
                params.add(parameter("body", schema, operation.getRequestBody().getRequired(),
                        operation.getRequestBody().getDescription(), "body"));
            } else {
                params.addAll(fields);
            }
        }
        Map<String, Object> invoke = new LinkedHashMap<>();
        invoke.put("server", server(openAPI, operation));
        invoke.put("method", verb);
        invoke.put("path", path);
        String originName = DPUtil.empty(operation.getOperationId()) ? verb + " " + path : operation.getOperationId();
        String title = DPUtil.empty(operation.getSummary()) ? originName : operation.getSummary();
        String description = DPUtil.empty(operation.getDescription()) ? operation.getSummary() : operation.getDescription();
        return ToolMethod.builder()
                .originName(originName)
                .title(title)
                .description(DPUtil.parseString(description))
                .params(DPUtil.stringify(params))
                .invoke(DPUtil.stringify(invoke))
                .parseError("")
                .build();
    }

    protected List<ToolMethod> parseMcp(String content) {
        JsonNode json = DPUtil.parseJSON(content);
        JsonNode tools = null == json ? null : json.at("/tools");
        if (null == tools || !tools.isArray()) {
            throw new IllegalArgumentException("MCP 配置里没有方法清单，请先同步 MCP 配置");
        }
        List<ToolMethod> methods = new ArrayList<>();
        for (JsonNode tool : tools) {
            String name = tool.at("/name").asText();
            if (DPUtil.empty(name)) continue;
            JsonNode schema = tool.at("/inputSchema");
            Set<String> required = new LinkedHashSet<>(DPUtil.parseStringList(DPUtil.toJSON(schema.at("/required"), Object.class)));
            List<Map<String, Object>> params = new ArrayList<>();
            JsonNode properties = schema.at("/properties");
            if (properties.isObject()) {
                Iterator<String> names = properties.fieldNames();
                while (names.hasNext()) {
                    String key = names.next();
                    JsonNode property = properties.at("/" + key);
                    params.add(parameter(key, property, required.contains(key),
                            property.at("/description").asText(""), "mcp"));
                }
            }
            Map<String, Object> invoke = new LinkedHashMap<>();
            invoke.put("name", name);
            methods.add(ToolMethod.builder()
                    .originName(name)
                    .title(name)
                    .description(tool.at("/description").asText(""))
                    .params(DPUtil.stringify(params))
                    .invoke(DPUtil.stringify(invoke))
                    .parseError("")
                    .build());
        }
        return methods;
    }

    /**
     * 请求体透明化：body 只是传输方式，调用端（模型）不需要感知。
     * 对象类型的请求体把顶层字段提升为方法参数（in=body），字段说明、必填、枚举照旧保留；
     * 非对象请求体（原始字符串、数组等）无法拍平，由调用方保留单个 body 参数。
     */
    protected List<Map<String, Object>> bodyFields(Schema<?> schema) {
        List<Map<String, Object>> fields = new ArrayList<>();
        if (null == schema || null == schema.getProperties() || schema.getProperties().isEmpty()) return fields;
        Set<String> required = null == schema.getRequired()
                ? new LinkedHashSet<>() : new LinkedHashSet<>(schema.getRequired());
        ((Map<?, ?>) schema.getProperties()).forEach((key, value) -> {
            String name = DPUtil.parseString(key);
            Schema<?> property = (Schema<?>) value;
            fields.add(parameter(name, property, required.contains(name),
                    null == property ? "" : property.getDescription(), "body"));
        });
        return fields;
    }

    protected Map<String, Object> parameter(String name, Schema<?> schema, Boolean required, String description, String in) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", DPUtil.parseString(name));
        item.put("type", schemaType(schema));
        item.put("required", Boolean.TRUE.equals(required));
        item.put("description", DPUtil.parseString(description));
        item.put("in", DPUtil.parseString(in));
        item.put("defaultValue", null);
        item.put("enum", schemaEnum(schema));
        // 结构信息：对象/数组参数带上 properties / items，前端据此生成填写模板，模型 tool 定义也用它描述参数
        item.put("schema", cleanSchema(schema, 0));
        return item;
    }

    protected Map<String, Object> parameter(String name, JsonNode property, boolean required, String description, String in) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", DPUtil.parseString(name));
        item.put("type", null == property ? "string" : property.at("/type").asText("string"));
        item.put("required", required);
        item.put("description", DPUtil.parseString(description));
        item.put("in", in);
        item.put("defaultValue", null);
        item.put("enum", new ArrayList<>());
        item.put("schema", cleanSchema(property));
        return item;
    }

    /**
     * 参数结构：只保留描述参数所需字段（type/format/description/default/enum/properties/items/required），
     * 递归展开并限制深度，避免 OpenAPI 里的大对象、循环引用与厂商扩展字段被带进落库结果
     */
    protected Map<String, Object> cleanSchema(Schema<?> schema, int depth) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (null == schema || depth > 6) return result;
        if (!DPUtil.empty(schema.getType())) result.put("type", schema.getType());
        if (!DPUtil.empty(schema.getFormat())) result.put("format", schema.getFormat());
        if (!DPUtil.empty(schema.getDescription())) result.put("description", schema.getDescription());
        if (null != schema.getDefault()) result.put("default", schema.getDefault());
        if (null != schema.getEnum()) result.put("enum", schema.getEnum());
        if (null != schema.getProperties()) {
            Map<String, Object> properties = new LinkedHashMap<>();
            ((Map<?, ?>) schema.getProperties()).forEach((key, value) ->
                    properties.put(DPUtil.parseString(key), cleanSchema((Schema<?>) value, depth + 1)));
            result.put("properties", properties);
        }
        if (null != schema.getItems()) result.put("items", cleanSchema(schema.getItems(), depth + 1));
        if (null != schema.getRequired()) result.put("required", schema.getRequired());
        return result;
    }

    /** MCP 的 inputSchema 本身就是 JSON，直接转成 Map 后清理 */
    protected Map<String, Object> cleanSchema(JsonNode schema) {
        if (null == schema || schema.isMissingNode() || schema.isNull()) return new LinkedHashMap<>();
        Map<String, Object> result = DPUtil.toJSON(schema, Map.class);
        return null == result ? new LinkedHashMap<>() : result;
    }

    protected Schema<?> requestBodySchema(io.swagger.v3.oas.models.media.Content content) {
        if (null == content) return null;
        for (MediaType mediaType : content.values()) {
            if (null != mediaType && null != mediaType.getSchema()) return mediaType.getSchema();
        }
        return null;
    }

    protected String server(OpenAPI openAPI, Operation operation) {
        List<Server> servers = operation.getServers();
        if (null == servers || servers.isEmpty()) servers = openAPI.getServers();
        if (null == servers || servers.isEmpty()) return "";
        return DPUtil.parseString(servers.get(0).getUrl());
    }

    protected String schemaType(Schema<?> schema) {
        if (null == schema) return "string";
        if (!DPUtil.empty(schema.getType())) return schema.getType();
        if (!DPUtil.empty(schema.getProperties())) return "object";
        if (!DPUtil.empty(schema.getItems())) return "array";
        return DPUtil.empty(schema.get$ref()) ? "string" : "object";
    }

    protected List<String> schemaEnum(Schema<?> schema) {
        List<String> values = new ArrayList<>();
        if (null == schema || null == schema.getEnum()) return values;
        for (Object value : schema.getEnum()) values.add(DPUtil.parseString(value));
        return values;
    }

    /** 方法名：规范化为合法函数名（模型侧要求字母数字、下划线、短横线），工具内唯一由调用方保证 */
    protected String methodName(String origin, Set<String> used) {
        String name = DPUtil.parseString(origin).replaceAll("[^0-9a-zA-Z_-]", "_");
        if (DPUtil.empty(name)) name = "method";
        if (name.length() > 64) name = name.substring(0, 60) + "_" + Integer.toHexString(DPUtil.parseString(origin).hashCode());
        String result = name;
        int index = 2;
        while (used.contains(result)) {
            result = name + "_" + index;
            index++;
        }
        used.add(result);
        return result;
    }

    /* ------------------------------- 落库 ------------------------------- */

    /** 解析工具配置并按 toolId + name upsert，返回解析结果概要 */
    public Map<String, Object> sync(Tool tool, Integer uid) {
        if (null == tool || null == tool.getId()) return ApiUtil.result(1001, "工具信息异常", null);
        List<ToolMethod> parsed;
        try {
            parsed = parse(tool.getType(), tool.getContent());
        } catch (Exception e) {
            tool.setParseError(cut(e.getMessage(), 1000));
            save(toolDao, tool, uid);
            return ApiUtil.result(1002, tool.getParseError(), null);
        }
        Map<String, ToolMethod> exists = new LinkedHashMap<>();
        all(tool.getId()).forEach(item -> exists.put(item.getName(), item));
        for (ToolMethod item : parsed) {
            ToolMethod entity = exists.remove(item.getName());
            if (null == entity) {
                entity = item;
                entity.setToolId(tool.getId());
            } else {
                entity.setOriginName(item.getOriginName());
                entity.setTitle(item.getTitle());
                entity.setDescription(item.getDescription());
                entity.setParams(item.getParams());
                entity.setInvoke(item.getInvoke());
                entity.setParseError("");
            }
            entity.setPresent(1);
            save(toolMethodDao, entity, uid);
        }
        // 本次未出现的方法：软失效，人工设置的停用状态与排序保留
        for (ToolMethod item : exists.values()) {
            item.setPresent(0);
            save(toolMethodDao, item, uid);
        }
        tool.setParseError("");
        save(toolDao, tool, uid);
        return ApiUtil.result(0, null, DPUtil.buildMap("count", parsed.size(), "invalidated", exists.size()));
    }

    /** 工具下的全部方法，按排序与主键排列 */
    public List<ToolMethod> all(Integer toolId) {
        if (null == toolId) return new ArrayList<>();
        return toolMethodDao.findAll((root, query, cb) -> {
            query.orderBy(cb.asc(root.get("sort")), cb.asc(root.get("id")));
            return cb.equal(root.get("toolId"), toolId);
        });
    }

    /** 工具删除时清理其方法行 */
    public void removeByToolId(Integer toolId) {
        List<ToolMethod> rows = all(toolId);
        if (rows.isEmpty()) return;
        toolMethodDao.deleteAll(rows);
    }

    /**
     * 方法维护：只允许改启用状态与排序（描述与参数由解析结果决定，重解析会覆盖）
     */
    public Map<String, Object> save(Map<?, ?> param, Integer uid) {
        int id = DPUtil.parseInt(param.get("id"));
        ToolMethod info = info(id);
        if (null == info) return ApiUtil.result(1404, "方法不存在", id);
        if (null != param.get("status")) {
            int status = DPUtil.parseInt(param.get("status"));
            if (!status().containsKey(status)) return ApiUtil.result(1001, "状态异常", status);
            info.setStatus(status);
        }
        if (null != param.get("sort")) info.setSort(DPUtil.parseInt(param.get("sort")));
        save(toolMethodDao, info, uid);
        return ApiUtil.result(0, null, format(DPUtil.toJSON(List.of(info))));
    }

    /** 方法参数（结构化）：编排发布时用于校验执行变量绑定并写入快照 */
    public List<Map<String, Object>> methodParams(ToolMethod method) {
        return null == method ? new ArrayList<>() : list(method.getParams());
    }

    /** 参数（对象类型）声明的字段名：模型把对象字段平铺提交时按这些名字回填 */
    protected Set<String> schemaFields(Map<String, Object> parameter) {
        Set<String> fields = new LinkedHashSet<>();
        Object schema = parameter.get("schema");
        if (!(schema instanceof Map)) return fields;
        Object properties = ((Map<?, ?>) schema).get("properties");
        if (!(properties instanceof Map)) return fields;
        ((Map<?, ?>) properties).keySet().forEach(key -> fields.add(DPUtil.parseString(key)));
        return fields;
    }

    /** 方法调用信息（结构化）：编排发布时写入快照 */
    public Map<String, Object> methodInvoke(ToolMethod method) {
        return null == method ? new LinkedHashMap<>() : map(method.getInvoke());
    }

    /**
     * 供编排运行时调用工具方法：`schema` 按 invoke 拼请求、`mcp` 走 tools/call。
     * 工具级 url / header / query 在此参与调用，方法级参数按 in 落到 query / path / header / body。
     * passSession 来自工具配置开关：调用工具服务时决定是否把调用方会话作为 x-auth-token 透传。
     */
    public Map<String, Object> invoke(String type, String url, String header, String query, String content,
                                      Integer passSession, ToolMethod method, Map<String, Object> args) {
        Tool tool = new Tool();
        tool.setId(0);
        tool.setType(type);
        tool.setUrl(url);
        tool.setHeader(header);
        tool.setQuery(query);
        tool.setContent(content);
        tool.setPassSession(null != passSession && passSession == 1 ? 1 : 0);
        return "mcp".equals(type) ? callMcp(tool, method, args) : callSchema(tool, method, args);
    }

    /** 方法清单：支持单个 id 或批量 ids（设计器一次拉多个工具的方法） */
    public Map<String, Object> methods(Map<?, ?> param) {
        List<Integer> ids = DPUtil.parseIntList(param.get("ids"));
        int id = DPUtil.parseInt(param.get("id"));
        if (id > 0 && !ids.contains(id)) ids.add(id);
        if (ids.isEmpty()) return ApiUtil.result(1001, "工具标识异常", null);
        List<ToolMethod> rows = new ArrayList<>();
        for (Integer toolId : ids) rows.addAll(all(toolId));
        return ApiUtil.result(0, null, format(DPUtil.toJSON(rows)));
    }

    /**
     * 方法检索：工具与方法都可能有大量数据，选择器按关键词分页检索，不再全量拉取。
     *
     * - 关键词命中方法名 / 原始名 / 展示名 / 描述，也命中工具名（按工具找方法比按方法名找更常见）；
     * - 行内补 toolName 供选择器按工具分组展示；
     * - withDetail 打开时附带 params / invoke 明细，选中即可用于执行变量配置，省掉二次请求。
     */
    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        String keyword = DPUtil.trim(DPUtil.parseString(param.get("name")));
        // 工具名命中的工具ID：与方法字段条件取并集；回显单个方法（带 id）时不做工具名匹配
        List<Integer> matchedToolIds = new ArrayList<>();
        if (!DPUtil.empty(keyword) && DPUtil.parseInt(param.get("id")) <= 0) {
            List<Tool> tools = toolDao.findAll((root, query, cb) -> cb.like(root.get("name"), "%" + keyword + "%"));
            for (Tool tool : tools) {
                if (matchedToolIds.size() >= 500) break; // 上限保护：命中工具过多时退化为仅按方法字段匹配
                matchedToolIds.add(tool.getId());
            }
        }
        ObjectNode result = search(toolMethodDao, param, (root, query, cb) -> {
            SpecificationHelper<ToolMethod> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.equalWithIntGTZero("id").equalWithIntGTZero("toolId")
                    .equalWithIntNotEmpty("status").equalWithIntNotEmpty("present");
            List<Predicate> predicates = new ArrayList<>(Arrays.asList(helper.predicates()));
            if (!DPUtil.empty(keyword)) {
                List<Predicate> ors = new ArrayList<>();
                ors.add(cb.or(
                        cb.like(root.get("name"), "%" + keyword + "%"),
                        cb.like(root.get("originName"), "%" + keyword + "%"),
                        cb.like(root.get("title"), "%" + keyword + "%"),
                        cb.like(root.get("description"), "%" + keyword + "%")));
                if (!matchedToolIds.isEmpty()) ors.add(root.get("toolId").in(matchedToolIds));
                predicates.add(cb.or(ors.toArray(new Predicate[0])));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, Sort.by(Sort.Order.asc("toolId"), Sort.Order.asc("sort"), Sort.Order.asc("id")), sorts().keySet());
        JsonNode rows = ApiUtil.rows(result);
        fillToolName(rows);
        if (!DPUtil.empty(args.get("withStatusText"))) fillStatus(rows, status());
        if (!DPUtil.empty(args.get("withDetail"))) format(rows);
        return result;
    }

    /** 补工具名：选择器按「工具 → 方法」分组展示需要 */
    protected JsonNode fillToolName(JsonNode rows) {
        if (null == rows) return null;
        Set<Integer> ids = new LinkedHashSet<>();
        for (JsonNode row : rows) {
            int toolId = row.at("/toolId").asInt(0);
            if (toolId > 0) ids.add(toolId);
        }
        if (ids.isEmpty()) return rows;
        Map<String, String> names = new LinkedHashMap<>();
        for (Tool tool : toolDao.findAllById(ids)) {
            names.put(String.valueOf(tool.getId()), tool.getName());
        }
        DPUtil.fillValues(rows, "toolId", "toolName", names);
        return rows;
    }

    /** 预览：解析未保存的配置，不给库 */
    public Map<String, Object> parse(Map<?, ?> param) {
        try {
            List<ToolMethod> methods = parse(DPUtil.parseString(param.get("type")), DPUtil.parseString(param.get("content")));
            return ApiUtil.result(0, null, format(DPUtil.toJSON(methods)));
        } catch (Exception e) {
            return ApiUtil.result(1002, e.getMessage(), null);
        }
    }

    /** JSON 字段转对象：params / invoke 以结构化数据返回 */
    public JsonNode format(JsonNode rows) {
        for (JsonNode row : rows) {
            ObjectNode node = (ObjectNode) row;
            node.replace("params", DPUtil.parseJSON(node.at("/params").asText(), k -> DPUtil.toJSON(new ArrayList<>())));
            node.replace("invoke", DPUtil.parseJSON(node.at("/invoke").asText(), k -> DPUtil.objectNode()));
        }
        return rows;
    }

    /* ------------------------------- 测试 ------------------------------- */

    /**
     * 方法测试：工具级 url / header / query 在此参与调用，方法级参数按 in 落到 query / path / header / body。
     * 支持已保存的工具（id + methodId / methodName）与未保存的配置（type + content + methodName）。
     */
    public Map<String, Object> test(Map<?, ?> param) {
        Tool tool = tool(param);
        if (null == tool) return ApiUtil.result(1404, "工具信息不存在", null);
        List<ToolMethod> methods;
        try {
            methods = parse(tool.getType(), tool.getContent());
        } catch (Exception e) {
            return ApiUtil.result(1002, e.getMessage(), null);
        }
        ToolMethod method = null;
        int methodId = DPUtil.parseInt(param.get("methodId"));
        String methodName = DPUtil.parseString(param.get("methodName"));
        for (ToolMethod item : methods) {
            if (methodId > 0 ? methodId == DPUtil.parseInt(item.getId()) : methodName.equals(item.getName())) {
                method = item;
                break;
            }
        }
        if (null == method && methodId > 0) {
            ToolMethod info = info(methodId);
            if (null != info && tool.getId().equals(info.getToolId())) {
                method = ToolMethod.builder().name(info.getName()).originName(info.getOriginName())
                        .params(info.getParams()).invoke(info.getInvoke()).build();
            }
        }
        if (null == method) return ApiUtil.result(1404, "方法不存在，请重新解析工具配置", methodName);
        Map<String, Object> args = DPUtil.toJSON(param.get("args"), Map.class);
        if (null == args) args = new LinkedHashMap<>();
        return "mcp".equals(tool.getType()) ? callMcp(tool, method, args) : callSchema(tool, method, args);
    }

    protected ToolMethod info(Integer id) {
        if (null == id || id <= 0) return null;
        return info(toolMethodDao, id);
    }

    /** 测试用的工具信息：请求里带了配置就用它（可测未保存的内容），否则按 id 取库 */
    protected Tool tool(Map<?, ?> param) {
        int id = DPUtil.parseInt(param.get("id"));
        String content = DPUtil.parseString(param.get("content"));
        if (id > 0 && DPUtil.empty(content)) {
            Tool info = info(toolDao, id);
            if (null != info && null != param.get("passSession")) {
                info.setPassSession(DPUtil.parseInt(param.get("passSession")) == 1 ? 1 : 0);
            }
            return info;
        }
        Tool tool = new Tool();
        tool.setId(id);
        tool.setName(DPUtil.parseString(param.get("name")));
        tool.setType(DPUtil.parseString(param.get("type")));
        tool.setUrl(DPUtil.parseString(param.get("url")));
        tool.setHeader(DPUtil.stringify(param.get("header")));
        tool.setQuery(DPUtil.stringify(param.get("query")));
        tool.setContent(content);
        tool.setPassSession(DPUtil.parseInt(param.get("passSession")) == 1 ? 1 : 0);
        return DPUtil.empty(tool.getType()) ? null : tool;
    }

    protected Map<String, Object> callMcp(Tool tool, ToolMethod method, Map<String, Object> args) {
        Map<String, String> headers = stringMap(tool.getHeader());
        String url;
        try {
            url = HttpUtil.buildUrlWithQueryString(tool.getUrl(), stringMap(tool.getQuery()));
        } catch (UnsupportedEncodingException e) {
            return ApiUtil.result(1003, "解析查询参数异常", e.getMessage());
        }
        // 是否传调用方登录会话只看工具配置开关；request 里保留配置的原始 header，实际发送使用 callHeaders
        boolean passSession = null != tool.getPassSession() && tool.getPassSession() == 1;
        Map<String, String> callHeaders = toolSessionSupport.headers(headers, passSession);
        Map<?, ?> request = DPUtil.buildMap("method", "tools/call", "url", url,
                "header", headers, "body", DPUtil.buildMap("name", method.getOriginName(), "arguments", args));
        // 与同步同一套客户端：SSE 端点与流式 HTTP 端点都能调用
        try (McpHttpClient client = new McpHttpClient(url, callHeaders, toolConnectTimeout, toolReadTimeout)) {
            client.initialize();
            JsonNode result = client.callTool(method.getOriginName(), args);
            boolean missing = null == result || result.isMissingNode() || result.isNull();
            boolean failed = missing || result.at("/isError").asBoolean(false);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("success", !failed);
            data.put("request", request);
            data.put("response", DPUtil.stringify(DPUtil.toJSON(result)));
            if (failed) {
                String error = missing ? "MCP 服务未返回工具结果" : mcpErrorText(result);
                data.put("error", error);
                logger.warn("MCP tool returned error: method={} error={}", method.getOriginName(), error);
            }
            return ApiUtil.result(0, null, data);
        } catch (Exception e) {
            // 连接、鉴权、地址等底层细节只写日志；回给模型的保持稳定、可读
            String message = "调用 MCP 方法失败，请检查服务地址、鉴权配置或稍后重试";
            logger.warn("MCP tool call failed: url={}, method={}", url, method.getOriginName(), e);
            return ApiUtil.result(0, null, DPUtil.buildMap("success", false, "request", request,
                    "error", message, "response", message));
        }
    }

    /** MCP isError 结果 → 给模型看的错误文本：优先取 content 里的 text，取不到再退回原始 JSON */
    protected String mcpErrorText(JsonNode result) {
        if (null == result) return "";
        StringBuilder text = new StringBuilder();
        JsonNode content = result.at("/content");
        if (content.isArray()) {
            for (JsonNode item : content) {
                String value = item.at("/text").asText("");
                if (DPUtil.empty(value)) continue;
                if (text.length() > 0) text.append('\n');
                text.append(value);
            }
        }
        return cut(text.length() > 0 ? text.toString() : DPUtil.stringify(result), 2000);
    }

    protected Map<String, Object> callSchema(Tool tool, ToolMethod method, Map<String, Object> args) {
        Map<String, Object> invoke = map(method.getInvoke());
        String server = DPUtil.parseString(invoke.get("server"));
        if (DPUtil.empty(server)) server = DPUtil.parseString(tool.getUrl());
        String path = DPUtil.parseString(invoke.get("path"));
        String verb = DPUtil.parseString(invoke.get("method"));
        Map<String, String> query = stringMap(tool.getQuery());
        Map<String, String> headers = stringMap(tool.getHeader());
        if (null == query) query = new LinkedHashMap<>();
        if (null == headers) headers = new LinkedHashMap<>();
        Map<String, Object> body = new LinkedHashMap<>();
        List<Map<String, Object>> params = list(method.getParams());
        Set<String> consumed = new LinkedHashSet<>();
        if (null != params) {
            for (Map<String, Object> item : params) {
                String name = DPUtil.parseString(item.get("name"));
                if (null == args || !args.containsKey(name)) continue;
                Object value = args.get(name);
                consumed.add(name);
                switch (DPUtil.parseString(item.get("in"))) {
                    case "path":
                        path = path.replace("{" + name + "}", DPUtil.parseString(value));
                        break;
                    case "header":
                        headers.put(name, DPUtil.parseString(value));
                        break;
                    case "query":
                        query.put(name, DPUtil.parseString(value));
                        break;
                    case "body":
                        if (value instanceof Map) {
                            ((Map<?, ?>) value).forEach((key, itemValue) -> body.put(DPUtil.parseString(key), itemValue));
                        } else {
                            body.put(name, value);
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        // 兼容模型把对象参数（如 body）内的字段平铺提交：按 schema 的字段名回填进对象
        if (null != params && null != args) {
            for (Map<String, Object> item : params) {
                if (!"body".equals(DPUtil.parseString(item.get("in")))) continue;
                String name = DPUtil.parseString(item.get("name"));
                Object exists = args.get(name);
                Map<String, Object> fields = exists instanceof Map
                        ? new LinkedHashMap<>((Map<String, Object>) exists) : new LinkedHashMap<>();
                for (String field : schemaFields(item)) {
                    if (fields.containsKey(field) || !args.containsKey(field)) continue;
                    fields.put(field, args.get(field));
                    consumed.add(field);
                }
                if (!fields.isEmpty()) body.putAll(fields);
            }
            // 仍未归位的平铺参数：方法存在请求体参数时直接放进请求体（避免整段丢失）
            boolean bodyable = params.stream().anyMatch(item -> "body".equals(DPUtil.parseString(item.get("in"))));
            if (bodyable) {
                for (Map.Entry<String, Object> entry : args.entrySet()) {
                    if (consumed.contains(entry.getKey())) continue;
                    if ("body".equals(entry.getKey())) continue;
                    body.put(entry.getKey(), entry.getValue());
                }
            }
        }
        String url = server + path;
        // 统一开关：schema 工具也按 passSession 决定是否把调用方会话作为 x-auth-token 发出
        boolean passSession = null != tool.getPassSession() && tool.getPassSession() == 1;
        Map<String, String> callHeaders = toolSessionSupport.headers(headers, passSession);
        Map<?, ?> request = DPUtil.buildMap("method", verb, "url", url, "query", query, "header", headers, "body", body);
        HttpURLConnection conn = null;
        try {
            boolean withBody = !("GET".equals(verb) || "DELETE".equals(verb));
            conn = (HttpURLConnection) new URL(HttpUtil.buildUrlWithQueryString(url, query)).openConnection();
            conn.setRequestMethod(verb);
            // 工具调用超时按配置走：默认连接 10s、读取 60s（原为 30s / 300s，太长的等待会让整轮对话卡住）
            conn.setConnectTimeout(toolConnectTimeout);
            conn.setReadTimeout(toolReadTimeout);
            conn.setDoInput(true);
            conn.setDoOutput(withBody);
            for (Map.Entry<String, String> entry : callHeaders.entrySet()) {
                conn.setRequestProperty(entry.getKey(), entry.getValue());
            }
            if (withBody) {
                if (null == conn.getRequestProperty("Content-Type")) {
                    conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                }
                byte[] data = DPUtil.stringify(body).getBytes(StandardCharsets.UTF_8);
                conn.setRequestProperty("Content-Length", String.valueOf(data.length));
                try (OutputStream out = conn.getOutputStream()) {
                    out.write(data);
                    out.flush();
                }
            }
            int status = conn.getResponseCode();
            InputStream stream = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
            String response = null == stream ? "" : new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            Map<String, Object> responseHeaders = new LinkedHashMap<>();
            conn.getHeaderFields().forEach((key, values) -> {
                if (null != key && null != values) responseHeaders.put(key, DPUtil.implode(", ", values));
            });
            return ApiUtil.result(0, null, DPUtil.buildMap("success", status < 400, "status", status,
                    "request", request, "header", responseHeaders, "response", response));
        } catch (Exception e) {
            String message = "调用工具失败，请检查服务地址、鉴权配置或稍后重试";
            logger.warn("Tool schema call failed: url={}, method={}", url, verb, e);
            return ApiUtil.result(0, null, DPUtil.buildMap("success", false, "request", request,
                    "error", message, "response", message));
        } finally {
            if (null != conn) conn.disconnect();
        }
    }

    /* ------------------------------- 工具方法 ------------------------------- */

    protected String message(List<String> messages) {
        if (null == messages || messages.isEmpty()) return "未知原因";
        return cut(DPUtil.implode("；", messages), 500);
    }

    /** JSON 字符串 → Map：invoke / header / query 在库里都是以 JSON 字符串保存的 */
    protected Map<String, Object> map(String json) {
        JsonNode node = DPUtil.parseJSON(json, k -> DPUtil.objectNode());
        Map<String, Object> result = DPUtil.toJSON(node, Map.class);
        return null == result ? new LinkedHashMap<>() : result;
    }

    /** JSON 字符串 → 字符串取值 Map：请求头与查询参数 */
    protected Map<String, String> stringMap(String json) {
        Map<String, Object> source = map(json);
        Map<String, String> result = new LinkedHashMap<>();
        source.forEach((key, value) -> result.put(key, DPUtil.parseString(value)));
        return result;
    }

    /** JSON 字符串 → Map 列表：方法参数 */
    protected List<Map<String, Object>> list(String json) {
        JsonNode node = DPUtil.parseJSON(json, k -> DPUtil.toJSON(new ArrayList<>()));
        List<Map<String, Object>> result = DPUtil.toJSON(node, List.class);
        return null == result ? new ArrayList<>() : result;
    }

    protected String cut(String text, int length) {
        String value = DPUtil.parseString(text);
        return value.length() <= length ? value : value.substring(0, length);
    }

}
