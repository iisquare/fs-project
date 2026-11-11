package com.iisquare.fs.app.spark.stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.app.spark.util.SparkUtil;
import com.iisquare.fs.app.spark.core.SparkRunner;
import com.iisquare.fs.app.spark.core.SparkTypes;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.core.DAGType;
import com.iisquare.fs.base.dag.sink.AbstractKafkaSink;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.DataFrameWriter;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.streaming.DataStreamWriter;
import org.apache.spark.sql.functions;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Kafka写入节点（Spark Structured Streaming / Spark Batch）。
 *
 * 上游需为含 value（可选 key）字段的数据集；配置项：servers、topic、checkpoint。
 */
public class KafkaSinkNode extends AbstractKafkaSink<Object> {

    @Override
    public DAGType<Object> outputType() {
        return SparkTypes.OBJECT;
    }

    @Override
    public Object process() throws Exception {
        Dataset<Row> dataset = SparkUtil.union(inputs(SparkTypes.DATASET));
        if (null == dataset) return null;
        SparkRunner runner = runner(SparkRunner.class);
        List<String> columns = Arrays.asList(dataset.columns());
        if (!columns.contains("value")) {
            String messageField = options.at("/messageField").asText("message");
            if (DPUtil.empty(messageField)) {
                dataset = dataset.selectExpr("to_json(struct(*)) AS value");
            } else {
                if (!columns.contains(messageField)) {
                    throw new IllegalArgumentException("Kafka写入上游缺少 value 或消息字段：" + messageField);
                }
                dataset = dataset.withColumn("value", functions.col(messageField).cast("string"));
            }
            columns = Arrays.asList(dataset.columns());
        }
        String servers = options.at("/servers").asText();
        if (DPUtil.empty(servers)) servers = options.at("/bootstrap").asText();
        if (DPUtil.empty(servers)) throw new IllegalArgumentException("Kafka写入节点未配置服务地址");
        String checkpoint = options.at("/checkpoint").asText();
        if (DPUtil.empty(checkpoint)) checkpoint = "file:///tmp/fs-spark-kafka-" + safe(name);
        String topic = options.at("/topic").asText();
        String topicKey = options.at("/topicKey").asText();
        if (DPUtil.empty(topic) && DPUtil.empty(topicKey)) {
            throw new IllegalArgumentException("Kafka写入节点未配置主题");
        }
        if (!DPUtil.empty(topicKey)) {
            String messageField = options.at("/messageField").asText("message");
            String source = columns.contains(messageField) ? messageField
                    : (columns.contains("message") ? "message" : "value");
            if (!columns.contains(source)) {
                throw new IllegalArgumentException("Kafka动态主题缺少可用于解析的消息字段");
            }
            dataset = dataset.withColumn("topic", functions.get_json_object(
                    functions.col(source).cast("string"), "$." + topicKey));
        }
        if (runner.isStreaming()) {
            DataStreamWriter<Row> writer = dataset.writeStream().format("kafka")
                    .option("kafka.bootstrap.servers", servers)
                    .option("checkpointLocation", checkpoint);
            if (DPUtil.empty(topicKey)) writer.option("topic", topic);
            writer = kafkaOptions(writer);
            return writer.start();
        }
        DataFrameWriter<Row> writer = dataset.write().format("kafka")
                .option("kafka.bootstrap.servers", servers);
        if (DPUtil.empty(topicKey)) writer.option("topic", topic);
        writer = kafkaOptions(writer);
        writer.save();
        return null;
    }

    private DataStreamWriter<Row> kafkaOptions(DataStreamWriter<Row> writer) {
        String securityProtocol = options.at("/securityProtocol").asText("");
        String saslMechanism = options.at("/saslMechanism").asText("");
        String saslJaasConfig = options.at("/saslJaasConfig").asText("");
        int connectTimeout = options.at("/connectTimeout").asInt(0);
        if (connectTimeout > 0) {
            writer.option("kafka.socket.connection.setup.timeout.ms", String.valueOf(connectTimeout));
        }
        if (!DPUtil.empty(securityProtocol)) writer.option("kafka.security.protocol", securityProtocol);
        if (!DPUtil.empty(saslMechanism)) writer.option("kafka.sasl.mechanism", saslMechanism);
        if (!DPUtil.empty(saslJaasConfig)) writer.option("kafka.sasl.jaas.config", saslJaasConfig);
        JsonNode properties = options.get("producerProperties");
        if (properties != null && properties.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> iterator = properties.fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = iterator.next();
                writer.option("kafka." + entry.getKey(), entry.getValue().asText());
            }
        }
        return writer;
    }

    private DataFrameWriter<Row> kafkaOptions(DataFrameWriter<Row> writer) {
        String securityProtocol = options.at("/securityProtocol").asText("");
        String saslMechanism = options.at("/saslMechanism").asText("");
        String saslJaasConfig = options.at("/saslJaasConfig").asText("");
        int connectTimeout = options.at("/connectTimeout").asInt(0);
        if (connectTimeout > 0) {
            writer.option("kafka.socket.connection.setup.timeout.ms", String.valueOf(connectTimeout));
        }
        if (!DPUtil.empty(securityProtocol)) writer.option("kafka.security.protocol", securityProtocol);
        if (!DPUtil.empty(saslMechanism)) writer.option("kafka.sasl.mechanism", saslMechanism);
        if (!DPUtil.empty(saslJaasConfig)) writer.option("kafka.sasl.jaas.config", saslJaasConfig);
        JsonNode properties = options.get("producerProperties");
        if (properties != null && properties.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> iterator = properties.fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = iterator.next();
                writer.option("kafka." + entry.getKey(), entry.getValue().asText());
            }
        }
        return writer;
    }

    private String safe(String value) {
        return DPUtil.empty(value) ? "sink" : value.replaceAll("[^A-Za-z0-9_-]", "_");
    }

}
