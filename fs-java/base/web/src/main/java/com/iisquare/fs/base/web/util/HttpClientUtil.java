package com.iisquare.fs.base.web.util;

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;

import java.util.HashMap;
import java.util.Map;

public class HttpClientUtil {

    public static Map<String, String> headers(CloseableHttpResponse response) {
        Map<String, String> headers = new HashMap<>();
        for (Header header : response.getAllHeaders()) {
            headers.put(header.getName(), header.getValue());
        }
        return headers;
    }

}
