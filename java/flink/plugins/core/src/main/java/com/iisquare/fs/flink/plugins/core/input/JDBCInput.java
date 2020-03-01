package com.iisquare.fs.flink.plugins.core.input;

import org.apache.flink.api.common.io.RichInputFormat;
import org.apache.flink.api.common.io.statistics.BaseStatistics;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.core.io.InputSplit;
import org.apache.flink.core.io.InputSplitAssigner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JDBCInput extends RichInputFormat<Map<String, Object>, MultiTableInputSplit> {

    protected static final Logger LOG = LoggerFactory.getLogger(MultiJDBCInput.class);
    private transient MultiTableInputSplit split;
    private Map<String, Object> jdbc;
    private transient Connection resource; // 当前连接资源
    private transient Statement statement; // 当前预处理对象
    private transient ResultSet result; // 当前查询结果集
    private transient ResultSetMetaData rsmd;

    public JDBCInput(Map<String, Object> jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void configure(Configuration parameters) {
        try {
            Class.forName(jdbc.get("driver").toString());
        } catch (ClassNotFoundException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    @Override
    public BaseStatistics getStatistics(BaseStatistics cachedStatistics) throws IOException {
        return null;
    }

    @Override
    public MultiTableInputSplit[] createInputSplits(int minNumSplits) throws IOException {
        List<MultiTableInputSplit> splits = new ArrayList<>();
        splits.add(new MultiTableInputSplit(splits.size(), jdbc));
        return splits.toArray(new MultiTableInputSplit[splits.size()]);
    }

    @Override
    public InputSplitAssigner getInputSplitAssigner(MultiTableInputSplit[] inputSplits) {
        return new InputSplitAssigner() {
            private int index = 0;
            @Override
            public InputSplit getNextInputSplit(String host, int taskId) {
                return (index < inputSplits.length) ? inputSplits[index++] : null;
            }
        };
    }

    @Override
    public void open(MultiTableInputSplit split) throws IOException {
        this.split = split;
        Map<String, Object> config = split.getConfig();
        try {
            resource = DriverManager.getConnection(config.get("url").toString(), config.get("username").toString(), config.get("password").toString());
        } catch (SQLException e) {
            throw new IOException(e.getMessage() + config, e);
        }
        try {
            statement = resource.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            statement.setFetchSize(Integer.MIN_VALUE);
        } catch (SQLException e) {
            throw new IOException(e.getMessage(), e);
        }
        try {
            result = statement.executeQuery(config.get("sql").toString());
        } catch (SQLException e) {
            throw new IOException(e.getMessage(), e);
        }
        try {
            rsmd = result.getMetaData();
        } catch (SQLException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public boolean reachedEnd() throws IOException {
        try {
            return null == result || !result.next();
        } catch (SQLException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public Map<String, Object> nextRecord(Map<String, Object> reuse) throws IOException {
        Map<String, Object> record = new LinkedHashMap<>();
        int count = 0;
        try {
            count = rsmd.getColumnCount();
            for (int i = 0; i < count; i++) {
                record.put(rsmd.getColumnLabel(i + 1), result.getObject(rsmd.getColumnLabel(i + 1)));
            }
        } catch (SQLException e) {
            throw new IOException(e.getMessage(), e);
        }
        Map<String, Object> config = split.getConfig();
        if(config.containsKey("fill")) {
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) config.get("fill")).entrySet()) {
                if (record.containsKey(entry.getKey())) continue;
                record.put(entry.getKey(), entry.getValue());
            }
        }
        return record;
    }

    @Override
    public void close() throws IOException {
        if(null != statement) {
            try {
                statement.close();
            } catch (SQLException e) {

            } finally {
                statement = null;
            }
        }
        if(null != resource) {
            try {
                resource.close();
            } catch (SQLException e) {

            } finally {
                resource = null;
            }
        }
    }

}
