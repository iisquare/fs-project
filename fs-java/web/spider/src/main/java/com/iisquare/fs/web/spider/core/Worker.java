package com.iisquare.fs.web.spider.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.base.jsoup.util.JsoupUtil;
import com.iisquare.fs.base.mongodb.MongoCore;
import com.iisquare.fs.base.mongodb.util.MongoUtil;
import com.iisquare.fs.web.spider.parser.Parser;
import com.iisquare.fs.web.spider.service.NodeService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.core.io.ClassPathResource;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Getter
@Setter
@Slf4j
public class Worker implements Runnable {

    private Thread thread;
    /**
     * 参数格式：{
     *     time: 任务拉取时间,
     *     nodeId: '认证节点标识',
     *     token: {},
     *     task: {},
     *     storage: {
     *         coast: 请求执行耗时,
     *         status: 请求响应状态码,
     *         location: '重定向后的链接地址',
     *         media: '页面类型',
     *         exception: '请求异常',
     *         html: '请求响应内容',
     *     },
     * }
     */
    private ObjectNode data;
    private NodeService nodeService;
    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    public static final String SCRIPT_MAPPER;

    static {
        try {
            SCRIPT_MAPPER = FileUtil.getContent(
                    new ClassPathResource("/scripts/worker-mapper.js").getFile(), false, DEFAULT_CHARSET);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Worker() {}

    public void call(NodeService nodeService, ObjectNode data) {
        this.nodeService = nodeService;
        this.data = data;
        this.thread.start();
    }

    private boolean process() {
        JsonNode task = data.at("/task");
        JsonNode storage = data.at("/storage");
        ZooJob job = nodeService.zookeeper().job(task.at("/jobId").asText());
        Document info = new Document();
        info.put(MongoCore.FIELD_ID, task.at("/url").asText());
        info.put("job_id", task.at("/jobId").asText());
        info.put("job_type", task.at("/type").asText());
        info.put("url", storage.at("/location").asText());
        if (DPUtil.empty(info.getString("url"))) {
            info.put("url", info.getString(MongoCore.FIELD_ID));
        }
        info.put("type", "");
        info.put("content_type", storage.at("/media").asText());
        try {
            URI uri = new URI(info.getString("url"));
            info.put("domain", uri.getHost());
            info.put("uri", uri.getPath());
            info.put("uris", Arrays.asList(DPUtil.explode("/", uri.getPath())));
        } catch (URISyntaxException e) {
            info.put("domain", "");
            info.put("uri", "");
            info.put("uris", new ArrayList<String>());
        }
        info.put("page_code", task.at("/pageCode").asText());
        info.put("task_args", MongoUtil.fromJson(task.at("/args")));
        info.put("content", storage.at("/html").asText());
        org.jsoup.nodes.Document document = Jsoup.parse(info.getString("content"), info.getString("url"));
        info.put("title", JsoupUtil.title(document));
        info.put("keywords", Arrays.asList(DPUtil.explode(",", JsoupUtil.keywords(document))));
        info.put("description", JsoupUtil.description(document));
        info.put("response_status", storage.at("/status").asInt());
        info.put("response_time", storage.at("/coast").asInt());
        info.put("exception", storage.at("/exception").asText());
        info.put("referer", task.at("/referer").asText());
        long time = System.currentTimeMillis();
        info.put("created_time", time);
        info.put("updated_time", time);
        info.put("deleted_time", 0L);
        JsonNode intercept = intercept(job, task, storage, info); // 拦截器检查
        String collection;
        if (null == intercept) { // 无拦截，执行内容提取操作
            info.put("collect", MongoUtil.fromJson(plan(job, task, storage, info)));
            collection = collection(job, info.getString("page_code"), info.getString("domain"));
        } else {
            info.put("intercept", MongoUtil.fromJson(intercept));
            collection = job.template.at("/intercepts/" + intercept.at("/id").asText() + "/collection").asText();
            if (DPUtil.empty(collection)) collection = "fs_spider_intercept";
        }
        info = nodeService.htmlMongo.switchTable(collection).upsert(info, "created_time");
        if (null != intercept) return true;
        if (!Arrays.asList(200, 404, 503).contains(info.getInteger("response_status"))) {
            retry(job, task, storage, info); // 非正常响应，重试任务
        }
        return broad(job, task, storage, info, document);
    }

    public boolean retry(ZooJob job, JsonNode task, JsonNode storage, Document info) {
        int retryCount = task.at("/retryCount").asInt();
        if (retryCount >= retryCount(job, task, info.getString("domain"))) return false;
        return nodeService.channel().putTask(RedisTask.decode(task.toString()).back(job));
    }

    public JsonNode intercept(ZooJob job, JsonNode task, JsonNode storage, Document info) {
        ObjectNode result = DPUtil.objectNode();
        int status = storage.at("/status").asInt();
        for (JsonNode intercept : job.template.at("/intercepts")) {
            if (status != intercept.at("/code").asInt()) continue;
            result.put("id", intercept.at("/id").asText());
            result.put("name", intercept.at("/name").asText());
            JsonNode parsed = null;
            try {
                Parser parser = nodeService.parser(
                        intercept.at("/id").asText() + "-" + job.updatedTime, intercept.at("/parser").asText());
                if (null != parser) parsed = parser.parse(storage.at("/html").asText(), info.getString("url"));
            } catch (Exception e) {
                return result.put("exception", ApiUtil.getStackTrace(e));
            }
            String script = intercept.at("/mapper").asText();
            if (DPUtil.empty(script)) continue; // 未配置映射器，忽略处理
            JsonNode mapped;
            try {
                mapped = eval(job, task, storage, info, parsed, script);
            } catch (ScriptException e) {
                return result.put("exception", ApiUtil.getStackTrace(e));
            }
            result.replace("collect", mapped.at("/collect"));
            switch (mapped.at("/next").asText()) {
                case "retry":
                    retry(job, task, storage, info);
                    return result;
                case "halt":
                    retry(job, task, storage, info);
                    nodeService.haltJob(job, mapped.at("/halt").asLong(30000));
                    return result;
                case "discard":
                    return result;
            }
        }
        return null;
    }

    public JsonNode plan(ZooJob job, JsonNode task, JsonNode storage, Document info) {
        if (200 != storage.at("/status").asInt()) return null;
        if (null == job) return null;
        if (!"plan".equals(job.getTemplate().at("/type").asText())) return null;
        JsonNode page = job.template.at("/pages/" + task.at("/pageCode").asText());
        JsonNode parsed = null;
        try {
            Parser parser = nodeService.parser(
                    page.at("/id").asText() + "-" + job.updatedTime, page.at("/parser").asText());
            if (null != parser) parsed = parser.parse(storage.at("/html").asText(), info.getString("url"));
        } catch (Exception e) {
            info.put("exception", ApiUtil.getStackTrace(e));
            return null;
        }
        String script = page.at("/mapper").asText();
        if (DPUtil.empty(script)) return parsed; // 未配置映射器，直接返回解析结果
        JsonNode mapped;
        try {
            mapped = eval(job, task, storage, info, parsed, script);
        } catch (ScriptException e) {
            info.put("exception", ApiUtil.getStackTrace(e));
            return null;
        }
        // 处理下一跳
        for (Map.Entry<String, JsonNode> entry : mapped.at("/fetch").properties()) {
            ObjectNode json = DPUtil.objectNode();
            json.put("id", task.at("/jobId").asText());
            ObjectNode params = json.putObject("params");
            params.put("referer", task.at("/url").asText());
            params.put("page", entry.getKey());
            params.put("args", DPUtil.stringify(entry.getValue()));
            nodeService.execute(json);
        }
        // 处理迭代
        JsonNode iterate = mapped.at("/iterate");
        if (iterate.isTextual()) {
            int iterateCount = iterateCount(job, task, info.getString("domain"));
            if (0 == iterateCount || task.at("/iterateCount").asInt() < iterateCount) {
                RedisTask redisTask = RedisTask.record(job, iterate.asText(), task.at("/args"), info.getString("url"));
                if (!DPUtil.empty(redisTask.getUrl())) {
                    nodeService.channel().putTask(redisTask);
                }
            }
        }
        return mapped.at("/collect");
    }

    public JsonNode eval(ZooJob job, JsonNode task, JsonNode storage, Document info, JsonNode parsed, String script) throws ScriptException {
        ObjectNode context = DPUtil.objectNode();
        context.put("url", info.getString("url"));
        context.putPOJO("job", job);
        context.replace("task", task);
        context.replace("body", storage.at("/html"));
        context.replace("parsed", parsed);
        context.replace("status", storage.at("/status"));
        context.replace("exception", storage.at("/exception"));
        script = SCRIPT_MAPPER + "JSON.stringify((function (context) {" + script + "})(JSON.parse(context)));";
        ScriptEngine engine = nodeService.scriptEngine();
        engine.put("context", context.toString());
        return DPUtil.parseJSON(DPUtil.parseString(engine.eval(script)));
    }

    public boolean broad(ZooJob job, JsonNode task, JsonNode storage, Document info, org.jsoup.nodes.Document document) {
        if (200 != storage.at("/status").asInt()) return true;
        if (null == job) return true;
        if (!"broad".equals(job.getTemplate().at("/type").asText())) return true;
        Set<String> links = new HashSet<>();
        for (Element element : document.select("a[href]")) {
            String href = element.attr("abs:href");
            if (!DPUtil.empty(href)) {
                links.add(href);
            }
        }
        for (Element element : document.select("iframe[src]")) {
            String href = element.attr("abs:src");
            if (!DPUtil.empty(href)) {
                links.add(href);
            }
        }
        if (!links.isEmpty()) {
            ObjectNode json = DPUtil.objectNode();
            json.put("id", task.at("/jobId").asText());
            ObjectNode params = json.putObject("params");
            params.put("referer", info.getString("url"));
            params.put("urls", DPUtil.implode("\n", links));
            nodeService.execute(json);
        }
        return true;
    }

    public static String collection(ZooJob job, String pageCode, String domain) {
        if (null == job) return "fs_spider_html";
        String collection = null;
        switch (job.template.at("/type").asText()) {
            case "broad":
            case "single": {
                JsonNode sites = job.template.at("/sites");
                collection = sites.at("/" + domain + "/collection").asText();
                if (DPUtil.empty(collection)) {
                    domain = "*";
                    collection = sites.at("/" + domain + "/collection").asText();
                }
                break;
            }
            case "plan": {
                collection = job.template.at("/pages/" + pageCode + "/collection").asText();
                break;
            }
        }
        return DPUtil.empty(collection) ? "fs_spider_html" : collection;
    }

    public static boolean retainQuery(ZooJob job, String domain) {
        if (null == job) return false;
        JsonNode sites = job.template.at("/sites");
        if (!sites.has(domain)) domain = "*";
        return sites.at("/" + domain + "/retainQuery").asBoolean();
    }

    public static boolean retainAnchor(ZooJob job, String domain) {
        if (null == job) return false;
        JsonNode sites = job.template.at("/sites");
        if (!sites.has(domain)) domain = "*";
        return sites.at("/" + domain + "/retainAnchor").asBoolean();
    }

    public static int iterateCount(ZooJob job, JsonNode task, String domain){
        if (null == job) return 0;
        int count = 0;
        switch (job.template.at("/type").asText()) {
            case "broad":
            case "single": {
                JsonNode sites = job.template.at("/sites");
                count = sites.at("/" + domain + "/iterateCount").asInt(0);
                if (0 == count) {
                    domain = "*";
                    count = sites.at("/" + domain + "/iterateCount").asInt(0);
                }
                break;
            }
            case "plan": {
                String pageCode = task.at("/pageCode").asText();
                count = job.template.at("/pages/" + pageCode + "/iterateCount").asInt();
                break;
            }
        }
        return count;
    }

    public static int retryCount(ZooJob job, JsonNode task, String domain){
        if (null == job) return 0;
        int count = 0;
        switch (job.template.at("/type").asText()) {
            case "broad":
            case "single": {
                JsonNode sites = job.template.at("/sites");
                count = sites.at("/" + domain + "/retryCount").asInt(0);
                if (0 == count) {
                    domain = "*";
                    count = sites.at("/" + domain + "/retryCount").asInt(0);
                }
                break;
            }
            case "plan": {
                String pageCode = task.at("/pageCode").asText();
                count = job.template.at("/pages/" + pageCode + "/retryCount").asInt();
                break;
            }
        }
        return count;
    }

    @Override
    public void run() {
        do {
            try {
                if (!process()) break;
            } catch (Exception e) {
                log.error("worker process error: {}", data, e);
            }
        } while ((data = nodeService.checkSynchronousQueue()) != null);
        try {
            nodeService.workerPool().returnObject(this);
        } catch (Exception e) {
            log.warn("worker return self failed", e);
        }
    }

}
