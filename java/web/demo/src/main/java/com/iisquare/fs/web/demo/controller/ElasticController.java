package com.iisquare.fs.web.demo.controller;

import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.web.mvc.ControllerBase;
import com.iisquare.fs.web.demo.elasticsearch.AccessLogES;
import com.iisquare.fs.web.demo.elasticsearch.DemoTestES;
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
    private DemoTestES testES;
    @Autowired
    private AccessLogES logES;

    @GetMapping("/create")
    public String createAction(@RequestParam Map<String, Object> param) {
        testES.version(DPUtil.parseInt(param.get("version")));
        boolean withAlias = !DPUtil.empty(param.get("withAlias"));
        return testES.create(withAlias);
    }

    @GetMapping("/template")
    public String templateAction(@RequestParam Map<String, Object> param) {
        boolean withAlias = !DPUtil.empty(param.get("withAlias"));
        return logES.template(withAlias);
    }

    @GetMapping("/alias")
    public String aliasAction(@RequestParam Map<String, Object> param) {
        int fromVersion = DPUtil.parseInt(param.get("fromVersion"));
        int toVersion = DPUtil.parseInt(param.get("toVersion"));
        return testES.alias(fromVersion, toVersion);
    }

}
