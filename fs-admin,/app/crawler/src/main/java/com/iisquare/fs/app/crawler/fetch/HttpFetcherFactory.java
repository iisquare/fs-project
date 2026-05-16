package com.iisquare.fs.app.crawler.fetch;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

public class HttpFetcherFactory extends BasePooledObjectFactory<HttpFetcher> {

    @Override
    public HttpFetcher create() throws Exception {
        return new HttpFetcher();
    }

    @Override
    public PooledObject<HttpFetcher> wrap(HttpFetcher obj) {
        return new DefaultPooledObject<>(obj);
    }

    @Override
    public void destroyObject(PooledObject<HttpFetcher> p) throws Exception {
        p.getObject().close();
    }

    @Override
    public void passivateObject(PooledObject<HttpFetcher> p) throws Exception {
        p.getObject().reset();
    }
}
