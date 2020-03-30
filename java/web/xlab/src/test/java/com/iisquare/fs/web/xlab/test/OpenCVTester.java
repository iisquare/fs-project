package com.iisquare.fs.web.xlab.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.web.xlab.service.OpenCVService;
import com.iisquare.fs.web.xlab.service.VerifyService;
import org.junit.Test;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;

import java.util.List;

public class OpenCVTester {

    @Test
    public void slideTest() {
        JsonNode json = DPUtil.parseJSON(FileUtil.getContent(getClass().getClassLoader().getResource("opencv-verfiy-slide.json"), false, "UTF-8"));
        OpenCVService cvService = new OpenCVService();
        cvService.init("cloud-rest/xlab");
        VerifyService verifyService = new VerifyService();
        Mat image = cvService.imdecode(json.get("image").asText(), Imgcodecs.IMREAD_COLOR);
        Mat slide = cvService.imdecode(json.get("slide").asText(), Imgcodecs.IMREAD_COLOR);
        Mat result = image.clone();
        List<Rect> rects = verifyService.slide(image, slide, result);
        Imgcodecs.imwrite("cloud-rest/xlab/images/verify/captcha_img_result.jpg", result);
        Imgcodecs.imwrite("cloud-rest/xlab/images/verify/captcha_img_slide.jpg", slide);
        System.out.println(rects);
    }

    @Test
    public void roadTest() {
        JsonNode json = DPUtil.parseJSON(FileUtil.getContent(getClass().getClassLoader().getResource("opencv-verify-road.json"), false, "UTF-8"));
        OpenCVService cvService = new OpenCVService();
        cvService.init("cloud-rest/xlab");
        VerifyService verifyService = new VerifyService();
        Mat image = cvService.imdecode(json.get("image").asText(), Imgcodecs.IMREAD_COLOR);
        Mat result = image.clone();
        List<Point> lines = verifyService.road(image, 10, result);
        System.out.println(Imgcodecs.imwrite("cloud-rest/xlab/images/verify/captcha_img_result.jpg", result));
        System.out.println(DPUtil.stringify(lines));
    }

}
