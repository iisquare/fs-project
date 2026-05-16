package com.iisquare.fs.web.xlab.controller;

import com.iisquare.fs.base.web.mvc.ControllerBase;
import com.iisquare.fs.web.xlab.service.FontService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RefreshScope
@Api(description = "字体库处理")
@RequestMapping("/font")
public class FontController extends ControllerBase {

    @Autowired
    private FontService ttfService;

}
