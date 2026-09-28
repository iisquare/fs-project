package com.iisquare.fs.web.agent.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.agent.core.AgenticRuntime;
import com.iisquare.fs.web.agent.tool.FileFetcher;
import com.iisquare.fs.web.core.rpc.FileRpc;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 编排运行时 - 各节点实现共用的能力集合：
 * 1. 线程局部状态：容器作用域、画布节点与连线、系统变量、当前节点解析后入参、步骤日志（并行迭代时子线程继承）；
 * 2. 变量解析：`{{#节点标识.变量名#}}` 的取值与文本替换（含系统变量、会话变量、容器作用域）；
 * 3. 模型调用：模型网关请求（/v1/chat/completions）、多模态消息构造、历史记忆窗口；
 * 4. 工具调用：工具方法定义（function calling schema）、参数绑定与执行。
 */
@Service
public class AgenticRuntime {

    /** 变量占位符：{{#节点标识.变量名#}} */
    public static final Pattern TOKEN = Pattern.compile("\\{\\{#([^#{}]+)#}}");

    @Value("${rpc.lm.rest:}")
    private String gatewayEndpoint;
    /** 模型网关认证密钥：配置文件 fs.agent.token，整个 agent 服务共用 */
    @Value("${fs.agent.token:}")
    private String gatewayToken;
    /**
     * 模型网关（会话补全）调用超时：长回复、长思考要留足时间，只兜住彻底卡死的连接；
     * 注意这是「相邻两次读取」的间隔上限，流式输出过程中不会因为整体耗时被掐断；单位毫秒
     */
    @Value("${fs.agent.gateway.chatConnectTimeout:5000}")
    private int chatConnectTimeout;
    @Value("${fs.agent.gateway.chatReadTimeout:600000}")
    private int chatReadTimeout;
    @Autowired
    FileRpc fileRpc;

    /**
     * 流式输出回调：由 /agentic/runStream 设置，模型节点把增量内容推给前端；
     * 为空时模型节点走非流式请求（普通 /run、/invoke 与工具调用场景）
     */
    private final ThreadLocal<Consumer<JsonNode>> streamThread = new ThreadLocal<>();
    /** 当前执行中的节点标识：流式增量按节点归属，前端可据此区分是哪个节点在输出 */
    private final ThreadLocal<String> nodeIdThread = new ThreadLocal<>();
    /**
     * 最终回复的来源节点：结束节点的回复内容整串引用某个节点输出时记录该节点，
     * 只有它的模型增量才推给前端（分类器、参数提取器、工具调用轮次等中间过程不进对话）
     */
    private final ThreadLocal<String> answerSourceThread = new ThreadLocal<>();

    public void streamSink(Consumer<JsonNode> sink) {
        streamThread.set(sink);
    }

    public Consumer<JsonNode> streamSink() {
        return streamThread.get();
    }

    /** 步骤回调（/runStream 设置）：节点开始与结束时实时推给前端，流程图上按状态着色 */
    private final ThreadLocal<Consumer<JsonNode>> stepThread = new ThreadLocal<>();

    public void stepSink(Consumer<JsonNode> sink) {
        stepThread.set(sink);
    }

    public Consumer<JsonNode> stepSink() {
        return stepThread.get();
    }

    /** 轮次回调（/runStream、/invokeStream 设置）：ReAct 每轮推理与每次工具调用的实时进度 */
    private final ThreadLocal<Consumer<JsonNode>> roundThread = new ThreadLocal<>();

    public void roundSink(Consumer<JsonNode> sink) {
        roundThread.set(sink);
    }

    public Consumer<JsonNode> roundSink() {
        return roundThread.get();
    }

    /**
     * 推送 ReAct 轮次进度：每轮模型推理开始、每次工具调用开始与返回各推一次，
     * 前端据此把执行过程里的工具轮次实时画出来，不必等节点跑完；没有回调时忽略。
     */
    public void emitRound(ObjectNode round) {
        Consumer<JsonNode> sink = roundThread.get();
        if (null == sink || null == round) return;
        ObjectNode data = round.deepCopy();
        data.put("nodeId", currentNode());
        sink.accept(data);
    }

