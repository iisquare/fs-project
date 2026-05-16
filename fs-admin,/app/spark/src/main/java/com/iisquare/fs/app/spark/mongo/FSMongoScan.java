package com.iisquare.fs.app.spark.mongo;

import com.mongodb.spark.sql.connector.config.ReadConfig;
import com.mongodb.spark.sql.connector.read.MongoScan;
import org.apache.spark.sql.connector.read.Batch;
import org.apache.spark.sql.types.StructType;

public class FSMongoScan extends MongoScan {

    private final StructType schema;
    private final ReadConfig readConfig;

    public FSMongoScan(StructType schema, ReadConfig readConfig) {
        super(schema, readConfig);
        this.schema = schema;
        this.readConfig = readConfig;
    }

    @Override
    public Batch toBatch() {
        return new FSMongoBatch(schema, readConfig);
    }
}
