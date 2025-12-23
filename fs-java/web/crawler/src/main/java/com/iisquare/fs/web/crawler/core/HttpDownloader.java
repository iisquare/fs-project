package com.iisquare.fs.web.crawler.core;

import org.apache.http.client.methods.CloseableHttpResponse;

public abstract class HttpDownloader {

    public abstract String download(HttpFetcher fetcher, CloseableHttpResponse response) throws Exception;

}