    /**
     * 推送节点执行进度：state 为 running（开始执行）/ success / failed（执行结束），
     * 前端据此把节点与连线按「执行中蓝、成功绿、失败红」实时着色；没有回调时忽略。
     */
    public void emitStep(JsonNode step, String state) {
        Consumer<JsonNode> sink = stepThread.get();
        if (null == sink || null == step || !step.isObject()) return;
        ObjectNode data = ((ObjectNode) step).deepCopy();
        data.put("state", DPUtil.parseString(state));
        sink.accept(data);
    }

    /** 标记当前执行中的节点（每次执行节点前由节点上下文设置） */
    public void currentNode(String nodeId) {
        nodeIdThread.set(DPUtil.parseString(nodeId));
    }

    public String currentNode() {
        return DPUtil.parseString(nodeIdThread.get());
    }

    /** 设置最终回复的来源节点（运行前由调度器解析结束节点的回复内容得到） */
    public void answerSource(String nodeId) {
        answerSourceThread.set(DPUtil.parseString(nodeId));
    }

    public String answerSource() {
        return DPUtil.parseString(answerSourceThread.get());
    }

    /** 推送一段模型增量：内容与思考过程分别对应，无回调或内容为空时忽略 */
    public void emitStream(String content, String reasoning) {
        Consumer<JsonNode> sink = streamThread.get();
        if (null == sink) return;
        if (DPUtil.empty(content) && DPUtil.empty(reasoning)) return;
        // 只推送最终回复来源节点的输出，避免中间过程的模型输出混进对话
        String source = answerSource();
        if (DPUtil.empty(source) || !source.equals(currentNode())) return;
        ObjectNode chunk = DPUtil.objectNode();
        chunk.put("nodeId", currentNode());
        chunk.put("content", DPUtil.parseString(content));
        chunk.put("reasoning", DPUtil.parseString(reasoning));
        sink.accept(chunk);
    }

    /**
     * 通知前端重置思考内容：每轮模型调用开始时触发一次，
     * ReAct 中间轮次的推理只在执行过程中展示，不留在最终展示与入库的「思考过程」里
     */
    public void emitReasoningReset() {
        Consumer<JsonNode> sink = streamThread.get();
        if (null == sink) return;
        String source = answerSource();
        if (DPUtil.empty(source) || !source.equals(currentNode())) return;
        ObjectNode chunk = DPUtil.objectNode();
        chunk.put("nodeId", currentNode());
        chunk.put("reasoningReset", true);
        sink.accept(chunk);
    }

    /**
     * 最终回复：整段下发（replace=true），前端据此把对话内容替换为最终结果。
     * 模板拼装、知识库回填等非模型输出同样以流式事件下发，保证最终返回结果走同一条通道。
     */
    public void emitAnswer(String text) {
        Consumer<JsonNode> sink = streamThread.get();
        if (null == sink) return;
        ObjectNode chunk = DPUtil.objectNode();
        chunk.put("nodeId", "");
        chunk.put("content", DPUtil.parseString(text));
        chunk.put("reasoning", "");
        chunk.put("replace", true);
        sink.accept(chunk);
    }

    /** 模型网关地址未配置时直接报错（模型类节点入口先做一次校验） */
    public void checkGateway() {
        if (DPUtil.empty(gatewayEndpoint)) throw new IllegalStateException("未配置模型网关地址（rpc.lm.rest）");
    }

    /**
     * 思考模式与思考强度：按模型网关的约定写入请求体
     * - 思考模式：`thinking: { type: enabled | disabled }`（选「自动」时不显式声明，交给模型/网关决定）
     * - 思考强度：`reasoning_effort: low | medium | high | max`，仅在思考未显式关闭时下发
     */
    public void thinking(ObjectNode json, ObjectNode data) {
        boolean modeEnabled = data.at("/thinkModeEnabled").asBoolean(false);
        String mode = data.at("/thinkMode").asText("auto");
        if (modeEnabled) {
            if ("on".equals(mode) || "enabled".equals(mode)) {
                json.putObject("thinking").put("type", "enabled");
            } else if ("off".equals(mode) || "disabled".equals(mode)) {
                json.putObject("thinking").put("type", "disabled");
            }
        }
        if (!data.at("/thinkEffortEnabled").asBoolean(false)) return;
        // 思考显式关闭时不再下发强度（与设计器里的可编辑判断保持一致）
        if (modeEnabled && ("off".equals(mode) || "disabled".equals(mode))) return;
        String effort = data.at("/thinkEffort").asText("");
        if (DPUtil.empty(effort)) return;
        json.put("reasoning_effort", effort);
    }

