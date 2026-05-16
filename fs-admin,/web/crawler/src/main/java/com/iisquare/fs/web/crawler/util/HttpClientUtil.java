package com.iisquare.fs.web.crawler.util;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.entity.DeflateDecompressingEntity;
import org.apache.http.client.entity.GzipDecompressingEntity;

public class HttpClientUtil {

    public static HttpEntity decode(HttpEntity entity) {
        Header encoding = entity.getContentEncoding();
        if (null == encoding) return entity;
        // br -> org.brotli:dec
        // zstd -> com.github.luben:zstd-jni
        return switch (encoding.getValue().toLowerCase()) {
            case "gzip" -> new GzipDecompressingEntity(entity);
            case "deflate" -> new DeflateDecompressingEntity(entity);
            default -> entity;
        };
    }

}
