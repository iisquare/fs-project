package com.iisquare.fs.web.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.agent.dao.AgenticChatDao;
import com.iisquare.fs.web.agent.dao.AgenticDao;
import com.iisquare.fs.web.agent.dao.AgenticDialogDao;
import com.iisquare.fs.web.agent.dao.AgenticLogDao;
import com.iisquare.fs.web.agent.entity.Agentic;
import com.iisquare.fs.web.agent.entity.AgenticChat;
import com.iisquare.fs.web.agent.entity.AgenticDialog;
import com.iisquare.fs.web.agent.entity.AgenticLog;
import com.iisquare.fs.web.agent.entity.Tool;
import com.iisquare.fs.web.agent.entity.ToolMethod;
import com.iisquare.fs.web.agent.mapper.AgenticStatisticMapper;
import com.iisquare.fs.web.agent.mvc.Configuration;
import com.iisquare.fs.web.agent.runner.ChartNodeHandler;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.core.rpc.FileRpc;
import com.iisquare.fs.base.web.sse.SsePlainEmitter;
import com.iisquare.fs.base.web.util.RpcUtil;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Consumer;

/**
 * 智能体编排服务。
 *
 * 编排内容分两条线：
 * - 草稿：保存（save）后的 content，仅用于设计器调试运行（run）
 * - 发布：发布（publish）把当时的草稿固化为 publishedContent 与发布版本，外部调用（invoke）只读该内容
 */
@Service
public class AgenticService extends JPAServiceBase {

    private static final Logger logger = LoggerFactory.getLogger(AgenticService.class);

    public static final String MODE_WORKFLOW = "workflow";
    public static final String MODE_CHAT = "chat";

    @Autowired
    AgenticDao agenticDao;
    @Autowired
    DefaultRbacService rbacService;
    @Autowired
    Configuration configuration;
    @Autowired
    ToolService toolService;
    @Autowired
    ToolMethodService toolMethodService;
    @Autowired
    AgenticRunner agenticRunner;
    @Autowired
    AgenticLogDao agenticLogDao;
    @Autowired
    AgenticChatDao agenticChatDao;
    @Autowired
    AgenticDialogDao agenticDialogDao;
    @Autowired
    AgenticStatisticMapper agenticStatisticMapper;
    @Autowired
    FileRpc fileRpc;
    @Autowired
    KnowledgeService knowledgeService;

    /** 编排调试上传的文件桶：与知识库共用文件服务存储，路径按 agentic 前缀隔离 */
    public static final String BUCKET = "fs-lm-knowledge";

    /** 编排工具（子编排）允许的最大嵌套层数：防止 A→B→A 这类相互调用无限嵌套 */
    private static final int MAX_NESTED_DEPTH = 5;
    /**
     * 当前线程上子编排的嵌套层数：编排工具在同一线程内递归执行，
     * 用线程局部变量计数，超限直接报错而不是把线程/栈耗尽
     */
    private final ThreadLocal<Integer> nestedDepthThread = ThreadLocal.withInitial(() -> 0);

    /**
     * 调试运行的文件上传：走文件服务存储，返回文件标识、原始名称、类型、后缀与大小，
     * 直接对应开始节点 files 数组的元素结构（文件存储服务返回的文件信息）
     */
    public Map<String, Object> upload(MultipartFile file, HttpServletRequest request) {
        if (null == file || file.isEmpty()) return ApiUtil.result(1001, "获取文件句柄失败", null);
        String filename = DPUtil.parseString(file.getOriginalFilename());
        int at = filename.lastIndexOf('.');
        String suffix = at < 0 ? "" : filename.substring(at + 1);
        String date = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String filepath = String.format("agentic/%s/%s%s", date,
                UUID.randomUUID().toString().replace("-", ""), DPUtil.empty(suffix) ? "" : "." + suffix);
        Map<String, Object> result = RpcUtil.result(fileRpc.form("/file/upload", DPUtil.buildMap(
                "bucket", BUCKET, "filepath", filepath, "traceIdentity", "fs-agent-agentic-debug"), file));
        if (ApiUtil.failed(result)) return result;
        JsonNode data = ApiUtil.data(result, ObjectNode.class);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", data.at("/id").asText(""));
        item.put("name", data.at("/name").asText(filename));
        item.put("type", data.at("/type").asText(""));
        item.put("suffix", data.at("/suffix").asText(suffix));
        item.put("size", data.at("/size").asLong(0));
        item.put("filepath", filepath);
        return ApiUtil.result(0, null, item);
    }

    @Override
    public Map<String, String> sorts() {
        Map<String, String> sorts = new LinkedHashMap<>();
        sorts.put("id", "desc");
        sorts.put("status", "asc");
        sorts.put("sort", "desc");
        return sorts;
    }