    /**
     * 容器作用域：容器标识 → 当前迭代/循环的内置变量（元素、索引、循环变量），取值优先于节点输出。
     * 用线程局部变量保存，并行迭代时每个迭代互不干扰，也避免嵌套容器的外层作用域被覆盖
     */
    private final ThreadLocal<Map<String, ObjectNode>> scopesThread = ThreadLocal.withInitial(LinkedHashMap::new);

    /**
     * 本次运行的系统变量（应用、会话、调用人、当前时间等）：
     * 节点里 `{{#sys.xxx#}}` 的取值来源，并行迭代时子线程继承同一份只读数据
     */
    private final ThreadLocal<Map<String, Object>> systemThread = ThreadLocal.withInitial(LinkedHashMap::new);

    /** 画布节点与连线：容器内的子图执行时复用（每次 execute 时重建） */
    private final ThreadLocal<Map<String, ObjectNode>> nodeThread = ThreadLocal.withInitial(LinkedHashMap::new);
    private final ThreadLocal<Map<String, List<Map<String, String>>>> edgeThread = ThreadLocal.withInitial(LinkedHashMap::new);
    /** 当前节点解析后的实际入参：写在步骤日志上，便于排查 */
    private final ThreadLocal<ObjectNode> requestThread = new ThreadLocal<>();
    /** 本次运行的步骤日志：容器内节点的步骤同样汇总在这里，并行迭代时按任务收集后合并 */
    private final ThreadLocal<ArrayNode> stepsThread = ThreadLocal.withInitial(DPUtil::arrayNode);

    public ObjectNode object(JsonNode parent, String path) {
        JsonNode value = null == parent ? null : parent.at(path);
        return null != value && value.isObject() ? (ObjectNode) value : DPUtil.objectNode();
    }

    public ArrayNode array(JsonNode parent, String path) {
        JsonNode value = null == parent ? null : parent.at(path);
        return null != value && value.isArray() ? (ArrayNode) value : DPUtil.arrayNode();
    }

    public Map<String, ObjectNode> scopes() {
        return scopesThread.get();
    }

    public Map<String, Object> system() {
        return systemThread.get();
    }

    public Map<String, ObjectNode> nodeMap() {
        return nodeThread.get();
    }

    public Map<String, List<Map<String, String>>> edgeMap() {
        return edgeThread.get();
    }

    public ObjectNode lastRequest() {
        return requestThread.get();
    }

    public ArrayNode steps() {
        return stepsThread.get();
    }

    /**
     * 替换当前线程的步骤日志：
     * 嵌套运行（编排工具）要挂起外层的步骤、并让并行迭代的子线程共用同一份日志
     */
    public void steps(ArrayNode steps) {
        stepsThread.set(null == steps ? DPUtil.arrayNode() : steps);
    }

    /** 记录当前节点解析后的实际入参（写步骤日志用） */
    public void lastRequest(ObjectNode value) {
        requestThread.set(value);
    }

    public String post(String url, String json) throws Exception {
        HttpPost http = new HttpPost(url);
        // 认证密钥：服务级配置 fs.agent.token，整个 agent 服务共用
        if (!DPUtil.empty(gatewayToken)) http.addHeader("Authorization", "Bearer " + gatewayToken);
        http.addHeader("Content-Type", "application/json;charset=" + StandardCharsets.UTF_8.name());
        http.setEntity(new StringEntity(json, StandardCharsets.UTF_8));
        try (CloseableHttpClient client = gatewayClient();
             CloseableHttpResponse response = client.execute(http)) {
            return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        }
    }

    /** 模型网关 HTTP 客户端：显式超时，避免网关无响应时整轮对话无限等待 */
    protected CloseableHttpClient gatewayClient() {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(chatConnectTimeout)
                .setConnectionRequestTimeout(chatConnectTimeout)
                .setSocketTimeout(chatReadTimeout)
                .build();
        return HttpClients.custom().setDefaultRequestConfig(config).build();
    }

