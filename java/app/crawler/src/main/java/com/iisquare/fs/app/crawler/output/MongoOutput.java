package com.iisquare.fs.app.crawler.output;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.io.IOException;
import java.util.*;

public class MongoOutput extends Output {

    private MongoCredential credential;
    private List<ServerAddress> addresses;
    private MongoClientOptions options;
    private String database;
    private String collection;
    private String unique;
    private MongoClient client;
    private MongoCollection<Document> table;

    @Override
    public void configure(JsonNode parameters) {
        String server = parameters.get("server").asText();
        String username = parameters.get("username").asText();
        String auth = parameters.get("auth").asText();
        String password = parameters.get("password").asText();
        database = parameters.get("database").asText();
        collection = parameters.get("collection").asText();
        unique = parameters.get("unique").asText();
        credential = MongoCredential.createCredential(username, auth, password.toCharArray());
        String[] servers = DPUtil.explode(server, ",");
        addresses = new ArrayList<>();
        for (String item : servers) {
            String[] host = DPUtil.explode(item, ":");
            int port = host.length > 1 ? DPUtil.parseInt(host[1]) : 27017;
            addresses.add(new ServerAddress(host[0], port));
        }
        options = MongoClientOptions.builder().build();
    }

    @Override
    public void open() throws IOException {
        if (addresses.size() == 1) {
            client = new MongoClient(addresses.get(0), credential, options);
        } else {
            client = new MongoClient(addresses, credential, options);
        }
        MongoDatabase database = client.getDatabase(this.database);
        table = database.getCollection(this.collection);
    }

    @Override
    public void record(JsonNode data) throws Exception {
        List<Document> documents = new ArrayList<>();
        Iterator<JsonNode> iterator = array(data).elements();
        while (iterator.hasNext()) {
            Document document = new Document();
            Iterator<Map.Entry<String, JsonNode>> fields = iterator.next().fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                document.put(entry.getKey(), DPUtil.toJSON(entry.getValue(), Object.class));
            }
            if (document.size() > 0) documents.add(document);
        }
        if (documents.size() < 1) return;
        if (DPUtil.empty(unique)) {
            table.insertMany(documents);
            return;
        }
        UpdateOptions options = new UpdateOptions().upsert(true);
        for (Document document : documents) {
            Bson filter = Filters.eq(unique, document.get(unique));
            document.remove(unique);
            Bson update =  new Document("$set", document);
            table.updateOne(filter, update, options);
        }
    }

    @Override
    public void close() throws IOException {
        FileUtil.close(client);
    }
}
