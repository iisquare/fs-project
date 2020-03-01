package com.iisquare.fs.web.admin.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.web.admin.entity.Menu;
import com.iisquare.fs.web.admin.mvc.Permission;
import com.iisquare.fs.web.admin.mvc.PermitController;
import com.iisquare.fs.web.admin.service.MenuService;
import com.iisquare.fs.base.core.util.DPUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/menu")
public class MenuController extends PermitController {

    @Autowired
    private MenuService menuService;

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<?, ?> param) {
        Map<?, ?> result = menuService.search(param, DPUtil.buildMap(
            "withUserInfo", true, "withStatusText", true, "withParentInfo", true
        ));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/save")
    @Permission({"add", "modify"})
    public String saveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if(DPUtil.empty(name)) return ApiUtil.echoResult(1001, "名称异常", name);
        int sort = DPUtil.parseInt(param.get("sort"));
        int status = DPUtil.parseInt(param.get("status"));
        if(!menuService.status("default").containsKey(status)) return ApiUtil.echoResult(1002, "状态异常", status);
        String description = DPUtil.parseString(param.get("description"));
        int parentId = DPUtil.parseInt(param.get("parentId"));
        if(parentId < 0) {
            return ApiUtil.echoResult(1003, "上级节点异常", name);
        } else if(parentId > 0) {
            Menu parent = menuService.info(parentId);
            if(null == parent || !menuService.status("default").containsKey(parent.getStatus())) {
                return ApiUtil.echoResult(1004, "上级节点不存在或已删除", name);
            }
        }
        String icon = DPUtil.trim(DPUtil.parseString(param.get("icon")));
        String url = DPUtil.trim(DPUtil.parseString(param.get("url")));
        String target = DPUtil.trim(DPUtil.parseString(param.get("target")));
        Menu info = null;
        if(id > 0) {
            if(!hasPermit(request, "modify")) return ApiUtil.echoResult(9403, null, null);
            info = menuService.info(id);
            if(null == info) return ApiUtil.echoResult(404, null, id);
        } else {
            if(!hasPermit(request, "add")) return ApiUtil.echoResult(9403, null, null);
            info = new Menu();
        }
        info.setName(name);
        info.setParentId(parentId);
        info.setIcon(icon);
        info.setUrl(url);
        info.setTarget(target);
        info.setSort(sort);
        info.setStatus(status);
        info.setDescription(description);
        info = menuService.save(info, uid(request));
        return ApiUtil.echoResult(null == info ? 500 : 0, null, info);
    }

    @RequestMapping("/delete")
    @Permission
    public String deleteAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        List<Integer> ids = null;
        if(param.get("ids") instanceof List) {
            ids = DPUtil.parseIntList((List<?>) param.get("ids"));
        } else {
            ids = Arrays.asList(DPUtil.parseInt(param.get("ids")));
        }
        boolean result = menuService.delete(ids, uid(request));
        return ApiUtil.echoResult(result ? 0 : 500, null, result);
    }

    @RequestMapping("/config")
    @Permission("")
    public String configAction(ModelMap model) {
        model.put("status", menuService.status("default"));
        return ApiUtil.echoResult(0, null, model);
    }

}
