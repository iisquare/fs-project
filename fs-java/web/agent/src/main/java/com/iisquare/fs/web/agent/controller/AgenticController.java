package com.iisquare.fs.web.agent.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.agent.service.AgenticService;
import com.iisquare.fs.web.agent.entity.AgenticDialog;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能体编排接口，权限键为 agent:agentic:*
 *
 * - list/info/config/run/invoke：agent:agentic:
 * - save/publish：agent:agentic:add 或 agent:agentic:modify
 * - delete：agent:agentic:delete
 */
@RestController
@RequestMapping("/agentic")
public class AgenticController extends PermitControllerBase {

    @Autowired
    AgenticService agenticService;

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<String, Object> param) {
        ObjectNode result = agenticService.search(param,
                DPUtil.buildMap("withUserInfo", true, "withStatusText", true));
        return ApiUtil.echoResult(0, null, result);
    }

    /**
     * 用户对话页可用的编排应用：已发布、状态启用，且授权角色命中当前用户
     */
    @RequestMapping("/authorized")
    @Permission("")
    public String authorizedAction(HttpServletRequest request) {
        return ApiUtil.echoResult(agenticService.authorized(request));
    }

    @RequestMapping("/info")
    @Permission("")
    public String infoAction(@RequestParam Map<?, ?> param) {
        int id = DPUtil.parseInt(param.get("id"));
        ObjectNode info = agenticService.detail(id);
        if (null == info) return ApiUtil.echoResult(404, null, id);
        return ApiUtil.echoResult(0, null, info);
    }

    @RequestMapping("/save")
    @Permission({"add", "modify"})
    public String saveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        return ApiUtil.echoResult(agenticService.save(param, request));
    }

    @RequestMapping("/publish")
    @Permission({"add", "modify"})
    public String publishAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        return ApiUtil.echoResult(agenticService.publish(param, request));
    }

    @RequestMapping("/delete")
    @Permission
    public String deleteAction(@RequestBody Map<?, ?> param) {
        List<Integer> ids = DPUtil.parseIntList(param.get("ids"));
        boolean result = agenticService.remove(ids);
        return ApiUtil.echoResult(result ? 0 : 500, null, result);
    }

    @RequestMapping("/config")
    @Permission("")
    public String configAction(ModelMap model) {
        model.put("status", agenticService.status());
        model.put("sorts", agenticService.sorts());
        return ApiUtil.echoResult(0, null, model);
    }

    @RequestMapping("/run")
    @Permission("")
    public String runAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        return ApiUtil.echoResult(agenticService.run(param, request));
    }

    /**
     * 流式调试运行：模型节点的增量内容实时推送（SSE），结束后推送运行结果
     * 事件格式：{ type: delta|done|error, data: ... }
     */
    @RequestMapping("/runStream")
    @Permission("")
    public SseEmitter runStreamAction(@RequestBody Map<?, ?> param, HttpServletRequest request,
                                      HttpServletResponse response) {
        return agenticService.runStream(param, request, response);
    }

    /**
     * 调试运行的文件上传：走文件服务存储，返回文件标识、原始名称、类型等信息，
     * 与开始节点 files 数组的元素结构一致
     */
    @PostMapping("/upload")
    @Permission("")
    public String uploadAction(HttpServletRequest request, @RequestPart("file") MultipartFile file) {
        return ApiUtil.echoResult(agenticService.upload(file, request));
    }

    @RequestMapping("/invoke")
    @Permission("")
    public String invokeAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        return ApiUtil.echoResult(agenticService.invoke(param, request));
    }

    /**
     * 流式外部调用：模型增量实时推送（SSE），供用户对话页展示流式输出
     * 事件格式：{ type: delta|step|done|error, data: ... }
     */
    @RequestMapping("/invokeStream")
    @Permission("")
    public SseEmitter invokeStreamAction(@RequestBody Map<?, ?> param, HttpServletRequest request,
                                         HttpServletResponse response) {
        return agenticService.invokeStream(param, request, response);
    }

    /**
     * 运行日志列表：不返回入参、输出与步骤等大字段
     */
    @RequestMapping("/logList")
    @Permission("")
    public String logListAction(@RequestBody Map<String, Object> param) {
        ObjectNode result = agenticService.logSearch(param, DPUtil.buildMap("withUserInfo", true));
        return ApiUtil.echoResult(0, null, result);
    }

    /**
     * 运行日志详情：含入参、输出与逐节点执行步骤
     */
    @RequestMapping("/logInfo")
    @Permission("")
    public String logInfoAction(@RequestBody Map<String, Object> param) {
        ObjectNode info = agenticService.logInfo(DPUtil.parseInt(param.get("id")));
        if (null == info) return ApiUtil.echoResult(1404, "日志不存在", null);
        return ApiUtil.echoResult(0, null, info);
    }

    /**
     * 删除运行日志
     */
    @RequestMapping("/logDelete")
    @Permission("delete")
    public String logDeleteAction(@RequestBody Map<String, Object> param) {
        List<Integer> ids = DPUtil.parseIntList(param.get("ids"));
        boolean result = agenticService.logRemove(ids);
        return ApiUtil.echoResult(result ? 0 : 500, null, result);
    }

    /**
     * 会话列表：调试运行与发布应用的对话历史，支持按标题检索
     */
    @RequestMapping("/chatList")
    @Permission("")
    public String chatListAction(@RequestBody Map<String, Object> param) {
        ObjectNode result = agenticService.chatSearch(param, DPUtil.buildMap("withUserInfo", true));
        return ApiUtil.echoResult(0, null, result);
    }

    /**
     * 会话详情：消息列表与每轮运行日志，可继续对话（继续调用 run / invoke 并带上 chatId）
     */
    @RequestMapping("/chatInfo")
    @Permission("")
    public String chatInfoAction(@RequestBody Map<String, Object> param) {
        ObjectNode info = agenticService.chatInfo(DPUtil.parseInt(param.get("id")));
        if (null == info) return ApiUtil.echoResult(1404, "会话不存在", null);
        return ApiUtil.echoResult(0, null, info);
    }

    /**
     * 删除会话：连同消息与运行日志一起清理
     */
    @RequestMapping("/chatDelete")
    @Permission("delete")
    public String chatDeleteAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        List<Integer> ids = DPUtil.parseIntList(param.get("ids"));
        boolean result = agenticService.chatRemove(ids, request);
        return ApiUtil.echoResult(result ? 0 : 500, null, result);
    }

    /**
     * 消息反馈：对助手回复点赞/点踩（可附标签与说明），再次提交同一情绪表示取消
     */
    @RequestMapping("/chatFeedback")
    @Permission("")
    public String chatFeedbackAction(@RequestBody Map<String, Object> param) {
        AgenticDialog dialog = agenticService.chatFeedback(DPUtil.parseInt(param.get("id")),
                DPUtil.parseString(param.get("emotion")), DPUtil.parseString(param.get("tag")),
                DPUtil.parseString(param.get("content")));
        if (null == dialog) return ApiUtil.echoResult(1404, "消息不存在", null);
        ObjectNode data = DPUtil.objectNode();
        data.put("id", dialog.getId());
        data.put("feedbackEmotion", DPUtil.parseString(dialog.getFeedbackEmotion()));
        data.put("feedbackTag", DPUtil.parseString(dialog.getFeedbackTag()));
        data.put("feedbackContent", DPUtil.parseString(dialog.getFeedbackContent()));
        data.put("feedbackTime", null == dialog.getFeedbackTime() ? 0L : dialog.getFeedbackTime());
        return ApiUtil.echoResult(0, null, data);
    }

    /**
     * 流程统计：会话数量与对话轮次按时间轴聚合，另出流程排名与用户排名
     * 入参：beginTime / endTime（毫秒）、agenticId、uid、type（draft / published）、
     * deleted（only 已删除 / without 未删除 / 空 全部）、status（success 成功 / failed 失败 / 空 全部）、
     * aggregation（hour / day / week / month）
     * 区间上限：整体 1 年；按小时的区间不超过 31 天
     */
    @RequestMapping("/statistic")
    @Permission("")
    public String statisticAction(@RequestBody(required = false) Map<String, Object> param) {
        return ApiUtil.echoResult(agenticService.statistic(null == param ? new HashMap<>() : param));
    }

}
