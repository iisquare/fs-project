package com.iisquare.fs.web.cron.stage;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.HttpUtil;
import com.iisquare.fs.base.dag.util.DAGUtil;
import com.iisquare.fs.web.cron.core.Stage;

import java.util.Map;

public class APIConfigStage extends Stage {
    @Override
    public Map<String, Object> call() throws Exception {
        options = DAGUtil.formatOptions(options, config);
        String url = options.at("/url").asText();
        String checkField = options.at("/checkField").asText();
        String checkValue = options.at("/checkValue").asText();
        String dataField = options.at("/dataField").asText();
        if (DPUtil.empty(url)) {
            return ApiUtil.result(1001, "接口地址不能为空", "url empty:" + options.toPrettyString());
        }
        String result = HttpUtil.get(url);
        JsonNode json = DPUtil.parseJSON(result);
        if (null == json) {
            return ApiUtil.result(1002, "无法解析JSON配置", "parse response error:" + result);
        }
        if (!DPUtil.empty(checkField) && !checkValue.equals(json.at("/" + checkField).asText())) {
            return ApiUtil.result(1003, "配置状态异常", "response code error:" + json.toPrettyString());
        }
        JsonNode data = DPUtil.empty(dataField) ? json : json.at("/" + dataField);
        return ApiUtil.result(0, null, data);
    }
}
