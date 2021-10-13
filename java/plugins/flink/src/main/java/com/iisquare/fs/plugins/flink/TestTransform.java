package com.iisquare.fs.plugins.flink;

import com.iisquare.fs.app.flink.core.FlinkRunner;
import com.iisquare.fs.base.dag.core.DAGTransform;

import java.util.Arrays;
import java.util.List;

public class TestTransform extends DAGTransform {
    @Override
    public Object process() throws Exception {
        List<Integer> data = Arrays.asList(1, 2, 3, 4, 5);
        return runner(FlinkRunner.class).batch().fromCollection(data);
    }
}
