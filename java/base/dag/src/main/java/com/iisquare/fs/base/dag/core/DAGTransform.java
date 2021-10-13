package com.iisquare.fs.base.dag.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.util.DAGUtil;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public abstract class DAGTransform extends DAGNode {
    protected String alias;
    protected String kvConfigPrefix;

    @Override
    public boolean configure(JsonNode... configs) {
        JsonNode config = DPUtil.value(DAGUtil.mergeConfig(configs), kvConfigPrefix);
        options = DAGUtil.formatOptions(options, config);
        return true;
    }
}
