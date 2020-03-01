package com.iisquare.fs.web.xlab.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.web.mvc.ControllerBase;
import com.iisquare.fs.web.xlab.service.TTFService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Map;

@RestController
@RefreshScope
@Api(description = "字体库处理")
@RequestMapping("/ttf")
public class TTFController extends ControllerBase {

    @Autowired
    private TTFService ttfService;

    @PostMapping("/extract")
    @ApiOperation(value = "提取字体", notes = "<table border='1' style='font-size:12px;text-indent:10px;'>" +
            "<tr><th>名称</th><th>类型</th><th>必填</th><th>默认值</th><th>说明</th></tr>" +
            "<tr><td>type</td><td>string</td><td>Y</td><td>无</td><td>字体类型</td></tr>" +
            "<tr><td>ttf</td><td>base64</td><td>Y</td><td>无</td><td>字体库内容</td></tr>")
    public String extractAction(@RequestBody Map<?, ?> param) {
        String type = DPUtil.parseString(param.get("type"));
        if (!Arrays.asList("anjuke").contains(type)) return ApiUtil.echoResult(1001, "类型不支持", type);
        String ttf = DPUtil.parseString(param.get("ttf"));
        if (DPUtil.empty(ttf)) return ApiUtil.echoResult(10021, "字体库内容不能为空", ttf);
        Map<String, Integer> result = ttfService.anjukeBase64(ttf);
        return ApiUtil.echoResult(null == result ? 500 : 0, null, result);
    }

}
