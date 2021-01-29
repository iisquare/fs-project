package com.iisquare.fs.spark.flow;

public abstract class Event {

    public void onNodeStart(Node node) {}

    public void onNodeEnd(Node node, Object result) throws Exception {
        if(null != result && (result instanceof Exception)) throw (Exception) result;
    }

}
