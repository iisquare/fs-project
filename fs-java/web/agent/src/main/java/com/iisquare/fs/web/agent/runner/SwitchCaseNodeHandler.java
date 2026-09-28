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
 * 条件分支（节点类型：SwitchCase）
 */
@Service
public class SwitchCaseNodeHandler implements AgenticNodeHandler {

    @Override
    public String type() {
        return "SwitchCase";
    }

    @Override
    public ObjectNode execute(AgenticNodeContext ctx) throws Exception {
        ObjectNode data = ctx.data();
        return switchCase(ctx, data);
    }

    protected ObjectNode switchCase(AgenticNodeContext ctx, ObjectNode data) {
        ObjectNode result = DPUtil.objectNode();
        String matched = null;
        for (JsonNode item : data.at("/cases")) {
            String logic = item.at("/logic").asText("and");
            // 任一（or）的初值必须是 false，否则第一次迭代就恒为真；全部（and）的初值为 true（0 条条件视为命中）
            boolean hit = !"or".equals(logic);
            for (JsonNode condition : item.at("/conditions")) {
                boolean current = match(ctx, condition);
                hit = "or".equals(logic) ? (hit || current) : (hit && current);
            }
            if (hit) {
                matched = item.at("/id").asText("");
                break;
            }
        }
        // 分支标识与设计器锚点一致：命中分支 case-{条件标识}，未命中走默认分支 default
        result.put("branch", null == matched ? "default" : "case-" + matched);
        return result;
    }

    protected boolean match(AgenticNodeContext ctx, JsonNode condition) {
        String operator = condition.at("/operator").asText("eq");
        Object value = ctx.value(condition.at("/variable").asText(""));
        // 比较规则与列表过滤、循环终止条件共用 compareWith；
        // 取值可能是节点的输出 JsonNode，文本节点用 String.valueOf 会带上一对引号，必须按标量文本比较
        return ctx.compareWith(ctx.runtime().scalar(value), operator, ctx.text(condition.at("/value").asText("")));
    }

}
