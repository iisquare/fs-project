package com.iisquare.fs.app.spark.batch;

import com.iisquare.fs.app.spark.core.SparkRunner;
import com.iisquare.fs.app.spark.core.SparkTypes;
import com.iisquare.fs.app.spark.util.SparkUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.core.DAGType;
import com.iisquare.fs.base.dag.transform.AbstractConvertTransform;
import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.functions;

import java.util.Locale;

/**
 * Spark 批处理排序节点。
 */
public class SortTransformNode extends AbstractConvertTransform<Dataset<Row>> {

    @Override
    public DAGType<Dataset<Row>> outputType() {
        return SparkTypes.DATASET;
    }

    @Override
    public Dataset<Row> process() {
        if (runner(SparkRunner.class).isStreaming()) {
            throw new IllegalArgumentException("排序节点仅支持Spark批处理");
        }
        Dataset<Row> dataset = SparkUtil.union(inputs(SparkTypes.DATASET));
        if (null == dataset) throw new IllegalArgumentException("排序节点缺少上游数据");
        String field = options.at("/field").asText("").trim();
        if (DPUtil.empty(field)) throw new IllegalArgumentException("排序节点未配置排序字段");
        Column column = functions.col(field);
        boolean desc = "DESC".equals(options.at("/direction").asText("").toUpperCase(Locale.ROOT));
        return dataset.orderBy(desc ? column.desc() : column.asc());
    }
}
