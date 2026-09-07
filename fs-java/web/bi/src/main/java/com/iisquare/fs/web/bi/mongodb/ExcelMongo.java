package com.iisquare.fs.web.bi.mongodb;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.mongodb.config.MongoConfiguration;
import com.iisquare.fs.base.mongodb.mvc.MongoBase;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 存储Excel解析后的数据，每个表格单独对应一个集合
 */
@Component
public class ExcelMongo extends MongoBase {

    @Autowired
    MongoConfiguration configuration;
    ThreadLocal<String> collection = new ThreadLocal<>();

    public ExcelMongo() {
        this.database = "fs_bi_excel";
        this.table = "t0";
    }

    public ExcelMongo switchTable(Integer excelId) {
        collection.set("t" + excelId);
        return this;
    }

    public ObjectNode config() {
        ObjectNode config = DPUtil.objectNode();
        config.put("uri", configuration.getUri());
        config.put("username", configuration.getUsername());
        config.put("password", configuration.getPassword());
        config.put("authSource", configuration.getAuthSource());
        config.put("database", getDatabase());
        return config;
    }

    @Override
    public MongoCollection<Document> collection() {
        String name = collection.get();
        return DPUtil.empty(name) ? super.collection() : collection(name);
    }

    @Override
    public Document filtration(Document document) {
        return document;
    }

}
