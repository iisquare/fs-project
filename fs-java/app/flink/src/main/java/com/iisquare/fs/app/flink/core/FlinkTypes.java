package com.iisquare.fs.app.flink.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.dag.core.DAGType;
import org.apache.flink.core.execution.JobClient;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.types.Row;

/**
 * Flink 节点输出类型令牌集合。
 *
 * 流批统一基于 DataStream，运行时按原始类型校验，泛型实参以标签表达。
 */
public final class FlinkTypes {

    public static final DAGType<DataStream<Row>> DATASTREAM = DAGType.of(DataStream.class, "DataStream<Row>");
    public static final DAGType<DataStream<JsonNode>> DATASTREAM_JSON = DAGType.of(DataStream.class, "DataStream<JsonNode>");
    public static final DAGType<DataStream<String>> DATASTREAM_STRING = DAGType.of(DataStream.class, "DataStream<String>");
    public static final DAGType<Row> ROW = DAGType.of(Row.class, "Row");
    public static final DAGType<JobClient> CLIENT = DAGType.of(JobClient.class, "JobClient");
    public static final DAGType<Void> VOID = DAGType.of(Void.class, "Void");

    private FlinkTypes() {
    }

}
