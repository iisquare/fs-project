package com.iisquare.fs.app.spark.batch;

import com.iisquare.fs.app.spark.core.SparkRunner;
import com.iisquare.fs.app.spark.core.SparkTypes;
import com.iisquare.fs.base.dag.core.DAGNode;
import com.iisquare.fs.base.dag.core.DAGType;
import com.iisquare.fs.base.dag.transform.AbstractConvertTransform;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.catalog.Catalog;

import java.util.List;

public class SQLTransformNode extends AbstractConvertTransform<Dataset<Row>> {

    @Override
    public DAGType<Dataset<Row>> outputType() {
        return SparkTypes.DATASET;
    }

    @Override
    public Dataset<Row> process() throws Exception {
        SparkSession session = runner(SparkRunner.class).session();
        List<DAGNode<?>> inputs = inputs();
        for (int index = 0; index < inputs.size(); index++) {
            input(index, SparkTypes.DATASET).createOrReplaceTempView(inputs.get(index).getName());
        }
        Dataset<Row> dataset = session.sql(options.at("/sql").asText());
        Catalog catalog = session.catalog();
        for (DAGNode<?> source : inputs) {
            catalog.dropTempView(source.getName());
        }
        return dataset;
    }

}
