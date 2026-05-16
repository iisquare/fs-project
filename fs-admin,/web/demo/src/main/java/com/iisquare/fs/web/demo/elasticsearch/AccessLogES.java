package com.iisquare.fs.web.demo.elasticsearch;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.elasticsearch.mvc.ElasticsearchBase;
import org.springframework.stereotype.Component;

@Component
public class AccessLogES extends ElasticsearchBase {

    public AccessLogES() {
        this.collection = "fs_access_log";
    }

    @Override
    public String id(ObjectNode source) {
        return source.get("_id").asText();
    }

    @Override
    protected ObjectNode mapping() {
        ObjectNode source = DPUtil.objectNode();
        source.putObject("_source").put("enabled", true);
        ObjectNode properties = source.putObject("properties");
        properties.putObject("topic").put("type", "keyword") // Kafka主题
                .put("index", true).put("store", true);
        properties.putObject("partition").put("type", "integer") // Kafka分区
                .put("index", true).put("store", true);
        properties.putObject("offset").put("type", "long") // Kafka偏移
                .put("index", true).put("store", true);
        properties.putObject("message").put("type", "text") // 原始访问日志
                .put("index", true).put("store", true);

        properties.putObject("url").put("type", "keyword") // 页面访问地址
                .put("index", true).put("store", true)
                .put("ignore_above", 65535);
        properties.replace("url_info", urlNested()); // 解析页面地址
        properties.putObject("http_user_agent").put("type", "keyword") // 浏览器标识
                .put("index", true).put("store", true);
        properties.putObject("spider").put("type", "keyword") // 爬虫标识
                .put("index", true).put("store", true);
        properties.putObject("http_x_forwarded_for").put("type", "keyword") // 路由转发
                .put("index", true).put("store", true);
        properties.putObject("remote_addr").put("type", "keyword") // 客户端地址
                .put("index", true).put("store", true);
        properties.putObject("server_addr").put("type", "keyword") // 服务端地址
                .put("index", true).put("store", true);
        properties.putObject("time_local").put("type", "keyword") // 日志记录时间
                .put("index", true).put("store", true);
        properties.putObject("request_time").put("type", "float") // 请求响应时间
                .put("index", true).put("store", true);

        properties.putObject("site").put("type", "keyword") // 统计标识
                .put("index", true).put("store", true);
        properties.putObject("type").put("type", "keyword") // 事件类型
                .put("index", true).put("store", true);
        properties.putObject("action").put("type", "keyword") // 事件动作
                .put("index", true).put("store", true);
        properties.replace("param", kvNested()); // 事件参数
        properties.putObject("time").put("type", "long") // 请求开始时间
                .put("index", true).put("store", true);
        properties.putObject("uuid").put("type", "keyword") // 用户标识
                .put("index", true).put("store", true);
        properties.putObject("referrer").put("type", "keyword") // 访问来源
                .put("index", true).put("store", true);
        properties.replace("referrer_info", urlNested()); // 解析访问来源
        properties.replace("hash", kvNested()); // 请求Hash参数
        properties.putObject("width").put("type", "integer") // 屏幕尺寸宽度
                .put("index", true).put("store", true);
        properties.putObject("height").put("type", "integer") // 屏幕尺寸高度
                .put("index", true).put("store", true);
        properties.putObject("color").put("type", "integer") // 屏幕颜色
                .put("index", true).put("store", true);
        properties.putObject("cookie_enabled").put("type", "integer") // Cookie是否启用
                .put("index", true).put("store", true);
        properties.putObject("language").put("type", "keyword") // 客户端语言
                .put("index", true).put("store", true);
        properties.putObject("step").put("type", "integer") // 步长
                .put("index", true).put("store", true);
        properties.putObject("stay").put("type", "integer") // 停留时长，当前记录距统计开始时间的间隔
                .put("index", true).put("store", true);
        properties.putObject("navigation").put("type", "keyword") // 导航方式
                .put("index", true).put("store", true);

        properties.putObject("@timestamp").put("type", "date") // 日志时间戳
                .put("index", true).put("store", true);
        return source;
    }

    protected ObjectNode urlNested() {
        ObjectNode source = DPUtil.objectNode();
        ObjectNode properties = source.putObject("properties");
        properties.putObject("host").put("type", "keyword") // 域名
                .put("index", true).put("store", true);
        properties.replace("path", pathNested()); // 路径
        properties.replace("query", kvNested()); // 请求参数
        properties.replace("hash", kvNested()); // 请求Hash
        return source;
    }

    protected ObjectNode kvNested() {
        ObjectNode source = DPUtil.objectNode();
        ObjectNode properties = source.putObject("properties");
        properties.putObject("key").put("type", "keyword") // 键
                .put("index", true).put("store", true);
        properties.putObject("value").put("type", "keyword") // 值
                .put("index", true).put("store", true);
        return source;
    }

    protected ObjectNode pathNested() {
        ObjectNode source = DPUtil.objectNode();
        ObjectNode properties = source.putObject("properties");
        properties.putObject("key").put("type", "integer") // 索引
                .put("index", true).put("store", true);
        properties.putObject("value").put("type", "keyword") // 值
                .put("index", true).put("store", true);
        return source;
    }

}
