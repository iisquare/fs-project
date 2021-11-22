package com.iisquare.fs.base.core.util;

public class SQLUtil {

    public static String escape(String str) {
        if(null == str) return null;
        return str.replaceAll("'", "''");
    }

}
