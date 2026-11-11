package com.iisquare.fs.app.flink.node;

import com.iisquare.fs.app.flink.core.FlinkTypes;
import com.iisquare.fs.app.flink.util.FlinkUtil;
import com.iisquare.fs.base.dag.core.DAGType;
import com.iisquare.fs.base.dag.transform.AbstractUnionTransform;
import org.apache.flink.streaming.api.datastream.DataStream;

/**
 * 按入边顺序合并多个结构一致的 Flink DataStream，适用于流处理和批处理。
 */
public class UnionTransformNode extends AbstractUnionTransform<DataStream<Object>> {

    @Override
    public DAGType<DataStream<Object>> outputType() {
        return FlinkTypes.DATASTREAM_OBJECT;
    }

    @Override
    public DataStream<Object> process() {
        return FlinkUtil.union(inputs());
    }
}
