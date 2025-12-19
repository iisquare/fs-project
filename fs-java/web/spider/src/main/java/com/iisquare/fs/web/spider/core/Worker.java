package com.iisquare.fs.web.spider.core;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.jsoup.util.JsoupUtil;
import com.iisquare.fs.base.mongodb.MongoCore;
import com.iisquare.fs.web.spider.service.NodeService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
@Setter
@Slf4j
public class Worker implements Runnable {

    private Thread thread;
    private ObjectNode data;
    private NodeService nodeService;

    public Worker() {}

    public void call(NodeService nodeService, ObjectNode data) {
        this.nodeService = nodeService;
        this.data = data;
        this.thread.start();
    }

    private boolean process() {
        Document info = new Document();
        info.put(MongoCore.FIELD_ID, data.at("/task/url").asText());
        info.put("job_id", data.at("/task/jobId").asText());
        info.put("url", data.at("/storage/location").asText());
        if (DPUtil.empty(info.getString("url"))) {
            info.put("url", info.getString(MongoCore.FIELD_ID));
        }
        info.put("type", "");
        info.put("content_type", data.at("/storage/media").asText());
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
        info.put("content", data.at("/storage/html").asText());
        org.jsoup.nodes.Document document = Jsoup.parse(info.getString("content"), info.getString("url"));
        info.put("title", JsoupUtil.title(document));
        info.put("keywords", Arrays.asList(DPUtil.explode(",", JsoupUtil.keywords(document))));
        info.put("description", JsoupUtil.description(document));
        info.put("response_status", data.at("/storage/status").asInt());
        info.put("response_time", data.at("/storage/coast").asInt());
        info.put("exception", data.at("/storage/exception").asText());
        info.put("referer", data.at("/task/referer").asText());
        long time = System.currentTimeMillis();
        info.put("created_time", time);
        info.put("updated_time", time);
        info.put("deleted_time", 0L);
        info = nodeService.htmlMongo.upsert(info, "created_time");
        if (200 != info.getInteger("response_status")) return true;
        List<String> links = new ArrayList<>();
        for (Element element : document.select("a[href]")) {
            String href = element.attr("abs:href");
            if (!DPUtil.empty(href)) {
                links.add(href);
            }
        }
        if (!links.isEmpty()) {
            ObjectNode json = DPUtil.objectNode();
            json.put("id", data.at("/task/jobId").asText());
            json.put("referer", data.at("/task/url").asText());
            ObjectNode params = json.putObject("params");
            params.put("urls", DPUtil.implode("\n", links));
            nodeService.execute(json);
        }
        return true;
    }

    @Override
    public void run() {
        do {
            try {
                if (!process()) break;
            } catch (Exception e) {
                log.error("worker process error", e);
            }
        } while ((data = nodeService.checkSynchronousQueue()) != null);
        try {
            nodeService.workerPool().returnObject(this);
        } catch (Exception e) {
            log.warn("worker return self failed", e);
        }
    }

}
