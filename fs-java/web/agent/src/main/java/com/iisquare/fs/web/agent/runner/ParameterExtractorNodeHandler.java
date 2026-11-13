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
 * 参数提取器（节点类型：ParameterExtractor）
 */
@Service
public class ParameterExtractorNodeHandler implements AgenticNodeHandler {

    @Override
    public String type() {
        return "ParameterExtractor";
    }

    @Override
    public ObjectNode execute(AgenticNodeContext ctx) throws Exception {
        ObjectNode data = ctx.data();
        return extractor(ctx, data);
    }

    protected ObjectNode extractor(AgenticNodeContext ctx, ObjectNode data) throws Exception {
        ObjectNode json = ctx.chat(data);
        ArrayNode messages = (ArrayNode) json.at("/messages");
        messages.addObject().put("role", "user").put("content", ctx.text(data.at("/query").asText("")));
        messages.addObject().put("role", "system").put("content", "请严格按以下参数返回 JSON 对象，不要输出多余内容："
                + DPUtil.stringify(data.at("/parameters")));
        String answer = ctx.completion(json).at("/choices/0/message/content").asText("");
        JsonNode parsed = DPUtil.parseJSON(answer.replaceAll("(?s)^```(json)?|```$", "").trim());
        boolean success = null != parsed && parsed.isObject();
        ObjectNode result = DPUtil.objectNode();
        result.put("__isSuccess", success);
        result.put("__reason", success ? "解析成功" : "模型返回内容无法解析为 JSON");
        for (JsonNode parameter : data.at("/parameters")) {
            String name = parameter.at("/name").asText("");
            if (DPUtil.empty(name)) continue;
            result.set(name, success ? parsed.at("/" + name) : DPUtil.toJSON(""));
        }
        result.put("text", answer);
        return result;
    }

}
