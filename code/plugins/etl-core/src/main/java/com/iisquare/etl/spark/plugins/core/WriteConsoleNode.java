package com.iisquare.etl.spark.plugins.core;

import java.util.Map;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.SparkSession;

import com.iisquare.etl.spark.flow.Node;
import com.iisquare.jwframe.utils.DPUtil;

public class WriteConsoleNode extends Node {

	private static final long serialVersionUID = 1L;

	@Override
	public JavaRDD<Map<String, Object>> call() {
		SQLContext sqlContext = SparkSession.builder().config(sparkConf).getOrCreate().sqlContext();
		for (Node node : source) {
			JavaRDD<Map<String, Object>> rdd = node.getResult();
			if(null == rdd) continue;
			Dataset<Row> dataset = sqlContext.read().json(rdd.map(new Function<Map<String, Object>, String>() {
				private static final long serialVersionUID = 1L;
				@Override
				public String call(Map<String, Object> v1) throws Exception {
					return DPUtil.stringifyJSON(v1);
			}}));
			dataset.show();
		}
		return null;
	}

}
