package com.iisquare.fs.app.flink.func;

import com.iisquare.fs.base.core.util.DPUtil;
import org.apache.flink.streaming.api.functions.source.SourceFunction;

public class ExampleRandomSourceFunction implements SourceFunction<String> {

    private volatile boolean isRunning = true;

    @Override
    public void run(SourceContext<String> ctx) throws Exception {
        while (isRunning) {
            Thread.sleep(DPUtil.random(100, 1000));
            ctx.collect(DPUtil.millis2dateTime(System.currentTimeMillis(), "yyyy-MM-dd HH:mm:ss.S"));
        }
    }

    @Override
    public void cancel() {
        isRunning = false;
    }
}
