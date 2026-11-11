package com.iisquare.fs.app.spark.stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.app.spark.core.SparkRunner;
import com.iisquare.fs.app.spark.core.SparkTypes;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.core.DAGType;
import com.iisquare.fs.base.dag.source.AbstractKafkaSource;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.DataFrameReader;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.DataStreamReader;
import org.apache.spark.sql.functions;

import java.util.Iterator;
import java.util.Map;

/**
 * Kafka读取节点（Spark Structured Streaming / Spark Batch）。
 *
 * 流模式使用 startingOffsets；批模式同时使用 startingOffsets 与 endingOffsets 形成有界区间。
 */
public class KafkaSourceNode extends AbstractKafkaSource<Dataset<Row>> {

    @Override
    public DAGType<Dataset<Row>> outputType() {
        return SparkTypes.DATASET;
    }

    @Override
    public Dataset<Row> process() {
        if (DPUtil.empty(servers())) throw new IllegalArgumentException("Kafka读取节点未配置服务地址");
        if (DPUtil.empty(options.at("/topic").asText())) throw new IllegalArgumentException("Kafka读取节点未配置主题");
        SparkRunner runner = runner(SparkRunner.class);
        SparkSession session = runner.session();
        String startingOffsets = startingOffsets(runner.isStreaming());
        if (!runner.isStreaming()) {
            DataFrameReader reader = session.read().format("kafka")
                    .option("kafka.bootstrap.servers", servers())
                    .option("subscribe", options.at("/topic").asText())
                    .option("startingOffsets", startingOffsets)
                    .option("endingOffsets", endingOffsets());
            reader = kafkaOptions(reader);
            return reader.load().withColumn("message", functions.col("value").cast("string"));
        }
        DataStreamReader reader = session.readStream().format("kafka")
                .option("kafka.bootstrap.servers", servers())
                .option("subscribe", options.at("/topic").asText())
                .option("startingOffsets", startingOffsets);
        String group = options.at("/groupId").asText();
        if (!DPUtil.empty(group)) reader = reader.option("kafka.group.id", group);
        reader = kafkaOptions(reader);
        return reader.load().withColumn("message", functions.col("value").cast("string"));
    }

    public String startingOffsets(boolean streaming) {
        String value = options.at("/startingOffsets").asText("");
        if (!DPUtil.empty(value)) return value;
        value = options.at("/offset").asText("");
        if (!streaming && (DPUtil.empty(value) || "latest".equalsIgnoreCase(value))) return "earliest";
        return DPUtil.empty(value) ? "latest" : value;
    }

    public String endingOffsets() {
        String value = options.at("/endingOffsets").asText("");
        return DPUtil.empty(value) ? "latest" : value;
    }

    public String servers() {
        String servers = options.at("/servers").asText();
        return DPUtil.empty(servers) ? options.at("/bootstrap").asText() : servers;
    }

    private DataStreamReader kafkaOptions(DataStreamReader reader) {
        String securityProtocol = options.at("/securityProtocol").asText("");
        String saslMechanism = options.at("/saslMechanism").asText("");
        String saslJaasConfig = options.at("/saslJaasConfig").asText("");
        int connectTimeout = options.at("/connectTimeout").asInt(0);
        if (connectTimeout > 0) {
            reader = reader.option("kafka.socket.connection.setup.timeout.ms", String.valueOf(connectTimeout));
        }
        if (!DPUtil.empty(securityProtocol)) reader = reader.option("kafka.security.protocol", securityProtocol);
        if (!DPUtil.empty(saslMechanism)) reader = reader.option("kafka.sasl.mechanism", saslMechanism);
        if (!DPUtil.empty(saslJaasConfig)) reader = reader.option("kafka.sasl.jaas.config", saslJaasConfig);
        JsonNode properties = options.get("consumerProperties");
        if (properties != null && properties.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> iterator = properties.fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = iterator.next();
                reader = reader.option("kafka." + entry.getKey(), entry.getValue().asText());
            }
        }
        return reader;
    }

    private DataFrameReader kafkaOptions(DataFrameReader reader) {
        String securityProtocol = options.at("/securityProtocol").asText("");
        String saslMechanism = options.at("/saslMechanism").asText("");
        String saslJaasConfig = options.at("/saslJaasConfig").asText("");
        int connectTimeout = options.at("/connectTimeout").asInt(0);
        if (connectTimeout > 0) {
            reader = reader.option("kafka.socket.connection.setup.timeout.ms", String.valueOf(connectTimeout));
        }
        if (!DPUtil.empty(securityProtocol)) reader = reader.option("kafka.security.protocol", securityProtocol);
        if (!DPUtil.empty(saslMechanism)) reader = reader.option("kafka.sasl.mechanism", saslMechanism);
        if (!DPUtil.empty(saslJaasConfig)) reader = reader.option("kafka.sasl.jaas.config", saslJaasConfig);
        JsonNode properties = options.get("consumerProperties");
        if (properties != null && properties.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> iterator = properties.fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = iterator.next();
                reader = reader.option("kafka." + entry.getKey(), entry.getValue().asText());
            }
        }
        return reader;
    }

}
