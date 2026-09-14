package com.iisquare.fs.web.kg.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.kg.service.Neo4jService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/neo4j")
public class Neo4jController extends PermitControllerBase {

    @Autowired
    Neo4jService neo4jService;

    @RequestMapping("/showIndex")
    @Permission("kg:ontology:schema")
    public String showIndexAction(@RequestBody Map<String, Object> param) {
        Map<String, Object> result = neo4jService.showIndex(param);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/createIndex")
    @Permission("kg:ontology:schema")
    public String createIndexAction(@RequestBody Map<String, Object> param) {
        Map<String, Object> result = neo4jService.createIndex(param);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/dropIndex")
    @Permission("kg:ontology:schema")
    public String dropIndexAction(@RequestBody Map<String, Object> param) {
        Map<String, Object> result = neo4jService.dropIndex(param);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/showConstraint")
    @Permission("kg:ontology:schema")
    public String showConstraintAction(@RequestBody Map<String, Object> param) {
        Map<String, Object> result = neo4jService.showConstraint(param);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/createConstraint")
    @Permission("kg:ontology:schema")
    public String createConstraintAction(@RequestBody Map<String, Object> param) {
        Map<String, Object> result = neo4jService.createConstraint(param);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/dropConstraint")
    @Permission("kg:ontology:schema")
    public String dropConstraintAction(@RequestBody Map<String, Object> param) {
        Map<String, Object> result = neo4jService.dropConstraint(param);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/showSchema")
    @Permission("kg:ontology:schema")
    public String showSchemaAction(@RequestBody Map<String, Object> param) {
        Map<String, Object> result = neo4jService.showSchema(param);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/createSchema")
    @Permission("kg:ontology:schema")
    public String createSchemaAction(@RequestBody Map<String, Object> param) {
        Map<String, Object> result = neo4jService.createSchema(param);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/dropSchema")
    @Permission("kg:ontology:schema")
    public String dropSchemaAction(@RequestBody Map<String, Object> param) {
        Map<String, Object> result = neo4jService.dropSchema(param);
        return ApiUtil.echoResult(result);
    }

}
