package com.iisquare.fs.web.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.agent.core.AgenticNodeContext;
import com.iisquare.fs.web.agent.core.AgenticNodeException;
import com.iisquare.fs.web.agent.core.AgenticNodeHandler;
import com.iisquare.fs.web.agent.core.AgenticRuntime;
import com.iisquare.fs.web.agent.core.AgenticScheduler;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;

/**
 * 编排调度服务 - 只负责调度，不包含任何节点实现：
 * 1. 解析画布内容得到节点表与连线表；
 * 2. 从开始节点按连线推导执行顺序，逐节点记录步骤日志（容器内节点同样记录，带容器与迭代序号）；
 * 3. 按节点类型分派到对应的 {@link AgenticNodeHandler} 实现类；
 * 4. 条件分支按命中的分支只走对应的边；
 * 5. 统一处理错误中断与运行结果组装。
 *
 * 节点实现见 com.iisquare.fs.web.agent.runner.node 包；运行期共用能力（变量解析、模型调用、
 * 工具调用、多模态、记忆窗口）见 {@link AgenticRuntime}。
 */
@Service
public class AgenticRunner implements AgenticScheduler {

    @Autowired
    AgenticRuntime runtime;
    @Autowired
    com.iisquare.fs.web.agent.react.AgenticInvokeTool agenticInvokeTool;
    @Autowired
    com.iisquare.fs.web.agent.core.AgenticToolInvoker agenticToolService;
    /** 节点实现类：由 Spring 注入后按节点类型建立索引 */
    @Autowired
    List<AgenticNodeHandler> nodeHandlers;
    private final Map<String, AgenticNodeHandler> handlers = new LinkedHashMap<>();
    /**
     * 当前线程上正在执行的编排层数：0 为最外层（结束时状态留在原处，失败日志要读本次步骤），
     * 嵌套层退出时把挂起的外层状态恢复回去
     */
    private final ThreadLocal<Integer> runDepthThread = ThreadLocal.withInitial(() -> 0);

    @PostConstruct
    public void init() {
        if (null == nodeHandlers) return;
        for (AgenticNodeHandler handler : nodeHandlers) {
            handlers.put(handler.type(), handler);
        }
    }

    /**
     * 执行编排
     * @param content 编排内容（草稿或发布内容）
     * @param inputs  运行入参
     * @param history 历史对话（[{ role, content }]），多轮对话时作为模型输入的前置消息
     * @return { answer, outputs, conversation, steps, duration }
     */
    public ObjectNode execute(JsonNode content, ObjectNode inputs, ArrayNode history) {
        return execute(content, inputs, history, null);
    }

    /**
     * 执行编排（带系统变量）
     * @param system 系统变量（应用标识、会话标识、调用人、当前时间等），供 sys.xxx 变量取值
     */
    public ObjectNode execute(JsonNode content, ObjectNode inputs, ArrayNode history, Map<String, Object> system) {
        long begin = System.currentTimeMillis();
        // 嵌套运行（编排工具把另一个编排当工具调用）跑在同一线程上，这里先把外层状态挂起、
        // 换成本次运行自己的步骤日志，结束后原样恢复：否则子流程会清掉外层的画布结构、
        // 容器作用域与步骤日志（外层容器随后就找不到自己的子节点了）
        int depth = runDepthThread.get();
        Map<String, ObjectNode> outerNodes = new LinkedHashMap<>(runtime.nodeMap());
        Map<String, List<Map<String, String>>> outerEdges = new LinkedHashMap<>(runtime.edgeMap());
        Map<String, ObjectNode> outerScopes = new LinkedHashMap<>(runtime.scopes());
        Map<String, Object> outerSystem = new LinkedHashMap<>(runtime.system());
        ArrayNode outerSteps = runtime.steps();
        String outerCurrent = runtime.currentNode();
        String outerAnswerSource = runtime.answerSource();
        ObjectNode outerRequest = runtime.lastRequest();
        runtime.steps(DPUtil.arrayNode());
        runDepthThread.set(depth + 1);
        try {
            return run(content, inputs, history, system, begin);
        } finally {
            runDepthThread.set(depth);
            // 最外层运行结束后状态留在原处（失败日志要读本次运行的步骤），只有嵌套层退出时才恢复外层状态
            if (depth > 0) {
                runtime.nodeMap().clear();
                runtime.nodeMap().putAll(outerNodes);
                runtime.edgeMap().clear();
                runtime.edgeMap().putAll(outerEdges);
                runtime.scopes().clear();
                runtime.scopes().putAll(outerScopes);
                runtime.system().clear();
                runtime.system().putAll(outerSystem);
                runtime.steps(outerSteps);
                runtime.currentNode(outerCurrent);
                runtime.answerSource(outerAnswerSource);
                runtime.lastRequest(outerRequest);
            }
        }
    }

