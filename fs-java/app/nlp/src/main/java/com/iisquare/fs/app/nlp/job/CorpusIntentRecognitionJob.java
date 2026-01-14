package com.iisquare.fs.app.nlp.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.app.spark.util.ConfigUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.base.core.util.TypeUtil;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * @see(https://github.com/crealytics/spark-excel)
 */
public class CorpusIntentRecognitionJob {

    final static String excelPath = "C:\\Users\\Ouyang\\Desktop\\政策检索与查询-语料V1.1.xlsx";
    final static String jsonPath = "C:\\Users\\Ouyang\\Desktop\\alpaca_intent_recognition.json";

    public static void main(String[] args) {
        URL url = CorpusIntentRecognitionJob.class.getClassLoader().getResource("intent-template.txt");
        String template = FileUtil.getContent(url.getFile());
        SparkConf config = ConfigUtil.spark().setMaster("local").setAppName(CorpusIntentRecognitionJob.class.getSimpleName());
        SparkSession session = SparkSession.builder().config(config).getOrCreate();
        Dataset<Row> excel = session.read()
                .option("dataAddress", "'语料标注'!").option("header", "true")
                .format("com.crealytics.spark.excel").load(excelPath);
        Dataset<JsonNode> dataset = excel.map((MapFunction<Row, JsonNode>) row -> {
            String question = template.replaceAll("\\$\\{问题\\}", row.getAs("问题"));
            ArrayNode chain = DPUtil.arrayNode();
            ObjectNode answer = chain.addObject();
            answer.put("intent", TypeUtil._string(row.getAs("意图")));
            ObjectNode params = answer.putObject("params");
            for (String key : Arrays.asList("标题", "文号", "地域", "部门", "关键词", "日期", "行业", "产业")) {
                String value = row.getAs(key);
                if (!DPUtil.empty(value)) {
                    params.put(key, value);
                }
            }
            ObjectNode prompt = DPUtil.objectNode();
            prompt.put("instruction", question);
            prompt.put("input", "");
            prompt.put("output", chain.toPrettyString());
            return prompt;
        }, Encoders.javaSerialization(JsonNode.class));
        String content = DPUtil.arrayNode().addAll(dataset.collectAsList()).toPrettyString();
        FileUtil.putContent(jsonPath, content, StandardCharsets.UTF_8);
        session.close();
    }
}
