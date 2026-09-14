package com.iisquare.fs.web.kg.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.kg.service.ExtractService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 知识抽取
 *
 * 数据源、抽取任务、抽取候选与入图，候选一律人工确认后才写入图数据库。
 */
@RestController
@RequestMapping("/extract")
public class ExtractController extends PermitControllerBase {

    @Autowired
    ExtractService extractService;
    @Autowired
    DefaultRbacService rbacService;

    /* ---------------- 数据源 ---------------- */

    @RequestMapping("/sourceList")
    @Permission("kg:extract:")
    public String sourceListAction(@RequestBody Map<String, Object> param) {
        return ApiUtil.echoResult(extractService.sourceList(param));
    }

    @RequestMapping("/sourceInfo")
    @Permission("kg:extract:")
    public String sourceInfoAction(@RequestBody Map<String, Object> param) {
        return ApiUtil.echoResult(extractService.sourceInfo(param));
    }

    @RequestMapping("/sourceSave")
    @Permission({"kg:extract:add", "kg:extract:modify"})
    public String sourceSaveAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        param.put("uid", rbacService.uid(request));
        return ApiUtil.echoResult(extractService.sourceSave(param));
    }

    @RequestMapping("/sourceDelete")
    @Permission("kg:extract:delete")
    public String sourceDeleteAction(@RequestBody Map<String, Object> param) {
        return ApiUtil.echoResult(extractService.sourceDelete(param));
    }

    /**
     * 标记数据源处理状态（抽取入图后置为已处理）
     */
    @RequestMapping("/sourceMark")
    @Permission("kg:extract:run")
    public String sourceMarkAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        param.put("uid", rbacService.uid(request));
        return ApiUtil.echoResult(extractService.sourceMark(param));
    }

    /* ---------------- 抽取与入图 ---------------- */

    /**
     * 预览抽取：直接对传入文本抽取，不落库，用于工作台实时预览
     */
    @RequestMapping("/preview")
    @Permission("kg:extract:run")
    public String previewAction(@RequestBody Map<String, Object> param) {
        return ApiUtil.echoResult(extractService.preview(param));
    }

    /**
     * 入图：把工作台人工采纳的候选按主键 MERGE 写入图数据库
     */
    @RequestMapping("/apply")
    @Permission("kg:extract:apply")
    public String applyAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        param.put("uid", rbacService.uid(request));
        return ApiUtil.echoResult(extractService.apply(param));
    }

}
