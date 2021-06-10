package com.iisquare.fs.web.demo.mongodb;

import com.iisquare.fs.base.mongodb.mvc.MongoBase;
import org.bson.Document;
import org.springframework.stereotype.Component;

@Component
public class TestMongo extends MongoBase {

    public TestMongo() {
        this.database = "fs_project";
        this.table = "fs.demo.test";
    }

    @Override
    public Document filtration(Document document) {
        return document;
    }

}
