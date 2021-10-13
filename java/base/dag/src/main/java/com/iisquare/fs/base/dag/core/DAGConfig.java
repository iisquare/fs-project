package com.iisquare.fs.base.dag.core;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public abstract class DAGConfig extends DAGNode {
    @Override
    public boolean configure(JsonNode... configs) {
        return true;
    }
}
