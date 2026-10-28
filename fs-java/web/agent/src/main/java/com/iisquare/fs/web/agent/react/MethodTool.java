package com.iisquare.fs.web.agent.react;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.agent.core.AgenticRuntime;
import com.iisquare.fs.web.agent.entity.Tool;
import com.iisquare.fs.web.agent.entity.ToolMethod;
import com.iisquare.fs.web.agent.service.ToolMethodService;
import com.iisquare.fs.web.agent.service.ToolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工具方法（kind = method） - 外部调用：
 * 工具级 url / header / query 由工具配置提供，方法级参数按 in 落到 query / path / header / body；
 * 编排侧可以把某个参数（或对象参数的某个字段）绑定为固定值或变量，绑定的内容不再交给模型填写。
 */
@Service
public class MethodTool implements AgenticTool {

    @Autowired
    ToolService toolService;
    @Autowired
    ToolMethodService toolMethodService;

    @Override
    public String kind() {
        return "method";
    }

    @Override
    public ObjectNode definition(JsonNode item) {
        String methodName = item.at("/method").asText("");
        if (DPUtil.empty(methodName)) return null;
        // 发布内容里带了方法快照时直接用，草稿运行按工具标识实时解析
        JsonNode params = item.at("/methodParams");
        String description = item.at("/methodDescription").asText("");
        if (!params.isArray()) {
            ToolMethod method = method(item.at("/toolId").asInt(0), methodName);
            if (null == method) return null;
            params = DPUtil.toJSON(toolMethodService.methodParams(method));
            description = method.getDescription();
        }
        return AgenticTool.definition(methodName, description, (ArrayNode) params, item);
    }

    @Override
    public Object invoke(AgenticRuntime runtime, JsonNode item, Map<String, Object> args,
                         Map<String, ObjectNode> outputs, Map<String, Object> variables) {
        String methodName = item.at("/method").asText("");
        JsonNode params = item.at("/methodParams");
        ToolMethod method;
        Tool tool;
        if (params.isArray()) {
            // 发布内容里的方法快照：方法后续变动不影响已发布编排
            method = ToolMethod.builder().name(methodName)
                    .originName(item.at("/methodOriginName").asText(methodName))
                    .params(DPUtil.stringify(params))
                    .invoke(DPUtil.stringify(item.at("/methodInvoke")))
                    .build();
            tool = new Tool();
            tool.setType(item.at("/toolType").asText("schema"));
            tool.setUrl(item.at("/toolUrl").asText(""));
            tool.setHeader(item.at("/toolHeader").asText("{}"));
            tool.setQuery(item.at("/toolQuery").asText("{}"));
            tool.setContent(item.at("/toolContent").asText(""));
            tool.setPassSession(item.at("/toolPassSession").asInt(0) == 1 ? 1 : 0);
        } else {
            int toolId = item.at("/toolId").asInt(0);
            Tool info = toolService.info(toolId);
            if (null == info) throw new IllegalStateException("工具不存在：" + toolId);
            method = method(toolId, methodName);
            if (null == method) throw new IllegalStateException("方法不存在：" + methodName);
            tool = info;
        }
        // 模型参数 + 编排手工绑定的执行变量（对象参数按字段覆盖）
        Map<String, Object> finalArgs = new LinkedHashMap<>();
        if (null != args) finalArgs.putAll(args);
        JsonNode binding = item.at("/args");
        for (Map<String, Object> parameter : toolMethodService.methodParams(method)) {
            String name = DPUtil.parseString(parameter.get("name"));
            JsonNode itemBinding = binding.at("/" + name);
            Object properties = parameter.get("schema") instanceof Map
                    ? ((Map<?, ?>) parameter.get("schema")).get("properties") : null;
            if (properties instanceof Map) {
                // 对象参数：按字段覆盖，未指定的字段沿用模型给出的取值
                Map<String, Object> exists = finalArgs.get(name) instanceof Map
                        ? new LinkedHashMap<>((Map<String, Object>) finalArgs.get(name)) : new LinkedHashMap<>();
                for (Object field : ((Map<?, ?>) properties).keySet()) {
                    JsonNode fieldBinding = itemBinding.at("/fields/" + field);
                    if (fieldBinding.has("auto") && false == fieldBinding.at("/auto").asBoolean(true)) {
                        exists.put(DPUtil.parseString(field),
                                runtime.value(fieldBinding.at("/value").asText(""), outputs, variables));
                    }
                }
                finalArgs.put(name, exists);
                continue;
            }
            if (itemBinding.has("auto") && false == itemBinding.at("/auto").asBoolean(true)) {
                finalArgs.put(name, runtime.value(itemBinding.at("/value").asText(""), outputs, variables));
            }
        }
        Map<String, Object> result = toolMethodService.invoke(tool.getType(), tool.getUrl(), tool.getHeader(),
                tool.getQuery(), tool.getContent(), tool.getPassSession(), method, finalArgs);
        if (0 != DPUtil.parseInt(result.get("code"))) {
            throw new IllegalStateException(DPUtil.parseString(result.get("message")));
        }
        return result.get("data");
    }

    /** 按工具与方法名查方法：草稿运行时用 */
    protected ToolMethod method(int toolId, String name) {
        for (ToolMethod item : toolMethodService.all(toolId)) {
            if (name.equals(item.getName())) return item;
        }
        return null;
    }

}
