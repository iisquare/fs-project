package com.iisquare.fs.base.dag.sink;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.core.DAGSink;

public abstract class AbstractConsoleSink extends DAGSink {

    @Override
    public boolean configure(JsonNode... configs) {
        if (!options.at("/echoConfig").asBoolean(false)) return true;
        for (JsonNode config : configs) {
            System.out.println("Console Config:" + DPUtil.stringify(config));
        }
        return true;
    }

}
