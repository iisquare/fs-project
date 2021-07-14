package com.iisquare.fs.web.worker.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.worker.service.CanalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/canal")
public class CanalController extends PermitControllerBase {

    @Autowired
    private CanalService canalService;

    @GetMapping("/makeCase")
    public String makeCaseAction(@RequestParam Map<?, ?> param) {
        Map<String, Object> result = canalService.makeCase();
        return ApiUtil.echoResult(result);
    }

}
