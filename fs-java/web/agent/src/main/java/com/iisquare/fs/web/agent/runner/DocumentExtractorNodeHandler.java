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
import com.iisquare.fs.web.agent.tool.FileFetcher;
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
 * 文档内容提取（节点类型：DocumentExtractor）
 */
@Service
public class DocumentExtractorNodeHandler implements AgenticNodeHandler {

    @Autowired
    FileRpc fileRpc;

    @Override
    public String type() {
        return "DocumentExtractor";
    }

    @Override
    public ObjectNode execute(AgenticNodeContext ctx) throws Exception {
        ObjectNode data = ctx.data();
        return document(ctx, data);
    }

    protected ObjectNode document(AgenticNodeContext ctx, ObjectNode data) throws Exception {
        List<String> texts = new ArrayList<>();
        for (Object file : ctx.list(ctx.value(data.at("/input").asText("")))) {
            Map<?, ?> item = file instanceof Map ? (Map<?, ?>) file : DPUtil.buildMap("id", file);
            String id = DPUtil.parseString(item.get("id"));
            if (DPUtil.empty(id)) continue;
            String name = DPUtil.parseString(item.get("name"));
            String url = FileFetcher.url(fileRpc, id, name);
            if (DPUtil.empty(name)) name = FileFetcher.filename(url);
            byte[] bytes = FileFetcher.bytes(url, name);
            if (0 == bytes.length) throw new IllegalStateException("文件内容为空：" + name);
            try {
                texts.add(DocumentParser.parse(name, new ByteArrayInputStream(bytes)).getMarkdown());
            } catch (Exception e) {
                throw new IllegalStateException("解析文件失败：" + name + "：" + e.getMessage(), e);
            }
        }
        if (texts.isEmpty()) throw new IllegalStateException("文档变量为空，请选择用户上传的文件");
        ObjectNode result = DPUtil.objectNode();
        result.set(data.at("/outputName").asText("text"), DPUtil.toJSON(DPUtil.implode("\n\n", texts)));
        return result;
    }

}