    public JsonNode completion(ObjectNode json) throws Exception {
        // 只有「最终回复来源节点」的模型调用走流式：分类器、参数提取器等中间节点仍用一次性请求
        if (null != streamThread.get() && !DPUtil.empty(answerSource()) && answerSource().equals(currentNode())) {
            return completionStream(json);
        }
        String response = post(gatewayEndpoint + "/v1/chat/completions", DPUtil.stringify(json));
        JsonNode result = DPUtil.parseJSON(response);
        if (null == result) throw new IllegalStateException("模型返回内容无法解析：" + DPUtil.parseString(response));
        if (result.has("error")) throw new IllegalStateException(result.at("/error/message").asText("模型调用失败"));
        // 网关按平台统一格式返回（code/message）时同样按失败处理，避免鉴权失败被静默当成空回复
        if (result.hasNonNull("code") && 0 != result.at("/code").asInt(0)) {
            String message = result.at("/message").asText("");
            throw new IllegalStateException(DPUtil.empty(message) ? "模型调用失败" : message);
        }
        return result;
    }

    /**
     * 流式调用模型网关：逐行读取 SSE，把内容/思考增量实时推送出去，
     * 同时把完整消息（含工具调用）聚合出来，返回结构与一次性返回保持一致。
     */
    protected JsonNode completionStream(ObjectNode json) throws Exception {
        // 新一轮模型调用：先通知前端重置思考内容（ReAct 中间轮次的推理不保留在最终「思考过程」里）
        emitReasoningReset();
        ObjectNode body = json.deepCopy();
        body.put("stream", true);
        if (!body.has("stream_options")) body.putObject("stream_options").put("include_usage", true);
        HttpPost request = new HttpPost(gatewayEndpoint + "/v1/chat/completions");
        if (!DPUtil.empty(gatewayToken)) request.addHeader("Authorization", "Bearer " + gatewayToken);
        request.addHeader("Content-Type", "application/json;charset=" + StandardCharsets.UTF_8.name());
        request.setEntity(new StringEntity(DPUtil.stringify(body), StandardCharsets.UTF_8));
        StringBuilder content = new StringBuilder();
        StringBuilder reasoning = new StringBuilder();
        // 工具调用增量按 index 聚合：id/name/arguments 都是分片下发的
        Map<Integer, ObjectNode> calls = new TreeMap<>();
        JsonNode usage = null;
        try (CloseableHttpClient client = gatewayClient();
             CloseableHttpResponse response = client.execute(request)) {
            int status = response.getStatusLine().getStatusCode();
            if (200 != status) {
                String text = null == response.getEntity() ? "" : EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                throw new IllegalStateException(DPUtil.empty(text) ? "模型网关响应异常：" + status : text);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    response.getEntity().getContent(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data:")) continue;
                    String text = line.substring("data:".length()).trim();
                    if (DPUtil.empty(text) || "[DONE]".equals(text)) continue;
                    JsonNode chunk = DPUtil.parseJSON(text);
                    if (null == chunk) continue;
                    if (chunk.has("error")) throw new IllegalStateException(chunk.at("/error/message").asText("模型调用失败"));
                    if (chunk.has("usage")) usage = chunk.at("/usage");
                    JsonNode delta = chunk.at("/choices/0/delta");
                    String piece = delta.at("/content").asText("");
                    String think = delta.at("/reasoning_content").asText("");
                    if (!DPUtil.empty(piece) || !DPUtil.empty(think)) {
                        content.append(piece);
                        reasoning.append(think);
                        // 正文与思考都实时推送：思考在下一轮调用前会被重置，
                        // 因此中间轮次的推理只是过程中的展示，最终只保留最后一轮的思考
                        emitStream(piece, think);
                    }
                    for (JsonNode call : delta.at("/tool_calls")) {
                        int index = call.at("/index").asInt(0);
                        ObjectNode exist = calls.get(index);
                        if (null == exist) {
                            exist = DPUtil.objectNode();
                            exist.put("id", "");
                            // 与 OpenAI 规范一致：上下文里的工具调用必须带 type
                            exist.put("type", "function");
                            ObjectNode function = exist.putObject("function");
                            function.put("name", "");
                            function.put("arguments", "");
                            calls.put(index, exist);
                        }
                        String id = call.at("/id").asText("");
                        if (!DPUtil.empty(id)) exist.put("id", id);
                        ObjectNode function = (ObjectNode) exist.at("/function");
                        String name = call.at("/function/name").asText("");
                        if (!DPUtil.empty(name)) function.put("name", function.at("/name").asText("") + name);
                        String args = call.at("/function/arguments").asText("");
                        if (!DPUtil.empty(args)) function.put("arguments", function.at("/arguments").asText("") + args);
                    }
                }
            }
        }
        ObjectNode result = DPUtil.objectNode();
        ObjectNode message = result.putArray("choices").addObject().putObject("message");
        // 角色必填：聚合出的消息会回填到多轮上下文，缺少 role 会被模型侧判为非法消息
        message.put("role", "assistant");
        message.put("content", content.toString());
        message.put("reasoning_content", reasoning.toString());
        if (!calls.isEmpty()) {
            ArrayNode toolCalls = message.putArray("tool_calls");
            calls.values().forEach(toolCalls::add);
        }
        result.set("usage", null == usage ? DPUtil.objectNode() : usage);
        return result;
    }

