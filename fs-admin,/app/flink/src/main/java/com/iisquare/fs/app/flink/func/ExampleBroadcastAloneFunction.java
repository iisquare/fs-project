package com.iisquare.fs.app.flink.func;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction;
import org.apache.flink.util.Collector;

import java.util.Map;

public class ExampleBroadcastAloneFunction extends BroadcastProcessFunction<ObjectNode, Map<Integer, String>, JsonNode> {

    private Map<Integer, String> dict; // 静态变量默认不能序列化，需要通过实例变量传递，并通过广播各自更新

    public ExampleBroadcastAloneFunction() {
        // 模拟初始化
        dict = DPUtil.buildMap(Integer.class, String.class, 1, "Yes", 2, "No");
    }

    @Override
    public void processElement(ObjectNode value, BroadcastProcessFunction<ObjectNode, Map<Integer, String>, JsonNode>.ReadOnlyContext ctx, Collector<JsonNode> out) throws Exception {
        // 一条数据在多个节点上只被处理一次
        System.out.printf("processElement[%d]:%s\n", Thread.currentThread().getId(), value);
        value.put("typeName", dict.get(value.at("/type").asInt()));
        out.collect(value);
    }

    @Override
    public void processBroadcastElement(Map<Integer, String> value, BroadcastProcessFunction<ObjectNode, Map<Integer, String>, JsonNode>.Context ctx, Collector<JsonNode> out) throws Exception {
        // 一条广播在多个节点的每个实例中都会接收到
        System.out.printf("processBroadcastElement[%d]:%s\n", Thread.currentThread().getId(), value);
        dict = value; // 如果通过接口等形式获取，需要控制好并发，避免重复处理
    }
}
