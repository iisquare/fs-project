package com.iisquare.fs.dag.core;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public class DagCore {

    public static Map<String, Map<String, String>> types = new LinkedHashMap(){{
        for (Class cls : Arrays.asList(
                Boolean.class,
                Byte.class,
                Double.class,
                Float.class,
                Integer.class,
                Long.class,
                Short.class,
                String.class,
                Date.class,
                java.sql.Date.class,
                java.sql.Time.class,
                java.sql.Timestamp.class
        )) {
            put(cls.getName(), new LinkedHashMap(){{
                put("name", cls.getSimpleName());
                put("classname", cls.getName());
            }});
        }
    }};

}
