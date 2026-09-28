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
 * 循环（节点类型：Loop）
 */
@Service
public class LoopNodeHandler implements AgenticNodeHandler {

    @Override
    public String type() {
        return "Loop";
    }

    @Override
    public ObjectNode execute(AgenticNodeContext ctx) throws Exception {
        ObjectNode data = ctx.data();
        return loop(ctx, ctx.node(), data);
    }

    protected ObjectNode loop(AgenticNodeContext ctx, ObjectNode node, ObjectNode data) throws Exception {
        String id = node.at("/id").asText("");
        ObjectNode scope = DPUtil.objectNode();
        for (JsonNode item : data.at("/variables")) {
            String name = item.at("/name").asText("");
            if (DPUtil.empty(name)) continue;
            Object value = "variable".equals(item.at("/source").asText("constant"))
                    ? ctx.value(item.at("/variable").asText(""))
                    : DPUtil.toJSON(item.at("/value").asText(""));
            scope.set(name, DPUtil.toJSON(value));
        }
        ctx.scopes().put(id, scope);
        int max = Math.max(1, data.at("/maxIterations").asInt(100));
        try {
            for (int round = 0; round < max; round++) {
                if (!condition(ctx, ctx.object(data, "/condition"), id)) break;
                ctx.runChildren(id, round);
            }
        } finally {
            ctx.scopes().remove(id);
        }
        ObjectNode result = DPUtil.objectNode();
        result.set("variables", scope);
        return result;
    }

    protected boolean condition(AgenticNodeContext ctx, ObjectNode condition, String scopeId) {
        List<JsonNode> items = new ArrayList<>();
        condition.at("/conditions").forEach(items::add);
        if (items.isEmpty()) return true;
        boolean any = "or".equals(condition.at("/logic").asText("and"));
        for (JsonNode item : items) {
            String reference = item.at("/variable").asText("");
            Object value = ctx.scoped(scopeId, reference);
            if (null == value) value = ctx.value(reference);
            // 取值可能是输出 JsonNode，文本节点用 String.valueOf 会带上一对引号，必须按标量文本比较；
            // 比较值与条件分支同一口径：支持变量引用（整串是变量取该变量取值，否则按文本解析占位符）
            boolean current = ctx.compareWith(ctx.runtime().scalar(value),
                    item.at("/operator").asText("eq"), ctx.text(item.at("/value").asText("")));
            if (any && current) return true;
            if (!any && !current) return false;
        }
        return !any;
    }

}
