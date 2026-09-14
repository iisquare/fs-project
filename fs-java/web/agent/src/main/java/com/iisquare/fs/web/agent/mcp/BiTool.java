package com.iisquare.fs.web.agent.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.web.util.RpcUtil;
import com.iisquare.fs.web.core.rpc.BIRpc;
import com.iisquare.fs.web.agent.ai.ToolParam;
import feign.Response;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 商业智能 (BI) 服务工具，通过BI服务RPC接口获取数据主题信息并执行数据集查询。
 */
@Service
public class BiTool {

    @Autowired
    BIRpc biRpc;

    @Tool(description = "获取数据主题(DataTheme)列表，返回分页数据，可指定名称、状态及分页条件")
    public String dataThemeList(
            @ToolParam(name = "name", description = "主题名称，支持模糊匹配", required = false) String name,
            @ToolParam(name = "status", description = "主题状态：1-启用，2-禁用", required = false) Integer status,
            @ToolParam(name = "page", description = "页码，从1开始，默认1", required = false) Integer page,
            @ToolParam(name = "pageSize", description = "每页条数，默认15，最大500", required = false) Integer pageSize) {
        Map<String, Object> param = new LinkedHashMap<>();
        param.put("page", page);
        param.put("pageSize", pageSize);
        if (!DPUtil.empty(name)) {
            param.put("name", name);
        }
        if (null != status) {
            param.put("status", status);
        }
        return post("/dataTheme/list", param);
    }

    @Tool(description = "获取数据主题(DataTheme)详情，包含数据集信息及字段关联配置")
    public String dataThemeInfo(
            @ToolParam(name = "id", description = "数据主题主键") Integer id) {
        Map<String, Object> param = new LinkedHashMap<>();
        param.put("id", id);
        return post("/dataTheme/info", param);
    }

    @Tool(description = "执行数据集查询(datasetQuery)，仅允许查询已发布的数据集，SQL中的数据集以表名引用")
    public String datasetQuery(
            @ToolParam(name = "sql", description = "待执行的SQL查询语句，例如：SELECT * FROM demo_dataset") String sql,
            @ToolParam(name = "limit", description = "返回的最大行数，默认10，最大10000", required = false) Integer limit,
            @ToolParam(name = "timeout", description = "查询超时秒数，默认15，最大3600", required = false) Integer timeout,
            @ToolParam(name = "explain", description = "是否仅返回执行计划而不执行查询", required = false) Boolean explain) {
        Map<String, Object> param = new LinkedHashMap<>();
        param.put("sql", sql);
        if (null != limit) param.put("limit", limit);
        if (null != timeout) param.put("timeout", timeout);
        if (null != explain) param.put("explain", explain);
        return get("/dataset/query", param);
    }

    private String post(String uri, Map<String, Object> param) {
        return call(biRpc.post(uri, param));
    }

    private String get(String uri, Map<String, Object> param) {
        return call(biRpc.get(uri, param));
    }

    private String call(Response response) {
        try {
            String body = RpcUtil.string(response);
            if (null == body) {
                return ApiUtil.echoResult(4501, "商业智能服务无响应", null);
            }
            JsonNode json = DPUtil.parseJSON(body);
            return DPUtil.stringify(null == json ? body : json);
        } catch (Exception e) {
            return ApiUtil.echoResult(4500, "调用商业智能服务失败", e.getMessage());
        }
    }

}
