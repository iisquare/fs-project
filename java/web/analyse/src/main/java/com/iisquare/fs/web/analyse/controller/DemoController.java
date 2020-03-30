package com.iisquare.fs.web.analyse.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.web.mvc.ControllerBase;
import com.iisquare.fs.web.analyse.entity.primary.PrimaryUser;
import com.iisquare.fs.web.analyse.entity.second.SecondRole;
import com.iisquare.fs.web.analyse.service.primary.UserService;
import com.iisquare.fs.web.analyse.service.second.RoleService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Api(description = "内部调用")
@RequestMapping("/demo")
@RestController
@RefreshScope
public class DemoController extends ControllerBase {

    @Value("${custom.bus}")
    private String busTest;
    @Autowired
    private UserService userService;
    @Autowired
    private RoleService roleService;

    @ApiOperation(value = "回显请求参数")
    @PostMapping("/echo")
    public String echoAction(@RequestBody Map<?, ?> param) {
        return ApiUtil.echoResult(0, null, param);
    }

    @PostMapping("/user")
    @ApiOperation(value = "查询用户", notes = "<table border='1' style='font-size:12px;text-indent:10px;'>" +
            "<tr><th>名称</th><th>类型</th><th>必填</th><th>默认值</th><th>说明</th></tr>" +
            "<tr><td>id</td><td>Integer</td><td>Y</td><td>无</td><td>主键</td></tr>")
    public String userAction(@RequestBody Map<?, ?> param) {
        PrimaryUser info = userService.info(DPUtil.parseInt(param.get("id")));
        return ApiUtil.echoResult(0, null, info);
    }

    @PostMapping("/role")
    @ApiOperation(value = "查询角色", notes = "<table border='1' style='font-size:12px;text-indent:10px;'>" +
            "<tr><th>名称</th><th>类型</th><th>必填</th><th>默认值</th><th>说明</th></tr>" +
            "<tr><td>id</td><td>Integer</td><td>Y</td><td>无</td><td>主键</td></tr>")
    public String roleAction(@RequestBody Map<?, ?> param) {
        SecondRole info = roleService.info("test", DPUtil.parseInt(param.get("id")));
        return ApiUtil.echoResult(0, null, info);
    }

    @GetMapping("/bus")
    @ApiOperation(value = "消息总线")
    public String busAction() {
        return busTest;
    }

    public String fallback(Map<?, ?> param) {
        return ApiUtil.echoResult(500, "fallback", null);
    }

}