    /**
     * 运行主体：按本次运行的画布内容重建节点表、连线与作用域，从开始节点沿连线执行到结束节点
     */
    protected ObjectNode run(JsonNode content, ObjectNode inputs, ArrayNode history,
                             Map<String, Object> system, long begin) {
        Map<String, ObjectNode> nodes = new LinkedHashMap<>();
        Map<String, List<Map<String, String>>> targets = new LinkedHashMap<>();
        read(content, nodes, targets);
        // 复用线程（容器线程池）时先清空，避免上一次运行的画布结构与连线残留
        runtime.nodeMap().clear();
        runtime.edgeMap().clear();
        runtime.nodeMap().putAll(nodes);
        runtime.edgeMap().putAll(targets);
        runtime.scopes().clear();
        // 系统变量：每次运行重置（用户输入与文件由开始节点自己提供，不放进系统变量）
        runtime.system().clear();
        if (null != system) runtime.system().putAll(system);
        runtime.lastRequest(null);
        String startId = null;
        for (Map.Entry<String, ObjectNode> entry : nodes.entrySet()) {
            if ("Start".equals(type(entry.getValue()))) {
                startId = entry.getKey();
                break;
            }
        }
        if (null == startId) throw new IllegalStateException("编排缺少开始节点");
        // 最终回复来源：结束节点的回复内容是整串变量引用时，该节点的模型增量即最终回复
        runtime.answerSource(answerSource(nodes));
        Map<String, ObjectNode> outputs = new LinkedHashMap<>();
        Map<String, Object> variables = new LinkedHashMap<>(); // 会话变量等可写变量
        Queue<String> queue = new LinkedList<>();
        Set<String> visited = new LinkedHashSet<>();
        queue.add(startId);
        String answer = null;
        String error = null;
        while (!queue.isEmpty()) {
            String id = queue.poll();
            if (!visited.add(id)) continue;
            ObjectNode node = nodes.get(id);
            if (null == node) continue;
            long beginStep = System.currentTimeMillis();
            ObjectNode step = step(node, id);
            boolean failed = false;
            try {
                step.set("input", runtime.summary(node.at("/data")));
                runtime.lastRequest(null);
                // 实时进度：节点开始执行即推给前端（画布上执行中为蓝色），结束时按结果定型
                runtime.emitStep(step, "running");
                ObjectNode result = executeNode(node, inputs, outputs, variables, history);
                outputs.put(id, result);
                step.put("status", 1);
                step.set("output", runtime.summary(result));
                // 条件分支：分支结果写在 step.branch 上，供调度器决定后续节点
                if (result.has("branch")) step.put("branch", result.at("/branch").asText(""));
                if ("End".equals(type(node))) {
                    answer = result.at("/answer").asText("");
                }
            } catch (Exception e) {
                step.put("status", 2);
                step.put("error", runtime.message(e));
                step.set("output", runtime.summary(partial(e)));
                error = runtime.message(e);
                failed = true;
            }
            step.put("duration", System.currentTimeMillis() - beginStep);
            // 解析后的实际入参（节点拼好的请求体 / 提示词，或配置里的变量解析结果）
            step.set("request", request(node, inputs, outputs, variables));
            runtime.steps().add(step);
            runtime.emitStep(step, failed ? "failed" : "success");
            if (failed) break;
            // 条件分支：优先走标记为命中分支的边；没有匹配到边时按普通节点继续所有下游
            String branch = step.at("/branch").asText("");
            List<Map<String, String>> edges = targets.getOrDefault(id, Collections.emptyList());
            List<Map<String, String>> matched = new ArrayList<>();
            if (!DPUtil.empty(branch)) {
                for (Map<String, String> edge : edges) {
                    if (branch.equals(edge.get("label"))) matched.add(edge);
                }
            }
            for (Map<String, String> edge : matched.isEmpty() ? edges : matched) {
                if (!visited.contains(edge.get("target"))) queue.add(edge.get("target"));
            }
        }
        ObjectNode result = DPUtil.objectNode();
        result.put("status", null == error ? 1 : 2);
        result.put("error", null == error ? "" : error);
        // 结束节点位于容器（迭代/循环）内部时不会经过顶层调度：按执行顺序回退取最后一次结束节点的回复
        if (DPUtil.empty(answer)) answer = containerAnswer(outputs);
        result.put("answer", null == answer ? "" : answer);
        result.set("outputs", DPUtil.toJSON(outputs));
        result.set("conversation", DPUtil.toJSON(variables));
        result.set("steps", runtime.steps());
        // 回复里的图表：由「输出图表」节点按内置提示词让模型归纳出图表类型与数据，这里按执行顺序收集
        result.set("charts", charts(nodes, outputs));
        result.put("duration", System.currentTimeMillis() - begin);
        // 最终回复整段下发：模板拼装等非模型输出也走流式通道，前端直接把内容替换为最终结果
        if (!DPUtil.empty(answer)) runtime.emitAnswer(answer);
        return result;
    }

