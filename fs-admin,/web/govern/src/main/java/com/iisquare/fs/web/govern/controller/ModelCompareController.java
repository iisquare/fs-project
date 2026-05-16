package com.iisquare.fs.web.govern.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.govern.service.ModelCompareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/modelCompare")
public class ModelCompareController extends PermitControllerBase {

    @Autowired
    private ModelCompareService compareService;

    @RequestMapping("/jdbc")
    @Permission("model:")
    public String jdbcAction(@RequestBody Map<?, ?> param) {
        Map<String, Object> result = compareService.jdbc(param);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/csv")
    @Permission("model:")
    public String csvAction(HttpServletRequest request, @RequestPart("file") MultipartFile file, @RequestParam Map param) {
        Map<String, Object> result = compareService.csv(file, param);
        return ApiUtil.echoResult(result);
    }

}
