package com.iisquare.fs.web.flink.service;

import com.ecwid.consul.v1.ConsulClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.base.web.util.ServiceUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.flink.dao.FlowDao;
import com.iisquare.fs.web.flink.dao.FlowPluginDao;
import com.iisquare.fs.web.flink.entity.Flow;
import com.iisquare.fs.web.flink.entity.FlowPlugin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.sql.*;
import java.util.*;

@Service
public class FlowService extends ServiceBase {

    @Autowired
    private ConsulClient client;
    @Value("${spring.profiles.active}")
    private String profilesActive;
    @Value("${spring.cloud.consul.config.data-key}")
    private String dataKey;
    @Autowired
    private FlowDao flowDao;
    @Autowired
    private FlowPluginDao flowPluginDao;

    public Map<String, Object> kvConfig() {
        StringBuffer keyPrefix = new StringBuffer("/config/application,").append(profilesActive).append("/").append(dataKey);
        String content = client.getKVValues(keyPrefix.toString()).getValue().get(0).getDecodedValue();
        return (new Yaml()).loadAs(content, Map.class);
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
        JsonNode kvConfig = DPUtil.parseJSON(DPUtil.stringify(kvConfig()));
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

    public Map<String, Object> jdbc(Map<?, ?> param, Map<?, ?> config) {
        ObjectNode node = DPUtil.objectNode();
        node.put("kvConfigPrefix", DPUtil.trim(DPUtil.parseString(param.get("prefix"))));
        node.put("driver", DPUtil.trim(DPUtil.parseString(param.get("driver"))));
        node.put("url", DPUtil.trim(DPUtil.parseString(param.get("url"))));
        node.put("username", DPUtil.trim(DPUtil.parseString(param.get("username"))));
        node.put("password", DPUtil.trim(DPUtil.parseString(param.get("password"))));
        node.put("sql", DPUtil.trim(DPUtil.parseString(param.get("sql"))));
        ObjectNode jdbc = node.deepCopy();
        JsonNode kvConfig = DPUtil.parseJSON(DPUtil.stringify(kvConfig()));
        kvReplace(kvConfig, jdbc);
        try {
            Class.forName(jdbc.get("driver").asText());
        } catch (ClassNotFoundException e) {
            return ApiUtil.result(1001, "驱动不存在", e.getMessage());
        }
        Connection connection;
        try {
            connection = DriverManager.getConnection(jdbc.get("url").asText(), jdbc.get("username").asText(), jdbc.get("password").asText());
        } catch (SQLException e) {
            return ApiUtil.result(1002, "获取连接失败", e.getMessage());
        }
        try {
            Statement statement = connection.createStatement();
            String sql = "select * from (" + jdbc.get("sql").asText() + ") t limit 1";
            ResultSet rs = statement.executeQuery(sql);
            ResultSetMetaData rsmd = rs.getMetaData();
            int count = rsmd.getColumnCount();
            ObjectNode result = DPUtil.objectNode();
            ArrayNode property = result.putArray("property");
            for (int i = 0; i < count; i++) {
                ObjectNode item = DPUtil.objectNode();
                item.put("field", rsmd.getColumnLabel(i + 1));
                item.put("classname", rsmd.getColumnClassName(i + 1));
                property.add(item);
            }
            String charset = DPUtil.trim(DPUtil.parseString(config.get("charset")));
            if (!DPUtil.empty(charset)) {
                ArrayNode content = result.putArray("content");
                if (rs.next()) {
                    for (int i = 0; i < rsmd.getColumnCount(); i++) {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("key", rsmd.getColumnName(i + 1));
                        Object value = rs.getObject(rsmd.getColumnName(i + 1));
                        item.put("value", value);
                        if (null != value && value instanceof String) {
                            String encoding = encoding(value.toString());
                            item.put("encoding", encoding);
                            if (!"".equals(encoding)) {
                                try {
                                    String target = new String(((String) value).getBytes(encoding), charset);
                                    item.put("target", target);
                                    item.put("charset", encoding(target));
                                } catch (UnsupportedEncodingException e) {
                                    item.put("error", e.getMessage());
                                }
                            }
                        }
                        content.add(DPUtil.convertJSON(item));
                    }
                }
                rs.close();
                return ApiUtil.result(0, null, result);
            }
            rs.close();
            URL url = ResourceUtils.getURL("classpath:analysis-anchor.json");
            ObjectNode content = (ObjectNode) DPUtil.parseJSON(FileUtil.getContent(url, false, "UTF-8"));
            Iterator<JsonNode> elements = content.get("nodes").get("flowChartItem1").get("property").elements();
            while (elements.hasNext()) {
                ObjectNode item = (ObjectNode) elements.next();
                switch (item.get("key").asText()) {
                    case "returns":
                        item.put("value", DPUtil.stringify(property));
                        continue;
                }
            }
            elements = content.get("nodes").get("flowChartItem2").get("property").elements();
            while (elements.hasNext()) {
                ObjectNode item = (ObjectNode) elements.next();
                switch (item.get("key").asText()) {
                    case "kvConfigPrefix":
                        item.put("value", node.get("kvConfigPrefix").asText());
                        continue;
                    case "driver":
                        item.put("value", node.get("driver").asText());
                        continue;
                    case "url":
                        item.put("value", node.get("url").asText());
                        continue;
                    case "username":
                        item.put("value", node.get("username").asText());
                        continue;
                    case "password":
                        item.put("value", node.get("password").asText());
                        continue;
                    case "sql":
                        item.put("value", node.get("sql").asText());
                        continue;
                }
            }
            result.replace("content", content);
            return ApiUtil.result(0, null, result);
        } catch (SQLException e) {
            return ApiUtil.result(1003, "执行SQL失败", e.getMessage());
        } catch (FileNotFoundException e) {
            return ApiUtil.result(1004, "读取配置模板失败", e.getMessage());
        } finally {
            try {connection.close();} catch (SQLException e) {}
        }
    }

}
