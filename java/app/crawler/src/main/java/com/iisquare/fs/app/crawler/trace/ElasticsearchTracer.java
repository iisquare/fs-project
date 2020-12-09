package com.iisquare.fs.app.crawler.trace;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.app.crawler.schedule.Job;
import com.iisquare.fs.app.crawler.schedule.Job;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.HttpUtil;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.List;

public class ElasticsearchTracer extends Tracer {

    private String node;
    private String uri;
    private String dateFormat = "yyyy-MM-dd";

    public ElasticsearchTracer(String node, String uri) {
        this.node = node;
        List<String> list = DPUtil.getMatcher("\\{.*?\\}", uri, true);
        for (String item : list) {
            String[] explode = DPUtil.explode(item.substring(1, item.length() - 1), ":");
            if (explode.length == 1) continue;
            switch (explode[0]) {
                case "date":
                    dateFormat = explode[1];
                    break;
            }
            uri = uri.replace(item, "{" + explode[0] + "}");
        }
        this.uri = uri;
    }

    @Override
    public void log(int level, String step, String message, Job job, int code, String content, JsonNode data, String status, Exception exception, String assist) {
        long time = System.currentTimeMillis();
        String url = uri.replaceAll("\\{date\\}", DPUtil.millisToDateTime(time, dateFormat));
        url += "/_bulk";
        ObjectNode context = DPUtil.objectNode();
        context.put("level", level);
        context.put("time", job.time);
        context.put("duration", time - job.time);
        context.put("node", node);
        context.put("step", step);
        context.put("message", message);
        context.put("url", job.task.getUrl());
        context.put("scheduleId", job.task.getScheduleId());
        context.put("templateKey", job.task.getTemplateKey());
        context.put("code", code);
        context.put("content", content);
        context.put("data", DPUtil.stringify(data));
        context.put("sdata", null == data ? -1 : data.size());
        context.put("status", status);
        context.put("exception", exception == null ? null : ExceptionUtils.getStackTrace(exception));
        context.put("assist", assist);
        context.put("@timestamp", time);
        if (null == HttpUtil.post(url, "{\"index\":{}}\n" + DPUtil.stringify(context) + "\n", null)) {
            logger.warn("tracer log record failed to " + url + " with context " + context);
        }
    }

}
