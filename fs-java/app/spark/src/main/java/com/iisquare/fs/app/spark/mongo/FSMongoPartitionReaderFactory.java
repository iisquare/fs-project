package com.iisquare.fs.app.spark.mongo;

import com.mongodb.spark.sql.connector.assertions.Assertions;
import com.mongodb.spark.sql.connector.config.ReadConfig;
import com.mongodb.spark.sql.connector.read.MongoInputPartition;
import com.mongodb.spark.sql.connector.schema.BsonDocumentToRowConverter;
import org.apache.spark.sql.catalyst.InternalRow;
import org.apache.spark.sql.connector.read.InputPartition;
import org.apache.spark.sql.connector.read.PartitionReader;
import org.apache.spark.sql.connector.read.PartitionReaderFactory;

import static java.lang.String.format;

public class FSMongoPartitionReaderFactory implements PartitionReaderFactory {

    private final BsonDocumentToRowConverter bsonDocumentToRowConverter;
    private final ReadConfig readConfig;

    public FSMongoPartitionReaderFactory(BsonDocumentToRowConverter bsonDocumentToRowConverter, ReadConfig readConfig) {
        this.bsonDocumentToRowConverter = bsonDocumentToRowConverter;
        this.readConfig = readConfig;
    }

    @Override
    public PartitionReader<InternalRow> createReader(final InputPartition partition) {
        Assertions.ensureState(
                () -> partition instanceof MongoInputPartition,
                () ->
                        format(
                                "Unsupported InputPartition type, a MongoInputPartition instance is required. Got: %s",
                                partition.getClass()));
        return new FSMongoPartitionReader(
                (MongoInputPartition) partition, bsonDocumentToRowConverter, readConfig);
    }
}
