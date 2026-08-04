package com.iisquare.fs.web.lm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.*;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.lm.dao.ToolDao;
import com.iisquare.fs.web.lm.entity.Tool;
import com.iisquare.fs.web.lm.mvc.Configuration;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
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
        }
        info.setName(name);
        info.setType(type);
        info.setUrl(DPUtil.parseString(param.get("url")));
        info.setHeader(header);
        info.setQuery(query);
        info.setLabels(DPUtil.implode(",", DPUtil.parseStringList(param.get("labels"))));
        info.setRoleIds(DPUtil.implode(",", DPUtil.parseIntList(param.get("roleIds"))));
        info.setContent(DPUtil.parseString(param.get("content")));
        info.setSort(DPUtil.parseInt(param.get("sort")));
        info.setStatus(status);
        info.setDescription(DPUtil.parseString(param.get("description")));
        info = save(toolDao, info, rbacService.uid(request));
        return ApiUtil.result(0, null, info);
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(toolDao, param, (root, query, cb) -> {
            SpecificationHelper<Tool> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.dateFormat(configuration.getFormatDate()).equalWithIntGTZero("id");
            helper.equalWithIntNotEmpty("status").like("name").equal("type");
            return cb.and(helper.predicates());
        }, Sort.by(Sort.Order.desc("sort")), "id", "status", "sort");
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

    public boolean remove(List<Integer> ids) {
        return remove(toolDao, ids);
    }

    public Map<String, Object> mcpSync(Map<String, Object> param) {
        String url = DPUtil.parseString(param.get("url"));
        if (DPUtil.empty(url)) return ApiUtil.result(1001, "服务地址异常", url);
        ObjectNode result = DPUtil.objectNode();
        result.put("url", url);
        try {
            url = HttpUtil.buildUrlWithQueryString(url, DPUtil.toJSON(param.get("query"), Map.class));
        } catch (UnsupportedEncodingException e) {
            return ApiUtil.result(1002, "解析查询参数异常", e.getMessage());
        }
        HttpClientSseClientTransport transport = HttpClientSseClientTransport.builder(url).customizeRequest(rb -> {
            for (Map.Entry<String, JsonNode> entry : DPUtil.toJSON(param.get("header")).properties()) {
                rb.header(entry.getKey(), entry.getValue().asText());
            }
        }).build();
        McpSyncClient client = McpClient.sync(transport).build();
        try {
            client.initialize();
            McpSchema.ServerCapabilities capabilities = client.getServerCapabilities();
            if (null == capabilities.tools()) {
                result.putArray("tools");
            } else {
                McpSchema.ListToolsResult tools = client.listTools();
                result.replace("tools", DPUtil.toJSON(tools).at("/tools"));
            }
            if (null == capabilities.resources()) {
                result.putArray("resources");
            } else {
                McpSchema.ListResourcesResult resources = client.listResources();
                result.replace("resources", DPUtil.toJSON(resources).at("/resources"));
            }
            if (null == capabilities.prompts()) {
                result.putArray("prompts");
            } else {
                McpSchema.ListPromptsResult prompts = client.listPrompts();
                result.replace("prompts", DPUtil.toJSON(prompts).at("/prompts"));
            }
            return ApiUtil.result(0, null, result);
        } catch (Exception e) {
            return ApiUtil.result(1500, "获取MCP服务信息失败", e.getMessage());
        } finally {
            FileUtil.close(client);
        }
    }

}
