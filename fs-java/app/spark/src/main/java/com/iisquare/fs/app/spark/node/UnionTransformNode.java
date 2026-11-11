package com.iisquare.fs.app.spark.node;

import com.iisquare.fs.app.spark.core.SparkTypes;
import com.iisquare.fs.app.spark.util.SparkUtil;
import com.iisquare.fs.base.dag.core.DAGType;
import com.iisquare.fs.base.dag.transform.AbstractUnionTransform;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

/**
 * 按入边顺序合并多个结构一致的 Spark Dataset，适用于批处理和流处理。
 */
public class UnionTransformNode extends AbstractUnionTransform<Dataset<Row>> {

    @Override
    public DAGType<Dataset<Row>> outputType() {
        return SparkTypes.DATASET;
    }

    @Override
    public Dataset<Row> process() {
        return SparkUtil.union(inputs(SparkTypes.DATASET));
    }
}
