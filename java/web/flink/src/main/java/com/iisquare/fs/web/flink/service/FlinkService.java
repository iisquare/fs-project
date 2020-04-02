package com.iisquare.fs.web.flink.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.HttpUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.base.web.util.ServiceUtil;
import com.iisquare.fs.web.flink.dao.FlowDao;
import com.iisquare.fs.web.flink.dao.FlowPluginDao;
import com.iisquare.fs.web.flink.entity.Flow;
import com.iisquare.fs.web.flink.entity.FlowPlugin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.UnsupportedEncodingException;
import java.util.*;

@Service
public class FlinkService extends ServiceBase {

    @Value("${fs.flink.kvConsulUrl}")
    private String kvConsulUrl;
    @Autowired
    private FlowDao flowDao;
    @Autowired
    private FlowPluginDao flowPluginDao;

    public JsonNode kvConfig() {
        String content = HttpUtil.get(kvConsulUrl);
        if (null == content) return null;
        Yaml yaml = new Yaml();
        JsonNode kv = DPUtil.convertJSON(yaml.load(content));
        String kvContent = null;
        try {
            kvContent = new String(Base64.getDecoder().decode(kv.get(0).get("Value").asText()), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
        return DPUtil.convertJSON(yaml.load(kvContent));
    }

    public Flow flowInfo(Integer id) {
        if(null == id || id < 1) return null;
        Optional<Flow> info = flowDao.findById(id);
        return info.isPresent() ? info.get() : null;
    }

    public Map<String, FlowPlugin> pluginsInfoMap(Collection<String> names) {
        if(DPUtil.empty(names)) return new LinkedHashMap<>();
        List<FlowPlugin> list = flowPluginDao.findAllByStatusAndNameIn(1, names);
        return ServiceUtil.indexObjectList(list, String.class, FlowPlugin.class, "name");
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
                node.put(entry.getKey(), getSafeString(kvConfig, DPUtil.explode(kvConfigName, "\\.", " ", true)));
            }
        }
    }

    public ObjectNode parse(Flow info, String logId, Object ext) {
        if(null == info) return null;
        JsonNode flow = DPUtil.parseJSON(info.getContent());
        if(null == flow) return null;
        ObjectNode result = DPUtil.objectNode();
        result.put("id", info.getId());
        result.put("name", info.getName());
        result.put("type", info.getType());
        result.put("logId", logId);
        String appname = "flow-" + info.getName() + "-" + info.getId();
        if(!DPUtil.empty(logId)) appname += "-" + logId;
        result.put("appname", appname);
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
