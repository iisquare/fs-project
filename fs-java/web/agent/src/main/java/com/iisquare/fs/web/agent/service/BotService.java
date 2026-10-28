package com.iisquare.fs.web.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.base.web.sse.SsePlainEmitter;
import com.iisquare.fs.base.web.sse.SsePlainRequest;
import com.iisquare.fs.base.web.sse.SsePlainRequestPool;
import com.iisquare.fs.base.web.util.RpcUtil;
import com.iisquare.fs.web.agent.core.AgenticRuntime;
import com.iisquare.fs.web.agent.react.AgenticToolService;
import com.iisquare.fs.web.agent.tool.FileUploader;
import com.iisquare.fs.web.core.mvc.FeignInterceptor;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.core.rpc.FileRpc;
import com.iisquare.fs.web.core.file.StreamFileUploader;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 本地客户端（桌面端等）接口服务：客户端自用的模型清单与模型对话。
 *
 * 与编排（agentic）、模型对比（compare）互不依赖——客户端需要的口径在这里维护，
 * 不动其它功能的接口与实现。会话历史由客户端自带并落在本地库，
 * 服务端只跑这一轮模型调用，不写运行日志、也不产生服务端会话。
 */
@Service
public class BotService implements DisposableBean {

    public final Charset charset = StandardCharsets.UTF_8;
    private final SsePlainRequestPool pool = new SsePlainRequestPool();
    /** 模型网关地址与认证密钥：与其它模型调用同一套服务级配置 */
    @Value("${rpc.lm.rest}")
    private String gatewayEndpoint;
    @Value("${fs.agent.token:}")
    private String gatewayToken;
    @Autowired
    AIService aiService;
    @Autowired
    DefaultRbacService rbacService;
    /** 工具方法与内置能力（知识库、数据主题、知识图谱本体、编排应用）的执行与定义组装 */
    @Autowired
    AgenticToolService agenticToolService;
    @Autowired
    AgenticRuntime agenticRuntime;
    /** 编排作为工具调用时，运行前需要注入「编排调用器」 */
    @Autowired
    AgenticService agenticService;
    /** 文件服务：客户端上传的本机文件转存到这里，拿回文件标识后即可按 id 引用 */
    @Autowired
    FileRpc fileRpc;
    @Autowired
    StreamFileUploader streamFileUploader;
    /** 客户端上传的文件桶：默认与 MCP 工具取用的文件同桶（桶需先存在于 MinIO / S3，可换成自建桶） */
    @Value("${fs.agent.bot.upload.bucket:fs-mcp}")
    private String uploadBucket;
    /** 单个文件的上传上限：与 servlet 的 multipart 上限同一量级（超出由容器先拦一次） */
    @Value("${fs.agent.bot.upload.maxSize:104857600}")
    private long uploadMaxSize;

    @Override
    public void destroy() throws Exception {
        FileUtil.close(pool);
    }

    /* ------------------------------- 模型清单 ------------------------------- */

    /**
     * 可用模型清单：取自网关的 /v1/models，也就是这个服务级密钥实际够得着的模型，
     * 归一成 `{ rows: [{ name, ownedBy, contextLength }] }`，客户端据此下拉与手输。
     * contextLength 是模型的上下文长度（词元数）：客户端用它算「上下文用量圆环」，
     * 服务端没配（给 0）时客户端按未知处理，由用户在界面上手工指定。
     */
    public Map<String, Object> models() {
        Map<String, Object> result = aiService.models();
        if (ApiUtil.failed(result)) return result;
        JsonNode payload = ApiUtil.data(result, JsonNode.class);
        if (null == payload) payload = DPUtil.objectNode();
        ObjectNode data = DPUtil.objectNode();
        ArrayNode rows = data.putArray("rows");
        for (JsonNode item : payload.at("/data")) {
            String name = item.at("/id").asText("");
            if (DPUtil.empty(name)) continue;
            ObjectNode row = rows.addObject();
            row.put("name", name);
            row.put("ownedBy", item.at("/owned_by").asText(""));
            row.put("contextLength", contextLength(item));
        }
        return ApiUtil.result(0, null, data);
    }

    /**
     * 模型上下文长度：取网关 /v1/models 给出的 context_length（网关按模型的「上下文长度」配置算出）。
     * 网关没给时退回上下文长度参数的本名 max_position_embeddings，两处都没有就是 0（未知）。
     */
    protected int contextLength(JsonNode item) {
        int length = item.at("/context_length").asInt(0);
        return length > 0 ? length : item.at("/max_position_embeddings").asInt(0);
    }

