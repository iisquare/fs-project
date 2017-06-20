package com.iisquare.etl.spark.utils;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.spark.sql.Row;
import org.apache.spark.sql.types.StructType;

public class SQLUtil {

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
