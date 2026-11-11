package com.iisquare.fs.web.spider.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.spider.entity.ProxyChannel;
import com.iisquare.fs.web.spider.service.ProxyChannelService;
import com.iisquare.fs.web.spider.service.ProxyLeaseService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/proxy/channel")
public class ProxyChannelController extends PermitControllerBase {

    @Autowired
    ProxyChannelService channelService;
    @Autowired
    ProxyLeaseService leaseService;

    @RequestMapping("/info")
    @Permission("spider:proxyChannel:")
    public String infoAction(@RequestBody Map<?, ?> param) {
        ProxyChannel info = channelService.info(DPUtil.parseInt(param.get("id")));
        return ApiUtil.echoResult(null == info ? 404 : 0, null, info);
    }

    @RequestMapping("/list")
    @Permission("spider:proxyChannel:")
    public String listAction(@RequestBody Map<String, Object> param) {
        ObjectNode result = channelService.search(param, DPUtil.buildMap(
                "withUserInfo", true, "withStatusText", true, "withAdapterText", true));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/config")
    @Permission("spider:proxyChannel:")
    public String configAction() {
        return ApiUtil.echoResult(channelService.config());
    }

    @RequestMapping("/save")
    @Permission({"spider:proxyChannel:add", "spider:proxyChannel:modify"})
    public String saveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        return ApiUtil.echoResult(channelService.save(param, request));
    }

    @RequestMapping("/delete")
    @Permission("spider:proxyChannel:delete")
    public String deleteAction(@RequestBody Map<?, ?> param) {
        List<Integer> ids = DPUtil.parseIntList(param.get("ids"));
        return ApiUtil.echoResult(channelService.remove(ids));
    }

    @RequestMapping("/test")
    @Permission("spider:proxyChannel:")
    public String testAction(@RequestBody Map<?, ?> param) {
        ProxyChannel channel;
        try {
            int id = DPUtil.parseInt(param.get("id"));
            channel = id > 0 ? channelService.info(id) : channelService.preview(param);
        } catch (IllegalArgumentException e) {
            return ApiUtil.echoResult(1005, e.getMessage(), null);
        }
        return ApiUtil.echoResult(leaseService.probe(channel,
                DPUtil.parseInt(param.get("batchSize")),
                DPUtil.parseString(param.get("protocol")),
                DPUtil.parseString(param.get("region"))));
    }

}
