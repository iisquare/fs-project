package com.iisquare.fs.base.jpa.core;

import com.fasterxml.jackson.databind.node.ArrayNode;

public abstract class SQLBatchCallback {

    public int fetchSize = 1;
    public int batchSize = 200;
    public int executeCount = 0;

    public SQLBatchCallback() {}

    public SQLBatchCallback (int fetchSize, int batchSize) {
        this.fetchSize = fetchSize;
        this.batchSize = batchSize;
    }

    public SQLBatchCallback fetchSize(int fetchSize) {
        this.fetchSize = fetchSize;
        return this;
    }

    public SQLBatchCallback batchSize(int batchSize) {
        this.batchSize = batchSize;
        return this;
    }

    public boolean call(ArrayNode rows) throws Exception {
        executeCount++;
        return execute(rows);
    }

    public abstract boolean execute(ArrayNode rows) throws Exception;

}
