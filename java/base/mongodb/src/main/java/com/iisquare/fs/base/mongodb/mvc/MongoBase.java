package com.iisquare.fs.base.mongodb.mvc;

import com.iisquare.fs.base.mongodb.MongoCore;
import com.iisquare.fs.base.mongodb.util.MongoUtil;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
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
        MongoCursor<Document> cursor = collection().find(Filters.eq(MongoCore.FIELD_ID, new ObjectId(id))).limit(1).cursor();
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
        return upsert(document, null);
    }

    public Document upsert(Document set, Document setOnInsert) {
        if(!set.containsKey(MongoCore.FIELD_ID)) return null;
        set = filtration(set);
        Bson filter = Filters.eq(MongoCore.FIELD_ID, set.remove(MongoCore.FIELD_ID));
        UpdateOptions options = new UpdateOptions().upsert(true);
        Document update = new Document("$set", set);
        if (null != setOnInsert) update.put("$setOnInsert", setOnInsert);
        collection().updateOne(filter, update, options);
        return MongoUtil.id2string(set);
    }

    public BulkWriteResult bulkWrite(List<Document> documents, String... insertFields) {
        List<WriteModel<Document>> list = new ArrayList<>();
        for (Document document : documents) {
            document = filtration(MongoUtil.id2object(document, true));
            if(document.containsKey(MongoCore.FIELD_ID)) {
                list.add(new InsertOneModel<>(document));
                continue;
            }
            Bson filter = Filters.eq(MongoCore.FIELD_ID, document.remove(MongoCore.FIELD_ID));
            UpdateOptions options = new UpdateOptions().upsert(true);
            Document insert = new Document();
            for (String field : insertFields) {
                if (document.containsKey(field)) {
                    insert.put(field, document.remove(field));
                }
            }
            Document update = new Document("$set", document);
            if (insert.size() > 0) update.put("$setOnInsert", insert);
            list.add(new UpdateOneModel<>(filter, update, options));
        }

        return collection().bulkWrite(list);
    }

    public long delete(String... ids) {
        if(ids.length < 1) return 0;
        Set<ObjectId> args = new HashSet<>();
        for (String id : ids) {
            args.add(new ObjectId(id));
        }
        return delete(Filters.in(MongoCore.FIELD_ID, args));
    }

    public long delete(Bson filter) {
        return collection().deleteMany(filter).getDeletedCount();
    }

    public abstract Document filtration(Document document);

}
