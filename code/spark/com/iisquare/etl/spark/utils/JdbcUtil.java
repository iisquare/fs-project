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

public class JdbcUtil {

	static final Logger logger = Logger.getLogger(JdbcUtil.class);
	
	public static Connection getConnection(Map<String, String> options) {
		try {
			Class.forName(options.get("driver"));
			return DriverManager.getConnection(options.get("url"), options.get("user"), options.get("password"));
		} catch (ClassNotFoundException | SQLException e) {
			logger.error("jdbc getConnection failed", e);
			return null;
		}
	}
	
	public static List<Map<String, Object>> fetchResultSet(ResultSet rs) throws Exception {
		ResultSetMetaData rsmd = rs.getMetaData();
		List<Map<String, Object>> tempList = new ArrayList<>();
		Map<String, Object> tempHash = null;
		while (rs.next()) {
			tempHash = new LinkedHashMap<String, Object>();
			for (int i = 0; i < rsmd.getColumnCount(); i++) {
				tempHash.put(rsmd.getColumnName(i + 1), rs.getObject(rsmd.getColumnName(i + 1)));
			}
			tempList.add(tempHash);
		}
		return tempList;
	}
	
}
