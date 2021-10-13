package com.iisquare.fs.base.dag.config;

import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.core.DAGConfig;
import com.iisquare.fs.base.dag.util.DAGUtil;

import java.util.ArrayList;
import java.util.List;

public class NumberGenerateConfigNode extends DAGConfig {

    @Override
    public Object process() {
        int start = options.at("/start").asInt(0);
        int step = options.at("/step").asInt(0);
        int end = options.at("/end").asInt(0);
        int divisor = options.at("/divisor").asInt(0);
        List<Number> list = new ArrayList<>();
        for (int number = start; step !=0 && number <= end; number += step) {
            if (0 == divisor) {
                list.add(number);
            } else {
                list.add(Double.valueOf(number) / Double.valueOf(divisor));
            }
        }
        return DAGUtil.withArg(DPUtil.toJSON(list), options.at("/arg").asText());
    }
}
