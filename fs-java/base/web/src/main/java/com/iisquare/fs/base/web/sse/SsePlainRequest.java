package com.iisquare.fs.base.web.sse;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class SsePlainRequest implements Closeable {

    AtomicBoolean aborted = new AtomicBoolean(false);
    public Throwable failure = null; // 记录最终异常，一般为服务器端断开连接
    private HttpRequestBase request = null; // 请求单例
    private ObjectNode message = DPUtil.objectNode();

    /**
     * 中断处理请求
     */
    public void abort() {
        aborted.set(true);
        if (null != request) {
            // HttpClient连接池在关闭CloseableHttpResponse时并不会立即断开连接，会阻塞至响应输出完成
            // 对SSE代理服务而言，在客户端断开连接后，代理请求依然会持续执行至服务输出完成
            // 通过HttpRequestBase.abort()终止底层连接，立即关闭请求
            request.abort();
        }
    }

    public boolean isAborted() {
        return aborted.get();
    }

    public HttpRequestBase request(boolean single) throws Exception {
        if (single && null != request) {
            return request;
        }
        return this.request = request();
    }

    public abstract HttpRequestBase request() throws Exception;

    /**
     * 每个message内部由若干行组成，每一行格式为"[field]: value"，其中field可以取data、event、id、retry值。
     * 还可以有冒号开头的行，表示注释。通常，服务器每隔一段时间就会向浏览器发送一个注释，保持连接不中断，如":keep-alive"。
     * 如果数据很长，可以分成多行，最后一行用\n\n结尾，前面行都用\n结尾。
     * --
     * : this is a test stream
     *
     * data: some text
     *
     * data: another message
     * data: with two lines
     *
     * data: [DONE]
     * --
     * @see(https://www.ruanyifeng.com/blog/2017/05/server-sent_events.html)
     */
    public boolean onLine(CloseableHttpResponse response, String line, boolean isStream) {
        if ("".equals(line)) {
            boolean result = onMessage(message, true, isStream);
            message = DPUtil.objectNode();
            return result;
        }
        List<String> strings = parse(line);
        if (strings.isEmpty()) {
            message = (ObjectNode) DPUtil.parseJSON(line);
        } else {
            String key = strings.get(0);
            String value = strings.get(2);
            if (message.has(key)) {
                value = message.get(key).asText() + value;
            }
            message.put(key, value);
        }
        if (!isStream) {
            boolean result = onMessage(message, !strings.isEmpty(), false);
            message = DPUtil.objectNode();
            return result;
        }
        return true;
    }

    public static void main(String[] args) {
        ObjectNode json = DPUtil.objectNode();
        json.put("a", 123);
        System.out.println(parse("data:" + json)); // [data, data:, {"a":123}]
        System.out.println(parse("data: " + json)); // [data, data: , {"a":123}]
        System.out.println(parse(":keep-alive")); // [, :, keep-alive]
        System.out.println(parse(": keep-alive")); // [, : , keep-alive]
        System.out.println(parse(json.toString())); // []
        System.out.println(parse(json.toPrettyString())); // []
    }

    /**
     * @param message 若原始信息非JSON格式，可能会丢失为null
     * @param isEvent 是否为Event消息格式
     * @param isStream 是否为流式响应
     */
    public boolean onMessage(ObjectNode message, boolean isEvent, boolean isStream) {
        return true;
    }

    public void onError(CloseableHttpResponse response, Throwable throwable, boolean isStream) {}

    /**
     * 无论是否正常建立连接请求，需保障该方法始终触发
     */
    public void onClose() {}

    /**
     * 解析消息结构，若非标准结构，直接返回空数组
     * ["<前缀>", "<前缀>:<空格>", "消息内容"]
     */
    public static List<String> parse(String line) {
        List<String> result = new ArrayList<>();
        int length = line.length();
        int colonIndex = -1; // 冒号所在位置
        int dataIndex = -1; // 消息内容开始位置
        for (int i = 0; i < length; i++) {
            char c = line.charAt(i);
            if (-1 == colonIndex) { // 未找到冒号所在位置
                if (':' == c) { // 找到冒号所在位置
                    colonIndex = i;
                    dataIndex = i + 1;
                    continue;
                }
                if (c < 'a' || c > 'z') { // 前缀不合法
                    return result;
                }
            } else {
                if (' ' == c) { // 冒号之后的空格
                    dataIndex = i + 1;
                } else {
                    result.add(line.substring(0, colonIndex)); // 前缀
                    result.add(line.substring(0, dataIndex)); // 前缀+冒号+空格
                    result.add(line.substring(dataIndex)); // 消息内容
                    return result;
                }
            }
        }
        return result;
    }

    public ObjectNode data(ObjectNode message, boolean isEvent) {
        if (isEvent) {
            String data = message.at("/data").asText("");
            if (data.startsWith("{")) {
                message = (ObjectNode) DPUtil.parseJSON(data);
            } else {
                message = DPUtil.objectNode();
            }
        }
        return message;
    }

    public boolean isComment(ObjectNode message, boolean isEvent) {
        if (!isEvent) return false;
        if (message.has("")) return true;
        if (!message.has("data")) return false;
        String data = message.at("/data").asText("");
        return data.startsWith("[");
    }

    @Override
    public void close() throws IOException {
        this.onClose();
    }
}
