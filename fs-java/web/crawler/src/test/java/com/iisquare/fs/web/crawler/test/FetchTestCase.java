package com.iisquare.fs.web.crawler.test;

import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.web.crawler.core.HttpFetcher;
import com.iisquare.fs.web.crawler.core.Worker;
import org.junit.After;
import org.junit.Test;

public class FetchTestCase implements AutoCloseable {

    HttpFetcher fetcher;

    public FetchTestCase() throws Exception {
        fetcher = new HttpFetcher();
    }

    @After
    @Override
    public void close() throws Exception {
        FileUtil.close(fetcher);
    }

    @Test
    public void forwardTest() {
        String url = "http://www.cma.gov.cn/";
        fetcher.headers(Worker.defaultHeaders());
        fetcher.url(url).get();
        System.out.println(fetcher.getLastStatus());
        System.out.println(fetcher.getLastLocation());
        System.out.println(fetcher.getLastResult());
    }

}
