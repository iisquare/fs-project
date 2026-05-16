package com.iisquare.fs.app.flink;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.app.flink.util.FlinkUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.ververica.cdc.connectors.mysql.source.MySqlSource;
import com.ververica.cdc.connectors.mysql.source.MySqlSourceBuilder;
import com.ververica.cdc.connectors.mysql.table.StartupOptions;
import com.ververica.cdc.debezium.JsonDebeziumDeserializationSchema;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.DataType;
import org.apache.flink.types.RowKind;
import org.apache.hudi.common.model.HoodieTableType;
import org.apache.hudi.configuration.FlinkOptions;
import org.apache.hudi.util.HoodiePipeline;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HudiTests {

    final String basePath = "file:///D:/htdocs/static/hudi";

    @Test
    public void cdcTest() throws Exception {
        List<HoodiePipeline.Builder> pipelines = Arrays.asList(HoodiePipeline.builder("t_word_acc")
                .column("name VARCHAR(64)")
                .column("score DOUBLE")
                .column("word_count BIGINT")
                .column("ts VARCHAR(64)")
                .pk("name")
                .partition("ts")
                .options(new LinkedHashMap<String, String>() {{
                    put(FlinkOptions.PATH.key(), basePath + "/t_word_acc");
                    put(FlinkOptions.TABLE_TYPE.key(), HoodieTableType.MERGE_ON_READ.name());
                    put(FlinkOptions.READ_AS_STREAMING.key(), "true");
                }}), HoodiePipeline.builder("t_word_gram")
                .column("name VARCHAR(64)")
                .column("title STRING")
                .column("ts VARCHAR(64)")
                .pk("name")
                .partition("ts")
                .options(new LinkedHashMap<String, String>() {{
                    put(FlinkOptions.PATH.key(), basePath + "/t_word_gram");
                    put(FlinkOptions.TABLE_TYPE.key(), HoodieTableType.MERGE_ON_READ.name());
                    put(FlinkOptions.READ_AS_STREAMING.key(), "true");
                }}));
        Configuration config = new Configuration();
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(config);
        MySqlSourceBuilder<String> builder = MySqlSource.<String>builder()
                .hostname("127.0.0.1").username("root").password("admin888")
                .databaseList("fs_test").tableList("fs_test.t_word_acc,fs_test.t_word_gram")
                .startupOptions(StartupOptions.latest())
                .serverTimeZone("Asia/Shanghai")
                .deserializer(new JsonDebeziumDeserializationSchema());
        DataStreamSource<String> source = env.fromSource(
                builder.build(), WatermarkStrategy.noWatermarks(), getClass().getSimpleName());
        SingleOutputStreamOperator<JsonNode> stream = source.map((MapFunction<String, JsonNode>) DPUtil::parseJSON);
        source.print();
        for (HoodiePipeline.Builder pipeline : pipelines) {
            String table = pipeline.getTableDescriptor().getTableId().getObjectName();
            SingleOutputStreamOperator<JsonNode> filter = stream.filter(
                    (FilterFunction<JsonNode>) record -> table.equals(record.at("/source/table").asText()));
            Map<String, DataType> columns = FlinkUtil.columns(pipeline);
            pipeline.sink(filter.map((MapFunction<JsonNode, RowData>) record -> {
                if (record.at("/after").isNull()) {
                    return GenericRowData.ofKind(RowKind.DELETE, FlinkUtil.row(columns, ts(record.at("/before"))));
                } else {
                    return GenericRowData.ofKind(RowKind.UPDATE_BEFORE, FlinkUtil.row(columns, ts(record.at("/after"))));
                }
            }), false);
        }
        env.execute();
    }

    public static ObjectNode ts(JsonNode node) {
        ObjectNode result = (ObjectNode) node;
        result.put("ts", "");
        return result;
    }

}
