package com.iisquare.fs.flink.flow;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;

import java.util.Map;

@Setter
@Getter
public abstract class BatchNode {

    protected Node current; // 所在节点实例
    protected DataSet<Map<String, Object>> result; // 执行结果
    protected ExecutionEnvironment environment;

    public abstract DataSet<Map<String, Object>> run(JsonNode config) throws Exception;

    public ExecutionEnvironment environment() {
        return environment;
    }

    public DataSet<Map<String, Object>> result() {
        if(null == result) return null;
        if(current.fieldReflect.size() < 1) return result;
        return result.map(current.fieldReflect());
    }

}
