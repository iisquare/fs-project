package com.iisquare.etl.spark.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.types.StructType;

public class JdbcUtil {

	static final Logger logger = Logger.getLogger(JdbcUtil.class);
	
	/**
	 * 获取连接对象
	 */
	public static Connection getConnection(Map<String, String> options) {
		try {
			Class.forName(options.get("driver"));
			return DriverManager.getConnection(options.get("url"), options.get("user"), options.get("password"));
		} catch (ClassNotFoundException | SQLException e) {
			logger.error("jdbc getConnection failed", e);
			return null;
		}
	}
	
	/**
	 * 将Row转换为Map
	 */
	public static Map<String, Object> rowToMap (Row row) {
		StructType structType = row.schema();
		String[] fieldNames = structType.fieldNames();
		Map<String, Object> map = new LinkedHashMap<>();
		for (int i = 0; i < fieldNames.length; i++) {
			map.put(fieldNames[i], row.get(i));
		}
		return map;
	}
	
	/**
	 * 读取ResultSet到List<Map<String, Object>>
	 */
	public static List<Map<String, Object>> fetchResultSet(ResultSet rs) throws Exception {
		ResultSetMetaData rsmd = rs.getMetaData();
		List<Map<String, Object>> list = new ArrayList<>();
		int count = rsmd.getColumnCount();
		do {
			Map<String, Object> item = new LinkedHashMap<>();
			for (int i = 1; i <= count; i++) {
				item.put(rsmd.getColumnName(i), rs.getObject(i));
			}
			list.add(item);
		} while (rs.next());
		return list;
	}
	
}
