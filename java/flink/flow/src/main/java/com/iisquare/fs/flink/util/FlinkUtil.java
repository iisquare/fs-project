package com.iisquare.fs.flink.util;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.types.Row;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

public class FlinkUtil {

    public static final Map<String, Map<String, Object>> infoType;

    static {
        infoType = new LinkedHashMap<>();
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", Boolean.class.getSimpleName());
        item.put("classname", Boolean.class.getName());
        item.put("flink", Types.BOOLEAN);
        infoType.put(Boolean.class.getName(), item);
        item = new LinkedHashMap<>();
        item.put("name", Byte.class.getSimpleName());
        item.put("classname", Byte.class.getName());
        item.put("flink", Types.BYTE);
        infoType.put(Byte.class.getName(), item);
        item = new LinkedHashMap<>();
        item.put("name", Double.class.getSimpleName());
        item.put("classname", Double.class.getName());
        item.put("flink", Types.DOUBLE);
        infoType.put(Double.class.getName(), item);
        item = new LinkedHashMap<>();
        item.put("name", Float.class.getSimpleName());
        item.put("classname", Float.class.getName());
        item.put("flink", Types.FLOAT);
        infoType.put(Float.class.getName(), item);
        item = new LinkedHashMap<>();
        item.put("name", Integer.class.getSimpleName());
        item.put("classname", Integer.class.getName());
        item.put("flink", Types.INT);
        infoType.put(Integer.class.getName(), item);
        item = new LinkedHashMap<>();
        item.put("name", BigInteger.class.getSimpleName());
        item.put("classname", BigInteger.class.getName());
        item.put("flink", Types.BIG_INT); // TableException: Type is not supported: BigInteger
        infoType.put(BigInteger.class.getName(), item);
        item = new LinkedHashMap<>();
        item.put("name", BigDecimal.class.getSimpleName());
        item.put("classname", BigDecimal.class.getName());
        item.put("flink", Types.BIG_DEC);
        infoType.put(BigDecimal.class.getName(), item);
        item = new LinkedHashMap<>();
        item.put("name", Long.class.getSimpleName());
        item.put("classname", Long.class.getName());
        item.put("flink", Types.LONG);
        infoType.put(Long.class.getName(), item);
        item = new LinkedHashMap<>();
        item.put("name", Short.class.getSimpleName());
        item.put("classname", Short.class.getName());
        item.put("flink", Types.SHORT);
        infoType.put(Short.class.getName(), item);
        item = new LinkedHashMap<>();
        item.put("name", String.class.getSimpleName());
        item.put("classname", String.class.getName());
        item.put("flink", Types.STRING);
        infoType.put(String.class.getName(), item);
        item = new LinkedHashMap<>();
        item.put("name", Date.class.getSimpleName());
        item.put("classname", Date.class.getName());
        item.put("flink", Types.SQL_TIMESTAMP);
        infoType.put(Date.class.getName(), item);
        item = new LinkedHashMap<>();
        item.put("name", java.sql.Date.class.getSimpleName());
        item.put("classname", java.sql.Date.class.getName());
        item.put("flink", Types.SQL_DATE);
        infoType.put(java.sql.Date.class.getName(), item);
        item = new LinkedHashMap<>();
        item.put("name", java.sql.Time.class.getSimpleName());
        item.put("classname", java.sql.Time.class.getName());
        item.put("flink", Types.SQL_TIME);
        infoType.put(java.sql.Time.class.getName(), item);
        item = new LinkedHashMap<>();
        item.put("name", java.sql.Timestamp.class.getSimpleName());
        item.put("classname", java.sql.Timestamp.class.getName());
        item.put("flink", Types.SQL_TIMESTAMP);
        infoType.put(java.sql.Timestamp.class.getName(), item);
    }

    public static Map<String, Object> map(RowTypeInfo typeInfo, Row row) {
        String[] fields = typeInfo.getFieldNames();
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < fields.length; i++) {
            map.put(fields[i], row.getField(i));
        }
        return map;
    }

    public static Row row(Map<String, Object> map) {
        Row row = new Row(map.size());
        Iterator it = map.values().iterator();
        int i = 0;
        while (it.hasNext()) {
            row.setField(i++, it.next());
        }
        return row;
    }

    public static Map<String, Object> extract(Map<String, Object> map, ObjectNode content) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            if(!content.has(key)) continue;
            map.put(key, content.get(key));
        }
        return map;
    }

    public static Row row(Object...items) {
        Row row = new Row(items.length);
        for (int i = 0; i < items.length; i++) {
            row.setField(i, items[i]);
        }
        return row;
    }

    public static RowTypeInfo mergeTypeInfo(String mode, RowTypeInfo a, RowTypeInfo b) {
        Map<String, TypeInformation> map = new LinkedHashMap<>();
        switch (mode) {
            case "unshift":
                map.putAll(DPUtil.buildMap(b.getFieldNames(), b.getFieldTypes(), String.class, TypeInformation.class));
                map.putAll(DPUtil.buildMap(a.getFieldNames(), a.getFieldTypes(), String.class, TypeInformation.class));
                break;
            case "replace":
                return b;
            default:
                map.putAll(DPUtil.buildMap(a.getFieldNames(), a.getFieldTypes(), String.class, TypeInformation.class));
                map.putAll(DPUtil.buildMap(b.getFieldNames(), b.getFieldTypes(), String.class, TypeInformation.class));

        }
        return new RowTypeInfo(map.values().toArray(new TypeInformation[map.size()]), map.keySet().toArray(new String[map.size()]));
    }

    public static TypeInformation convertType(String classname) {
        Map<String, Object> item = infoType.get(classname);
        if(null == item) return Types.STRING;
        return (TypeInformation) item.get("flink");
    }

}
