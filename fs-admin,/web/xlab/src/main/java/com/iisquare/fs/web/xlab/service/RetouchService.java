package com.iisquare.fs.web.xlab.service;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;

@Service
public class RetouchService extends ServiceBase {

    @Autowired
    public OpenCVService cvService;
    @Autowired
    public WatermarkService watermarkService;

    public Map<String, Object> site(Map<?, ?> param) {
        String url = DPUtil.parseString(param.get("url"));
        URL uri;
        try {
            uri = new URL(url);
        } catch (MalformedURLException e) {
            return ApiUtil.result(1001, "图片地址异常", e.getMessage());
        }
        String host = uri.getHost();
        if (host.endsWith(".ajkimg.com")) return ajkimg(url);
        if (host.endsWith(".soufunimg.com")) return soufunimg(url);
        return ApiUtil.result(1403, "目标地址暂不支持", null);
    }

    public Map<String, Object> ajkimg(String url) {
        String big = url.replaceAll("\\/\\d+x\\d+[a-z]?\\.", "/800x600.");
        String small = url.replaceAll("\\/\\d+x\\d+[a-z]?\\.", "/465x465.");
        Rect rect = new Rect(660, 518, 120, 60);
        return substitute(big, small, rect);
    }

    public Map<String, Object> soufunimg(String url) {
        String big = url.replaceAll("\\/\\d+x\\d+[a-z]?\\.", "/800x600.");
        String small = url.replaceAll("\\/\\d+x\\d+[a-z]?\\.", "/399x399.");
        Rect rect = new Rect(600, 530, 190, 65);
        return substitute(big, small, rect);
    }

    public Map<String, Object> substitute(String big, String small, Rect rect) {
        Mat bigImage = cvService.imdecode(big, Imgcodecs.IMREAD_COLOR);
        if (null == bigImage || bigImage.width() < 100 || bigImage.height() < 100) {
            return ApiUtil.result(5001, "大图读取失败", null);
        }
        Mat smallImage = cvService.imdecode(small, Imgcodecs.IMREAD_COLOR);
        if (null == smallImage || smallImage.width() < 100 || smallImage.height() < 100) {
            return ApiUtil.result(5002, "小图读取失败", null);
        }
        if (bigImage.width() < rect.x + rect.width) {
            return ApiUtil.result(5003, "大图宽度不满足掩码要求", bigImage.size());
        }
        if (bigImage.height() < rect.y + rect.height) {
            return ApiUtil.result(5003, "大图高度不满足掩码要求", bigImage.size());
        }
        Mat maskImage = new Mat(bigImage.size(),bigImage.type());
        MatOfPoint points = new MatOfPoint(new Point[]{
            rect.tl(), new Point(rect.x + rect.width, rect.y),
            rect.br(), new Point(rect.x, rect.y + rect.height)});
        Imgproc.fillPoly(maskImage, Arrays.asList(points), Scalar.all(255));
        if (!watermarkService.substitute(bigImage, smallImage, maskImage, rect)) {
            return ApiUtil.result(5001, "处理失败", null);
        }
        return ApiUtil.result(0, null, cvService.imencode(bigImage, true));
    }

}
