package com.iisquare.etl.spark.plugins.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.VoidFunction;
import org.apache.spark.sql.SparkSession;

import com.iisquare.etl.spark.flow.Node;

public class TestSimpleNode extends Node {

	private static final long serialVersionUID = 1L;

	@Override
	public JavaRDD<Map<String, Object>> call() throws Exception {
		JavaSparkContext sparkContext = new JavaSparkContext(SparkSession.builder().config(sparkConf).getOrCreate().sparkContext());
		List<String> list = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			list.add("s" + i);
		}
		JavaRDD<String> rdd = sparkContext.parallelize(list);
		rdd.foreach(new VoidFunction<String>() {
			private static final long serialVersionUID = 1L;
			@Override
			public void call(String t) throws Exception {
				System.out.println(t);
			}
		});
		sparkContext.close();
		return null;
	}

}
