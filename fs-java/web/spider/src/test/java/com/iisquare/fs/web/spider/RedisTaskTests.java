package com.iisquare.fs.web.spider;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.spider.core.RedisTask;
import com.iisquare.fs.web.spider.core.ZooJob;
import org.junit.Assert;
import org.junit.Test;

public class RedisTaskTests {

    @Test
    public void keepsDistinctParametersForSamePostUrl() {
        ObjectNode template = DPUtil.objectNode();
        template.put("type", "plan");
        template.putObject("pages").putObject("search").put("requestType", "POST");
        ZooJob job = new ZooJob();
        job.template = template;

        RedisTask first = task("a", "1");
        RedisTask second = task("a", "2");
        Assert.assertNotEquals(first.taskId(job), second.taskId(job));
    }

    @Test
    public void ignoresArgumentOrderForTaskIdentity() {
        ObjectNode template = DPUtil.objectNode();
        template.put("type", "plan");
        template.putObject("pages").putObject("search").put("requestType", "POST");
        ZooJob job = new ZooJob();
        job.template = template;

        RedisTask first = task("a", "1");
        first.args = DPUtil.parseJSON("{\"a\":\"1\",\"b\":\"2\"}");
        RedisTask second = task("b", "2");
        second.args = DPUtil.parseJSON("{\"b\":\"2\",\"a\":\"1\"}");
        Assert.assertEquals(first.taskId(job), second.taskId(job));
    }

    @Test
    public void keepsGetUrlsBackwardCompatible() {
        ObjectNode template = DPUtil.objectNode();
        template.put("type", "plan");
        template.putObject("pages").putObject("search").put("requestType", "GET");
        ZooJob job = new ZooJob();
        job.template = template;
        RedisTask task = task("a", "1");
        Assert.assertEquals(task.url, task.taskId(job));
    }

    @Test
    public void usesTaskIdentityAsQueueKey() {
        ObjectNode template = DPUtil.objectNode();
        template.put("type", "plan");
        ObjectNode page = template.putObject("pages").putObject("search");
        page.put("requestType", "POST");
        page.put("url", "https://example.com/search");
        ZooJob job = new ZooJob();
        job.template = template;
        RedisTask first = RedisTask.record(job, "search", DPUtil.parseJSON("{\"keyword\":\"一\"}"), "");
        RedisTask second = RedisTask.record(job, "search", DPUtil.parseJSON("{\"keyword\":\"二\"}"), "");
        Assert.assertEquals(first.taskId(job), first.queueKey());
        Assert.assertNotEquals(first.queueKey(), second.queueKey());
        Assert.assertEquals(first.taskKey, RedisTask.decode(RedisTask.encode(first)).taskKey);
    }

    private RedisTask task(String key, String value) {
        RedisTask task = new RedisTask();
        task.url = "https://example.com/search";
        task.pageCode = "search";
        task.args = DPUtil.objectNode().put(key, value);
        return task;
    }
}
