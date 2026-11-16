package com.iisquare.fs.web.agent.react;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.agent.core.AgenticRuntime;
import com.iisquare.fs.web.agent.core.AgenticToolInvoker;
import com.iisquare.fs.web.agent.service.ToolDescriptionService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ReAct 工具注册中心 - 按 kind 分派到对应的工具实现：
 * definitions() 生成模型可见的 function 定义，invoke() 按方法名找到工具项并执行。
 */
@Service
public class AgenticToolService implements AgenticToolInvoker {

    @Autowired
    List<AgenticTool> toolList;
    @Autowired
    ToolDescriptionService toolDescriptionService;
    private final Map<String, AgenticTool> tools = new LinkedHashMap<>();

    /** 建立 kind → 实现 的索引（kind 缺省为 method，兼容历史数据） */
    @PostConstruct
    protected void registry() {
        if (null == toolList) return;
        for (AgenticTool tool : toolList) tools.put(tool.kind(), tool);
    }

    /** 工具定义清单：同类工具批量生成，不可用的配置跳过 */
    public ArrayNode definitions(JsonNode items) {
        ArrayNode result = DPUtil.arrayNode();
        for (JsonNode slot : definitionSlots(items)) {
            result.add(slot.at("/definition"));
        }
        return result;
    }

    /**
     * 带配置下标的定义结果。同类工具只调用一次批量 definitions()，
     * BotService 可据此把服务端 ref 与最终 function 一一对应。
     */
    public ArrayNode definitionSlots(JsonNode items) {
        List<DefinitionItem> contexts = new ArrayList<>();
        Set<String> used = new HashSet<>();
        int index = 0;
        for (JsonNode item : items) {
            int itemIndex = index++;
            if (item.has("enabled") && !item.at("/enabled").asBoolean(true)) continue;
            String kind = item.at("/kind").asText("method");
            AgenticTool tool = tools.get(DPUtil.empty(kind) ? "method" : kind);
            if (null == tool) continue;
            // 名称按完整配置顺序分配，即使目标暂时不可用也不能跳过占位，
            // 否则定义与执行在“前一只工具失效”时会算出不同的后缀。
            String functionName = ToolFunctionName.unique(item, used);
            contexts.add(new DefinitionItem(itemIndex, item, tool, functionName));
        }

        Map<AgenticTool, List<DefinitionItem>> groups = new LinkedHashMap<>();
        for (DefinitionItem context : contexts) {
            groups.computeIfAbsent(context.tool, key -> new ArrayList<>()).add(context);
        }
        for (Map.Entry<AgenticTool, List<DefinitionItem>> entry : groups.entrySet()) {
            AgenticTool tool = entry.getKey();
            List<DefinitionItem> group = entry.getValue();
            ArrayNode batch = DPUtil.arrayNode();
            for (DefinitionItem context : group) batch.add(context.item);
            ArrayNode definitions;
            try {
                definitions = tool.definitions(batch);
            } catch (Exception batchError) {
                // 批量实现失败时退回逐项，单个失效配置不拖累同类型其他工具。
                definitions = DPUtil.arrayNode();
                for (DefinitionItem context : group) {
                    try {
                        definitions.add(tool.definition(context.item));
                    } catch (Exception ignored) {
                        definitions.addNull();
                    }
                }
            }
            for (int i = 0; i < group.size(); i++) {
                if (i >= definitions.size() || !definitions.get(i).isObject()) continue;
                group.get(i).definition = (ObjectNode) definitions.get(i);
            }
        }

        ArrayNode result = DPUtil.arrayNode();
        for (DefinitionItem context : contexts) {
            ObjectNode definition = context.definition;
            if (null == definition) continue;
            // 用户可以为内置工具改写名称；模型侧统一归一，并保证同一份清单内不重名。
            ((ObjectNode) definition.at("/function")).put("name", context.functionName);
            ObjectNode slot = result.addObject();
            slot.put("index", context.index);
            slot.put("functionName", context.functionName);
            slot.set("definition", toolDescriptionService.apply(definition, context.item));
        }
        return result;
    }

    private static class DefinitionItem {
        private final int index;
        private final JsonNode item;
        private final AgenticTool tool;
        private final String functionName;
        private ObjectNode definition;

        private DefinitionItem(int index, JsonNode item, AgenticTool tool, String functionName) {
            this.index = index;
            this.item = item;
            this.tool = tool;
            this.functionName = functionName;
        }
    }

    /** 执行工具调用：按方法名匹配工具项后交给对应实现 */
    public Object invoke(AgenticRuntime runtime, JsonNode items, String methodName, Map<String, Object> args,
                         Map<String, ObjectNode> outputs, Map<String, Object> variables) {
        return invoke(runtime, items, methodName, args, outputs, variables,
                ToolResultFormatter.FORMAT_FULL);
    }

    /**
     * 执行工具并按调用方要求生成模型视图。format 只作用于返回结果，不进入工具参数。
     */
    public Object invoke(AgenticRuntime runtime, JsonNode items, String methodName, Map<String, Object> args,
                         Map<String, ObjectNode> outputs, Map<String, Object> variables, String format) {
        Set<String> used = new HashSet<>();
        for (JsonNode item : items) {
            if (item.has("enabled") && !item.at("/enabled").asBoolean(true)) continue;
            String kind = item.at("/kind").asText("method");
            if (DPUtil.empty(kind)) kind = "method";
            AgenticTool tool = tools.get(kind);
            if (null == tool) continue;
            String name = ToolFunctionName.unique(item, used);
            if (!methodName.equals(name)) continue;
            try {
                Object value = tool.invoke(runtime, item, args, outputs, variables);
                return ToolResultFormatter.format(kind, value, format);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException(DPUtil.parseString(e.getMessage()));
            }
        }
        throw new IllegalStateException("未找到工具方法：" + methodName);
    }

}
