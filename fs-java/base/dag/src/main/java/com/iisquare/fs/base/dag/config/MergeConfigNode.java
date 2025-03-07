package com.iisquare.fs.base.dag.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.core.DAGConfig;
import com.iisquare.fs.base.dag.util.DAGUtil;

import java.util.Arrays;

public class MergeConfigNode extends DAGConfig {
    @Override
    public boolean configure(JsonNode... configs) {
        String mergeType = options.at("/mergeType").asText("");
        JsonNode json = DPUtil.toJSON(null);
        switch (mergeType) {
            case "map":
                json = DAGUtil.mergeConfig(configs);
                break;
            case "array":
                json = DPUtil.arrayNode().addAll(Arrays.asList(configs));
                break;
        }
        result = DPUtil.value(DAGUtil.withArg(json, options.at("/arg").asText()), options.at("/echoPrefix").asText(""));
        return true;
    }

    @Override
    public Object process() {
        return result;
    }
}
