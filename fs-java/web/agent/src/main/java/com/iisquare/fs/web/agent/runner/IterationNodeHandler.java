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
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.function.Consumer;

/**
 * 迭代（串行/并行）（节点类型：Iteration）
 */
@Service
public class IterationNodeHandler implements AgenticNodeHandler {

    @Autowired
    com.iisquare.fs.web.agent.react.AgenticInvokeTool agenticInvokeTool;

    @Override
    public String type() {
        return "Iteration";
    }

    @Override
    public ObjectNode execute(AgenticNodeContext ctx) throws Exception {
        ObjectNode data = ctx.data();
        return iteration(ctx, ctx.node(), data, ctx.outputs(), ctx.variables(), ctx.history());
    }

    protected ObjectNode iteration(AgenticNodeContext ctx, ObjectNode node, ObjectNode data, Map<String, ObjectNode> outputs,
                                   Map<String, Object> context, ArrayNode history) throws Exception {
        String id = node.at("/id").asText("");
        String itemName = data.at("/itemName").asText("item");
        String indexName = data.at("/indexName").asText("index");
        String outputVariable = data.at("/outputVariable").asText("");
        String outputName = data.at("/outputName").asText("output");
        String errorMode = data.at("/errorMode").asText("terminated");
        List<Object> items = ctx.list(ctx.value(data.at("/input").asText("")));
        ArrayNode results = DPUtil.arrayNode();
        boolean parallel = data.at("/parallel").asBoolean(false) && items.size() > 1;
        if (parallel) return iterationParallel(ctx, id, data, items, outputs, context, history, itemName, indexName, outputVariable, outputName);
        for (int index = 0; index < items.size(); index++) {
            ObjectNode scope = DPUtil.objectNode();
            scope.set(itemName, DPUtil.toJSON(items.get(index)));
            scope.put(indexName, index);
            ctx.scopes().put(id, scope);
            try {
                ctx.runChildren(id, index);
                Object value = ctx.value(outputVariable);
                if ("removed".equals(errorMode)) {
                    if (!ctx.blank(value)) results.add(DPUtil.toJSON(value));
                    continue;
                }
                results.add(DPUtil.toJSON(value));
            } catch (Exception e) {
                if ("terminated".equals(errorMode)) {
                    throw new IllegalStateException("迭代第 " + (index + 1) + " 项执行失败：" + ctx.message(e));
                }
            } finally {
                ctx.scopes().remove(id);
            }
        }
        ObjectNode result = DPUtil.objectNode();
        result.set(outputName, results);
        return result;
    }

    protected ObjectNode iterationParallel(AgenticNodeContext ctx, String containerId, ObjectNode data, List<Object> items,
                                           Map<String, ObjectNode> outputs, Map<String, Object> context,
                                           ArrayNode history, String itemName, String indexName,
                                           String outputVariable, String outputName) {
        String errorMode = data.at("/errorMode").asText("terminated");
        Map<String, ObjectNode> parentScopes = new LinkedHashMap<>(ctx.scopes());
        Map<String, Object> parentSystem = new LinkedHashMap<>(ctx.system());
        Map<String, ObjectNode> parentNodes = new LinkedHashMap<>(ctx.nodeMap());
        Map<String, List<Map<String, String>>> parentEdges = new LinkedHashMap<>(ctx.edgeMap());
        // 运行态同样不随线程传播：流式回调、最终回复来源节点、编排调用器都需要子线程继承
        Consumer<JsonNode> parentSink = ctx.runtime().streamSink();
        Consumer<JsonNode> parentStepSink = ctx.runtime().stepSink();
        Consumer<JsonNode> parentRoundSink = ctx.runtime().roundSink();
        // 步骤日志按线程保存：并行迭代的子线程要共用父线程这一份，否则容器内的步骤不会进运行日志
        ArrayNode parentSteps = ctx.runtime().steps();
        String parentAnswerSource = ctx.runtime().answerSource();
        java.util.function.BiFunction<Integer, String, ObjectNode> parentInvoker = agenticInvokeTool.invoker();
        int threads = Math.min(items.size(), Math.max(1, data.at("/parallelCount").asInt(1)));
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<Object>> futures = new ArrayList<>();
            for (int index = 0; index < items.size(); index++) {
                final int current = index;
                futures.add(pool.submit(() -> {
                    // 请求上下文不随线程自动继承：内部 RPC（数据主题、数据集查询等）要透传登录用户
                    RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
                    if (null != requestAttributes) RequestContextHolder.setRequestAttributes(requestAttributes);
                    // 继承外层容器作用域（支持嵌套容器），并隔离本次迭代的变量
                    ctx.scopes().clear();
                    ctx.scopes().putAll(parentScopes);
                    // 系统变量在子线程里同样可用（只读，运行期不修改）
                    ctx.system().clear();
                    ctx.system().putAll(parentSystem);
                    // 流式回调、最终回复来源、编排调用器：并行迭代内的节点同样可用
                    ctx.runtime().streamSink(parentSink);
                    ctx.runtime().stepSink(parentStepSink);
                    // ReAct 轮次进度与步骤日志同样继承：否则并行迭代里的工具调用过程与节点步骤都看不到
                    ctx.runtime().roundSink(parentRoundSink);
                    ctx.runtime().steps(parentSteps);
                    ctx.runtime().answerSource(parentAnswerSource);
                    agenticInvokeTool.invoker(parentInvoker);
                    // 子线程继承画布结构，容器内节点才能在并行迭代里继续执行
                    ctx.nodeMap().clear();
                    ctx.edgeMap().clear();
                    ctx.nodeMap().putAll(parentNodes);
                    ctx.edgeMap().putAll(parentEdges);
                    ObjectNode scope = DPUtil.objectNode();
                    scope.set(itemName, DPUtil.toJSON(items.get(current)));
                    scope.put(indexName, current);
                    ctx.scopes().put(containerId, scope);
                    Map<String, ObjectNode> scopedOutputs = new LinkedHashMap<>(outputs);
                    Map<String, Object> scopedContext = new LinkedHashMap<>(context);
                    try {
                        ctx.runChildren(containerId, scopedOutputs, scopedContext, history, current);
                        Object value = ctx.value(outputVariable, scopedOutputs, scopedContext);
                        synchronized (context) {
                            context.putAll(scopedContext);
                        }
                        return ctx.blank(value) && "removed".equals(errorMode) ? null : value;
                    } catch (Exception e) {
                        if ("terminated".equals(errorMode)) {
                            throw new IllegalStateException("迭代第 " + (current + 1) + " 项执行失败：" + ctx.message(e));
                        }
                        return null;
                    } finally {
                        ctx.scopes().remove(containerId);
                        ctx.runtime().streamSink(null);
                        ctx.runtime().stepSink(null);
                        ctx.runtime().roundSink(null);
                        agenticInvokeTool.invoker(null);
                        RequestContextHolder.resetRequestAttributes();
                    }
                }));
            }
            ArrayNode results = DPUtil.arrayNode();
            for (Future<Object> future : futures) {
                Object value = future.get();
                if (null == value) continue;
                results.add(DPUtil.toJSON(value));
            }
            ObjectNode result = DPUtil.objectNode();
            result.set(outputName, results);
            return result;
        } catch (ExecutionException e) {
            throw new IllegalStateException(ctx.message(null == e.getCause() ? e : e.getCause()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("迭代执行被中断");
        } finally {
            pool.shutdownNow();
        }
    }

}
