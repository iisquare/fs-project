package com.iisquare.fs.app.spark.stream;

import com.iisquare.fs.app.spark.util.SparkUtil;
import com.iisquare.fs.app.spark.core.SparkTypes;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.core.DAGType;
import com.iisquare.fs.base.dag.sink.AbstractKafkaSink;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.streaming.StreamingQuery;

/**
 * 流式计算Kafka数据写入节点（Spark Structured Streaming）。
 *
 * 上游需为含 value（可选 key）字段的数据集；配置项：servers、topic、checkpoint。
 */
public class KafkaSinkNode extends AbstractKafkaSink<StreamingQuery> {

    @Override
    public DAGType<StreamingQuery> outputType() {
        return SparkTypes.QUERY;
    }

    @Override
    public StreamingQuery process() throws Exception {
        Dataset<Row> dataset = SparkUtil.union(inputs(SparkTypes.DATASET));
        if (null == dataset) return null;
        String servers = options.at("/servers").asText();
        if (DPUtil.empty(servers)) servers = options.at("/bootstrap").asText();
        String checkpoint = options.at("/checkpoint").asText();
        if (DPUtil.empty(checkpoint)) checkpoint = "file:///tmp/fs-spark-kafka-checkpoint";
        StreamingQuery query = dataset.writeStream().format("kafka")
                .option("kafka.bootstrap.servers", servers)
                .option("topic", options.at("/topic").asText())
                .option("checkpointLocation", checkpoint)
                .start();
        return query;
    }

}
