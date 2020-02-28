package com.iisquare.etl.spark.plugins.core;

import java.util.Map;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.SparkSession;

import com.iisquare.etl.spark.flow.Node;
import com.iisquare.etl.spark.utils.JdbcUtil;
import com.iisquare.jwframe.utils.DPUtil;

public class CalculateSQLNode extends Node {

	private static final long serialVersionUID = 1L;

	@Override
	public JavaRDD<Map<String, Object>> call() throws Exception {
		SQLContext sqlContext = SparkSession.builder().config(sparkConf).getOrCreate().sqlContext();
		for (Node node : source) {
			String viewName = node.getProperties().getProperty("alias");
			if(DPUtil.empty(viewName)) viewName = node.getProperties().getProperty("node");
			JavaRDD<Map<String, Object>> rdd = node.getResult();
			Dataset<Row> dataset = sqlContext.read().json(rdd.map(new Function<Map<String, Object>, String>() {
				private static final long serialVersionUID = 1L;
				@Override
				public String call(Map<String, Object> v1) throws Exception {
					return DPUtil.stringifyJSON(v1);
			}}));
			dataset.createTempView(viewName);
		}
		Dataset<Row> dataset = sqlContext.sql(properties.getProperty("sql"));
		return dataset.toJavaRDD().map(new Function<Row, Map<String, Object>>() {
			private static final long serialVersionUID = 1L;
			@Override
			public Map<String, Object> call(Row v1) throws Exception {
				return JdbcUtil.rowToMap(v1);
			}
		});
	}

}
