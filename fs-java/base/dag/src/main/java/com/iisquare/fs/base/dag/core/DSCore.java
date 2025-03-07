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
    }};

    public static String jdbc2format(String name) {
        return jdbc.getOrDefault(name, FMT_UNKNOWN);
    }

}