    /**
     * 当前登录用户的积分概览：网关按 x-auth-token 识别用户，
     * 返回总消耗、剩余与各积分限流周期的用量、剩余和下次重置时间。
     */
    public Map<String, Object> credit(HttpServletRequest request) {
        return aiService.credit(rbacService.token(request));
    }

    /* ------------------------------- 模型对话 ------------------------------- */

    /**
     * 模型对话（SSE）：客户端把这一轮的会话历史（messages）带上来，逐段推送增量。
     * 事件格式与编排的流式接口一致：`{ type: delta | error, data }`，
     * delta.data 为 `{ content, reasoning, finishReason }`，error.data 为 `{ message }`。
     */
    public SseEmitter chat(String body, HttpServletRequest request, HttpServletResponse response) throws IOException {
        SsePlainEmitter emitter = new SsePlainEmitter(request, response, 0L);
        ObjectNode payload = completion(parse(body));
        if (DPUtil.empty(payload.at("/model").asText(""))) {
            return emit(emitter, "error", error("模型名称不能为空")).sync(null);
        }
        if (payload.at("/messages").size() < 1) {
            return emit(emitter, "error", error("对话内容不能为空")).sync(null);
        }
        final String session = rbacService.token(request);
        SsePlainRequest req = new SsePlainRequest() {
            @Override
            public HttpRequestBase request() throws Exception {
                return http(payload, session);
            }

            @Override
            public boolean onMessage(ObjectNode message, boolean isEvent, boolean isStream) {
                if (isComment(message, isEvent)) { // 心跳包与结束标识原样转发
                    return emitter.message(message, isEvent).isRunning();
                }
                ObjectNode data = data(message, isEvent);
                if (data.has("error")) {
                    emit(emitter, "error", data.at("/error"));
                    return false;
                }
                if (data.has("choices")) {
                    // 一次请求只跑一个模型：取首个候选，正文、思考过程、工具调用与结束原因各归各的
                    JsonNode choice = data.at("/choices").path(0);
                    JsonNode source = choice.at("/delta");
                    ObjectNode delta = DPUtil.objectNode();
                    delta.put("content", source.at("/content").asText(""));
                    delta.put("reasoning", source.at("/reasoning_content").asText(""));
                    delta.put("finishReason", choice.at("/finish_reason").asText(""));
                    // 工具调用：网关按 OpenAI 规范分片推送（index / id / function.name / function.arguments 的增量），
                    // 原样转发，客户端按 index 累积成完整的调用
                    if (source.at("/tool_calls").isArray()) delta.set("toolCalls", source.at("/tool_calls"));
                    emit(emitter, "delta", delta);
                    return emitter.isRunning();
                }
                return true;
            }

            @Override
            public void onError(CloseableHttpResponse response, Throwable throwable, boolean isStream) {
                emit(emitter, "error", error(throwable.getMessage())).abort();
            }
        };
        return pool.process(req, emitter);
    }

    /**
     * 请求体组装：只认客户端真的传过来的字段，没传的交由网关取默认值。
     * messages 原样透传（客户端自带历史，含完整的工具调用链：assistant 的 tool_calls 与逐条 tool 返回），
     * systemPrompt 非空时作为首条 system 消息补在最前；tools 是客户端组装好的 function 定义清单。
     */
    public ObjectNode completion(ObjectNode json) {
        ObjectNode body = DPUtil.objectNode();
        body.put("model", json.at("/model").asText(""));
        body.put("stream", true);
        if (json.has("temperature")) body.put("temperature", json.at("/temperature").asDouble(0));
        int maxTokens = json.at("/maxTokens").asInt(0);
        if (maxTokens > 0) body.put("max_tokens", maxTokens);
        thinking(body, json);
        // 工具：客户端把本地工具与项目工具归一成同一份 function 定义清单，原样透传给网关
        if (json.at("/tools").isArray() && !json.at("/tools").isEmpty()) body.set("tools", json.at("/tools"));
        if (json.has("toolChoice")) body.set("tool_choice", json.at("toolChoice"));
        ArrayNode messages = body.putArray("messages");
        String systemPrompt = json.at("/systemPrompt").asText("");
        if (!DPUtil.empty(systemPrompt)) {
            messages.addObject().put("role", "system").put("content", systemPrompt);
        }
        for (JsonNode item : json.at("/messages")) {
            String role = item.at("/role").asText("");
            if (DPUtil.empty(role)) continue;
            ObjectNode row = messages.addObject();
            row.put("role", role);
            message(row, item);
        }
        return body;
    }

