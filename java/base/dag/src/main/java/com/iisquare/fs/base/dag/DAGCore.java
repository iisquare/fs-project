package com.iisquare.fs.base.dag;

import com.iisquare.fs.base.core.util.DPUtil;

import java.util.*;

public class DAGCore {

    public static final String TYPE_FIELD = "cls";

    public static Map<String, Map<String, Object>> clsTypes = new LinkedHashMap(){{
        for (Class cls : Arrays.asList(
                Boolean.class,
                Byte.class,
                Double.class,
                Float.class,
                Integer.class,
                Long.class,
                Short.class,
                String.class,
                Date.class
        )) {
            String name = cls.getSimpleName();
            put(name, new LinkedHashMap(){{
                put(TYPE_FIELD, cls);
                put("name", name);
                put("classname", cls.getName());
            }});
        }
        for (Class cls : Arrays.asList(
                java.sql.Date.class,
                java.sql.Timestamp.class
        )) {
            String name = "SQL" + cls.getSimpleName();
            put(name, new LinkedHashMap(){{
                put(TYPE_FIELD, cls);
                put("name", name);
                put("classname", cls.getName());
            }});
        }
    }};

    public static final Map<String, Map<String, Object>> timezones = new LinkedHashMap() {{
        for (String timezone : Arrays.asList(
                "GMT",
                "GMT+1",
                "GMT+2",
                "GMT+3",
                "GMT+4",
                "GMT+5",
                "GMT+6",
                "GMT+7",
                "GMT+8",
                "GMT+9",
                "GMT+10",
                "GMT+11",
                "GMT-1",
                "GMT-2",
                "GMT-3",
                "GMT-4",
                "GMT-5",
                "GMT-6",
                "GMT-7",
                "GMT-8",
                "GMT-9",
                "GMT-10",
                "GMT-11"
        )) {
            put(timezone, new LinkedHashMap(){{
                put("name", timezone);
                put("timezone", TimeZone.getTimeZone(timezone));
            }});
        }
    }};

    public static final Map<String, Map<String, Object>> locales = new LinkedHashMap(){{
        put("en", new LinkedHashMap(){{
            put("name", "ENGLISH");
            put("language", "en");
            put("locale", Locale.ENGLISH);
        }});
        put("fr", new LinkedHashMap(){{
            put("name", "FRENCH");
            put("language", "fr");
            put("locale", Locale.FRENCH);
        }});
        put("de", new LinkedHashMap(){{
            put("name", "GERMAN");
            put("language", "de");
            put("locale", Locale.GERMAN);
        }});
        put("it", new LinkedHashMap(){{
            put("name", "ITALIAN");
            put("language", "it");
            put("locale", Locale.ITALIAN);
        }});
        put("ja", new LinkedHashMap(){{
            put("name", "JAPANESE");
            put("language", "ja");
            put("locale", Locale.JAPANESE);
        }});
        put("ko", new LinkedHashMap(){{
            put("name", "KOREAN");
            put("language", "ko");
            put("locale", Locale.KOREAN);
        }});
        put("zh", new LinkedHashMap(){{
            put("name", "CHINESE");
            put("language", "zh");
            put("locale", Locale.CHINESE);
        }});
        put("zh_CN", new LinkedHashMap(){{
            put("name", "SIMPLIFIED_CHINESE");
            put("language", "zh_CN");
            put("locale", Locale.SIMPLIFIED_CHINESE);
        }});
        put("zh_TW", new LinkedHashMap(){{
            put("name", "TRADITIONAL_CHINESE");
            put("language", "zh_TW");
            put("locale", Locale.TRADITIONAL_CHINESE);
        }});
        put("fr_FR", new LinkedHashMap(){{
            put("name", "FRANCE");
            put("language", "fr_FR");
            put("locale", Locale.FRANCE);
        }});
        put("de_DE", new LinkedHashMap(){{
            put("name", "GERMANY");
            put("language", "de_DE");
            put("locale", Locale.GERMANY);
        }});
        put("it_IT", new LinkedHashMap(){{
            put("name", "ITALY");
            put("language", "it_IT");
            put("locale", Locale.ITALY);
        }});
        put("ja_JP", new LinkedHashMap(){{
            put("name", "JAPAN");
            put("language", "ja_JP");
            put("locale", Locale.JAPAN);
        }});
        put("ko_KR", new LinkedHashMap(){{
            put("name", "KOREA");
            put("language", "ko_KR");
            put("locale", Locale.KOREA);
        }});
        put("en_GB", new LinkedHashMap(){{
            put("name", "UK");
            put("language", "en_GB");
            put("locale", Locale.UK);
        }});
        put("en_US", new LinkedHashMap(){{
            put("name", "US");
            put("language", "en_US");
            put("locale", Locale.US);
        }});
        put("en_CA", new LinkedHashMap(){{
            put("name", "CANADA");
            put("language", "en_CA");
            put("locale", Locale.CANADA);
        }});
        put("fr_CA", new LinkedHashMap(){{
            put("name", "CANADA_FRENCH");
            put("language", "fr_CA");
            put("locale", Locale.CANADA_FRENCH);
        }});
    }};

    public static List<Map<String, Object>> simple(Map<String, Map<String, Object>> map, String label) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<String, Map<String, Object>> entry : map.entrySet()) {
            list.add(new LinkedHashMap(){{
                put("value", entry.getKey());
                put("label", entry.getValue().get(label));
            }});
        }
        return list;
    }

    public static TimeZone timezone(String timezone) {
        return (TimeZone) timezones.get(timezone).get("timezone");
    }

    public static Locale locale(String locale) {
        return (Locale) locales.get(locale).get("locale");
    }

    public static Class type(String type) {
        if (!clsTypes.containsKey(type)) return null;
        return (Class) clsTypes.get(type).get(TYPE_FIELD);
    }

    public static <T> T convert(Class<T> classType, Object value) {
        if (null == value) return null;
        if (Integer.class.equals(classType)) {
            return (T) DPUtil.parseInt(value, null);
        }
        if (Double.class.equals(classType)) {
            return (T) DPUtil.parseDouble(value, null);
        }
        if (Boolean.class.equals(classType)) {
            return (T) Boolean.valueOf(DPUtil.parseBoolean(value));
        }
        if (Byte.class.equals(classType)) {
            return (T) Byte.valueOf(DPUtil.parseString(value));
        }
        if (Float.class.equals(classType)) {
            return (T) DPUtil.parseFloat(value, null);
        }
        if (Long.class.equals(classType)) {
            return (T) DPUtil.parseLong(value, null);
        }
        if (Short.class.equals(classType)) {
            return (T) Short.valueOf((short) DPUtil.parseLong(value));
        }
        if (String.class.equals(classType)) {
            return (T) DPUtil.parseString(value);
        }
        return (T) value;
    }

}
