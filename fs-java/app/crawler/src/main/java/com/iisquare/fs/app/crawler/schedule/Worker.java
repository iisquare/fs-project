package com.iisquare.fs.app.crawler.schedule;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.app.crawler.assist.Assist;
import com.iisquare.fs.app.crawler.fetch.HttpFetcher;
import com.iisquare.fs.app.crawler.output.Output;
import com.iisquare.fs.app.crawler.parse.Parser;
import com.iisquare.fs.app.crawler.trace.Tracer;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Getter
@Setter
public class Worker implements Runnable {

    protected final static Logger logger = LoggerFactory.getLogger(Worker.class);
    protected final static Tracer tracer = Tracer.tracer();
    private static final String SCRIPT_CODE =
        FileUtil.getContent(Worker.class.getClassLoader().getResource("crawler-script.js"), false, "UTF-8");
    private Thread thread;
    private Job job;

    public Worker() {}

    public void call(Job job) {
        this.job = job;
        this.thread.start();
    }

    public Token process() {
        HttpFetcher fetcher = job.scheduler.borrowFetcher(); // 请求对象
        if (null == fetcher) {
            job.scheduler.schedule(job.task, false);
            return job.token;
        }
        fetcher.charset(job.template.getCharset());
        RequestConfig proxy = job.scheduler.getProxyFactory().config();
        if (null != proxy) fetcher.config(proxy);
        Map<String, String> headers = job.token.headers(job.template.getHeaders());
        fetcher.url(job.task.getUrl()).headers(headers).get();
        Exception lastException = fetcher.getLastException();
        int status = fetcher.getLastStatus();
        String content = fetcher.getLastResult();
        if (job.schedule.isDealRequestHeader()) { // 记录请求Cookie信息，兼容302跳转
            job.token.cookie(fetcher.getLastRequestHeaders());
        }
        if (job.schedule.isDealResponseHeader()) { // 记录响应Cookie信息
            job.token.cookie(fetcher.getLastResponseHeaders());
        }
        job.scheduler.returnFetcher(fetcher);
        tracer.debug("request", null, job, status, null, null, null, lastException, null);
        if (null != lastException) { // 请求异常
            this.fallback(content, proxy, "REQUEST_ERROR", lastException);
            return job.token;
        }
        if (!intercept(content, proxy, status, "INTERCEPT", lastException)) { // 拦截器终止执行
            return job.token;
        }
        // 执行默认规则
        if (404 == status) { // 找不到页面，不执行后续处理
            return job.token;
        }
        if (200 == status) { // 解析请求
            return parse(content, proxy);
        }
        retry(content, proxy, status, lastException); // 重新尝试执行
        return job.token;
    }

    @Override
    public void run() {
        Scheduler scheduler = job.scheduler;
        do {
            scheduler.tick(process());
        } while ((job = scheduler.takeJob()) != null);
        try {
            scheduler.getWorkerPool().returnObject(this);
        } catch (Exception e) {
            logger.warn("return worker failed", e);
        }
    }

    private void retry(String content, RequestConfig proxy, int code, Exception exception) {
        if (job.task.getRetryCount() >= job.template.getMaxRetry()) {
            this.fallback(content, proxy, "RETRY_EXHAUST", exception);
            return;
        }
        if (job.task.getUrl().startsWith("{")) {
            tracer.info("retry", "Illegal url, ignored!", job, code, content, null, null, exception, null);
            return;
        }
        job.task.retry(job.scheduler);
        tracer.info("retry", null, job, code, content, null, null, exception, null);
        job.scheduler.schedule(job.task, false);
    }

    public void output(String config, String result, JsonNode data) throws Exception {
        if (null == data) return;
        Output output = Output.output(config);
        if (null == output) return;
        try {
            output.open();
            output.record(data);
            tracer.info("output", null, job, 200, null, data, null, null, null);
        } finally {
            output.close();
        }
    }

