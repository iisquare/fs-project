package com.iisquare.fs.app.crawler.schedule;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.*;

@Getter
@NoArgsConstructor
public class Template {

    /**
     Timeout int // 整体请求超时时间（毫秒）
     ProxyInterval int // 代理IP更换频率（毫秒），小于1为不使用代理
     WithSession bool // 保持会话状态
     WithReferer bool // 携带请求来源
     FixedIp bool // 采用固定IP访问
     RetreatStrategy string // 退避策略[default-请求被限制时自动增加间隔时长]
     DispatchStrategy string // 调度策略[default-随机，directly-立即调用]

     Error map[string]*Rule // 请求异常规则
     Fail map[string]interface{} // 访问失败后，通过第三方协助判断是否继续执行
     Output *Output // 数据输出
     */

    private String id;
    private String name;
    private String description;
    private String url;
    private String requestType;
    private String charset;
    private long priority; // Task优先级，设置调度间隔后无效
    private Integer minInterval; // 最小访问间隔（毫秒）
    private Integer maxInterval; // 最大访问间隔（毫秒）
    private Integer maxRetry; // 单次访问最大失败重试次数
    private Integer maxIterate; // 最大翻页迭代次数
    private Map<String, String> headers;
    private String parser;
    private String mapper;
    private String output;

    public Template(JsonNode template) {
        this.id = UUID.randomUUID().toString();
        this.name = template.at("/name").asText(this.id);
        this.description = template.at("/description").asText("");
        this.url = template.at("/url").asText();
        this.requestType = template.at("/requestType").asText("GET");
        this.charset = template.at("/charset").asText("");
        this.priority = template.at("/priority").asLong(0);
        this.minInterval = template.at("/minInterval").asInt(0);
        this.maxInterval = template.at("/maxInterval").asInt(0);
        this.maxRetry = template.at("/maxRetry").asInt(0);
        this.maxIterate = template.at("/maxIterate").asInt(0);
        this.headers = defaultHeaders();
        Iterator<Map.Entry<String, JsonNode>> iterator = template.at("/headers").fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            this.headers.put(entry.getKey(), entry.getValue().asText(""));
        }
        this.parser = template.at("/parser").asText("");
        this.mapper = template.at("/mapper").asText("");
        this.output = template.at("/output").asText("{}");
    }

    public static Map<String, String> defaultHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        headers.put("Cache-Control", "no-cache");
        headers.put("Connection", "keep-alive");
        headers.put("Pragma", "no-cache");
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.80 Safari/537.36");
        return headers;
    }

}
