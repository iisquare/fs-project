package com.iisquare.fs.flink.plugins.core.output;

import org.apache.flink.api.common.io.OutputFormat;
import org.apache.flink.configuration.Configuration;

import java.io.IOException;
import java.util.Map;

public class EmptyOutput implements OutputFormat<Map<String, Object>> {
    @Override
    public void configure(Configuration parameters) {

    }

    @Override
    public void open(int taskNumber, int numTasks) throws IOException {

    }

    @Override
    public void writeRecord(Map<String, Object> record) throws IOException {

    }

    @Override
    public void close() throws IOException {

    }
}
