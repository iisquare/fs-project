package com.iisquare.fs.app.flink.stream;

import com.iisquare.fs.app.flink.core.FlinkRunner;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.source.AbstractKafkaSource;
import com.ververica.cdc.connectors.mysql.source.MySqlSource;
import com.ververica.cdc.connectors.mysql.source.MySqlSourceBuilder;
import com.ververica.cdc.connectors.mysql.table.StartupOptions;
import com.ververica.cdc.debezium.JsonDebeziumDeserializationSchema;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.datastream.DataStreamSource;

import java.util.Properties;

/**
 * @see(https://ververica.github.io/flink-cdc-connectors/master/content/connectors/mysql-cdc.html)
 */
public class MySQLCaptureNode extends AbstractKafkaSource {
    @Override
    public Object process() {
        String[] database = DPUtil.explode("\n", options.at("/database").asText());
        if (database.length == 0) database = new String[]{".*"};
        String[] table = DPUtil.explode("\n", options.at("/table").asText());
        if (table.length == 0) table = new String[]{".*"};
        MySqlSourceBuilder<String> builder = MySqlSource.<String>builder()
                .hostname(options.at("/hostname").asText())
                .port(options.at("/port").asInt(3306))
                .databaseList(database).tableList(table)
                .username(options.at("/username").asText())
                .password(options.at("/password").asText())
                .deserializer(new JsonDebeziumDeserializationSchema());
        String timeZone = options.at("/timeZone").asText();
        if (!DPUtil.empty(timeZone)) builder.serverTimeZone(timeZone);
        int poolSize = options.at("/poolSize").asInt(0);
        if (poolSize > 0) builder.connectionPoolSize(poolSize); // default 20
        int fetchSize = options.at("/fetchSize").asInt(0);
        if (fetchSize > 0) builder.fetchSize(fetchSize); // default 1024
        StartupOptions startup = startup(options.at("/startup").asText());
        if (null != startup) builder.startupOptions(startup);
        builder.jdbcProperties(properties(options.at("/jdbcProperties").asText()));
        builder.debeziumProperties(properties(options.at("/debeziumProperties").asText()));
        DataStreamSource<String> source = runner(FlinkRunner.class).stream()
                .fromSource(builder.build(), WatermarkStrategy.noWatermarks(), getClass().getSimpleName());
        return source;
    }

    public static StartupOptions startup(String startup) {
        if (DPUtil.empty(startup) || "latest".equals(startup)) {
            return StartupOptions.latest();
        }
        if ("initial".equals(startup)) return StartupOptions.initial();
        if ("earliest".equals(startup)) return StartupOptions.earliest();
        if (startup.startsWith("specific:")) {
            String[] strings = DPUtil.explode(",", startup.substring("specific:".length()));
            if (2 != strings.length) {
                throw new RuntimeException("startup specific offset should like specific:OffsetFile,OffsetPos");
            }
            return StartupOptions.specificOffset(strings[0], DPUtil.parseInt(strings[1]));
        }
        if (startup.startsWith("timestamp:")) {
            long timestamp = DPUtil.parseLong(startup.substring("timestamp:".length()));
            return StartupOptions.timestamp(timestamp);
        }
        if (startup.startsWith("datetime:")) {
            String format = "yyyy-MM-dd HH:mm:ss.S";
            long timestamp = DPUtil.dateTime2millis(startup.substring("datetime:".length()), format);
            return StartupOptions.timestamp(timestamp);
        }
        return null;
    }

    public static Properties properties(String uri) {
        Properties properties = new Properties();
        for (String kv : DPUtil.explode("&", uri)) {
            String[] strings = DPUtil.explode("=", kv);
            if (2 != strings.length) continue;
            properties.put(strings[0], strings[1]);
        }

        return properties;
    }
}
