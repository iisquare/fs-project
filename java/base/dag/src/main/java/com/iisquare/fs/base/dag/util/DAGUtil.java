package com.iisquare.fs.base.dag.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.base.core.util.ReflectUtil;
import com.iisquare.fs.base.dag.core.DAGNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

public class DAGUtil {

    public static JsonNode withArg(JsonNode json, String arg) {
        if (DPUtil.empty(arg)) return json;
        ObjectNode result =  DPUtil.objectNode();
        result.replace(arg, json);
        return result;
    }

    public static ObjectNode mergeConfig(JsonNode... configs) {
        ObjectNode result = DPUtil.objectNode();
        for (JsonNode config : configs) {
            if (config.isObject()) {
                result.setAll((ObjectNode) config);
                continue;
            }
            int index = 0;
            Iterator<JsonNode> iterator = config.elements();
            while (iterator.hasNext()) {
                result.replace(String.valueOf(index++), iterator.next());
            }
        }
        return result;
    }

    public static JsonNode formatOptions(JsonNode options, JsonNode config) {
        if (options.isObject() || options.isArray()) {
            Iterator<Map.Entry<String, JsonNode>> iterator = options.fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = iterator.next();
                JsonNode result = formatOptions(entry.getValue(), config);
                if (options.isObject()) {
                    ((ObjectNode) options).replace(entry.getKey(), result);
                } else {
                    ((ArrayNode) options).set(Integer.valueOf(entry.getKey()), result);
                }
            }
            return options;
        }
        if (!options.isTextual()) return options;
        String text = options.asText();
        if (DPUtil.empty(text)) return options;
        List<String> list = DPUtil.matcher("\\{.*?\\}", text, false);
        for (String item : list) {
            JsonNode value = DPUtil.value(config, item.substring(1, item.length() - 1));
            text = text.replace(item, value.asText());
        }
        return DPUtil.toJSON(text);
    }

    public static Map<String, Class<DAGNode>> scanNodes(String... packages) throws ClassNotFoundException {
        Map<String, Class<DAGNode>> nodes = new LinkedHashMap<>();
        for (String pkg : packages) {
            List<String> list = ReflectUtil.getClassName(pkg);
            for (String item : list) {
                Class<?> cls = Class.forName(item);
                if (!DAGNode.class.isAssignableFrom(cls)) continue;
                String name = cls.getSimpleName();
                if (!name.endsWith("Node")) continue;
                nodes.put(name.substring(0, name.length() - 4), (Class<DAGNode>) cls);
            }
        }
        return nodes;
    }

    public static String loadFromUrl(String url) throws IOException {
        InputStream inputStream = null;
        InputStreamReader inputReader = null;
        BufferedReader bufferReader = null;
        String output = "";
        try {
            inputStream = new URL(url).openStream();
            inputReader = new InputStreamReader(inputStream);
            bufferReader = new BufferedReader(inputReader);
            StringBuilder sb = new StringBuilder();
            String text;
            while ((text = bufferReader.readLine()) != null) {
                sb.append(text);
            }
            output = sb.toString();
        } catch (IOException ioException) {
            throw ioException;
        } finally {
            FileUtil.close(bufferReader, inputReader, inputStream);
        }
        return output;
    }

    public static JsonNode loadDiagramFromUrl(String url) throws IOException {
        String result = loadFromUrl(url);
        JsonNode json = DPUtil.parseJSON(result);
        if (null == json) throw new RuntimeException("无法解析JSON配置：" + result);
        if (0 != json.at("/code").asInt(-1)) {
            throw new RuntimeException("配置状态异常：" + DPUtil.stringify(json));
        }
        return json.at("/data");
    }

    public static JsonNode loadDiagramFromFile(String url) throws IOException {
        url = url.replaceFirst("file://", "");
        JsonNode json = DPUtil.parseJSON(FileUtil.getContent(url));
        if (null == json) throw new RuntimeException("无法解析JSON配置：" + url);
        return json.at("/data");
    }

    public static JsonNode loadDiagram(String uri) throws IOException {
        if(uri.startsWith("http")) return loadDiagramFromUrl(uri);
        if(uri.startsWith("file")) return loadDiagramFromFile(uri);
        if (uri.matches("^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)$")) {
            uri = new String(Base64.getDecoder().decode(uri));
        }
        return DPUtil.parseJSON(uri);
    }

}
