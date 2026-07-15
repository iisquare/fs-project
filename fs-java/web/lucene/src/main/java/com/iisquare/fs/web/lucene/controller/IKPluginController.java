package com.iisquare.fs.web.lucene.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.lucene.service.IKPluginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/ik")
public class IKPluginController extends PermitControllerBase {

    @Autowired
    IKPluginService ikPluginService;

    /**
     * 索引调试分析
     * 模拟索引过程，查看检索匹配结果
     */
    @PostMapping("/demo")
    @Permission
    public String demoAction(@RequestBody Map<String, Object> param) {
        Map<String, Object> result = ikPluginService.demo(param);
        return ApiUtil.echoResult(result);
    }

    /**
     * 获取分词结果
     * 查看指定文本的分词结果，包含偏移量和分词类型
     */
    @PostMapping("/index")
    @Permission
    public String indexAction(@RequestBody Map<String, Object> param) {
        Map<String, Object> result = ikPluginService.index(param);
        return ApiUtil.echoResult(result);
    }

    /**
     * 词典重新载入
     * 通知ES集群重新加载词典
     */
    @PostMapping("/reload")
    @Permission
    public String reloadAction(@RequestBody Map<String, Object> param) {
        Map<String, Object> result = ikPluginService.reload(param);
        return ApiUtil.echoResult(result);
    }

}
