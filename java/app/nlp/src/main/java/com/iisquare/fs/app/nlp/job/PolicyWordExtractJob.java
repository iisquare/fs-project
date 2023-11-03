package com.iisquare.fs.app.nlp.job;

import com.iisquare.fs.app.nlp.NLPCore;
import com.iisquare.fs.app.nlp.bean.WordSDPNode;
import com.iisquare.fs.app.nlp.bean.WordTitleNode;
import com.iisquare.fs.app.spark.util.ConfigUtil;
import com.iisquare.fs.app.spark.util.SinkUtil;
import com.iisquare.fs.app.spark.util.SourceUtil;
import com.iisquare.fs.app.nlp.util.WordUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.*;
import org.apache.spark.sql.catalyst.encoders.RowEncoder;
import org.apache.spark.sql.types.DataTypes;
import scala.Tuple2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class PolicyWordExtractJob {
    public static void main(String[] args) {
        SparkConf config = ConfigUtil.spark().setAppName(PolicyWordExtractJob.class.getSimpleName());
        SparkSession session = SparkSession.builder().config(config).getOrCreate();
        // 获取政策标题和内容
        Dataset<Row> dataset = SourceUtil.mysql(session, NLPCore.SQL_POLICY_SAMPLE, 500);
        // 清除政策内容的HTML标签
        dataset = dataset.map((MapFunction<Row, Row>) row -> {
            String text = WordUtil.clean(row.getString(2));
            return RowFactory.create(row.get(0), row.get(1), text);
        }, dataset.encoder());
        // 解析段落结构生成标题大纲
        Dataset<Tuple2<String, WordTitleNode>> dn = dataset.map((MapFunction<Row, Tuple2<String, WordTitleNode>>) row -> {
            WordTitleNode node = new WordTitleNode();
            node.text = row.getString(1);
            if (node.text.length() > 200) { // 处理异常标题
                node.text = "";
            }
            String[] sections = row.getString(2).split("\n");
            for (String section : sections) {
                if (section.startsWith("附件：")) break;
                WordTitleNode title = WordUtil.title(section);
                if (null == title) {
                    WordUtil.mount(node, section);
                } else {
                    WordUtil.mount(node, title);
                    WordUtil.mount(title, section);
                }
            }
            return Tuple2.apply(row.getString(0), node);
        }, Encoders.tuple(Encoders.STRING(), Encoders.javaSerialization(WordTitleNode.class)));
        SinkUtil.mysql(dn.map(
                (MapFunction<Tuple2<String, WordTitleNode>, Row>) tuple2 -> RowFactory.create(tuple2._1, DPUtil.stringify(tuple2._2))
        , RowEncoder.apply(DataTypes.createStructType(Arrays.asList(
                DataTypes.createStructField("base_id", DataTypes.StringType, false),
                DataTypes.createStructField("level", DataTypes.StringType, false)
        )))), SaveMode.Overwrite, "fs_policy_level", 200);
        // 依存分析
        dataset = dn.flatMap(new FlatMapFunction<Tuple2<String, WordTitleNode>, Row>() {
            List<Row> level(String baseId, List<Row> result, String parent, WordTitleNode node) {
                parent += "#" + node.text;
                for (String paragraph : node.paragraphs) {
                    if (DPUtil.empty(paragraph)) continue;
                    String sentence = DPUtil.substring(parent + "#" + paragraph, -1024);
                    WordSDPNode sdp = WordUtil.sdp(sentence);
                    result.add(RowFactory.create(baseId, sentence, DPUtil.stringify(sdp)));
                }
                for (WordTitleNode child : node.children) {
                    level(baseId, result, parent, child);
                }
                return result;
            }
            @Override
            public Iterator<Row> call(Tuple2<String, WordTitleNode> tuple2) throws Exception {
                return level(tuple2._1, new ArrayList<>(), "", tuple2._2).iterator();
            }
        }, RowEncoder.apply(DataTypes.createStructType(Arrays.asList(
                DataTypes.createStructField("base_id", DataTypes.StringType, false),
                DataTypes.createStructField("sentence", DataTypes.StringType, false),
                DataTypes.createStructField("sdp", DataTypes.StringType, false)
        ))));
        SinkUtil.mysql(dataset, SaveMode.Overwrite, "fs_policy_sdp", 200);
        session.close();
    }
}
