package com.iisquare.fs.app.spark.batch;

import com.iisquare.fs.app.spark.core.SparkRunner;
import com.iisquare.fs.base.dag.core.DAGNode;
import com.iisquare.fs.base.dag.transform.AbstractConvertTransform;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.catalog.Catalog;

public class SQLTransformNode extends AbstractConvertTransform {
    @Override
    public Object process() throws Exception {
        SparkSession session = runner(SparkRunner.class).session();
        for (DAGNode source : sources) {
            source.result(Dataset.class).createOrReplaceTempView(source.getName());
        }
        Dataset<Row> dataset = session.sql(options.at("/sql").asText());
        Catalog catalog = session.catalog();
        for (DAGNode source : sources) {
            catalog.dropTempView(source.getName());
        }
        return dataset;
    }

}