    public String type(ObjectNode node) {
        return node.at("/data/type").asText("");
    }

    /**
     * 容器（迭代/循环）内部结束节点的回复：容器内的节点由所在的容器调度执行，
     * 顶层调度只认顶层的结束节点，因此按步骤执行顺序回退取最后一次成功执行的结束节点回复。
     * 容器内节点每轮都会重新执行，取最后一次即为容器结束后应返回的回复。
     */
    protected String containerAnswer(Map<String, ObjectNode> outputs) {
        ArrayNode steps = runtime.steps();
        for (int i = steps.size() - 1; i >= 0; i--) {
            JsonNode step = steps.get(i);
            if (2 == step.at("/status").asInt(1)) continue;
            if (!"End".equals(step.at("/type").asText(""))) continue;
            ObjectNode output = outputs.get(step.at("/id").asText(""));
            if (null == output) continue;
            String answer = output.at("/answer").asText("");
            if (!DPUtil.empty(answer)) return answer;
        }
        return "";
    }

    /**
     * 回复里的图表：取「输出图表」节点的运行结果（图表类型与展示数据由该节点的模型归纳给出），
     * 按画布顺序收集，供前端跟在最终回复里展示（节点没跑到或执行失败时自然没有图表）。
     */
    protected ArrayNode charts(Map<String, ObjectNode> nodes, Map<String, ObjectNode> outputs) {
        ArrayNode charts = DPUtil.arrayNode();
        for (Map.Entry<String, ObjectNode> entry : nodes.entrySet()) {
            if (!"Chart".equals(type(entry.getValue()))) continue;
            ObjectNode output = outputs.get(entry.getKey());
            if (null == output) continue;
            // hasChart 由图表节点按本轮上下文判定（模型返回 type=none 时为 false），这里据此收集
            if (!output.at("/hasChart").asBoolean(!output.at("/series").isEmpty())) continue;
            charts.add(output);
        }
        return charts;
    }

    /**
     * 最终回复的来源节点：从结束节点「回复内容」的变量引用出发，沿引用向上找到产出回复文本的大语言模型节点。
     * 直接引用（`{{#n1.text#}}`）、模板拼装（`回答：{{#n1.text#}}`）、经模板节点中转都能识别；
     * 找不到模型节点时返回空串（没有可逐字流式的内容，只推送最终结果）。
     */
    protected String answerSource(Map<String, ObjectNode> nodes) {
        Queue<String> queue = new LinkedList<>();
        for (ObjectNode node : nodes.values()) {
            if ("End".equals(type(node))) queue.addAll(references(node.at("/data/template").asText("")));
        }
        Set<String> visited = new LinkedHashSet<>();
        while (!queue.isEmpty()) {
            String id = queue.poll();
            if (DPUtil.empty(id) || !visited.add(id)) continue;
            ObjectNode node = nodes.get(id);
            if (null == node) continue;
            if ("LLM".equals(type(node))) return id;
            // 继续向上找：被引用节点的配置里可能又引用了别的节点（如模板节点中转）
            queue.addAll(references(DPUtil.stringify(node.at("/data"))));
        }
        return "";
    }

