package com.iisquare.fs.web.demo.mongodb;

import com.iisquare.fs.base.mongodb.mvc.MongoBase;
import org.bson.Document;
import org.springframework.stereotype.Component;

@Component
public class DemoTestMongo extends MongoBase {

    public DemoTestMongo() {
        this.database = "fs_project";
        this.table = "fs_demo_test";
    }

    @Override
    public Document filtration(Document document) {
        return document;
    }

}
