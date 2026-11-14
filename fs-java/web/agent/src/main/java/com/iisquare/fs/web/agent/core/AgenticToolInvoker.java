package com.iisquare.fs.web.agent.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.Map;

/**
 * 工具入口 - 节点上下文只依赖这个接口（定义与调用），
 * 具体实现（按类型分派到 ReAct 工具）在 react 包里，避免 core 与 react 相互依赖。
 */
public interface AgenticToolInvoker {

    /** 生成模型可见的 function 定义清单 */
    ArrayNode definitions(JsonNode items);

    /** 执行工具调用：按方法名匹配工具项后交给对应实现 */
    Object invoke(AgenticRuntime runtime, JsonNode items, String methodName, Map<String, Object> args,
                  Map<String, com.fasterxml.jackson.databind.node.ObjectNode> outputs, Map<String, Object> variables);

    /**
     * 执行工具并指定返回格式。默认保持完整结果，兼容只关心原始数据的调用方。
     */
    default Object invoke(AgenticRuntime runtime, JsonNode items, String methodName, Map<String, Object> args,
                          Map<String, com.fasterxml.jackson.databind.node.ObjectNode> outputs,
                          Map<String, Object> variables, String format) {
        return invoke(runtime, items, methodName, args, outputs, variables);
    }

}
