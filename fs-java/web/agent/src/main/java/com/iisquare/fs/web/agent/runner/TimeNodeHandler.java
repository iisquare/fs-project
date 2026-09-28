package com.iisquare.fs.web.agent.runner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.HttpUtil;
import com.iisquare.fs.web.agent.entity.Tool;
import com.iisquare.fs.web.agent.entity.ToolMethod;
import com.iisquare.fs.web.agent.core.AgenticNodeContext;
import com.iisquare.fs.web.agent.core.AgenticNodeHandler;
import com.iisquare.fs.web.agent.core.AgenticRuntime;
import com.iisquare.fs.web.agent.tool.DocumentParser;
import com.hubspot.jinjava.Jinjava;
import com.iisquare.fs.web.core.rpc.FileRpc;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 时间处理（节点类型：Time）
 */
@Service
public class TimeNodeHandler implements AgenticNodeHandler {

    @Override
    public String type() {
        return "Time";
    }

    @Override
    public ObjectNode execute(AgenticNodeContext ctx) throws Exception {
        ObjectNode data = ctx.data();
        return time(ctx, data);
    }

    protected ObjectNode time(AgenticNodeContext ctx, ObjectNode data) {
        String operation = data.at("/operation").asText("current");
        ZoneId zone = ZoneId.of(DPUtil.empty(data.at("/timezone").asText("")) ? "Asia/Shanghai" : data.at("/timezone").asText(""));
        // 面板与文档统一用 java.time 标准写法（如 yyyy-MM-dd HH:mm:ss），这里兼容历史数据里的 Moment 写法
        String format = pattern(data.at("/format").asText(""));
        // 取值优先级：时间变量 → 固定时间值 → 当前时间（面板上固定值的说明就是「时间变量为空时使用」）
        Object input = ctx.value(data.at("/variable").asText(""));
        if (ctx.blank(input)) input = ctx.text(data.at("/datetime").asText(""));
        ZonedDateTime source = moment(input, zone, format);
        Object value;
        switch (operation) {
            case "now2timestamp":
                value = ZonedDateTime.now(zone).toInstant().toEpochMilli();
                break;
            case "time2timestamp":
                // 与其它操作一致：用统一解析出来的时间，不再只认固定时间值
                value = source.toInstant().toEpochMilli();
                break;
            case "timezone":
                value = source.withZoneSameInstant(ZoneId.of(data.at("/targetTimezone").asText("Asia/Shanghai")))
                        .format(DateTimeFormatter.ofPattern(format));
                break;
            case "add":
                value = source.plus(data.at("/amount").asLong(1), unit(data.at("/unit").asText("day")))
                        .format(DateTimeFormatter.ofPattern(format));
                break;
            case "diff":
                value = Math.abs(Duration.between(source, moment(ctx.value(data.at("/variable2").asText("")), zone, format)).toSeconds());
                break;
            case "weekday": {
                String[] weeks = {"星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"};
                value = weeks[source.getDayOfWeek().getValue() - 1];
                break;
            }
            case "timestamp2time":
            case "format":
                value = source.format(DateTimeFormatter.ofPattern(format));
                break;
            default:
                value = ZonedDateTime.now(zone).format(DateTimeFormatter.ofPattern(format));
                break;
        }
        ObjectNode result = DPUtil.objectNode();
        result.set(data.at("/outputName").asText("output"), DPUtil.toJSON(value));
        return result;
    }

    protected ChronoUnit unit(String name) {
        switch (name) {
            case "second": return ChronoUnit.SECONDS;
            case "minute": return ChronoUnit.MINUTES;
            case "hour": return ChronoUnit.HOURS;
            case "week": return ChronoUnit.WEEKS;
            case "month": return ChronoUnit.MONTHS;
            case "year": return ChronoUnit.YEARS;
            default: return ChronoUnit.DAYS;
        }
    }

    /**
     * 历史数据兼容：早期面板按 Moment 语法配置格式，而 DateTimeFormatter 里同名标记含义不同
     * （DD 是一年中的第几天、YYYY 是周历年份），会把 2026-09-28 渲染成 2026-09-271。
     * 标准写法是 java.time 语法，这里只把 Moment 与 Java 不同名的常用标记换掉；其余原样保留，
     * 所以标准的 yyyy-MM-dd HH:mm:ss 转换后不变。
     */
    protected String pattern(String format) {
        if (DPUtil.empty(format)) return "yyyy-MM-dd HH:mm:ss";
        StringBuilder result = new StringBuilder();
        Matcher matcher = Pattern.compile("([A-Za-z])\\1*").matcher(format);
        int index = 0;
        while (matcher.find()) {
            result.append(format, index, matcher.start());
            result.append(token(matcher.group()));
            index = matcher.end();
        }
        result.append(format.substring(index));
        return result.toString();
    }

    /**
     * Moment 与 Java 不同名的常用标记：年、日与星期。
     * Moment 的 dd 是星期缩写，但与 Java 的「日」同名且后者是常见写法，这里保留 Java 语义不动
     */
    protected String token(String token) {
        switch (token) {
            case "YYYY": return "yyyy";
            case "YY": return "yy";
            case "DD": return "dd";
            case "D": return "d";
            case "dddd": return "EEEE";
            case "ddd": return "EEE";
            case "A": return "a";
            default: return token;
        }
    }

    protected ZonedDateTime moment(Object value, ZoneId zone, String format) {
        String text = DPUtil.parseString(value);
        if (DPUtil.empty(text)) return ZonedDateTime.now(zone);
        if (text.matches("\\d{10,}")) {
            long number = DPUtil.parseLong(text);
            return Instant.ofEpochMilli(text.length() <= 10 ? number * 1000 : number).atZone(zone);
        }
        // 依次按节点配置的格式与默认格式解析，都失败时按当前时间兜底
        for (String pattern : new String[]{format, "yyyy-MM-dd HH:mm:ss"}) {
            try {
                return LocalDateTime.parse(text, DateTimeFormatter.ofPattern(pattern)).atZone(zone);
            } catch (Exception ignored) {
                // 换下一个格式
            }
        }
        return ZonedDateTime.now(zone);
    }

}