    /**
     * 单条消息的字段拼装：
     * - 带工具调用的助手消息：按 OpenAI 规范给 tool_calls，content 为空时置 null（不是空串）；
     * - 工具返回消息：带上 tool_call_id，模型据此把结果对回那次调用；
     * - 其余消息只带 content（文本原样，多模态数组原样透传）。
     */
    protected void message(ObjectNode row, JsonNode item) {
        JsonNode calls = item.at("/toolCalls");
        JsonNode content = item.at("/content");
        if (calls.isArray() && !calls.isEmpty()) {
            row.set("tool_calls", calls);
            // 带工具调用的一轮：content 为空时按规范给 null，模型据此区分「只调工具」与「边说边调」
            if (content.isMissingNode() || content.isNull() || DPUtil.empty(content.asText(""))) row.putNull("content");
            else row.set("content", content);
        } else if (content.isContainerNode()) {
            row.set("content", content);
        } else if (content.isMissingNode() || content.isNull()) {
            row.put("content", "");
        } else {
            row.put("content", content.asText(""));
        }
        if (item.has("toolCallId")) row.put("tool_call_id", item.at("/toolCallId").asText(""));
        if (item.has("name")) row.put("name", item.at("/name").asText(""));
    }

    /**
     * 思考模式与思考强度：与编排节点、模型对比同一套口径
     * - 思考模式：`thinking: { type: enabled | disabled }`（auto 时不显式声明，交给模型/网关决定）
     * - 思考强度：`reasoning_effort`，仅在思考未被显式关闭时下发
     */
    protected void thinking(ObjectNode body, ObjectNode json) {
        boolean modeEnabled = json.has("thinkMode");
        String mode = json.at("/thinkMode").asText("auto");
        if (modeEnabled) {
            if ("on".equals(mode) || "enabled".equals(mode)) {
                body.putObject("thinking").put("type", "enabled");
            } else if ("off".equals(mode) || "disabled".equals(mode)) {
                body.putObject("thinking").put("type", "disabled");
            }
        }
        if (json.has("thinkEffort")) {
            if (modeEnabled && ("off".equals(mode) || "disabled".equals(mode))) return;
            String effort = json.at("/thinkEffort").asText("");
            if (!DPUtil.empty(effort)) body.put("reasoning_effort", effort);
        }
    }

    /**
     * 调用网关的 HTTP 请求：带上服务级密钥，并在调用端带了会话令牌时一并透传，
     * 供网关按「用户代理」口径记账（密钥未开启代理时网关忽略该头）
     */
    public HttpRequestBase http(ObjectNode body, String session) {
        HttpPost http = new HttpPost(gatewayEndpoint + "/v1/chat/completions");
        http.addHeader("Authorization", "Bearer " + gatewayToken);
        http.addHeader("Content-Type", "application/json;charset=" + charset.name());
        if (!DPUtil.empty(session)) http.addHeader(FeignInterceptor.HEADER_AUTH_TOKEN, session);
        http.setEntity(new StringEntity(body.toString(), charset));
        return http;
    }

    /* ------------------------------- 文件上传 ------------------------------- */

    /**
     * 本地文件上传：客户端把本机文件按 multipart 直传上来（不经过 base64，图片 / 视频这类大文件也吃得下），
     * 服务端转存文件服务后返回文件标识（id）与名称、类型、后缀、大小。
     *
     * 客户端拿 id 即可引用该文件——如在对话里交给 multimodalAnalyze 等工具方法分析、写进知识库，
     * 不必在多轮对话里反复传文件内容。服务端读不到调用方的磁盘，所以本机路径传不进工具方法，
     * 有本地文件时的顺序是：先从这里传一次拿 id，再把 id 交给工具方法。
     *
     * 类型判定：以调用方给到的 contentType 为准；缺省或 application/octet-stream（部分客户端不带类型）
     * 时按文件名后缀补齐，两者都判断不出来按二进制存，引用时由调用方自行给出 type。
     */
    public Map<String, Object> upload(MultipartFile file, Map<?, ?> param) {
        if (null == file || file.isEmpty()) return ApiUtil.result(1001, "获取文件句柄失败", null);
        if (file.getSize() > uploadMaxSize) {
            return ApiUtil.result(1002, "文件超过上传上限（" + (uploadMaxSize / 1024 / 1024) + "MB）",
                    file.getOriginalFilename());
        }
        String filename = DPUtil.parseString(file.getOriginalFilename());
        if (DPUtil.empty(filename)) filename = DPUtil.parseString(param.get("name"));
        String declared = FileUploader.normalize(file.getContentType());
        String type = declared;
        if (DPUtil.empty(type) || "application/octet-stream".equalsIgnoreCase(type)) {
            type = FileUploader.mime("", filename, null); // 客户端没给可用类型时按文件名后缀补
        }
        if (DPUtil.empty(type)) type = "application/octet-stream";
        String suffix = FileUploader.suffix(filename);
        String date = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String filepath = String.format("bot/%s/%s%s", date,
                UUID.randomUUID().toString().replace("-", ""), DPUtil.empty(suffix) ? "" : "." + suffix);
        Map<String, Object> result = streamFileUploader.upload("/file/upload", DPUtil.buildMap(
                "bucket", uploadBucket, "filepath", filepath, "traceIdentity", "fs-agent-bot-upload"),
                file, filename, type);
        if (ApiUtil.failed(result)) return result;
        JsonNode data = ApiUtil.data(result, ObjectNode.class);
        if (null == data || DPUtil.empty(data.at("/id").asText())) {
            return ApiUtil.result(1004, "文件服务未返回文件标识", DPUtil.stringify(result.get("data")));
        }
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", data.at("/id").asText(""));
        item.put("name", DPUtil.empty(data.at("/name").asText("")) ? filename : data.at("/name").asText(""));
        item.put("type", DPUtil.empty(data.at("/type").asText("")) ? type : data.at("/type").asText(""));
        item.put("suffix", data.at("/suffix").asText(suffix));
        item.put("size", data.at("/size").asLong(file.getSize()));
        return ApiUtil.result(0, null, item);
    }

