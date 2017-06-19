package com.iisquare.etl.spark.utils;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SQLUtil {

	/**
	 * 读取ResultSet到List<Map<String, Object>>
	 */
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
