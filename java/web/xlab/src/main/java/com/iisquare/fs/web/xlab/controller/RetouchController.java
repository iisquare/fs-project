package com.iisquare.fs.web.xlab.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.web.mvc.ControllerBase;
import com.iisquare.fs.web.xlab.service.OpenCVService;
import com.iisquare.fs.web.xlab.service.RetouchService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RefreshScope
@Api(description = "图像修整")
@RequestMapping("/retouch")
public class RetouchController extends ControllerBase {

    @Autowired
    private OpenCVService cvService;
    @Autowired
    private RetouchService retouchService;
    protected final static Logger logger = LoggerFactory.getLogger(RetouchController.class);

    @PostMapping("/site")
    @ApiOperation(value = "站点定制", notes = "<table border='1' style='font-size:12px;text-indent:10px;'>" +
            "<tr><th>名称</th><th>类型</th><th>必填</th><th>默认值</th><th>说明</th></tr>" +
            "<tr><td>url</td><td>String</td><td>Y</td><td>无</td><td>图片地址</td></tr>" +
            "<tr><td>width</td><td>Integer</td><td>Y</td><td>无</td><td>图片宽度</td></tr>" +
            "<tr><td>height</td><td>Integer</td><td>Y</td><td>false</td><td>图片高度</td></tr>")
    public String siteAction(@RequestBody Map<?, ?> param) {
        return ApiUtil.echoResult(retouchService.site(param));
    }

}
