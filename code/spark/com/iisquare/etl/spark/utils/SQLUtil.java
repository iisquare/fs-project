package com.iisquare.etl.spark.utils;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.types.StructType;

public class SQLUtil {

	/**
	 * 获取ResultSet对应的字段名称
	 */
	public static StructType structType(ResultSet rs) throws SQLException {
		StructType structType = new StructType();
		ResultSetMetaData rsmd = rs.getMetaData();
		int count = rsmd.getColumnCount();
		for (int i = 1; i <= count; i++) {
			structType.add(rsmd.getColumnName(i), rsmd.getColumnTypeName(i), ResultSetMetaData.columnNoNulls != rsmd.isNullable(i));
		}
		return structType;
	}
	
	/**
	 * 读取ResultSet到List<Map<String, Object>>
	 */
	public static List<Row> fetchResultSet(ResultSet rs) throws Exception {
		List<Row> list = new ArrayList<>();
		int count = rs.getMetaData().getColumnCount();
		do {
			Object[] values = new Object[count];
			for (int i = 0; i < count; i++) {
				values[i] = rs.getObject(i);
			}
			list.add(RowFactory.create(values));
		} while (rs.next());
		return list;
	}
	
}
