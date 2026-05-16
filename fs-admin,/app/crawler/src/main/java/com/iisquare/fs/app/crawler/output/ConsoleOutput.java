package com.iisquare.fs.app.crawler.output;

import com.fasterxml.jackson.databind.JsonNode;

public class ConsoleOutput extends Output {
    @Override
    public void record(JsonNode data) throws Exception {
        System.out.println(data);
    }
}
