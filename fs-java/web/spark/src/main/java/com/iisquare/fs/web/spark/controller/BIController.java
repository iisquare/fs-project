package com.iisquare.fs.web.spark.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.spark.service.BIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/bi")
public class BIController extends PermitControllerBase {

    @Autowired
    private BIService biService;

    @PostMapping("/dag")
    public String dagAction(@RequestBody Map<?, ?> param) {
        JsonNode diagram = DPUtil.toJSON(param);
        return ApiUtil.echoResult(biService.dag(diagram));
    }

    @PostMapping("/dataset")
    public String datasetAction(@RequestBody Map<?, ?> param) {
        JsonNode options = DPUtil.toJSON(param);
        return ApiUtil.echoResult(biService.dataset(options));
    }

    @PostMapping("/matrix")
    public String matrixAction(@RequestBody Map<?, ?> param) {
        JsonNode options = DPUtil.toJSON(param);
        JsonNode dataset = options.at("/dataset");
        JsonNode preview = options.at("/preview");
        return ApiUtil.echoResult(biService.matrix(dataset, preview));
    }

    @PostMapping("/visualize")
    public String visualizeAction(@RequestBody Map<?, ?> param) {
        JsonNode options = DPUtil.toJSON(param);
        JsonNode dataset = options.at("/dataset");
        JsonNode preview = options.at("/preview");
        JsonNode level = options.at("/level");
        return ApiUtil.echoResult(biService.visualize(dataset, preview, level));
    }

}
