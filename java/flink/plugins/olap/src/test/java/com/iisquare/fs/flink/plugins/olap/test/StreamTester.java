package com.iisquare.fs.flink.plugins.olap.test;

import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.util.Arrays;

public class StreamTester {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        DataStreamSource<Integer> data = env.fromCollection(Arrays.asList(1, 2, 4, 5, 6));
        data.print();
        env.execute();
    }

}