    /** 文本里的变量引用（`{{#节点标识.变量名#}}`）对应的节点标识，按出现顺序去重 */
    protected List<String> references(String text) {
        List<String> list = new ArrayList<>();
        Matcher matcher = AgenticRuntime.TOKEN.matcher(DPUtil.parseString(text));
        while (matcher.find()) {
            String reference = matcher.group(1);
            int at = reference.lastIndexOf('.');
            if (at <= 0) continue;
            String id = reference.substring(0, at);
            if (!list.contains(id)) list.add(id);
        }
        return list;
    }

    /** 注入「编排工具」调用器（AgenticService 在运行前设置） */
    public void agenticInvoker(java.util.function.BiFunction<Integer, String, ObjectNode> invoker) {
        agenticInvokeTool.invoker(invoker);
    }

    /** 注入流式输出回调（/runStream 设置）：模型节点的增量实时推给前端 */
    public void streamSink(java.util.function.Consumer<JsonNode> sink) {
        runtime.streamSink(sink);
    }

    /** 当前流式回调：子编排（编排工具）执行时临时关闭，避免把内部节点的输出混进外层对话 */
    public java.util.function.Consumer<JsonNode> streamSink() {
        return runtime.streamSink();
    }

    /** 注入节点执行进度回调（/runStream 设置）：节点开始与结束时实时推给前端 */
    public void stepSink(java.util.function.Consumer<JsonNode> sink) {
        runtime.stepSink(sink);
    }

    /** 当前进度回调：子编排执行时临时关闭，避免子流程的节点状态混进外层画布 */
    public java.util.function.Consumer<JsonNode> stepSink() {
        return runtime.stepSink();
    }

    /** 注入 ReAct 轮次进度回调（/runStream、/invokeStream 设置）：每轮推理与每次工具调用实时推给前端 */
    public void roundSink(java.util.function.Consumer<JsonNode> sink) {
        runtime.roundSink(sink);
    }

    /** 当前轮次回调：子编排执行时临时关闭，避免子流程的 ReAct 轮次混进外层对话 */
    public java.util.function.Consumer<JsonNode> roundSink() {
        return runtime.roundSink();
    }

    /** 本轮已产生的步骤：运行中途抛异常时用于补写失败日志（步骤表按线程保存，取当前线程的即可） */
    public ArrayNode steps() {
        return runtime.steps();
    }

    /** 画布内容 → 节点表与连线表 */
    protected void read(JsonNode content, Map<String, ObjectNode> nodes, Map<String, List<Map<String, String>>> targets) {
        JsonNode cells = content.at("/cells");
        if (!cells.isArray()) return;
        for (JsonNode cell : cells) {
            String shape = cell.at("/shape").asText("");
            if (shape.endsWith("edge")) {
                String source = cell.at("/source/cell").asText("");
                String target = cell.at("/target/cell").asText("");
                if (DPUtil.empty(source) || DPUtil.empty(target)) continue;
                Map<String, String> edge = new LinkedHashMap<>();
                edge.put("target", target);
                // 分支标识取自连线起点锚点：条件分支 case-{caseId}、默认分支 default、问题分类器 class-{classId}
                edge.put("label", cell.at("/source/port").asText(cell.at("/data/name").asText("")));
                targets.computeIfAbsent(source, key -> new ArrayList<>()).add(edge);
            } else {
                nodes.put(cell.at("/id").asText(""), (ObjectNode) cell);
            }
        }
    }

    /** 单节点执行：按节点类型分派到对应的实现类 */
    protected ObjectNode executeNode(ObjectNode node, ObjectNode inputs, Map<String, ObjectNode> outputs,
                                     Map<String, Object> variables, ArrayNode history) throws Exception {
        String type = type(node);
        AgenticNodeHandler handler = handlers.get(type);
        if (null == handler) throw new UnsupportedOperationException("节点类型暂未支持：" + type);
        return handler.execute(new AgenticNodeContext(runtime, this, agenticToolService, node, inputs, outputs, variables, history));
    }

