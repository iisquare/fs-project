package com.iisquare.fs.app.spark.mongo;

import com.mongodb.spark.sql.connector.MongoTable;
import com.mongodb.spark.sql.connector.config.MongoConfig;
import org.apache.spark.sql.connector.expressions.Transform;
import org.apache.spark.sql.connector.read.ScanBuilder;
import org.apache.spark.sql.types.StructType;
import org.apache.spark.sql.util.CaseInsensitiveStringMap;

public class FSMongoTable extends MongoTable {
    private final StructType schema;
    private final Transform[] partitioning;
    private final MongoConfig mongoConfig;

    public FSMongoTable(MongoConfig mongoConfig) {
        this(new StructType(), mongoConfig);
    }

    public FSMongoTable(StructType schema, MongoConfig mongoConfig) {
        this(schema, new Transform[0], mongoConfig);
    }

    public FSMongoTable(StructType schema, Transform[] partitioning, MongoConfig mongoConfig) {
        super(schema, partitioning, mongoConfig);
        this.schema = schema;
        this.partitioning = partitioning;
        this.mongoConfig = mongoConfig;
    }

    @Override
    public ScanBuilder newScanBuilder(final CaseInsensitiveStringMap options) {
        return new FSMongoScanBuilder(
                schema, mongoConfig.toReadConfig().withOptions(options.asCaseSensitiveMap()));
    }

}
