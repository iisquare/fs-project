package com.iisquare.fs.app.spark.batch;

import com.iisquare.fs.app.spark.util.SparkUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.transform.AbstractConvertTransform;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.types.StructType;

public class JSONStringifyTransformNode extends AbstractConvertTransform {

    @Override
    public Object process() {
        Dataset<Row> dataset = SparkUtil.union(Dataset.class, sources);
        StructType schema = dataset.schema();
        return dataset.map((MapFunction<Row, String>) row -> DPUtil.stringify(SparkUtil.row2json(schema, row)), Encoders.STRING());
    }

}
