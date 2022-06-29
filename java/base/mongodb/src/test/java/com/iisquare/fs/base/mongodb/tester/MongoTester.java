package com.iisquare.fs.base.mongodb.tester;

import com.iisquare.fs.base.core.util.DPUtil;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

public class MongoTester {

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
    public void writeTest() {
        MongoDatabase database = client.getDatabase("fs_project");
        MongoCollection<Document> collection = database.getCollection("fs.test");
        Date date = new Date();
        for (int index = 0; index < 2000000; index++) {
            date.setTime(date.getTime() + DPUtil.random(0, 10));
            String time = DPUtil.millis2dateTime(date.getTime(), "yyyy-MM-dd HH:mm:ss.SSS");
            Document document = new Document();
            document.put("i", index);
            document.put("xn", date.getTime());
            document.put("xd", date);
            document.put("xs", time);
            document.put("nn", date.getTime());
            document.put("nd", date);
            document.put("ns", time);
            collection.insertOne(document);
            if (index % 2000 == 0) {
                System.out.println(String.format("i-%d, n-%d, s-%s", index, date.getTime(), time));
            }
        }
    }

}
