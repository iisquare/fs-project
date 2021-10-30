package com.iisquare.fs.base.dag.core;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Setter
@Getter
public abstract class DAGNode implements Serializable {

    protected String id;
    protected DAGRunner runner;
    protected JsonNode options;
    protected Set<DAGNode> sources = new HashSet<>();
    protected Set<DAGNode> targets = new HashSet<>();
    protected boolean isConfigured = false;
    protected boolean isProcessed = false;
    protected Object result = null;

    public abstract boolean configure(JsonNode... configs);

    public abstract Object process() throws Exception;

    protected  <T> T runner(Class<T> classType) {
        return (T) this.runner;
    }

    public <T> T result(Class<T> classType) {
        return (T) this.result;
    }

    public String alias() {
        if (this instanceof DAGSource) {
            return ((DAGSource) this).getAlias();
        }
        if (this instanceof DAGTransform) {
            return ((DAGTransform) this).getAlias();
        }
        return null;
    }

}
