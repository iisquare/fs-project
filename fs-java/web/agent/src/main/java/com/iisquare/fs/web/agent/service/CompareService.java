package com.iisquare.fs.web.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.base.web.sse.SsePlainEmitter;
import com.iisquare.fs.base.web.sse.SsePlainRequest;
import com.iisquare.fs.base.web.sse.SsePlainRequestPool;
import com.iisquare.fs.base.web.util.ServletUtil;
import com.iisquare.fs.web.agent.dao.CompareRunDao;
import com.iisquare.fs.web.agent.dao.CompareWorkspaceDao;
import com.iisquare.fs.web.agent.dao.CompareCallDao;
import com.iisquare.fs.web.agent.dao.CompareFeedbackDao;
import com.iisquare.fs.web.agent.entity.CompareRun;
import com.iisquare.fs.web.agent.entity.CompareWorkspace;
import com.iisquare.fs.web.agent.entity.CompareCall;
import com.iisquare.fs.web.agent.entity.CompareFeedback;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模型对比调试：agent 模块里直接面向模型网关的一条线。
 *
 * 要点在「一个模型一条连接」——每个模型有自己的温度、输出长度与思考档位，
 * 合并成一条流就分不开了，所以一次请求只跑一个模型，由前端并发发起、逐卡展示。
 *
 * 报文是 `{ action, data }`，前端按 action 归位；
 * 推理参数的下发口径与编排节点的 AgenticRuntime.thinking 保持一致，
 * 免得同一个「思考强度」在调试页和编排里落到网关上是两个不同的写法。
 */
@Service
public class CompareService extends JPAServiceBase implements DisposableBean {

    public final Charset charset = StandardCharsets.UTF_8;
    private final SsePlainRequestPool pool = new SsePlainRequestPool();
    @Autowired
    AIService aiService;
    @Autowired
    CompareWorkspaceDao workspaceDao;
    @Autowired
    CompareRunDao runDao;
    @Autowired
    CompareCallDao callDao;
    @Autowired
    CompareFeedbackDao feedbackDao;
    @Autowired
    DefaultRbacService rbacService;
    @Value("${rpc.lm.rest}")
    private String gatewayEndpoint; // 模型网关地址，推理经 /v1/chat/completions 转发
    /** 模型网关认证密钥：整个 agent 服务共用一个（应用上配置的认证标识是对话那条线的事） */
    @Value("${fs.agent.token:}")
    private String gatewayToken;
    /** 单个模型的输出上限（字符）：模型陷入死循环时不能无限攒下去，越过即主动中断 */
    @Value("${fs.agent.compare.maxContentLength:500000}")
    private int maxContentLength;
    /** 单个模型的运行时长上限（毫秒）：到点主动中断，保证任何一轮都会收口 */
    @Value("${fs.agent.compare.maxDuration:1800000}")
    private long maxDuration;
    /** 此刻还在跑的流，按运行记录分组（内存态，只认本实例），供「终止本轮」用 */
    protected final Map<Integer, Set<CallRecord>> active = new ConcurrentHashMap<>();

    @Override
    public void destroy() throws Exception {
        FileUtil.close(pool);
    }

    @Override
    public Map<String, String> sorts() {
        Map<String, String> sorts = new LinkedHashMap<>();
        sorts.put("id", "desc");
        return sorts;
    }

    /* ------------------------------- 流式推理 ------------------------------- */

    /**
     * 单个模型的流式推理：一次请求一个模型，增量实时推送。
     * 事件：choices.message（增量，含 finish_reason）、error.message、error.throwable
     *
     * 记录由服务端写：请求带上 runId 与 key，增量在转发给页面的同时攒一份，
     * 流结束时（不论正常结束、模型报错，还是调用方中途离开）落成一条调用明细。
     * 页面只负责把标识传下来，切页面、刷新、断网都不会让这一轮消失在历史之外。
     */
    public SseEmitter stream(ObjectNode json, HttpServletRequest request, HttpServletResponse response) throws IOException {
        SsePlainEmitter emitter = new SsePlainEmitter(request, response, 0L);
        ObjectNode body = completion(json);
        // 发起时间与参数快照在请求进来时就定下来：调用方的时钟与服务端不一致时以服务端为准
        final CallRecord record = new CallRecord(
                DPUtil.parseInt(json.at("/runId").asInt(0)),
                json.at("/key").asText(""),
                body.at("/model").asText(""),
                rbacService.uid(request),
                snapshot(json));
        if (DPUtil.empty(body.at("/model").asText(""))) {
            // 连模型都没指定也要留一行：否则这一轮在历史上永远停在「进行中」
            record.fail("模型名称不能为空");
            saveCall(record);
            message(emitter, "error.message", error("模型名称不能为空"));
            return emitter.sync(null);
        }
        SsePlainRequest req = new SsePlainRequest() {
            @Override
            public HttpRequestBase request() throws Exception {
                return http(body);
            }

            @Override
            public boolean onMessage(ObjectNode message, boolean isEvent, boolean isStream) {
                // 兜底检查放在最前面：心跳包也算「还活着」，模型只发心跳不出内容时同样要能到点收口
                String exceeded = exceeded(record);
                if (null != exceeded) return abortByLimit(emitter, record, exceeded);
                if (isComment(message, isEvent)) { // 心跳包与结束标识原样转发
                    return emitter.message(message, isEvent).isRunning();
                }
                ObjectNode data = data(message, isEvent);
                if (data.has("error")) {
                    record.fail(data.at("/error/message").asText("模型调用失败"));
                    message(emitter, "error.message", data);
                    return false;
                }
                if (data.has("choices")) {
                    // 整个 choices 数组原样转发：思考、正文、finish_reason 都在里面，由前端各归各的；
                    // 服务端按同一份增量攒记录，页面看到什么，历史里就留下什么
                    record.accumulate(data.at("/choices"));
                    message(emitter, "choices.message", data.at("/choices"));
                    return emitter.isRunning();
                }
                message(emitter, "error.unknown", data);
                return false;
            }

            @Override
            public void onError(CloseableHttpResponse response, Throwable throwable, boolean isStream) {
                record.fail(throwable.getMessage());
                message(emitter, "error.throwable", error(throwable.getMessage())).abort();
            }

            @Override
            public void onClose() {
                // 客户端主动断开（切页面 / 点停止）、响应超时都会先把 emitter 停掉，这两种算中断；
                // 已经在记录里的增量照样存下来，调了半天什么都没有才是最难受的
                if (!emitter.isRunning()) record.markAborted("已中断（连接断开或超时）");
                saveCall(record);
                // 先落库再摘牌：跟随刷新以库里那份为准，顺序反了会有一瞬间两边都看不到这条
                unregister(record);
            }
        };
        record.attach(emitter, req);
        register(record);
        return pool.process(req, emitter);
    }

