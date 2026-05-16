package com.iisquare.fs.base.core.util;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

public class SQLUtil {

    public static String escape(String str) {
        if(null == str) return null;
        return str.replaceAll("'", "''");
    }

    public static String pkWhere(JsonNode node, String... pks) {
        if (null == node) return "1 = 1";
        List<String> list = new ArrayList<>();
        for (int i = 0; i < pks.length; i++) {
            List<String> items = new ArrayList<>();
            for (int j = 0; j <= i; j++) {
                String text = escape(node.at("/" + pks[j]).asText(""));
                items.add(String.format("%s %s '%s'", pks[j], j == i ? ">" : "=", text));
            }
            list.add(String.format("(%s)", DPUtil.implode(" AND ", items)));
        }
        return DPUtil.implode(" OR ", list);
    }

}
