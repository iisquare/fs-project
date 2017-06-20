package com.iisquare.etl.test.spark;

import java.util.Map;

import net.sf.json.JSONObject;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.SparkSession;
import com.iisquare.etl.spark.flow.Node;
import com.iisquare.etl.spark.utils.SQLUtil;
import com.iisquare.jwframe.utils.DPUtil;

public class CalculateSQLNode extends Node {

	private static final long serialVersionUID = 1L;

	@Override
	public JavaRDD<Map<String, Object>> process() throws Exception {
		SQLContext sqlContext = SparkSession.builder().config(sparkConf).getOrCreate().sqlContext();
		for (Node node : source) {
			String viewName = node.getProperties().getProperty("alias");
			if(DPUtil.empty(viewName)) viewName = node.getProperties().getProperty("node");
			JavaRDD<Map<String, Object>> rdd = node.getResult();
			Dataset<Row> dataset = sqlContext.read().json(rdd.map(new Function<Map<String, Object>, String>() {
				private static final long serialVersionUID = 1L;
				@Override
				public String call(Map<String, Object> v1) throws Exception {
					return JSONObject.fromObject(v1).toString();
			}}));
			dataset.createTempView(viewName);
		}
		Dataset<Row> dataset = sqlContext.sql(properties.getProperty("sql"));
		return dataset.toJavaRDD().map(new Function<Row, Map<String, Object>>() {
			private static final long serialVersionUID = 1L;
			@Override
			public Map<String, Object> call(Row v1) throws Exception {
				return SQLUtil.rowToMap(v1);
			}
		});
	}

}
