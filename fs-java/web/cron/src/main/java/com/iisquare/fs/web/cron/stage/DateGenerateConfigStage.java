package com.iisquare.fs.web.cron.stage;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.DAGCore;
import com.iisquare.fs.web.cron.core.Stage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class DateGenerateConfigStage extends Stage {
    @Override
    public Map<String, Object> call() throws Exception {
        ObjectNode result = DPUtil.objectNode();
        String arg = options.at("/arg").asText("");
        String datetime = options.at("/datetime").asText();
        if (DPUtil.empty(datetime)) {
            result.put(arg, new Date().getTime());
            return ApiUtil.result(0, null, result);
        }
        String pattern = options.at("/pattern").asText();
        TimeZone timezone = DAGCore.timezone(options.at("/timezone").asText());
        Locale locale = DAGCore.locale(options.at("/locale").asText());
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern, locale);
        dateFormat.setTimeZone(timezone);
        result.put(arg, dateFormat.parse(datetime).getTime());
        return ApiUtil.result(0, null, result);
    }
}
