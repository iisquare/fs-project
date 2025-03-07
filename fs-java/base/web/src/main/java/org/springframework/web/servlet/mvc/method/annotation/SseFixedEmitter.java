package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * 若采用同步方式返回SseEmitter对象，或异步返回但业务执行时间过短，
 * ResponseBodyEmitterReturnValueHandler.handleReturnValue()在调用ResponseBodyEmitter.initialize()执行初始化时，
 * ResponseBodyEmitter.complete状态已完成，会导致onCompletion事件无法触发。
 * 通过重写initialize()方法，先注册回调，然后再执行handler.complete()，确保回调函数始终被触发一次。
 * 同时支持自定义MediaType功能，可在返回SseEmitter对象前，改变响应头的ContentType值。
 */
public class SseFixedEmitter extends SseEmitter {

    private final DefaultCallback timeoutCallback = new DefaultCallback();

    private final ErrorCallback errorCallback = new ErrorCallback();

    private final DefaultCallback completionCallback = new DefaultCallback();

    private MediaType contentMediaType = MediaType.TEXT_EVENT_STREAM;

    public SseFixedEmitter() {
        super();
    }

    public SseFixedEmitter(Long timeout) {
        super(timeout);
    }

    public SseFixedEmitter setMediaType(MediaType type) {
        this.contentMediaType = type;
        return this;
    }

    @Override
    protected void extendResponse(ServerHttpResponse response) {
        super.extendResponse(response);
        HttpHeaders headers = response.getHeaders();
        headers.setContentType(contentMediaType);
    }

    synchronized void initialize(Handler handler) throws IOException {
        handler.onTimeout(this.timeoutCallback);
        handler.onError(this.errorCallback);
        handler.onCompletion(this.completionCallback);
        super.initialize(handler);
    }

    @Override
    public synchronized void onTimeout(Runnable callback) {
        super.onTimeout(callback);
        timeoutCallback.setDelegate(callback);
    }

    @Override
    public synchronized void onError(Consumer<Throwable> callback) {
        super.onError(callback);
        errorCallback.setDelegate(callback);
    }

    @Override
    public synchronized void onCompletion(Runnable callback) {
        super.onCompletion(callback);
        completionCallback.setDelegate(callback);
    }

    private class DefaultCallback implements Runnable {

        @Nullable
        private Runnable delegate;

        public void setDelegate(Runnable delegate) {
            this.delegate = delegate;
        }

        @Override
        public void run() {
            if (this.delegate != null) {
                this.delegate.run();
            }
        }
    }


    private class ErrorCallback implements Consumer<Throwable> {

        @Nullable
        private Consumer<Throwable> delegate;

        public void setDelegate(Consumer<Throwable> callback) {
            this.delegate = callback;
        }

        @Override
        public void accept(Throwable t) {
            if (this.delegate != null) {
                this.delegate.accept(t);
            }
        }
    }
}
