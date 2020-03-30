package com.iisquare.fs.web.admin.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.web.admin.entity.Analysis;
import com.iisquare.fs.web.admin.mvc.Permission;
import com.iisquare.fs.web.admin.mvc.PermitController;
import com.iisquare.fs.web.admin.service.AnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.util.*;

@Controller
@RequestMapping("/analysis")
public class AnalysisController extends PermitController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnalysisController.class);
    @Autowired
    private AnalysisService analysisService;

    @RequestMapping("/list")
    @Permission("")
    @ResponseBody
    public String listAction(@RequestBody Map<?, ?> param) {
        Map<?, ?> result = analysisService.search(param, DPUtil.buildMap(
            "withCronInfo", true, "withUserInfo", true, "withStatusText", true, "withTypeText", true
        ));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/save")
    @Permission({"add", "modify"})
    @ResponseBody
    public String saveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        int sort = DPUtil.parseInt(param.get("sort"));
        int status = DPUtil.parseInt(param.get("status"));
        String description = DPUtil.parseString(param.get("description"));
        String content = DPUtil.parseString(param.get("content"));
        ObjectNode settingNode = (ObjectNode) DPUtil.parseJSON(DPUtil.parseString(param.get("setting")));
        long ttl = 604800000L;
        if (settingNode == null) {
            settingNode = DPUtil.objectNode();
        } else {
            ttl = settingNode.findPath("ttl").asLong();
            if (ttl <= 0) {
                ttl = 604800000L;
            } else if (ttl > 15552000000L) {
                ttl = 15552000000L;
            }
        }
        settingNode.put("ttl", ttl);
        String setting = DPUtil.stringify(settingNode);
        String cronExpression = DPUtil.parseString(param.get("cronExpression"));
        String cronParams = DPUtil.parseString(param.get("cronParams"));
        Analysis info = null;
        boolean isModifyCron = false;
        if(id > 0) {
            if(!hasPermit(request, "modify")) return ApiUtil.echoResult(9403, null, null);
            info = analysisService.info(id, false);
            if(null == info) return ApiUtil.echoResult(404, null, id);
            if(param.containsKey("name")) {
                if(DPUtil.empty(name)) return ApiUtil.echoResult(1001, "名称异常", name);
                info.setName(name);
                isModifyCron = true;
            }
            if(param.containsKey("sort")) info.setSort(sort);
            if(param.containsKey("status")) {
                if(!analysisService.status("default").containsKey(status)) return ApiUtil.echoResult(1002, "状态异常", status);
                info.setStatus(status);
            }
            if(param.containsKey("description")) info.setDescription(description);
            if(param.containsKey("content")) info.setContent(content);
            if(param.containsKey("setting")) info.setSetting(setting);
            if(param.containsKey("cronExpression")) info.setCronExpression(cronExpression);
            if(param.containsKey("cronParams")) info.setCronParams(cronParams);
        } else {
            if(!hasPermit(request, "add")) return ApiUtil.echoResult(9403, null, null);
            info = new Analysis();
            if(DPUtil.empty(name)) return ApiUtil.echoResult(1001, "名称异常", name);
            info.setName(name);
            info.setSort(sort);
            if(!analysisService.status("default").containsKey(status)) return ApiUtil.echoResult(1002, "状态异常", status);
            info.setStatus(status);
            info.setDescription(description);
            info.setContent(content);
            info.setSetting(setting);
            info.setCronExpression(cronExpression);
            info.setCronParams(cronParams);
            isModifyCron = true;
        }
        if(param.containsKey("type")) {
            String type = DPUtil.trim(DPUtil.parseString(param.get("type")));
            if(!analysisService.types().containsKey(type)) return ApiUtil.echoResult(1009, "类型异常", type);
            info.setType(type);
        }
        info = analysisService.save(info, uid(request), isModifyCron);
        return ApiUtil.echoResult(null == info ? 500 : 0, null, info);
    }

    @RequestMapping("/delete")
    @Permission
    @ResponseBody
    public String deleteAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        List<Integer> ids = null;
        if(param.get("ids") instanceof List) {
            ids = DPUtil.parseIntList((List<?>) param.get("ids"));
        } else {
            ids = Arrays.asList(DPUtil.parseInt(param.get("ids")));
        }
        boolean result = analysisService.delete(ids, uid(request));
        return ApiUtil.echoResult(result ? 0 : 500, null, result);
    }

    @RequestMapping("/config")
    @Permission("")
    @ResponseBody
    public String configAction(ModelMap model) {
        model.put("status", analysisService.status("default"));
        model.put("types", analysisService.types());
        return ApiUtil.echoResult(0, null, model);
    }

    @RequestMapping("/draw")
    @Permission
    public String drawAction(@RequestParam Map<String, Object> param, ModelMap model, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        Analysis info = analysisService.info(id, false);
        model.put("info", info);
        return displayTemplate(model, request);
    }

    @RequestMapping("/property")
    @Permission("draw")
    public String propertyAction(@RequestParam Map<String, Object> param, ModelMap model, HttpServletRequest request) {
        return displayTemplate(model, request);
    }

    @RequestMapping("/returns")
    @Permission("draw")
    public String returnsAction(@RequestParam Map<String, Object> param, ModelMap model, HttpServletRequest request) {
        return displayTemplate(model, request);
    }

    @RequestMapping("/infoType")
    @Permission("draw")
    @ResponseBody
    public String infoTypeAction(@RequestParam Map<String, Object> param, ModelMap model, HttpServletRequest request) {
        Map<String, Map<String, Object>> infoType = new LinkedHashMap<>();
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", Boolean.class.getSimpleName());
        item.put("classname", Boolean.class.getName());
        infoType.put(Boolean.class.getName(), item);
        item = new LinkedHashMap<>();
        item.put("name", Byte.class.getSimpleName());
        item.put("classname", Byte.class.getName());
        infoType.put(Byte.class.getName(), item);
        item = new LinkedHashMap<>();
        item.put("name", Double.class.getSimpleName());
        item.put("classname", Double.class.getName());
        infoType.put(Double.class.getName(), item);
        item = new LinkedHashMap<>();
        item.put("name", Float.class.getSimpleName());
        item.put("classname", Float.class.getName());
        infoType.put(Float.class.getName(), item);
        item = new LinkedHashMap<>();
        item.put("name", Integer.class.getSimpleName());
        item.put("classname", Integer.class.getName());
        infoType.put(Integer.class.getName(), item);
        item = new LinkedHashMap<>();
        item.put("name", Long.class.getSimpleName());
        item.put("classname", Long.class.getName());
        infoType.put(Long.class.getName(), item);
        item = new LinkedHashMap<>();
        item.put("name", Short.class.getSimpleName());
        item.put("classname", Short.class.getName());
        infoType.put(Short.class.getName(), item);
        item = new LinkedHashMap<>();
        item.put("name", String.class.getSimpleName());
        item.put("classname", String.class.getName());
        infoType.put(String.class.getName(), item);
        item = new LinkedHashMap<>();
        item.put("name", Date.class.getSimpleName());
        item.put("classname", Date.class.getName());
        infoType.put(Date.class.getName(), item);
        return DPUtil.stringify(infoType.values());
    }

    @RequestMapping("/info")
    @ResponseBody
    public String infoAction(@RequestBody Map<?, ?> param) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        Analysis info = analysisService.info(id, true);
        return ApiUtil.echoResult(null == info ? 404 : 0, null, info);
    }

