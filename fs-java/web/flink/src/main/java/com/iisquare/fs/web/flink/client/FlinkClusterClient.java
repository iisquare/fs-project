package com.iisquare.fs.web.flink.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ProcessUtil;
import com.iisquare.fs.base.core.util.RestUtil;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 远程 Flink 集群客户端。
 *
 * 支持两种提交方式：
 * - rest：通过 Flink REST API 上传作业Jar后运行（/jars/upload + /jars/{id}/run）
 * - command：通过 flink run -m 命令行提交
 *
 * 集群配置项（Cluster.content）：
 * {
 *   "submitType": "rest" | "command",
 *   "rest": "http://host:8081",       // Flink REST 地址
 *   "jobmanager": "host:8081",        // flink run -m 目标地址
 *   "home": "/opt/flink",             // FLINK_HOME，command方式使用
 *   "jar": "/path/to/fs-flink.jar",   // 作业Jar路径，运行本服务的宿主机可读
 *   "entryClass": "com.iisquare.fs.app.flink.FlinkApplication",
 *   "parallelism": 2,
 *   "args": ""
 * }
 */
public class FlinkClusterClient {

    public static final String ENTRY_CLASS = "com.iisquare.fs.app.flink.FlinkApplication";
    private static final long TIMEOUT = 120L;

    public static Map<String, Object> test(JsonNode content) {
        String rest = rest(content);
        if (DPUtil.empty(rest)) return ApiUtil.result(2001, "REST地址不能为空", rest);
        RestUtil.Response response = RestUtil.get(rest + "/overview");
        if (!response.succeed()) return ApiUtil.result(2002, "集群连接失败", response.toString());
        return ApiUtil.result(0, null, response.json());
    }

    public static Map<String, Object> submit(JsonNode content, String jobName, String diagramArg, Integer parallelism) {
        String submitType = content.at("/submitType").asText("rest");
        String rest = rest(content);
        if ("command".equalsIgnoreCase(submitType)) {
            return submitByCommand(content, jobName, diagramArg, parallelism);
        }
        if (DPUtil.empty(rest)) return ApiUtil.result(2001, "REST地址不能为空", rest);
        String jar = content.at("/jar").asText("");
        if (DPUtil.empty(jar)) return ApiUtil.result(2003, "作业Jar路径不能为空", jar);
        File file = new File(jar);
        if (!file.exists() || !file.isFile()) return ApiUtil.result(2004, "作业Jar文件不存在", jar);
        byte[] data;
        try {
            data = Files.readAllBytes(file.toPath());
        } catch (Exception e) {
            return ApiUtil.result(2005, "读取作业Jar失败", e.getMessage());
        }
        RestUtil.Response upload = RestUtil.upload(rest + "/jars/upload", "jarfile", file.getName(), data, TIMEOUT);
        if (!upload.succeed()) return ApiUtil.result(2006, "上传作业Jar失败", upload.toString());
        String filename = upload.json().at("/filename").asText("");
        String jarId = DPUtil.empty(filename) ? "" : filename.substring(filename.lastIndexOf('/') + 1);
        if (DPUtil.empty(jarId)) return ApiUtil.result(2007, "无法解析作业Jar标识", upload.body());
        ObjectNode body = DPUtil.objectNode();
        String entryClass = content.at("/entryClass").asText(ENTRY_CLASS);
        body.put("entryClass", entryClass);
        body.put("programArgs", programArgs(content, diagramArg));
        int para = null == parallelism ? content.at("/parallelism").asInt(1) : parallelism;
        body.put("parallelism", Math.max(1, para));
        body.put("jobId", UUID.randomUUID().toString().replace("-", ""));
        RestUtil.Response run = RestUtil.postJson(rest + "/jars/" + jarId + "/run", DPUtil.stringify(body));
        if (!run.succeed()) return ApiUtil.result(2008, "提交作业失败", run.toString());
        return ApiUtil.result(0, null, run.json());
    }

    private static Map<String, Object> submitByCommand(JsonNode content, String jobName, String diagramArg, Integer parallelism) {
        String home = content.at("/home").asText("");
        String jar = content.at("/jar").asText("");
        if (DPUtil.empty(jar)) return ApiUtil.result(2003, "作业Jar路径不能为空", jar);
        if (!new File(jar).isFile()) return ApiUtil.result(2004, "作业Jar文件不存在", jar);
        List<String> command = new ArrayList<>();
        String bin = DPUtil.empty(home) ? "flink" : (home + "/bin/flink");
        command.add(bin);
        command.add("run");
        String jobmanager = content.at("/jobmanager").asText("");
        if (!DPUtil.empty(jobmanager)) {
            command.add("-m");
            command.add(jobmanager);
        }
        command.add("-c");
        command.add(content.at("/entryClass").asText(ENTRY_CLASS));
        int para = null == parallelism ? content.at("/parallelism").asInt(1) : parallelism;
        command.add("-p");
        command.add(String.valueOf(Math.max(1, para)));
        command.add(jar);
        command.add(diagramArg);
        ProcessUtil.Result result = ProcessUtil.execute(command, null, TIMEOUT, "UTF-8");
        if (!result.succeed()) return ApiUtil.result(2009, "命令行提交失败", result.output());
        String jobId = regex(result.output(), "JobID\\s*[:：]?\\s*([0-9a-fA-F]+)", 1);
        ObjectNode json = DPUtil.objectNode();
        json.put("jobid", DPUtil.empty(jobId) ? UUID.randomUUID().toString().replace("-", "") : jobId);
        json.put("output", result.output());
        return ApiUtil.result(0, null, json);
    }

    public static Map<String, Object> status(JsonNode content, String jobId) {
        String rest = rest(content);
        if (DPUtil.empty(rest) || DPUtil.empty(jobId)) return ApiUtil.result(2001, "REST地址或作业标识不能为空", jobId);
        RestUtil.Response response = RestUtil.get(rest + "/jobs/" + jobId);
        if (!response.succeed()) return ApiUtil.result(2010, "查询作业状态失败", response.toString());
        return ApiUtil.result(0, null, response.json());
    }

    public static Map<String, Object> cancel(JsonNode content, String jobId) {
        String rest = rest(content);
        if (DPUtil.empty(rest) || DPUtil.empty(jobId)) return ApiUtil.result(2001, "REST地址或作业标识不能为空", jobId);
        RestUtil.Response response = RestUtil.request("PATCH", rest + "/jobs/" + jobId + "?mode=cancel", null, "", 60);
        if (!response.succeed()) return ApiUtil.result(2011, "取消作业失败", response.toString());
        return ApiUtil.result(0, null, true);
    }

    public static String diagramArg(String url, JsonNode diagram) {
        if (!DPUtil.empty(url)) return url;
        return Base64.getEncoder().encodeToString(DPUtil.stringify(diagram).getBytes());
    }

    private static String programArgs(JsonNode content, String diagramArg) {
        String args = content.at("/args").asText("");
        return DPUtil.empty(args) ? diagramArg : (args + " " + diagramArg);
    }

    private static String rest(JsonNode content) {
        String rest = content.at("/rest").asText("");
        if (!DPUtil.empty(rest) && rest.endsWith("/")) rest = rest.substring(0, rest.length() - 1);
        return rest;
    }

    private static String regex(String text, String pattern, int group) {
        List<String> list = DPUtil.matcher(pattern, DPUtil.parseString(text), true);
        if (list.size() <= group) return "";
        return list.get(group);
    }

}
