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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
     */
    public SseEmitter stream(ObjectNode json, HttpServletRequest request, HttpServletResponse response) throws IOException {
        SsePlainEmitter emitter = new SsePlainEmitter(request, response, 0L);
        ObjectNode body = completion(json);
        if (DPUtil.empty(body.at("/model").asText(""))) {
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
                if (isComment(message, isEvent)) { // 心跳包与结束标识原样转发
                    return emitter.message(message, isEvent).isRunning();
                }
                ObjectNode data = data(message, isEvent);
                if (data.has("error")) {
                    message(emitter, "error.message", data);
                    return false;
                }
                if (data.has("choices")) {
                    // 整个 choices 数组原样转发：思考、正文、finish_reason 都在里面，由前端各归各的
                    message(emitter, "choices.message", data.at("/choices"));
                    return emitter.isRunning();
                }
                message(emitter, "error.unknown", data);
                return false;
            }

            @Override
            public void onError(CloseableHttpResponse response, Throwable throwable, boolean isStream) {
                message(emitter, "error.throwable", error(throwable.getMessage())).abort();
            }
        };
        return pool.process(req, emitter);
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
        result.put("modelCount", info.getModelCount());
        result.put("duration", info.getDuration());
        result.put("summary", info.getSummary());
        result.put("createdTime", info.getCreatedTime());
        result.replace("global", object(info.getSettings()));
        result.replace("models", array(info.getModels()));
        result.replace("results", results(info.getId(), uid));
        return result;
    }

    /** 调用明细 → 页面上的结果数组（按 id 升序，与提交时的顺序一致） */
    protected ArrayNode results(Integer runId, int uid) {
        ArrayNode rows = DPUtil.arrayNode();
        for (CompareCall item : callDao.findByRunIdOrderByIdAsc(runId)) {
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
     * 保存一次运行记录：摘要（完成/失败/中断几个）与耗时在写入时算好，
     * 列表接口就不必为了看一眼状态去读几个 longtext。
     */
    public Map<String, Object> runSave(Map<?, ?> param, HttpServletRequest request) {
        int uid = rbacService.uid(request);
        if (uid < 1) return ApiUtil.result(9403, null, null);
        ObjectNode settings = object(DPUtil.stringify(DPUtil.toJSON(param.get("global"))));
        ArrayNode models = array(DPUtil.stringify(DPUtil.toJSON(param.get("models"))));
        ArrayNode results = array(DPUtil.stringify(DPUtil.toJSON(param.get("results"))));
        if (results.isEmpty()) return ApiUtil.result(1001, "运行结果不能为空", null);
        int finish = 0, error = 0, abort = 0;
        long earliest = 0, latest = 0;
        for (JsonNode item : results) {
            String state = item.at("/state").asText("");
            if ("finish".equals(state)) finish++;
            else if ("error".equals(state)) error++;
            else if ("abort".equals(state)) abort++;
            long created = item.at("/createdTime").asLong(0);
            long finished = item.at("/finishedTime").asLong(0);
            if (created > 0 && (earliest < 1 || created < earliest)) earliest = created;
            if (finished > earliest) latest = finished;
        }
        StringBuilder summary = new StringBuilder();
        if (finish > 0) summary.append(finish).append(" 个完成");
        if (error > 0) summary.append(DPUtil.empty(summary.toString()) ? "" : " · ").append(error).append(" 个失败");
        if (abort > 0) summary.append(DPUtil.empty(summary.toString()) ? "" : " · ").append(abort).append(" 个中断");
        String title = DPUtil.trim(DPUtil.parseString(param.get("title")));
        if (DPUtil.empty(title)) title = DPUtil.trim(settings.at("/input").asText(""));
        if (title.length() > 40) title = title.substring(0, 40);
        CompareRun info = CompareRun.builder()
                .title(title)
                .status(error > 0 || abort > 0 ? 2 : 1)
                .modelCount(results.size())
                .duration(earliest > 0 && latest > earliest ? latest - earliest : 0L)
                .summary(summary.toString())
                .settings(settings.toString())
                .models(models.toString())
                .ip(ServletUtil.getRemoteAddr(request))
                .createdTime(System.currentTimeMillis())
                .createdUid(uid)
                .deletedTime(0L)
                .build();
        info = runDao.save(info);
        saveCalls(info.getId(), results, uid);
        return ApiUtil.result(0, null, runInfo(info.getId(), request));
    }

    /**
     * 输出落明细：一个模型一行，正文、思考与参数快照都存这里。
     * 时间在列表里只存「耗时」与「首字耗时」两个增量，避免再存一遍绝对时间戳。
     */
    protected void saveCalls(Integer runId, ArrayNode results, int uid) {
        List<CompareCall> rows = new ArrayList<>();
        for (JsonNode item : results) {
            long created = item.at("/createdTime").asLong(0);
            long finished = item.at("/finishedTime").asLong(0);
            long first = item.at("/firstTokenTime").asLong(0);
            String content = item.at("/content").asText("");
            String reasoning = item.at("/reasoning").asText("");
            rows.add(CompareCall.builder()
                    .runId(runId)
                    .callKey(item.at("/key").asText(""))
                    .model(item.at("/model").asText(""))
                    .state(item.at("/state").asText(""))
                    .duration(created > 0 && finished > created ? finished - created : 0L)
                    .firstToken(first > created ? first - created : 0L)
                    .contentLength(content.length())
                    .reasoningLength(reasoning.length())
                    .finishReason(item.at("/finishReason").asText(""))
                    .error(item.at("/error").asText(""))
                    .payload(DPUtil.stringify(item.at("/payload")))
                    .content(content)
                    .reasoning(reasoning)
                    .createdTime(created > 0 ? created : System.currentTimeMillis())
                    .createdUid(uid)
                    .build());
        }
        callDao.saveAll(rows);
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
