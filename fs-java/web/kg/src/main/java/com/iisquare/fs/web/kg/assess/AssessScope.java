package com.iisquare.fs.web.kg.assess;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 评估范围结果：一个实体标签或一个关系类型的检查明细与得分
 */
public class AssessScope {

    public String label = "";
    public String name = "";
    public String primaryField = ""; // 实体主键字段，用于问题样例定位数据
    public long count;
    public long issueCount;
    public double score = -1D;
    public final List<Item> items = new ArrayList<>();

    public static class Item {
        public String rule = "";
        public String message = "";
        public long total;
        public long issues;
        public boolean sampled;
        public ArrayNode samples;
    }

    /**
     * 精确统计的检查项
     */
    public void add(String rule, String message, long total, long issues, ArrayNode samples) {
        add(false, rule, message, total, issues, samples);
    }

    public void add(boolean sampled, String rule, String message, long total, long issues, ArrayNode samples) {
        Item item = new Item();
        item.rule = rule;
        item.message = message;
        item.total = total;
        item.issues = issues;
        item.sampled = sampled;
        item.samples = samples;
        items.add(item);
        issueCount += issues;
    }

    public ObjectNode toJson(String kind) {
        ObjectNode node = DPUtil.objectNode();
        node.put("kind", kind);
        node.put("label", label);
        node.put("name", DPUtil.empty(name) ? label : name);
        node.put("primaryField", primaryField);
        node.put("count", count);
        node.put("issueCount", issueCount);
        double total = 0D;
        int size = 0;
        ArrayNode arrays = node.putArray("items");
        for (Item item : items) {
            ObjectNode json = DPUtil.objectNode();
            json.put("rule", item.rule);
            json.put("message", item.message);
            json.put("total", item.total);
            json.put("issues", item.issues);
            json.put("sampled", item.sampled);
            double rate = item.total <= 0 ? 1D : (double) (item.total - item.issues) / item.total;
            json.put("score", Math.round(rate * 10000D) / 100D);
            if (null != item.samples) json.set("samples", item.samples);
            arrays.add(json);
            if (item.total > 0) {
                total += rate;
                size++;
            }
        }
        score = size <= 0 ? -1D : Math.round((total / size) * 10000D) / 100D;
        node.put("score", score < 0 ? 100D : score);
        return node;
    }

}
