package com.iisquare.fs.app.flink.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.app.flink.func.ExampleBroadcastProcessFunction;
import com.iisquare.fs.app.flink.func.ExampleBroadcastAloneFunction;
import com.iisquare.fs.base.core.util.DPUtil;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.BroadcastStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.util.Arrays;
import java.util.Map;

public class BroadcastJob {
    public static void main(String[] args) throws Exception {
        Configuration config = new Configuration();
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment(config);
        DataStreamSource<ObjectNode> source = env.fromCollection(Arrays.asList(
                DPUtil.objectNode().put("id", 1).put( "type", 1).put("name", "a"),
                DPUtil.objectNode().put("id", 2).put( "type", 2).put("name", "b"),
                DPUtil.objectNode().put("id", 3).put( "type", 1).put("name", "c")
        ));
        DataStreamSource<Map<Integer, String>> dict = env.fromCollection(Arrays.asList(
                DPUtil.buildMap(Integer.class, String.class, 1, "Y", 2, "N"),
                DPUtil.buildMap(Integer.class, String.class, 1, "是", 2, "否")
        ));
        BroadcastStream<Map<Integer, String>> broadcast = dict.broadcast(ExampleBroadcastProcessFunction.descriptor);
        SingleOutputStreamOperator<JsonNode> dataset = source.connect(broadcast).process(new ExampleBroadcastAloneFunction());
        dataset.print();
        env.execute("broadcast-test");
    }
}
