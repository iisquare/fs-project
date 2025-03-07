package com.iisquare.fs.app.spark.node;

import com.iisquare.fs.base.dag.core.DAGNode;
import com.iisquare.fs.base.dag.sink.AbstractConsoleSink;
import org.apache.spark.sql.Dataset;

public class ConsoleSinkNode extends AbstractConsoleSink {
    @Override
    public Object process() {
        for (DAGNode source : sources) {
            Object result = source.result(Object.class);
            if (result instanceof Dataset) {
                Dataset dataset = ((Dataset) result);
                dataset.show();
                continue;
            }
            System.out.println(result.toString());
        }
        return null;
    }
}
