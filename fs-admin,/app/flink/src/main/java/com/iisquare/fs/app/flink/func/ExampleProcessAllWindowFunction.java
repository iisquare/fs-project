package com.iisquare.fs.app.flink.func;

import com.google.common.collect.Lists;
import org.apache.flink.streaming.api.functions.windowing.ProcessAllWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.Window;
import org.apache.flink.util.Collector;

import java.util.List;

public class ExampleProcessAllWindowFunction<T, W extends Window> extends ProcessAllWindowFunction<T, List<T>, W> {
    @Override
    public void process(ProcessAllWindowFunction<T, List<T>, W>.Context context, Iterable<T> elements, Collector<List<T>> out) throws Exception {
        out.collect(Lists.newArrayList(elements));
    }
}
