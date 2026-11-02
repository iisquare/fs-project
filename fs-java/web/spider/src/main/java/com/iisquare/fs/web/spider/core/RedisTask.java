package com.iisquare.fs.web.spider.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.CodeUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class RedisTask {

    public String id;
    public String url;
    public String jobId;
    public String referer; // 来源页面链接地址
    public Long dispatchTime; // 加入调度时间
    public Long lastTime; // 最后调度时间
    public Long score; // Task优先级
    public Boolean force; // 强制重新采集
    public String taskKey; // 稳定任务标识，用于结果落库去重
    public String queueKey; // 队列判重键，与落库任务标识保持一致
    public String pageCode; // 定向采集所属页面编码
    public JsonNode args; // 定向采集任务执行参数
    public Integer retryCount; // 失败重试次数
    public Integer iterateCount; // 翻页迭代次数
    @JsonIgnore
    private URI uri = null;

    public static String encode(RedisTask task) {
        return DPUtil.stringify(DPUtil.toJSON(task));
    }

    public static RedisTask decode(String task) {
        return DPUtil.toJSON(DPUtil.parseJSON(task), RedisTask.class);
    }

    /**
     * 泛采集任务
     */
    public static RedisTask record(ZooJob job, String url, Boolean force, String referer) {
        RedisTask task = record(job);
        task.url = url;
        task.force = force;
        task.referer = referer;
        task.pageCode = "";
        task.args = DPUtil.objectNode();
        return task;
    }

    /**
     * 定向集任务
     */
    public static RedisTask record(ZooJob job, String pageCode, JsonNode args, String referer) {
        RedisTask task = record(job);
        task.pageCode = pageCode;
        task.args = args;
        task.url = url(job.template.at("/pages/" + pageCode + "/url").asText(), args);
        task.referer = referer;
        task.force = job.template.at("/pages/" + pageCode + "/force").asBoolean();
        task.taskKey = task.taskId(job);
        task.queueKey = task.taskKey;
        return task;
    }

    /**
     * 替换链接中参数变量
     */
    public static String url(String url, JsonNode args) {
        List<String> names = DPUtil.matcher("\\{(.*?)\\}", url, true);
        int size = names.size();
        if (size % 2 != 0) return "";
        for (int i = 0; i < size; i += 2) {
            String text = args.at("/" + names.get(i + 1)).asText();
            if (DPUtil.empty(text)) return ""; // 存在异常变量
            url = url.replace(names.get(i), text);
        }
        return url;
    }

    public String domain() {
        if (null == uri) {
            uri = URI.create(url);
        }
        return uri.getHost();
    }

    /**
     * 可重复请求的任务使用页面、参数和方法生成稳定标识，
     * 避免 POST 同一地址的不同参数被误判为同一页面。
     */
    public String taskId(ZooJob job) {
        if (null == job || !"plan".equals(job.template.at("/type").asText())) {
            return url;
        }
        if (!DPUtil.empty(taskKey)) return taskKey;
        JsonNode page = job.template.at("/pages/" + pageCode);
        String requestType = page.at("/requestType").asText("GET").trim().toUpperCase();
        JsonNode payloadForm = page.at("/payloadForm");
        boolean parameterized = !"GET".equals(requestType) || (payloadForm.isObject() && payloadForm.size() > 0);
        for (JsonNode value : page.at("/headers")) {
            if (value.asText("").contains("{")) {
                parameterized = true;
                break;
            }
        }
        if (!parameterized) return url;
        String args = canonical(null == this.args ? DPUtil.objectNode() : this.args);
        String signature = pageCode + "|" + requestType + "|" + args;
        return url + "#" + CodeUtil.md5(signature);
    }

    public String queueKey() {
        if (!DPUtil.empty(queueKey)) return queueKey;
        return DPUtil.empty(taskKey) ? url : taskKey;
    }

    private static String canonical(JsonNode node) {
        if (null == node) return "null";
        if (node.isObject()) {
            List<String> names = new ArrayList<>();
            node.fieldNames().forEachRemaining(names::add);
            Collections.sort(names);
            StringBuilder result = new StringBuilder("{");
            for (String name : names) {
                if (result.length() > 1) result.append(',');
                result.append(DPUtil.stringify(name)).append(':').append(canonical(node.get(name)));
            }
            return result.append('}').toString();
        }
        if (node.isArray()) {
            StringBuilder result = new StringBuilder("[");
            for (JsonNode item : node) {
                if (result.length() > 1) result.append(',');
                result.append(canonical(item));
            }
            return result.append(']').toString();
        }
        return node.toString();
    }

    private static RedisTask record(ZooJob job) {
        long time = System.currentTimeMillis();
        RedisTask task = new RedisTask();
        task.id = UUID.randomUUID().toString();
        task.jobId = job.getId();
        task.dispatchTime = time;
        task.lastTime = time;
        task.score = job.score(true);
        task.force = false;
        task.retryCount = 0;
        task.iterateCount = 0;
        return task;
    }

    /**
     * 停顿指定时长
     */
    public RedisTask halt(long halt) {
        long time = System.currentTimeMillis();
        this.score = time + halt;
        this.lastTime = time;
        return this;
    }

    public RedisTask back(ZooJob job) {
        this.score = job.score(true);
        this.lastTime = System.currentTimeMillis();
        this.retryCount++;
        return this;
    }

}
