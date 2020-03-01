package com.iisquare.fs.web.admin.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.web.admin.entity.Role;
import com.iisquare.fs.web.admin.mvc.Permission;
import com.iisquare.fs.web.admin.mvc.PermitController;
import com.iisquare.fs.web.admin.service.MenuService;
import com.iisquare.fs.web.admin.service.RelationService;
import com.iisquare.fs.web.admin.service.ResourceService;
import com.iisquare.fs.web.admin.service.RoleService;
import com.iisquare.fs.base.core.util.DPUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@RestController
@RequestMapping("/role")
public class RoleController extends PermitController {

    @Autowired
    private RoleService roleService;
    @Autowired
    private MenuService menuService;
    @Autowired
    private ResourceService resourceService;
    @Autowired
    private RelationService relationService;

    @RequestMapping("/tree")
    @Permission({"", "menu", "resource"})
    public String treeAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        if(id < 1) return ApiUtil.echoResult(1001, "参数异常", id);
        Role info = roleService.info(id);
        if(null == info || -1 == info.getStatus()) return ApiUtil.echoResult(1002, "记录不存在", id);
        Map<String, Object> result = new LinkedHashMap<>();
        String type = DPUtil.parseString(param.get("type"));
        if(param.containsKey("bids")) {
            switch (type) {
                case "menu":
                case "resource":
                    if(!hasPermit(request, type)) return ApiUtil.echoResult(9403, null, null);
                    Set<Integer> bids = new HashSet<>();
                    bids.addAll((List<Integer>) param.get("bids"));
                    bids = relationService.relationIds("role_" + type, id, bids);
                    return ApiUtil.echoResult(null == bids ? 500 : 0, null, bids);
                default:
                    return ApiUtil.echoResult(1003, "类型异常", id);
            }
        } else {
            switch (type) {
                case "menu":
                    result.put("tree", menuService.tree());
                    break;
                case "resource":
                    result.put("tree", resourceService.tree());
                    break;
                default:
                    result.put("tree", new ArrayList<>());
            }
            result.put("checked", relationService.relationIds("role_" + type, info.getId(), null));
            return ApiUtil.echoResult(0, null, result);
        }
    }

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<?, ?> param) {
        Map<?, ?> result = roleService.search(param, DPUtil.buildMap("withUserInfo", true, "withStatusText", true));
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
        if(!roleService.status("default").containsKey(status)) return ApiUtil.echoResult(1002, "状态异常", status);
        String description = DPUtil.parseString(param.get("description"));
        Role info = null;
        if(id > 0) {
            if(!hasPermit(request, "modify")) return ApiUtil.echoResult(9403, null, null);
            info = roleService.info(id);
            if(null == info) return ApiUtil.echoResult(404, null, id);
        } else {
            if(!hasPermit(request, "add")) return ApiUtil.echoResult(9403, null, null);
            info = new Role();
        }
        info.setName(name);
        info.setSort(sort);
        info.setStatus(status);
        info.setDescription(description);
        info = roleService.save(info, uid(request));
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
        boolean result = roleService.delete(ids, uid(request));
        return ApiUtil.echoResult(result ? 0 : 500, null, result);
    }

    @RequestMapping("/config")
    @Permission("")
    public String configAction(ModelMap model) {
        model.put("status", roleService.status("default"));
        return ApiUtil.echoResult(0, null, model);
    }

}