    /**
     * 兜底判断：越过阈值时给出中断原因，正常返回 null（阈值见 fs.agent.compare.*）。
     *
     * 模型陷入死循环时输出会一直续下去，网关的读超时（相邻两次读取的间隔）对它不起作用，
     * 必须由服务端自己设上限，否则这一轮会永远停在「进行中」。
     */
    protected String exceeded(CallRecord record) {
        if (record.failed) return null;
        if (maxContentLength > 0 && record.length() > maxContentLength) {
            return "输出已超过 " + maxContentLength + " 字符，已自动中断（疑似异常输出）";
        }
        if (maxDuration > 0 && System.currentTimeMillis() - record.createdTime > maxDuration) {
            return "运行已超过 " + (maxDuration / 60000L) + " 分钟，已自动中断";
        }
        return null;
    }

    /** 越过兜底阈值：先告诉页面为什么停，再收口（返回 false，由 pool 断开上游请求） */
    protected boolean abortByLimit(SsePlainEmitter emitter, CallRecord record, String reason) {
        record.markAborted(reason);
        message(emitter, "error.message", error(reason));
        emitter.abort();
        return false;
    }

    /**
     * 一次模型调用的服务端记录：增量在转发的同时攒在这里，流结束时整行落库。
     *
     * 放在服务端而不是等页面回传，是因为「调用方随时可能消失」：
     * 切页面、刷新、关标签、断网都不会走页面的收尾逻辑，但一定会走 SsePlainRequest.onClose()。
     * 它同时是「终止」的落点：记录上挂着这条流的两端，终止时先告诉页面为什么停，再断开上下游。
     */
    protected class CallRecord {

        /** 所属运行记录，0 表示这一轮不留档（建档失败或没带标识的调用方） */
        final int runId;
        /** 页面上的本地键：明细行与卡片靠它对上，反馈也按它对应的 callId 定位 */
        final String key;
        final String model;
        final int uid;
        /** 实际下发的参数快照：回溯以它为准，而不是去关联可变的配置 */
        final ObjectNode payload;
        /** 发起时间（服务端接收时刻） */
        final long createdTime;
        /** 首个增量的时刻，0 表示一个增量都没收到 */
        long firstTokenTime = 0;
        /** finish-完成，abort-中断，error-失败 */
        volatile String state = "finish";
        volatile String error = "";
        String finishReason = "";
        /** 是否已经定性为失败：失败优先于中断，读取异常与业务报错都算（终止线程也会读，故 volatile） */
        volatile boolean failed = false;
        /** 是否已经落库：跟随刷新会并发读这份内容，落库前它是「还在跑」的那一条 */
        volatile boolean settled = false;
        /**
         * 攒下来的正文与思考。
         * 用 StringBuffer 而不是 StringBuilder：跟随刷新会在另一个线程上取这两份文本，
         * 非线程安全的缓冲被并发读写会取到撕裂的内容。
         */
        final StringBuffer content = new StringBuffer();
        final StringBuffer reasoning = new StringBuffer();
        /** 这条流的两端：由 stream() 启动前挂上，终止时先给页面一个原因，再断开上下游 */
        SsePlainEmitter emitter;
        SsePlainRequest request;

        CallRecord(int runId, String key, String model, int uid, ObjectNode payload) {
            this.runId = runId;
            this.key = key;
            this.model = model;
            this.uid = uid;
            this.payload = payload;
            this.createdTime = System.currentTimeMillis();
        }

        void attach(SsePlainEmitter emitter, SsePlainRequest request) {
            this.emitter = emitter;
            this.request = request;
        }

        /** 已经攒下的输出长度：正文 + 思考，供兜底阈值判断 */
        int length() {
            return content.length() + reasoning.length();
        }

        /** 记一次失败：网关给的 error 报文与读取异常都归到这里 */
        void fail(String message) {
            if (failed) return;
            failed = true;
            state = "error";
            error = DPUtil.trim(DPUtil.parseString(message));
        }

        /**
         * 定性为中断：失败优先，且只认第一个中断原因——
         * 自动兜底与手动终止各有更具体的说法，别被收尾时的通用原因盖掉。
         */
        void markAborted(String reason) {
            if (failed || "abort".equals(state)) return;
            state = "abort";
            error = reason;
        }

        /**
         * 终止这条流：先给页面一个原因——否则页面只看到连接断开，会把这一轮当成正常结束；
         * 再断开上下游：上游断开后读取循环结束，onClose 照常把已经收到的内容落库。
         */
        void stop(String reason) {
            markAborted(reason);
            SsePlainEmitter target = emitter;
            if (null == target) return;
            if (target.isRunning()) message(target, "error.message", error(error));
            target.abort();
            if (null != request) request.abort();
        }

        /** 攒增量：正文与思考各归各的，结束原因以最后一次非空为准 */
        void accumulate(JsonNode choices) {
            for (JsonNode choice : choices) {
                JsonNode source = choice.at("/delta");
                if (source.isMissingNode() || source.isNull()) source = choice.at("/message");
                String contentPart = source.at("/content").asText("");
                String reasoningPart = source.at("/reasoning_content").asText("");
                if (!contentPart.isEmpty() || !reasoningPart.isEmpty()) {
                    if (firstTokenTime < 1) firstTokenTime = System.currentTimeMillis();
                    content.append(contentPart);
                    reasoning.append(reasoningPart);
                }
                String reason = choice.at("/finish_reason").asText("");
                if (!DPUtil.empty(reason)) finishReason = reason;
            }
        }

    }

