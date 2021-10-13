package com.iisquare.fs.app.flink.node;

import com.iisquare.fs.app.flink.output.EmptyOutput;
import com.iisquare.fs.base.dag.core.DAGNode;
import com.iisquare.fs.base.dag.sink.AbstractConsoleSink;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.streaming.api.datastream.DataStream;

public class ConsoleSinkNode extends AbstractConsoleSink {
    @Override
    public Object process() throws Exception {
        for (DAGNode source : sources) {
            Object result = source.result(Object.class);
            if (result instanceof DataSet) {
                DataSet batch = ((DataSet) result);
                batch.print();
                batch.output(new EmptyOutput());
                continue;
            }
            if (result instanceof DataStream) {
                ((DataStream) result).print();
                continue;
            }
            System.out.println(result.toString());
        }
        return null;
    }
}
