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
import com.iisquare.fs.web.agent.core.AgenticNodeException;
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
 * 大语言模型（含工具调用循环）（节点类型：LLM）
 */
@Service
public class LlmNodeHandler implements AgenticNodeHandler {

    @Override
    public String type() {
        return "LLM";
    }

    @Override
    public ObjectNode execute(AgenticNodeContext ctx) throws Exception {
        ObjectNode data = ctx.data();
        return llm(ctx, data);
    }

    protected ObjectNode llm(AgenticNodeContext ctx, ObjectNode data) throws Exception {
        ctx.checkGateway();
        ObjectNode json = DPUtil.objectNode();
        json.put("model", data.at("/model").asText(""));
        json.put("stream", false);
        double temperature = data.at("/temperature").asDouble(0);
        if (data.at("/temperatureEnabled").asBoolean(false)) {
            json.put("temperature", temperature);
        }
        ctx.runtime().thinking(json, data);
        // 调度策略：无 = 不带工具的单次调用；FunctionCalling = 单轮工具调用（不再回填推理）；
        // ReAct = 循环迭代（工具结果回填上下文，直到模型给出非工具调用的回复）
        String strategy = data.at("/agentStrategy").asText("none");
        boolean react = "react".equalsIgnoreCase(strategy);
        boolean toolable = react || "functionCalling".equalsIgnoreCase(strategy);
        if (toolable) {
            ArrayNode tools = ctx.toolDefinitions(data);
            if (!tools.isEmpty()) json.set("tools", tools);
        }
        ArrayNode messages = json.putArray("messages");
        String system = ctx.text(data.at("/systemPrompt").asText(""));
        if (!DPUtil.empty(system)) messages.addObject().put("role", "system").put("content", system);
        // 多轮对话：历史消息插在系统提示词与本次输入之间
        for (JsonNode item : ctx.historyMessages(data)) messages.add(item);
        // 多模态输入：有图片/音频/视频/文件参数时，用户消息的 content 改成多段数组
        ctx.userMessage(messages, ctx.text(data.at("/prompt").asText("")), data);
        ArrayNode calls = DPUtil.arrayNode();
        // ReAct 调用链：逐轮记下模型输出（思考 + 内容）与该轮触发的工具调用及其返回结果
        ArrayNode rounds = DPUtil.arrayNode();
        JsonNode result = null;
        ctx.lastRequest(json.deepCopy());
        // ReAct 循环迭代：每轮返回工具调用就执行并把结果回填上下文继续推理；
        // FunctionCalling 只执行一轮，直接返回该轮（含工具调用）的输出
        int maxRounds = react ? Math.max(1, data.at("/maxIterations").asInt(5)) : 1;
        boolean limited = true;
        for (int round = 0; round < maxRounds; round++) {
            // 实时进度：本轮开始模型推理（ReAct 过程要实时返回给前端，不能等节点跑完）
            ctx.runtime().emitRound(roundEvent(round + 1, maxRounds, "running", null));
            result = ctx.completion(json);
            JsonNode message = result.at("/choices/0/message");
            ObjectNode roundNode = rounds.addObject();
            roundNode.put("round", round + 1);
            roundNode.put("content", message.at("/content").asText(""));
            roundNode.put("reasoning", message.at("/reasoning_content").asText(""));
            ArrayNode roundCalls = roundNode.putArray("calls");
            // 没有工具调用（或该字段缺失）时结束循环：at() 取不到返回 MissingNode，不能直接强转
            JsonNode toolCalls = message.at("/tool_calls");
            if (null == toolCalls || !toolCalls.isArray() || toolCalls.isEmpty()) {
                limited = false;
                break;
            }
            messages.add(message.deepCopy());
            for (JsonNode call : toolCalls) {
                ObjectNode record = DPUtil.objectNode();
                record.put("id", call.at("/id").asText(""));
                String name = call.at("/function/name").asText("");
                record.put("method", name);
                Map<String, Object> args = ctx.parseArguments(call.at("/function/arguments").asText("{}"));
                record.set("args", DPUtil.toJSON(args));
                long beginCall = System.currentTimeMillis();
                Object tool = null;
                // 实时进度：这次工具方法开始调用（前端先按 id 挂上「调用中」，长耗时也能看到）
                ObjectNode begin = DPUtil.objectNode();
                begin.put("id", record.at("/id").asText(""));
                begin.put("method", name);
                ctx.runtime().emitRound(roundEvent(round + 1, maxRounds, "calling", begin));
                try {
                    tool = ctx.invokeTool(data, name, args);
                    record.put("status", 1);
                    record.set("result", ctx.summary(DPUtil.toJSON(tool)));
                } catch (Exception e) {
                    record.put("status", 2);
                    record.put("error", ctx.message(e));
                }
                record.put("duration", System.currentTimeMillis() - beginCall);
                // 实时进度：这次工具方法已返回（成功/失败与耗时），前端按 id 更新同一条
                ctx.runtime().emitRound(roundEvent(round + 1, maxRounds, "calling", record));
                calls.add(record);
                roundCalls.add(record.deepCopy());
                // 工具结果回填上下文：失败时把失败原因一并交给模型（否则模型只看到 null，无法修正后重试）
                String content = 2 == record.at("/status").asInt(1)
                        ? "调用失败：" + record.at("/error").asText("")
                        : DPUtil.stringify(DPUtil.toJSON(tool));
                messages.addObject().put("role", "tool").put("tool_call_id", call.at("/id").asText(""))
                        .put("content", content);
            }
        }
        // 实时进度：本节点的 ReAct 轮次结束（收尾由节点级 step 事件负责）
        ctx.runtime().emitRound(roundEvent(rounds.size(), maxRounds, "completed", null));
        if (null == result) throw new IllegalStateException("模型未返回内容");
        JsonNode choice = result.at("/choices/0");
        ObjectNode value = DPUtil.objectNode();
        value.put("text", choice.at("/message/content").asText(""));
        value.put("reasoning", choice.at("/message/reasoning_content").asText(""));
        // 网关未返回 usage（或字段缺失）时补一个空对象，避免把 MissingNode 写进输出与步骤日志
        JsonNode usage = result.at("/usage");
        value.set("usage", null == usage || usage.isNull() || usage.isMissingNode() ? DPUtil.objectNode() : usage);
        value.set("calls", calls);
        value.set("rounds", rounds);
        // 便于核对多轮上下文：用过工具时把最后一轮请求的消息链（system/user/assistant/tool）一并记录
        if (!calls.isEmpty()) value.set("context", messages.deepCopy());
        // 只有 ReAct 循环才有「超限」概念：跑满上限仍在请求工具时按节点异常处理，
        // 异常里带上已产生的输出（calls / rounds），调试面板仍能展示完整的调用链
        if (react && limited) {
            throw new AgenticNodeException("工具调用已达最大迭代次数（" + maxRounds
                    + " 次），模型仍在请求工具：请调整提示词或提高「最大迭代次数」", value);
        }
        return value;
    }

    /**
     * ReAct 轮次事件：round 轮次、maxRounds 上限、state（running 模型推理 / calling 工具调用 /
     * completed 本轮结束），call 为这次工具方法的标识、状态与耗时（调用开始时只有 id 与 method）；
     * 节点标识由运行时补上，前端据此把执行过程实时画出来。
     */
    protected ObjectNode roundEvent(int round, int maxRounds, String state, ObjectNode call) {
        ObjectNode node = DPUtil.objectNode();
        node.put("round", round);
        node.put("maxRounds", maxRounds);
        node.put("state", DPUtil.parseString(state));
        if (null != call) node.set("call", call);
        return node;
    }

}
