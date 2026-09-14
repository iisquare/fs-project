package com.iisquare.fs.web.kg.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.kg.service.DataLogService;
import com.iisquare.fs.web.kg.service.DataQueryService;
import com.iisquare.fs.web.kg.service.GraphDataService;
import com.iisquare.fs.web.kg.util.ExcelUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 图数据管理
 *
 * 基于本体定义管理图数据库中的实体、关系与属性数据。
 * 权限优先使用kg:graph资源，未配置时回退到kg:ontology资源。
 */
@RestController
@RequestMapping("/graph")
public class GraphDataController extends PermitControllerBase {

    @Autowired
    GraphDataService graphDataService;
    @Autowired
    DataLogService dataLogService;
    @Autowired
    DataQueryService dataQueryService;
    @Autowired
    DefaultRbacService rbacService;

    @RequestMapping("/summary")
    @Permission("kg:graph:")
    public String summaryAction(@RequestBody Map<String, Object> param) {
        return ApiUtil.echoResult(graphDataService.summary(param));
    }

    @RequestMapping("/search")
    @Permission("kg:graph:")
    public String searchAction(@RequestBody Map<String, Object> param) {
        return ApiUtil.echoResult(graphDataService.search(param));
    }

    @RequestMapping("/info")
    @Permission("kg:graph:")
    public String infoAction(@RequestBody Map<String, Object> param) {
        return ApiUtil.echoResult(graphDataService.info(param));
    }

    @RequestMapping("/save")
    @Permission({"kg:graph:add", "kg:graph:modify"})
    public String saveAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        param.put("uid", rbacService.uid(request));
        return ApiUtil.echoResult(graphDataService.save(param));
    }

    @RequestMapping("/batch")
    @Permission({"kg:graph:add", "kg:graph:modify"})
    public String batchAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        param.put("uid", rbacService.uid(request));
        return ApiUtil.echoResult(graphDataService.batch(param));
    }

    @RequestMapping("/export")
    @Permission("kg:graph:")
    public String exportAction(@RequestBody Map<String, Object> param) {
        return ApiUtil.echoResult(graphDataService.export(param));
    }

    @RequestMapping("/exportExcel")
    @Permission("kg:graph:")
    public String exportExcelAction(@RequestBody Map<String, Object> param) {
        return ApiUtil.echoResult(graphDataService.exportExcel(param));
    }

    /**
     * 生成节点（实体）与关系的导入模板，xlsx以Base64返回，csv直接返回文本
     */
    @RequestMapping("/importTemplate")
    @Permission("kg:graph:")
    public String importTemplateAction(@RequestBody Map<String, Object> param) {
        return ApiUtil.echoResult(graphDataService.importTemplate(param));
    }

    /**
     * 解析上传的Excel，返回行数据供前端确认后再导入
     */
    @RequestMapping("/importExcel")
    @Permission({"kg:graph:add", "kg:graph:modify"})
    public String importExcelAction(@RequestParam("file") MultipartFile file) {
        try {
            List<List<String>> rows = ExcelUtil.read(file.getBytes());
            if (rows.isEmpty()) return ApiUtil.echoResult(1001, "文件内容为空", null);
            com.fasterxml.jackson.databind.node.ArrayNode data = DPUtil.arrayNode();
            for (List<String> row : rows) {
                com.fasterxml.jackson.databind.node.ArrayNode line = DPUtil.arrayNode();
                for (String value : row) line.add(value);
                data.add(line);
            }
            return ApiUtil.echoResult(0, null, data);
        } catch (Exception e) {
            return ApiUtil.echoResult(500, "解析Excel失败：" + e.getMessage(), null);
        }
    }

    @RequestMapping("/remove")
    @Permission("kg:graph:delete")
    public String removeAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        param.put("uid", rbacService.uid(request));
        return ApiUtil.echoResult(graphDataService.remove(param));
    }

    @RequestMapping("/aggregate")
    @Permission("kg:graph:")
    public String aggregateAction(@RequestBody Map<String, Object> param) {
        return ApiUtil.echoResult(graphDataService.aggregate(param));
    }

    @RequestMapping("/path")
    @Permission("kg:graph:")
    public String pathAction(@RequestBody Map<String, Object> param) {
        return ApiUtil.echoResult(graphDataService.shortestPath(param));
    }

    /**
     * 路径推理：指定起止节点与最大深度，返回两点之间的全部路径
     */
    @RequestMapping("/paths")
    @Permission("kg:graph:")
    public String pathsAction(@RequestBody Map<String, Object> param) {
        return ApiUtil.echoResult(graphDataService.paths(param));
    }

    @RequestMapping("/inspect")
    @Permission("kg:graph:")
    public String inspectAction(@RequestBody Map<String, Object> param) {
        return ApiUtil.echoResult(graphDataService.inspect(param));
    }

    @RequestMapping("/relationshipSearch")
    @Permission("kg:graph:")
    public String relationshipSearchAction(@RequestBody Map<String, Object> param) {
        return ApiUtil.echoResult(graphDataService.relationshipSearch(param));
    }

    @RequestMapping("/relationshipSave")
    @Permission({"kg:graph:add", "kg:graph:modify"})
    public String relationshipSaveAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        param.put("uid", rbacService.uid(request));
        return ApiUtil.echoResult(graphDataService.relationshipSave(param));
    }

    @RequestMapping("/relationshipRemove")
    @Permission("kg:graph:delete")
    public String relationshipRemoveAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        param.put("uid", rbacService.uid(request));
        return ApiUtil.echoResult(graphDataService.relationshipRemove(param));
    }

    @RequestMapping("/traverse")
    @Permission("kg:graph:")
    public String traverseAction(@RequestBody Map<String, Object> param) {
        return ApiUtil.echoResult(graphDataService.traverse(param));
    }

    @RequestMapping("/log")
    @Permission("kg:graph:")
    public String logAction(@RequestBody Map<String, Object> param) {
        ObjectNode result = dataLogService.search(param, DPUtil.buildMap("withUserInfo", true));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/logClean")
    @Permission("kg:graph:delete")
    public String logCleanAction(@RequestBody Map<String, Object> param) {
        long before = DPUtil.parseLong(param.get("beforeTime"));
        long count = dataLogService.clean(before);
        return ApiUtil.echoResult(0, null, count);
    }

    @RequestMapping("/queryList")
    @Permission("kg:graph:")
    public String queryListAction(@RequestBody Map<String, Object> param) {
        return ApiUtil.echoResult(dataQueryService.list(param));
    }

    @RequestMapping("/querySave")
    @Permission("kg:graph:")
    public String querySaveAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        param.put("uid", rbacService.uid(request));
        return ApiUtil.echoResult(dataQueryService.save(param));
    }

    @RequestMapping("/queryDelete")
    @Permission("kg:graph:")
    public String queryDeleteAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        param.put("uid", rbacService.uid(request));
        return ApiUtil.echoResult(dataQueryService.remove(param));
    }

}
