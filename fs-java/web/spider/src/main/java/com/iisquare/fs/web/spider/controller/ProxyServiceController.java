package com.iisquare.fs.web.spider.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.spider.entity.ProxyGroup;
import com.iisquare.fs.web.spider.service.ProxyGroupService;
import com.iisquare.fs.web.spider.service.ProxyLeaseService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 代理服务接口。管理统计走鉴权会话，租赁接口按分组分别对外提供服务。
 */
@RestController
@RequestMapping("/proxy")
public class ProxyServiceController extends PermitControllerBase {

    @Autowired
    ProxyGroupService groupService;
    @Autowired
    ProxyLeaseService leaseService;

    @GetMapping("/stats")
    @Permission("spider:proxy:stats")
    public String statsAction(@RequestParam Map<String, String> param) {
        return ApiUtil.echoResult(leaseService.stats(param.get("dimension"), param.get("date"),
                DPUtil.parseInt(param.get("groupId")), DPUtil.parseInt(param.get("channelId"))));
    }

    @PostMapping("/service/{code}/acquire")
    public String acquireAction(@PathVariable String code, @RequestBody Map<?, ?> param,
                                HttpServletRequest request) {
        ProxyGroup group = authorized(code, param, request);
        if (null == group) return ApiUtil.echoResult(9403, "分组不存在或访问密钥无效", null);
        int ttl = DPUtil.parseInt(param.get("stateTtl"));
        String stateKey = DPUtil.parseString(param.get("stateKey"));
        return ApiUtil.echoResult(leaseService.acquire(group,
                stateKey,
                DPUtil.parseString(param.get("clientId")),
                DPUtil.parseString(param.get("taskId")),
                DPUtil.parseBoolean(param.get("sticky")),
                !DPUtil.empty(stateKey), ttl));
    }

    @PostMapping("/service/{code}/heartbeat")
    public String heartbeatAction(@PathVariable String code, @RequestBody Map<?, ?> param,
                                  HttpServletRequest request) {
        ProxyGroup group = authorized(code, param, request);
        if (null == group) {
            return ApiUtil.echoResult(9403, "分组不存在或访问密钥无效", null);
        }
        return ApiUtil.echoResult(leaseService.keepalive(
                DPUtil.parseString(param.get("leaseId")), group.getId()));
    }

    @PostMapping("/service/{code}/release")
    public String releaseAction(@PathVariable String code, @RequestBody Map<?, ?> param,
                                HttpServletRequest request) {
        ProxyGroup group = authorized(code, param, request);
        if (null == group) {
            return ApiUtil.echoResult(9403, "分组不存在或访问密钥无效", null);
        }
        boolean success = DPUtil.empty(param.get("success")) || DPUtil.parseBoolean(param.get("success"));
        return ApiUtil.echoResult(leaseService.release(DPUtil.parseString(param.get("leaseId")),
                DPUtil.parseBoolean(param.get("keepState")), group.getId(),
                DPUtil.toJSON(param.get("state")), success));
    }

    @GetMapping("/service/{code}/stats")
    public String groupStatsAction(@PathVariable String code, @RequestParam Map<String, String> param,
                                   HttpServletRequest request) {
        if (null == authorized(code, param, request)) {
            return ApiUtil.echoResult(9403, "分组不存在或访问密钥无效", null);
        }
        ProxyGroup group = groupService.byCode(code);
        return ApiUtil.echoResult(leaseService.stats(
                DPUtil.empty(param.get("dimension")) ? "group" : param.get("dimension"),
                param.get("date"), group.getId(), null));
    }

    private ProxyGroup authorized(String code, Map<?, ?> param, HttpServletRequest request) {
        ProxyGroup group = groupService.byCode(code);
        if (null == group || !Integer.valueOf(1).equals(group.getStatus())) return null;
        String provided = DPUtil.parseString(param.get("accessKey"));
        if (DPUtil.empty(provided)) provided = request.getHeader("X-Proxy-Access-Key");
        if (DPUtil.empty(group.getAccessKey()) || !group.getAccessKey().equals(provided)) return null;
        return group;
    }

}
