package com.iisquare.fs.app.spark.core;

import com.iisquare.fs.base.dag.core.DAGType;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.streaming.StreamingQuery;

/**
 * Spark 节点输出类型令牌集合。
 *
 * 运行时只能校验原始类型（如 Dataset），泛型实参通过标签表达，用于错误信息与图校验。
 */
public final class SparkTypes {

    public static final DAGType<Dataset<Row>> DATASET = DAGType.of(Dataset.class, "Dataset<Row>");
    public static final DAGType<Dataset<String>> DATASET_JSON = DAGType.of(Dataset.class, "Dataset<String>");
    public static final DAGType<StreamingQuery> QUERY = DAGType.of(StreamingQuery.class, "StreamingQuery");
    public static final DAGType<Void> VOID = DAGType.of(Void.class, "Void");

    private SparkTypes() {
    }

}
