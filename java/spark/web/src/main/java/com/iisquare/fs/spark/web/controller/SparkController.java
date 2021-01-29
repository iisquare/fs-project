package com.iisquare.fs.spark.web.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.spark.web.core.Submitter;
import com.iisquare.fs.spark.web.entity.Flow;
import com.iisquare.fs.spark.web.entity.Plugin;
import com.iisquare.fs.spark.web.service.FlowService;
import com.iisquare.fs.spark.web.service.PluginService;
import com.iisquare.fs.spark.web.service.SparkService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.*;

@RequestMapping("/spark")
@RestController
@RefreshScope
public class SparkController {

    @Autowired
    private SparkService sparkService;
    @Autowired
    private FlowService flowService;
    @Autowired
    private PluginService pluginService;

    @GetMapping("/info")
    @ApiOperation("info")
    public String infoAction() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("SPARK_HOME", System.getenv("SPARK_HOME"));
        data.put("pluginsDir", sparkService.pluginsDir());
        data.put("pluginsUrl", sparkService.pluginsUrl());
        return ApiUtil.echoResult(0, null, data);
    }

    @GetMapping("/plain")
    @ApiOperation("plain")
    public String plainAction(@RequestParam Map<String, Object> param) {
        Flow info = flowService.info(DPUtil.parseInt(param.get("flowId")));
        JsonNode flow = sparkService.flow(info, DPUtil.parseString(param.get("logId")), param.get("ext"));
        if(null == flow) return "";
        return DPUtil.stringify(flow);
    }

    @PostMapping("/submit")
    public String submitAction(@RequestBody Map<?, ?> param) {
        Flow info = flowService.info(DPUtil.parseInt(param.get("flowId")));
        if(null == info || info.getStatus() != 1) return ApiUtil.echoResult(403, "信息不存在或状态不可用", info);
        JsonNode flow  = sparkService.flow(info, DPUtil.parseString(param.get("logId")), param.get("ext"));
        if(null == flow) return ApiUtil.echoResult(1001, "解析流程图失败", info);
        Set<String> plugins = new HashSet<>();
        Iterator<String> iterator = flow.get("plugins").fieldNames();
        while (iterator.hasNext()) {
            plugins.add(iterator.next());
        }
        Map<String, Plugin> pluginsInfoMap = pluginService.infoMap(plugins);
        List<String> jars = new ArrayList<>();
        File pluginsDir = new File(sparkService.pluginsDir());
        if(!pluginsDir.isDirectory()) {
            return ApiUtil.echoResult(1003, "插件目录不存在", pluginsDir.getAbsolutePath());
        }
        for (String plugin : plugins) {
            if(!pluginsInfoMap.containsKey(plugin)) {
                return ApiUtil.echoResult(1002, "插件状态异常", plugin);
            }
            for (File jar : new File(pluginsDir.getAbsolutePath() + File.separator + plugin).listFiles()) {
                if(!jar.isFile() || !jar.getName().endsWith(".jar")) continue;
                String url = DPUtil.implode("/", new String[]{sparkService.pluginsUrl(), plugin, jar.getName()});
                jars.add(url);
                ((ArrayNode) flow.get("plugins").get(plugin)).add(url);
            }
        }
        ObjectNode config = DPUtil.objectNode();
        config.put("master", sparkService.k8sUrl());
        if(param.containsKey("detached")) {
            config.put("detached", DPUtil.parseBoolean(param.get("detached")));
        }
        if(param.containsKey("parallelism")) {
            config.put("parallelism", DPUtil.parseInt(param.get("parallelism")));
        }
        if (jars.size() > 0) {
            config.put("jars", DPUtil.implode(",", jars.toArray(new String[jars.size()])));
        }
        config.put("entry", "com.iisquare.fs.spark.FlowApplication");
        config.put("jar",pluginsDir.getAbsolutePath() + File.separator + "app.jar");
        config.replace("args", DPUtil.arrayNode().add(DPUtil.stringify(flow)));
        Submitter.kubernetes(config);
        return null;
//        Object message = null;
//        try {
//            message = Submitter.run(config);
//        } catch (Exception e) {
//            return ApiUtil.echoResult(500, null, e.getMessage());
//        }
//        String messageStr = String.valueOf(message);
//        return ApiUtil.echoResult(messageStr.contains("The program finished with the following exception") ? 500 : 0, messageStr, flow);
    }

}
