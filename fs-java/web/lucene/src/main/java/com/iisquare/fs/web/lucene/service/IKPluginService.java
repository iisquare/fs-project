package com.iisquare.fs.web.lucene.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.elasticsearch.util.ElasticsearchUtil;
import org.elasticsearch.client.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class IKPluginService {

    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    ElasticsearchClient client;

    /**
     * 索引调试分析
     */
    public Map<String, Object> demo(Map<String, Object> param) {
        return request("/_plugin/analysis-ik-online/demo", param);
    }

    /**
     * 获取分词结果
     */
    public Map<String, Object> index(Map<String, Object> param) {
        return request("/_plugin/analysis-ik-online/index", param);
    }

    /**
     * 词典重新载入
     */
    public Map<String, Object> reload(Map<String, Object> param) {
        return request("/_plugin/analysis-ik-online/reload", param);
    }

    private Map<String, Object> request(String endpoint, Map<String, Object> body) {
        try {
            Response response = ElasticsearchUtil.perform(client, endpoint, "POST", DPUtil.stringify(body));
            return DPUtil.toJSON(DPUtil.parseJSON(ElasticsearchUtil.entity(response)), Map.class);
        } catch (Exception e) {
            logger.warn("analysis-ik-online request failed: {}", endpoint, e);
            return ApiUtil.result(17500, "调用插件失败", e.getMessage());
        }
    }

}