    public Map<Integer, String> status() {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "启用");
        status.put(2, "禁用");
        return status;
    }

    public Map<String, String> modes() {
        Map<String, String> modes = new LinkedHashMap<>();
        modes.put(MODE_WORKFLOW, "工作流");
        modes.put(MODE_CHAT, "对话流");
        return modes;
    }

    public Agentic info(Integer id) {
        return info(agenticDao, id);
    }

    /** 调用人的角色标识集合（授权角色校验用） */
    /**
     * 删除状态筛选（与前端 form-deleted 组件一致）：`only` 只看已删除、`without` 只看未删除、其余为全部
     */
    protected void addDeleted(List<jakarta.persistence.criteria.Predicate> predicates,
                              jakarta.persistence.criteria.Root<?> root, jakarta.persistence.criteria.CriteriaBuilder cb,
                              Map<?, ?> param) {
        String value = DPUtil.parseString(param.get("deleted"));
        if ("only".equals(value)) {
            predicates.add(cb.greaterThan(root.get("deletedTime"), 0L));
        } else if ("without".equals(value)) {
            predicates.add(cb.equal(root.get("deletedTime"), 0L));
        }
    }

    protected Set<Integer> roleIds(HttpServletRequest request) {
        JsonNode identity = rbacService.identity(request);
        return DPUtil.values(identity.at("/roles"), Integer.class, "id");
    }

    /**
     * 编排应用的授权角色校验：没有配置授权角色时所有登录用户可用；
     * 配置后要求调用人的角色与之有交集，否则拒绝运行/调用
     */
    public boolean authorized(Agentic info, HttpServletRequest request) {
        if (null == info) return false;
        Set<Integer> allowed = new LinkedHashSet<>(DPUtil.parseIntList(info.getRoleIds()));
        if (allowed.isEmpty()) return true;
        return !Collections.disjoint(allowed, roleIds(request));
    }

    /**
     * 发布内容里开始节点的输入配置：对话页新建会话时按它渲染参数表单。
     * 取的是发布内容（外部调用实际执行的那一份），草稿怎么改都不影响线上对话的参数口径。
     */
    protected ObjectNode publishedStart(Agentic info) {
        JsonNode content = DPUtil.parseJSON(DPUtil.parseString(info.getPublishedContent()));
        if (null == content || !content.isObject()) return DPUtil.objectNode();
        for (JsonNode cell : content.at("/cells")) {
            if (!"Start".equals(cell.at("/data/type").asText(""))) continue;
            JsonNode data = cell.at("/data");
            return data.isObject() ? data.deepCopy() : DPUtil.objectNode();
        }
        return DPUtil.objectNode();
    }

    /**
     * 用户对话页可用的编排应用：已发布、状态启用、且授权角色命中当前用户；
     * 只返回对话所需的基础信息，不含画布内容
     */
    public Map<String, Object> authorized(HttpServletRequest request) {
        Set<Integer> mine = roleIds(request);
        List<Agentic> rows = agenticDao.findAll((root, query, cb) -> cb.and(
                cb.equal(root.get("status"), 1),
                cb.greaterThan(root.get("publishedVersion"), 0)), Sort.by(Sort.Order.desc("sort"), Sort.Order.desc("id")));
        ArrayNode items = DPUtil.arrayNode();
        for (Agentic info : rows) {
            Set<Integer> allowed = new LinkedHashSet<>(DPUtil.parseIntList(info.getRoleIds()));
            if (!allowed.isEmpty() && Collections.disjoint(allowed, mine)) continue;
            ObjectNode item = items.addObject();
            item.put("id", info.getId());
            item.put("name", DPUtil.parseString(info.getName()));
            item.put("mode", DPUtil.parseString(info.getMode()));
            item.put("modeText", modes().get(info.getMode()));
            item.put("icon", DPUtil.parseString(info.getIcon()));
            // 对话页参数表单：给出「发布内容」里的开始节点输入配置（含自定义参数），
            // 前端不必再拉草稿，避免按草稿渲染参数、与线上实际执行的口径不一致
            item.set("start", publishedStart(info));
            item.put("description", DPUtil.parseString(info.getDescription()));
            item.set("tags", parseArray(info.getTags()));
            item.put("publishedVersion", null == info.getPublishedVersion() ? 0 : info.getPublishedVersion());
            item.set("roleIds", DPUtil.toJSON(DPUtil.parseIntList(info.getRoleIds())));
        }
        ObjectNode result = DPUtil.objectNode();
        result.set("rows", items);
        return ApiUtil.result(0, null, result);
    }

    /**
     * 编排详情：设计器使用，返回草稿内容与发布状态，不返回发布内容
     */
    public ObjectNode detail(Integer id) {
        Agentic info = info(id);
        if (null == info) return null;
        ObjectNode node = DPUtil.objectNode();
        node.put("id", info.getId());
        node.put("name", info.getName());
        node.put("mode", info.getMode());
        node.put("modeText", modes().get(info.getMode()));
        node.put("icon", info.getIcon());
        node.set("tags", parseArray(info.getTags()));
        // 授权角色：以数组返回，供设计器里的角色多选回显
        node.set("roleIds", DPUtil.toJSON(DPUtil.parseIntList(info.getRoleIds())));
        node.put("status", info.getStatus());
        node.put("statusText", status().get(info.getStatus()));
        node.set("content", parseObject(info.getContent()));
        node.put("publishedVersion", null == info.getPublishedVersion() ? 0 : info.getPublishedVersion());
        node.put("publishedTime", null == info.getPublishedTime() ? 0L : info.getPublishedTime());
        node.put("publishedUid", null == info.getPublishedUid() ? 0 : info.getPublishedUid());
        node.put("publishText", publishText(info));
        node.put("sort", info.getSort());
        node.put("description", info.getDescription());
        node.put("createdTime", null == info.getCreatedTime() ? 0L : info.getCreatedTime());
        node.put("createdUid", null == info.getCreatedUid() ? 0 : info.getCreatedUid());
        node.put("updatedTime", null == info.getUpdatedTime() ? 0L : info.getUpdatedTime());
        node.put("updatedUid", null == info.getUpdatedUid() ? 0 : info.getUpdatedUid());
        ArrayNode rows = DPUtil.arrayNode();
        rows.add(node); // fillUserInfo 会就地填充，传入同一节点引用即可
        rbacService.fillUserInfo(rows, "createdUid", "updatedUid", "publishedUid");
        return node;
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if (DPUtil.empty(name)) return ApiUtil.result(1001, "编排名称异常", name);
        String mode = DPUtil.trim(DPUtil.parseString(param.get("mode")));
        if (!modes().containsKey(mode)) return ApiUtil.result(1002, "应用类型异常", mode);
        int status = DPUtil.parseInt(param.get("status"));
        if (!status().containsKey(status)) return ApiUtil.result(1003, "状态异常", status);
        Agentic info;
        if (id > 0) {
            if (!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if (null == info) return ApiUtil.result(404, null, id);
        } else {
            if (!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new Agentic();
            // 新增时显式赋初值：@DynamicInsert 会省略 null 字段，text/longtext 列在 MySQL 中没有默认值
            info.setTags("[]");
            info.setRoleIds("");
            info.setContent("");
            info.setDescription("");
            info.setPublishedContent(""); // 未发布
            info.setPublishedVersion(0);
            info.setPublishedTime(0L);
            info.setPublishedUid(0);
        }
        info.setName(name);
        info.setMode(mode);
        info.setIcon(DPUtil.parseString(param.get("icon")));
        if (null != param.get("tags")) info.setTags(DPUtil.stringify(parseArray(param.get("tags"))));
        // 授权角色：为空表示所有登录用户可用（逗号分隔存储，与知识库等模块保持一致）
        if (null != param.get("roleIds")) info.setRoleIds(DPUtil.implode(",", DPUtil.parseIntList(param.get("roleIds"))));
        // 保存的内容为草稿，仅用于调试运行；外部调用使用发布内容
        if (null != param.get("content")) info.setContent(DPUtil.stringify(param.get("content")));
        info.setSort(DPUtil.parseInt(param.get("sort")));
        info.setStatus(status);
        info.setDescription(DPUtil.parseString(param.get("description")));
        info = save(agenticDao, info, rbacService.uid(request));
        return ApiUtil.result(0, null, detail(info.getId()));
    }

    /**
     * 发布：把当前草稿固化为对外提供的发布内容，版本号递增
     */
    public Map<String, Object> publish(Map<?, ?> param, HttpServletRequest request) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        if (id < 1) return ApiUtil.result(1001, "编排标识异常", id);
        if (!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
        Agentic info = info(id);
        if (null == info) return ApiUtil.result(404, null, id);
        if (DPUtil.empty(info.getContent())) return ApiUtil.result(1002, "编排内容为空，无法发布", id);
        JsonNode content = DPUtil.parseJSON(DPUtil.parseString(info.getContent()));
        if (null == content || !content.isObject()) return ApiUtil.result(1002, "编排内容无效，无法发布", id);
        // 工具引用校验 + 方法定义快照：发布内容自洽，工具后续变动不影响已发布编排
        List<String> errors = new ArrayList<>();
        JsonNode published = content.deepCopy();
        bindToolMethods(published, errors);
        if (!errors.isEmpty()) return ApiUtil.result(1003, DPUtil.implode("；", errors), errors);
        info.setPublishedContent(DPUtil.stringify(published));
        info.setPublishedVersion((null == info.getPublishedVersion() ? 0 : info.getPublishedVersion()) + 1);
        info.setPublishedTime(System.currentTimeMillis());
        info.setPublishedUid(rbacService.uid(request));
        save(agenticDao, info, rbacService.uid(request));
        return ApiUtil.result(0, null, detail(id));
    }

    /**
     * 发布时的工具方法处理：LLM 节点引用的工具方法需存在、未失效、未停用，执行变量绑定要有取值；
     * 校验通过后把方法定义（名称 / 描述 / 参数 / 调用信息）快照进发布内容，供运行时直接使用
     */
    protected void bindToolMethods(JsonNode content, List<String> errors) {
        for (JsonNode cell : content.at("/cells")) {
            JsonNode data = cell.at("/data");
            if (!"LLM".equals(data.at("/type").asText())) continue;
            // 策略为「无」时不携带工具，配置的工具引用不参与运行，也无需校验
            if ("none".equalsIgnoreCase(data.at("/agentStrategy").asText("none"))) continue;
            for (JsonNode row : data.at("/tools")) {
                if (row.at("/enabled").isBoolean() && !row.at("/enabled").asBoolean()) continue;
                // 内置工具：知识库与编排应用同样按 function calling 暴露给模型
                String toolKind = row.at("/kind").asText("method");
                if ("knowledge".equals(toolKind) || "agentic".equals(toolKind)
                        || "theme".equals(toolKind) || "ontology".equals(toolKind)) {
                    boolean knowledge = "knowledge".equals(toolKind);
                    boolean theme = "theme".equals(toolKind);
                    boolean ontology = "ontology".equals(toolKind);
                    String label = knowledge ? "知识库" : (theme ? "数据主题" : (ontology ? "本体" : "编排应用"));
                    if (DPUtil.empty(row.at("/name").asText(""))) {
                        errors.add("存在未配置函数名的" + label + "工具");
                        continue;
                    }
                    if (knowledge) {
                        int knowledgeId = row.at("/knowledgeId").asInt(0);
                        if (knowledgeId < 1) errors.add("存在未选择知识库的工具");
                        else if (null == knowledgeService.info(knowledgeId)) errors.add("知识库不存在（ID：" + knowledgeId + "）");
                    } else if (theme) {
                        if (row.at("/themeId").asInt(0) < 1) errors.add("存在未选择数据主题的工具");
                    } else if (ontology) {
                        if (row.at("/ontologyId").asInt(0) < 1) errors.add("存在未选择本体的工具");
                    } else {
                        int agenticId = row.at("/agenticId").asInt(0);
                        Agentic target = agenticId < 1 ? null : info(agenticId);
                        if (agenticId < 1) errors.add("存在未选择编排应用的工具");
                        else if (null == target) errors.add("编排应用不存在（ID：" + agenticId + "）");
                        else if (null == target.getPublishedVersion() || target.getPublishedVersion() < 1) {
                            errors.add("编排工具引用的应用尚未发布：" + target.getName());
                        }
                    }
                    continue;
                }
                int toolId = row.at("/toolId").asInt(0);
                String methodName = row.at("/method").asText("");
                if (toolId < 1 || DPUtil.empty(methodName)) {
                    errors.add("存在未选择完整的工具引用，请重新选择工具与方法");
                    continue;
                }
                Tool tool = toolService.info(toolId);
                if (null == tool) {
                    errors.add("工具不存在（ID：" + toolId + "）");
                    continue;
                }
                if (1 != (null == tool.getStatus() ? 0 : tool.getStatus())) {
                    errors.add("工具已停用：" + tool.getName());
                    continue;
                }
                ToolMethod method = null;
                for (ToolMethod item : toolMethodService.all(toolId)) {
                    if (methodName.equals(item.getName())) {
                        method = item;
                        break;
                    }
                }
                String label = tool.getName() + "." + methodName;
                if (null == method) {
                    errors.add("方法不存在：" + label);
                    continue;
                }
                if (1 != (null == method.getPresent() ? 1 : method.getPresent())) {
                    errors.add("方法已失效：" + label);
                    continue;
                }
                if (1 != (null == method.getStatus() ? 1 : method.getStatus())) {
                    errors.add("方法已停用：" + label);
                    continue;
                }
                // 执行变量绑定：来源为引用变量或固定值时必须有取值
                JsonNode args = row.at("/args");
                for (Map<String, Object> parameter : toolMethodService.methodParams(method)) {
                    String name = DPUtil.parseString(parameter.get("name"));
                    Map<String, Object> properties = schemaProperties(parameter);
                    if (!properties.isEmpty()) {
                        // 对象参数（如 body）按字段绑定：未配置的字段依旧由模型决定
                        JsonNode fields = args.at("/" + name).at("/fields");
                        for (String field : properties.keySet()) {
                            validateBinding(label + "." + name + "." + field, fields.at("/" + field), errors);
                        }
                        continue;
                    }
                    validateBinding(label + "." + name, args.at("/" + name), errors);
                }
                ((ObjectNode) row).put("methodTitle", DPUtil.parseString(method.getTitle()));
                ((ObjectNode) row).put("methodOriginName", DPUtil.parseString(method.getOriginName()));
                ((ObjectNode) row).put("methodDescription", DPUtil.parseString(method.getDescription()));
                ((ObjectNode) row).set("methodParams", DPUtil.toJSON(toolMethodService.methodParams(method)));
                ((ObjectNode) row).set("methodInvoke", DPUtil.toJSON(toolMethodService.methodInvoke(method)));
                // 工具级调用配置一并快照：运行时按快照发起调用，不受工具后续改动影响
                ((ObjectNode) row).put("toolType", DPUtil.parseString(tool.getType()));
                ((ObjectNode) row).put("toolUrl", DPUtil.parseString(tool.getUrl()));
                ((ObjectNode) row).put("toolHeader", DPUtil.parseString(tool.getHeader()));
                ((ObjectNode) row).put("toolQuery", DPUtil.parseString(tool.getQuery()));
                ((ObjectNode) row).put("toolContent", DPUtil.parseString(tool.getContent()));
            }
        }
    }

    /** 参数的结构化字段：对象参数（schema.properties）按字段绑定执行变量 */
    protected Map<String, Object> schemaProperties(Map<String, Object> parameter) {
        Object schema = parameter.get("schema");
        if (!(schema instanceof Map)) return new LinkedHashMap<>();
        Object properties = ((Map<?, ?>) schema).get("properties");
        if (!(properties instanceof Map)) return new LinkedHashMap<>();
        Map<String, Object> result = DPUtil.toJSON(properties, Map.class);
        return null == result ? new LinkedHashMap<>() : result;
    }

    /**
     * 执行变量绑定校验：`auto=false` 时必须填写内容（内容里可以混排固定字符串与变量占位符，由运行时解析）；
     * 兼容早期数据里的 `source` 写法（model / variable / constant）
     */
    protected void validateBinding(String label, JsonNode binding, List<String> errors) {
        if (binding.has("auto")) {
            if (!binding.at("/auto").asBoolean(true) && DPUtil.empty(binding.at("/value").asText(""))) {
                errors.add("执行变量未填写内容：" + label);
            }
            return;
        }
        String source = binding.at("/source").asText("model");
        if ("variable".equals(source) && DPUtil.empty(binding.at("/variable").asText(""))) {
            errors.add("执行变量未选择变量：" + label);
        } else if ("constant".equals(source) && DPUtil.empty(binding.at("/value").asText(""))) {
            errors.add("执行变量未填写固定值：" + label);
        }
    }

    /**
     * 调试运行：执行草稿内容，返回运行结果（回复内容、各节点输出、执行步骤）并记录运行日志
     */
    /**
     * 运行前准备：注入「编排工具」的调用器（把另一个编排应用当工具调用），
     * 用回调注入而不是让运行时反向依赖本服务
     */
    protected void prepareRuntime(Integer uid, HttpServletRequest request) {
        agenticRunner.agenticInvoker((agenticId, query) -> toolInvoke(agenticId, query, uid, request));
    }

    /**
     * 编排作为工具：以 query 作为开始节点入参调用另一个编排应用的发布内容。
     * 子编排不写会话与运行日志，避免污染调用方的对话记录。
     */
    public ObjectNode toolInvoke(Integer agenticId, String query, Integer uid, HttpServletRequest request) {
        Agentic info = null == agenticId || agenticId < 1 ? null : info(agenticId);
        if (null == info) throw new IllegalStateException("编排应用不存在：" + agenticId);
        // 嵌套层级保护：A 的工具指向 B、B 又指回 A（或自引用）时会无限嵌套，这里直接拦住
        int depth = nestedDepthThread.get();
        if (depth >= MAX_NESTED_DEPTH) {
            throw new IllegalStateException("编排嵌套调用层级过深（上限 " + MAX_NESTED_DEPTH
                    + " 层）：请检查是否存在相互调用的编排应用，当前为「" + info.getName() + "」");
        }
        int version = null == info.getPublishedVersion() ? 0 : info.getPublishedVersion();
        if (version < 1 || DPUtil.empty(info.getPublishedContent())) {
            throw new IllegalStateException("编排应用尚未发布：" + info.getName());
        }
        JsonNode content = DPUtil.parseJSON(DPUtil.parseString(info.getPublishedContent()));
        if (null == content || !content.isObject()) {
            throw new IllegalStateException("编排应用发布内容无效：" + info.getName());
        }
        ObjectNode inputs = DPUtil.objectNode();
        inputs.put("query", DPUtil.parseString(query));
        // 子编排同样要能继续使用「编排工具」（嵌套调用），并临时关闭流式输出：
        // 子编排的输出作为工具结果返回给外层模型，不应混进外层对话的气泡
        prepareRuntime(uid, request);
        Consumer<JsonNode> sink = agenticRunner.streamSink();
        // 子编排的节点状态同样不推给外层画布（外层只展示自己这一层的执行过程）
        Consumer<JsonNode> stepSink = agenticRunner.stepSink();
        // 子编排的 ReAct 轮次同样不推给外层（否则外层的执行过程里会混进子流程的轮次）
        Consumer<JsonNode> roundSink = agenticRunner.roundSink();
        agenticRunner.streamSink(null);
        agenticRunner.stepSink(null);
        agenticRunner.roundSink(null);
        // 真正开始执行子编排时才计数，避免上面几处提前抛错把层级计数留在线程上
        nestedDepthThread.set(depth + 1);
        ObjectNode result;
        try {
            result = agenticRunner.execute(content, inputs, DPUtil.arrayNode(), system(info, null, uid, request));
        } finally {
            nestedDepthThread.set(depth);
            agenticRunner.streamSink(sink);
            agenticRunner.stepSink(stepSink);
            agenticRunner.roundSink(roundSink);
        }
        ObjectNode value = DPUtil.objectNode();
        value.put("answer", result.at("/answer").asText(""));
        value.put("status", result.at("/status").asInt(1));
        value.put("error", result.at("/error").asText(""));
        return value;
    }

    /**
     * 流式调试运行：模型节点的增量内容实时推送（SSE），结束后推送运行结果。
     * 事件格式：`{ type: delta|step|done|error, data: ... }`
     */
    public SseEmitter runStream(Map<?, ?> param, HttpServletRequest request, HttpServletResponse response) {
        return stream(param, request, response, false);
    }

    /**
     * 流式外部调用：与调试运行同一套事件，只是执行的是发布内容（用户对话页使用）
     */
    public SseEmitter invokeStream(Map<?, ?> param, HttpServletRequest request, HttpServletResponse response) {
        return stream(param, request, response, true);
    }

    /** 流式运行（SSE）：published=false 走草稿调试运行，true 走发布内容的外部调用 */
    protected SseEmitter stream(Map<?, ?> param, HttpServletRequest request, HttpServletResponse response,
                                boolean published) {
        // 异步线程内沿用请求上下文：FeignInterceptor 从「当前请求」透传 x-auth-token 等身份头，
        // BI 侧（数据主题 / 数据集查询）据此替换与登录用户相关的变量
        ServletRequestAttributes attributes = new ServletRequestAttributes(request, response);
        SsePlainEmitter emitter = new SsePlainEmitter(request, response, 0L);
        return emitter.async(() -> {
            RequestContextHolder.setRequestAttributes(attributes);
            // 注意：流式回调必须在执行线程上设置（运行时按线程保存），否则增量推不出去
            agenticRunner.streamSink(chunk -> emitter.data(DPUtil.stringify(streamMessage("delta", chunk))));
            // 节点执行进度同样实时推送：前端把流程图上的节点与连线按执行状态着色
            agenticRunner.stepSink(step -> emitter.data(DPUtil.stringify(streamMessage("step", step))));
            // ReAct 轮次进度：每轮模型推理与每次工具调用实时推送，前端据此展示执行过程
            agenticRunner.roundSink(round -> emitter.data(DPUtil.stringify(streamMessage("round", round))));
            try {
                Map<String, Object> data = published ? invoke(param, request) : run(param, request);
                // 业务失败（授权被撤销、缺少必填参数、未发布等）在 run/invoke 里是正常返回的错误码，
                // 流式通道必须按错误事件下发，否则前端会当成运行成功、只留一个空的回复气泡
                if (ApiUtil.failed(data)) {
                    ObjectNode error = DPUtil.objectNode();
                    error.put("code", DPUtil.parseInt(data.get(ApiUtil.FIELD_CODE)));
                    error.put("message", DPUtil.parseString(data.get(ApiUtil.FIELD_MSG)));
                    emitter.data(DPUtil.stringify(streamMessage("error", error)));
                } else {
                    emitter.data(DPUtil.stringify(streamMessage("done", data)));
                }
            } catch (Exception e) {
                ObjectNode error = DPUtil.objectNode();
                error.put("code", 500);
                error.put("message", DPUtil.parseString(e.getMessage()));
                emitter.data(DPUtil.stringify(streamMessage("error", error)));
            } finally {
                agenticRunner.streamSink(null);
                agenticRunner.stepSink(null);
                agenticRunner.roundSink(null);
                RequestContextHolder.resetRequestAttributes();
            }
        });
    }

    /** 流式事件体：type + data，前端按 type 分发 */
    protected ObjectNode streamMessage(String type, Object data) {
        ObjectNode message = DPUtil.objectNode();
        message.put("type", type);
        message.set("data", DPUtil.toJSON(data));
        return message;
    }

    public Map<String, Object> run(Map<?, ?> param, HttpServletRequest request) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        Agentic info = id > 0 ? info(id) : null;
        if (null == info) return ApiUtil.result(404, null, id);
        // 授权角色：配置了授权角色时按调用人的角色放行
        if (!authorized(info, request)) return ApiUtil.result(9403, "无权限使用该编排应用：" + info.getName(), id);
        JsonNode content = DPUtil.parseJSON(DPUtil.parseString(info.getContent()));
        if (null == content || !content.isObject()) return ApiUtil.result(1001, "编排内容为空，请先保存", id);
        ObjectNode inputs = parseObject(param.get("inputs"));
        Integer uid = rbacService.uid(request);
        // 多轮对话：调试运行与发布应用的会话分开存（draft / published），且只能续写自己的会话
        String query = query(inputs);
        Integer existsId = ownedChatId(DPUtil.parseInt(param.get("chatId")), info, CHAT_DRAFT, uid);
        // 仅新建会话时校验必填参数：继续对话的入参已随会话带入，不再重复要求
        if (0 == existsId) {
            String absent = missingInputs(content, inputs);
            if (!DPUtil.empty(absent)) return ApiUtil.result(1004, "缺少必填参数：" + absent, absent);
        }
        AgenticChat chat = resolveChat(existsId, info, CHAT_DRAFT, query, uid);
        // 分支位置：未传 parentId 接在会话当前分支尾，显式传 0 从会话起点新起分支；
        // reuseQuestion 表示重新生成，不重复落用户消息
        Integer parentId = parentId(param);
        Integer branch = branchOf(chat, parentId);
        // 显式从会话起点新起分支（编辑第一条提问重新发送）：本轮模型上下文为空，不能退回全部消息
        boolean branchStart = null != parentId && parentId <= 0;
        boolean reuseQuestion = DPUtil.parseBoolean(param.get("reuseQuestion"));
        prepareRuntime(uid, request);
        long begin = System.currentTimeMillis();
        ObjectNode result;
        try {
            result = agenticRunner.execute(content, inputs, history(chat.getId(), branch, branchStart), system(info, chat, uid, request));
        } catch (Exception e) {
            return failureResult(info, chat.getId(), "draft", 0, inputs, e, System.currentTimeMillis() - begin, uid, request);
        }
        result.put("id", info.getId());
        result.put("name", info.getName());
        result.put("mode", info.getMode());
        result.put("source", "draft"); // 调试运行使用保存后的草稿内容
        result.set("inputs", inputs);
        result.put("chatId", chat.getId());
        // 消息落库与标识回传：前端按 answerId 对本轮回复做反馈，按 questionId/leafId 续写或新起分支
        appendTurn(chat, branch, reuseQuestion, result, query, inputs, uid);
        AgenticLog log = writeLog(info, chat.getId(), "draft", 0, inputs, result, uid, request);
        result.put("logId", null == log ? 0 : log.getId());
        return ApiUtil.result(0, null, result);
    }

    /**
     * 外部调用：只读取已发布内容，未发布或发布内容为空时拒绝调用
     */
    public Map<String, Object> invoke(Map<?, ?> param, HttpServletRequest request) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        Agentic info = id > 0 ? info(id) : null;
        if (null == info) return ApiUtil.result(404, null, id);
        int version = null == info.getPublishedVersion() ? 0 : info.getPublishedVersion();
        if (version < 1 || DPUtil.empty(info.getPublishedContent())) {
            return ApiUtil.result(1002, "编排尚未发布，外部调用不可用", id);
        }
        // 授权角色：配置了授权角色时按调用人的角色放行
        if (!authorized(info, request)) return ApiUtil.result(9403, "无权限使用该编排应用：" + info.getName(), id);
        JsonNode content = DPUtil.parseJSON(DPUtil.parseString(info.getPublishedContent()));
        if (null == content || !content.isObject()) return ApiUtil.result(1002, "发布内容无效，请重新发布", id);
        ObjectNode inputs = parseObject(param.get("inputs"));
        Integer uid = rbacService.uid(request);
        String query = query(inputs);
        Integer existsId = ownedChatId(DPUtil.parseInt(param.get("chatId")), info, CHAT_PUBLISHED, uid);
        // 仅新建会话时校验必填参数：继续对话的入参已随会话带入，不再重复要求
        if (0 == existsId) {
            String absent = missingInputs(content, inputs);
            if (!DPUtil.empty(absent)) return ApiUtil.result(1004, "缺少必填参数：" + absent, absent);
        }
        AgenticChat chat = resolveChat(existsId, info, CHAT_PUBLISHED, query, uid);
        // 分支位置：未传 parentId 接在会话当前分支尾，显式传 0 从会话起点新起分支；
        // reuseQuestion 表示重新生成，不重复落用户消息
        Integer parentId = parentId(param);
        Integer branch = branchOf(chat, parentId);
        // 显式从会话起点新起分支（编辑第一条提问重新发送）：本轮模型上下文为空，不能退回全部消息
        boolean branchStart = null != parentId && parentId <= 0;
        boolean reuseQuestion = DPUtil.parseBoolean(param.get("reuseQuestion"));
        prepareRuntime(uid, request);
        long begin = System.currentTimeMillis();
        ObjectNode result;
        try {
            result = agenticRunner.execute(content, inputs, history(chat.getId(), branch, branchStart), system(info, chat, uid, request));
        } catch (Exception e) {
            return failureResult(info, chat.getId(), "published", version, inputs, e, System.currentTimeMillis() - begin, uid, request);
        }
        result.put("id", info.getId());
        result.put("name", info.getName());
        result.put("mode", info.getMode());
        result.put("version", version);
        result.put("publishedTime", null == info.getPublishedTime() ? 0L : info.getPublishedTime());
        result.put("source", "published");
        result.set("inputs", inputs);
        result.put("chatId", chat.getId());
        // 消息落库与标识回传：前端按 answerId 对本轮回复做反馈，按 questionId/leafId 续写或新起分支
        appendTurn(chat, branch, reuseQuestion, result, query, inputs, uid);
        AgenticLog log = writeLog(info, chat.getId(), "published", version, inputs, result, uid, request);
        result.put("logId", null == log ? 0 : log.getId());
        return ApiUtil.result(0, null, result);
    }

    /* ------------------------------- 会话与历史 ------------------------------- */

    /** 会话类型：调试运行与发布应用分开，避免调试记录混进线上会话（与运行日志的 source 取值一致） */
    public static final String CHAT_DRAFT = "draft";
    public static final String CHAT_PUBLISHED = "published";

    /**
     * 入参校验：开始节点里声明为必填的自定义参数是否都有取值（与设计器调试表单的必填规则一致），
     * 返回缺失参数的展示名称（以、分隔），没有缺失时返回空串
     */
    protected String missingInputs(JsonNode content, ObjectNode inputs) {
        JsonNode start = null;
        for (JsonNode cell : content.at("/cells")) {
            if ("Start".equals(cell.at("/data/type").asText(""))) {
                start = cell.at("/data");
                break;
            }
        }
        if (null == start) return "缺少开始节点";
        List<String> missing = new ArrayList<>();
        for (JsonNode item : start.at("/variables")) {
            String name = item.at("/name").asText("");
            if (DPUtil.empty(name) || !item.at("/required").asBoolean(false)) continue;
            if (blank(inputs.get(name))) {
                missing.add(DPUtil.empty(item.at("/label").asText("")) ? name : item.at("/label").asText(""));
            }
        }
        return DPUtil.implode("、", missing);
    }

    /**
     * 取值是否为空：JsonNode 的 isEmpty() 对数值、布尔等标量节点恒为 true，
     * 直接用 DPUtil.empty 会把 0、false 这类合法取值判成「未填写」，因此单独判断。
     */
    protected boolean blank(JsonNode value) {
        if (null == value || value.isNull()) return true;
        if (value.isTextual()) return DPUtil.empty(value.asText(""));
        if (value.isArray() || value.isObject()) return value.isEmpty();
        return false;
    }

    /**
     * 系统变量：节点里 `{{#sys.xxx#}}` 取这些值——应用标识、会话标识、调用人、当前时间与日期。
     * 用户输入与用户文件属于开始节点自身，直接引用开始节点的输出，不在这里重复。
     * 时间按东八区格式化，一次运行内所有节点取到同一时刻：
     * datetime 为「yyyy-MM-dd HH:mm:ss」，date 为「yyyy-MM-dd」。
     */
    protected Map<String, Object> system(Agentic info, AgenticChat chat, Integer uid, HttpServletRequest request) {
        JsonNode user = currentUser(request);
        Map<String, Object> system = new LinkedHashMap<>();
        system.put("appId", String.valueOf(info.getId()));
        system.put("userId", null == uid ? "" : String.valueOf(uid));
        system.put("userName", null == user ? "" : user.at("/name").asText(""));
        system.put("conversationId", null == chat ? "" : String.valueOf(chat.getId()));
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"));
        system.put("datetime", now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        system.put("date", now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        return system;
    }

    /** 当前登录用户：取不到时返回 null，不影响运行 */
    protected JsonNode currentUser(HttpServletRequest request) {
        try {
            return null == request ? null : rbacService.currentInfo(request);
        } catch (Exception e) {
            return null;
        }
    }

    /** 用户输入：取开始节点的 query，没有时用入参 JSON 兜底 */
    protected String query(ObjectNode inputs) {
        String query = inputs.at("/query").asText("");
        return DPUtil.empty(query) ? "" : DPUtil.trim(query);
    }

    /**
     * 可续写的会话标识：会话必须存在、未删除，且属于同一编排、同一类型、同一用户。
     * 不满足时返回 0（按新会话处理），避免拿别人的 chatId 续写或读到他人会话。
     */
    protected Integer ownedChatId(Integer chatId, Agentic info, String type, Integer uid) {
        if (null == chatId || chatId <= 0) return 0;
        AgenticChat chat = info(agenticChatDao, chatId);
        if (null == chat) return 0;
        if (null != chat.getDeletedTime() && chat.getDeletedTime() > 0) return 0;
        if (!Objects.equals(chat.getAgenticId(), info.getId())) return 0;
        if (!type.equals(DPUtil.parseString(chat.getType()))) return 0;
        if (!Objects.equals(chat.getCreatedUid(), null == uid ? 0 : uid)) return 0;
        return chat.getId();
    }

    /** 取会话：chatId 有效时复用，否则按标题新建（标题取用户输入前 60 字） */
    protected AgenticChat resolveChat(Integer chatId, Agentic info, String type, String title, Integer uid) {
        if (null != chatId && chatId > 0) {
            AgenticChat exists = info(agenticChatDao, chatId);
            if (null != exists) return exists;
        }
        long now = System.currentTimeMillis();
        AgenticChat chat = AgenticChat.builder()
                .agenticId(info.getId())
                .title(DPUtil.empty(title) ? "新会话" : cut(title, 60))
                .type(type)
                .leafId(0)
                .createdTime(now)
                .createdUid(null == uid ? 0 : uid)
                .updatedTime(now)
                .updatedUid(null == uid ? 0 : uid)
                // 各字段显式赋初值：@DynamicInsert 省略 null 字段，文本列没有默认值
                .deletedTime(0L)
                .deletedUid(0)
                .build();
        return agenticChatDao.save(chat);
    }

    /**
     * 会话历史（模型上下文）：从 leafId 沿 parentId 回溯出当前分支，按时间正序返回最近若干轮。
     * branchStart 表示本轮显式从会话起点新起分支，此时上下文就是空的；
     * 其余情况 leafId 为空或该消息已不存在时回退成整条会话的线性历史（迁移前的数据没有消息链）。
     */
    protected ArrayNode history(Integer chatId, Integer leafId, boolean branchStart) {
        ArrayNode result = DPUtil.arrayNode();
        if (null == chatId || chatId < 1) return result;
        List<AgenticDialog> rows = agenticDialogDao.findAll((root, query, cb) -> cb.equal(root.get("chatId"), chatId),
                Sort.by(Sort.Order.asc("id")));
        Map<Integer, AgenticDialog> byId = new LinkedHashMap<>();
        for (AgenticDialog row : rows) byId.put(row.getId(), row);
        List<AgenticDialog> branch = new ArrayList<>();
        Set<Integer> visited = new LinkedHashSet<>();
        Integer current = null == leafId ? 0 : leafId;
        while (null != current && current > 0 && visited.add(current)) {
            AgenticDialog row = byId.get(current);
            if (null == row) break;
            branch.add(row);
            current = row.getParentId();
        }
        Collections.reverse(branch);
        // 分支起点重发（branchStart）时上下文就是空的，不能退回全部消息，否则新分支会读到旧分支的问答；
        // 其余情况（早期数据没有分支尾）仍退回全部消息，保持升级前的上下文口径
        if (branch.isEmpty() && !branchStart) branch = rows;
        for (AgenticDialog row : branch) {
            // 标记删除的消息不进模型上下文
            if (null != row.getDeletedTime() && row.getDeletedTime() > 0) continue;
            if (!Arrays.asList("user", "assistant").contains(DPUtil.parseString(row.getRole()))) continue;
            // 回复正文里的图表占位符不进上下文（图表由前端按占位符渲染，不是模型该读的内容）
            String content = ChartNodeHandler.stripPlaceholder(DPUtil.parseString(row.getContent()));
            if (DPUtil.empty(content)) continue;
            ObjectNode item = result.addObject();
            item.put("role", DPUtil.parseString(row.getRole()));
            item.put("content", content);
            // 工具调用明细随历史一起带给节点，由节点按「工具链」开关决定是否加入上下文
            String reference = DPUtil.parseString(row.getReference());
            if (!DPUtil.empty(reference)) {
                ArrayNode calls = referenceCalls(reference);
                if (!calls.isEmpty()) item.set("toolCalls", calls);
            }
        }
        return result;
    }

    /**
     * 本次运行的父消息：未传（或传空）表示沿用会话当前分支尾；
     * 显式传 0 表示从会话起点新起分支（编辑第一条提问重新发送时，它的父消息就是分支起点），
     * 因此这里必须区分「没传」与「传了 0」，否则父消息是分支起点的重新发送会被当成接在分支尾。
     */
    protected Integer parentId(Map<?, ?> param) {
        Object value = param.get("parentId");
        if (null == value || DPUtil.empty(DPUtil.parseString(value))) return null;
        return DPUtil.parseInt(value);
    }

    /**
     * 本次运行的分支位置：parentId 指定从哪条消息往下续写，
     * null 表示未指定（沿用会话当前分支尾），0 表示从会话起点新起分支。
     * parentId 不属于本会话时按当前分支尾处理，避免跨会话拼上下文。
     */
    protected Integer branchOf(AgenticChat chat, Integer parentId) {
        Integer leaf = null == chat.getLeafId() ? 0 : chat.getLeafId();
        if (null == parentId) return leaf;
        if (parentId <= 0) return 0;
        AgenticDialog row = info(agenticDialogDao, parentId);
        if (null == row || !Objects.equals(row.getChatId(), chat.getId())) return leaf;
        return row.getId();
    }

    /**
     * 会话消息的参考数据：工具调用明细（供开启「工具链」记忆的节点在后续轮次里复用）
     * 与最终回复要展示的图表（供历史消息回显）。没有图表时沿用早期的工具调用数组格式。
     */
    protected String reference(ObjectNode result) {
        ArrayNode calls = calls(result);
        ArrayNode charts = result.at("/charts").isArray()
                ? (ArrayNode) result.at("/charts") : DPUtil.arrayNode();
        if (charts.isEmpty()) return calls.isEmpty() ? "" : DPUtil.stringify(calls);
        ObjectNode reference = DPUtil.objectNode();
        reference.set("calls", calls);
        reference.set("charts", charts);
        return DPUtil.stringify(reference);
    }

    /** 本轮的工具调用明细：取自各节点输出里的 calls（工具方法名、参数、返回结果） */
    protected ArrayNode calls(ObjectNode result) {
        ArrayNode calls = DPUtil.arrayNode();
        for (JsonNode node : result.at("/outputs")) {
            JsonNode items = node.at("/calls");
            if (items.isArray()) items.forEach(calls::add);
        }
        return calls;
    }

    /**
     * 会话消息参考数据里的工具调用明细：早期直接存数组，带图表后存 {calls, charts} 对象，
     * 这里统一取工具调用，两种格式都能用于「工具链」记忆。
     */
    protected ArrayNode referenceCalls(String reference) {
        if (DPUtil.empty(reference)) return DPUtil.arrayNode();
        JsonNode json = DPUtil.parseJSON(reference);
        if (null == json) return DPUtil.arrayNode();
        JsonNode calls = json.isObject() ? json.at("/calls") : json;
        return calls.isArray() ? (ArrayNode) calls : DPUtil.arrayNode();
    }

    /** 会话消息参考数据里的图表：没有图表（早期数据）时返回空数组 */
    protected ArrayNode referenceCharts(String reference) {
        if (DPUtil.empty(reference)) return DPUtil.arrayNode();
        JsonNode json = DPUtil.parseJSON(reference);
        JsonNode charts = null == json ? null : json.at("/charts");
        return null != charts && charts.isArray() ? (ArrayNode) charts : DPUtil.arrayNode();
    }

    /**
     * 追加会话消息：parentId 指定这条消息接在哪条消息之后（0 表示分支起点）；
     * 追加后把会话的 leafId 指向新消息（当前分支尾）并刷新更新时间，reference 存本轮的工具调用明细。
     */
    protected AgenticDialog appendDialog(AgenticChat chat, Integer parentId, String role, String content, String reasoning, String reference, Integer uid) {
        if (DPUtil.empty(content)) return null;
        // @DynamicInsert 会省略 null 字段，text/longtext 列在 MySQL 中没有默认值，
        // 因此这里把各字段显式赋初值（不在实体与建表语句上做默认值）
        AgenticDialog dialog = AgenticDialog.builder()
                .chatId(chat.getId())
                .parentId(null == parentId || parentId < 0 ? 0 : parentId)
                .role(role)
                .content(DPUtil.parseString(content))
                .reasoningContent(DPUtil.parseString(reasoning))
                .reference(DPUtil.parseString(reference))
                .feedbackEmotion("")
                .feedbackTag("")
                .feedbackContent("")
                .feedbackTime(0L)
                .createdTime(System.currentTimeMillis())
                .createdUid(null == uid ? 0 : uid)
                .deletedTime(0L)
                .deletedUid(0)
                .build();
        AgenticDialog saved = agenticDialogDao.save(dialog);
        chat.setLeafId(saved.getId());
        chat.setUpdatedTime(System.currentTimeMillis());
        chat.setUpdatedUid(null == uid ? 0 : uid);
        agenticChatDao.save(chat);
        return saved;
    }

    /**
     * 落库一轮对话：正常提问追加「用户提问 + 助手回复」；
     * 重新生成（reuseQuestion=true）只追加助手回复，挂在被重新生成的那条用户消息下，形成新分支。
     * 消息标识（questionId / answerId / leafId）随运行结果回传，前端据此反馈、续写或切换分支。
     */
    protected AgenticDialog appendTurn(AgenticChat chat, Integer branch, boolean reuseQuestion,
                                       ObjectNode result, String query, ObjectNode inputs, Integer uid) {
        AgenticDialog question = null;
        Integer parentId = branch;
        if (!reuseQuestion) {
            question = appendDialog(chat, branch, "user",
                    DPUtil.empty(query) ? DPUtil.stringify(inputs) : query, "", "", uid);
            parentId = null == question ? branch : question.getId();
        }
        AgenticDialog reply = appendDialog(chat, parentId, "assistant",
                result.at("/answer").asText(""), reasoning(result), reference(result), uid);
        result.put("questionId", null == question ? 0 : question.getId());
        result.put("answerId", null == reply ? 0 : reply.getId());
        result.put("leafId", null == chat.getLeafId() ? 0 : chat.getLeafId());
        return reply;
    }

    /** 回复的思考过程：取第一个大模型节点输出的 reasoning */
    protected String reasoning(ObjectNode result) {
        for (JsonNode node : result.at("/outputs")) {
            String reasoning = node.at("/reasoning").asText("");
            if (!DPUtil.empty(reasoning)) return reasoning;
        }
        return "";
    }

    /** 会话列表：只看编排产生的会话（调试与发布），支持按标题检索 */
    public ObjectNode chatSearch(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(agenticChatDao, param, (root, query, cb) -> {
            SpecificationHelper<AgenticChat> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.dateFormat(configuration.getFormatDate()).equalWithIntGTZero("id")
                    .equalWithIntGTZero("agenticId").equalWithIntGTZero("createdUid").equal("type").like("title");
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>(Arrays.asList(helper.predicates()));
            predicates.add(cb.in(root.get("type")).value(Arrays.asList(CHAT_DRAFT, CHAT_PUBLISHED)));
            addDeleted(predicates, root, cb, param);
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        }, Sort.by(Sort.Order.desc("updatedTime"), Sort.Order.desc("id")), Arrays.asList("id", "updatedTime", "createdTime"));
        JsonNode rows = ApiUtil.rows(result);
        for (JsonNode row : rows) {
            ObjectNode node = (ObjectNode) row;
            node.put("typeText", CHAT_PUBLISHED.equals(node.at("/type").asText("")) ? "发布应用" : "调试运行");
            node.put("deletedText", node.at("/deletedTime").asLong(0) > 0 ? "已删除" : "正常");
        }
        // 所属编排按 agenticId 关联填充：列表直接展示编排名称，不必再逐条打开会话
        fillAgenticName(rows);
        if (!DPUtil.empty(args.get("withUserInfo"))) {
            // 删除人随列表返回：排查「谁删了这条会话」时不必再连库
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid", "deletedUid");
        }
        return result;
    }

    /** 会话详情：消息列表 + 每轮运行日志（按钮点开可看节点与工具明细） */
    public ObjectNode chatInfo(Integer chatId) {
        AgenticChat chat = null == chatId ? null : info(agenticChatDao, chatId);
        if (null == chat) return null;
        ObjectNode result = (ObjectNode) DPUtil.toJSON(List.of(chat)).get(0);
        result.put("typeText", CHAT_PUBLISHED.equals(chat.getType()) ? "发布应用" : "调试运行");
        // 创建人/更新人随详情返回，前端展示会话的归属信息
        ArrayNode chatRows = DPUtil.arrayNode();
        chatRows.add(result); // fillUserInfo 会就地填充，传入同一节点引用即可
        rbacService.fillUserInfo(chatRows, "createdUid", "updatedUid");
        /**
         * 运行日志：一次查询拿齐会话回看需要的轻量字段（answerId / failures 等），
         * 回复与日志按 answer_id 列关联（不再解析 outputs JSON），
         * inputs / outputs / steps 三个大字段留给「执行过程」按 logId 懒加载
         */
        List<AgenticLogDao.LogIndex> logs = agenticLogDao.findByChatIdAndDeletedTimeOrderByIdAsc(chatId, 0L);
        Map<Integer, Integer> logByAnswer = new LinkedHashMap<>();
        Map<Integer, JsonNode> failuresByLog = new LinkedHashMap<>();
        for (AgenticLogDao.LogIndex log : logs) {
            if (null != log.getAnswerId() && log.getAnswerId() > 0) logByAnswer.put(log.getAnswerId(), log.getId());
            JsonNode failures = parseArray(DPUtil.parseString(log.getFailures()));
            if (!failures.isEmpty()) failuresByLog.put(log.getId(), failures);
        }
        List<AgenticDialog> dialogs = agenticDialogDao.findAll((root, query, cb) -> cb.and(
                cb.equal(root.get("chatId"), chatId),
                cb.equal(root.get("deletedTime"), 0L)), Sort.by(Sort.Order.asc("id")));
        ArrayNode messages = result.putArray("messages");
        for (AgenticDialog dialog : dialogs) {
            if (dialog.getDeletedTime() != null && dialog.getDeletedTime() > 0) continue;
            ObjectNode node = messages.addObject();
            node.put("id", dialog.getId());
            // 父消息标识：前端按它把消息还原成树，支持多条分支之间切换
            node.put("parentId", null == dialog.getParentId() ? 0 : dialog.getParentId());
            node.put("role", DPUtil.parseString(dialog.getRole()));
            node.put("content", DPUtil.parseString(dialog.getContent()));
            node.put("reasoning", DPUtil.parseString(dialog.getReasoningContent()));
            // 反馈信息随消息返回，前端据此回显点赞/点踩状态
            node.put("feedbackEmotion", DPUtil.parseString(dialog.getFeedbackEmotion()));
            node.put("feedbackTag", DPUtil.parseString(dialog.getFeedbackTag()));
            node.put("feedbackContent", DPUtil.parseString(dialog.getFeedbackContent()));
            node.put("createdTime", null == dialog.getCreatedTime() ? 0L : dialog.getCreatedTime());
            // 本轮回复要展示的图表：随消息返回，历史记录打开后与当时展示一致
            ArrayNode charts = referenceCharts(DPUtil.parseString(dialog.getReference()));
            if (!charts.isEmpty()) node.set("charts", charts);
            // 本轮运行日志标识：前端按它拉取完整的节点与工具调用过程（定位问题用）
            Integer logId = logByAnswer.get(dialog.getId());
            if (null != logId) {
                node.put("logId", logId);
                // 与调试面板一致：节点/工具调用异常挂到这条回复上，历史记录打开就能看到异常图标
                JsonNode failures = failuresByLog.getOrDefault(logId, DPUtil.arrayNode());
                if (!failures.isEmpty()) {
                    ObjectNode notice = DPUtil.objectNode();
                    notice.put("summary", failures.get(0).asText(""));
                    StringBuilder detail = new StringBuilder();
                    for (int index = 1; index < failures.size(); index++) {
                        if (detail.length() > 0) detail.append('\n');
                        detail.append(failures.get(index).asText(""));
                    }
                    notice.put("detail", detail.toString());
                    node.set("notice", notice);
                }
            }
        }
        ArrayNode runs = result.putArray("runs");
        Map<Integer, String> agenticNames = new LinkedHashMap<>();
        for (AgenticLogDao.LogIndex log : logs) {
            ObjectNode node = runs.addObject();
            node.put("id", log.getId());
            node.put("agenticId", null == log.getAgenticId() ? 0 : log.getAgenticId());
            node.put("status", null == log.getStatus() ? 1 : log.getStatus());
            node.put("duration", null == log.getDuration() ? 0L : log.getDuration());
            node.put("error", DPUtil.parseString(log.getError()));
            node.put("createdTime", null == log.getCreatedTime() ? 0L : log.getCreatedTime());
        }
        fillAgenticName(runs);
        return result;
    }

    /**
     * 运行异常清单：节点执行失败与工具方法调用失败（与前端 runFailures 同一口径）。
     * 写入日志时预计算并存进 failures 列，会话回看直接取用，不必再解析 steps
     */
    protected ArrayNode failures(JsonNode steps, int status, String error) {
        ArrayNode result = DPUtil.arrayNode();
        for (JsonNode step : steps) {
            // 摘要只留前几条：够定位问题即可，完整信息仍在 steps / error 列里
            if (result.size() >= 5) break;
            if (2 == step.at("/status").asInt(1)) {
                result.add(brief("节点「" + step.at("/name").asText("") + "」执行失败：" + step.at("/error").asText("未知原因")));
            }
            JsonNode output = parseObject(step.at("/output").asText(""));
            for (JsonNode call : output.at("/calls")) {
                if (2 != call.at("/status").asInt(1)) continue;
                if (result.size() >= 5) break;
                result.add(brief("工具方法「" + call.at("/method").asText("") + "」调用失败：" + call.at("/error").asText("未知原因")));
            }
        }
        if (result.isEmpty() && 2 == status && !DPUtil.empty(error)) {
            result.add(brief(error));
        }
        return result;
    }

    /** 摘要文案限长：failures 列按 varchar(2000) 存，单条截断避免超长（完整内容仍在 error / steps 列） */
    protected String brief(String text) {
        String value = DPUtil.parseString(text).trim();
        return value.length() > 200 ? value.substring(0, 200) + "…" : value;
    }

    /**
     * 日志输出：去掉与 inputs / steps 列重复的内容，
     * 避免同一份大字段（尤其逐节点明细）在 longtext 里存两遍
     */
    protected ObjectNode logOutputs(ObjectNode result) {
        ObjectNode outputs = result.deepCopy();
        outputs.remove("inputs");
        outputs.remove("steps");
        return outputs;
    }

    /**
     * 消息反馈：对助手回复点赞/点踩，可附标签与说明；再次提交同一情绪表示取消反馈。
     * 反馈只做运营分析，不参与对话上下文。
     */
    public AgenticDialog chatFeedback(Integer dialogId, String emotion, String tag, String content) {
        if (null == dialogId || dialogId <= 0) return null;
        AgenticDialog dialog = info(agenticDialogDao, dialogId);
        if (null == dialog) return null;
        String next = Arrays.asList("positive", "negative").contains(emotion) ? emotion : "";
        // 再次点击同一情绪即取消，避免误操作无法撤回
        if (next.equals(DPUtil.parseString(dialog.getFeedbackEmotion()))) next = "";
        dialog.setFeedbackEmotion(next);
        dialog.setFeedbackTag(DPUtil.empty(next) ? "" : DPUtil.parseString(tag));
        dialog.setFeedbackContent(DPUtil.empty(next) ? "" : DPUtil.parseString(content));
        dialog.setFeedbackTime(DPUtil.empty(next) ? 0L : System.currentTimeMillis());
        return agenticDialogDao.save(dialog);
    }

    /**
     * 删除会话：标记删除（保留消息与运行日志，便于追溯），
     * 会话、消息、运行日志一起打删除标记；默认列表与模型历史都不再取到
     */
    public boolean chatRemove(List<Integer> ids, HttpServletRequest request) {
        if (null == ids || ids.isEmpty()) return false;
        int uid = rbacService.uid(request);
        long time = System.currentTimeMillis();
        for (Integer id : ids) {
            AgenticChat chat = id > 0 ? info(agenticChatDao, id) : null;
            if (null == chat) continue;
            markDeleted(chat, uid, time);
            save(agenticChatDao, chat, uid);
            // 只处理未删除的子记录：已删除消息/日志不再重复打标记
            List<AgenticDialog> dialogs = agenticDialogDao.findAll((root, query, cb) -> cb.and(
                    cb.equal(root.get("chatId"), id), cb.equal(root.get("deletedTime"), 0L)));
            for (AgenticDialog dialog : dialogs) {
                markDeleted(dialog, uid, time);
                save(agenticDialogDao, dialog, uid);
            }
            List<AgenticLog> logs = agenticLogDao.findAll((root, query, cb) -> cb.and(
                    cb.equal(root.get("chatId"), id), cb.equal(root.get("deletedTime"), 0L)));
            for (AgenticLog log : logs) {
                markDeleted(log, uid, time);
                save(agenticLogDao, log, uid);
            }
        }
        return true;
    }

    /** 打删除标记：会话、消息、运行日志共用（deletedTime=0 表示未删除） */
    protected void markDeleted(AgenticChat chat, int uid, long time) {
        chat.setDeletedTime(time);
        chat.setDeletedUid(uid);
    }

    protected void markDeleted(AgenticDialog dialog, int uid, long time) {
        dialog.setDeletedTime(time);
        dialog.setDeletedUid(uid);
    }

    protected void markDeleted(AgenticLog log, int uid, long time) {
        log.setDeletedTime(time);
        log.setDeletedUid(uid);
    }

    /* ------------------------------- 运行日志 ------------------------------- */

    /**
     * 运行异常的统一处理：先补一条失败日志（含已产生的步骤与失败原因），再返回统一的错误信封。
     * 此前执行阶段抛出的异常（知识库向量化/重排序超时、连接断开导致推送异常等）会直接冒到接口层，
     * 会话消息与运行日志都写不下来，前端只剩一个网络错误，问题无从追溯；流式通道据此按 error 事件下发。
     */
    protected Map<String, Object> failureResult(Agentic info, Integer chatId, String source, int version,
                                                ObjectNode inputs, Exception e, long duration,
                                                Integer uid, HttpServletRequest request) {
        String message = DPUtil.parseString(e.getMessage());
        String error = DPUtil.empty(message) ? e.getClass().getSimpleName() : message;
        ObjectNode result = DPUtil.objectNode();
        result.put("status", 2);
        result.put("error", error);
        result.put("duration", duration);
        // 已执行到的节点明细：运行时按线程保存，异常时取到的正是部分进度
        result.set("steps", agenticRunner.steps());
        AgenticLog log = writeLog(info, chatId, source, version, inputs, result, uid, request);
        logger.warn("编排运行异常（agenticId={}, chatId={}, logId={}）：{}", info.getId(), chatId,
                null == log ? 0 : log.getId(), error, e);
        return ApiUtil.result(9500, "运行异常：" + error, null);
    }

    /** 写入运行日志：调试运行与外部调用共用，记录入参、输出、步骤与失败原因 */
    protected AgenticLog writeLog(Agentic info, Integer chatId, String source, int version, ObjectNode inputs,
                                  ObjectNode result, Integer uid, HttpServletRequest request) {
        try {
            AgenticLog log = AgenticLog.builder()
                    .agenticId(info.getId())
                    .chatId(null == chatId ? 0 : chatId)
                    // 回复消息标识落到列：会话回看时按列关联，不再解析 outputs JSON
                    .answerId(result.at("/answerId").asInt(0))
                    .source(source)
                    .version(version)
                    .status(2 == result.at("/status").asInt(1) ? 2 : 1)
                    .duration(result.at("/duration").asLong(0))
                    .inputs(DPUtil.stringify(inputs))
                    .outputs(DPUtil.stringify(logOutputs(result)))
                    .steps(DPUtil.stringify(result.at("/steps")))
                    .error(DPUtil.parseString(result.at("/error").asText("")))
                    // 异常摘要预计算：会话回看直接取用，不必再解析 steps（列表/历史消息都用得到）
                    .failures(DPUtil.stringify(failures(result.at("/steps"),
                            result.at("/status").asInt(1), result.at("/error").asText(""))))
                    .ip(null == request ? "" : DPUtil.parseString(request.getRemoteAddr()))
                    .createdTime(System.currentTimeMillis())
                    .createdUid(null == uid ? 0 : uid)
                    .deletedTime(0L)
                    .deletedUid(0)
                    .build();
            return agenticLogDao.save(log);
        } catch (Exception e) {
            // 日志失败不影响运行结果，但要留下痕迹，避免「日志丢失」无从发现
            logger.warn("写入运行日志失败（agenticId={}, chatId={}）", info.getId(), chatId, e);
            return null;
        }
    }

    /** 运行日志列表：不返回大字段（入参、输出、步骤在详情里返回） */
    public ObjectNode logSearch(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(agenticLogDao, param, (root, query, cb) -> {
            SpecificationHelper<AgenticLog> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.dateFormat(configuration.getFormatDate()).equalWithIntGTZero("id")
                    .equalWithIntGTZero("agenticId").equalWithIntGTZero("createdUid")
                    .equalWithIntNotEmpty("status").equal("source");
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>(Arrays.asList(helper.predicates()));
            // 删除状态筛选：only 只看已删除、without 只看未删除、其余为全部
            addDeleted(predicates, root, cb, param);
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        }, Sort.by(Sort.Order.desc("id")), sorts().keySet());
        // 列表不返回入参、输出与步骤：直接按「无详情」格式化，避免先把大字段 JSON.parse 成对象再丢掉
        JsonNode rows = formatLog(ApiUtil.rows(result), false);
        // 编排名称不再冗余存库：按 agenticId 填充展示
        fillAgenticName(rows);
        if (!DPUtil.empty(args.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid");
        }
        return result;
    }

    /** 详情：解析入参、输出与步骤，供运行日志详情与设计器调试结果展示 */
    public ObjectNode logInfo(Integer id) {
        AgenticLog log = null == id ? null : info(agenticLogDao, id);
        if (null == log) return null;
        JsonNode rows = DPUtil.toJSON(List.of(log));
        ObjectNode node = (ObjectNode) formatLog(rows).get(0);
        JsonNode items = DPUtil.arrayNode();
        ((ArrayNode) items).add(node); // fillAgenticName 会就地填充，传入同一节点引用即可
        fillAgenticName(items);
        // 调用人信息：列表走 withUserInfo 填充，详情同样要填充，否则前端只能显示「系统」
        ArrayNode userRows = DPUtil.arrayNode();
        userRows.add(node);
        rbacService.fillUserInfo(userRows, "createdUid");
        return node;
    }

    /** 列表里用户输入的解析上限：超过该长度不解析（避免极端大字段拖慢列表） */
    private static final int LOG_INPUT_PARSE_LIMIT = 200 * 1024;
    /** 列表里用户输入的展示上限 */
    private static final int LOG_INPUT_TEXT_LIMIT = 200;

    /**
     * 列表里的用户输入摘要：入参结构随节点配置而变，可能是 { query }、嵌套在 inputs/data 里，
     * 或存成 [{ name: 'query', value }] 的键值对数组；按优先级取第一个命中的字符串并截断。
     */
    private String logInput(String inputs) {
        if (DPUtil.empty(inputs) || inputs.length() > LOG_INPUT_PARSE_LIMIT) return "";
        JsonNode parsed = parseObject(inputs);
        if (null == parsed) return "";
        for (String path : List.of("/query", "/input", "/message", "/prompt", "/inputs/query", "/inputs/input", "/data/query")) {
            String text = parsed.at(path).asText("");
            if (!text.isEmpty()) return truncateLogInput(text);
        }
        for (JsonNode item : parsed) {
            String key = item.at("/name").asText(item.at("/key").asText(""));
            if (!List.of("query", "input", "message", "prompt").contains(key)) continue;
            String text = item.at("/value").asText(item.at("/content").asText(""));
            if (!text.isEmpty()) return truncateLogInput(text);
        }
        return "";
    }

    /** 截断列表里的用户输入，避免长文本撑开表格 */
    private String truncateLogInput(String text) {
        return text.length() > LOG_INPUT_TEXT_LIMIT ? text.substring(0, LOG_INPUT_TEXT_LIMIT) + "…" : text;
    }

    public JsonNode formatLog(JsonNode rows) {
        return formatLog(rows, true);
    }

    /**
     * 运行日志格式化：withDetail=false 时不解析 inputs / outputs / steps（列表页用不到，内容很大），
     * 只做展示文案的补齐，避免把大字段解析成对象后又移除
     */
    public JsonNode formatLog(JsonNode rows, boolean withDetail) {
        for (JsonNode row : rows) {
            ObjectNode node = (ObjectNode) row;
            if (withDetail) {
                node.replace("inputs", parseObject(node.at("/inputs").asText("")));
                node.replace("outputs", parseObject(node.at("/outputs").asText("")));
                node.replace("steps", parseArray(node.at("/steps").asText("")));
            } else {
                // 列表不返回大字段，但「用户输入」是列表的检索线索：只抽一条摘要，不整块返回
                node.put("inputsText", logInput(node.at("/inputs").asText("")));
                node.remove("inputs");
                node.remove("outputs");
                node.remove("steps");
            }
            // 异常摘要：预计算的小字段，列表与详情都以数组返回
            node.replace("failures", parseArray(node.at("/failures").asText("")));
            node.put("sourceText", "published".equals(node.at("/source").asText("")) ? "外部调用" : "调试运行");
            node.put("statusText", 2 == node.at("/status").asInt(1) ? "失败" : "成功");
            node.put("deletedText", node.at("/deletedTime").asLong(0) > 0 ? "已删除" : "正常");
        }
        return rows;
    }

    /** 运行日志里的编排名称：按 agenticId 关联编排填充（列表与详情共用） */
    protected void fillAgenticName(JsonNode rows) {
        Set<Integer> ids = new LinkedHashSet<>();
        for (JsonNode row : rows) {
            int id = row.at("/agenticId").asInt(0);
            if (id > 0) ids.add(id);
        }
        Map<Integer, String> names = new LinkedHashMap<>();
        for (Integer id : ids) {
            Agentic info = info(id);
            names.put(id, null == info ? "已删除的编排（ID：" + id + "）" : DPUtil.parseString(info.getName()));
        }
        for (JsonNode row : rows) {
            ((ObjectNode) row).put("agenticName", names.getOrDefault(row.at("/agenticId").asInt(0), ""));
        }
    }

    public boolean logRemove(List<Integer> ids) {
        if (null == ids || ids.isEmpty()) return false;
        long time = System.currentTimeMillis();
        for (Integer id : ids) {
            AgenticLog log = id > 0 ? info(agenticLogDao, id) : null;
            if (null == log) continue;
            markDeleted(log, 0, time);
            save(agenticLogDao, log, 0);
        }
        return true;
    }

    /* ------------------------------- 流程统计 ------------------------------- */

    /** 统计时区：与 sys.datetime 一样按东八区切分，服务所在时区（容器常见为 UTC）不影响分桶 */
    private static final ZoneId STATISTIC_ZONE = ZoneId.of("Asia/Shanghai");
    /**
     * 时间聚合层级的日期格式：键是可排序的日期文本，用 TreeMap 直接得到时间升序；
     * 周用「周号」（2026-W39）而不是周一日期，看图时不必再换算这是哪一周
     */
    private static final Map<String, DateTimeFormatter> STATISTIC_FORMATTER = Map.of(
            "hour", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00"),
            "day", DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            "week", DateTimeFormatter.ofPattern("YYYY-'W'ww"),
            "month", DateTimeFormatter.ofPattern("yyyy-MM"));
    /** 统计区间上限：整体 1 年；按小时再收紧到 31 天（745 个桶），明细全量取回，区间必须封顶 */
    private static final long MAX_STATISTIC_RANGE = 366L * 24 * 60 * 60 * 1000;
    private static final long MAX_HOUR_RANGE = 31L * 24 * 60 * 60 * 1000;

    /** 排名行：会话/轮次/成败/耗时，members 是另一个维度的去重成员（流程看用户、用户看流程） */
    private static class StatRank {
        long sessions;
        long rounds;
        long succeeded;
        long failed;
        long duration;
        long durationCount;
        long lastTime;
        final Set<Integer> members = new LinkedHashSet<>();
    }

    /**
     * 流程统计：会话数量与对话轮次按时间轴聚合，另出流程排名与用户排名。
     *
     * - 会话取会话表（按创建时间），对话轮次取运行日志（一轮对话一条），两侧共用
     *   「流程 + 用户 + 类型 + 删除状态」四个条件，两张时间轴看的是同一批对话；
     * - 支持时间、流程、用户、对话类型（draft / published）、对话状态（全部 / 未删除 / 已删除）
     *   执行状态（全部 / 成功 / 失败）与时间聚合层级（hour / day / week / month），默认近一周、按天；
     * - 数据经 MyBatis 取回（与 lm 用量统计同一套做法）：SQL 只按条件取聚合需要的列
     *   （不读标题、入参、输出、步骤等大字段），分桶、排名与去重都在服务层内存里完成，
     *   既免了为每种筛选组合各写一条 SQL，也不用数据库方言的日期函数；
     * - 执行状态：会话状态由它自己的轮次推导（有一轮失败即失败，全成功才算成功），
     *   轮次状态就是运行日志的状态；筛选只影响统计结果，不影响会话状态的推导；
     * - 时间轴按东八区切分（周取周一、小时取整点）并按键升序返回，成功 / 失败分开给；
     *   额外的状态分布（statuses）不带执行状态筛选：先记分布再按筛选决定算不算数，
     *   筛「失败」时分布图里仍能看到成功与失败各占多少，一眼看出筛选前的基础盘；
     * - 区间内的空桶会补齐 0：没有对话的时段照样占一格，趋势图不会因为缺桶而断线；
     * - 区间上限：整体 1 年，按小时 31 天（明细全量取回在内存里聚合，区间不封顶会有风险）；
     *   排名按会话数量降序、轮次数量次之，数量相同时按标识升序保证结果稳定。
     */
    public Map<String, Object> statistic(Map<String, Object> param) {
        long endTime = DPUtil.parseLong(param.get("endTime"));
        if (endTime < 1) endTime = System.currentTimeMillis();
        long beginTime = DPUtil.parseLong(param.get("beginTime"));
        if (beginTime < 1) beginTime = endTime - 7 * 24 * 60 * 60 * 1000L;
        if (beginTime > endTime) {
            long swap = beginTime;
            beginTime = endTime;
            endTime = swap;
        }
        if (endTime - beginTime > MAX_STATISTIC_RANGE) {
            return ApiUtil.result(1002, "统计区间不能超过 1 年", null);
        }
        String aggregation = statisticAggregation(DPUtil.parseString(param.get("aggregation")));
        if ("hour".equals(aggregation) && endTime - beginTime > MAX_HOUR_RANGE) {
            return ApiUtil.result(1002, "按小时统计的区间不能超过 31 天，请改用按天", null);
        }
        int status = statisticStatus(param.get("status"));
        // 明细经 MyBatis 取回：会话侧按 type 过滤、日志侧按 source 过滤，其余条件两边一致
        // 执行状态不落到 SQL：会话状态要由它自己的轮次推导，得先拿到全量轮次
        List<Map<String, Object>> chats = agenticStatisticMapper.chatRows(
                statisticParam(param, beginTime, endTime, "type"));
        List<Map<String, Object>> logs = agenticStatisticMapper.logRows(
                statisticParam(param, beginTime, endTime, "source"));

        // 会话的执行状态：同一会话只要有一轮失败就算失败，全成功才算成功
        Map<Integer, Integer> failedRounds = new HashMap<>();
        for (Map<String, Object> log : logs) {
            if (2 != DPUtil.parseInt(log.get("status"))) continue;
            int chatId = DPUtil.parseInt(log.get("chatId"));
            if (chatId < 1) continue;
            failedRounds.merge(chatId, 1, Integer::sum);
        }

        Map<String, long[]> timeline = new TreeMap<>();
        // 时间桶 -> [会话数量, 对话轮次, 参与用户, 覆盖流程, 成功轮次, 失败轮次, 成功会话, 失败会话]
        Map<String, Set<Integer>> timelineUsers = new TreeMap<>();
        Map<String, Set<Integer>> timelineFlows = new TreeMap<>();
        Map<String, long[]> typeRows = new LinkedHashMap<>(); // 对话类型 -> [会话数量, 对话轮次]
        // 执行状态分布：先记分布再按筛选决定算不算数，所以这里不带执行状态筛选
        Map<Integer, long[]> statusRows = new LinkedHashMap<>();
        Map<Integer, StatRank> flowRanks = new LinkedHashMap<>();
        Map<Integer, StatRank> userRanks = new LinkedHashMap<>();
        Set<Integer> users = new LinkedHashSet<>();
        Set<Integer> flows = new LinkedHashSet<>();
        long sessions = 0, rounds = 0, succeeded = 0, duration = 0, durationCount = 0;

        // 会话数量：按会话创建时间落桶；用户与流程两个维度也从这里开始累计
        for (Map<String, Object> chat : chats) {
            long created = DPUtil.parseLong(chat.get("createdTime"));
            if (created < 1) continue;
            int flowId = DPUtil.parseInt(chat.get("agenticId"));
            int owner = DPUtil.parseInt(chat.get("createdUid"));
            boolean failed = failedRounds.getOrDefault(DPUtil.parseInt(chat.get("id")), 0) > 0;
            statisticStatusRow(statusRows, failed ? 2 : 1)[0]++;
            if (1 == status && failed) continue;
            if (2 == status && !failed) continue;
            String key = statisticTimeKey(created, aggregation);
            long[] cell = timeline.computeIfAbsent(key, k -> new long[8]);
            sessions++;
            cell[0]++;
            cell[failed ? 7 : 6]++;
            timelineUsers.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(owner);
            timelineFlows.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(flowId);
            users.add(owner);
            flows.add(flowId);
            statisticTypeRow(typeRows, DPUtil.parseString(chat.get("type")))[0]++;
            StatRank flow = statisticRank(flowRanks, flowId);
            flow.sessions++;
            flow.members.add(owner);
            flow.lastTime = Math.max(flow.lastTime, created);
            StatRank user = statisticRank(userRanks, owner);
            user.sessions++;
            user.members.add(flowId);
            user.lastTime = Math.max(user.lastTime, created);
        }

        // 对话轮次：一轮对话一条日志，成败与耗时都按日志统计（会话删除时日志一并打删除标记）
        for (Map<String, Object> log : logs) {
            long created = DPUtil.parseLong(log.get("createdTime"));
            if (created < 1) continue;
            boolean succeed = 2 != DPUtil.parseInt(log.get("status"));
            statisticStatusRow(statusRows, succeed ? 1 : 2)[1]++;
            // 执行状态筛选只作用于统计结果，不影响上面推导出来的会话状态
            if (1 == status && !succeed) continue;
            if (2 == status && succeed) continue;
            int flowId = DPUtil.parseInt(log.get("agenticId"));
            int owner = DPUtil.parseInt(log.get("createdUid"));
            long cost = DPUtil.parseLong(log.get("duration"));
            String key = statisticTimeKey(created, aggregation);
            long[] cell = timeline.computeIfAbsent(key, k -> new long[8]);
            rounds++;
            if (succeed) succeeded++;
            if (cost > 0) {
                duration += cost;
                durationCount++;
            }
            cell[1]++;
            cell[succeed ? 4 : 5]++;
            timelineUsers.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(owner);
            timelineFlows.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(flowId);
            users.add(owner);
            flows.add(flowId);
            statisticTypeRow(typeRows, DPUtil.parseString(log.get("source")))[1]++;
            StatRank flow = statisticRank(flowRanks, flowId);
            flow.rounds++;
            if (succeed) flow.succeeded++;
            else flow.failed++;
            if (cost > 0) {
                flow.duration += cost;
                flow.durationCount++;
            }
            flow.members.add(owner);
            flow.lastTime = Math.max(flow.lastTime, created);
            StatRank user = statisticRank(userRanks, owner);
            user.rounds++;
            if (succeed) user.succeeded++;
            else user.failed++;
            if (cost > 0) {
                user.duration += cost;
                user.durationCount++;
            }
            user.members.add(flowId);
            user.lastTime = Math.max(user.lastTime, created);
        }

        ObjectNode result = DPUtil.objectNode();
        result.put("beginTime", beginTime);
        result.put("endTime", endTime);
        result.put("aggregation", aggregation);
        result.put("status", status);
        ObjectNode summary = result.putObject("summary");
        summary.put("sessions", sessions);
        summary.put("rounds", rounds);
        summary.put("users", users.size());
        summary.put("flows", flows.size());
        summary.put("succeeded", succeeded);
        summary.put("failed", rounds - succeeded);
        summary.put("successRate", rounds < 1 ? 0L : Math.round(succeeded * 100d / rounds));
        summary.put("avgDuration", durationCount < 1 ? 0L : Math.round(duration * 1d / durationCount));

        // 补齐区间内的空桶：没有数据的时段也给 0，趋势图才连续（缺桶会让折线与柱距看起来不均匀）
        statisticFillTimeline(timeline, beginTime, endTime, aggregation);

        ArrayNode timelineNodes = result.putArray("timeline");
        for (Map.Entry<String, long[]> entry : timeline.entrySet()) {
            long[] cell = entry.getValue();
            ObjectNode row = timelineNodes.addObject();
            row.put("time", entry.getKey());
            row.put("sessions", cell[0]);
            row.put("rounds", cell[1]);
            // 参与用户与覆盖流程按桶内去重：同一用户在同一个桶里聊了十轮，仍然算一个人
            row.put("users", timelineUsers.getOrDefault(entry.getKey(), Collections.emptySet()).size());
            row.put("flows", timelineFlows.getOrDefault(entry.getKey(), Collections.emptySet()).size());
            row.put("succeeded", cell[4]);
            row.put("failed", cell[5]);
            row.put("succeededSessions", cell[6]);
            row.put("failedSessions", cell[7]);
        }

        ArrayNode typeNodes = result.putArray("types");
        for (Map.Entry<String, long[]> entry : typeRows.entrySet()) {
            ObjectNode row = typeNodes.addObject();
            row.put("type", entry.getKey());
            row.put("typeText", CHAT_PUBLISHED.equals(entry.getKey()) ? "发布应用" : "调试运行");
            row.put("sessions", entry.getValue()[0]);
            row.put("rounds", entry.getValue()[1]);
        }

        // 执行状态分布：固定「成功在前、失败在后」，与页面上的分布图顺序一致；
        // 这里不套用执行状态筛选（其余筛选照常生效），筛选后仍能看到两边的盘面
        ArrayNode statusNodes = result.putArray("statuses");
        for (int value = 1; value <= 2; value++) {
            long[] cell = statusRows.getOrDefault(value, new long[2]);
            ObjectNode row = statusNodes.addObject();
            row.put("status", value);
            row.put("statusText", 1 == value ? "成功" : "失败");
            row.put("sessions", cell[0]);
            row.put("rounds", cell[1]);
        }

        // 流程排名：会话数量为主序；编排名称按 agenticId 关联填充，仓库里不冗余存
        ArrayNode flowNodes = result.putArray("flows");
        for (Map.Entry<Integer, StatRank> entry : statisticSorted(flowRanks)) {
            StatRank item = entry.getValue();
            ObjectNode row = flowNodes.addObject();
            row.put("agenticId", entry.getKey());
            row.put("sessions", item.sessions);
            row.put("rounds", item.rounds);
            row.put("users", item.members.size());
            row.put("succeeded", item.succeeded);
            row.put("failed", item.failed);
            row.put("successRate", item.rounds < 1 ? 0L : Math.round(item.succeeded * 100d / item.rounds));
            row.put("avgDuration", item.durationCount < 1 ? 0L : Math.round(item.duration * 1d / item.durationCount));
            row.put("lastTime", item.lastTime);
        }
        fillAgenticName(flowNodes);

        // 用户排名：与流程排名同一套排序，成员集合换成「覆盖了几个流程」
        ArrayNode userNodes = result.putArray("users");
        for (Map.Entry<Integer, StatRank> entry : statisticSorted(userRanks)) {
            StatRank item = entry.getValue();
            ObjectNode row = userNodes.addObject();
            row.put("createdUid", entry.getKey());
            row.put("sessions", item.sessions);
            row.put("rounds", item.rounds);
            row.put("flows", item.members.size());
            row.put("succeeded", item.succeeded);
            row.put("failed", item.failed);
            row.put("successRate", item.rounds < 1 ? 0L : Math.round(item.succeeded * 100d / item.rounds));
            row.put("avgDuration", item.durationCount < 1 ? 0L : Math.round(item.duration * 1d / item.durationCount));
            row.put("lastTime", item.lastTime);
        }
        rbacService.fillUserInfo(userNodes, "createdUid");
        return ApiUtil.result(0, null, result);
    }

    /**
     * 统计的查询条件：把页面上的「空串 / 0 即不过滤」翻成带命名参数的 where 片段，
     * 与 lm 用量统计的写法一致（条件片段放进 params 的 where 键，取值一律走 #{}）。
     *
     * typeColumn 是会话类型列：会话表叫 type、运行日志叫 source，取值同为 draft / published，
     * 两边的明细查询共用这段逻辑，只有列名不同。
     */
    protected Map<String, Object> statisticParam(Map<String, Object> param,
                                                 long beginTime, long endTime, String typeColumn) {
        Map<String, Object> filters = new LinkedHashMap<>();
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        where.append(" AND created_time >= #{beginTime}");
        filters.put("beginTime", beginTime);
        where.append(" AND created_time <= #{endTime}");
        filters.put("endTime", endTime);
        int agenticId = DPUtil.parseInt(param.get("agenticId"));
        if (agenticId > 0) {
            where.append(" AND agentic_id = #{agenticId}");
            filters.put("agenticId", agenticId);
        }
        int uid = DPUtil.parseInt(param.get("uid"));
        if (uid > 0) {
            where.append(" AND created_uid = #{uid}");
            filters.put("uid", uid);
        }
        String type = DPUtil.trim(DPUtil.parseString(param.get("type")));
        if (CHAT_DRAFT.equals(type) || CHAT_PUBLISHED.equals(type)) {
            where.append(" AND ").append(typeColumn).append(" = #{type}");
            filters.put("type", type);
        }
        int deleted = statisticDeleted(param.get("deleted"));
        if (1 == deleted) where.append(" AND deleted_time > 0");
        else if (0 == deleted) where.append(" AND deleted_time = 0");
        filters.put("where", where.toString());
        return filters;
    }

    /** 时间聚合层级：只认四种，其余按天（与 lm 用量统计同一组取值） */
    protected String statisticAggregation(String value) {
        return STATISTIC_FORMATTER.containsKey(value) ? value : "day";
    }

    /** 对话状态（与列表页 form-deleted 一致）：only 只看已删除、without 只看未删除，其余为全部 */
    protected int statisticDeleted(Object value) {
        String mode = DPUtil.parseString(value);
        if ("only".equals(mode)) return 1;
        if ("without".equals(mode)) return 0;
        return -1;
    }

    /**
     * 执行状态：success 只看成功、failed 只看失败，其余为全部。
     * 轮次看运行日志自身的状态（1 成功 / 2 失败），会话看它自己的轮次有没有失败。
     */
    protected int statisticStatus(Object value) {
        String mode = DPUtil.parseString(value);
        if ("success".equals(mode)) return 1;
        if ("failed".equals(mode)) return 2;
        return 0;
    }

    /** 时间轴分桶：东八区切分，周取周一、小时取整点 */
    protected String statisticTimeKey(long time, String aggregation) {
        return statisticTimeKey(Instant.ofEpochMilli(time).atZone(STATISTIC_ZONE), aggregation);
    }

    protected String statisticTimeKey(ZonedDateTime datetime, String aggregation) {
        if ("week".equals(aggregation)) datetime = datetime.with(DayOfWeek.MONDAY);
        return datetime.format(STATISTIC_FORMATTER.getOrDefault(aggregation, STATISTIC_FORMATTER.get("day")));
    }

    /**
     * 补齐区间内的空桶：从区间起点所在桶走到终点所在桶，缺的补 0。
     * 桶数由区间上限兜住（最多 745 个），不会因为补齐而把返回体撑大。
     */
    protected void statisticFillTimeline(Map<String, long[]> timeline,
                                         long beginTime, long endTime, String aggregation) {
        ZonedDateTime cursor = statisticBucketStart(beginTime, aggregation);
        ZonedDateTime last = statisticBucketStart(endTime, aggregation);
        while (!cursor.isAfter(last)) {
            timeline.computeIfAbsent(statisticTimeKey(cursor, aggregation), k -> new long[8]);
            cursor = statisticNextBucket(cursor, aggregation);
        }
    }

    /** 桶起点：小时取整点、天取零点、周取周一、月取 1 号（东八区） */
    protected ZonedDateTime statisticBucketStart(long time, String aggregation) {
        ZonedDateTime datetime = Instant.ofEpochMilli(time).atZone(STATISTIC_ZONE);
        switch (aggregation) {
            case "hour":
                return datetime.truncatedTo(ChronoUnit.HOURS);
            case "month":
                return datetime.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
            case "week":
                return datetime.with(DayOfWeek.MONDAY).truncatedTo(ChronoUnit.DAYS);
            default:
                return datetime.truncatedTo(ChronoUnit.DAYS);
        }
    }

    protected ZonedDateTime statisticNextBucket(ZonedDateTime datetime, String aggregation) {
        switch (aggregation) {
            case "hour":
                return datetime.plusHours(1);
            case "month":
                return datetime.plusMonths(1);
            case "week":
                return datetime.plusWeeks(1);
            default:
                return datetime.plusDays(1);
        }
    }

    /** 对话类型行：[会话数量, 对话轮次]，键归一成 draft / published（日志侧叫 source，取值相同） */
    protected long[] statisticTypeRow(Map<String, long[]> rows, String type) {
        String key = CHAT_PUBLISHED.equals(type) ? CHAT_PUBLISHED : CHAT_DRAFT;
        return rows.computeIfAbsent(key, k -> new long[2]);
    }

    /** 执行状态分布行：[会话数量, 对话轮次]，键 1 成功 / 2 失败 */
    protected long[] statisticStatusRow(Map<Integer, long[]> rows, int status) {
        return rows.computeIfAbsent(1 == status ? 1 : 2, k -> new long[2]);
    }

    protected StatRank statisticRank(Map<Integer, StatRank> rows, Integer id) {
        return rows.computeIfAbsent(null == id ? 0 : id, k -> new StatRank());
    }

    /** 排名排序：会话数量降序 -> 轮次数量降序 -> 标识升序（数量相同时结果稳定） */
    protected List<Map.Entry<Integer, StatRank>> statisticSorted(Map<Integer, StatRank> rows) {
        List<Map.Entry<Integer, StatRank>> list = new ArrayList<>(rows.entrySet());
        list.sort((a, b) -> {
            int compare = Long.compare(b.getValue().sessions, a.getValue().sessions);
            if (0 != compare) return compare;
            compare = Long.compare(b.getValue().rounds, a.getValue().rounds);
            if (0 != compare) return compare;
            return Integer.compare(a.getKey(), b.getKey());
        });
        return list;
    }

    protected String cut(String text, int length) {
        String value = DPUtil.parseString(text);
        return value.length() <= length ? value : value.substring(0, length);
    }

    public boolean remove(List<Integer> ids) {
        return remove(agenticDao, ids);
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(agenticDao, param, (root, query, cb) -> {
            SpecificationHelper<Agentic> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.dateFormat(configuration.getFormatDate()).equalWithIntGTZero("id");
            helper.equalWithIntNotEmpty("status").equal("mode").like("name");
            return cb.and(helper.predicates());
        }, Sort.by(Sort.Order.desc("sort"), Sort.Order.desc("id")), sorts().keySet());
        JsonNode rows = format(ApiUtil.rows(result));
        if (!DPUtil.empty(args.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid", "publishedUid");
            // 授权角色：按 roleIds 填充角色信息（列表页展示角色名称）
            rbacService.fillInfos(rows);
        }
        if (!DPUtil.empty(args.get("withStatusText"))) {
            fillStatus(rows, status());
        }
        return result;
    }

    /**
     * 列表数据：不返回画布内容（草稿与发布内容都很大，仅在详情中返回）
     */
    public JsonNode format(JsonNode rows) {
        for (JsonNode row : rows) {
            ObjectNode node = (ObjectNode) row;
            node.set("tags", parseArray(node.at("/tags").asText("")));
            // 授权角色：列表以数组返回，供列表页展示与用户对话页筛选
            node.set("roleIds", DPUtil.toJSON(DPUtil.parseIntList(node.at("/roleIds").asText(""))));
            node.remove("content");
            node.remove("publishedContent");
            node.put("modeText", modes().get(node.at("/mode").asText("")));
            int version = node.at("/publishedVersion").asInt(0);
            node.put("publishText", version > 0 ? "已发布 v" + version : "未发布");
        }
        return rows;
    }

    protected String publishText(Agentic info) {
        int version = null == info.getPublishedVersion() ? 0 : info.getPublishedVersion();
        if (version < 1) return "未发布";
        boolean changed = null != info.getUpdatedTime() && null != info.getPublishedTime()
                && info.getUpdatedTime() > info.getPublishedTime();
        return changed ? "已发布 v" + version + "（有未发布改动）" : "已发布 v" + version;
    }

    /**
     * 参数可能是集合（前端传入）、JsonNode、JSON 文本（数据库读取）或其它对象，统一转为数组
     */
    protected JsonNode parseArray(Object value) {
        JsonNode node;
        if (value instanceof Collection) {
            node = DPUtil.toJSON(value);
        } else if (value instanceof CharSequence) {
            node = DPUtil.parseJSON(value.toString());
            if (null == node) { // 兼容逗号分割的文本
                ArrayNode result = DPUtil.arrayNode();
                for (String item : DPUtil.explode(",", value.toString(), null, true)) {
                    result.add(item);
                }
                return result;
            }
        } else if (value instanceof JsonNode) {
            node = (JsonNode) value;
        } else {
            node = DPUtil.parseJSON(DPUtil.stringify(value));
        }
        return null != node && node.isArray() ? node : DPUtil.arrayNode();
    }

    /**
     * 参见 parseArray，统一转为对象
     */
    protected ObjectNode parseObject(Object value) {
        if (value instanceof ObjectNode) return (ObjectNode) value;
        JsonNode node;
        if (value instanceof CharSequence) {
            node = DPUtil.parseJSON(value.toString());
        } else if (value instanceof JsonNode) {
            node = (JsonNode) value;
        } else {
            node = DPUtil.parseJSON(DPUtil.stringify(value));
        }
        return null != node && node.isObject() ? (ObjectNode) node : DPUtil.objectNode();
    }

    /**
     * 运行计划：按连线从开始节点推导执行顺序，并给出画布告警，便于调试画布
     */
    protected ObjectNode plan(JsonNode content) {
        Map<String, ObjectNode> nodes = new LinkedHashMap<>();
        Map<String, List<String>> targets = new LinkedHashMap<>();
        JsonNode cells = content.at("/cells");
        if (cells.isArray()) {
            for (JsonNode cell : cells) {
                String shape = cell.at("/shape").asText("");
                if (shape.endsWith("edge")) {
                    String source = cell.at("/source/cell").asText("");
                    String target = cell.at("/target/cell").asText("");
                    if (DPUtil.empty(source) || DPUtil.empty(target)) continue;
                    targets.computeIfAbsent(source, key -> new ArrayList<>()).add(target);
                } else {
                    nodes.put(cell.at("/id").asText(""), (ObjectNode) cell);
                }
            }
        }
        List<String> warnings = new ArrayList<>();
        String startId = null;
        for (Map.Entry<String, ObjectNode> entry : nodes.entrySet()) {
            if ("Start".equals(entry.getValue().at("/data/type").asText(""))) {
                startId = entry.getKey();
                break;
            }
        }
        if (null == startId) warnings.add("缺少开始节点，无法推导执行顺序");
        Queue<String> queue = new LinkedList<>();
        Set<String> visited = new LinkedHashSet<>();
        if (null != startId) queue.add(startId);
        while (!queue.isEmpty()) {
            String id = queue.poll();
            if (!visited.add(id)) continue;
            for (String target : targets.getOrDefault(id, Collections.emptyList())) {
                if (!visited.contains(target)) queue.add(target);
            }
        }
        List<ObjectNode> steps = new ArrayList<>();
        for (String id : visited) {
            ObjectNode node = nodes.get(id);
            if (null == node) continue;
            ObjectNode step = DPUtil.objectNode();
            step.put("id", id);
            step.put("type", node.at("/data/type").asText(""));
            step.put("name", node.at("/data/name").asText(""));
            step.put("shape", node.at("/shape").asText(""));
            steps.add(step);
        }
        for (Map.Entry<String, ObjectNode> entry : nodes.entrySet()) {
            if (visited.contains(entry.getKey())) continue;
            warnings.add("节点未连接到开始节点：" + entry.getValue().at("/data/name").asText(entry.getKey()));
        }
        ObjectNode plan = DPUtil.objectNode();
        plan.put("engine", "plan"); // 执行引擎接入前，先返回运行计划
        plan.put("count", steps.size());
        plan.set("steps", DPUtil.toJSON(steps));
        plan.set("warnings", DPUtil.toJSON(warnings));
        return plan;
    }

}
