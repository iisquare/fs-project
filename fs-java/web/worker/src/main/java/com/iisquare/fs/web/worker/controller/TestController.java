package com.iisquare.fs.web.worker.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.worker.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/test")
public class TestController extends PermitControllerBase {

    @Autowired
    private TestService testService;

    @GetMapping("/makeCase")
    public String makeCaseAction(@RequestParam Map<?, ?> param) {
        Map<String, Object> result = testService.makeCase();
        return ApiUtil.echoResult(result);
    }

    @GetMapping("/sendMessage")
    public String sendMessageAction(@RequestParam Map<?, ?> param) {
        String message = DPUtil.parseString(param.get("message"));
        int count = ValidateUtil.filterInteger(param.get("count"), true, 1, 10000, 1);
        for (int i = 0; i < count; i++) {
            testService.sendMessage(message);
        }
        return ApiUtil.echoResult(0, null, param);
    }

}
