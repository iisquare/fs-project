package com.iisquare.fs.app.spark.batch;

import com.iisquare.fs.app.spark.core.SparkTypes;
import com.iisquare.fs.app.spark.util.SparkUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.core.DAGType;
import com.iisquare.fs.base.dag.transform.AbstractConvertTransform;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.types.StructType;

public class JSONStringifyTransformNode extends AbstractConvertTransform<Dataset<String>> {

    @Override
    public DAGType<Dataset<String>> outputType() {
        return SparkTypes.DATASET_JSON;
    }

    @Override
    public Dataset<String> process() {
        Dataset<Row> dataset = SparkUtil.union(inputs(SparkTypes.DATASET));
        StructType schema = dataset.schema();
        return dataset.map((MapFunction<Row, String>) row -> DPUtil.stringify(SparkUtil.row2json(schema, row)), Encoders.STRING());
    }

}
