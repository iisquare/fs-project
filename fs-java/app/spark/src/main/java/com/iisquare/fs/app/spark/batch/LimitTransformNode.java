package com.iisquare.fs.app.spark.batch;

import com.iisquare.fs.app.spark.core.SparkRunner;
import com.iisquare.fs.app.spark.core.SparkTypes;
import com.iisquare.fs.app.spark.util.SparkUtil;
import com.iisquare.fs.base.dag.core.DAGType;
import com.iisquare.fs.base.dag.transform.AbstractConvertTransform;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

/**
 * Spark 批处理行数限制节点。
 */
public class LimitTransformNode extends AbstractConvertTransform<Dataset<Row>> {

    @Override
    public DAGType<Dataset<Row>> outputType() {
        return SparkTypes.DATASET;
    }

    @Override
    public Dataset<Row> process() {
        if (runner(SparkRunner.class).isStreaming()) {
            throw new IllegalArgumentException("行数限制节点仅支持Spark批处理");
        }
        Dataset<Row> dataset = SparkUtil.union(inputs(SparkTypes.DATASET));
        if (null == dataset) throw new IllegalArgumentException("行数限制节点缺少上游数据");
        int limit = options.at("/limit").asInt(0);
        if (limit < 1) throw new IllegalArgumentException("行数限制必须大于0");
        return dataset.limit(limit);
    }
}
