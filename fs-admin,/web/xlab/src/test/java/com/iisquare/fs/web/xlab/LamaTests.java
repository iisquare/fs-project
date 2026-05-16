package com.iisquare.fs.web.xlab;

import com.iisquare.fs.web.xlab.service.OpenCVService;
import com.iisquare.fs.web.xlab.torch.LamaService;
import org.junit.Test;
import org.opencv.core.Mat;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class LamaTests {

    public LamaService lamaService = new LamaService();
    public OpenCVService cvService = new OpenCVService();

    public LamaTests() {
        cvService.init("");
        lamaService.cvService = cvService;
        lamaService.modelPath = "D:\\htdocs\\fs-project-vip\\python\\lama\\LaMa_models\\lama.pt";
        lamaService.reload();
    }

    @Test
    public void singleTest() {
        Mat image = Imgcodecs.imread("D:\\htdocs\\fs-project-vip\\python\\lama\\LaMa_test_images\\test\\a1.jpg", Imgcodecs.IMREAD_COLOR);
        Mat mask = Imgcodecs.imread("D:\\htdocs\\fs-project-vip\\python\\lama\\LaMa_test_images\\test\\a1_mask.png", Imgcodecs.IMREAD_GRAYSCALE);
        Imgproc.threshold(mask, mask, 128, 255, Imgproc.THRESH_BINARY);
        long millis = System.currentTimeMillis();
        Mat result = lamaService.predict(image, mask);
        System.out.println(String.format("predict time consuming:%d", System.currentTimeMillis() - millis));
        HighGui.imshow("result", result);
        HighGui.waitKey();
    }

}