    public Object value(String text, Map<String, ObjectNode> outputs, Map<String, Object> context) {
        if (DPUtil.empty(text)) return "";
        Matcher matcher = TOKEN.matcher(text);
        if (matcher.matches()) return reference(matcher.group(1), outputs, context);
        return this.text(text, outputs, context);
    }

    /**
     * 变量引用的字面量 - 整串是占位符（`{{#节点标识.变量名#}}`、`{{#sys.变量名#}}`、`{{#conversation.变量名#}}`）
     * 时取其中的引用，否则按原文返回（兼容历史数据里直接存裸取值的字段，如赋值节点的目标变量）。
     * 用于「引用本身就是要写入的取值」的字段：这类字段不能用 value() 解析取值，只需要统一规范的书写形式
     */
    public static String referenceLiteral(String text) {
        String value = DPUtil.parseString(text).trim();
        Matcher matcher = TOKEN.matcher(value);
        return matcher.matches() ? matcher.group(1).trim() : value;
    }

    public String text(String text, Map<String, ObjectNode> outputs, Map<String, Object> context) {
        if (DPUtil.empty(text)) return "";
        Matcher matcher = TOKEN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            Object value = reference(matcher.group(1), outputs, context);
            String replacement = scalar(value);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    /**
     * 解析节点配置里的变量引用：字符串逐个按文本替换，对象与数组递归处理。
     * 用于把「解析后的实际入参」写进运行日志的步骤里——节点自己没有拼装请求（如模板、代码、聚合）时，
     * 解析后的配置就是它这一轮真实的输入。
     */
    public JsonNode resolve(JsonNode value, Map<String, ObjectNode> outputs, Map<String, Object> context) {
        if (null == value) return DPUtil.objectNode();
        if (value.isTextual()) return DPUtil.toJSON(text(value.asText(""), outputs, context));
        if (value.isObject()) {
            ObjectNode result = DPUtil.objectNode();
            value.properties().forEach(entry -> result.set(entry.getKey(), resolve(entry.getValue(), outputs, context)));
            return result;
        }
        if (value.isArray()) {
            ArrayNode result = DPUtil.arrayNode();
            value.forEach(item -> result.add(resolve(item, outputs, context)));
            return result;
        }
        return value.deepCopy();
    }

    /**
     * 标量取值：文本/数值/布尔直接取文本值，对象与数组按 JSON 文本。
     * 注意不能用 DPUtil.stringify / parseString 处理文本节点：它们会带上一对引号，
     * 表现为回复内容、提示词、模板里出现 "xxx" 这种情况。
     */
    public String scalar(Object value) {
        if (null == value) return "";
        if (value instanceof JsonNode) {
            JsonNode node = (JsonNode) value;
            if (node.isNull() || node.isMissingNode()) return "";
            return node.isValueNode() ? node.asText("") : DPUtil.stringify(node);
        }
        return DPUtil.parseString(value);
    }

    public Object reference(String reference, Map<String, ObjectNode> outputs, Map<String, Object> context) {
        String text = DPUtil.parseString(reference);
        int dot = text.lastIndexOf('.');
        if (dot > 0) {
            // 容器内变量优先：容器自身与其内部的节点都可引用
            Object scoped = scoped(text.substring(0, dot), text);
            if (null != scoped) return scoped;
        } else {
            for (ObjectNode scope : scopes().values()) {
                if (scope.has(text)) return scope.get(text);
            }
        }
        if (text.startsWith("conversation.")) return context.get(text);
        // 系统变量：应用/会话/调用人/当前时间与用户输入，取不到时返回空串
        if (text.startsWith("sys.")) return systemValue(text.substring(4));
        if (dot < 0) return "";
        ObjectNode node = outputs.get(text.substring(0, dot));
        if (null == node) return "";
        return node.get(text.substring(dot + 1));
    }

    public Object systemValue(String name) {
        Object value = system().get(DPUtil.parseString(name).trim());
        return null == value ? "" : value;
    }

    public Object scoped(String scopeId, String reference) {
        ObjectNode scope = scopes().get(scopeId);
        if (null == scope) return null;
        String name = DPUtil.parseString(reference);
        int at = name.lastIndexOf('.');
        if (at >= 0 && !scopeId.equals(name.substring(0, at))) return null;
        String key = at < 0 ? name : name.substring(at + 1);
        return scope.has(key) ? scope.get(key) : null;
    }

    public ObjectNode scopeOf(String name) {
        ObjectNode result = null;
        for (ObjectNode scope : scopes().values()) {
            if (scope.has(name)) result = scope;
        }
        return result;
    }

    public Map<String, String> stringMap(JsonNode node, Map<String, ObjectNode> outputs, Map<String, Object> context) {
        Map<String, String> result = new LinkedHashMap<>();
        if (null == node || !node.isObject()) return result;
        node.fields().forEachRemaining(entry ->
                result.put(entry.getKey(), text(entry.getValue().asText(""), outputs, context)));
        return result;
    }

    public boolean blank(Object value) {
        if (null == value) return true;
        if (value instanceof JsonNode) {
            JsonNode node = (JsonNode) value;
            if (node.isNull()) return true;
            if (node.isTextual()) return DPUtil.empty(node.asText(""));
            if (node.isArray() || node.isObject()) return node.isEmpty();
            return false;
        }
        return DPUtil.empty(value);
    }

    public List<Object> list(Object value) {
        List<Object> result = new ArrayList<>();
        if (null == value) return result;
        if (value instanceof JsonNode) {
            JsonNode node = (JsonNode) value;
            if (node.isArray()) {
                node.forEach(item -> result.add(DPUtil.toJSON(item, Object.class)));
            } else if (!node.isNull() && !node.isMissingNode()) {
                result.add(DPUtil.toJSON(node, Object.class));
            }
            return result;
        }
        if (value instanceof Collection) {
            result.addAll((Collection<?>) value);
            return result;
        }
        result.add(value);
        return result;
    }

    public Object field(Object item, String path) {
        if (DPUtil.empty(path)) return "";
        // 字符串等标量元素没有字段可取（字符串数组直接比较元素本身，见列表操作节点的 itemValue）
        if (item instanceof CharSequence) return "";
        JsonNode node = DPUtil.toJSON(item);
        if (null == node) return "";
        for (String key : DPUtil.parseString(path).split("\\.")) {
            if (null == node) return "";
            node = node.get(key);
        }
        if (null == node) return "";
        return node.isValueNode() ? node.asText() : DPUtil.stringify(node);
    }

    public int compare(Object left, Object right) {
        String a = DPUtil.parseString(left);
        String b = DPUtil.parseString(right);
        if (a.matches("-?\\d+(\\.\\d+)?") && b.matches("-?\\d+(\\.\\d+)?")) {
            return Double.compare(DPUtil.parseDouble(a), DPUtil.parseDouble(b));
        }
        return a.compareTo(b);
    }

    public boolean compareWith(String left, String operator, String right) {
        switch (operator) {
            case "exists": return !DPUtil.empty(left);
            case "empty": return DPUtil.empty(left);
            case "eq": return left.equals(right);
            case "ne": return !left.equals(right);
            case "contains": return left.contains(right);
            case "notContains": return !left.contains(right);
            case "startsWith": return left.startsWith(right);
            case "endsWith": return left.endsWith(right);
            // 正则写错时给出明确原因，而不是把 PatternSyntaxException 原文抛给使用方
            case "regex":
                try {
                    return left.matches(right);
                } catch (PatternSyntaxException e) {
                    throw new IllegalStateException("正则表达式无效：" + right);
                }
            case "gt": return DPUtil.parseDouble(left) > DPUtil.parseDouble(right);
            case "gte": return DPUtil.parseDouble(left) >= DPUtil.parseDouble(right);
            case "lt": return DPUtil.parseDouble(left) < DPUtil.parseDouble(right);
            case "lte": return DPUtil.parseDouble(left) <= DPUtil.parseDouble(right);
            case "in": return Arrays.asList(DPUtil.explode(",", right, null, true)).contains(left);
            case "notIn": return !Arrays.asList(DPUtil.explode(",", right, null, true)).contains(left);
            default: return false;
        }
    }

    public JsonNode summary(JsonNode value) {
        return DPUtil.toJSON(DPUtil.stringify(value));
    }

    public String message(Throwable throwable) {
        String message = throwable.getMessage();
        return DPUtil.empty(message) ? throwable.getClass().getSimpleName() : message;
    }

    /**
     * 历史记忆（多轮上下文）：按「节点自身」的记忆配置取用，节点之间不共享历史，也不互相读取。
     * - scope = conversation（完整对话，默认）：取最近 window 轮，一轮 = 用户消息 + 助手回复；
     *   开启 toolchain 时，带工具调用的助手回复还原为 assistant(tool_calls) + tool(调用结果) 的消息序列；
     * - scope = user（仅用户提问）：只取最近 window 条用户消息，适合只需理解问题的节点
     *   （问题分类器、参数提取器、输出图表）；
     * - enabled = false：不带历史。
     * 上下文不做长度截断，记忆窗口是唯一口径。
     */
    public ArrayNode historyMessages(ArrayNode history, ObjectNode data) {
        ArrayNode result = DPUtil.arrayNode();
        if (null == history || history.isEmpty()) return result;
        JsonNode memory = data.at("/memory");
        // 没有配置记忆的节点不带历史：需要历史的模型节点在节点配置里显式开启，避免隐式上下文
        if (!memory.isObject()) return result;
        if (!memory.at("/enabled").asBoolean(true)) return result;
        // 记忆范围：默认完整对话，旧数据没有该字段时保持原有行为
        boolean conversation = !"user".equals(memory.at("/scope").asText("conversation"));
        int window = Math.max(1, memory.at("/window").asInt(10));
        // 工具链记忆：只有完整对话才有历史工具调用可带（调用参数 + 返回结果）
        boolean toolchain = conversation && memory.at("/toolchain").asBoolean(false);
        for (Integer index : historyIndexes(history, window, conversation)) {
            JsonNode item = history.get(index);
            JsonNode calls = item.at("/toolCalls");
            if (toolchain && calls.isArray() && !calls.isEmpty()) {
                ArrayNode toolCalls = DPUtil.arrayNode();
                for (JsonNode call : calls) {
                    ObjectNode callNode = toolCalls.addObject();
                    callNode.put("id", call.at("/id").asText(""));
                    callNode.put("type", "function");
                    ObjectNode function = callNode.putObject("function");
                    function.put("name", call.at("/method").asText(""));
                    function.put("arguments", DPUtil.stringify(call.at("/args")));
                }
                ObjectNode assistant = result.addObject();
                assistant.put("role", "assistant");
                assistant.put("content", item.at("/content").asText(""));
                assistant.set("tool_calls", toolCalls);
                for (JsonNode call : calls) {
                    ObjectNode tool = result.addObject();
                    tool.put("role", "tool");
                    tool.put("tool_call_id", call.at("/id").asText(""));
                    tool.put("content", toolContent(call));
                }
                continue;
            }
            ObjectNode message = result.addObject();
            message.put("role", item.at("/role").asText("user"));
            message.put("content", item.at("/content").asText(""));
        }
        return result;
    }

    /**
     * 历史工具调用的返回内容：结果本身是 JSON 文本时按原文（不再套一层引号与转义），
     * 失败时把失败原因告诉模型，便于它在下一轮修正参数
     */
    protected String toolContent(JsonNode call) {
        if (2 == call.at("/status").asInt(1)) return "调用失败：" + call.at("/error").asText("");
        JsonNode result = call.at("/result");
        return result.isTextual() ? result.asText("") : DPUtil.stringify(result);
    }

    /**
     * 记忆取用的历史下标：完整对话取最近 window 轮（window × 2 条消息，保持原顺序）；
     * 仅用户提问从末尾往前取 window 条用户消息，再恢复为时间正序。
     */
    protected List<Integer> historyIndexes(ArrayNode history, int window, boolean conversation) {
        List<Integer> indexes = new ArrayList<>();
        if (conversation) {
            int from = Math.max(0, history.size() - window * 2);
            // 窗口边界可能落在助手消息上（某轮回复为空时用户与助手不成对）：
            // 从第一条用户消息开始，避免上下文以助手消息或孤立的工具消息开头
            while (from < history.size() && !"user".equals(history.get(from).at("/role").asText(""))) from++;
            for (int index = from; index < history.size(); index++) {
                indexes.add(index);
            }
            return indexes;
        }
        for (int index = history.size() - 1; index >= 0 && indexes.size() < window; index--) {
            if ("user".equals(history.get(index).at("/role").asText(""))) indexes.add(index);
        }
        Collections.reverse(indexes);
        return indexes;
    }

    public ArrayNode multimodalParts(ObjectNode data, Map<String, ObjectNode> outputs, Map<String, Object> context) {
        ArrayNode parts = DPUtil.arrayNode();
        for (JsonNode item : data.at("/multimodal")) {
            String type = item.at("/type").asText("file");
            Object value = value(item.at("/variable").asText(""), outputs, context);
            for (Object file : list(value)) {
                Map<?, ?> info = file instanceof Map ? (Map<?, ?>) file : DPUtil.buildMap("id", file);
                String id = DPUtil.parseString(info.get("id"));
                if (DPUtil.empty(id)) continue;
                String filename = DPUtil.parseString(info.get("name"));
                if ("image".equals(type)) {
                    parts.addObject().put("type", "image_url")
                            .putObject("image_url").put("url", dataUrl(id, filename, DPUtil.parseString(info.get("type"))));
                    continue;
                }
                ObjectNode part = parts.addObject();
                part.put("type", "file");
                part.putObject("file").put("file_id", id)
                        .put("filename", DPUtil.empty(filename) ? item.at("/name").asText(type) : filename);
            }
        }
        return parts;
    }

    public String dataUrl(String fileId, String filename, String contentType) {
        try {
            String url = FileFetcher.url(fileRpc, fileId, filename);
            byte[] bytes = FileFetcher.bytes(url, filename);
            String type = DPUtil.empty(contentType) ? "image/png" : contentType;
            return "data:" + type + ";base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            throw new IllegalStateException("读取图片失败：" + message(e));
        }
    }

    public ObjectNode userMessage(ArrayNode messages, String prompt, ObjectNode data,
                                     Map<String, ObjectNode> outputs, Map<String, Object> context) {
        ArrayNode parts = multimodalParts(data, outputs, context);
        ObjectNode user = messages.addObject().put("role", "user");
        if (parts.isEmpty()) {
            user.put("content", prompt);
            return user;
        }
        ArrayNode content = user.putArray("content");
        content.addObject().put("type", "text").put("text", prompt);
        parts.forEach(content::add);
        return user;
    }

    public ObjectNode chat(ObjectNode data, Map<String, ObjectNode> outputs, Map<String, Object> context, ArrayNode history) {
        if (DPUtil.empty(gatewayEndpoint)) throw new IllegalStateException("未配置模型网关地址（rpc.lm.rest）");
        ObjectNode json = DPUtil.objectNode();
        json.put("model", data.at("/model").asText(""));
        json.put("stream", false);
        if (data.at("/temperatureEnabled").asBoolean(false)) {
            json.put("temperature", data.at("/temperature").asDouble(0));
        }
        thinking(json, data);
        ArrayNode messages = json.putArray("messages");
        String system = text(data.at("/systemPrompt").asText(""), outputs, context);
        if (!DPUtil.empty(system)) messages.addObject().put("role", "system").put("content", system);
        for (JsonNode item : historyMessages(history, data)) messages.add(item);
        return json;
    }

    /** 模型 tool_calls 的 arguments：JSON 文本 → 参数表 */
    public Map<String, Object> parseArguments(String text) {
        Map<String, Object> args = DPUtil.toJSON(DPUtil.parseJSON(text, k -> DPUtil.objectNode()), Map.class);
        return null == args ? new LinkedHashMap<>() : args;
    }

}
