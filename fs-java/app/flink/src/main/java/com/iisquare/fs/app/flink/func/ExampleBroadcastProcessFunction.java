package com.iisquare.fs.app.flink.func;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.flink.api.common.state.BroadcastState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ReadOnlyBroadcastState;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction;
import org.apache.flink.util.Collector;

import java.util.Map;

public class ExampleBroadcastProcessFunction extends BroadcastProcessFunction<ObjectNode, Map<Integer, String>, JsonNode> {

    public static final String STATE_KEY = ExampleBroadcastProcessFunction.class.getName();

    public static MapStateDescriptor<String, Map<Integer, String>> descriptor =
            new MapStateDescriptor<>(ExampleBroadcastProcessFunction.class.getSimpleName(), Types.STRING, Types.MAP(Types.INT, Types.STRING));

    public ExampleBroadcastProcessFunction() {
        // 实例化一次，通过序列化分发到各个节点，每个并发反序列化一个独立的实例
    }

    @Override
    public void processElement(ObjectNode value, BroadcastProcessFunction<ObjectNode, Map<Integer, String>, JsonNode>.ReadOnlyContext ctx, Collector<JsonNode> out) throws Exception {
        // 一条数据在多个节点上只被处理一次
        System.out.printf("processElement[%d]:%s\n", Thread.currentThread().getId(), value);
        ReadOnlyBroadcastState<String, Map<Integer, String>> state = ctx.getBroadcastState(descriptor);
        Map<Integer, String> dict = state.get(STATE_KEY);
        if (null != dict) { // 广播流未到达前，可能取不到数据
            value.put("typeName", dict.get(value.at("/type").asInt()));
        }
        out.collect(value);
    }

    @Override
    public void processBroadcastElement(Map<Integer, String> value, BroadcastProcessFunction<ObjectNode, Map<Integer, String>, JsonNode>.Context ctx, Collector<JsonNode> out) throws Exception {
        // 一条广播在多个节点的每个实例中都会接收到
        System.out.printf("processBroadcastElement[%d]:%s\n", Thread.currentThread().getId(), value);
        BroadcastState<String, Map<Integer, String>> state = ctx.getBroadcastState(descriptor);
        state.put(STATE_KEY, value);
    }
}
