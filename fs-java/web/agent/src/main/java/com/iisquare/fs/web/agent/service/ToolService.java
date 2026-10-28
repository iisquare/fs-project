package com.iisquare.fs.web.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.*;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.agent.dao.ToolDao;
import com.iisquare.fs.web.agent.entity.Tool;
import com.iisquare.fs.web.agent.mvc.Configuration;
import com.iisquare.fs.web.agent.mcp.McpHttpClient;
import com.iisquare.fs.web.agent.mcp.ToolSessionSupport;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ToolService extends JPAServiceBase {

    @Autowired
    ToolDao toolDao;
    @Autowired
    DefaultRbacService rbacService;
    @Autowired
    Configuration configuration;
    @Autowired
    ToolMethodService toolMethodService;
    @Autowired
    ToolSessionSupport toolSessionSupport;

    /** MCP 同步超时（毫秒）：与工具方法调用同一套配置，连不上要尽快失败并把原因交给页面 */
    @Value("${fs.agent.tool.connectTimeout:10000}")
    private int toolConnectTimeout;
    @Value("${fs.agent.tool.readTimeout:60000}")
    private int toolReadTimeout;


    @Override
    public Map<String, String> sorts() {
        Map<String, String> sorts = new LinkedHashMap<>();
        sorts.put("id", "desc");
        sorts.put("status", "asc");
        sorts.put("sort", "desc");
        return sorts;
    }

    public Map<?, ?> status() {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "启用");
        status.put(2, "禁用");
        return status;
    }

    public Map<String, String> types() {
        Map<String, String> types = new LinkedHashMap<>();
        types.put("schema", "自定义");
        types.put("mcp", "MCP服务");
        return types;
    }

    public Tool info(Integer id) {
        return info(toolDao, id);
    }

    public Map<String, Object> info(Map<?, ?> param) {
        Tool info = info(DPUtil.parseInt(param.get("id")));
        if (null == info) return ApiUtil.result(1404, "信息不存在", null);
        JsonNode node = DPUtil.firstNode(format(DPUtil.toArrayNode(info)));
        return ApiUtil.result(0, null, node);
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if(DPUtil.empty(name)) return ApiUtil.result(1001, "工具名称异常", name);
        String type = DPUtil.parseString(param.get("type"));
        if (!types().containsKey(type)) return ApiUtil.result(1002, "工具类型异常", type);
        int status = DPUtil.parseInt(param.get("status"));
        if(!status().containsKey(status)) return ApiUtil.result(1003, "状态异常", status);
        String header = DPUtil.stringify(param.get("header"));
        if (DPUtil.empty(header)) return ApiUtil.result(1004, "请求头异常", header);
        String query = DPUtil.stringify(param.get("query"));
        if (DPUtil.empty(query)) return ApiUtil.result(1005, "查询参数异常", query);
        Tool info;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new Tool();
            info.setPassSession(0);
        }
        info.setName(name);
        info.setType(type);
        info.setUrl(DPUtil.parseString(param.get("url")));
        info.setHeader(header);
        info.setQuery(query);
        if (null != param.get("passSession")) {
            info.setPassSession(DPUtil.parseInt(param.get("passSession")) == 1 ? 1 : 0);
        }
        info.setLabels(DPUtil.implode(",", DPUtil.parseStringList(param.get("labels"))));
        info.setRoleIds(DPUtil.implode(",", DPUtil.parseIntList(param.get("roleIds"))));
        info.setContent(DPUtil.parseString(param.get("content")));
        info.setSort(DPUtil.parseInt(param.get("sort")));
        info.setStatus(status);
        info.setDescription(DPUtil.parseString(param.get("description")));
        info = save(toolDao, info, rbacService.uid(request));
        // 保存后立即按最新配置解析方法并落库缓存，方法行按 toolId + name upsert
        toolMethodService.sync(info, rbacService.uid(request));
        return ApiUtil.result(0, null, info);
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(toolDao, param, (root, query, cb) -> {
            SpecificationHelper<Tool> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.dateFormat(configuration.getFormatDate()).equalWithIntGTZero("id");
            helper.equalWithIntNotEmpty("status").like("name").equal("type");
            return cb.and(helper.predicates());
        }, Sort.by(Sort.Order.desc("sort"), Sort.Order.desc("id")), sorts().keySet());
        JsonNode rows = format(ApiUtil.rows(result));
        if(!DPUtil.empty(args.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(args.get("withStatusText"))) {
            fillStatus(rows, status());
            DPUtil.fillValues(rows, "type", "typeText", types());
        }
        if (!DPUtil.empty(args.get("withRoles"))) {
            rbacService.fillInfos(rows);
        }
        if (!DPUtil.empty(args.get("withMethodCount"))) {
            for (JsonNode row : rows) {
                int toolId = row.at("/id").asInt(0);
                ((ObjectNode) row).put("methodCount", toolId > 0 ? toolMethodService.all(toolId).size() : 0);
            }
        }
        return result;
    }

    public JsonNode format(JsonNode rows) {
        for (JsonNode row : rows) {
            ObjectNode node = (ObjectNode) row;
            node.replace("header", DPUtil.parseJSON(node.at("/header").asText(), k -> DPUtil.objectNode()));
            node.replace("query", DPUtil.parseJSON(node.at("/query").asText(), k -> DPUtil.objectNode()));
            List<String> labels = DPUtil.parseStringList(node.at("/labels").asText(""));
            node.replace("labels", DPUtil.toJSON(labels));
            List<Integer> ids = DPUtil.parseIntList(node.at("/roleIds").asText(""));
            node.replace("roleIds", DPUtil.toJSON(ids));
        }
        return rows;
    }

    public Map<String, Object> remove(List<Integer> ids) {
        if (null == ids || ids.isEmpty()) return ApiUtil.result(0, null, 0);
        try {
            // 工具删除时连同其方法一起清理，避免残留方法行被引用
            ids.forEach(id -> toolMethodService.removeByToolId(id));
        } catch (Exception e) {
            return ApiUtil.result(1500, "删除工具方法失败", e.getMessage());
        }
        return remove(toolDao, ids);
    }

    /**
     * 重新解析：按已保存工具的配置重解析方法并落库（存量工具或内容变更后手动触发）
     */
    public Map<String, Object> parseSource(Map<?, ?> param, HttpServletRequest request) {
        Tool info = info(DPUtil.parseInt(param.get("id")));
        if (null == info) return ApiUtil.result(1404, "工具不存在", null);
        return toolMethodService.sync(info, rbacService.uid(request));
    }

    /**
     * 方法维护：启用 / 停用与排序（描述与参数由解析结果决定）
     */
    public Map<String, Object> saveMethod(Map<?, ?> param, HttpServletRequest request) {
        return toolMethodService.save(param, rbacService.uid(request));
    }

    public Map<String, Object> mcpSync(Map<String, Object> param, HttpServletRequest request) {
        String url = DPUtil.parseString(param.get("url"));
        if (DPUtil.empty(url)) return ApiUtil.result(1001, "服务地址异常", url);
        ObjectNode result = DPUtil.objectNode();
        result.put("url", url);
        String endpoint;
        try {
            endpoint = HttpUtil.buildUrlWithQueryString(url, DPUtil.toJSON(param.get("query"), Map.class));
        } catch (UnsupportedEncodingException e) {
            return ApiUtil.result(1002, "解析查询参数异常", e.getMessage());
        }
        Map<String, String> headers = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> entry : DPUtil.toJSON(param.get("header")).properties()) {
            headers.put(entry.getKey(), entry.getValue().asText());
        }
        // 与工具方法调用同一口径：是否传调用方会话只看工具配置开关
        boolean passSession = DPUtil.parseInt(param.get("passSession")) == 1;
        headers = toolSessionSupport.headers(headers, passSession, request);
        // 客户端按地址自动判别传输：SSE 端点（/sse）与流式 HTTP 端点（/mcp）都能同步
        try (McpHttpClient client = new McpHttpClient(endpoint, headers, toolConnectTimeout, toolReadTimeout)) {
            client.initialize();
            JsonNode capabilities = client.serverCapabilities();
            result.replace("tools", capabilities.has("tools") ? client.listTools() : DPUtil.arrayNode());
            result.replace("resources", capabilities.has("resources") ? client.listResources() : DPUtil.arrayNode());
            result.replace("prompts", capabilities.has("prompts") ? client.listPrompts() : DPUtil.arrayNode());
            result.set("serverInfo", client.serverInfo());
            if (result.at("/tools").isEmpty()) {
                return ApiUtil.result(1003, "该 MCP 服务未返回工具方法：能力声明里没有 tools，或 tools 清单为空", result);
            }
            return ApiUtil.result(0, null, result);
        } catch (Exception e) {
            String reason = DPUtil.parseString(e.getMessage());
            if (DPUtil.empty(reason)) reason = e.getClass().getSimpleName();
            return ApiUtil.result(1500, "获取MCP服务信息失败", reason + "（服务地址：" + url + "）");
        }
    }

}
