package com.iisquare.fs.web.cms.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/setting")
public class SettingController extends PermitControllerBase {

    @Autowired
    private DefaultRbacService rbacService;

    @RequestMapping("/load")
    @Permission
    public String loadAction(@RequestBody Map<?, ?> param) {
        List<String> names = DPUtil.parseStringList(param.get("names"));
        if (DPUtil.empty(names)) return ApiUtil.echoResult(1001, "参数异常", names);
        Map<String, String> result = rbacService.setting("cms", names, null);
        return ApiUtil.echoResult(null == result ? 500 : 0, null, result);
    }

    @RequestMapping("/change")
    @Permission
    public String changeAction(@RequestBody Map<?, ?> param) {
        Map<String, String> data = (Map<String, String>) param.get("data");
        if (DPUtil.empty(data)) return ApiUtil.echoResult(1001, "参数异常", data);
        int result = rbacService.setting("cms", data);
        return ApiUtil.echoResult(result >= 0 ? 0 : 500, null, result);
    }

}
