package com.iisquare.fs.app.crawler.schedule;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.*;

@Getter
@NoArgsConstructor
public class Schedule {

    private String id;
    private String name;
    private String group; // 所属分组
    private String description;
    private long priority; // Token优先级，停顿时无效
    private int maxThread; // 最大执行线程数(申请资源)
    private int maxPerNode; // 每个节点最大接收资源个数
    private long minHalt; // 最小停顿间隔
    private long maxHalt; // 最大停顿间隔
    private boolean dealRequestHeader; // 是否处理请求头
    private boolean dealResponseHeader; // 是否处理响应头
    private Map<String, Template> templates;
    private Map<Integer, List<Intercept>> intercepts;
    private String initTask;
    private List<Map<String, String>> initParams;

    public Schedule(JsonNode schedule) {
        this.id = schedule.at("/id").asText(UUID.randomUUID().toString()).replaceAll("\\s", "");
        this.name = schedule.at("/name").asText(this.id);
        this.group = schedule.at("/group").asText("");
        this.description = schedule.at("/description").asText("");
        this.priority = schedule.at("/priority").asLong(0);
        this.maxThread = schedule.at("/maxThread").asInt(1);
        this.maxPerNode = schedule.at("/maxPerNode").asInt(0);
        this.minHalt = schedule.at("/minHalt").asLong(0);
        this.maxHalt = schedule.at("/maxHalt").asLong(0);
        this.dealRequestHeader = schedule.at("/dealRequestHeader").asBoolean(false);
        this.dealResponseHeader = schedule.at("/dealResponseHeader").asBoolean(false);
        this.templates = templates(schedule.at("/templates"));
        this.intercepts = intercepts(schedule.at("/intercepts"));
        this.initTask = schedule.at("/initTask").asText();
        this.initParams = params(schedule.at("/initParams"));
    }

    public static String encode(Schedule schedule) {
        return DPUtil.stringify(DPUtil.toJSON(schedule));
    }

    public static Schedule decode(String schedule) {
        return DPUtil.toJSON(DPUtil.parseJSON(schedule), Schedule.class);
    }

    public long halt() {
        return (long) ((maxHalt - minHalt) * Math.random() + minHalt);
    }

    public Template template(String key) {
        return templates.get(key);
    }

    public Map<Integer, List<Intercept>> intercepts(JsonNode intercepts) {
        Map<Integer, List<Intercept>> map = new HashMap<>();
        Iterator<JsonNode> iterator = intercepts.elements();
        while (iterator.hasNext()) {
            JsonNode item = iterator.next();
            Intercept intercept = new Intercept(item);
            List<Intercept> list = map.get(intercept.getCode());
            if (null == list) {
                list = new ArrayList<>();
                map.put(intercept.getCode(), list);
            }
            list.add(intercept);
        }
        return map;
    }

    public List<Intercept> intercepts(int code) {
        if (null == intercepts) return new ArrayList<>();
        List<Intercept> list = this.intercepts.get(code);
        if (null == list) return new ArrayList<>();
        return list;
    }

    public Map<String, Template> templates(JsonNode templates) {
        Map<String, Template> map = new HashMap<>();
        Iterator<Map.Entry<String, JsonNode>> iterator = templates.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            map.put(entry.getKey(), new Template(entry.getValue()));
        }
        return map;
    }

    public List<Map<String, String>> params(JsonNode parameters) {
        if (null == parameters || parameters.isNull()) return null;
        if (!parameters.isArray()) parameters = DPUtil.arrayNode().add(parameters);
        List<Map<String, String>> list = new ArrayList<>();
        Iterator<JsonNode> elements = parameters.elements();
        while (elements.hasNext()) {
            Map<String, String> param = new HashMap<>();
            Iterator<Map.Entry<String, JsonNode>> iterator = elements.next().fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = iterator.next();
                param.put(entry.getKey(), entry.getValue().asText(""));
            }
            list.add(param);
        }
        return list;
    }

}
