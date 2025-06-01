package com.iisquare.fs.base.web.sse;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class SsePlainRequest implements Closeable {

    AtomicBoolean aborted = new AtomicBoolean(false);
    public Throwable failure = null; // 记录最终异常，一般为服务器端断开连接
    private HttpRequestBase request = null; // 请求单例

    /**
     * 中断处理请求
     */
    public void abort() {
        aborted.set(true);
        if (null != request) {
            // HttpClient连接池在关闭CloseableHttpResponse时并不会立即断开连接，会阻塞至响应输出完成
            // 对SSE代理服务而言，在客户端断开连接后，代理请求依然会持续执行至服务输出完成
            // 通过HttpRequestBase.abort()终止底层连接，立即关闭请求
            request.abort();
        }
    }

    public boolean isAborted() {
        return aborted.get();
    }

    public HttpRequestBase request(boolean single) throws Exception {
        if (single && null != request) {
            return request;
        }
        return this.request = request();
    }

    public abstract HttpRequestBase request() throws Exception;

    public boolean onMessage(CloseableHttpResponse response, String line, boolean isStream) {
        return true;
    }

    public void onError(CloseableHttpResponse response, Throwable throwable, boolean isStream) {}

    /**
     * 无论是否正常建立连接请求，需保障该方法始终触发
     */
    public void onClose() {}

    @Override
    public void close() throws IOException {
        this.onClose();
    }
}
