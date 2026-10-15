package com.iisquare.fs.app.flink.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.dag.core.DAGNode;
import com.iisquare.fs.base.dag.core.DAGRunner;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

import java.util.Map;

/**
 * Flink 执行器（流批统一）。
 *
 * Flink 2.x 移除了 DataSet API，流批均基于 DataStream API，通过执行环境上的
 * RuntimeExecutionMode（STREAMING/BATCH）区分；批处理场景的 SQL 算子借助 Table API 完成。
 *
 * 流作业在提交前无法采样数据，调试输出由基类按节点结果类型描述。
 */
public class FlinkRunner extends DAGRunner {

    private final StreamExecutionEnvironment environment;
    private StreamTableEnvironment tableEnvironment;

    public FlinkRunner(StreamExecutionEnvironment environment, JsonNode diagram, Map<String, Class<? extends DAGNode<?>>> nodes) {
        super(diagram, nodes);
        this.environment = environment;
    }

    public StreamExecutionEnvironment stream() {
        return environment;
    }

    /**
     * 批处理 SQL 算子使用的 Table 环境，按需创建并复用
     */
    public StreamTableEnvironment table() {
        if (null == tableEnvironment) {
            tableEnvironment = StreamTableEnvironment.create(environment);
        }
        return tableEnvironment;
    }

    public boolean isBatch() {
        return "batch".equals(diagram.at("/model").asText(""));
    }

}