    public void fetch(JsonNode data) {
        if (null == data) return;
        Iterator<Map.Entry<String, JsonNode>> iterator = data.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            job.scheduler.schedule(job.schedule, entry.getKey(), job.schedule.params(entry.getValue()), true);
            tracer.info("fetch", entry.getKey(), job, 200, null, entry.getValue(), null, null, null);
        }
    }

    public String next(JsonNode data) {
        if (null == data || !data.isTextual()) return "";
        return data.asText("");
    }

    public boolean iterate(JsonNode data) {
        if (null == data || !data.isTextual()) return true;
        String uri = data.asText();
        int iterateCount = job.template.getMaxIterate();
        if (iterateCount > 0 && job.task.getIterateCount() >= iterateCount) return false;
        tracer.info("iterate", null, job, 200, null, data, null, null, null);
        job.task.iterate(job.scheduler, uri);
        job.scheduler.schedule(job.task, false);
        return true;
    }

    public JsonNode eval(Map<String, Object> context, String code) throws ScriptException {
        ScriptEngine engine = job.scheduler.scriptEngine();
        engine.put("context", context);
        code = new StringBuilder(SCRIPT_CODE)
                .append("context.data && (context.data = JSON.parse(context.data));")
                .append("JSON.stringify((function (context) {").append(code).append("})(context));").toString();
        Object result = engine.eval(code);
        return DPUtil.parseJSON(DPUtil.parseString(result));
    }

    public Map<String, Object> context(String content, JsonNode data, String status, Exception exception, String assist) {
        Map<String, Object> context = new HashMap<>();
        context.put("url", job.task.getUrl());
        context.put("scheduleId", job.task.getScheduleId());
        context.put("templateKey", job.task.getTemplateKey());
        context.put("param", job.task.getParam());
        context.put("content", content);
        context.put("data", DPUtil.stringify(data));
        context.put("status", status);
        context.put("exception", exception == null ? null : ExceptionUtils.getStackTrace(exception));
        context.put("assist", assist);
        return context;
    }

    public String assist(String content, JsonNode data, Intercept intercept, RequestConfig config, String status, Exception exception) throws Exception {
        if (null == intercept) return null;
        String code = intercept.getAssistor();
        if (DPUtil.empty(code)) return null;
        String proxy = null;
        if (null != config) {
            HttpHost host = config.getProxy();
            if (null != host) {
                proxy = host.getSchemeName() + "://" + host.getHostName() + ":" + host.getPort();
            }
        }
        JsonNode result;
        try {
            result = eval(context(content, data, status, exception, proxy), code);
        } catch (ScriptException e) {
            logger.warn("schedule " + job.schedule.getId() + " intercept " + intercept.getId() + " eval map failed & continue", e);
            tracer.warn("assist", "eval map failed & continue", job, 200, content, data, status, e, intercept.getId());
            return e.getMessage();
        }
        Assist assist = Assist.assist(result.at("/type").asText(), result.get("property"));
        if (null == assist) return null;
        if (!assist.open()) return "尝试打开失败";
        try {
            return assist.run();
        } finally {
            assist.close();
        }
    }

    public boolean fallback(String content, RequestConfig proxy, String status, Exception exception) {
        logger.warn("schedule " + job.task.getScheduleId() + " template " + job.task.getTemplateKey() + " fallback with status " + status, exception);
        tracer.warn("fallback", null, job, 200, content, null, status, exception, proxy == null ? null : proxy.toString());
        return intercept(content, proxy, -1, status, exception);
    }

    public String collect(String output, String content, JsonNode result) {
        if (null == result) result = DPUtil.objectNode();
        fetch(result.get("fetch"));
        iterate(result.get("iterate"));
        try {
            output(output, content, result.get("output"));
        } catch (Exception e) {
            logger.error("output error", e);
            tracer.error("output", null, job, 200, content, result.get("output"), null, e, null);
        }
        return next(result.get("next"));
    }

    public Token parse(String content, RequestConfig proxy) {
        JsonNode data = null;
        try {
            Parser parser = job.scheduler.parser(job.template.getId(), job.template.getParser());
            if (null != parser) data = parser.parse(content);
        } catch (Exception e) {
            this.fallback(content, proxy, "PARSE_ERROR", e);
            return job.token;
        }
        String mapper = job.template.getMapper();
        JsonNode result;
        try {
            if (DPUtil.empty(mapper)) {
                result = DPUtil.objectNode();
                ((ObjectNode) result).replace("output", data);
            } else {
                result = eval(context(content, data, "OK", null, null), mapper);
            }
        } catch (ScriptException e) {
            this.fallback(content, proxy, "SCRIPT_ERROR", e);
            return job.token;
        }
        this.collect(job.template.getOutput(), content, result);
        return job.token;
    }

    public boolean intercept(String content, RequestConfig proxy, int code, String status, Exception exception) {
        for (Intercept intercept : job.schedule.intercepts(code)) {
            JsonNode data = null;
            try {
                Parser parser = job.scheduler.parser(intercept.getId(), intercept.getParser());
                if (null != parser) data = parser.parse(content);
            } catch (Exception e) {
                logger.warn("schedule " + job.schedule.getId() + " intercept " + intercept.getId() + " parse failed & continue", e);
                tracer.warn("intercept", "parse failed & continue", job, code, content, data, status, e, intercept.getId());
                continue;
            }
            String assist;
            try {
                assist = assist(content, data, intercept, proxy, status, exception);
            } catch (Exception e) {
                logger.warn("schedule " + job.schedule.getId() + " intercept " + intercept.getId() + " eval assist failed & continue", e);
                tracer.warn("intercept", "eval assist failed & continue", job, code, content, data, status, e, intercept.getId());
                continue;
            }
            String mapper = intercept.getMapper();
            if (DPUtil.empty(mapper)) continue;
            JsonNode result;
            try {
                result = eval(context(content, data, status, exception, assist), mapper);
            } catch (ScriptException e) {
                logger.warn("schedule " + job.schedule.getId() + " intercept " + intercept.getId() + " eval map failed & continue", e);
                tracer.warn("intercept", "eval map failed & continue", job, code, content, data, status, e, intercept.getId());
                continue;
            }
            if ("RETRY_EXHAUST".equals(status)) return false; // 重试次数耗尽
            String next = this.collect(intercept.getOutput(), content, result);
            switch (next) {
                case "retry":
                    retry(content, proxy, code, exception);
                    return false;
                case "retryWithTokenHalt":
                    retry(content, proxy, code, exception);
                    halt();
                    return false;
                case "discard":
                    return false;
                default:
                    continue;
            }
        }
        return true;
    }

    public Token halt() {
        long halt = job.schedule.halt();
        logger.debug("check verify halt " + halt);
        tracer.debug("halt", "check verify halt", job, 0, String.valueOf(halt), null, null, null, null);
        return job.token.halt(halt);
    }

}
