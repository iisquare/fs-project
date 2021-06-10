package com.iisquare.fs.base.mongodb.mvc;

import com.iisquare.fs.base.mongodb.MongoCore;
import com.iisquare.fs.base.mongodb.util.MongoUtil;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import lombok.Getter;
import lombok.Setter;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

@Setter
@Getter
public abstract class MongoBase {

    @Autowired
    protected MongoClient client;
    protected String database;
    protected String table;

    public MongoCollection<Document> collection(String database, String collection) {
        MongoDatabase db = client.getDatabase(database);
        MongoCollection<Document> table = db.getCollection(collection);
        return table;
    }

    public MongoCollection<Document> collection(String collection) {
        return collection(getDatabase(), collection);
    }

    public MongoCollection<Document> collection() {
        return collection(getDatabase(), getTable());
    }

    public long count(Bson filter) {
        return collection().countDocuments(filter);
    }

    public Document one(String id) {
        MongoCursor<Document> cursor = collection().find(Filters.eq(MongoCore.FIELD_ID, id)).limit(1).cursor();
        return cursor.hasNext() ? MongoUtil.id2string(cursor.next()) : null;
    }

    public List<Document> all(Bson filter, Bson sort, Integer page, Integer pageSize) {
        MongoCollection<Document> collection = collection();
        FindIterable<Document> documents = null == filter ? collection.find() : collection.find(filter);
        if (null != sort) documents.sort(sort);
        if (null != pageSize) {
            if (null != page) documents.skip((page - 1) * pageSize);
            documents.limit(pageSize);
        }
        List<Document> result = new ArrayList<>();
        MongoCursor<Document> cursor = documents.cursor();
        while (cursor.hasNext()) {
            result.add(MongoUtil.id2string(cursor.next()));
        }
        return result;
    }

    public Document save(Document document) {
        document = filtration(MongoUtil.id2object(document, true));
        if(document.containsKey(MongoCore.FIELD_ID)) {
            collection().updateOne(Filters.eq(MongoCore.FIELD_ID, document.get(MongoCore.FIELD_ID)), new Document("$set", document));
        } else {
            collection().insertOne(document);
        }
        return MongoUtil.id2string(document);
    }

    public Document upsert(Document document) {
        if(!document.containsKey(MongoCore.FIELD_ID)) return null;
        document = filtration(document);
        Bson filter = Filters.eq(MongoCore.FIELD_ID, document.remove(MongoCore.FIELD_ID));
        UpdateOptions options = new UpdateOptions().upsert(true);
        collection().updateOne(filter, new Document("$set", document), options);
        return MongoUtil.id2string(document);
    }

    public long delete(String... ids) {
        if(ids.length < 1) return 0;
        Set<ObjectId> args = new HashSet<>();
        for (String id : ids) {
            args.add(new ObjectId(id));
        }
        return collection().deleteMany(Filters.in(MongoCore.FIELD_ID, args)).getDeletedCount();
    }

    public abstract Document filtration(Document document);

}