//    @RequestMapping("/results")
//    @ResponseBody
//    public String resultsAction(@RequestBody Map<String, Object> params) {
//        int analysisid = DPUtil.parseInt(params.get("analysisid"));
//        if (analysisid == 0) {
//            return ApiUtil.echoResult(1001, "参数错误", null);
//        }
//        long stime, etime;
//        String timeFormat = "yyyy-MM-dd";
//        stime = params.containsKey("sdate") ? DPUtil.dateTimeToMillis(params.get("sdate"), timeFormat, -1) : 0L;
//        etime = params.containsKey("edate") ? DPUtil.dateTimeToMillis(params.get("edate"), timeFormat, -1) + 86399999L : System.currentTimeMillis();
//        int page = ValidateUtil.filterInteger(params.get("page"), true, 1, null, 1);
//        int pageSize = ValidateUtil.filterInteger(params.get("pageSize"), true, 1, 500, 15);
//        Map<String, Object> result = analysisService.results(analysisid, stime, etime, page, pageSize);
//        return ApiUtil.echoResult(0, null, result);
//    }
//
//    @RequestMapping("/result")
//    @ResponseBody
//    public String resultAction(@RequestParam Map<String, Object> params) {
//        String resultid = DPUtil.parseString(params.get("resultid"));
//        Map<String, Object> result = analysisService.result(resultid);
//        return ApiUtil.echoResult(0, null, result);
//    }
//
//    @RequestMapping("/exportresult")
//    public void exportResultAction(@RequestParam Map<String, Object> params, HttpServletResponse response) {
//        String resultid = DPUtil.parseString(params.get("resultid"));
//        response.setContentType("application/vnd.ms-excel;charset=UTF-8");
//        response.setHeader("Content-Disposition", "attachment;filename=" + resultid + ".xlsx");
//        try {
//            OutputStream outputStream = response.getOutputStream();
//            analysisService.exportResult(resultid).write(outputStream);
//            outputStream.close();
//        } catch (Exception e) {
//            LOGGER.error(e.getMessage(), e);
//        }
//    }
//
//    @RequestMapping("/deleteresult")
//    @ResponseBody
//    public String deleteResultAction(@RequestBody Map<String, Object> params) {
//        String resultid = DPUtil.parseString(params.get("resultid"));
//        boolean result = analysisService.deleteResult(resultid);
//        return ApiUtil.echoResult(result ? 0 : 500, null, result);
//    }
}
