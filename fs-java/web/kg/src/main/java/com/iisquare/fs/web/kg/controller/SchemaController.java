package com.iisquare.fs.web.kg.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.kg.service.SchemaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 图数据库结构治理
 *
 * 索引与约束的全量管理、预检、登记与对账，不受本体定义限制。
 * 权限优先使用kg:schema资源，未配置时回退到kg:neo4j资源。
 */
@RestController
@RequestMapping("/schema")
public class SchemaController extends PermitControllerBase {

    @Autowired
    SchemaService schemaService;
    @Autowired
    DefaultRbacService rbacService;

    @RequestMapping("/capabilities")
    @Permission("kg:ontology:schema")
    public String capabilitiesAction(@RequestBody Map<String, Object> param) {
        return ApiUtil.echoResult(schemaService.capabilities(param));
    }

    @RequestMapping("/show")
    @Permission("kg:ontology:schema")
    public String showAction(@RequestBody Map<String, Object> param) {
        return ApiUtil.echoResult(schemaService.show(param));
    }

    @RequestMapping("/precheck")
    @Permission("kg:ontology:schema")
    public String precheckAction(@RequestBody Map<String, Object> param) {
        return ApiUtil.echoResult(schemaService.precheck(param));
    }

    @RequestMapping("/create")
    @Permission("kg:ontology:schema")
    public String createAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        param.put("uid", rbacService.uid(request));
        return ApiUtil.echoResult(schemaService.create(param));
    }

    @RequestMapping("/drop")
    @Permission("kg:ontology:schema")
    public String dropAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        param.put("uid", rbacService.uid(request));
        return ApiUtil.echoResult(schemaService.drop(param));
    }

    @RequestMapping("/batch")
    @Permission("kg:ontology:schema")
    public String batchAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        param.put("uid", rbacService.uid(request));
        return ApiUtil.echoResult(schemaService.batch(param));
    }

    @RequestMapping("/plan")
    @Permission("kg:ontology:schema")
    public String planAction(@RequestBody Map<String, Object> param) {
        return ApiUtil.echoResult(schemaService.plan(param));
    }

    @RequestMapping("/diff")
    @Permission("kg:ontology:schema")
    public String diffAction(@RequestBody Map<String, Object> param) {
        return ApiUtil.echoResult(schemaService.diff(param));
    }

    @RequestMapping("/apply")
    @Permission("kg:ontology:schema")
    public String applyAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        param.put("uid", rbacService.uid(request));
        return ApiUtil.echoResult(schemaService.apply(param));
    }

    @RequestMapping("/scan")
    @Permission("kg:ontology:schema")
    public String scanAction(@RequestBody Map<String, Object> param) {
        return ApiUtil.echoResult(schemaService.scan(param));
    }

    @RequestMapping("/attach")
    @Permission("kg:ontology:schema")
    public String attachAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        param.put("uid", rbacService.uid(request));
        return ApiUtil.echoResult(schemaService.attach(param));
    }

}
