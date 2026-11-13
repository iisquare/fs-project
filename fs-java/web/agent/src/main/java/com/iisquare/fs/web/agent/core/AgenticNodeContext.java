package com.iisquare.fs.web.agent.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.web.agent.entity.ToolMethod;

import java.util.List;
import java.util.Map;

/**
 * 节点执行上下文 - 节点实现类与运行时之间的唯一入口：
 * 1. 读取节点配置、运行入参、已执行节点输出、会话变量与历史对话；
 * 2. 变量解析（`{{#节点标识.变量名#}}`、系统变量、容器作用域）；
 * 3. 模型调用与工具调用（委托 AgenticRuntime）；
 * 4. 容器节点调度内部子图（委托 AgenticScheduler）。
 *
 * 每次节点执行创建一个实例，因此节点实现类本身可以安全地做成单例。
 */
public class AgenticNodeContext {

    private final AgenticRuntime runtime;
    private final AgenticScheduler scheduler;
    private final AgenticToolInvoker toolService;
    private final ObjectNode node;
    private final ObjectNode data;
    private final ObjectNode inputs;
    private final Map<String, ObjectNode> outputs;
    private final Map<String, Object> variables;
    private final ArrayNode history;

    public AgenticNodeContext(AgenticRuntime runtime, AgenticScheduler scheduler, AgenticToolInvoker toolService,
                              ObjectNode node, ObjectNode inputs,
                              Map<String, ObjectNode> outputs, Map<String, Object> variables, ArrayNode history) {
        this.runtime = runtime;
        this.scheduler = scheduler;
        this.toolService = toolService;
        this.node = node;
        this.data = runtime.object(node, "/data");
        this.inputs = inputs;
        this.outputs = outputs;
        this.variables = variables;
        this.history = null == history ? runtime.array(null, "/") : history;
        // 标记当前节点：流式增量带上节点标识，前端可据此区分是哪个节点在输出
        runtime.currentNode(this.node.at("/id").asText(""));
    }

    public AgenticRuntime runtime() {
        return runtime;
    }

    public AgenticScheduler scheduler() {
        return scheduler;
    }

    /** 画布节点（含 id / parent / data） */
    public ObjectNode node() {
        return node;
    }

    public String id() {
        return node.at("/id").asText("");
    }

    /** 节点配置（node.data） */
    public ObjectNode data() {
        return data;
    }

    /** 运行入参（开始节点声明的内容） */
    public ObjectNode inputs() {
        return inputs;
    }

    /** 已执行节点的输出表：节点标识 → 输出 */
    public Map<String, ObjectNode> outputs() {
        return outputs;
    }

    /** 会话变量（可写，变量赋值节点写入这里） */
    public Map<String, Object> variables() {
        return variables;
    }

    public ArrayNode history() {
        return history;
    }

    /** 执行容器内的子图（迭代、循环使用） */
    public void runChildren(String containerId, Map<String, ObjectNode> outputs, Map<String, Object> variables,
                            ArrayNode history, int iteration) throws Exception {
        scheduler.runChildren(containerId, outputs, variables, history, iteration);
    }

    public void runChildren(String containerId, int iteration) throws Exception {
        runChildren(containerId, outputs, variables, history, iteration);
    }

    /* ------------------------------- 变量解析 ------------------------------- */

    public Object value(String text) {
        return runtime.value(text, outputs, variables);
    }

    public Object value(String text, Map<String, ObjectNode> outputs, Map<String, Object> variables) {
        return runtime.value(text, outputs, variables);
    }

    public String text(String text) {
        return runtime.text(text, outputs, variables);
    }

    public String text(String text, Map<String, ObjectNode> outputs, Map<String, Object> variables) {
        return runtime.text(text, outputs, variables);
    }

    public Object reference(String reference) {
        return runtime.reference(reference, outputs, variables);
    }

    public Map<String, String> stringMap(JsonNode node) {
        return runtime.stringMap(node, outputs, variables);
    }

    public Object scoped(String scopeId, String reference) {
        return runtime.scoped(scopeId, reference);
    }

    public ObjectNode scopeOf(String name) {
        return runtime.scopeOf(name);
    }

    public String systemValue(String name) {
        return String.valueOf(runtime.systemValue(name));
    }

    /* ------------------------------- 画布与运行状态 ------------------------------- */

    public Map<String, ObjectNode> scopes() {
        return runtime.scopes();
    }

    public Map<String, Object> system() {
        return runtime.system();
    }

    public Map<String, ObjectNode> nodeMap() {
        return runtime.nodeMap();
    }

    public Map<String, List<Map<String, String>>> edgeMap() {
        return runtime.edgeMap();
    }

    public ObjectNode lastRequest() {
        return runtime.lastRequest();
    }

    public void lastRequest(ObjectNode value) {
        runtime.lastRequest(value);
    }

    /* ------------------------------- 通用工具 ------------------------------- */

    public ObjectNode object(JsonNode parent, String path) {
        return runtime.object(parent, path);
    }

    public ArrayNode array(JsonNode parent, String path) {
        return runtime.array(parent, path);
    }

    public boolean blank(Object value) {
        return runtime.blank(value);
    }

    public List<Object> list(Object value) {
        return runtime.list(value);
    }

    public Object field(Object item, String path) {
        return runtime.field(item, path);
    }

    public int compare(Object left, Object right) {
        return runtime.compare(left, right);
    }

    public boolean compareWith(String left, String operator, String right) {
        return runtime.compareWith(left, operator, right);
    }

    public JsonNode summary(JsonNode value) {
        return runtime.summary(value);
    }

    public String message(Throwable throwable) {
        return runtime.message(throwable);
    }

    /* ------------------------------- 模型调用 ------------------------------- */

    /** 模型网关地址未配置时直接报错（各模型节点入口先做一次） */
    public void checkGateway() {
        runtime.checkGateway();
    }

    public JsonNode completion(ObjectNode json) throws Exception {
        return runtime.completion(json);
    }

    public ObjectNode chat(ObjectNode data) {
        return runtime.chat(data, outputs, variables, history);
    }

    public ObjectNode chat(ObjectNode data, Map<String, ObjectNode> outputs, Map<String, Object> variables) {
        return runtime.chat(data, outputs, variables, history);
    }

    public ArrayNode historyMessages(ObjectNode data) {
        return runtime.historyMessages(history, data);
    }

    /* ------------------------------- 工具调用 ------------------------------- */

    public ArrayNode toolDefinitions(ObjectNode data) {
        return toolService.definitions(data.at("/tools"));
    }

    public Object invokeTool(ObjectNode data, String methodName, Map<String, Object> args) {
        return toolService.invoke(runtime, data.at("/tools"), methodName, args, outputs, variables);
    }

    public Map<String, Object> parseArguments(String text) {
        return runtime.parseArguments(text);
    }

}
