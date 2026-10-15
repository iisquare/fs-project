package com.iisquare.fs.base.dag.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.core.DAGConfig;

public class JSONConfigNode extends DAGConfig {
    @Override
    public JsonNode process() {
        return DPUtil.parseJSON(options.at("/json").asText("{}"));
    }
}