    /* ------------------------------- 工具 ------------------------------- */

    /**
     * 工具定义清单：客户端把项目里选中的工具（ref）带上来，这里补全成模型可见的 function 定义。
     *
     * 客户端驱动工具调用循环（本地文件工具由客户端自己执行），服务端只负责把「服务端工具」
     * （工具方法 / 知识库 / 数据主题 / 知识图谱本体 / 编排应用）的 function 定义与调用入口给出去，
     * 凭据与真实调用都留在服务端。
     *
     * 函数名按 OpenAI 规范归一（中文、空格等替换为下划线并去重），随定义一起回带 functionName，
     * 客户端调用时把该 ref 原样带回，服务端据此定位到同一只工具。
     */
    public Map<String, Object> tools(String body) {
        ObjectNode json = parse(body);
        ArrayNode rows = DPUtil.arrayNode();
        Set<String> used = new HashSet<>();
        for (JsonNode ref : json.at("/tools")) {
            ObjectNode item = item(ref);
            if (null == item) continue;
            String functionName = functionName(item, used);
            item.put("name", functionName);
            ArrayNode definitions;
            try {
                definitions = agenticToolService.definitions(DPUtil.arrayNode().add(item));
            } catch (Exception e) {
                continue; // 单只工具不可用（本体取不到模型等）不拖累其余工具
            }
            if (definitions.isEmpty() || !definitions.get(0).isObject()) continue;
            ObjectNode definition = ((ObjectNode) definitions.get(0)).deepCopy();
            JsonNode function = definition.at("/function");
            if (!function.isObject()) continue;
            // 归一后的函数名以服务端为准：中文名也必须是合法函数名，模型才调用得动
            ((ObjectNode) function).put("name", functionName);
            ObjectNode row = rows.addObject();
            ObjectNode target = refWithName(ref, functionName);
            row.set("ref", target);
            row.set("function", function);
        }
        ObjectNode data = DPUtil.objectNode();
        data.set("rows", rows);
        return ApiUtil.result(0, null, data);
    }

    /**
     * 执行一次服务端工具调用：客户端给出工具 ref 与模型给出的参数，这里跑完并返回结果。
     * 结果统一按 JSON 返回，失败给出原因（客户端把它作为该次工具调用的返回喂回模型，便于下一轮自行修正）。
     */
    public Map<String, Object> tool(String body, Integer uid, HttpServletRequest request) {
        ObjectNode json = parse(body);
        ObjectNode item = item(json.at("/tool"));
        if (null == item) return ApiUtil.result(1004, "工具标识无效，请重新选择工具", null);
        Map<String, Object> args = new LinkedHashMap<>();
        if (json.at("/args").isObject()) {
            Map<String, Object> parsed = DPUtil.toJSON(json.at("/args"), Map.class);
            if (null != parsed) args.putAll(parsed);
        }
        String kind = item.at("/kind").asText("method");
        String methodName = "method".equals(kind) ? item.at("/method").asText("") : item.at("/name").asText("");
        // 编排作为工具：运行前注入编排调用器（与编排运行同一套实现，子编排不写会话与运行日志）
        agenticService.prepareRuntime(uid, request);
        try {
            Object data = agenticToolService.invoke(agenticRuntime, DPUtil.arrayNode().add(item), methodName, args,
                    new LinkedHashMap<>(), new LinkedHashMap<>());
            return ApiUtil.result(0, null, DPUtil.toJSON(data));
        } catch (Exception e) {
            return ApiUtil.result(1002, DPUtil.parseString(e.getMessage()), null);
        }
    }

