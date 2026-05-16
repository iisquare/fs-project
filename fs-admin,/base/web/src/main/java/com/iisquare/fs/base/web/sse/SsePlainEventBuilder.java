package com.iisquare.fs.base.web.sse;

import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class SsePlainEventBuilder implements SseEmitter.SseEventBuilder {

    private static final MediaType TEXT_PLAIN = new MediaType("text", "plain", StandardCharsets.UTF_8);
    private final Set<ResponseBodyEmitter.DataWithMediaType> dataToSend = new LinkedHashSet<>(4);

    @Nullable
    private StringBuilder sb;

    @Override
    public SseEmitter.SseEventBuilder id(String id) {
        append("id:").append(id).append("\n");
        return this;
    }

    @Override
    public SseEmitter.SseEventBuilder name(String name) {
        append("event:").append(name).append("\n");
        return this;
    }

    @Override
    public SseEmitter.SseEventBuilder reconnectTime(long reconnectTimeMillis) {
        append("retry:").append(String.valueOf(reconnectTimeMillis)).append("\n");
        return this;
    }

    @Override
    public SseEmitter.SseEventBuilder comment(String comment) {
        append(":").append(comment).append("\n");
        return this;
    }

    @Override
    public SseEmitter.SseEventBuilder data(Object object) {
        return data(object, null);
    }

    @Override
    public SseEmitter.SseEventBuilder data(Object object, @Nullable MediaType mediaType) {
        append("data:");
        saveAppendedText();
        this.dataToSend.add(new ResponseBodyEmitter.DataWithMediaType(object, mediaType));
        append("\n");
        return this;
    }

    SsePlainEventBuilder append(String text) {
        if (this.sb == null) {
            this.sb = new StringBuilder();
        }
        this.sb.append(text);
        return this;
    }

    public SsePlainEventBuilder line(String line) {
        append(line).append("\n");
        return this;
    }

    @Override
    public Set<ResponseBodyEmitter.DataWithMediaType> build() {
        if (!StringUtils.hasLength(this.sb) && this.dataToSend.isEmpty()) {
            return Collections.emptySet();
        }
        append("\n");
        saveAppendedText();
        return this.dataToSend;
    }

    private void saveAppendedText() {
        if (this.sb != null) {
            this.dataToSend.add(new ResponseBodyEmitter.DataWithMediaType(this.sb.toString(), TEXT_PLAIN));
            this.sb = null;
        }
    }
}
