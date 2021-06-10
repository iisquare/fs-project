package com.iisquare.fs.web.demo.controller;

import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.web.mvc.ControllerBase;
import com.iisquare.fs.web.demo.elasticsearch.TestES;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RequestMapping("/elastic")
@RestController
public class ElasticController extends ControllerBase {

    @Autowired
    private TestES testES;

    /**
     * PUT /index_name
     * @see(https://www.elastic.co/guide/en/elasticsearch/reference/7.9/indices-put-mapping.html)
     */
    @GetMapping("/indicesCreate")
    public String indicesCreateAction(@RequestParam Map<String, Object> param) {
        int version = DPUtil.parseInt(param.get("version"));
        boolean withAlias = !DPUtil.empty(param.get("withAlias"));
        return testES.indicesCreate(version, withAlias);
    }

    /**
     * POST /_aliases
     * @see(https://www.elastic.co/guide/en/elasticsearch/reference/7.9/indices-aliases.html)
     */
    @GetMapping("/alias")
    public String aliasAction(@RequestParam Map<String, Object> param) {
        int fromVersion = DPUtil.parseInt(param.get("fromVersion"));
        int toVersion = DPUtil.parseInt(param.get("toVersion"));
        return testES.alias(fromVersion, toVersion);
    }

}
