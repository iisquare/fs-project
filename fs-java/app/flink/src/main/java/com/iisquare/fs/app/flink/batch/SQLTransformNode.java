package com.iisquare.fs.app.flink.batch;

import com.iisquare.fs.app.flink.core.FlinkRunner;
import com.iisquare.fs.app.flink.core.FlinkTypes;
import com.iisquare.fs.base.dag.core.DAGNode;
import com.iisquare.fs.base.dag.core.DAGType;
import com.iisquare.fs.base.dag.transform.AbstractSQLTransform;
import org.apache.flink.types.Row;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

import java.util.List;

/**
 * 批处理SQL转换节点。
 *
 * 借助 Table API 在 BATCH 运行模式下执行SQL：上游 DataStream（Row）注册为临时视图，
 * SQL 结果再转回 DataStream 供下游节点使用。
 *
 * 视图以来源节点名称注册，便于SQL中按节点名称引用（与Spark侧保持一致）。
 */
public class SQLTransformNode extends AbstractSQLTransform<DataStream<Row>> {

    @Override
    public DAGType<DataStream<Row>> outputType() {
        return FlinkTypes.DATASTREAM;
    }

    @Override
    public DataStream<Row> process() throws Exception {
        FlinkRunner runner = runner(FlinkRunner.class);
        StreamTableEnvironment table = runner.table();
        List<DAGNode<?>> inputs = inputs();
        for (int index = 0; index < inputs.size(); index++) {
            table.createTemporaryView(inputs.get(index).getName(), input(index, FlinkTypes.DATASTREAM));
        }
        Table result = table.sqlQuery(options.at("/sql").asText());
        return table.toDataStream(result);
    }

}
