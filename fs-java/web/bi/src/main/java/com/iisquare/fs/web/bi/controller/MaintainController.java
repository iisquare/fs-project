package com.iisquare.fs.web.bi.controller;

import com.iisquare.fs.base.web.sse.MaintainEmitter;
import com.iisquare.fs.web.bi.service.DatasetService;
import com.iisquare.fs.web.bi.service.TrinoService;
import com.iisquare.fs.web.core.rbac.MaintainControllerBase;
import com.iisquare.fs.web.core.rbac.Permission;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/maintain")
public class MaintainController extends MaintainControllerBase {

    @Autowired
    TrinoService trinoService;
    @Autowired
    DatasetService datasetService;

    @RequestMapping("/catalog")
    @Permission
    public SseEmitter catalogAction(@RequestBody Map<String, Object> param, HttpServletRequest request, HttpServletResponse response) {
        MaintainEmitter emitter = new MaintainEmitter(request, response, 0L);
        return emitter.async(() -> trinoService.reload(param, emitter));
    }

    @RequestMapping("/dataset")
    @Permission("bi:maintain:")
    public SseEmitter datasetAction(@RequestBody Map<String, Object> param, HttpServletRequest request, HttpServletResponse response) {
        MaintainEmitter emitter = new MaintainEmitter(request, response, 0L);
        return emitter.async(() -> datasetService.reload(emitter));
    }

}