    /**
     * 工具项还原：客户端只存了「类型 + 标识 + 名称」，这里按类型补成 AgenticTool 认得的配置。
     * 类型与编排里大语言模型节点的工具种类一致，因此服务端只有这一条实现路径。
     */
    protected ObjectNode item(JsonNode ref) {
        if (null == ref || !ref.isObject()) return null;
        String kind = ref.at("/kind").asText("method");
        if (DPUtil.empty(kind)) kind = "method";
        ObjectNode item = DPUtil.objectNode();
        item.put("kind", kind);
        item.put("description", ref.at("/description").asText(""));
        // 固定参数：客户端在项目里配的执行变量绑定原样带上——定义时摘掉这些参数（不再交给模型），
        // 调用时按取值覆盖，与编排里大语言模型节点的工具项是同一套实现
        if (ref.at("/args").isObject()) item.set("args", ref.at("/args"));
        if ("method".equals(kind)) {
            // 工具方法：函数名就是方法名，定义与调用都按「工具标识 + 方法名」定位
            String method = ref.at("/methodName").asText("");
            if (DPUtil.empty(method)) return null;
            item.put("toolId", ref.at("/toolId").asInt(0));
            item.put("method", method);
            item.put("name", method);
            return item;
        }
        String name = ref.at("/functionName").asText("");
        if (DPUtil.empty(name)) name = ref.at("/name").asText("");
        if (DPUtil.empty(name)) return null;
        item.put("name", name);
        int id = ref.at("/id").asInt(0);
        if ("knowledge".equals(kind)) item.put("knowledgeId", id);
        else if ("theme".equals(kind)) item.put("themeId", id);
        else if ("ontology".equals(kind)) item.put("ontologyId", id);
        else if ("agentic".equals(kind)) item.put("agenticId", id);
        else return null;
        if (DPUtil.empty(item.at("/description").asText(""))) item.put("description", name);
        return item;
    }

    /** 回带给客户端的 ref：原样保留并补上归一后的函数名，调用时原样带回即可定位到同一只工具 */
    protected ObjectNode refWithName(JsonNode ref, String functionName) {
        ObjectNode target = ref.isObject() ? ((ObjectNode) ref).deepCopy() : DPUtil.objectNode();
        target.put("functionName", functionName);
        return target;
    }

    /** 函数名归一：只保留 [0-9a-zA-Z_-]，其余（中文、空格等）替换为下划线，重复时按序加后缀 */
    protected String functionName(ObjectNode item, Set<String> used) {
        String kind = item.at("/kind").asText("method");
        String base = PLACEHOLDER.matcher(item.at("/name").asText("")).replaceAll("_");
        base = base.replaceAll("_{2,}", "_").replaceAll("^_+|_+$", "");
        if (DPUtil.empty(base)) base = kind + "_" + item.at("/toolId").asInt(item.at("/knowledgeId").asInt(0));
        if (base.length() > 60) base = base.substring(0, 60);
        String name = base;
        int index = 2;
        while (used.contains(name)) {
            name = base + "_" + index;
            index++;
        }
        used.add(name);
        return name;
    }

    /** 函数名里不允许的字符 */
    protected static final Pattern PLACEHOLDER = Pattern.compile("[^0-9a-zA-Z_-]");

    /** 请求体解析：不是对象时按空对象处理，由必填校验给出明确提示 */
    protected ObjectNode parse(String json) {
        if (DPUtil.empty(json)) return DPUtil.objectNode();
        JsonNode node = DPUtil.parseJSON(json);
        return null != node && node.isObject() ? (ObjectNode) node : DPUtil.objectNode();
    }

    public ObjectNode error(String message) {
        return DPUtil.objectNode().putObject("error").put("message", message);
    }

    /** 事件报文：`{ type, data }`，与编排的流式接口同一套格式 */
    public String message(String type, Object data) {
        ObjectNode message = DPUtil.objectNode();
        message.put("type", type);
        message.replace("data", DPUtil.toJSON(data));
        return message.toString();
    }

    public SsePlainEmitter emit(SsePlainEmitter emitter, String type, Object data) {
        return emitter.data(message(type, data));
    }

}
