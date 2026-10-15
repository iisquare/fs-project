package com.iisquare.fs.app.spark.stream;

import com.iisquare.fs.app.spark.core.SparkRunner;
import com.iisquare.fs.app.spark.core.SparkTypes;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.core.DAGType;
import com.iisquare.fs.base.dag.source.AbstractKafkaSource;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.DataStreamReader;

/**
 * 流式计算Kafka数据读取节点（Spark Structured Streaming）。
 *
 * 配置项：servers、topic、groupId、offset(latest/earliest)、checkpoint
 */
public class KafkaSourceNode extends AbstractKafkaSource<Dataset<Row>> {

    @Override
    public DAGType<Dataset<Row>> outputType() {
        return SparkTypes.DATASET;
    }

    @Override
    public Dataset<Row> process() {
        SparkSession session = runner(SparkRunner.class).session();
        DataStreamReader reader = session.readStream().format("kafka")
                .option("kafka.bootstrap.servers", servers())
                .option("subscribe", options.at("/topic").asText())
                .option("startingOffsets", DPUtil.empty(options.at("/offset").asText()) ? "latest" : options.at("/offset").asText());
        String group = options.at("/groupId").asText();
        if (!DPUtil.empty(group)) reader = reader.option("kafka.group.id", group);
        return reader.load();
    }

    public String servers() {
        String servers = options.at("/servers").asText();
        return DPUtil.empty(servers) ? options.at("/bootstrap").asText() : servers;
    }

}
