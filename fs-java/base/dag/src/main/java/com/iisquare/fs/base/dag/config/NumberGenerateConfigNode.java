package com.iisquare.fs.base.dag.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.core.DAGConfig;
import com.iisquare.fs.base.dag.util.DAGUtil;

public class NumberGenerateConfigNode extends DAGConfig {

    @Override
    public Object process() {
        int start = options.at("/start").asInt(0);
        int step = options.at("/step").asInt(0);
        int end = options.at("/end").asInt(0);
        int divisor = options.at("/divisor").asInt(0);
        String inner = options.at("/inner").asText();
        String outer = options.at("/outer").asText();
        ArrayNode list = DPUtil.arrayNode();
        for (int number = start; step !=0 && number <= end; number += step) {
            JsonNode item = 0 == divisor ? DPUtil.toJSON(number) : DPUtil.toJSON((double) number / (double) divisor);
            list.add(DAGUtil.withArg(item, inner));
        }
        return DAGUtil.withArg(list, outer);
    }
}
