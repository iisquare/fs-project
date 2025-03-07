package com.iisquare.fs.app.crawler.web.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.app.crawler.output.Output;
import com.iisquare.fs.app.crawler.parse.Parser;
import com.iisquare.fs.app.crawler.schedule.*;
import com.iisquare.fs.app.crawler.web.ControllerBase;
import com.iisquare.fs.app.crawler.web.service.ScheduleService;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.List;
import java.util.Map;

public class ScheduleController extends ControllerBase {

    private ScheduleService scheduleService = ScheduleService.getInstance();

    public String parserAction(ChannelHandlerContext context, FullHttpRequest request) throws Exception {
        String content = body(request);
        Scheduler scheduler = scheduleService.scheduler();
        if (null == scheduler) return ApiUtil.echoResult(1001, "调度器异常", null);
        try {
            Parser parser = scheduler.getParserFactory().parser(null, content);
            return ApiUtil.echoResult(0, null, parser.expression());
        } catch (Exception e) {
            return ApiUtil.echoResult(500, e.getMessage(), ExceptionUtils.getStackTrace(e));
        }
    }

    public String saveAction(ChannelHandlerContext context, FullHttpRequest request) throws Exception {
        JsonNode param = DPUtil.parseJSON(body(request));
        if (null == param) return ApiUtil.echoResult(1001, "解析参数失败", param);
        String type = param.at("/type").asText();
        JsonNode content = DPUtil.parseJSON(param.at("/content").asText());
        Object result = scheduleService.save(type, content);
        return ApiUtil.echoResult(null == result ? 500: 0, null, result);
    }

    public String removeAction(ChannelHandlerContext context, FullHttpRequest request) throws Exception {
        JsonNode param = DPUtil.parseJSON(body(request));
        if (null == param) return ApiUtil.echoResult(1001, "解析参数失败", param);
        String type = param.at("/type").asText();
        String id = param.at("/id").asText();
        boolean result = scheduleService.remove(type, id);
        return ApiUtil.echoResult(result ? 0: 500, null, id);
    }

    public String listAction(ChannelHandlerContext context, FullHttpRequest request) throws Exception {
        ArrayNode data = DPUtil.arrayNode();
        Map<String, Schedule> schedules = scheduleService.schedules();
        for (Map.Entry<String, Schedule> entry : schedules.entrySet()) {
            Schedule schedule = entry.getValue();
            ObjectNode item = (ObjectNode) DPUtil.toJSON(schedule);
            ArrayNode intercepts = DPUtil.arrayNode();
            for (Map.Entry<Integer, List<Intercept>> interceptEntry : schedule.getIntercepts().entrySet()) {
                for (Intercept intercept : interceptEntry.getValue()) {
                    intercepts.addPOJO(intercept);
                }
            }
            item.replace("intercepts", intercepts);
            data.add(item);
        }
        return ApiUtil.echoResult(0, null, data);
    }

    public String groupAction(ChannelHandlerContext context, FullHttpRequest request) throws Exception {
        ArrayNode data = DPUtil.arrayNode();
        Map<String, Group> groups = scheduleService.groups();
        for (Map.Entry<String, Group> entry : groups.entrySet()) {
            Group group = entry.getValue();
            data.addPOJO(group);
        }
        return ApiUtil.echoResult(0, null, data);
    }

    public String proxyAction(ChannelHandlerContext context, FullHttpRequest request) throws Exception {
        ArrayNode data = DPUtil.arrayNode();
        Map<String, Proxy> proxies = scheduleService.proxies();
        for (Map.Entry<String, Proxy> entry : proxies.entrySet()) {
            Proxy proxy = entry.getValue();
            data.addPOJO(proxy);
        }
        return ApiUtil.echoResult(0, null, data);
    }

    public String historyAction(ChannelHandlerContext context, FullHttpRequest request) throws Exception {
        ArrayNode data = scheduleService.histories();
        return ApiUtil.echoResult(0, null, data);
    }

    public String schedulesAction(ChannelHandlerContext context, FullHttpRequest request) throws Exception {
        ArrayNode data = DPUtil.arrayNode();
        Map<String, Schedule> schedules = scheduleService.schedules();
        for (Map.Entry<String, Schedule> entry : schedules.entrySet()) {
            Schedule schedule = entry.getValue();
            data.addPOJO(schedule);
        }
        return ApiUtil.echoResult(0, null, data);
    }

    public String outputAction(ChannelHandlerContext context, FullHttpRequest request) throws Exception {
        return ApiUtil.echoResult(0, null, Output.config(null));
    }

    public String stateAction(ChannelHandlerContext context, FullHttpRequest request) throws Exception {
        return ApiUtil.echoResult(0, null, scheduleService.state());
    }

    public String startAction(ChannelHandlerContext context, FullHttpRequest request) throws Exception {
        scheduleService.start();
        return ApiUtil.echoResult(0, null, scheduleService.state());
    }

    public String stopAction(ChannelHandlerContext context, FullHttpRequest request) throws Exception {
        scheduleService.stop();
        return ApiUtil.echoResult(0, null, null);
    }

    public String changeAction(ChannelHandlerContext context, FullHttpRequest request) throws Exception {
        JsonNode param = DPUtil.parseJSON(body(request));
        if (null == param) return ApiUtil.echoResult(1001, "解析参数失败", param);
        String id = param.at("/id").asText();
        String status = param.at("/status").asText("");
        switch (status) {
            case "start": return ApiUtil.echoResult(scheduleService.start(id));
            case "pause": return ApiUtil.echoResult(scheduleService.pause(id));
            case "stop": return ApiUtil.echoResult(scheduleService.stop(id));
            default: return ApiUtil.echoResult(503, "指令错误", status);
        }
    }

    public String clearAction(ChannelHandlerContext context, FullHttpRequest request) throws Exception {
        JsonNode param = DPUtil.parseJSON(body(request));
        if (null == param) return ApiUtil.echoResult(1001, "解析参数失败", param);
        String id = param.at("/id").asText();
        return ApiUtil.echoResult(0, null, scheduleService.clear(id));
    }

    public String submitAction(ChannelHandlerContext context, FullHttpRequest request) throws Exception {
        JsonNode param = DPUtil.parseJSON(body(request));
        if (null == param) return ApiUtil.echoResult(1001, "解析参数失败", param);
        String id = param.at("/id").asText();
        return ApiUtil.echoResult(scheduleService.submit(id));
    }

    public String executeAction(ChannelHandlerContext context, FullHttpRequest request) throws Exception {
        JsonNode param = DPUtil.parseJSON(body(request));
        if (null == param) return ApiUtil.echoResult(1001, "解析参数失败", param);
        String id = param.at("/scheduleId").asText();
        String key = param.at("/templateKey").asText();
        return ApiUtil.echoResult(scheduleService.submit(id, key, param.at("/parameters")));
    }

}
