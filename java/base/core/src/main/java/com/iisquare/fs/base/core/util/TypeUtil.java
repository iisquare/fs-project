package com.iisquare.fs.base.core.util;

public class TypeUtil {

    public static <T> T as(Object obj) {
        return (T) obj;
    }

    public static String _string(String val) {
        return val;
    }

    public static String asString(Object obj) {
        return as(obj);
    }

    public static Double _double(Double val) {
        return val;
    }

    public static Double asDouble(Object obj) {
        return as(obj);
    }

}
