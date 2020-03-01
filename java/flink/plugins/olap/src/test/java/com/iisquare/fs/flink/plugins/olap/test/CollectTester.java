package com.iisquare.fs.flink.plugins.olap.test;

import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.flink.plugins.core.output.EmptyOutput;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;

import java.util.Arrays;

public class CollectTester {

    public static void main(String[] args) throws Exception {
        ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
        DataSet<Integer> data = env.fromCollection(Arrays.asList(1, 2, 3, 4, 6, 7, 8));
        System.out.println(data.collect());
        System.out.println(env.getLastJobExecutionResult());
        data = data.map(new MapFunction<Integer, Integer>() {
            @Override
            public Integer map(Integer value) throws Exception {
                return value * 10;
            }
        });
        data.print();
        System.out.println(env.getLastJobExecutionResult());
        env.fromCollection(Arrays.asList(DPUtil.buildMap(String.class, Object.class))).output(new EmptyOutput()).name(EmptyOutput.class.getSimpleName());
        env.execute();
    }

}
