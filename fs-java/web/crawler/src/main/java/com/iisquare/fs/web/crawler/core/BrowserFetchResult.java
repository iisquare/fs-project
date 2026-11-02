package com.iisquare.fs.web.crawler.core;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BrowserFetchResult {

    private int status;
    private String result;
    private String location;
    private String exception;
    private String warning;
    private long coast;
    private String nodeId;
    private boolean retryable;
}
