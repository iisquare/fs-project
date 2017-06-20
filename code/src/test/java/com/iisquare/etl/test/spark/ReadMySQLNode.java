package com.iisquare.etl.test.spark;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.rdd.JdbcRDD;
import org.apache.spark.rdd.JdbcRDD.ConnectionFactory;
import org.apache.spark.sql.SparkSession;

import com.iisquare.etl.spark.flow.Node;
import com.iisquare.etl.spark.utils.JdbcUtil;
import com.iisquare.etl.spark.utils.SQLUtil;
import com.iisquare.jwframe.utils.ValidateUtil;

public class ReadMySQLNode extends Node {

	private static final long serialVersionUID = 1L;
	
	@Override
	public JavaRDD<Map<String, Object>> call() {
		// 执行参数
		int numPartitions = ValidateUtil.filterInteger(properties.get("numPartitions"), true, 1, null, 1);
		Long lowerBound = ValidateUtil.filterLong(properties.get("lowerBound"), true, 0L, null, 0L);
		Long upperBound = ValidateUtil.filterLong(properties.get("upperBound"), true, 0L, null, Long.MAX_VALUE);
		final Map<String, String> mysqlOptions = new HashMap<>();
		StringBuilder sb = new StringBuilder("jdbc:mysql://");
		sb.append(properties.getProperty("host"));
		sb.append(":").append(properties.getProperty("port"));
		sb.append("/").append(properties.getProperty("database"));
		sb.append("?characterEncoding=").append(properties.getProperty("charset"));
		mysqlOptions.put("url", sb.toString());
		mysqlOptions.put("driver", "com.mysql.jdbc.Driver");
		mysqlOptions.put("user", properties.getProperty("username"));
		mysqlOptions.put("password", properties.getProperty("password"));
		JavaSparkContext sparkContext = new JavaSparkContext(SparkSession.builder().config(sparkConf).getOrCreate().sparkContext());
		JavaRDD<List<Map<String, Object>>> rdd = JdbcRDD.create(sparkContext, new ConnectionFactory() {
			private static final long serialVersionUID = 1L;
			@Override
			public Connection getConnection() throws Exception {
				return JdbcUtil.getConnection(mysqlOptions);
			}
		}, properties.getProperty("sql"), lowerBound, upperBound, numPartitions, new Function<ResultSet, List<Map<String, Object>>>() {
			private static final long serialVersionUID = 1L;
			@Override
			public List<Map<String, Object>> call(ResultSet v1) throws Exception {
				return SQLUtil.fetchResultSet(v1);
			}
		});
		return rdd.flatMap(new FlatMapFunction<List<Map<String, Object>>, Map<String, Object>> () {
			private static final long serialVersionUID = 1L;
			@Override
			public Iterator<Map<String, Object>> call(List<Map<String, Object>> t) throws Exception {
				return t.iterator();
			}
		});
	}

}