    /**
     * 执行容器内的子图：以容器为父节点、且在容器内没有入边的节点作为入口，按连线顺序执行；
     * 子图内节点的输出写入同一份节点输出表，容器内的变量引用（item / index / 循环变量）走作用域。
     */
    @Override
    public void runChildren(String containerId, Map<String, ObjectNode> outputs,
                            Map<String, Object> variables, ArrayNode history, int iteration) {
        List<String> entries = new ArrayList<>();
        for (Map.Entry<String, ObjectNode> entry : runtime.nodeMap().entrySet()) {
            if (!containerId.equals(entry.getValue().at("/parent").asText(""))) continue;
            boolean incoming = false;
            for (List<Map<String, String>> edges : runtime.edgeMap().values()) {
                for (Map<String, String> edge : edges) {
                    if (entry.getKey().equals(edge.get("target"))) incoming = true;
                }
            }
            if (!incoming) entries.add(entry.getKey());
        }
        if (entries.isEmpty()) throw new IllegalStateException("容器内暂无节点，请把需要重复执行的节点拖入容器内部");
        Queue<String> queue = new LinkedList<>(entries);
        Set<String> visited = new LinkedHashSet<>();
        ArrayNode parentSteps = runtime.steps();
        while (!queue.isEmpty()) {
            String id = queue.poll();
            if (!visited.add(id)) continue;
            ObjectNode node = runtime.nodeMap().get(id);
            if (null == node) continue;
            // 容器内节点同样记录步骤（记录所属容器，便于区分是哪次迭代/循环执行）
            ObjectNode step = step(node, id);
            step.put("container", containerId);
            step.put("iteration", iteration);
            step.set("input", runtime.summary(node.at("/data")));
            long beginStep = System.currentTimeMillis();
            boolean failed = false;
            String reason = "";
            try {
                runtime.lastRequest(null);
                // 容器内节点同样实时推送执行进度（带上容器与迭代序号，画布按同一条规则着色）
                runtime.emitStep(step, "running");
                ObjectNode value = executeNode(node, DPUtil.objectNode(), outputs, variables, history);
                outputs.put(id, value);
                step.put("status", 1);
                step.set("output", runtime.summary(value));
            } catch (Exception e) {
                step.put("status", 2);
                step.put("error", runtime.message(e));
                step.set("output", runtime.summary(partial(e)));
                failed = true;
                reason = runtime.message(e);
            }
            step.put("duration", System.currentTimeMillis() - beginStep);
            // 容器内节点同样记录解析后的实际入参
            step.set("request", request(node, DPUtil.objectNode(), outputs, variables));
            synchronized (parentSteps) {
                parentSteps.add(step);
            }
            runtime.emitStep(step, failed ? "failed" : "success");
            if (failed) {
                throw new IllegalStateException(node.at("/data/name").asText(id) + "：" + reason);
            }
            for (Map<String, String> edge : runtime.edgeMap().getOrDefault(id, Collections.emptyList())) {
                if (!visited.contains(edge.get("target"))) queue.add(edge.get("target"));
            }
        }
    }

    /* ------------------------------- 通用工具 ------------------------------- */

    /**
     * 失败节点的部分输出：节点抛错时可能已产生内容（如 ReAct 达到迭代上限时的调用链），
     * 随步骤一起记录，调试面板才能展示失败前的完整过程
     */
    protected JsonNode partial(Throwable throwable) {
        if (!(throwable instanceof AgenticNodeException)) return DPUtil.objectNode();
        JsonNode output = ((AgenticNodeException) throwable).output();
        return null == output ? DPUtil.objectNode() : output;
    }

    /** 步骤日志骨架：节点标识、类型与名称 */
    protected ObjectNode step(ObjectNode node, String id) {
        ObjectNode step = DPUtil.objectNode();
        step.put("id", id);
        step.put("type", type(node));
        step.put("name", node.at("/data/name").asText(id));
        return step;
    }

    /**
     * 解析后的实际入参：开始节点是本次运行入参；其余节点优先用节点自己记录的请求
     * （大语言模型/图表/HTTP 会记录拼好的提示词或请求体），没有记录时用配置的变量解析结果
     */
    protected JsonNode request(ObjectNode node, ObjectNode inputs, Map<String, ObjectNode> outputs,
                               Map<String, Object> variables) {
        try {
            if ("Start".equals(type(node))) return inputs.deepCopy();
            JsonNode recorded = runtime.lastRequest();
            return null == recorded ? runtime.resolve(node.at("/data"), outputs, variables) : recorded;
        } catch (Exception e) {
            // 记录入参失败不影响运行：退回节点配置原文
            return runtime.summary(node.at("/data"));
        }
    }

}
