package com.iisquare.fs.base.web.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;

import java.util.HashSet;
import java.util.Set;

/**
 * 通用业务辅助处理类
 *
 * @author Ouyang <iisquare@163.com>
 */
public class ServiceUtil {

    /**
     * 保留输出字段
     * @param rows 数据数组
     * @param columns 保留的字段
     */
    public static JsonNode retain(JsonNode rows, Object columns) {
        Set<String> names = new HashSet<>();
        for (String column : DPUtil.parseStringList(columns)) {
            String[] strings = DPUtil.explode("\\.", column);
            if (strings.length == 0) continue;
            names.add(strings[0]);
        }
        if (names.isEmpty()) return rows;
        for (JsonNode row : rows) {
            ObjectNode node = (ObjectNode) row;
            node.retain(names);
        }
        return rows;
    }

}
