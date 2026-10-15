package com.iisquare.fs.web.spark.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ProcessUtil;
import com.iisquare.fs.base.core.util.RestUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 远程 Spark 集群客户端。
 *
 * 支持两种提交方式：
 * - command：通过 spark-submit 命令行提交（支持 --master spark://... / yarn / k8s://...）
 * - rest：通过 Spark Standalone 提交服务（/v1/submissions/create）提交
 *
 * 集群配置项（Cluster.content）：
 * {
 *   "submitType": "command" | "rest",
 *   "master": "spark://host:7077" | "yarn" | "k8s://...",
 *   "rest": "http://host:6066",       // Standalone提交服务地址
 *   "home": "/opt/spark",             // SPARK_HOME，command方式使用
 *   "deployMode": "client" | "cluster",
 *   "jar": "/path/to/fs-spark.jar",   // 作业Jar路径，运行本服务的宿主机可读
 *   "entryClass": "com.iisquare.fs.app.spark.SparkApplication",
 *   "args": "",
 *   "conf": { "spark.executor.memory": "2g" }
 * }
 */
public class SparkClusterClient {

    public static final String ENTRY_CLASS = "com.iisquare.fs.app.spark.SparkApplication";
    private static final long TIMEOUT = 180L;

    public static Map<String, Object> test(JsonNode content) {
        String submitType = content.at("/submitType").asText("command");
        if ("rest".equalsIgnoreCase(submitType)) {
            String rest = rest(content);
            if (DPUtil.empty(rest)) return ApiUtil.result(2001, "提交服务地址不能为空", rest);
            RestUtil.Response response = RestUtil.get(rest + "/v1/submissions/status/probe");
            if (response.status() < 0) return ApiUtil.result(2002, "集群连接失败", response.toString());
            return ApiUtil.result(0, null, DPUtil.buildMap("reachable", true, "probe", response.body()));
        }
        String submit = submitBin(content);
        if (!new File(submit).isFile()) return ApiUtil.result(2003, "spark-submit不存在", submit);
        ProcessUtil.Result result = ProcessUtil.execute(Arrays.asList(submit, "--version"), null, 60, "UTF-8");
        if (!result.succeed()) return ApiUtil.result(2004, "spark-submit执行失败", result.output());
        return ApiUtil.result(0, null, DPUtil.buildMap("reachable", true, "version", result.output()));
    }

    public static Map<String, Object> submit(JsonNode content, String jobName, String diagramArg, Integer parallelism) {
        String submitType = content.at("/submitType").asText("command");
        if ("rest".equalsIgnoreCase(submitType)) {
            return submitByRest(content, jobName, diagramArg);
        }
        return submitByCommand(content, jobName, diagramArg, parallelism);
    }

