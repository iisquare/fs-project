package com.iisquare.fs.app.spark.job;

import com.iisquare.fs.app.spark.demo.DemoConfig;
import com.iisquare.fs.app.spark.demo.DemoSource;
import org.apache.spark.SparkConf;
import org.apache.spark.sql.*;

import java.util.Arrays;
import java.util.List;

public class SessionJob {
    public static void main(String[] args) {
        List<String> sqlList = Arrays.asList(
                "select * from fs_member_user limit 2",
                "select * from t limit 2",
                "select * from fs_member_user u join t t on u.id=t.id limit 2"
        );
        SparkConf config = DemoConfig.spark().setAppName(SessionJob.class.getSimpleName());
        SparkSession session = SparkSession.builder().config(config).getOrCreate();
        SQLContext sqlContext = session.sqlContext();
        sqlContext.registerDataFrameAsTable(DemoSource.mysql(session,
                "select * from fs_project.fs_member_user"
                , Integer.MIN_VALUE), "fs_member_user");
        sqlContext.registerDataFrameAsTable(DemoSource.mysql(session,
                "select * from fs_test.t"
                , Integer.MIN_VALUE), "t");
        for (String sql : sqlList) {
            long time = System.currentTimeMillis();
            System.out.println("sql: " + sql);
            Dataset<Row> dataset = sqlContext.sql(sql);
            dataset.show();
            System.out.println("coast: " + (System.currentTimeMillis() - time));
        }
        session.close();
    }
}
