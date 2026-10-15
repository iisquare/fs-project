package com.iisquare.fs.app.flink.node;

import com.iisquare.fs.app.flink.core.FlinkTypes;
import com.iisquare.fs.base.dag.core.DAGNode;
import com.iisquare.fs.base.dag.core.DAGType;
import com.iisquare.fs.base.dag.sink.AbstractConsoleSink;
import org.apache.flink.streaming.api.datastream.DataStream;

public class ConsoleSinkNode extends AbstractConsoleSink<Void> {

    @Override
    public DAGType<Void> outputType() {
        return FlinkTypes.VOID;
    }

    @Override
    public Void process() throws Exception {
        for (DAGNode<?> source : inputs()) {
            Object result = source.result();
            if (result instanceof DataStream) {
                ((DataStream) result).print().setParallelism(parallelism).name(getClass().getSimpleName());
                continue;
            }
            System.out.println(result);
        }
        return null;
    }
}