    private static Map<String, Object> submitByRest(JsonNode content, String jobName, String diagramArg) {
        String rest = rest(content);
        if (DPUtil.empty(rest)) return ApiUtil.result(2001, "提交服务地址不能为空", rest);
        String jar = content.at("/jar").asText("");
        if (DPUtil.empty(jar)) return ApiUtil.result(2005, "作业Jar路径不能为空", jar);
        ObjectNode body = DPUtil.objectNode();
        body.put("action", "CreateSubmissionRequest");
        body.put("appName", DPUtil.empty(jobName) ? "fs-spark-job" : jobName);
        body.put("appResource", jar);
        body.put("mainClass", content.at("/entryClass").asText(ENTRY_CLASS));
        body.put("sparkMaster", content.at("/master").asText("spark://localhost:7077"));
        body.put("deployMode", "cluster");
        ArrayNode appArgs = body.putArray("appArgs");
        appArgs.add(diagramArg);
        ObjectNode conf = body.putObject("sparkProperties");
        JsonNode extra = content.at("/conf");
        if (extra.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> iterator = extra.fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = iterator.next();
                conf.put(entry.getKey(), entry.getValue().asText());
            }
        }
        RestUtil.Response response = RestUtil.postJson(rest + "/v1/submissions/create", DPUtil.stringify(body));
        if (!response.succeed()) return ApiUtil.result(2006, "提交作业失败", response.toString());
        return ApiUtil.result(0, null, response.json());
    }

    private static Map<String, Object> submitByCommand(JsonNode content, String jobName, String diagramArg, Integer parallelism) {
        String submit = submitBin(content);
        if (!new File(submit).isFile()) return ApiUtil.result(2003, "spark-submit不存在", submit);
        String jar = content.at("/jar").asText("");
        if (DPUtil.empty(jar)) return ApiUtil.result(2005, "作业Jar路径不能为空", jar);
        if (!new File(jar).isFile()) return ApiUtil.result(2007, "作业Jar文件不存在", jar);
        List<String> command = new ArrayList<>();
        command.add(submit);
        command.add("--master");
        command.add(content.at("/master").asText("local"));
        command.add("--deploy-mode");
        command.add(content.at("/deployMode").asText("cluster"));
        command.add("--name");
        command.add(DPUtil.empty(jobName) ? "fs-spark-job" : jobName);
        command.add("--class");
        command.add(content.at("/entryClass").asText(ENTRY_CLASS));
        JsonNode conf = content.at("/conf");
        if (conf.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> iterator = conf.fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = iterator.next();
                command.add("--conf");
                command.add(entry.getKey() + "=" + entry.getValue().asText());
            }
        }
        int para = null == parallelism ? content.at("/parallelism").asInt(0) : parallelism;
        if (para > 0) {
            command.add("--num-executors");
            command.add(String.valueOf(para));
        }
        String args = content.at("/args").asText("");
        command.add(jar);
        if (!DPUtil.empty(args)) command.addAll(DPUtil.array2list(DPUtil.explode(" ", args)));
        command.add(diagramArg);
        ProcessUtil.Result result = ProcessUtil.execute(command, null, TIMEOUT, "UTF-8");
        if (!result.succeed()) return ApiUtil.result(2008, "命令行提交失败", result.output());
        String appId = regex(result.output(), "Application ID\\s*[:：]\\s*([\\w\\-]+)", 1);
        if (DPUtil.empty(appId)) appId = regex(result.output(), "app-\\d{14}-\\d{4}", 0).replace("\n", "");
        if (DPUtil.empty(appId)) appId = UUID.randomUUID().toString();
        ObjectNode json = DPUtil.objectNode();
        json.put("submissionId", appId);
        json.put("output", result.output());
        return ApiUtil.result(0, null, json);
    }

    public static Map<String, Object> status(JsonNode content, String jobId) {
        String submitType = content.at("/submitType").asText("command");
        if (!"rest".equalsIgnoreCase(submitType)) {
            return ApiUtil.result(0, null, DPUtil.buildMap("submissionId", jobId, "driverState", "UNKNOWN"));
        }
        String rest = rest(content);
        if (DPUtil.empty(rest) || DPUtil.empty(jobId)) return ApiUtil.result(2001, "提交服务地址或作业标识不能为空", jobId);
        RestUtil.Response response = RestUtil.get(rest + "/v1/submissions/status/" + jobId);
        if (!response.succeed()) return ApiUtil.result(2009, "查询作业状态失败", response.toString());
        return ApiUtil.result(0, null, response.json());
    }

    public static Map<String, Object> cancel(JsonNode content, String jobId) {
        String submitType = content.at("/submitType").asText("command");
        if (!"rest".equalsIgnoreCase(submitType)) return ApiUtil.result(2010, "命令行提交的作业不支持远程取消", jobId);
        String rest = rest(content);
        if (DPUtil.empty(rest) || DPUtil.empty(jobId)) return ApiUtil.result(2001, "提交服务地址或作业标识不能为空", jobId);
        RestUtil.Response response = RestUtil.postJson(rest + "/v1/submissions/kill/" + jobId, "{}");
        if (!response.succeed()) return ApiUtil.result(2011, "取消作业失败", response.toString());
        return ApiUtil.result(0, null, response.json());
    }

    public static String diagramArg(String url, JsonNode diagram) {
        if (!DPUtil.empty(url)) return url;
        return Base64.getEncoder().encodeToString(DPUtil.stringify(diagram).getBytes());
    }

    private static String submitBin(JsonNode content) {
        String home = content.at("/home").asText("");
        return DPUtil.empty(home) ? "spark-submit" : (home + "/bin/spark-submit");
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
