package com.iisquare.fs.app.nlp.job;

import com.iisquare.fs.app.nlp.bean.WordTitleNode;
import com.iisquare.fs.app.nlp.util.SinkUtil;
import com.iisquare.fs.app.nlp.util.SourceUtil;
import com.iisquare.fs.app.nlp.util.WordUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.*;
import org.apache.spark.sql.catalyst.encoders.RowEncoder;
import org.apache.spark.sql.types.DataTypes;
import scala.Tuple2;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public class WordExtractJob implements Serializable {

    public static void main(String[] args) {
        SparkConf config = new SparkConf().setAppName(WordExtractJob.class.getSimpleName());
        if (DPUtil.empty(System.getenv("spark.master"))) config.setMaster("local[*]");
        SparkSession session = SparkSession.builder().config(config).getOrCreate();
        // 获取政策标题和内容
        String sql = "select base_id, title, content from t_policy";
        Dataset<Row> dataset = SourceUtil.mysql(session, sql, 500);
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
                if (null == title) continue;
                WordUtil.mount(node, title);
            }
            return Tuple2.apply(row.getString(0), node);
        }, Encoders.tuple(Encoders.STRING(), Encoders.javaSerialization(WordTitleNode.class)));
        SinkUtil.mysql(dn.map(
                (MapFunction<Tuple2<String, WordTitleNode>, Row>) tuple2 -> RowFactory.create(tuple2._1, DPUtil.stringify(tuple2._2))
        , RowEncoder.apply(DataTypes.createStructType(Arrays.asList(
                DataTypes.createStructField("base_id", DataTypes.StringType, false),
                DataTypes.createStructField("level", DataTypes.StringType, false)
        )))), SaveMode.Overwrite, "fs_word_level", 200);
        // 生成标签词汇
        dataset = dn.flatMap(new FlatMapFunction<Tuple2<String, WordTitleNode>, Row>() {
            List<Row> level(List<Row> result, WordTitleNode root, String parent, WordTitleNode node, int minLength, int maxLength) {
                parent += "#" + node.text;
                List<Tuple2<String, Double>> words = WordUtil.words(node.text, minLength, maxLength);
                for (Tuple2<String, Double> tuple2 : words) {
                    result.add(RowFactory.create(tuple2._1, tuple2._2, root.text, parent));
                }
                for (WordTitleNode child : node.children) {
                    level(result, root, parent, child,  minLength, maxLength);
                }
                return result;
            }
            @Override
            public Iterator<Row> call(Tuple2<String, WordTitleNode> tuple2) throws Exception {
                return level(new ArrayList<>(), tuple2._2, "", tuple2._2, 4, 8).iterator();
            }
        }, RowEncoder.apply(DataTypes.createStructType(Arrays.asList(
                DataTypes.createStructField("name", DataTypes.StringType, false),
                DataTypes.createStructField("score", DataTypes.DoubleType, false),
                DataTypes.createStructField("title", DataTypes.StringType, false),
                DataTypes.createStructField("level", DataTypes.StringType, false)
        ))));
        SinkUtil.mysql(dataset, SaveMode.Overwrite, "fs_word_gram", 200);
        // 统计凝固度
        dataset.createOrReplaceTempView("t_word_gram");
        sql = "select name, count(name) as ct from t_word_gram group by name having ct > 1";
        List<Row> wct = session.sql(sql).collectAsList();
        long size = wct.size();
        Map<String, Object[]> acc = new LinkedHashMap<>();
        // 0-name, 1-score, 2-word_count, 3-under_left_count, 4-over_left_count, 5-under_right_count, 6-over_right_count, 7-total_count
        for (Row la : wct) {
            String na = la.getString(0);
            Object[] ma = acc.computeIfAbsent(na, s -> new Object[]{na, 0.0, la.getLong(1), 0L, 0L, 0L, 0L, size});
            for (Row lb : wct) {
                String nb = lb.getString(0);
                Object[] mb = acc.computeIfAbsent(nb, s -> new Object[]{nb, 0.0, lb.getLong(1), 0L, 0L, 0L, 0L, size});
                if (na.startsWith(nb)) {
                    ma[4] = (long) ma[4] + 1;
                    mb[3] = (long) ma[3] + 1;
                }
                if (na.endsWith(nb)) {
                    ma[6] = (long) ma[6] + 1;
                    mb[5] = (long) ma[5] + 1;
                }
            }
        }
        dataset = session.createDataset(acc.values().stream().map(RowFactory::create).collect(Collectors.toList()), RowEncoder.apply(DataTypes.createStructType(Arrays.asList(
                DataTypes.createStructField("name", DataTypes.StringType, false),
                DataTypes.createStructField("score", DataTypes.DoubleType, false),
                DataTypes.createStructField("word_count", DataTypes.LongType, false),
                DataTypes.createStructField("under_left_count", DataTypes.LongType, false),
                DataTypes.createStructField("over_left_count", DataTypes.LongType, false),
                DataTypes.createStructField("under_right_count", DataTypes.LongType, false),
                DataTypes.createStructField("over_right_count", DataTypes.LongType, false),
                DataTypes.createStructField("total_count", DataTypes.LongType, false)
        ))));
        SinkUtil.mysql(dataset, SaveMode.Overwrite, "fs_word_acc", 200);
        session.close();
    }

}
