package com.iisquare.fs.dag.web.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.HttpUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.dag.web.entity.Flow;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.Map;

@Service
public class DagService extends ServiceBase {

    @Value("${fs.dag.k8sUrl}")
    private String k8sUrl;
    @Value("${fs.dag.kvConfigUrl}")
    private String kvConfigUrl;
    @Value("${fs.dag.kvConfigType:json}")
    private String kvConfigType;
    @Value("${fs.dag.appDir}")
    private String pluginsDir;
    @Value("${fs.dag.appUrl}")
    private String pluginsUrl;

    public String k8sUrl() {
        return this.k8sUrl;
    }

    public String pluginsDir() {
        return this.pluginsDir;
    }

    public String pluginsUrl() {
        return this.pluginsUrl;
    }

    public JsonNode kvConfig() {
        if (DPUtil.empty(kvConfigUrl)) return null;
        String content = HttpUtil.get(kvConfigUrl);
        if (null == content) return null;
        switch (kvConfigType) {
            case "json":
                return DPUtil.toJSON(content);
            default:
                return null;
        }
    }

    public ObjectNode extend(ObjectNode node, Object ext) {
        if(null == ext) return node;
        String json = DPUtil.stringify(ext);
        if(null == json) return node;
        JsonNode data = DPUtil.parseJSON(json);
        Iterator<String> iterator = data.fieldNames();
        while (iterator.hasNext()) {
            String field = iterator.next();
            node.replace(field, data.get(field));
        }
        return node;
    }

    protected String getSafeString(JsonNode node, String...keys) {
        if(null == node) return "";
        for (String key : keys) {
            if(!node.has(key)) return "";
            node = node.get(key);
            if(node.isNull()) return "";
        }
        return node.asText();
    }

    protected void kvReplace(JsonNode kvConfig, ObjectNode node) {
        if(node.has("kvConfigPrefix") && !DPUtil.empty(node.get("kvConfigPrefix"))) {
            String kvConfigPrefix = DPUtil.trim(node.get("kvConfigPrefix").asText());
            Iterator<Map.Entry<String, JsonNode>> entryIterator = node.fields();
            while (entryIterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = entryIterator.next();
                String kvConfigName = entry.getValue().asText();
                if(!kvConfigName.startsWith("kvConfig:")) continue;
                kvConfigName = kvConfigPrefix + "." + kvConfigName.replaceFirst("kvConfig:", "");
                node.put(entry.getKey(), getSafeString(kvConfig, DPUtil.explode(kvConfigName, "\\.")));
            }
        }
    }

    public ObjectNode flow(Flow info, String logId, Object ext) {
        if(null == info) return null;
        JsonNode flow = DPUtil.parseJSON(info.getContent());
        if(null == flow) return null;
        ObjectNode result = DPUtil.objectNode();
        result.put("id", info.getId());
        result.put("name", info.getName());
        result.put("type", info.getType());
        result.put("logId", logId); // 用于关联查询
        String appName = "flow-" + info.getName() + "-" + info.getId();
        if(!DPUtil.empty(logId)) appName += "-" + logId;
        result.put("appName", appName);
        result.put("appId", ""); // 由执行器生成
        // 解析节点
        Iterator<JsonNode> it = flow.get("nodes").elements();
        ArrayNode nodes = result.putArray("nodes");
        JsonNode kvConfig = kvConfig();
        ObjectNode plugins = DPUtil.objectNode();
        while (it.hasNext()) {
            JsonNode item = it.next();
            ObjectNode node = DPUtil.objectNode();
            // 配置项
            Iterator<JsonNode> iterator = item.get("property").elements();
            while (iterator.hasNext()) {
                JsonNode prop = iterator.next();
                node.put(prop.get("key").asText(), prop.get("value").asText());
            }
            // 采用配置中心参数替换配置项
            kvReplace(kvConfig, node);
            // 返回字段
            JsonNode returns = DPUtil.parseJSON(node.get("returns").asText());
            if(null == returns) returns = DPUtil.objectNode();
            node.replace("returns", returns);
            plugins.putArray(node.get("plugin").asText());
            nodes.add(node);
        }
        // 插件集合
        result.replace("plugins", plugins);
        // 节点连线
        result.replace("connections", flow.get("connections"));
        return extend(result, ext);
    }

    public static String encoding(String str) {
        String encode = "GB2312";
        try {
            if (str.equals(new String(str.getBytes(encode), encode))) {
                String s = encode;
                return s;
            }
        } catch (Exception exception) {
        }
        encode = "ISO-8859-1";
        try {
            if (str.equals(new String(str.getBytes(encode), encode))) {
                String s1 = encode;
                return s1;
            }
        } catch (Exception exception1) {
        }
        encode = "UTF-8";
        try {
            if (str.equals(new String(str.getBytes(encode), encode))) {
                String s2 = encode;
                return s2;
            }
        } catch (Exception exception2) {
        }
        encode = "GBK";
        try {
            if (str.equals(new String(str.getBytes(encode), encode))) {
                String s3 = encode;
                return s3;
            }
        } catch (Exception exception3) {
        }
        return "";
    }

}
