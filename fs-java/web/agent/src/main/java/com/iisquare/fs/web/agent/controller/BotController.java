package com.iisquare.fs.web.agent.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.web.agent.service.BotService;
import com.iisquare.fs.web.agent.service.ProjectShareService;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * 本地客户端（桌面端等）接口，权限键为 agent:bot:。
 *
 * 只放客户端自用的能力（模型清单、模型对话、项目分享）：编排（agentic）、模型对比（compare）
 * 等其它功能的接口与实现都不在这里改动，客户端也不必借道它们的权限。
 */
@RestController
@RequestMapping("/bot")
public class BotController extends PermitControllerBase {

    @Autowired
    BotService botService;
    /** 项目分享：客户端发布与浏览共用管理端那套服务，可见范围由服务端按分享人与授权角色收敛 */
    @Autowired
    ProjectShareService projectShareService;
    @Autowired
    DefaultRbacService rbacService;

    /**
     * 可用模型清单：返回 `{ rows: [{ name, ownedBy }] }`，供客户端选择模型
     */
    @RequestMapping("/models")
    @Permission("")
    public String modelsAction() {
        return ApiUtil.echoResult(botService.models());
    }

    /**
     * 当前登录用户的积分概览：`{ available, status, remained, consumed, intervals: [...] }`。
     * 总消耗与剩余来自用户积分记录，每个 interval 内是该限流周期的已用量、剩余与下次重置时间。
     */
    @RequestMapping("/credit")
    @Permission("")
    public String creditAction(HttpServletRequest request) {
        return ApiUtil.echoResult(botService.credit(request));
    }

    /**
     * 模型对话（SSE）：请求体 `{ model, systemPrompt, messages: [{ role, content }], ... }`，
     * 事件为 `{ type: delta | error, data }`
     */
    @RequestMapping("/chat")
    @Permission("")
    public SseEmitter chatAction(@RequestBody String body, HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        return botService.chat(body, request, response);
    }

    /**
     * 工具定义清单：请求体 `{ tools: [{ kind, id, name, toolId, methodName, args }] }`，
     * 返回 `{ rows: [{ ref, function }] }`，客户端把 function 组装成 OpenAI 的 tools 清单；
     * args 是项目里配的固定参数（执行变量绑定），固定的参数不会出现在 function 定义里
     */
    @RequestMapping("/tools")
    @Permission("")
    public String toolsAction(@RequestBody String body) {
        return ApiUtil.echoResult(botService.tools(body));
    }

    /**
     * 执行一次服务端工具调用：请求体 `{ tool: { kind, id, ..., args }, args: {} }`，
     * 返回 `{ data: <工具返回> }`；本地文件工具由客户端自己执行，不走这里
     */
    @RequestMapping("/tool")
    @Permission("")
    public String toolAction(@RequestBody String body, HttpServletRequest request) {
        return ApiUtil.echoResult(botService.tool(body, rbacService.uid(request), request));
    }

    /**
     * 客户端上传本机文件：multipart（`file`，可选 `name`），服务端转存文件服务后返回文件标识等信息。
     * 客户端拿 id 即可引用该文件（如交给 multimodalAnalyze 等工具方法分析）——
     * 服务端读不到客户端的磁盘，本机路径进不了工具方法，所以本地文件要先经这里传一次。
     */
    @PostMapping("/upload")
    @Permission("")
    public String uploadAction(@RequestPart("file") MultipartFile file, @RequestParam Map<String, Object> param) {
        return ApiUtil.echoResult(botService.upload(file, param));
    }

    /**
     * 我分享过的项目：入参 `{ name?, page?, pageSize? }`，
     * 出参 `{ page, pageSize, total, rows: [{ id, name, icon, description, status, versionCount, latestVersion }] }`。
     * 客户端再次分享时从这里选择目标项目（继续追加版本），也用于回看历史版本。
     */
    @RequestMapping("/project/mine")
    @Permission("")
    public String projectMineAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        return ApiUtil.echoResult(projectShareService.mine(param, request));
    }

    /**
     * 浏览分享的项目：入参 `{ name?, page?, pageSize? }`，只返回「已启用 + 授权命中」的项目，
     * 出参同上并带上 `mine` 标记；客户端据此导出别人的项目配置或创建本地项目
     */
    @RequestMapping("/project/browse")
    @Permission("")
    public String projectBrowseAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        return ApiUtil.echoResult(projectShareService.browse(param, request));
    }

    /**
     * 某个分享项目的版本清单：入参 `{ projectId }`，
     * 出参 `{ project, rows: [{ id, projectId, name, status, description, contentSize, createdTime }] }`，
     * 不含项目配置原文（按需经 /bot/project/version 取单个版本）
     */
    @RequestMapping("/project/versions")
    @Permission("")
    public String projectVersionsAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        return ApiUtil.echoResult(projectShareService.versions(param, request));
    }

    /**
     * 单个版本详情：入参 `{ id }`，出参 `{ project, version: { ..., content } }`，
     * content 是该项目配置（JSON 对象）——客户端按它导出配置文件或创建本地项目
     */
    @RequestMapping("/project/version")
    @Permission("")
    public String projectVersionAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        return ApiUtil.echoResult(projectShareService.version(param, request));
    }

    /**
     * 发布一个版本：入参
     * `{ projectId, name, icon, description, version, versionDescription, content }`，
     * projectId 为 0 时按名称新建分享项目，否则在指定项目下追加版本（只有分享人本人可以追加）。
     * 出参 `{ project, version }`，客户端据此记下「这个本地项目分享到了哪个项目、最新版本是什么」
     */
    @RequestMapping("/project/publish")
    @Permission("")
    public String projectPublishAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        return ApiUtil.echoResult(projectShareService.publish(param, request));
    }

}
