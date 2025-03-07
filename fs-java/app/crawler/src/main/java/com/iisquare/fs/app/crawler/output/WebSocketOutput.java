package com.iisquare.fs.app.crawler.output;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public class WebSocketOutput extends Output {
    @Override
    public void configure(JsonNode parameters) {
        super.configure(parameters);
    }

    @Override
    public void open() throws IOException {
        super.open();
    }

    @Override
    public void record(JsonNode data) throws Exception {
        super.record(data);
    }

    @Override
    public void close() throws IOException {
        super.close();
    }
}
