package com.iisquare.fs.web.flink.service;

import org.apache.flink.api.common.RuntimeExecutionMode;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.CoreOptions;
import org.apache.flink.configuration.RestOptions;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.io.Closeable;
import java.io.IOException;

/**
 * Flink 本地执行环境。
 *
 * Flink 2.x 已移除 DataSet API，流批统一由 DataStream API 承载：
 * - stream：RuntimeExecutionMode.STREAMING
 * - batch：RuntimeExecutionMode.BATCH
 */
@Service
public class FlinkService implements InitializingBean, Closeable {

    private StreamExecutionEnvironment streamEnvironment = null;
    private StreamExecutionEnvironment batchEnvironment = null;

    @Override
    public void afterPropertiesSet() throws Exception {
        streamEnvironment = environment(RuntimeExecutionMode.STREAMING);
        batchEnvironment = environment(RuntimeExecutionMode.BATCH);
    }

    private StreamExecutionEnvironment environment(RuntimeExecutionMode mode) {
        Configuration config = new Configuration();
        config.set(CoreOptions.DEFAULT_PARALLELISM, 1);
        // Flink 2.x：Configuration#setString(String,String) 已移除，统一使用 ConfigOption
        config.set(RestOptions.BIND_PORT, "7080-7180");
        // 每个作业独立一个WebUi，执行execute方法会占用一个端口
        StreamExecutionEnvironment environment = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(config);
        environment.setRuntimeMode(mode);
        return environment;
    }

    @Override
    public void close() throws IOException {
    }

    /**
     * 按处理模式取执行环境：batch-批处理，其余-流处理
     */
    public StreamExecutionEnvironment environment(String model) {
        return "batch".equals(model) ? batchEnvironment : streamEnvironment;
    }

    public StreamExecutionEnvironment stream() {
        return streamEnvironment;
    }

    public StreamExecutionEnvironment batch() {
        return batchEnvironment;
    }

}
