package com.iisquare.fs.web.flink.service;

import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.CoreOptions;
import org.apache.flink.configuration.RestOptions;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.io.Closeable;
import java.io.IOException;

@Service
public class FlinkService implements InitializingBean, Closeable {

    private ExecutionEnvironment batchEnvironment = null;
    private StreamExecutionEnvironment streamEnvironment = null;

    @Override
    public void afterPropertiesSet() throws Exception {
        Configuration config = new Configuration();
        config.set(CoreOptions.DEFAULT_PARALLELISM, 1);
        config.setString(RestOptions.BIND_PORT, "7080-7180");
        // 每个作业独立一个WebUi，执行execute方法会占用一个端口
        batchEnvironment = ExecutionEnvironment.createLocalEnvironmentWithWebUI(config);
        streamEnvironment = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(config);
    }

    @Override
    public void close() throws IOException {
    }

    public ExecutionEnvironment batch() {
        return batchEnvironment;
    }

    public StreamExecutionEnvironment stream() {
        return streamEnvironment;
    }

}
