package com.iisquare.fs.app.flink.script;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.app.flink.util.FlinkUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.core.DAGTransform;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.streaming.api.datastream.DataStream;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class FAnalyseScript extends DAGTransform implements MapFunction<JsonNode, JsonNode> {

    public static final String ANALYSE_PREFIX = "/analyse.gif?";

    public static Map<String, List<String>> spiders = new LinkedHashMap(){{
        put("baidu", Arrays.asList("baiduspider"));
        put("google", Arrays.asList("google"));
        put("360", Arrays.asList("360spider", "haosouspider"));
        put("sogou", Arrays.asList("soso", "sogou web spider"));
        put("yahoo", Arrays.asList("yahoo"));
        put("youdao", Arrays.asList("youdao", "yodao"));
        put("msn", Arrays.asList("msnbot"));
        put("bing", Arrays.asList("bingbot", "bingpreview"));
        put("yisou", Arrays.asList("yisouspider"));
        put("alexa", Arrays.asList("ia_archiver"));
        put("easou", Arrays.asList("easouspider"));
        put("jike", Arrays.asList("sikespider"));
        put("etao", Arrays.asList("etaospider"));
        put("weixin", Arrays.asList("mpcrawler"));
        put("python", Arrays.asList("python"));
        put("java", Arrays.asList("java"));
        put("spider", Arrays.asList("spider"));
        put("watering", Arrays.asList("watering"));
        put("robot", Arrays.asList("robot"));

    }};

    @Override
    public Object process() throws Exception {
        return FlinkUtil.union(DataStream.class, sources).map(this);
    }

    public Date date(String datetime) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MMM/yyyy:hh:mm:ss Z", Locale.ENGLISH);
        try {
            return dateFormat.parse(datetime);
        } catch (ParseException e) {
            return null;
        }
    }

    public ArrayNode cookie(String cookie) {
        ArrayNode cookies = DPUtil.arrayNode();
        if (DPUtil.empty(cookie) || cookie.equals("-")) return cookies;
        for (String pair : cookie.split("; ")) {
            cookies.add(parseKV(pair));
        }
        return cookies;
    }

    public static String spider(String ua) {
        ua = ua.toLowerCase();
        for (Map.Entry<String, List<String>> entry : spiders.entrySet()) {
            for (String ident : entry.getValue()) {
                if (!ua.contains(ident)) continue;
                return entry.getKey();
            }
        }
        return null;
    }

    public ObjectNode parseKV(String kv) {
        int idx = kv.indexOf("=");
        String key = -1 == idx ? kv : kv.substring(0, idx);
        String value = -1 == idx ? "" : kv.substring(idx + 1);
        try { key = URLDecoder.decode(key, "UTF-8"); } catch (Exception e) {}
        try { value = URLDecoder.decode(value, "UTF-8"); } catch (Exception e) {}
        return DPUtil.objectNode().put("key", key).put("value", value);
    }

    public ArrayNode parseQueryString(String uri) {
        ArrayNode result = DPUtil.arrayNode();
        if (DPUtil.empty(uri)) return result;
        for (String pair : uri.split("&")) {
            result.add(parseKV(pair));
        }
        return result;
    }

    public ArrayNode parseUri(String uri) {
        ArrayNode result = DPUtil.arrayNode();
        String[] paths = DPUtil.explode(uri, "/", "/", true);
        for (int index = 0; index < paths.length; index++) {
            result.addObject().put("key", index).put("value", paths[index]);
        }
        return result;
    }

    public ObjectNode parseUrl(String str) {
        ObjectNode result = DPUtil.objectNode();
        if (DPUtil.empty(str)) return result;
        URL url;
        try {
            url = new URL(str);
        } catch (MalformedURLException e) {
            return result;
        }
        result.put("host", url.getHost());
        result.replace("path", parseUri(url.getPath()));
        result.replace("query", parseQueryString(url.getQuery()));
        result.replace("hash", parseQueryString(url.getRef()));
        return result;
    }

    public ObjectNode analyse(String uri) {
        ObjectNode result = DPUtil.objectNode();
        if (!uri.startsWith(ANALYSE_PREFIX)) return result;
        uri = uri.substring(ANALYSE_PREFIX.length());
        Iterator<JsonNode> iterator = parseQueryString(uri).iterator();
        while (iterator.hasNext()) {
            JsonNode item = iterator.next();
            String key = item.get("key").asText();
            String value = item.get("value").asText();
            switch (key) {
                case "si": // 统计标识
                    result.put("site", value);
                    break;
                case "tp": // 事件类型
                    result.put("type", value);
                    break;
                case "ac": // 事件动作
                    result.put("action", value);
                    break;
                case "ps": // 事件参数
                    result.replace("param", parseQueryString(value));
                    break;
                case "rs": // 请求开始时间
                    result.put("time", DPUtil.parseLong(value));
                    break;
                case "ui": // 用户标识
                    result.put("uuid", value);
                    break;
                case "su": // 访问来源
                    result.put("referrer", value);
                    result.replace("referrer_info", parseUrl(value));
                    break;
                case "hs": // 请求Hash参数
                    result.replace("hash", parseQueryString(value));
                    break;
                case "ds": // 屏幕尺寸
                    String[] ds = value.split("x");
                    result.put("width", 2 == ds.length ? DPUtil.parseInt(ds[0]) : 0);
                    result.put("height", 2 == ds.length ? DPUtil.parseInt(ds[1]) : 0);
                    break;
                case "cl": // 屏幕颜色
                    result.put("color", DPUtil.parseInt(value));
                    break;
                case "ce": // Cookie是否启用
                    result.put("cookie_enabled", DPUtil.parseInt(value));
                    break;
                case "ln": // 客户端语言
                    result.put("language", value);
                    break;
                case "st": // 步长
                    result.put("step", DPUtil.parseInt(value));
                    break;
                case "rc": // 停留时长，当前记录距统计开始时间的间隔
                    result.put("stay", DPUtil.parseInt(value));
                    break;
                case "nv": // 导航方式
                    result.put("navigation", value);
                    break;
            }
        }
        return result;
    }

    public ObjectNode access(String message) {
        ObjectNode result = DPUtil.objectNode();
        JsonNode access = DPUtil.parseJSON(message);
        result.replace("cookie", cookie(access.at("/http_cookie").asText())); // Cookie记录
        /**
         *  Chrome85+将策略修改为strict-origin-when-cross-origin
         *  即如果请求地址与请求页面非同源，将只携带请求的域名，不会再带上来源页面地址的请求参数
         *  解决方案：在html里设置 <meta name="referrer" content="no-referrer-when-downgrade" />
         */
        result.put("url", access.at("/http_referer").asText()); // 页面访问地址
        result.replace("url_info", parseUrl(access.at("/http_referer").asText())); // 解析页面地址
        result.put("http_user_agent", access.at("/http_user_agent").asText()); // 浏览器标识
        result.put("spider", spider(access.at("/http_user_agent").asText())); // 爬虫标识
        result.put("http_x_forwarded_for", access.at("/http_x_forwarded_for").asText()); // 路由转发
        result.put("remote_addr", access.at("/remote_addr").asText()); // 客户端地址
        result.put("server_addr", access.at("/server_addr").asText()); // 服务端地址
        result.put("time_local", access.at("/time_local").asText()); // 日志记录时间
        result.put("request_time", DPUtil.parseFloat(access.at("/request_time").asText())); // 请求响应时间
        result.setAll(analyse(access.at("/request_uri").asText())); // 请求参数
        return result;
    }

    @Override
    public JsonNode map(JsonNode record) throws Exception {
        ObjectNode row = DPUtil.objectNode();
        ObjectNode access = access(record.at("/message").asText());
        Date date = date(access.at("/time_local").asText());
        row.put("_id", String.format("%s-%s-%d-%d",
                record.at("/topic").asText(),
                DPUtil.millis2dateTime(date.getTime(), "yyyyMMddHHmmss"),
                record.at("/partition").asInt(), record.at("/offset").asLong()));
        row.put("_table", String.format("{table}_%s", DPUtil.millis2dateTime(date.getTime(), "yyyyMM")));
        row.setAll((ObjectNode) record); // Kafka参数
        row.setAll(access); // 访问日志
        row.put("@timestamp", date.getTime()); // 日志时间戳
        return row;
    }
}
