package com.iisquare.fs.base.dag.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.HttpUtil;
import com.iisquare.fs.base.dag.core.DAGConfig;
import org.yaml.snakeyaml.Yaml;

import java.util.Base64;

public class ConsulConfigNode extends DAGConfig {

    @Override
    public Object process() throws Exception {
        String url = options.at("/url").asText();
        if (DPUtil.empty(url)) throw new RuntimeException("配置路径不能为空：" + DPUtil.stringify(options));
        String content = HttpUtil.get(url);
        if (null == content) return null;
        Yaml yaml = new Yaml();
        JsonNode kv = DPUtil.toJSON(yaml.load(content));
        String kvContent = new String(Base64.getDecoder().decode(kv.get(0).get("Value").asText()), "UTF-8");
        return DPUtil.toJSON(yaml.load(kvContent));
    }
}
