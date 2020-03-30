package com.iisquare.fs.app.crawler.assist;

import com.fasterxml.jackson.databind.JsonNode;

public class HaltAssist extends Assist {

    private long time;

    @Override
    public void configure(JsonNode parameters) {
        time = parameters.at("/time").asLong(0);
    }

    @Override
    public String run() throws Exception {
        if (time > 0) {
            Thread.sleep(time);
            return String.valueOf(time);
        }
        return null;
    }
}
