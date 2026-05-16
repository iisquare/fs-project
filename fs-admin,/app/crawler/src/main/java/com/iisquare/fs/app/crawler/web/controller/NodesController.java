package com.iisquare.fs.app.crawler.web.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.app.crawler.web.ControllerBase;
import com.iisquare.fs.app.crawler.web.service.NodeService;
import com.iisquare.fs.app.crawler.web.service.ScheduleService;
import com.iisquare.fs.app.crawler.web.ControllerBase;
import com.iisquare.fs.app.crawler.web.service.NodeService;
import com.iisquare.fs.app.crawler.web.service.ScheduleService;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.HttpUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

public class NodesController extends ControllerBase {

    private NodeService nodeService = NodeService.getInstance();
    private ScheduleService scheduleService = ScheduleService.getInstance();

    public String stateAction(ChannelHandlerContext context, FullHttpRequest request) throws Exception {
        ObjectNode data = DPUtil.objectNode();
        ObjectNode nodes = data.putObject("nodes");
        for (String node : nodeService.participants()) {
            ObjectNode item = nodes.putObject(node);
            String url = "http://" + node;
            String content = HttpUtil.get(url + "/node/state/");
            if (null == content) return ApiUtil.echoResult(5001, "载入节点信息失败", url);
            JsonNode json = DPUtil.parseJSON(content);
            if (null != json) json = json.get("data");
            if (null != json && !json.isNull()) item.setAll((ObjectNode) json);
            content = HttpUtil.get(url + "/schedule/state/");
            if (null == content) return ApiUtil.echoResult(5002, "载入作业信息失败", url);
            json = DPUtil.parseJSON(content);
            if (null != json) json = json.get("data");
            if (null != json && !json.isNull()) item.setAll((ObjectNode) json);
        }
        data.replace("channel", scheduleService.channelState());
        return ApiUtil.echoResult(0, null, data);
    }

}
