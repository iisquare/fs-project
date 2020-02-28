package com.iisquare.etl.spark.utils;

import java.util.LinkedHashMap;

import org.apache.spark.sql.Row;

public class SparkUtil {

	public static LinkedHashMap<String,Object> rowToMap(Row row, int offset, String... fields) {
		LinkedHashMap<String,Object> map = new LinkedHashMap<>();
		for (int i = 0; i < fields.length; i++) {
			map.put(fields[0], row.get(i + offset));
		}
		return map;
	}
	
}
