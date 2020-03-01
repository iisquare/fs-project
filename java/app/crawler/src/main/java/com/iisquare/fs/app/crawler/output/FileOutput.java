package com.iisquare.fs.app.crawler.output;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

public class FileOutput extends Output {

    private String filename;
    private FileWriter writer;

    @Override
    public void configure(JsonNode parameters) {
        this.filename = parameters.get("filename").asText();
    }

    @Override
    public void open() throws IOException {
        writer = new FileWriter(filename, true);
    }

    @Override
    public void record(JsonNode data) throws Exception {
        Iterator<JsonNode> iterator = array(data).elements();
        while (iterator.hasNext()) {
            writer.write(DPUtil.stringify(iterator.next()));
            writer.write("\n");
        }
    }

    @Override
    public void close() throws IOException {
        writer.close();
        writer = null;
    }
}
