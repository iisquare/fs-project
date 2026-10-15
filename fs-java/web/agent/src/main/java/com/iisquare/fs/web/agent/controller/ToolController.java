package com.iisquare.fs.web.agent.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.agent.service.ToolService;
import com.iisquare.fs.web.agent.service.ToolMethodService;
import com.iisquare.fs.web.agent.mvc.Configuration;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tool")
public class ToolController extends PermitControllerBase {

    @Autowired
    ToolService toolService;
    @Autowired
    ToolMethodService toolMethodService;
    @Autowired
    Configuration configuration;

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<String, Object> param) {
        ObjectNode result = toolService.search(param,
                DPUtil.buildMap("withUserInfo", true, "withStatusText", true, "withRoles", true, "withMethodCount", true));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/save")
    @Permission({"add", "modify"})
    public String saveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Map<String, Object> result = toolService.save(param, request);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/delete")
    @Permission
    public String deleteAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        List<Integer> ids = DPUtil.parseIntList(param.get("ids"));
        return ApiUtil.echoResult(toolService.remove(ids));
    }

    @RequestMapping("/config")
    @Permission("")
    public String configAction(ModelMap model) {
        model.put("status", toolService.status());
        model.put("types", toolService.types());
        model.put("sorts", toolService.sorts());
        model.put("methodStatus", toolMethodService.status());
        // 工具页「MCP样例」按钮用的地址：本服务自带的 MCP 服务
        model.put("mcpSampleUrl", configuration.getMcpSampleUrl());
        return ApiUtil.echoResult(0, null, model);
    }

    /**
     * 方法清单：支持单个 id 或批量 ids，供工具页与智能体编排读取（读库缓存，不再解析配置）
     */
    @RequestMapping("/methods")
    @Permission("")
    public String methodsAction(@RequestBody Map<String, Object> param) {
        Map<String, Object> result = toolMethodService.methods(param);
        return ApiUtil.echoResult(result);
    }

    /**
     * 方法检索：工具与方法都可能有大量数据，选择器按关键词分页检索（行内含工具名与参数明细），
     * 不再为了一个下拉框全量拉取所有工具的所有方法
     */
    @RequestMapping("/methodList")
    @Permission("")
    public String methodListAction(@RequestBody Map<String, Object> param) {
        ObjectNode result = toolMethodService.search(param,
                DPUtil.buildMap("withStatusText", true, "withDetail", true));
        return ApiUtil.echoResult(0, null, result);
    }

    /**
     * 解析预览：解析未保存的工具配置，返回方法清单但不落库（编辑抽屉里粘贴 / 同步后立即可见）
     */
    @RequestMapping("/parse")
    @Permission("")
    public String parseAction(@RequestBody Map<String, Object> param) {
        Map<String, Object> result = toolMethodService.parse(param);
        return ApiUtil.echoResult(result);
    }

    /**
     * 重新解析：按已保存工具的配置重解析并落库（存量工具或内容变更后手动触发）
     */
    @RequestMapping("/parseSource")
    @Permission({"add", "modify"})
    public String parseSourceAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        Map<String, Object> result = toolService.parseSource(param, request);
        return ApiUtil.echoResult(result);
    }

    /**
     * 方法测试：按工具配置真正发起一次调用（MCP tools/call 或 OpenAPI 请求），返回调用结果
     */
    @RequestMapping("/test")
    @Permission("")
    public String testAction(@RequestBody Map<String, Object> param) {
        Map<String, Object> result = toolMethodService.test(param);
        return ApiUtil.echoResult(result);
    }

    /**
     * 方法维护：启用 / 停用与排序（描述与参数由解析结果决定）
     */
    @RequestMapping("/methodSave")
    @Permission({"add", "modify"})
    public String methodSaveAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        Map<String, Object> result = toolService.saveMethod(param, request);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/mcpSync")
    @Permission("")
    public String mcpSyncAction(@RequestBody Map<String, Object> param) {
        Map<String, Object> result = toolService.mcpSync(param);
        return ApiUtil.echoResult(result);
    }

}
