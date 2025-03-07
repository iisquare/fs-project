package com.iisquare.fs.app.nlp.job;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.Segment;
import com.hankcs.hanlp.seg.common.Term;
import com.iisquare.fs.app.nlp.NLPCore;
import com.iisquare.fs.app.spark.util.ConfigUtil;
import com.iisquare.fs.app.spark.util.SinkUtil;
import com.iisquare.fs.app.spark.util.SourceUtil;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.catalyst.encoders.RowEncoder;
import org.apache.spark.sql.types.DataTypes;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PolicyTermExtractJob {
    public static void main(String[] args) {
        SparkConf config = ConfigUtil.spark().setAppName(PolicyTermExtractJob.class.getSimpleName());
        SparkSession session = SparkSession.builder().config(config).getOrCreate();
        Dataset<Row> dataset = SourceUtil.mysql(session, NLPCore.SQL_POLICY, 500);
        dataset = dataset.map(new MapFunction<Row, Row>() {
            transient Segment segment = null;
            @Override
            public Row call(Row row) throws Exception {
                if (null == segment) {
                    segment = HanLP.newSegment().enableAllNamedEntityRecognize(true).enableOffset(true).enableIndexMode(false);
                }
                List<String> terms = new ArrayList<>();
                for (Term term : segment.seg(row.getString(1))) {
                    terms.add(term.word);
                }
                Timestamp timestamp = row.getTimestamp(3);
                if (timestamp.getTime() < 0) {
                    timestamp.setTime(0L);
                } else {
                    timestamp.setTime(timestamp.getTime() + 8 * 60 * 60 * 1000);
                }
                return RowFactory.create(row.get(0), row.get(1), terms.toArray(new String[0]), timestamp);
            }
        }, RowEncoder.apply(DataTypes.createStructType(Arrays.asList(
                DataTypes.createStructField("id", DataTypes.StringType, false),
                DataTypes.createStructField("title", DataTypes.StringType, false),
                DataTypes.createStructField("term", DataTypes.createArrayType(DataTypes.StringType), false),
                DataTypes.createStructField("time", DataTypes.TimestampType, false)
        ))));
        SinkUtil.elasticsearch(dataset, "fs_policy_term", "id", 200);
        session.close();
    }
}
