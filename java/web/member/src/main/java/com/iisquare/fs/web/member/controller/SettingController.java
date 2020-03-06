package com.iisquare.fs.web.member.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.web.member.entity.Setting;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.member.service.SettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/setting")
public class SettingController extends PermitControllerBase {

    @Autowired
    private SettingService settingService;

    @RequestMapping("/get")
    @Permission("")
    public String getAction(@RequestParam("type")String type, @RequestParam("value")String value) {
        return ApiUtil.echoResult(0, null, settingService.get(type, value));
    }

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<?, ?> param) {
        Map<?, ?> result = settingService.search(param, DPUtil.buildMap("withUserInfo", true));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/save")
    @Permission({"add", "modify"})
    public String saveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if(DPUtil.empty(name)) return ApiUtil.echoResult(1001, "名称异常", name);
        int sort = DPUtil.parseInt(param.get("sort"));
        String description = DPUtil.parseString(param.get("description"));
        String type = DPUtil.trim(DPUtil.parseString(param.get("type")));
        Setting info = null;
        if(id > 0) {
            if(!hasPermit(request, "modify")) return ApiUtil.echoResult(9403, null, null);
            info = settingService.info(id);
            if(null == info) return ApiUtil.echoResult(404, null, id);
        } else {
            if(!hasPermit(request, "add")) return ApiUtil.echoResult(9403, null, null);
            info = new Setting();
        }
        info.setName(name);
        info.setType(type);
        info.setContent(DPUtil.parseString(param.get("content")));
        info.setSort(sort);
        info.setDescription(description);
        info = settingService.save(info, uid(request));
        return ApiUtil.echoResult(null == info ? 500 : 0, null, info);
    }

    @RequestMapping("/delete")
    @Permission
    public String deleteAction(@RequestBody Map<?, ?> param) {
        List<Integer> ids = null;
        if(param.get("ids") instanceof List) {
            ids = DPUtil.parseIntList((List<?>) param.get("ids"));
        } else {
            ids = Arrays.asList(DPUtil.parseInt(param.get("ids")));
        }
        boolean result = settingService.delete(ids);
        return ApiUtil.echoResult(result ? 0 : 500, null, result);
    }

}
