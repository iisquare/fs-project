package com.iisquare.etl.test.spark;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import org.apache.spark.rdd.JdbcRDD;
import org.apache.spark.rdd.JdbcRDD.ConnectionFactory;

import com.iisquare.etl.spark.flow.Node;
import com.iisquare.etl.spark.utils.JdbcUtil;
import com.iisquare.jwframe.utils.ValidateUtil;

public class ReadMySQLNode extends Node {

	@Override
	public Object process() {
		System.out.println(this.getClass());
		// 执行参数
		int numPartitions = ValidateUtil.filterInteger(properties.get("numPartitions"), true, 1, null, 3);
		Long lowerBound = ValidateUtil.filterLong(properties.get("lowerBound"), true, 0L, null, 0L);
		Long upperBound = ValidateUtil.filterLong(properties.get("upperBound"), true, 0L, null, Long.MAX_VALUE);
		final Map<String, String> mysqlOptions = new HashMap<>();
		return JdbcRDD.create(sparkContext, new ConnectionFactory() {
			private static final long serialVersionUID = 1L;
			@Override
			public Connection getConnection() throws Exception {
				return JdbcUtil.getConnection(mysqlOptions);
			}
		}, properties.getProperty("sql"), lowerBound, upperBound, numPartitions);
	}

}
