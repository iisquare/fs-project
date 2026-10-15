package com.iisquare.fs.web.agent.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.agent.service.CompareService;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模型对比调试接口。
 *
 * 权限键为 agent:compare:（控制器级）：`@Permission("")` 即「包名:控制器名:空」，
 * 本页的读、写、删都是同一个功能面，且记录与工作区在服务层按用户隔离，
 * 所以一个控制器级资源足够，不必按 action 再拆。
 */
@RestController
@RequestMapping("/compare")
public class CompareController extends PermitControllerBase {

    @Autowired
    CompareService compareService;

    /**
     * 单模型流式推理：一次请求只跑一个模型（参数逐模型独立），由页面并发发起
     * 事件格式：{ action: choices.message | error.message | error.throwable, data: ... }
     */
    @RequestMapping("/stream")
    @Permission("")
    public SseEmitter streamAction(@RequestBody String body, HttpServletRequest request, HttpServletResponse response) throws Exception {
        ObjectNode json = (ObjectNode) DPUtil.parseJSON(body);
        return compareService.stream(json, request, response);
    }

    /**
     * 可调试的模型清单：取自模型网关，供页面下拉与手输
     */
    @RequestMapping("/models")
    @Permission("")
    public String modelsAction() {
        return ApiUtil.echoResult(compareService.models());
    }

    /**
     * 可见的工作区列表：我的 + 别人共享出来的（带 mine 标记区分）
     */
    @RequestMapping("/workspaceList")
    @Permission("")
    public String workspaceListAction(HttpServletRequest request) {
        return ApiUtil.echoResult(0, null, compareService.workspaceList(request));
    }

    /**
     * 打开某个工作区：自己的或别人共享出来的都能看，含模型参数与全局设置
     */
    @RequestMapping("/workspaceInfo")
    @Permission("")
    public String workspaceInfoAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        ObjectNode info = compareService.workspaceInfo(DPUtil.parseInt(param.get("id")), request);
        if (null == info) return ApiUtil.echoResult(1404, "工作区不存在", null);
        return ApiUtil.echoResult(0, null, info);
    }

    /**
     * 保存工作区：带 id 是更新，不带是新建；只能保存自己的
     */
    @RequestMapping("/workspaceSave")
    @Permission("")
    public String workspaceSaveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        return ApiUtil.echoResult(compareService.workspaceSave(param, request));
    }

    /**
     * 共享 / 收回共享：只有属主能动；共享后所有人可见，但只有属主能改
     */
    @RequestMapping("/workspaceShare")
    @Permission("")
    public String workspaceShareAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        List<Integer> ids = DPUtil.parseIntList(param.get("ids"));
        boolean shared = DPUtil.parseBoolean(param.get("shared"));
        boolean result = compareService.workspaceShare(ids, shared, request);
        return ApiUtil.echoResult(result ? 0 : 500, null, result);
    }

    /**
     * 删除工作区：标记删除，只作用于自己的；对比记录不受影响
     */
    @RequestMapping("/workspaceDelete")
    @Permission("")
    public String workspaceDeleteAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        List<Integer> ids = DPUtil.parseIntList(param.get("ids"));
        return ApiUtil.echoResult(compareService.workspaceDelete(ids, request));
    }

    /**
     * 运行记录列表：不返回设置、模型参数与输出等大字段
     */
    @RequestMapping("/runList")
    @Permission("")
    public String runListAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        return ApiUtil.echoResult(0, null, compareService.runSearch(param, request));
    }

    /**
     * 运行记录详情：含设置、各模型参数与输出，用于整条恢复
     */
    @RequestMapping("/runInfo")
    @Permission("")
    public String runInfoAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        ObjectNode info = compareService.runInfo(DPUtil.parseInt(param.get("id")), request);
        if (null == info) return ApiUtil.echoResult(1404, "记录不存在", null);
        return ApiUtil.echoResult(0, null, info);
    }

    /**
     * 保存一次运行记录：中断与失败同样入档
     */
    @RequestMapping("/runSave")
    @Permission("")
    public String runSaveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        return ApiUtil.echoResult(compareService.runSave(param, request));
    }

    /**
     * 删除运行记录：标记删除，只作用于自己的记录
     */
    @RequestMapping("/runDelete")
    @Permission("")
    public String runDeleteAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        List<Integer> ids = DPUtil.parseIntList(param.get("ids"));
        return ApiUtil.echoResult(compareService.runRemove(ids, request));
    }

    /**
     * 清空我的全部运行记录（列表有分页，页面拿不到全部 id，所以单独开一个口子）
     */
    @RequestMapping("/runClear")
    @Permission("")
    public String runClearAction(HttpServletRequest request) {
        return ApiUtil.echoResult(compareService.runClear(request));
    }

    /**
     * 模型输出反馈：赞 / 踩，按 callId 定位某一次输出；再次提交同一情绪表示取消
     */
    @RequestMapping("/feedback")
    @Permission("")
    public String feedbackAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        return ApiUtil.echoResult(compareService.feedback(param, request));
    }

    /**
     * 模型统计：调用量按模型、按用户各一张，模型评分来自反馈
     */
    @RequestMapping("/statistic")
    @Permission("")
    public String statisticAction(@RequestBody(required = false) Map<String, Object> param) {
        return ApiUtil.echoResult(0, null, compareService.statistic(null == param ? new HashMap<>() : param));
    }

}
