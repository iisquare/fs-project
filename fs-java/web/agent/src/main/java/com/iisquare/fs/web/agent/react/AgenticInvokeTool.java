package com.iisquare.fs.web.agent.react;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.agent.core.AgenticRuntime;
import com.iisquare.fs.web.agent.dao.AgenticDao;
import com.iisquare.fs.web.agent.entity.Agentic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.function.BiFunction;

/**
 * 编排应用（kind = agentic） - 内部调用：以模型给出的 query 作为开始节点入参，
 * 调用另一个编排的发布内容。子编排不写会话与运行日志，返回 { answer, status, error }。
 *
 * 调用器由 AgenticService 在运行前注入（回调），避免服务之间互相依赖。
 */
@Service
public class AgenticInvokeTool implements AgenticTool {

    @Autowired
    AgenticDao agenticDao;

    /** 编排调用器：agenticId + query → 运行结果（由 AgenticService 注入） */
    private final ThreadLocal<BiFunction<Integer, String, ObjectNode>> invokerThread = new ThreadLocal<>();

    public void invoker(BiFunction<Integer, String, ObjectNode> invoker) {
        invokerThread.set(invoker);
    }

    /** 当前编排调用器：并行迭代的子线程需要继承（ThreadLocal 不随线程传播） */
    public BiFunction<Integer, String, ObjectNode> invoker() {
        return invokerThread.get();
    }

    /** 调用其它编排应用（发布内容），返回 { answer, status, error } */
    public ObjectNode invokeAgentic(int agenticId, String query) {
        BiFunction<Integer, String, ObjectNode> invoker = invokerThread.get();
        if (null == invoker) throw new IllegalStateException("编排工具不可用：未初始化编排调用器");
        return invoker.apply(agenticId, query);
    }

    @Override
    public String kind() {
        return "agentic";
    }

    @Override
    public ObjectNode definition(JsonNode item) {
        String name = item.at("/name").asText("");
        if (DPUtil.empty(name)) return null;
        int agenticId = item.at("/agenticId").asInt(0);
        Agentic target = agenticId < 1 ? null : agenticDao.findById(agenticId).orElse(null);
        if (null == target || 1 != DPUtil.parseInt(target.getStatus())
                || DPUtil.parseInt(target.getPublishedVersion()) < 1
                || DPUtil.empty(target.getPublishedContent())) {
            return null;
        }
        if (item.isObject()) {
            ObjectNode value = (ObjectNode) item;
            value.put("agenticName", DPUtil.parseString(target.getName()));
            value.put("description", DPUtil.parseString(target.getDescription()));
        }
        ArrayNode params = DPUtil.arrayNode();
        params.add(AgenticTool.parameter("query", true, AgenticTool.schema("string", "交给该编排处理的问题或指令")));
        return AgenticTool.definition(name, params, item);
    }

    @Override
    public Object invoke(AgenticRuntime runtime, JsonNode item, Map<String, Object> args,
                         Map<String, ObjectNode> outputs, Map<String, Object> variables) {
        int agenticId = item.at("/agenticId").asInt(0);
        if (agenticId < 1) throw new IllegalStateException("编排工具未选择编排应用");
        return invokeAgentic(agenticId, DPUtil.parseString(args.get("query")));
    }

}
