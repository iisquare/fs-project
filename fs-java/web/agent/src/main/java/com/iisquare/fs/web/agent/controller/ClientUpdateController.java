package com.iisquare.fs.web.agent.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.web.agent.service.ClientVersionService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 客户端启动用的公开更新接口。
 *
 * 版本检查和安装包下载不继承 PermitControllerBase：登录态失效或尚未登录时，
 * 桌面端仍可以先判断是否必须升级。安装包本身不是敏感数据，管理操作仍走受 RBAC 保护的
 * /clientVersion 接口。
 */
@RestController
@RequestMapping("/clientUpdate")
public class ClientUpdateController {

    @Autowired
    ClientVersionService clientVersionService;

    @PostMapping("/check")
    public String checkAction(@RequestBody Map<?, ?> param) {
        return ApiUtil.echoResult(clientVersionService.check(param));
    }

    /**
     * 产品介绍页使用的公开发布列表：只含启用且已全量放量的安装包。
     * 不返回文件服务标识、内部路径等维护字段。
     */
    @PostMapping("/releases")
    public String releasesAction(@RequestBody Map<?, ?> param) {
        return ApiUtil.echoResult(clientVersionService.publicReleases(param));
    }

    @GetMapping("/download")
    public String downloadAction(@RequestParam Map<String, Object> param,
            HttpServletResponse response) throws Exception {
        Map<String, Object> result = clientVersionService.downloadForClient(param);
        if (ApiUtil.failed(result)) return ApiUtil.echoResult(result);
        response.sendRedirect(ApiUtil.data(result, JsonNode.class).asText());
        return null;
    }

    @GetMapping("/delta")
    public String deltaAction(@RequestParam Map<String, Object> param) {
        return ApiUtil.echoResult(clientVersionService.delta(param));
    }

    @PostMapping("/report")
    public String reportAction(@RequestBody Map<?, ?> param) {
        return ApiUtil.echoResult(clientVersionService.report(param));
    }

}
