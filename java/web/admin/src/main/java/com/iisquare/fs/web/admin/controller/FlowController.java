package com.iisquare.fs.web.admin.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.web.admin.entity.Flow;
import com.iisquare.fs.web.admin.mvc.Permission;
import com.iisquare.fs.web.admin.mvc.PermitController;
import com.iisquare.fs.web.admin.service.FlowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Controller
@RequestMapping("/flow")
public class FlowController extends PermitController {

    @Autowired
    private FlowService flowService;

    @RequestMapping("/list")
    @Permission("")
    @ResponseBody
    public String listAction(@RequestBody Map<?, ?> param) {
        Map<?, ?> result = flowService.search(param, DPUtil.buildMap(
            "withUserInfo", true, "withStatusText", true, "withTypeText", true
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
        Flow info = null;
        if(id > 0) {
            if(!hasPermit(request, "modify")) return ApiUtil.echoResult(9403, null, null);
            info = flowService.info(id);
            if(null == info) return ApiUtil.echoResult(404, null, id);
            if(param.containsKey("name")) {
                if(DPUtil.empty(name)) return ApiUtil.echoResult(1001, "名称异常", name);
                info.setName(name);
            }
            if(param.containsKey("sort")) info.setSort(sort);
            if(param.containsKey("status")) {
                if(!flowService.status("default").containsKey(status)) return ApiUtil.echoResult(1002, "状态异常", status);
                info.setStatus(status);
            }
            if(param.containsKey("description")) info.setDescription(description);
        } else {
            if(!hasPermit(request, "add")) return ApiUtil.echoResult(9403, null, null);
            info = new Flow();
            if(DPUtil.empty(name)) return ApiUtil.echoResult(1001, "名称异常", name);
            info.setName(name);
            info.setSort(sort);
            if(!flowService.status("default").containsKey(status)) return ApiUtil.echoResult(1002, "状态异常", status);
            info.setStatus(status);
            info.setDescription(description);
        }
        if(param.containsKey("type")) {
            String type = DPUtil.trim(DPUtil.parseString(param.get("type")));
            if(!flowService.types().containsKey(type)) return ApiUtil.echoResult(1009, "类型异常", type);
            info.setType(type);
        }
        info.setContent(content);
        info = flowService.save(info, uid(request));
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
        boolean result = flowService.delete(ids, uid(request));
        return ApiUtil.echoResult(result ? 0 : 500, null, result);
    }

    @RequestMapping("/config")
    @Permission("")
    @ResponseBody
    public String configAction(ModelMap model) {
        model.put("status", flowService.status("default"));
        model.put("types", flowService.types());
        return ApiUtil.echoResult(0, null, model);
    }

    @RequestMapping("/draw")
    @Permission
    public String drawAction(@RequestParam Map<String, Object> param, ModelMap model, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        Flow info = flowService.info(id);
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
        item = new LinkedHashMap<>();
        item.put("name", java.sql.Date.class.getSimpleName());
        item.put("classname", java.sql.Date.class.getName());
        infoType.put(java.sql.Date.class.getName(), item);
        item = new LinkedHashMap<>();
        item.put("name", java.sql.Time.class.getSimpleName());
        item.put("classname", java.sql.Time.class.getName());
        infoType.put(java.sql.Time.class.getName(), item);
        item = new LinkedHashMap<>();
        item.put("name", java.sql.Timestamp.class.getSimpleName());
        item.put("classname", java.sql.Timestamp.class.getName());
        infoType.put(java.sql.Timestamp.class.getName(), item);
        return DPUtil.stringify(infoType.values());
    }

    @RequestMapping("/plain")
    @ResponseBody
    public String plainAction(@RequestParam Map<String, Object> param, ModelMap model, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        Flow info = flowService.info(id);
        if(null == info) return "";
        return info.getContent();
    }

}
