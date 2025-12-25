package com.iisquare.fs.web.spider;

import com.iisquare.fs.base.jsoup.util.JsoupUtil;
import com.iisquare.fs.base.mongodb.MongoCore;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
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
        MongoCollection<org.bson.Document> collection = database.getCollection("fs_spider_index");
        String url = "http://gjzwfw.www.gov.cn/index.html";
        try (MongoCursor<org.bson.Document> cursor = collection.find(Filters.eq(MongoCore.FIELD_ID, url)).limit(1).cursor()) {
            if (!cursor.hasNext()) {
                System.out.println("empty");
                return;
            }
            Document document = Jsoup.parse(cursor.next().getString("content"), url);
            System.out.println("title:" + JsoupUtil.title(document));
            System.out.println("keywords:" + JsoupUtil.keywords(document));
            System.out.println("description:" + JsoupUtil.description(document));
        }
    }

    @Test
    public void homeTest() {
        MongoDatabase database = client.getDatabase("fs_project");
        MongoCollection<org.bson.Document> collection = database.getCollection("fs_spider_index");
        Bson filters = Filters.and(
                Filters.eq("title", ""),
                Filters.ne("content", "")
        );
        long count = collection.countDocuments(filters);
        System.out.println("count:" + count);
        try (MongoCursor<org.bson.Document> cursor = collection.find(filters).cursor()) {
            while (cursor.hasNext()) {
                org.bson.Document document = cursor.next();
                System.out.println(document.getString(MongoCore.FIELD_ID));
            }
        }
    }

}
