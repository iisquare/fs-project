package com.iisquare.fs.app.nlp.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.app.nlp.util.WordUtil;
import com.iisquare.fs.app.spark.util.ConfigUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.base.core.util.TypeUtil;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.ForeachPartitionFunction;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @see(https://github.com/crealytics/spark-excel)
 */
public class CorpusClassifyJob {

    final static String excelPath = "C:\\Users\\Ouyang\\Desktop\\dwd_policy_info_ai_202504091153.xlsx";
    final static String jsonPath = "C:\\Users\\Ouyang\\Desktop\\classify.jsonl";

    public static void main(String[] args) {
        SparkConf config = ConfigUtil.spark().setMaster("local").setAppName(CorpusClassifyJob.class.getSimpleName());
        SparkSession session = SparkSession.builder().config(config).getOrCreate();
        Dataset<Row> excel = session.read()
                .option("dataAddress", "'Sheet0'!").option("header", "true")
                .format("com.crealytics.spark.excel").load(excelPath);
        Dataset<JsonNode> dataset = excel.map((MapFunction<Row, JsonNode>) row -> {
            String text = row.getAs("title") + "\n" + WordUtil.clean(row.getAs("content"));
            List<String> labels = new ArrayList<>();
            for (String name : Arrays.asList(
                    "classify_primary_name",
                    "classify_secondary_name",
                    "supportmode"
            )) {
                labels.addAll(Arrays.asList(DPUtil.explode(row.getAs(name))));
            }
            ObjectNode item = DPUtil.objectNode();
            item.put("text", text);
            item.put("label", DPUtil.implode(labels));
            item.put("url", TypeUtil.asString(row.getAs("url")));
            return item;
        }, Encoders.javaSerialization(JsonNode.class));
        dataset.foreachPartition((ForeachPartitionFunction<JsonNode>) iterator -> {
            String charset = StandardCharsets.UTF_8.name();
            FileUtil.putContent(jsonPath, "", charset);
            while (iterator.hasNext()) {
                JsonNode node = iterator.next();
                FileUtil.putContent(jsonPath, node.toString() + "\n", true, true, charset);
            }
        });
        session.close();
    }
}
