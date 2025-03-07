package com.iisquare.fs.app.crawler.web.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.app.crawler.web.ControllerBase;
import com.iisquare.fs.app.crawler.web.HttpHandler;
import com.iisquare.fs.app.crawler.web.service.NodeService;
import com.iisquare.fs.app.crawler.web.service.ScheduleService;
import com.iisquare.fs.app.crawler.web.ControllerBase;
import com.iisquare.fs.app.crawler.web.HttpHandler;
import com.iisquare.fs.app.crawler.web.service.NodeService;
import com.iisquare.fs.app.crawler.web.service.ScheduleService;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

public class NodeController extends ControllerBase {

    private NodeService nodeService = NodeService.getInstance();
    private ScheduleService scheduleService = ScheduleService.getInstance();

    public String stateAction(ChannelHandlerContext context, FullHttpRequest request) throws Exception {
        ObjectNode data = (ObjectNode) nodeService.state();
        data.replace("counter", scheduleService.counterState());
        return ApiUtil.echoResult(0, null, data);
    }

    public String mappingAction(ChannelHandlerContext context, FullHttpRequest request) throws Exception {
        return DPUtil.stringify(HttpHandler.routes());
    }

}
