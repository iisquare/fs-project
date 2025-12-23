package com.iisquare.fs.web.spider;

import com.iisquare.fs.base.mongodb.MongoCore;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JsoupTests {

    private MongoClient client;

    @Before
    public void init() {
        this.client = MongoClients.create("mongodb://root:admin888@127.0.0.1:27017/");
    }

    @After
    public void destroy() {
        client.close();
    }

    @Test
    public void linkTest() {
        MongoDatabase database = client.getDatabase("fs_project");
        MongoCollection<org.bson.Document> collection = database.getCollection("fs_spider_html");
        String url = "https://www.miit.gov.cn/zwgk/zcwj/wjfb/tz/art/2023/art_345e17e8729443eb8be3ecac76765874.html";
        try (MongoCursor<org.bson.Document> cursor = collection.find(Filters.eq(MongoCore.FIELD_ID, url)).limit(1).cursor()) {
            if (!cursor.hasNext()) {
                System.out.println("empty");
                return;
            }
            Document document = Jsoup.parse(cursor.next().getString("content"), url);
            for (Element element : document.select("iframe[src]")) {
                String href = element.attr("abs:src");
                System.out.println(href);
            }
        }
    }

}
