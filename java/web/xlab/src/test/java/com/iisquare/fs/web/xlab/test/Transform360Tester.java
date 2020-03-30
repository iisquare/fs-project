package com.iisquare.fs.web.xlab.test;

import com.iisquare.fs.web.xlab.service.OpenCVService;
import com.iisquare.fs.web.xlab.service.Transform360Service;
import org.junit.Test;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.LinkedHashMap;
import java.util.Map;

public class Transform360Tester {

    @Test
    public void cubic2equirectangularTest() {
        OpenCVService cvService = new OpenCVService();
        cvService.init(null);
        Transform360Service service = new Transform360Service();
        service.help();
        String sky = "images/theta/";
        Map<String, Mat> map = new LinkedHashMap<>();
        map.put("back", Imgcodecs.imread(sky + "back.jpg"));
        map.put("bottom", Imgcodecs.imread(sky + "bottom.jpg"));
        map.put("front", Imgcodecs.imread(sky + "front.jpg"));
        map.put("left", Imgcodecs.imread(sky + "left.jpg"));
        map.put("right", Imgcodecs.imread(sky + "right.jpg"));
        map.put("top", Imgcodecs.imread(sky + "top.jpg"));
//        Mat cubic = service.cubic(map);
        Mat result = service.cubic2equirectangular(map, "horizontal");
        Imgproc.resize(result, result, new Size(5376, 2688));
        System.out.println(Imgcodecs.imwrite("images/result.jpg", result));
    }

}
