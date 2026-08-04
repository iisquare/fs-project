package com.iisquare.fs.base.dag.core;

import java.util.LinkedHashMap;
import java.util.Map;

public class DSCore {

    public static final String FMT_NUMBER = "Number"; // 数值，小数位，截取方式（向上、向下、四舍五入）
    public static final String FMT_STRING = "String"; // 字符串
    public static final String FMT_DATE = "Date"; // 日期，格式
    public static final String FMT_LOCATION = "Location"; // 位置，坐标系；省市区，坐标，邮编等
    public static final String FMT_LEVEL = "Level"; // 层级结构；省市区多字段，上下级单字段
    public static final String FMT_UNKNOWN = "Unknown"; // 未知，不受支持

    public static final Map<String, String> jdbc = new LinkedHashMap(){{
        // MySQL types
        put("BIT", FMT_NUMBER);
        put("INT", FMT_NUMBER);
        put("TINYINT", FMT_NUMBER);
        put("SMALLINT", FMT_NUMBER);
        put("BIGINT", FMT_NUMBER);
        put("FLOAT", FMT_NUMBER);
        put("DOUBLE", FMT_NUMBER);
        put("DECIMAL", FMT_NUMBER);
        put("CHAR", FMT_STRING);
        put("VARCHAR", FMT_STRING);
        put("TEXT", FMT_STRING);
        put("TINYTEXT", FMT_STRING);
        put("MEDIUMTEXT", FMT_STRING);
        put("LONGTEXT", FMT_STRING);
        put("DATE", FMT_DATE);
        put("TIME", FMT_DATE);
        put("DATETIME", FMT_DATE);
        put("TIMESTAMP", FMT_DATE);
        // PostgreSQL types
        put("INT2", FMT_NUMBER);
        put("INT4", FMT_NUMBER);
        put("INT8", FMT_NUMBER);
        put("FLOAT4", FMT_NUMBER);
        put("FLOAT8", FMT_NUMBER);
        put("NUMERIC", FMT_NUMBER);
        put("SERIAL", FMT_NUMBER);
        put("BIGSERIAL", FMT_NUMBER);
        put("BOOL", FMT_NUMBER);
        put("BPCHAR", FMT_STRING);
        put("UUID", FMT_STRING);
        put("JSON", FMT_STRING);
        put("JSONB", FMT_STRING);
        put("BYTEA", FMT_STRING);
        // Oracle types
        put("NUMBER", FMT_NUMBER);
        put("BINARY_FLOAT", FMT_NUMBER);
        put("BINARY_DOUBLE", FMT_NUMBER);
        put("NVARCHAR2", FMT_STRING);
        put("NCHAR", FMT_STRING);
        put("NCLOB", FMT_STRING);
        put("CLOB", FMT_STRING);
        put("BLOB", FMT_STRING);
        put("RAW", FMT_STRING);
        put("ROWID", FMT_STRING);
        // SQL Server types
        put("NVARCHAR", FMT_STRING);
        put("NCHAR", FMT_STRING);
        put("NTEXT", FMT_STRING);
        put("MONEY", FMT_NUMBER);
        put("SMALLMONEY", FMT_NUMBER);
        put("UNIQUEIDENTIFIER", FMT_STRING);
        put("XML", FMT_STRING);
        // Generic / cross-DB types
        put("BINARY", FMT_STRING);
        put("VARBINARY", FMT_STRING);
        put("LONGVARBINARY", FMT_STRING);
        put("ARRAY", FMT_STRING);
        put("OTHER", FMT_STRING);
        // ClickHouse types
        put("UINT8", FMT_NUMBER);
        put("UINT16", FMT_NUMBER);
        put("UINT32", FMT_NUMBER);
        put("UINT64", FMT_NUMBER);
        put("INT8", FMT_NUMBER);
        put("INT16", FMT_NUMBER);
        put("INT32", FMT_NUMBER);
        put("INT64", FMT_NUMBER);
        put("FLOAT32", FMT_NUMBER);
        put("FLOAT64", FMT_NUMBER);
        put("STRING", FMT_STRING);
        put("FIXEDSTRING", FMT_STRING);
        put("ENUM", FMT_STRING);
        put("UUID", FMT_STRING);
        put("IPv4", FMT_STRING);
        put("IPv6", FMT_STRING);
    }};

    public static final Map<String, String> jdbcDrivers = new LinkedHashMap(){{
        put("MySQL", "com.mysql.cj.jdbc.Driver");
        put("PostgreSQL", "org.postgresql.Driver");
        put("Oracle", "oracle.jdbc.OracleDriver");
        put("SQLServer", "com.microsoft.sqlserver.jdbc.SQLServerDriver");
        put("ClickHouse", "com.clickhouse.jdbc.ClickHouseDriver");
        put("H2", "org.h2.Driver");
        put("SQLite", "org.sqlite.JDBC");
    }};

    public static String jdbc2format(String name) {
        if (null == name) return FMT_UNKNOWN;
        return jdbc.getOrDefault(name.toUpperCase(), FMT_UNKNOWN);
    }

}