    /** 登记一条正在跑的流：只有建档的那一轮才需要被「终止本轮」找到 */
    protected void register(CallRecord record) {
        if (record.runId < 1) return;
        active.computeIfAbsent(record.runId, key -> ConcurrentHashMap.newKeySet()).add(record);
    }

    /** 注销：流收尾时调用；空集合顺手清掉，免得这张表随着历史一直长 */
    protected void unregister(CallRecord record) {
        if (record.runId < 1) return;
        Set<CallRecord> streams = active.get(record.runId);
        if (null == streams) return;
        streams.remove(record);
        if (streams.isEmpty()) active.remove(record.runId, streams);
    }

    /** 某一轮此刻还有几条流在跑：页面据此分辨「进行中」是真在跑，还是只剩一条待收口的记录 */
    public int activeCount(Integer runId) {
        Set<CallRecord> streams = null == runId ? null : active.get(runId);
        return null == streams ? 0 : streams.size();
    }

    /** 断掉某一轮还在跑的流，返回断掉几条（内存态，只认本实例） */
    protected int stop(Integer runId) {
        Set<CallRecord> streams = active.get(runId);
        if (null == streams || streams.isEmpty()) return 0;
        List<CallRecord> targets = new ArrayList<>(streams);
        for (CallRecord item : targets) item.stop("本轮已终止");
        return targets.size();
    }

