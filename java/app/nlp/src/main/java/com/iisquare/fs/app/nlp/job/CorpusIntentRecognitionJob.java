package com.iisquare.fs.app.nlp.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.app.spark.util.ConfigUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.base.core.util.TypeUtil;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.FilterFunction;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import java.nio.charset.StandardCharsets;

/**
 * @see(https://github.com/crealytics/spark-excel)
 */
public class CorpusIntentRecognitionJob {

    final static String excelPath = "C:\\Users\\Ouyang\\Desktop\\大模型场景梳理.xlsx";
    final static String jsonPath = "C:\\Users\\Ouyang\\Desktop\\alpaca_intent_recognition.json";

    public static void main(String[] args) {
        SparkConf config = ConfigUtil.spark().setMaster("local").setAppName(CorpusIntentRecognitionJob.class.getSimpleName());
        SparkSession session = SparkSession.builder().config(config).getOrCreate();
        Dataset<Row> excel = session.read().option("header", "true").format("com.crealytics.spark.excel").load(excelPath);
        excel = excel.filter((FilterFunction<Row>) row -> !DPUtil.empty(TypeUtil.string(row.getAs("类别"))));
        Dataset<JsonNode> dataset = excel.map((MapFunction<Row, JsonNode>) row -> {
            StringBuilder question = new StringBuilder();
            question.append("<指令>请参考已知内容对问题进行意图识别，采用JSON格式输出问题的分类和相关参数。</指令>\n");
            question.append("<已知内容>问题分类包括政策知识库、政策检索、场景提示、搜索引擎、材料撰写、身份认同、申报检索、场景对话、工具调用、增强对话、无效提示。</已知内容>\n");
            question.append("<问题>").append(TypeUtil.string(row.getAs("问题"))).append("</问题>");
            StringBuilder answer = new StringBuilder();
//            answer.append("```json\r\n");
            ObjectNode json = DPUtil.objectNode().put("type", TypeUtil.string(row.getAs("类别")));
            answer.append(json.toPrettyString());
//            answer.append("\r\n```");
            ObjectNode prompt = DPUtil.objectNode();
            prompt.put("instruction", question.toString());
            prompt.put("input", "");
            prompt.put("output", answer.toString());
            return prompt;
        }, Encoders.javaSerialization(JsonNode.class));
        String content = DPUtil.arrayNode().addAll(dataset.collectAsList()).toPrettyString();
        FileUtil.putContent(jsonPath, content, StandardCharsets.UTF_8.name());
        session.close();
    }
}
