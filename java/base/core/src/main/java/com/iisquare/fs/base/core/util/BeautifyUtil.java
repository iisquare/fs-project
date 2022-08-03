package com.iisquare.fs.base.core.util;

import java.util.ArrayList;
import java.util.List;

public class BeautifyUtil {

    public static String duration(long timestamp) {
        List<String> result = new ArrayList<>();
        result.add(0, timestamp % 1000 + "ms");
        timestamp /= 1000;
        if (timestamp > 0) {
            result.add(0, timestamp % 60 + "s");
            timestamp /= 60;
        }
        if (timestamp > 0) {
            result.add(0, timestamp % 60 + "m");
            timestamp /= 60;
        }
        if (timestamp > 0) {
            result.add(0, timestamp % 24 + "h");
            timestamp /= 24;
        }
        if (timestamp > 0) {
            result.add(0, timestamp + "d");
        }
        return DPUtil.implode("", result);
    }

}
