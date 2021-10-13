package com.iisquare.fs.base.dag.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.core.DAGConfig;
import com.iisquare.fs.base.dag.util.DAGUtil;

public class APIConfigNode extends DAGConfig {
    @Override
    public Object process() throws Exception {
        String url = options.at("/url").asText();
        String checkField = options.at("/checkField").asText();
        String checkValue = options.at("/checkValue").asText();
        String dataField = options.at("/dataField").asText();
        if (DPUtil.empty(url)) throw new RuntimeException("接口地址不能为空：" + DPUtil.stringify(options));
        String result = DAGUtil.loadFromUrl(url);
        JsonNode json = DPUtil.parseJSON(result);
        if (null == json) throw new RuntimeException("无法解析JSON配置：" + result);
        if (!DPUtil.empty(checkField) && !checkValue.equals(json.at("/" + checkField).asText())) {
            throw new RuntimeException("配置状态异常：" + DPUtil.stringify(json));
        }
        return DPUtil.empty(dataField) ? json : json.at("/" + dataField);
    }
}
