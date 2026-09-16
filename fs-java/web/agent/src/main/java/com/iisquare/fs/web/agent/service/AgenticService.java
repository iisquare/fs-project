package com.iisquare.fs.web.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.agent.dao.AgenticDao;
import com.iisquare.fs.web.agent.entity.Agentic;
import com.iisquare.fs.web.agent.mvc.Configuration;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * 智能体编排服务。
 *
 * 编排内容分两条线：
 * - 草稿：保存（save）后的 content，仅用于设计器调试运行（run）
 * - 发布：发布（publish）把当时的草稿固化为 publishedContent 与发布版本，外部调用（invoke）只读该内容
 */
@Service
public class AgenticService extends JPAServiceBase {

    public static final String MODE_WORKFLOW = "workflow";
    public static final String MODE_CHAT = "chat";

    @Autowired
    AgenticDao agenticDao;
    @Autowired
    DefaultRbacService rbacService;
    @Autowired
    Configuration configuration;

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
        info.setPublishedContent(info.getContent());
        info.setPublishedVersion((null == info.getPublishedVersion() ? 0 : info.getPublishedVersion()) + 1);
        info.setPublishedTime(System.currentTimeMillis());
        info.setPublishedUid(rbacService.uid(request));
        save(agenticDao, info, rbacService.uid(request));
        return ApiUtil.result(0, null, detail(id));
    }

    /**
     * 调试运行：使用草稿内容，执行引擎接入前先返回校验后的运行计划
     */
    public Map<String, Object> run(Map<?, ?> param, HttpServletRequest request) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        Agentic info = id > 0 ? info(id) : null;
        if (null == info) return ApiUtil.result(404, null, id);
        JsonNode content = DPUtil.parseJSON(DPUtil.parseString(info.getContent()));
        if (null == content || !content.isObject()) return ApiUtil.result(1001, "编排内容为空，请先保存", id);
        ObjectNode result = DPUtil.objectNode();
        result.put("id", info.getId());
        result.put("name", info.getName());
        result.put("mode", info.getMode());
        result.put("source", "draft"); // 调试运行使用保存后的草稿内容
        result.set("inputs", null == param.get("inputs") ? DPUtil.objectNode() : parseObject(param.get("inputs")));
        result.set("plan", plan(content));
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
        ObjectNode result = DPUtil.objectNode();
        result.put("id", info.getId());
        result.put("name", info.getName());
        result.put("mode", info.getMode());
        result.put("version", version);
        result.put("publishedTime", null == info.getPublishedTime() ? 0L : info.getPublishedTime());
        result.set("content", parseObject(info.getPublishedContent()));
        result.set("inputs", null == param.get("inputs") ? DPUtil.objectNode() : parseObject(param.get("inputs")));
        return ApiUtil.result(0, null, result);
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
