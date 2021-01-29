package com.iisquare.fs.spark.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.HtmlUtil;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.spark.MongoSpark;
import com.mongodb.spark.config.ReadConfig;
import com.mongodb.spark.rdd.api.java.JavaMongoRDD;
import org.apache.commons.cli.*;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.*;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;
import org.bson.Document;

import java.io.Serializable;
import java.util.*;

public class Word implements Serializable {

    public List<String> split(String s, int min, int max) {
        List<String> list = new ArrayList<>();
        if (null == s) return list;
        int length = s.length();
        for (int i = 0; i < length; i++) {
            int size = Math.min(length, i + max);
            String w = "";
            for (int j = i; j < size; j++) {
                char c = s.charAt(j);
                if (!Character.isLetterOrDigit(c)) break;
                w += c;
                if (w.length() >= min) list.add(w);
            }
        }
        return list;
    }

    public JsonNode config(String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        Options options = new Options();
        options.addOption("u", "uri", true, "mongo input uri");
        options.addOption("d", "database", true, "mongo input database");
        options.addOption("c", "collection", true, "mongo input collection");
        CommandLine commandLine = parser.parse(options, args);
        if(commandLine.hasOption("help")) {
            new HelpFormatter().printHelp("-u -d -c", options);
            System.exit(0);
        }
        ObjectNode config = DPUtil.objectNode();
        if(commandLine.hasOption("uri")) {
            config.put("uri", commandLine.getOptionValue("uri"));
        }
        if(commandLine.hasOption("database")) {
            config.put("database", commandLine.getOptionValue("database"));
        }
        if(commandLine.hasOption("collection")) {
            config.put("collection", commandLine.getOptionValue("collection"));
        }
        return config;
    }

    public Dataset<Row> datasetUseMongoSpark(SparkSession spark, JsonNode config) {
        JavaSparkContext jsc = new JavaSparkContext(spark.sparkContext());
        JavaMongoRDD<Document> rdd = MongoSpark.load(jsc, ReadConfig.create(new HashMap<String, String>() {{
            put("uri", config.get("uri").asText());
            put("database", config.get("database").asText());
            put("collection", config.get("collection").asText());
        }})).withPipeline(Arrays.asList(
                Document.parse("{$match:{extract_time:'2020-06-04'}}")
        ));
        return rdd.toDF();
    }

    public Dataset<Row> datasetUseMongoClient(SparkSession spark, JsonNode config) {
        MongoClient client = new MongoClient(new MongoClientURI(config.get("uri").asText()));
        MongoDatabase database = client.getDatabase(config.get("database").asText());
        MongoCollection<Document> collection = database.getCollection(config.get("collection").asText());
        List<Row> rows = new ArrayList<>();
        MongoCursor<Document> cursor = collection.find().limit(1).cursor();
        while (cursor.hasNext()) {
            Document document = cursor.next();
            rows.add(RowFactory.create(document.getString("robot_text")));
        }
        cursor.close();
        client.close();
        return spark.createDataFrame(rows, new StructType()
                .add("robot_text", DataTypes.StringType));
    }

    public static void main(String[] args) throws Exception {
        Word word = new Word();
        JsonNode config = word.config(args);
        SparkSession spark = SparkSession.builder().appName("word").master("local").getOrCreate();
        Dataset<Row> dataset = word.datasetUseMongoClient(spark, config);
        dataset.show();
        int index = dataset.schema().fieldIndex("robot_text");
        Dataset<String> line = dataset.map((MapFunction<Row, String>) row -> {
            String content = row.getString(index);
            return HtmlUtil.beauty(content);
        }, Encoders.STRING()).flatMap((FlatMapFunction<String, String>) s -> {
            return word.split(s, 2, 12).iterator();
        }, Encoders.STRING());
        line.show();
        Dataset<Row> count = line.groupBy("value").count().sort(new Column("count").desc());
        count.show();
        spark.close();
    }

}
