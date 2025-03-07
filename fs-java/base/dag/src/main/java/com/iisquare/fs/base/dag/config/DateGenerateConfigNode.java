package com.iisquare.fs.base.dag.config;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.DAGCore;
import com.iisquare.fs.base.dag.core.DAGConfig;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateGenerateConfigNode extends DAGConfig {

    @Override
    public Object process() throws Exception {
        ObjectNode result = DPUtil.objectNode();
        String arg = options.at("/arg").asText("");
        String datetime = options.at("/datetime").asText();
        if (DPUtil.empty(datetime)) {
            result.put(arg, new Date().getTime());
            return result;
        }
        String pattern = options.at("/pattern").asText();
        TimeZone timezone = DAGCore.timezone(options.at("/timezone").asText());
        Locale locale = DAGCore.locale(options.at("/locale").asText());
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern, locale);
        dateFormat.setTimeZone(timezone);
        result.put(arg, dateFormat.parse(datetime).getTime());
        return result;
    }
}
