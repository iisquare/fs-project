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
 * 问题分类器（节点类型：QuestionClassifier）
 */
@Service
public class QuestionClassifierNodeHandler implements AgenticNodeHandler {

    @Override
    public String type() {
        return "QuestionClassifier";
    }

    @Override
    public ObjectNode execute(AgenticNodeContext ctx) throws Exception {
        ObjectNode data = ctx.data();
        return classifier(ctx, data);
    }

    protected ObjectNode classifier(AgenticNodeContext ctx, ObjectNode data) throws Exception {
        ObjectNode json = ctx.chat(data);
        StringBuilder prompt = new StringBuilder("请判断用户问题属于以下哪个分类，只回答对应的分类标识：\n");
        for (JsonNode item : data.at("/classes")) {
            prompt.append("- ").append(item.at("/id").asText("")).append("：")
                    .append(ctx.text(item.at("/description").asText(""))).append("\n");
        }
        prompt.append("用户问题：").append(ctx.text(data.at("/query").asText("")));
        ((ArrayNode) json.at("/messages")).addObject().put("role", "user").put("content", prompt.toString());
        String answer = ctx.completion(json).at("/choices/0/message/content").asText("");
        ObjectNode result = DPUtil.objectNode();
        for (JsonNode item : data.at("/classes")) {
            String id = item.at("/id").asText("");
            if (!DPUtil.empty(id) && answer.contains(id)) {
                result.put("classId", id);
                result.put("className", item.at("/name").asText(""));
                break;
            }
        }
        // 分支标识与设计器锚点一致：class-{分类标识}
        String classId = result.at("/classId").asText("");
        result.put("branch", DPUtil.empty(classId) ? "" : "class-" + classId);
        result.put("text", answer);
        return result;
    }

}