    /**
     * 等这一轮的流收尾：终止是异步生效的（要等上游断开、明细落库），
     * 补齐残局之前先让它们走完，免得同一条明细被写两次。
     */
    protected void await(Integer runId, long timeout) {
        long begin = System.currentTimeMillis();
        while (System.currentTimeMillis() - begin < timeout) {
            if (activeCount(runId) < 1) return;
            try {
                Thread.sleep(50L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /**
     * 参数快照：请求体去掉记录用的两个字段，剩下的才是「实际下发给模型的参数」，
     * 免得 runId / key 混进快照里污染回溯。
     */
    protected ObjectNode snapshot(ObjectNode json) {
        ObjectNode snapshot = json.deepCopy();
        snapshot.remove("runId");
        snapshot.remove("key");
        return snapshot;
    }

    /**
     * 明细落库 + 批次摘要重算。
     *
     * 跑在流式线程的收尾处（onClose），一定早于响应结束：页面拿到「流已结束」时，
     * 这一行已经在库里了，回来后立刻就能在历史里看到。
     */
    protected void saveCall(CallRecord record) {
        if (record.runId < 1) return; // 没建档：只推流，不留档
        CompareRun run = info(runDao, record.runId);
        // 记录已被删除、或不是本人的：这一轮的输出不再落库（删掉之后又被写回来最让人意外）
        if (null == run || run.getDeletedTime() > 0 || !Objects.equals(run.getCreatedUid(), record.uid)) return;
        long finishedTime = System.currentTimeMillis();
        callDao.save(CompareCall.builder()
                .runId(record.runId)
                .callKey(record.key)
                .model(record.model)
                .state(record.state)
                .duration(finishedTime - record.createdTime)
                .firstToken(record.firstTokenTime > 0 ? record.firstTokenTime - record.createdTime : 0L)
                .contentLength(record.content.length())
                .reasoningLength(record.reasoning.length())
                .finishReason(record.finishReason)
                .error(record.error)
                .payload(record.payload.toString())
                .content(record.content.toString())
                .reasoning(record.reasoning.toString())
                .createdTime(record.createdTime)
                .createdUid(record.uid)
                .build());
        // 落库之后才摘掉「还在跑」这个身份：在那之前，跟随刷新会拿内存里这一份给页面看
        record.settled = true;
        refreshRun(run);
    }

    /**
     * 重算批次摘要、状态与耗时：调用方可能中途就走，摘要只能由服务端按已落库的明细自己收口。
     * 状态：0-进行中（还有模型的流没结束），1-全部完成，2-存在失败或中断。
     */
    protected void refreshRun(CompareRun run) {
        List<CompareCall> calls = callDao.findByRunIdOrderByCreatedTimeAscIdAsc(run.getId());
        int finish = 0, error = 0, abort = 0;
        long earliest = 0, latest = 0;
        for (CompareCall item : calls) {
            String state = item.getState();
            if ("finish".equals(state)) finish++;
            else if ("error".equals(state)) error++;
            else if ("abort".equals(state)) abort++;
            long created = null == item.getCreatedTime() ? 0L : item.getCreatedTime();
            long finished = created + (null == item.getDuration() ? 0L : item.getDuration());
            if (created > 0 && (earliest < 1 || created < earliest)) earliest = created;
            if (finished > latest) latest = finished;
        }
        int total = null == run.getModelCount() ? 0 : run.getModelCount();
        int pending = Math.max(0, total - finish - error - abort);
        StringBuilder summary = new StringBuilder();
        appendSummary(summary, finish, "完成");
        appendSummary(summary, error, "失败");
        appendSummary(summary, abort, "中断");
        appendSummary(summary, pending, "进行中");
        int status = pending > 0 ? 0 : (error > 0 || abort > 0 ? 2 : 1);
        runDao.updateSummary(run.getId(), status, summary.toString(),
                earliest > 0 && latest > earliest ? latest - earliest : 0L);
    }

    /** 摘要片段：`N 个完成 · M 个失败`，个数为 0 的不写 */
    protected void appendSummary(StringBuilder summary, int count, String label) {
        if (count < 1) return;
        if (summary.length() > 0) summary.append(" · ");
        summary.append(count).append(" 个").append(label);
    }

    /**
     * 请求体组装：只认页面上真的传过来的字段，没传的交由网关取默认值。
     * 「没配过」与「显式配成 0 / 关闭」在这里必须分得开，否则调参对照就没有意义。
     */
    public ObjectNode completion(ObjectNode json) {
        ObjectNode body = DPUtil.objectNode();
        // 自定义参数打底：其余显式字段在其之上覆盖，避免被 parameter 里的同名项改掉
        JsonNode parameter = parse(json.at("/parameter").asText(""));
        if (null != parameter && parameter.isObject()) body.setAll((ObjectNode) parameter);
        body.put("model", json.at("/model").asText(""));
        body.put("stream", json.at("/stream").asBoolean(true));
        if (json.has("temperature")) body.put("temperature", json.at("/temperature").asDouble(0));
        int maxTokens = json.at("/maxTokens").asInt(0);
        if (maxTokens > 0) body.put("max_tokens", maxTokens);
        thinking(body, json);
        ArrayNode messages = body.putArray("messages");
        String systemPrompt = json.at("/systemPrompt").asText("");
        if (!DPUtil.empty(systemPrompt)) {
            messages.addObject().put("role", "system").put("content", systemPrompt);
        }
        messages.addObject().put("role", "user").put("content", json.at("/input").asText(""));
        return body;
    }

    /**
     * 思考模式与思考强度：与编排节点 AgenticRuntime.thinking 同一套口径
     * - 思考模式：`thinking: { type: enabled | disabled }`（auto 时不显式声明，交给模型/网关决定）
     * - 思考强度：`reasoning_effort`，仅在思考未被显式关闭时下发
     * - 全局的「启用思考」开关：关掉且没有更具体的档位时，显式声明关闭
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
        if (!body.has("thinking") && json.has("think") && !json.at("/think").asBoolean(true)) {
            body.putObject("thinking").put("type", "disabled");
        }
        if (json.has("thinkEffort")) {
            // 思考显式关闭时不再下发强度（与设计器里的可编辑判断保持一致）
            if (modeEnabled && ("off".equals(mode) || "disabled".equals(mode))) return;
            String effort = json.at("/thinkEffort").asText("");
            if (!DPUtil.empty(effort)) body.put("reasoning_effort", effort);
        }
    }

    public HttpRequestBase http(ObjectNode body) {
        HttpPost http = new HttpPost(gatewayEndpoint + "/v1/chat/completions");
        http.addHeader("Authorization", "Bearer " + gatewayToken);
        http.addHeader("Content-Type", "application/json;charset=" + charset.name());
        http.setEntity(new StringEntity(body.toString(), charset));
        return http;
    }

    /* ------------------------------- 可选模型 ------------------------------- */

    /**
     * 可调试的模型清单：取自网关的 /v1/models，也就是这个服务级密钥实际够得着的模型。
     * 归一成 `{ rows: [{ name, ownedBy, contextLength }] }`，页面的下拉与手输都用它。
     * contextLength 是模型的上下文长度（词元数），与客户端 bot 接口同一口径。
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

    /* ------------------------------- 工作区 ------------------------------- */

    /**
     * 可见的工作区：我的全部（含我共享出去的）+ 别人共享出来的，我的排在前面。
     * 一个用户可以有多个、各自命名，用来并行推进不同场景的对比测试。
     * 只返回摘要，不含 models / settings 两个大字段；mine 标记决定页面上能做什么。
     */
    public ObjectNode workspaceList(HttpServletRequest request) {
        int uid = rbacService.uid(request);
        ObjectNode result = DPUtil.objectNode();
        ArrayNode rows = result.putArray("rows");
        for (CompareWorkspaceDao.WorkspaceIndex item : workspaceDao
                .findByCreatedUidAndDeletedTimeOrderByIdDesc(uid, 0L)) {
            rows.add(workspaceNode(item, uid));
        }
        for (CompareWorkspaceDao.WorkspaceIndex item : workspaceDao
                .findBySharedAndCreatedUidNotAndDeletedTimeOrderByIdDesc(1, uid, 0L)) {
            rows.add(workspaceNode(item, uid));
        }
        // 补上属主昵称，页面上「谁共享的」才看得清
        rbacService.fillUserInfo(rows, "createdUid");
        return result;
    }

    /**
     * 打开某个工作区：自己的、或别人共享出来的都能看；既不是自己的又没共享就是不存在。
     * mine 标记决定页面是「可保存」还是「只能另存为」。
     * 数据库列名用 settings（global 是 MySQL 保留字），接口上仍叫 global，与页面字段保持一致。
     */
    public ObjectNode workspaceInfo(Integer id, HttpServletRequest request) {
        int uid = rbacService.uid(request);
        CompareWorkspace info = workspaceDao.findFirstByIdAndDeletedTime(id, 0L);
        if (null == info) return null;
        boolean mine = Objects.equals(info.getCreatedUid(), uid);
        if (!mine && !Integer.valueOf(1).equals(info.getShared())) return null;
        ObjectNode result = workspaceSummary(info);
        result.put("mine", mine);
        result.replace("models", array(info.getModels()));
        result.replace("global", object(info.getSettings()));
        ArrayNode rows = DPUtil.arrayNode();
        rows.add(result);
        rbacService.fillUserInfo(rows, "createdUid");
        return result;
    }

    /**
     * 保存工作区：带 id 是更新，不带是新建；名称必填——多个工作区靠它区分场景。
     * 只能保存自己的：他人的共享工作区要留成自己的，走「另存为」（不带 id）新建一份。
     * 保存是显式动作，不随编辑自动触发。
     */
    public Map<String, Object> workspaceSave(Map<?, ?> param, HttpServletRequest request) {
        int uid = rbacService.uid(request);
        if (uid < 1) return ApiUtil.result(9403, null, null);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if (DPUtil.empty(name)) return ApiUtil.result(1001, "工作区名称不能为空", null);
        int id = DPUtil.parseInt(param.get("id"));
        CompareWorkspace info;
        if (id > 0) {
            info = workspaceDao.findFirstByIdAndDeletedTime(id, 0L);
            if (null == info) return ApiUtil.result(404, null, id);
            if (!Objects.equals(info.getCreatedUid(), uid)) {
                return ApiUtil.result(9403, "只能保存自己的工作区，他人的共享工作区请另存为", null);
            }
        } else {
            info = CompareWorkspace.builder().deletedTime(0L).shared(0).sharedTime(0L).build();
        }
        ArrayNode models = array(DPUtil.stringify(DPUtil.toJSON(param.get("models"))));
        info.setName(name);
        info.setModelCount(models.size());
        info.setModels(models.toString());
        info.setSettings(object(DPUtil.stringify(DPUtil.toJSON(param.get("global")))).toString());
        info = save(workspaceDao, info, uid);
        ObjectNode summary = workspaceSummary(info);
        summary.put("mine", true);
        return ApiUtil.result(0, null, summary);
    }

    /**
     * 共享 / 收回共享：只有属主能动。
     * 共享后所有人可见，但可见不等于可改——他人打开后要留成自己的，走「另存为」新建一份。
     */
    public boolean workspaceShare(List<Integer> ids, boolean shared, HttpServletRequest request) {
        if (null == ids || ids.isEmpty()) return false;
        int uid = rbacService.uid(request);
        long time = System.currentTimeMillis();
        List<CompareWorkspace> targets = new ArrayList<>();
        for (CompareWorkspace item : workspaceDao.findAllById(ids)) {
            if (item.getDeletedTime() > 0) continue;
            if (!Objects.equals(item.getCreatedUid(), uid)) continue; // 别人的共享不能替人收回
            item.setShared(shared ? 1 : 0);
            item.setSharedTime(shared ? time : 0L);
            targets.add(item);
        }
        if (targets.isEmpty()) return false;
        workspaceDao.saveAll(targets);
        return true;
    }

    /** 删除工作区：标记删除，只作用于自己的；对比记录不受影响 */
    public Map<String, Object> workspaceDelete(List<Integer> ids, HttpServletRequest request) {
        if (null == ids || ids.isEmpty()) return ApiUtil.result(0, null, 0);
        try {
            int uid = rbacService.uid(request);
            List<CompareWorkspace> rows = workspaceDao.findAllById(ids);
            long time = System.currentTimeMillis();
            List<CompareWorkspace> targets = new ArrayList<>();
            for (CompareWorkspace item : rows) {
                if (item.getDeletedTime() > 0) continue;
                if (!Objects.equals(item.getCreatedUid(), uid)) continue;
                item.setDeletedTime(time);
                item.setDeletedUid(uid);
                targets.add(item);
            }
            if (targets.isEmpty()) return ApiUtil.result(1404, "没有可删除的工作区", ids);
            workspaceDao.saveAll(targets);
            return ApiUtil.result(0, null, targets.size());
        } catch (Exception e) {
            return ApiUtil.result(1500, "删除工作区失败", e.getMessage());
        }
    }

    /** 投影 → 摘要行：列表用 */
    protected ObjectNode workspaceNode(CompareWorkspaceDao.WorkspaceIndex item, int uid) {
        ObjectNode row = DPUtil.objectNode();
        row.put("id", item.getId());
        row.put("name", item.getName());
        row.put("modelCount", null == item.getModelCount() ? 0 : item.getModelCount());
        row.put("shared", null == item.getShared() ? 0 : item.getShared());
        row.put("sharedTime", null == item.getSharedTime() ? 0L : item.getSharedTime());
        row.put("createdTime", null == item.getCreatedTime() ? 0L : item.getCreatedTime());
        row.put("updatedTime", null == item.getUpdatedTime() ? 0L : item.getUpdatedTime());
        row.put("createdUid", null == item.getCreatedUid() ? 0 : item.getCreatedUid());
        row.put("mine", Objects.equals(item.getCreatedUid(), uid));
        return row;
    }

    /** 实体 → 摘要行：保存与详情用（不含 models / settings） */
    protected ObjectNode workspaceSummary(CompareWorkspace info) {
        ObjectNode result = DPUtil.objectNode();
        result.put("id", info.getId());
        result.put("name", info.getName());
        result.put("modelCount", null == info.getModelCount() ? 0 : info.getModelCount());
        result.put("shared", null == info.getShared() ? 0 : info.getShared());
        result.put("sharedTime", null == info.getSharedTime() ? 0L : info.getSharedTime());
        result.put("createdTime", null == info.getCreatedTime() ? 0L : info.getCreatedTime());
        result.put("updatedTime", null == info.getUpdatedTime() ? 0L : info.getUpdatedTime());
        result.put("createdUid", null == info.getCreatedUid() ? 0 : info.getCreatedUid());
        return result;
    }

    /* ------------------------------- 运行记录 ------------------------------- */

    /** 运行记录列表：按标题检索（标题即用户输入），分页返回 */
    public ObjectNode runSearch(Map<String, Object> param, HttpServletRequest request) {
        int page = ValidateUtil.filterInteger(param.get("page"), 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), 1, 100, 20);
        String title = DPUtil.trim(DPUtil.parseString(param.get("title")));
        // 传原文即可：空串拼成 like '%%' 就是全匹配，查询本身不用再分两条
        Page<CompareRunDao.RunIndex> data = runDao.search(
                rbacService.uid(request), title, PageRequest.of(page - 1, pageSize));
        ObjectNode result = DPUtil.objectNode();
        result.put(ApiUtil.FIELD_DATA_PAGE, page)
                .put(ApiUtil.FIELD_DATA_PAGE_SIZE, pageSize)
                .put(ApiUtil.FIELD_DATA_TOTAL, data.getTotalElements())
                .replace(ApiUtil.FIELD_DATA_ROWS, DPUtil.toJSON(data.getContent(), ArrayNode.class));
        return result;
    }

    /**
     * 运行记录详情：只允许看自己的。全局设置与模型参数取自运行记录，
     * 各模型的输出从调用明细（CompareCall）拼回来——输出只存一份，
     * 顺带把「我」给每张卡片的反馈挂上去，回看时赞/踩状态能直接还原。
     */
    public ObjectNode runInfo(Integer id, HttpServletRequest request) {
        int uid = rbacService.uid(request);
        CompareRun info = info(runDao, id);
        if (null == info || info.getDeletedTime() > 0) return null;
        if (!Objects.equals(info.getCreatedUid(), uid)) return null;
        ObjectNode result = DPUtil.objectNode();
        result.put("id", info.getId());
        result.put("title", info.getTitle());
        result.put("status", info.getStatus());
        // 还在跑的流有几条（内存态）：页面据此分辨「进行中」是真在跑，还是只剩一条待收口的记录
        result.put("active", activeCount(info.getId()));
        result.put("modelCount", info.getModelCount());
        result.put("duration", info.getDuration());
        result.put("summary", info.getSummary());
        result.put("createdTime", info.getCreatedTime());
        result.replace("global", object(info.getSettings()));
        result.replace("models", array(info.getModels()));
        result.replace("results", results(info.getId(), uid));
        return result;
    }

    /**
     * 调用明细 → 页面上的结果数组（按发起时间升序，与提交时各卡片的顺序一致）。
     *
     * 明细是在流收尾时才落库的，只读库里那份的话，一轮「进行中」的记录在收尾前全是空的——
     * 跟随刷新什么都看不到。所以这里把此刻还在跑的流一并带上（内存态，只认本实例），
     * 页面看到的就与「在这一轮里直接看」是同一份内容；收尾落库后自动改以库里那份为准。
     */
    protected ArrayNode results(Integer runId, int uid) {
        ArrayNode rows = DPUtil.arrayNode();
        for (CompareCall item : callDao.findByRunIdOrderByCreatedTimeAscIdAsc(runId)) {
            ObjectNode row = rows.addObject();
            row.put("id", item.getId());
            row.put("key", item.getCallKey());
            row.put("model", item.getModel());
            row.put("state", item.getState());
            row.put("content", item.getContent());
            row.put("reasoning", item.getReasoning());
            row.put("finishReason", item.getFinishReason());
            row.put("error", item.getError());
            row.replace("payload", object(item.getPayload()));
            row.put("createdTime", null == item.getCreatedTime() ? 0L : item.getCreatedTime());
            long first = null == item.getFirstToken() ? 0L : item.getFirstToken();
            row.put("firstTokenTime", first > 0 ? item.getCreatedTime() + first : 0L);
            row.put("finishedTime", item.getCreatedTime() + (null == item.getDuration() ? 0L : item.getDuration()));
        }
        fillFeedback(rows, runId, uid);
        Set<CallRecord> streams = active.get(runId);
        if (null != streams) {
            for (CallRecord item : streams) {
                if (item.settled) continue; // 已经落库的以库里那份为准
                ObjectNode row = rows.addObject();
                row.put("id", 0); // 还没落库：反馈按 id 定位，回读时才拿得到
                row.put("key", item.key);
                row.put("model", item.model);
                row.put("state", "streaming"); // 库里只存收尾后的状态，在跑的这条按「生成中」展示
                row.put("content", item.content.toString());
                row.put("reasoning", item.reasoning.toString());
                row.put("finishReason", item.finishReason);
                row.put("error", "");
                row.replace("payload", item.payload.deepCopy());
                row.put("createdTime", item.createdTime);
                row.put("firstTokenTime", item.firstTokenTime);
                row.put("finishedTime", 0L);
            }
        }
        return rows;
    }

    /** 把「我」对每一行的赞/踩挂到对应结果上，卡片据此回显 */
    protected void fillFeedback(ArrayNode rows, Integer runId, int uid) {
        for (CompareFeedback item : feedbackDao.findByRunIdAndDeletedTime(runId, 0L)) {
            if (!Objects.equals(item.getCreatedUid(), uid)) continue;
            for (JsonNode node : rows) {
                if (!(node instanceof ObjectNode row)) continue;
                if (!Objects.equals(row.at("/id").asInt(0), item.getCallId())) continue;
                row.put("feedbackEmotion", item.getEmotion());
                row.put("feedbackTag", item.getTag());
                row.put("feedbackContent", item.getContent());
            }
        }
    }

    /**
     * 建立一条运行记录：页面点「发送」时先建档，拿到标识后再把标识交给各模型的流。
     *
     * 先建档、后写明细的顺序，是为了让记录的归属落在服务端：
     * 明细随各自的流在服务端落库，页面切走、刷新、断网都影响不到它；
     * 建档时状态是 0（进行中），每写完一条明细重算一次摘要，全部到齐才收口。
     */
    public Map<String, Object> runCreate(Map<?, ?> param, HttpServletRequest request) {
        int uid = rbacService.uid(request);
        if (uid < 1) return ApiUtil.result(9403, null, null);
        ArrayNode models = array(DPUtil.stringify(DPUtil.toJSON(param.get("models"))));
        if (models.isEmpty()) return ApiUtil.result(1001, "请先添加至少一个模型", null);
        ObjectNode settings = object(DPUtil.stringify(DPUtil.toJSON(param.get("global"))));
        // 标题即用户输入：列表里一眼能认出是哪一问，没填输入就留空由页面兜底展示
        String title = DPUtil.trim(DPUtil.parseString(param.get("title")));
        if (DPUtil.empty(title)) title = DPUtil.trim(settings.at("/input").asText(""));
        if (title.length() > 40) title = title.substring(0, 40);
        CompareRun info = CompareRun.builder()
                .title(title)
                .status(0)
                .modelCount(models.size())
                .duration(0L)
                .summary("")
                .settings(settings.toString())
                .models(models.toString())
                .ip(ServletUtil.getRemoteAddr(request))
                .createdTime(System.currentTimeMillis())
                .createdUid(uid)
                .deletedTime(0L)
                .build();
        info = runDao.save(info);
        ObjectNode result = DPUtil.objectNode();
        result.put("id", info.getId());
        result.put("modelCount", info.getModelCount());
        result.put("createdTime", info.getCreatedTime());
        return ApiUtil.result(0, null, result);
    }

    /**
     * 终止一轮：断掉这一轮还在跑的流，再把一直没有回来的模型补成「中断」。
     *
     * 模型卡死、输出失控时，页面上的「停止」不一定还够得着——从历史里打开的、调用方早就离开的、
     * 或者服务端重启后残留的「进行中」，都只能按记录来收口，否则它会永远停在「进行中」。
     */
    public Map<String, Object> runStop(Map<?, ?> param, HttpServletRequest request) {
        int uid = rbacService.uid(request);
        if (uid < 1) return ApiUtil.result(9403, null, null);
        int runId = DPUtil.parseInt(param.get("id"));
        CompareRun run = info(runDao, runId);
        if (null == run || run.getDeletedTime() > 0) return ApiUtil.result(404, "运行记录不存在", runId);
        if (!Objects.equals(run.getCreatedUid(), uid)) return ApiUtil.result(9403, "只能终止自己发起的对比", null);
        int stopped = stop(runId);
        if (stopped > 0) await(runId, 3000L); // 等被断掉的流把各自那行写完
        int finished = settle(run);
        ObjectNode result = DPUtil.objectNode();
        result.put("id", runId);
        result.put("stopped", stopped);
        result.put("finished", finished);
        return ApiUtil.result(0, null, result);
    }

    /**
     * 收口：把这一轮里始终没有回来过的模型补成「中断」明细。
     *
     * 补的是明细而不是状态，是因为每一张卡片都得有自己那一行：
     * 只改状态的话，恢复历史时会少几张卡片，看不出当时缺的是哪一个模型。
     * 补齐依据是建档时的模型快照（key 与模型名都在里面）。
     */
    protected int settle(CompareRun run) {
        ArrayNode models = array(run.getModels());
        Set<String> exists = new HashSet<>();
        for (CompareCall item : callDao.findByRunIdOrderByCreatedTimeAscIdAsc(run.getId())) {
            exists.add(String.valueOf(item.getCallKey()));
        }
        long time = System.currentTimeMillis();
        int count = 0;
        for (JsonNode model : models) {
            String key = model.at("/key").asText("");
            if (exists.contains(key)) continue;
            callDao.save(CompareCall.builder()
                    .runId(run.getId())
                    .callKey(key)
                    .model(model.at("/model").asText(""))
                    .state("abort")
                    .duration(0L)
                    .firstToken(0L)
                    .contentLength(0)
                    .reasoningLength(0)
                    .finishReason("")
                    .error("本轮已终止，未收到输出")
                    .payload("{}")
                    .content("")
                    .reasoning("")
                    .createdTime(time)
                    .createdUid(run.getCreatedUid())
                    .build());
            count++;
        }
        refreshRun(run); // 补齐之后重算一次：进行中 → 中断
        return count;
    }

    /** 删除运行记录：标记删除，只作用于自己的记录 */
    public Map<String, Object> runRemove(List<Integer> ids, HttpServletRequest request) {
        if (null == ids || ids.isEmpty()) return ApiUtil.result(0, null, 0);
        try {
            int uid = rbacService.uid(request);
            List<CompareRun> rows = runDao.findAllById(ids);
            long time = System.currentTimeMillis();
            List<CompareRun> targets = new ArrayList<>();
            for (CompareRun item : rows) {
                if (item.getDeletedTime() > 0) continue;
                if (!Objects.equals(item.getCreatedUid(), uid)) continue;
                item.setDeletedTime(time);
                item.setDeletedUid(uid);
                targets.add(item);
            }
            if (targets.isEmpty()) return ApiUtil.result(1404, "没有可删除的运行记录", ids);
            runDao.saveAll(targets);
            // 明细与反馈是纯派生数据，跟着运行记录一起清掉，不占用「已删除」的空间
            List<Integer> ids2 = new ArrayList<>();
            for (CompareRun item : targets) ids2.add(item.getId());
            Map<String, Object> calls = removeByParentId(callDao, "runId", ids2);
            if (ApiUtil.failed(calls)) return calls;
            Map<String, Object> feedbacks = removeByParentId(feedbackDao, "runId", ids2);
            if (ApiUtil.failed(feedbacks)) return feedbacks;
            return ApiUtil.result(0, null, targets.size());
        } catch (Exception e) {
            return ApiUtil.result(1500, "删除运行记录失败", e.getMessage());
        }
    }

    /**
     * 清空我的全部运行记录。
     * 单独开一个口子是因为列表有分页与检索：页面上拿到的只是当前一页的 id，
     * 拿它去删就成了「只删这一页」。
     */
    public Map<String, Object> runClear(HttpServletRequest request) {
        try {
            int uid = rbacService.uid(request);
            if (uid < 1) return ApiUtil.result(9403, null, null);
            List<CompareRun> rows = runDao.findAll((root, query, cb) -> cb.and(
                    cb.equal(root.get("createdUid"), uid), cb.equal(root.get("deletedTime"), 0L)));
            if (rows.isEmpty()) return ApiUtil.result(0, null, 0);
            long time = System.currentTimeMillis();
            List<Integer> ids = new ArrayList<>();
            for (CompareRun item : rows) {
                item.setDeletedTime(time);
                item.setDeletedUid(uid);
                ids.add(item.getId());
            }
            runDao.saveAll(rows);
            Map<String, Object> calls = removeByParentId(callDao, "runId", ids);
            if (ApiUtil.failed(calls)) return calls;
            Map<String, Object> feedbacks = removeByParentId(feedbackDao, "runId", ids);
            if (ApiUtil.failed(feedbacks)) return feedbacks;
            return ApiUtil.result(0, null, ids.size());
        } catch (Exception e) {
            return ApiUtil.result(1500, "清空运行记录失败", e.getMessage());
        }
    }

    /* ------------------------------- 反馈与统计 ------------------------------- */

    /**
     * 提交反馈：按 callId 定位「某一次模型输出」，再次提交同一情绪表示取消
     * （与对话反馈口径一致）。只能评价自己跑出来的那一轮。
     */
    public Map<String, Object> feedback(Map<?, ?> param, HttpServletRequest request) {
        int uid = rbacService.uid(request);
        if (uid < 1) return ApiUtil.result(9403, null, null);
        int callId = DPUtil.parseInt(param.get("callId"));
        String emotion = DPUtil.trim(DPUtil.parseString(param.get("emotion")));
        if (callId < 1) return ApiUtil.result(1001, "缺少调用标识", null);
        if (!"positive".equals(emotion) && !"negative".equals(emotion)) {
            return ApiUtil.result(1002, "反馈情绪异常", emotion);
        }
        CompareCall call = info(callDao, callId);
        if (null == call) return ApiUtil.result(404, null, callId);
        if (!Objects.equals(call.getCreatedUid(), uid)) {
            return ApiUtil.result(9403, "只能评价自己发起的对比", null);
        }
        CompareFeedback item = feedbackDao.findFirstByCallIdAndCreatedUidAndDeletedTime(callId, uid, 0L);
        if (null != item && emotion.equals(item.getEmotion())) {
            // 同一情绪再点一次 = 取消：直接标记删除，下次再评会新建一条
            item.setDeletedTime(System.currentTimeMillis());
            item.setDeletedUid(uid);
            save(feedbackDao, item, uid);
            return ApiUtil.result(0, null, DPUtil.objectNode().put("callId", callId).put("emotion", ""));
        }
        if (null == item) {
            item = CompareFeedback.builder()
                    .callId(callId)
                    .runId(call.getRunId())
                    .model(call.getModel())
                    .deletedTime(0L)
                    .build();
        }
        item.setEmotion(emotion);
        item.setTag(DPUtil.trim(DPUtil.parseString(param.get("tag"))));
        item.setContent(DPUtil.trim(DPUtil.parseString(param.get("content"))));
        item = save(feedbackDao, item, uid);
        ObjectNode result = DPUtil.objectNode();
        result.put("callId", callId);
        result.put("emotion", item.getEmotion());
        return ApiUtil.result(0, null, result);
    }

    /**
     * 模型统计：调用量按模型与按用户各一张，评价（赞 / 踩 / 合计）来自反馈。
     * 评分 = 赞 / (赞 + 踩) × 100；一条评价都没有时给 -1，页面显示「—」而不是 0 分。
     *
     * 支持时间、模型、用户三个筛选，默认近一周——不传时间就按最近 7 天算，
     * 免得统计页一打开就是全量数据，那个口径没人会看。
     */
    public ObjectNode statistic(Map<String, Object> param) {
        long endTime = DPUtil.parseLong(param.get("endTime"));
        if (endTime < 1) endTime = System.currentTimeMillis();
        long beginTime = DPUtil.parseLong(param.get("beginTime"));
        if (beginTime < 1) beginTime = endTime - 7 * 24 * 60 * 60 * 1000L;
        String model = DPUtil.trim(DPUtil.parseString(param.get("model")));
        int uid = DPUtil.parseInt(param.get("uid"));
        Map<String, CompareFeedbackDao.ModelScore> scores = new LinkedHashMap<>();
        for (CompareFeedbackDao.ModelScore item : feedbackDao.scoreByModel(beginTime, endTime, model, uid)) {
            scores.put(String.valueOf(item.getModel()), item);
        }
        // 变量名与下面模型行的 rated（赞+踩）区分开
        Map<Integer, Long> userRated = new LinkedHashMap<>();
        for (CompareFeedbackDao.UserScore item : feedbackDao.scoreByUser(beginTime, endTime, model, uid)) {
            userRated.put(item.getCreatedUid(), null == item.getRated() ? 0L : item.getRated());
        }
        ObjectNode result = DPUtil.objectNode();
        result.put("beginTime", beginTime);
        result.put("endTime", endTime);
        result.put("model", model);
        result.put("uid", uid);
        ArrayNode models = result.putArray("models");
        for (CompareCallDao.ModelStat item : callDao.statByModel(beginTime, endTime, model, uid)) {
            long total = null == item.getTotal() ? 0L : item.getTotal();
            long finished = null == item.getFinished() ? 0L : item.getFinished();
            ObjectNode row = models.addObject();
            row.put("model", item.getModel());
            row.put("total", total);
            row.put("finished", finished);
            row.put("aborted", null == item.getAborted() ? 0L : item.getAborted());
            row.put("errored", null == item.getErrored() ? 0L : item.getErrored());
            row.put("successRate", total < 1 ? 0 : Math.round(finished * 100d / total));
            row.put("avgDuration", null == item.getAvgDuration() ? 0L : Math.round(item.getAvgDuration()));
            row.put("avgFirstToken", null == item.getAvgFirstToken() ? 0L : Math.round(item.getAvgFirstToken()));
            CompareFeedbackDao.ModelScore score = scores.get(String.valueOf(item.getModel()));
            long positive = null == score || null == score.getPositive() ? 0L : score.getPositive();
            long negative = null == score || null == score.getNegative() ? 0L : score.getNegative();
            long rated = positive + negative;
            row.put("positive", positive);
            row.put("negative", negative);
            row.put("rated", rated);
            row.put("score", rated < 1 ? -1L : Math.round(positive * 100d / rated));
        }
        ArrayNode users = result.putArray("users");
        for (CompareCallDao.UserStat item : callDao.statByUser(beginTime, endTime, model, uid)) {
            ObjectNode row = users.addObject();
            Integer createdUid = null == item.getCreatedUid() ? 0 : item.getCreatedUid();
            row.put("createdUid", createdUid);
            row.put("total", null == item.getTotal() ? 0L : item.getTotal());
            row.put("modelCount", null == item.getModelCount() ? 0L : item.getModelCount());
            row.put("rated", userRated.getOrDefault(createdUid, 0L));
        }
        rbacService.fillUserInfo(users, "createdUid");
        // 「用户 × 模型」的分段数据：用户堆叠柱状图要按用户把每个模型分开画
        ArrayNode userModels = result.putArray("userModels");
        for (CompareCallDao.UserModelStat item : callDao.statByUserModel(beginTime, endTime, model, uid)) {
            ObjectNode row = userModels.addObject();
            row.put("createdUid", null == item.getCreatedUid() ? 0 : item.getCreatedUid());
            row.put("model", item.getModel());
            row.put("total", null == item.getTotal() ? 0L : item.getTotal());
        }
        return result;
    }

    /* ------------------------------- 工具 ------------------------------- */

    protected JsonNode parse(String json) {
        if (DPUtil.empty(json)) return null;
        return DPUtil.parseJSON(json);
    }

    protected ArrayNode array(String json) {
        JsonNode node = parse(json);
        return null != node && node.isArray() ? (ArrayNode) node : DPUtil.arrayNode();
    }

    protected ObjectNode object(String json) {
        JsonNode node = parse(json);
        return null != node && node.isObject() ? (ObjectNode) node : DPUtil.objectNode();
    }

    public ObjectNode error(String message) {
        return DPUtil.objectNode().putObject("error").put("message", message);
    }

    public String message(String action, Object data) {
        ObjectNode message = DPUtil.objectNode();
        message.put("action", action);
        message.replace("data", DPUtil.toJSON(data));
        return message.toString();
    }

    public SsePlainEmitter message(SsePlainEmitter emitter, String action, Object data) {
        return emitter.data(message(action, data));
    }

}
