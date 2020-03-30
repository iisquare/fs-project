package com.iisquare.fs.flink.util;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class DateUtil {

    public static final Map<String, Locale> locales;

    static {
        locales = new LinkedHashMap<>();
        locales.put("en", Locale.ENGLISH);
        locales.put("fr", Locale.FRENCH);
        locales.put("de", Locale.GERMAN);
        locales.put("it", Locale.ITALIAN);
        locales.put("ja", Locale.JAPANESE);
        locales.put("ko", Locale.KOREAN);
        locales.put("zh", Locale.CHINESE);
        locales.put("zh_CN", Locale.SIMPLIFIED_CHINESE);
        locales.put("zh_TW", Locale.TRADITIONAL_CHINESE);
        locales.put("fr_FR", Locale.FRANCE);
        locales.put("de_DE", Locale.GERMANY);
        locales.put("it_IT", Locale.ITALY);
        locales.put("ja_JP", Locale.JAPAN);
        locales.put("ko_KR", Locale.KOREA);
        locales.put("en_GB", Locale.UK);
        locales.put("en_US", Locale.US);
        locales.put("en_CA", Locale.CANADA);
        locales.put("fr_CA", Locale.CANADA_FRENCH);
    }

    public static Locale locale(String locale) {
        Locale result = locales.get(locale);
        if(null == result) return Locale.ENGLISH;
        return result;
    }
}
