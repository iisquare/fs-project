package com.iisquare.fs.app.flink.output;

import org.apache.flink.api.common.io.OutputFormat;
import org.apache.flink.configuration.Configuration;

import java.io.IOException;

public class EmptyOutput<T> implements OutputFormat<T> {

    @Override
    public void configure(Configuration parameters) {

    }

    @Override
    public void open(int taskNumber, int numTasks) throws IOException {

    }

    @Override
    public void writeRecord(T record) throws IOException {

    }

    @Override
    public void close() throws IOException {

    }
}
