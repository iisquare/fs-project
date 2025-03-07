package com.iisquare.fs.base.dag.config;

import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.core.DAGConfig;

public class JSONConfigNode extends DAGConfig {
    @Override
    public Object process() {
        return DPUtil.parseJSON(options.at("/json").asText("{}"));
    }
}
