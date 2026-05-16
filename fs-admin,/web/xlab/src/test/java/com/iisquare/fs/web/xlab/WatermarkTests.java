package com.iisquare.fs.web.xlab;

import com.iisquare.fs.web.xlab.service.OpenCVService;
import com.iisquare.fs.web.xlab.service.WatermarkService;
import org.junit.Test;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;

public class WatermarkTests {

    OpenCVService cvService = new OpenCVService();
    WatermarkService service = new WatermarkService();

    public WatermarkTests() {
        cvService.init(null);
    }

    @Test
    public void detachTest() {
        Mat background = Imgcodecs.imread("images/test.png.1440x1080.jpg", Imgcodecs.IMREAD_COLOR);
        Mat overlay = Imgcodecs.imread("images/lianjia_overlay.png", Imgcodecs.IMREAD_UNCHANGED);
        Mat image = background.clone();
        boolean result = service.detach(image, overlay, new Point(0, 0));
        service.blurAfterDetach(background, image, overlay);
        Imgcodecs.imwrite("images/result.jpg", image);
        System.out.println(result);
    }

    @Test
    public void encodeTest() {
        String filename = "C:\\Users\\Ouyang\\Desktop\\etui\\image\\gakki-src.png";
        String watermark = "C:\\Users\\Ouyang\\Desktop\\etui\\image\\watermark.png";
        String dest = "C:\\Users\\Ouyang\\Desktop\\etui\\image\\result.jpg";
        Mat img = Imgcodecs.imread(filename, Imgcodecs.IMREAD_COLOR);
        Mat wm = Imgcodecs.imread(watermark, Imgcodecs.IMREAD_GRAYSCALE);
        Mat result = service.encode(img, wm, "hello world!");
        HighGui.imshow("img", img);
        HighGui.imshow("wm", wm);
        HighGui.imshow("result", result);
        HighGui.waitKey();
        Imgcodecs.imwrite(dest, result);
    }

    @Test
    public void decodeTest() {
        String dest = "C:\\Users\\Ouyang\\Desktop\\etui\\6c533ad0-feab-4c14-975b-b89c7849c361\\6c533ad0-feab-4c14-975b-b89c7849c361.jpeg";
        String revert = "C:\\Users\\Ouyang\\Desktop\\etui\\image\\revert.jpg";
        Mat img = Imgcodecs.imread(dest, Imgcodecs.IMREAD_GRAYSCALE);
        Mat result = service.decode(img.clone(), false);
        HighGui.imshow("img", img);
        HighGui.imshow("result", result);
        HighGui.waitKey();
        Imgcodecs.imwrite(revert, result);
    }

}
