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

import java.util.ArrayList;
import java.util.List;

/**
 * Flink 批处理分组聚合节点。
 *
 * groupBy 配置逗号分隔的字段名，aggregations 配置聚合表达式，如
 * count(*) AS total,sum(amount) AS amount_total。
 */
public class AggregateTransformNode extends AbstractConvertTransform<DataStream<Row>> {

    @Override
    public DAGType<DataStream<Row>> outputType() {
        return FlinkTypes.DATASTREAM;
    }

    @Override
    public DataStream<Row> process() throws Exception {
        FlinkRunner runner = runner(FlinkRunner.class);
        if (!runner.isBatch()) throw new IllegalArgumentException("分组聚合节点仅支持Flink批处理");
        DataStream<Row> stream = FlinkUtil.union(inputs());
        if (null == stream) throw new IllegalArgumentException("分组聚合节点缺少上游数据");
        String name = DPUtil.empty(getName()) ? "input" : getName();
        String view = queryView(name);
        StreamTableEnvironment table = runner.table();
        table.createTemporaryView(name, stream);
        Table result = table.sqlQuery(query(view,
                options.at("/groupBy").asText(""), options.at("/aggregations").asText("")));
        return table.toDataStream(result);
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
        String value = DPUtil.empty(name) ? "input" : name;
        return identifier(value);
    }

    static String identifier(String value) {
        return "`" + value.replace("`", "``") + "`";
    }
}
