package com.iisquare.etl.spark.plugins.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.SparkSession;

import com.iisquare.etl.spark.flow.Node;

public class TestSimpleNode extends Node {

	private static final long serialVersionUID = 1L;

	@Override
	public JavaRDD<Map<String, Object>> call() throws Exception {
		SparkSession session = SparkSession.builder().config(sparkConf).getOrCreate();
		JavaSparkContext sparkContext = new JavaSparkContext(session.sparkContext());
		List<String> list = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			list.add("{\"s\":" + i + "}");
		}
		JavaRDD<String> rdd = sparkContext.parallelize(list);
		SQLContext sqlContext = session.sqlContext();
		Dataset<Row> dataset = sqlContext.read().json(rdd);
		dataset.show();
		sparkContext.close();
		return null;
	}

}
