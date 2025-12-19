package com.iisquare.fs.web.spider.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.net.URI;
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
    public String pageId; // 定向采集所属页面标识
    public ObjectNode args; // 定向采集任务执行参数
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
        task.pageId = "";
        task.args = DPUtil.objectNode();
        return task;
    }

    /**
     * 定向集任务
     */
    public static RedisTask record(ZooJob job, String pageId, ObjectNode args, String referer) {
        RedisTask task = record(job);
        task.pageId = pageId;
        task.args = args;
        task.url = url(job, pageId, args);
        task.referer = referer;
        return task;
    }

    /**
     * 获取定向采集页面链接
     */
    public static String url(ZooJob job, String pageId, ObjectNode args) {
        return "";
    }

    public String domain() {
        if (null == uri) {
            uri = URI.create(url);
        }
        return uri.getHost();
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
