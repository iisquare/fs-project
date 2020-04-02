package com.iisquare.fs.web.flink.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.web.flink.cli.Submitter;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.flink.entity.Flow;
import com.iisquare.fs.web.flink.entity.FlowPlugin;
import com.iisquare.fs.web.flink.service.FlinkService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.util.*;

@RequestMapping("/flink")
@RestController
@RefreshScope
@Api(description = "流程图")
public class FlinkController {

    @Autowired
    private FlinkService flinkService;
    @Value("${fs.flink.plugins.path}")
    private String flowPluginsPath;
    @Value("${fs.flink.plugins.nfs}")
    private String flowPluginsNFS;

    @PostMapping("/command")
    @ApiOperation(value = "command", notes = "请求参数：  \n" +
        "command-List<Integer>-必填-无-执行命令  \n" +
        "debug-Boolean-选填-false-打印命令")
    public String commandAction(@RequestBody Map<String, Object> param) {
        List<String> command = (List<String>) param.get("command");
        if(DPUtil.parseBoolean(param.get("debug"))) return DPUtil.implode(" ", command.toArray(new String[command.size()]));
        try {
            Process process = Runtime.getRuntime().exec(command.toArray(new String[command.size()]));
            StringBuilder sb = new StringBuilder();
            Submitter.read(sb, process.getErrorStream());
            Submitter.read(sb, process.getInputStream());
            return sb.toString();
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    @GetMapping("/sysenv")
    @ApiOperation("sysenv")
    public String sysenvAction(@RequestParam("name") String name) {
        return ApiUtil.echoResult(0, null, System.getenv(name));
    }

    @GetMapping("/info")
    @ApiOperation("info")
    public String infoAction() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("FLINK_CONF_DIR", System.getenv("FLINK_CONF_DIR"));
        data.put("flowPluginsPath", flowPluginsPath);
        data.put("flowPluginsNFS", flowPluginsNFS);
        return ApiUtil.echoResult(0, null, data);
    }

    @GetMapping("/plain")
    @ApiOperation("plain")
    public String plainAction(@RequestParam Map<String, Object> param) {
        Flow info = flinkService.flowInfo(DPUtil.parseInt(param.get("flowId")));
        JsonNode flow = flinkService.parse(info, DPUtil.parseString(param.get("logId")), param.get("ext"));
        if(null == flow) return "";
        return DPUtil.stringify(flow);
    }

    @PostMapping("/submit")
    @ApiOperation(value = "submit", notes = "请求参数：  \n" +
        "flowId-Integer-选填-无-流程图主键  \n" +
        "logId-String-选填-空-日志标识  \n" +
        "detached-Boolean-选填-true-后台运行  \n" +
        "parallelism-Integer-选填-1-并行等级  \n" +
        "yarn-Object-选填-无-独立作业方式运行  \n" +
        "ext.checkpoint.interval-Long-选填-无-检查点间隔  \n" +
        "ext.checkpoint.mode-String-选填-EXACTLY_ONCE-检查点策略[AT_LEAST_ONCE|EXACTLY_ONCE]  \n" +
        "ext.restart-Object-选填-无-失败重试策略（不设置-不重启，设置为空值-默认策略）  \n" +
        "ext.restart.fixedDelay.restartAttempts-Integer-必填-无-最大尝试次数  \n" +
        "ext.restart.fixedDelay.delayBetweenAttempts-Long-必填-无-重试间隔（毫秒）  \n" +
        "ext.restart.failureRate.failureRate-Integer-必填-无-给定时间内的最大重试次数  \n" +
        "ext.restart.failureRate.failureInterval-Long-必填-无-给定的时间间隔（毫秒）  \n" +
        "ext.restart.failureRate.delayInterval-Long-必填-无-重试间隔（毫秒）")
    public String submitAction(@RequestBody Map<?, ?> param) {
        Flow info = flinkService.flowInfo(DPUtil.parseInt(param.get("flowId")));
        if(null == info || info.getStatus() != 1) return ApiUtil.echoResult(403, "信息不存在或状态不可用", info);
        JsonNode flow  = flinkService.parse(info, DPUtil.parseString(param.get("logId")), param.get("ext"));
        if(null == flow) return ApiUtil.echoResult(1001, "解析流程图失败", info);
        Set<String> plugins = new HashSet<>();
        Iterator<String> iterator = flow.get("plugins").fieldNames();
        while (iterator.hasNext()) {
            plugins.add(iterator.next());
        }
        Map<String, FlowPlugin> pluginsInfoMap = flinkService.pluginsInfoMap(plugins);
        ArrayNode jars = DPUtil.arrayNode();
        File pluginsDir = new File(flowPluginsPath);
        if(!pluginsDir.isDirectory()) {
            return ApiUtil.echoResult(1003, "插件目录不存在", pluginsDir.getAbsolutePath());
        }
        for (String plugin : plugins) {
            if(!pluginsInfoMap.containsKey(plugin)) {
                return ApiUtil.echoResult(1002, "插件状态异常", plugin);
            }
            for (File jar : new File(pluginsDir.getAbsolutePath() + File.separator + plugin).listFiles()) {
                if(!jar.isFile() || !jar.getName().endsWith(".jar")) continue;
                String url = DPUtil.implode("/", new String[]{flowPluginsNFS, plugin, jar.getName()});
                jars.add(url);
                ((ArrayNode) flow.get("plugins").get(plugin)).add(url);
            }
        }
        ObjectNode config = DPUtil.objectNode();
        config.put("detached", param.containsKey("detached") ? DPUtil.parseBoolean(param.get("detached")) : true);
        if(param.containsKey("parallelism")) config.put("parallelism", DPUtil.parseInt(param.get("parallelism")));
        config.replace("jars", jars);
        config.put("entry", "com.iisquare.fs.flink.FlowApplication");
        config.put("jar",pluginsDir.getAbsolutePath() + File.separator + "app.jar");
        config.replace("args", DPUtil.arrayNode().add(DPUtil.stringify(flow)));
        if(param.containsKey("yarn")) {
            config.put("appname", flow.get("appname").asText());
            config.replace("yarn", flinkService.extend(DPUtil.objectNode(), param.get("yarn")));
        }
        config.put("debug", DPUtil.parseBoolean(param.get("debug")));
        Object message = null;
        try {
            message = Submitter.run(config);
        } catch (Exception e) {
            return ApiUtil.echoResult(500, null, e.getMessage());
        }
        String messageStr = String.valueOf(message);
        return ApiUtil.echoResult(messageStr.contains("The program finished with the following exception") ? 500 : 0, messageStr, flow);
    }

}
