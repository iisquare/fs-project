package com.iisquare.fs.web.bi.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.web.bi.service.DatasetService;
import com.iisquare.fs.web.core.rbac.RpcControllerBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/rpc")
public class RpcController extends RpcControllerBase {

    @Autowired
    DatasetService datasetService;

    @PostMapping("/datasetRefresh")
    public String datasetRefreshAction(@RequestBody Map<String, Object> param) {
        Map<String, Object> result = datasetService.refresh(param);
        return ApiUtil.echoResult(result);
    }

}
