package com.iisquare.fs.app.spark.mongo;

import com.mongodb.spark.sql.connector.config.MongoConfig;
import com.mongodb.spark.sql.connector.config.ReadConfig;
import com.mongodb.spark.sql.connector.read.MongoScanBuilder;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.connector.read.Scan;
import org.apache.spark.sql.sources.Filter;
import org.apache.spark.sql.types.StructType;
import org.bson.BsonDocument;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

public class FSMongoScanBuilder extends MongoScanBuilder {

    private final StructType schema;
    private final ReadConfig readConfig;
    private final boolean isCaseSensitive;
    private List<BsonDocument> datasetAggregationPipeline;
    private Filter[] pushedFilters;
    private StructType prunedSchema;

    public FSMongoScanBuilder(StructType schema, ReadConfig readConfig) {
        super(schema, readConfig);
        this.schema = schema;
        this.readConfig = readConfig;
        this.prunedSchema = schema;
        this.isCaseSensitive =
                SparkSession.getActiveSession()
                        .map(s -> s.sessionState().conf().caseSensitiveAnalysis())
                        .getOrElse(() -> false);
        this.datasetAggregationPipeline = emptyList();
        this.pushedFilters = new Filter[0];
    }

    @Override
    public Scan build() {
        List<BsonDocument> scanAggregationPipeline = new ArrayList<>();
        scanAggregationPipeline.addAll(readConfig.getAggregationPipeline());
        scanAggregationPipeline.addAll(datasetAggregationPipeline);

        ReadConfig scanReadConfig =
                readConfig.withOption(
                        MongoConfig.READ_PREFIX + ReadConfig.AGGREGATION_PIPELINE_CONFIG,
                        scanAggregationPipeline.stream()
                                .map(BsonDocument::toJson)
                                .collect(Collectors.joining(",", "[", "]")));
        return new FSMongoScan(prunedSchema, scanReadConfig);
    }
}
