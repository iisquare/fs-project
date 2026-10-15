package com.iisquare.fs.app.spark.node;

import com.iisquare.fs.app.spark.core.SparkRunner;
import com.iisquare.fs.app.spark.core.SparkTypes;
import com.iisquare.fs.base.dag.core.DAGNode;
import com.iisquare.fs.base.dag.core.DAGType;
import com.iisquare.fs.base.dag.sink.AbstractConsoleSink;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.streaming.StreamingQuery;

/**
 * 控制台输出节点：批处理直接展示，流式计算启动控制台流式查询
 */
public class ConsoleSinkNode extends AbstractConsoleSink<StreamingQuery> {

    @Override
    public DAGType<StreamingQuery> outputType() {
        return SparkTypes.QUERY;
    }

    @Override
    public StreamingQuery process() throws Exception {
        boolean streaming = runner(SparkRunner.class).isStreaming();
        StreamingQuery query = null;
        for (DAGNode<?> source : inputs()) {
            Object result = source.result();
            if (result instanceof Dataset) {
                Dataset dataset = ((Dataset) result);
                if (streaming) {
                    query = dataset.writeStream().format("console")
                            .option("truncate", false).start();
                    continue;
                }
                dataset.show();
                continue;
            }
            System.out.println(result.toString());
        }
        return query;
    }
}
