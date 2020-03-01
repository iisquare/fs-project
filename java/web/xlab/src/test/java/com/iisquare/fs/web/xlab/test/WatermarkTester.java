package com.iisquare.fs.web.xlab.test;

import com.iisquare.fs.web.xlab.service.OpenCVService;
import com.iisquare.fs.web.xlab.service.WatermarkService;
import org.junit.Test;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;

public class WatermarkTester {

    @Test
    public void detachTest() {
        OpenCVService cvService = new OpenCVService();
        cvService.init(null);
        WatermarkService service = new WatermarkService();
        Mat background = Imgcodecs.imread("images/test.png.1440x1080.jpg", Imgcodecs.IMREAD_COLOR);
        Mat overlay = Imgcodecs.imread("images/lianjia_overlay.png", Imgcodecs.IMREAD_UNCHANGED);
        Mat image = background.clone();
        boolean result = service.detach(image, overlay, new Point(0, 0));
        service.blurAfterDetach(background, image, overlay);
        Imgcodecs.imwrite("images/result.jpg", image);
        System.out.println(result);
    }

}
