package com.iisquare.fs.web.xlab.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.web.mvc.ControllerBase;
import com.iisquare.fs.web.xlab.service.OpenCVService;
import com.iisquare.fs.web.xlab.service.VerifyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RefreshScope
@Api(description = "验证码识别")
@RequestMapping("/verify")
public class VerifyController extends ControllerBase {

    @Autowired
    private OpenCVService cvService;
    @Autowired
    private VerifyService verifyService;
    protected final static Logger logger = LoggerFactory.getLogger(VerifyController.class);

    @PostMapping("/slide")
    @ApiOperation(value = "滑块验证", notes = "<table border='1' style='font-size:12px;text-indent:10px;'>" +
            "<tr><th>名称</th><th>类型</th><th>必填</th><th>默认值</th><th>说明</th></tr>" +
            "<tr><td>type</td><td>string</td><td>Y</td><td>无</td><td>图片类型</td></tr>" +
            "<tr><td>image</td><td>base64</td><td>Y</td><td>无</td><td>图片内容</td></tr>" +
            "<tr><td>slide</td><td>base64</td><td>N</td><td>无</td><td>滑块内容</td></tr>" +
            "<tr><td>withTrack</td><td>Boolean</td><td>N</td><td>false</td><td>模拟运动</td></tr>")
    public String slideAction(@RequestBody Map<?, ?> param) {
        String type = DPUtil.parseString(param.get("type"));
        if (!Arrays.asList("anjuke").contains(type)) {
            return ApiUtil.echoResult(1001, "类型不支持", type);
        }
        Mat image = cvService.imdecode(DPUtil.parseString(param.get("image")), Imgcodecs.IMREAD_COLOR);
        Mat slide = cvService.imdecode(DPUtil.parseString(param.get("slide")), Imgcodecs.IMREAD_COLOR);
        if (cvService.empty(image)) return ApiUtil.echoResult(1001, "读取图片失败", image);
        List<Rect> rects = verifyService.slide(image, slide, null);
        if (DPUtil.empty(param.get("withTrack"))) {
            return ApiUtil.echoResult(0, null, rects);
        }
        return ApiUtil.echoResult(0, null, verifyService.track(slide, rects));
    }

    @PostMapping("/road")
    @ApiOperation(value = "绘制路径", notes = "<table border='1' style='font-size:12px;text-indent:10px;'>" +
            "<tr><th>名称</th><th>类型</th><th>必填</th><th>默认值</th><th>说明</th></tr>" +
            "<tr><td>type</td><td>string</td><td>Y</td><td>无</td><td>图片类型</td></tr>" +
            "<tr><td>image</td><td>base64</td><td>Y</td><td>无</td><td>图片内容</td></tr>" +
            "<tr><td>step</td><td>Integer</td><td>N</td><td>10</td><td>路径步长</td></tr>")
    public String roadAction(@RequestBody Map<?, ?> param) {
        String type = DPUtil.parseString(param.get("type"));
        if (!Arrays.asList("58").contains(type)) {
            return ApiUtil.echoResult(1001, "类型不支持", type);
        }
        Mat image = cvService.imdecode(DPUtil.parseString(param.get("image")), Imgcodecs.IMREAD_COLOR);
        if (cvService.empty(image)) return ApiUtil.echoResult(1001, "读取图片失败", image);
        int step = ValidateUtil.filterInteger(param.get("step"), true, 1, 100, 10);
        boolean debug = !DPUtil.empty(param.get("debug"));
        Mat result = debug ? image.clone() : null;
        List<Point> line = verifyService.road(image, step, result);
        if (debug) {
            FileUtil.putContent("cloud-rest/xlab/src/test/resources/opencv-verify-road.json", DPUtil.stringify(param), "UTF-8");
            logger.warn(DPUtil.stringify(line) + "->" + Imgcodecs.imwrite("cloud-rest/xlab/images/verify/captcha_img_result.jpg", result));
        }
        return ApiUtil.echoResult(0, null, line);
    }

}
