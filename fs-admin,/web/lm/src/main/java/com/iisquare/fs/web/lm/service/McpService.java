package com.iisquare.fs.web.lm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.lm.dao.McpDao;
import com.iisquare.fs.web.lm.entity.Mcp;
import com.iisquare.fs.web.lm.mvc.Configuration;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class McpService extends JPAServiceBase {

    @Autowired
    private McpDao mcpDao;
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

    public Mcp info(Integer id) {
        return info(mcpDao, id);
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if(DPUtil.empty(name)) return ApiUtil.result(1001, "服务名称不能为空", name);
        String url = DPUtil.trim(DPUtil.parseString(param.get("url")));
        if(DPUtil.empty(url)) return ApiUtil.result(1002, "调用地址不能为空", url);
        int status = DPUtil.parseInt(param.get("status"));
        if(!status().containsKey(status)) return ApiUtil.result(1003, "状态异常", status);
        Mcp info;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new Mcp();
        }
        info.setName(name);
        info.setUrl(url);
        info.setContent(DPUtil.stringify(param.get("content")));
        info.setSort(DPUtil.parseInt(param.get("sort")));
        info.setStatus(status);
        info.setDescription(DPUtil.parseString(param.get("description")));
        info = save(mcpDao, info, rbacService.uid(request));
        return ApiUtil.result(0, null, info);
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(mcpDao, param, (root, query, cb) -> {
            SpecificationHelper<Mcp> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.dateFormat(configuration.getFormatDate()).equalWithIntGTZero("id");
            helper.equalWithIntNotEmpty("status").like("name").like("url");
            return cb.and(helper.predicates());
        }, Sort.by(Sort.Order.desc("sort")), "id", "status", "sort");
        JsonNode rows = format(ApiUtil.rows(result));
        if(!DPUtil.empty(args.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(args.get("withStatusText"))) {
            fillStatus(rows, status());
        }
        return result;
    }

    public JsonNode format(JsonNode rows) {
        for (JsonNode row : rows) {
            ObjectNode node = (ObjectNode) row;
            node.replace("content", DPUtil.parseJSON(row.at("/content").asText()));
        }
        return rows;
    }

    public boolean remove(List<Integer> ids) {
        return remove(mcpDao, ids);
    }

    public Map<String, Object> info(Map<?, ?> param) {
        Mcp info = info(DPUtil.parseInt(param.get("id")));
        if (null == info) return ApiUtil.result(1404, "信息不存在", null);
        ObjectNode result = (ObjectNode) DPUtil.toJSON(info);
        try (McpSyncClient client = McpClient.sync(HttpClientSseClientTransport.builder(info.getUrl()).build()).build()) {
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
        }
    }

}
