package com.iisquare.fs.web.spider.core;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

public class WorkerFactory extends BasePooledObjectFactory<Worker> {

    public WorkerFactory() {}

    @Override
    public Worker create() throws Exception {
        return new Worker();
    }

    @Override
    public PooledObject<Worker> wrap(Worker obj) {
        return new DefaultPooledObject<>(obj);
    }

    @Override
    public void activateObject(PooledObject<Worker> p) throws Exception {
        Worker worker = p.getObject();
        worker.setThread(new Thread(worker));
    }

    @Override
    public void passivateObject(PooledObject<Worker> p) throws Exception {
        p.getObject().setThread(null);
    }
}
