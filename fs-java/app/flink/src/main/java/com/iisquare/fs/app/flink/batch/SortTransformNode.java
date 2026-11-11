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

import java.util.Locale;

/**
 * Flink 批处理排序节点。
 */
public class SortTransformNode extends AbstractConvertTransform<DataStream<Row>> {

    @Override
    public DAGType<DataStream<Row>> outputType() {
        return FlinkTypes.DATASTREAM;
    }

    @Override
    public DataStream<Row> process() throws Exception {
        FlinkRunner runner = runner(FlinkRunner.class);
        if (!runner.isBatch()) throw new IllegalArgumentException("排序节点仅支持Flink批处理");
        DataStream<Row> stream = FlinkUtil.union(inputs());
        if (null == stream) throw new IllegalArgumentException("排序节点缺少上游数据");
        String field = options.at("/field").asText("").trim();
        if (DPUtil.empty(field)) throw new IllegalArgumentException("排序节点未配置排序字段");
        String direction = "DESC".equals(options.at("/direction").asText("").toUpperCase(Locale.ROOT))
                ? "DESC" : "ASC";
        String name = DPUtil.empty(getName()) ? "input" : getName();
        StreamTableEnvironment table = runner.table();
        table.createTemporaryView(name, stream);
        String sql = "SELECT * FROM " + AggregateTransformNode.queryView(name)
                + " ORDER BY " + AggregateTransformNode.identifier(field) + " " + direction;
        Table result = table.sqlQuery(sql);
        return table.toDataStream(result);
    }
}
