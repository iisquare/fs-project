package com.iisquare.fs.app.flink.stream;

import com.iisquare.fs.app.flink.core.FlinkRunner;
import com.iisquare.fs.app.flink.core.FlinkTypes;
import com.iisquare.fs.app.flink.util.CDCSupport;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.core.DAGType;
import com.iisquare.fs.base.dag.source.AbstractCDCSource;
import org.apache.flink.cdc.connectors.base.options.StartupOptions;
import org.apache.flink.cdc.connectors.mongodb.MongoDBSource;
import org.apache.flink.cdc.debezium.JsonDebeziumDeserializationSchema;
import org.apache.flink.streaming.api.datastream.DataStream;

/**
 * MongoDB CDC 读取节点（Flink CDC MongoDB change stream）。
 *
 * 建议连接 mongos 或副本集主节点，并保证账号具备 changeStream 权限。
 */
public class MongoCaptureNode extends AbstractCDCSource<DataStream<String>> {

    @Override
    public DAGType<DataStream<String>> outputType() {
        return FlinkTypes.DATASTREAM_STRING;
    }

    @Override
    public DataStream<String> process() {
        String hosts = options.at("/hosts").asText("");
        if (DPUtil.empty(hosts)) throw new IllegalArgumentException("MongoDB CDC未配置连接地址");
        MongoDBSource.Builder<String> builder = MongoDBSource.<String>builder()
                .hosts(hosts)
                .username(options.at("/username").asText(""))
                .password(options.at("/password").asText(""))
                .databaseList(CDCSupport.patterns(options, "databaseList", ".*"))
                .collectionList(CDCSupport.patterns(options, "collectionList", ".*"))
                .deserializer(new JsonDebeziumDeserializationSchema(options.at("/includeSchema").asBoolean(false)));
        String scheme = options.at("/scheme").asText("");
        if (!DPUtil.empty(scheme)) builder.scheme(scheme);
        builder.copyExisting(options.at("/copyExisting").asBoolean(true));
        String startup = options.at("/startup").asText("");
        builder.startupOptions(startup(startup));
        String connectionOptions = options.at("/connectionOptions").asText("");
        if (!DPUtil.empty(connectionOptions)) builder.connectionOptions(connectionOptions);
        int batchSize = options.at("/batchSize").asInt(0);
        if (batchSize > 0) builder.batchSize(batchSize);
        int pollAwaitTimeMillis = options.at("/pollAwaitTimeMillis").asInt(0);
        if (pollAwaitTimeMillis > 0) builder.pollAwaitTimeMillis(pollAwaitTimeMillis);
        int pollMaxBatchSize = options.at("/pollMaxBatchSize").asInt(0);
        if (pollMaxBatchSize > 0) builder.pollMaxBatchSize(pollMaxBatchSize);
        int heartbeatIntervalMillis = options.at("/heartbeatIntervalMillis").asInt(0);
        if (heartbeatIntervalMillis > 0) builder.heartbeatIntervalMillis(heartbeatIntervalMillis);
        if (options.has("scanFullChangelog")) {
            builder.scanFullChangelog(options.at("/scanFullChangelog").asBoolean(false));
        }
        return runner(FlinkRunner.class).stream()
                .addSource(builder.build()).name(getClass().getSimpleName())
                .setParallelism(parallelism);
    }

    public static StartupOptions startup(String value) {
        if ("initial".equalsIgnoreCase(value) || "snapshot".equalsIgnoreCase(value)) {
            return StartupOptions.initial();
        }
        if ("earliest".equalsIgnoreCase(value)) return StartupOptions.earliest();
        if ("latest".equalsIgnoreCase(value) || DPUtil.empty(value)) return StartupOptions.latest();
        if ("committed".equalsIgnoreCase(value)) return StartupOptions.committed();
        throw new IllegalArgumentException("MongoDB CDC启动位置仅支持initial、earliest、latest、committed");
    }
}
