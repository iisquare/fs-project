package com.iisquare.fs.app.flink.tester;

import com.ververica.cdc.connectors.mongodb.MongoDBSource;
import com.ververica.cdc.debezium.JsonDebeziumDeserializationSchema;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.CoreOptions;
import org.apache.flink.configuration.RestOptions;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.junit.Test;

public class CDCTester {

    @Test
    public void mongoTest() throws Exception {
        Configuration config = new Configuration();
        config.setString(RestOptions.BIND_PORT,"8080-8089");
        config.setInteger(CoreOptions.DEFAULT_PARALLELISM, 1);
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(config);
        env.enableCheckpointing(1000); // 重启时需要手动加载检查点文件
        MongoDBSource.Builder<String> builder = MongoDBSource.<String>builder()
                .hosts("node101:27017") // 支持分片集群，指定为mongos查询路由服务
                .username("root")
                .password("admin888")
                .databaseList("fs_test")
                .collectionList(".*")
                .deserializer(new JsonDebeziumDeserializationSchema());
        DataStreamSource<String> source = env.addSource(builder.build());
        source.print();
        env.execute("cdc-mongo");
    }

}
