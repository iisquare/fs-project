package com.iisquare.fs.app.spark.batch;

import com.iisquare.fs.app.spark.core.SparkRunner;
import com.iisquare.fs.app.spark.core.SparkTypes;
import com.iisquare.fs.app.spark.util.SparkUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.core.DAGType;
import com.iisquare.fs.base.dag.transform.AbstractConvertTransform;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.catalog.Catalog;

import java.util.ArrayList;
import java.util.List;

/**
 * Spark 批处理分组聚合节点。
 */
public class AggregateTransformNode extends AbstractConvertTransform<Dataset<Row>> {

    @Override
    public DAGType<Dataset<Row>> outputType() {
        return SparkTypes.DATASET;
    }

    @Override
    public Dataset<Row> process() throws Exception {
        SparkRunner runner = runner(SparkRunner.class);
        if (runner.isStreaming()) throw new IllegalArgumentException("分组聚合节点仅支持Spark批处理");
        Dataset<Row> dataset = SparkUtil.union(inputs(SparkTypes.DATASET));
        if (null == dataset) throw new IllegalArgumentException("分组聚合节点缺少上游数据");
        SparkSession session = runner.session();
        String name = DPUtil.empty(getName()) ? "input" : getName();
        dataset.createOrReplaceTempView(name);
        try {
            return session.sql(query(queryView(name),
                    options.at("/groupBy").asText(""), options.at("/aggregations").asText("")));
        } finally {
            Catalog catalog = session.catalog();
            catalog.dropTempView(name);
        }
    }

    static String query(String view, String groupBy, String aggregations) {
        String grouped = aggregations.trim();
        if (DPUtil.empty(grouped)) throw new IllegalArgumentException("分组聚合节点未配置聚合表达式");
        List<String> groups = new ArrayList<>();
        for (String field : groupBy.split(",")) {
            field = field.trim();
            if (!DPUtil.empty(field)) groups.add(identifier(field));
        }
        String selection = String.join(",", groups);
        selection = DPUtil.empty(selection) ? grouped : selection + "," + grouped;
        String sql = "SELECT " + selection + " FROM " + view;
        if (!groups.isEmpty()) sql += " GROUP BY " + String.join(",", groups);
        return sql;
    }

    static String queryView(String name) {
        return identifier(DPUtil.empty(name) ? "input" : name);
    }

    static String identifier(String value) {
        return "`" + value.replace("`", "``") + "`";
    }
}
