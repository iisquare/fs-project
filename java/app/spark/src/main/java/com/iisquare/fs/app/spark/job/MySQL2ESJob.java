package com.iisquare.fs.app.spark.job;

import com.iisquare.fs.app.spark.util.ConfigUtil;
import com.iisquare.fs.app.spark.util.SinkUtil;
import com.iisquare.fs.app.spark.util.SourceUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.catalyst.encoders.RowEncoder;
import org.apache.spark.sql.types.DataTypes;

import java.util.Arrays;

public class MySQL2ESJob {
    public static void main(String[] args) {
        SparkConf config = ConfigUtil.spark().setAppName(MySQL2ESJob.class.getSimpleName());
        SparkSession session = SparkSession.builder().config(config).getOrCreate();
        String sql = "select base_id, base_name, type, state, province, registry_time, base_update_time from dwd_company_info";
        Dataset<Row> dataset = SourceUtil.mysql(session, sql, Integer.MIN_VALUE);
        dataset = dataset.map((MapFunction<Row, Row>) row -> RowFactory.create(
                row.getString(0), // _id
                row.getString(1), // k_tt
                row.getString(1), // k_tf
                row.getString(1), // k_ft
                row.getString(1), // k_ff
                row.getString(1), // t_tt
                row.getString(1), // t_tf
                row.getString(1), // t_ft
                row.getString(1), // t_ff
                row.getString(2), // k_type
                row.getString(2), // t_type
                row.getString(3), // k_state
                row.getString(3), // t_state
                row.getString(4), // province
                DPUtil.dateTime2millis(row.get(5), "yyyy-MM-dd"), // registry_time
                (double) Math.round(Math.random() * 1000) / 100, // comprehensive_score
                row.getTimestamp(6).getTime() // updated_time
                ), RowEncoder.apply(DataTypes.createStructType(Arrays.asList(
                DataTypes.createStructField("id", DataTypes.StringType, false),
                DataTypes.createStructField("k_tt", DataTypes.StringType, false),
                DataTypes.createStructField("k_tf", DataTypes.StringType, false),
                DataTypes.createStructField("k_ft", DataTypes.StringType, false),
                DataTypes.createStructField("k_ff", DataTypes.StringType, false),
                DataTypes.createStructField("t_tt", DataTypes.StringType, false),
                DataTypes.createStructField("t_tf", DataTypes.StringType, false),
                DataTypes.createStructField("t_ft", DataTypes.StringType, false),
                DataTypes.createStructField("t_ff", DataTypes.StringType, false),
                DataTypes.createStructField("k_type", DataTypes.StringType, false),
                DataTypes.createStructField("t_type", DataTypes.StringType, false),
                DataTypes.createStructField("k_state", DataTypes.StringType, false),
                DataTypes.createStructField("t_state", DataTypes.StringType, false),
                DataTypes.createStructField("province", DataTypes.StringType, false),
                DataTypes.createStructField("registry_time", DataTypes.LongType, false),
                DataTypes.createStructField("comprehensive_score", DataTypes.DoubleType, false),
                DataTypes.createStructField("updated_time", DataTypes.LongType, false)
        ))));
        SinkUtil.elasticsearch(dataset, "fs_test", "id", 1000);
        session.close();
    }
}
