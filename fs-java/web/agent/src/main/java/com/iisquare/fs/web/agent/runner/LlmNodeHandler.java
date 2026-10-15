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
 * 大语言模型（含工具调用循环）（节点类型：LLM）
 */
@Service
public class LlmNodeHandler implements AgenticNodeHandler {

    /**
     * 「接续次数」的缺省值与上限：跑满一批（节点配置的「迭代次数」）而模型仍在请求工具时自动接续的批次数，
     * 由节点配置 continueCount 决定（历史数据没有该字段时按缺省值处理），**不含首批**——
     * 配 0 表示跑满一批即收尾，配 2 表示总共跑 3 批；与客户端 MAX_ROUNDS_CONTINUE 同口径。
     * 接续用完后再额外做一轮不带工具的收尾调用（不占批次）。上限兜一道：节点数据可能被导入或直接改库，
     * 避免单个节点无限接续。
     */
    protected static final int DEFAULT_CONTINUE_COUNT = 2;
    protected static final int MAX_CONTINUE_COUNT = 5;

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
        // 跑满一批（配置的「迭代次数」）仍在请求工具时自动接续下一批，接续点补一条提醒；
        // 接续用完（共 1 + 接续次数 批）则禁用工具做一次收尾调用，不再按节点异常中断。
        // 收尾是额外加的第 maxRounds 轮、不占用批次数：每一批都是完整的 batchRounds 轮，
        // 否则「迭代次数配 1、接续次数配 0」这种极值会只剩一个收尾轮、一次工具都调不到。
        // FunctionCalling 只执行一轮，直接返回该轮（含工具调用）的输出
        int batchRounds = react ? Math.max(1, data.at("/maxIterations").asInt(5)) : 1;
        // 接续次数由节点配置：跑满一批仍在调工具就接续下一批，配 0 表示跑满一批即收尾
        int continueCount = react
                ? Math.max(0, Math.min(MAX_CONTINUE_COUNT, data.at("/continueCount").asInt(DEFAULT_CONTINUE_COUNT)))
                : 0;
        int maxRounds = react ? batchRounds * (1 + continueCount) + 1 : 1;
        // 接续提醒在消息链里的下标：只保留最新一条，-1 表示当前链上没有提醒
        int hintIndex = -1;
        // 收尾标记：达到总上限后工具被禁用、模型按已有信息作答，调用方可据此提示用户或提高上限
        boolean wrapped = false;
        for (int round = 0; round < maxRounds; round++) {
            boolean closing = react && round == maxRounds - 1;
            if (closing) {
                // 收尾轮：撤掉还在链上的接续提醒，换成收尾引导语（两条并存只会互相打架）
                if (hintIndex >= 0) {
                    messages.remove(hintIndex);
                    hintIndex = -1;
                }
                messages.addObject().put("role", "user").put("content", wrapupPrompt(maxRounds - 1));
            } else if (react && round > 0 && 0 == round % batchRounds) {
                // 接续点：跑满一批仍在调工具，只留最新一条提醒（换掉上一条），随下一批一起交给模型
                if (hintIndex >= 0) messages.remove(hintIndex);
                messages.addObject().put("role", "user")
                        .put("content", continuePrompt(round, maxRounds, batchRounds));
                hintIndex = messages.size() - 1;
            }
            // 实时进度：本轮开始模型推理（ReAct 过程要实时返回给前端，不能等节点跑完）
            ctx.runtime().emitRound(roundEvent(round + 1, maxRounds, "running", null));
            result = ctx.completion(closing ? withoutTools(json) : json);
            JsonNode message = result.at("/choices/0/message");
            ObjectNode roundNode = rounds.addObject();
            roundNode.put("round", round + 1);
            roundNode.put("content", message.at("/content").asText(""));
            roundNode.put("reasoning", message.at("/reasoning_content").asText(""));
            ArrayNode roundCalls = roundNode.putArray("calls");
            // 没有工具调用（或该字段缺失）时结束循环：at() 取不到返回 MissingNode，不能直接强转
            JsonNode toolCalls = message.at("/tool_calls");
            if (null == toolCalls || !toolCalls.isArray() || toolCalls.isEmpty()) {
                // 收尾轮给出正文即完成收尾；其余轮没有工具调用就是模型自然结束
                wrapped = closing;
                break;
            }
            if (closing) {
                // 收尾轮没有下发工具定义，模型正常不会再要工具：万一要了也不执行，按这一轮已有的内容收尾
                wrapped = true;
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
        // 收尾标记：只有 ReAct 循环才会置位——跑满总上限后工具被禁用，模型按已有信息作答，
        // 节点仍按成功返回（复杂任务不该因为跑满轮数而失败）；需要提示用户或重试时由流程按它分支
        value.put("wrapped", wrapped);
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

    /**
     * 接续提醒：跑满一批仍在请求工具时，随下一批的请求一起交给模型。
     * 讲清已跑多少轮、还剩多少轮，点名无效轮转的风险（反复调同一个工具、同样的失败反复出现、
     * 拿不到新信息），同时说明没有上述情况就照常推进——提醒是链尾的 user 消息，比用户输入更「新」，
     * 少了这一句，模型可能为了省轮数提前收尾。
     */
    protected String continuePrompt(int round, int maxRounds, int batchRounds) {
        // 剩余的工具往返轮数：扣掉最后那一轮收尾（它不执行工具）
        int left = maxRounds - round - 1;
        return "（系统提示：本节点已连续调用工具 " + round + " 轮仍在继续，工具往返上限是 " + (maxRounds - 1)
                + " 轮，剩余 " + left + " 轮，用完后还有一次不带工具的收尾。"
                + "请注意无效轮转的风险：如果发现自己在重复调用同一个工具、"
                + "用同样的参数反复失败，或已经拿不到新信息，请立即改变做法——换参数或换工具、"
                + "把多次调用合并成一次，或者基于已完成的工作直接给出结论并说明卡在哪里；"
                + "没有上述情况就照常推进原计划，不要为了省轮数提前收尾。已经成功过的调用不要重复执行。"
                + (left <= batchRounds ? "这是最后一次接续，用完即进入收尾（不再执行工具）。" : "") + "）";
    }

    /** 收尾引导语：工具已禁用，让模型按已完成的工作给出结论（而不是把节点判为失败） */
    protected String wrapupPrompt(int toolRounds) {
        return "（系统提示：本节点已连续调用工具 " + toolRounds + " 轮，达到上限，工具已禁用。"
                + "请基于以上已经完成的工作直接给出最终答复：说明已完成的部分、尚未完成的部分，以及下一步建议。）";
    }

    /** 收尾请求：不下发工具定义（模型只能给结论），其余参数与正式请求一致 */
    protected ObjectNode withoutTools(ObjectNode json) {
        ObjectNode copy = json.deepCopy();
        copy.remove("tools");
        return copy;
    }

}
