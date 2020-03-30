package com.iisquare.fs.flink.flow;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.util.Map;

@Setter
@Getter
public abstract class StreamNode {

    protected Node current; // 所在节点实例
    protected DataStream<Map<String, Object>> result; // 执行结果
    protected StreamExecutionEnvironment environment;

    public abstract DataStream<Map<String, Object>> run(JsonNode config) throws Exception;

    public StreamExecutionEnvironment environment() {
        return environment;
    }

    public DataStream<Map<String, Object>> result() {
        if(null == result) return null;
        if(current.fieldReflect.size() < 1) return result;
        return result.map(current.fieldReflect());
    }

}
