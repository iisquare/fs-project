package com.iisquare.fs.app.flink.batch;

import com.iisquare.fs.app.flink.core.FlinkRunner;
import com.iisquare.fs.app.flink.core.FlinkTypes;
import com.iisquare.fs.app.flink.util.FlinkUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.core.DAGType;
import com.iisquare.fs.base.dag.transform.AbstractConvertTransform;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.types.Row;

/**
 * Flink 批处理行数限制节点。
 */
public class LimitTransformNode extends AbstractConvertTransform<DataStream<Row>> {

    @Override
    public DAGType<DataStream<Row>> outputType() {
        return FlinkTypes.DATASTREAM;
    }

    @Override
    public DataStream<Row> process() throws Exception {
        FlinkRunner runner = runner(FlinkRunner.class);
        if (!runner.isBatch()) throw new IllegalArgumentException("行数限制节点仅支持Flink批处理");
        DataStream<Row> stream = FlinkUtil.union(inputs());
        if (null == stream) throw new IllegalArgumentException("行数限制节点缺少上游数据");
        int limit = options.at("/limit").asInt(0);
        if (limit < 1) throw new IllegalArgumentException("行数限制必须大于0");
        String name = DPUtil.empty(getName()) ? "input" : getName();
        StreamTableEnvironment table = runner.table();
        table.createTemporaryView(name, stream);
        String sql = "SELECT * FROM " + AggregateTransformNode.queryView(name)
                + " LIMIT " + limit;
        Table result = table.sqlQuery(sql);
        return table.toDataStream(result);
    }
}
