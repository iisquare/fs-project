package com.iisquare.fs.base.web.sse;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;

public abstract class SsePlainRequest {

    private boolean aborted = false;
    public Throwable failure = null; // 记录最终异常，一般为服务器端断开连接

    /**
     * 中断处理请求
     */
    public void abort() {
        this.aborted = true;
    }

    public boolean isAborted() {
        return this.aborted;
    }

    public abstract HttpEntityEnclosingRequestBase request() throws Exception;

    public boolean onMessage(CloseableHttpResponse response, String line, boolean isStream) {
        return true;
    }

    public void onError(CloseableHttpResponse response, Throwable throwable, boolean isStream) {}

    public void onComplete() {}

}
