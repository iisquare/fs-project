package com.iisquare.fs.app.flink.stream;

import com.iisquare.fs.app.flink.core.FlinkRunner;
import com.iisquare.fs.app.flink.core.FlinkTypes;
import com.iisquare.fs.app.flink.util.CDCSupport;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.core.DAGType;
import com.iisquare.fs.base.dag.source.AbstractCDCSource;
import org.apache.flink.cdc.connectors.postgres.PostgreSQLSource;
import org.apache.flink.cdc.debezium.JsonDebeziumDeserializationSchema;
import org.apache.flink.streaming.api.datastream.DataStream;

import java.util.Properties;

/**
 * PostgreSQL CDC 读取节点（Flink CDC / Debezium PostgreSQL）。
 *
 * PostgreSQL 需要开启 logical replication，并按部署方式配置 wal_level、复制权限与发布槽。
 */
public class PostgreSQLCaptureNode extends AbstractCDCSource<DataStream<String>> {

    @Override
    public DAGType<DataStream<String>> outputType() {
        return FlinkTypes.DATASTREAM_STRING;
    }

    @Override
    public DataStream<String> process() {
        String database = options.at("/database").asText("");
        if (DPUtil.empty(database)) throw new IllegalArgumentException("PostgreSQL CDC未配置数据库");
        PostgreSQLSource.Builder<String> builder = PostgreSQLSource.<String>builder()
                .hostname(options.at("/hostname").asText())
                .port(options.at("/port").asInt(5432))
                .database(database)
                .schemaList(CDCSupport.patterns(options, "schemaList", "public"))
                .tableList(CDCSupport.patterns(options, "tableList", ".*"))
                .username(options.at("/username").asText())
                .password(options.at("/password").asText())
                .decodingPluginName(CDCSupport.text(options, "decodingPluginName", "pgoutput"))
                .deserializer(new JsonDebeziumDeserializationSchema(options.at("/includeSchema").asBoolean(true)));
        Properties properties = CDCSupport.properties(options, "debeziumProperties");
        String startup = options.at("/startup").asText("");
        if (!properties.containsKey("snapshot.mode")) {
            if ("latest".equalsIgnoreCase(startup)) {
                properties.put("snapshot.mode", "never");
            } else if ("initial".equalsIgnoreCase(startup) || "earliest".equalsIgnoreCase(startup)) {
                properties.put("snapshot.mode", "initial");
            }
        }
        String slotName = options.at("/slotName").asText("");
        if (DPUtil.empty(slotName)) slotName = "fs_" + safe(DPUtil.empty(id) ? name : id);
        builder.slotName(slotName.length() > 63 ? slotName.substring(0, 63) : slotName);
        builder.debeziumProperties(properties);
        return runner(FlinkRunner.class).stream()
                .addSource(builder.build()).name(getClass().getSimpleName())
                .setParallelism(parallelism);
    }

    private String safe(String value) {
        String result = DPUtil.empty(value) ? "cdc" : value.toLowerCase().replaceAll("[^a-z0-9_]", "_");
        return result.replaceAll("_+", "_");
    }
}
