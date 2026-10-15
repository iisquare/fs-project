package com.iisquare.fs.ext.flink;

import com.iisquare.fs.app.flink.core.FlinkRunner;
import com.iisquare.fs.base.dag.core.DAGTransform;
import com.iisquare.fs.base.dag.core.DAGType;
import org.apache.flink.streaming.api.datastream.DataStream;

import java.util.Arrays;
import java.util.List;

/**
 * 脚本插件示例：从集合构造有界数据流（流批通用）
 */
public class TestTransform extends DAGTransform<DataStream<Integer>> {

    @Override
    public DAGType<DataStream<Integer>> outputType() {
        return DAGType.of(DataStream.class, "DataStream<Integer>");
    }

    @Override
    public DataStream<Integer> process() throws Exception {
        List<Integer> data = Arrays.asList(1, 2, 3, 4, 5);
        return runner(FlinkRunner.class).stream().fromData(data);
    }
}
