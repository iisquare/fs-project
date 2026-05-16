package com.iisquare.fs.app.crawler.trace;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.app.crawler.schedule.Job;
import com.iisquare.fs.app.crawler.schedule.Job;
import com.iisquare.fs.app.crawler.web.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Tracer {

    protected final static Logger logger = LoggerFactory.getLogger(Tracer.class);
    private static Tracer tracer = null;
    private static int LEVEL = 0;
    private static int LEVEL_TRACE = 1;
    private static int LEVEL_DEBUG = 2;
    private static int LEVEL_INFO = 4;
    private static int LEVEL_WARN = 8;
    private static int LEVEL_ERROR = 16;

    protected Tracer() {}

    public static Tracer tracer() {
        if (null != tracer) return tracer;
        synchronized (Tracer.class) {
            Configuration configuration = Configuration.getInstance();
            JsonNode config = configuration.config().at("/crawler/tracer");
            switch (config.at("/level").asText("").toLowerCase().trim()) {
                case "error":
                    LEVEL = LEVEL_ERROR;;
                    break;
                case "warn":
                    LEVEL = LEVEL_WARN | LEVEL_ERROR;;
                    break;
                case "info":
                    LEVEL = LEVEL_INFO | LEVEL_WARN | LEVEL_ERROR;;
                    break;
                case "debug":
                    LEVEL = LEVEL_DEBUG | LEVEL_INFO | LEVEL_WARN | LEVEL_ERROR;;
                    break;
                case "trace":
                    LEVEL = LEVEL_TRACE | LEVEL_DEBUG | LEVEL_INFO | LEVEL_WARN | LEVEL_ERROR;;
                    break;
            }
            Tracer instance;
            switch (config.at("/type").asText("")) {
                case "elasticsearch":
                    String uri = config.at("/uri").asText("http://127.0.0.1:9200/crawler-trace-{date}/default");
                    instance = new ElasticsearchTracer(configuration.nodeName(), uri);
                    break;
                default:
                    instance = new EmptyTracer();
            }
            tracer = instance;
        }
        return tracer;
    }

    public abstract void log(int level, String step, String message, Job job, int code, String content, JsonNode data, String status, Exception exception, String assist);

    public boolean isTraceEnabled() {
        return (LEVEL & LEVEL_TRACE) == LEVEL_TRACE;
    }

    public boolean isDebugEnabled() {
        return (LEVEL & LEVEL_DEBUG) == LEVEL_DEBUG;
    }

    public boolean isInfoEnabled() {
        return (LEVEL & LEVEL_INFO) == LEVEL_INFO;
    }

    public boolean isWarnEnabled() {
        return (LEVEL & LEVEL_WARN) == LEVEL_WARN;
    }

    public boolean isErrorEnabled() {
        return (LEVEL & LEVEL_ERROR) == LEVEL_ERROR;
    }

    public void trace(String step, String message, Job job, int code, String content, JsonNode data, String status, Exception exception, String assist) {
        if (!isTraceEnabled()) return;
        log(LEVEL_TRACE, step, message, job, code, content, data, status, exception, assist);
    }

    public void debug(String step, String message, Job job, int code, String content, JsonNode data, String status, Exception exception, String assist) {
        if (!isDebugEnabled()) return;
        log(LEVEL_DEBUG, step, message, job, code, content, data, status, exception, assist);
    }

    public void info(String step, String message, Job job, int code, String content, JsonNode data, String status, Exception exception, String assist) {
        if (!isInfoEnabled()) return;
        log(LEVEL_INFO, step, message, job, code, content, data, status, exception, assist);
    }

    public void warn(String step, String message, Job job, int code, String content, JsonNode data, String status, Exception exception, String assist) {
        if (!isWarnEnabled()) return;
        log(LEVEL_WARN, step, message, job, code, content, data, status, exception, assist);
    }

    public void error(String step, String message, Job job, int code, String content, JsonNode data, String status, Exception exception, String assist) {
        if (!isErrorEnabled()) return;
        log(LEVEL_ERROR, step, message, job, code, content, data, status, exception, assist);
    }

}
