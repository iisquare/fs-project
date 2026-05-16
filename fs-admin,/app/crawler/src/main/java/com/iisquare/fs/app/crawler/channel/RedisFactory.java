package com.iisquare.fs.app.crawler.channel;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

public class RedisFactory extends BasePooledObjectFactory<StatefulRedisConnection<String, String>> {

    private RedisClient client;

    public RedisFactory(RedisClient client) {
        this.client = client;
    }

    @Override
    public StatefulRedisConnection<String, String> create() throws Exception {
        return client.connect();
    }

    @Override
    public PooledObject<StatefulRedisConnection<String, String>> wrap(StatefulRedisConnection<String, String> obj) {
        return new DefaultPooledObject<>(obj);
    }

    @Override
    public void destroyObject(PooledObject<StatefulRedisConnection<String, String>> p) throws Exception {
        p.getObject().close();
    }

    @Override
    public boolean validateObject(PooledObject<StatefulRedisConnection<String, String>> p) {
        return p.getObject().isOpen();
    }

}
