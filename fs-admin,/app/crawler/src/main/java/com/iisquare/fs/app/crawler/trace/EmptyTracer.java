package com.iisquare.fs.app.crawler.trace;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.app.crawler.schedule.Job;
import com.iisquare.fs.app.crawler.schedule.Job;

public class EmptyTracer extends Tracer {

    @Override
    public void log(int level, String step, String message, Job job, int code, String content, JsonNode data, String status, Exception exception, String assist) {

    }
}
