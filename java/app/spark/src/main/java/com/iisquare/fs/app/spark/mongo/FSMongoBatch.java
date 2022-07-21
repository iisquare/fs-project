package com.iisquare.fs.app.spark.mongo;

import com.mongodb.spark.sql.connector.config.ReadConfig;
import com.mongodb.spark.sql.connector.read.MongoBatch;
import com.mongodb.spark.sql.connector.read.MongoInputPartition;
import com.mongodb.spark.sql.connector.read.partitioner.PartitionerHelper;
import com.mongodb.spark.sql.connector.schema.BsonDocumentToRowConverter;
import org.apache.spark.sql.connector.read.InputPartition;
import org.apache.spark.sql.connector.read.PartitionReaderFactory;
import org.apache.spark.sql.types.StructType;

public class FSMongoBatch extends MongoBatch {

    private final BsonDocumentToRowConverter bsonDocumentToRowConverter;
    private final ReadConfig readConfig;

    public FSMongoBatch(StructType schema, ReadConfig readConfig) {
        super(schema, readConfig);
        this.bsonDocumentToRowConverter = new BsonDocumentToRowConverter(schema);
        this.readConfig = readConfig;
    }

    @Override
    public InputPartition[] planInputPartitions() {
        // 直接采用单分区
        return PartitionerHelper.SINGLE_PARTITIONER.generatePartitions(readConfig).toArray(new MongoInputPartition[0]);
    }

    @Override
    public PartitionReaderFactory createReaderFactory() {
        return new FSMongoPartitionReaderFactory(bsonDocumentToRowConverter, readConfig);
    }
}
