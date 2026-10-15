package com.iisquare.fs.app.spark.mongo;

import com.mongodb.spark.sql.connector.config.MongoConfig;
import org.apache.spark.sql.connector.catalog.SupportsRead;
import org.apache.spark.sql.connector.catalog.Table;
import org.apache.spark.sql.connector.catalog.TableCapability;
import org.apache.spark.sql.connector.expressions.Transform;
import org.apache.spark.sql.connector.read.ScanBuilder;
import org.apache.spark.sql.types.StructType;
import org.apache.spark.sql.util.CaseInsensitiveStringMap;

import java.util.Collections;
import java.util.Set;

public class FSMongoTable implements Table, SupportsRead {

    private static final Set<TableCapability> CAPABILITIES =
            Collections.singleton(TableCapability.BATCH_READ);

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
        this.schema = schema;
        this.partitioning = partitioning;
        this.mongoConfig = mongoConfig;
    }

    @Override
    public String name() {
        return String.format(
                "%s(%s)", FSMongoTable.class.getSimpleName(), mongoConfig.toReadConfig().getNamespaceDescription());
    }

    @Override
    public StructType schema() {
        return schema;
    }

    @Override
    public Transform[] partitioning() {
        return partitioning;
    }

    @Override
    public Set<TableCapability> capabilities() {
        return CAPABILITIES;
    }

    @Override
    public ScanBuilder newScanBuilder(final CaseInsensitiveStringMap options) {
        return new FSMongoScanBuilder(
                schema, mongoConfig.toReadConfig().withOptions(options.asCaseSensitiveMap()));
    }

}
