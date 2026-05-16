package com.iisquare.fs.web.xlab;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.web.xlab.service.OpenCVService;
import com.iisquare.fs.web.xlab.tensorflow.CrnnService;
import com.iisquare.fs.web.xlab.tensorflow.MrcnnService;
import com.iisquare.fs.web.xlab.service.VerifyService;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.opencv.core.*;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OpenCVTests {

    public OpenCVService cvService = new OpenCVService();

    public OpenCVTests() {
        cvService.init(null);
//        cvService.init("cloud-rest/xlab");
    }

    @Test
    public void textTest() {
        float confThreshold = 0.1f;
        float nmsThreshold = 0.2f;
        String cvDir = "C:\\Users\\Ouyang\\Desktop\\text\\";
        String imPath = cvDir + "test/21-sn.jpg";
        String modelDecoder = cvDir + "east.pb";
        List<String> outNames = Arrays.asList("feature_fusion/Conv_7/Sigmoid", "feature_fusion/concat_3");
        Net detector = Dnn.readNet(modelDecoder);
        CrnnService crnnService = new TensorFlowTests().crnnService;
        Mat frame = Imgcodecs.imread(imPath);
        int inpWidth = frame.width() / 32 * 32;
        int inpHeight = frame.height() / 32 * 32;
        Mat blob = Dnn.blobFromImage(frame, 1.0, new Size(inpWidth, inpHeight), new Scalar(123.68, 116.78, 103.94), true, false);
        TickMeter tickMeter = new TickMeter();
        detector.setInput(blob);
        List<Mat> outs = new ArrayList<>();
        tickMeter.start();
        detector.forward(outs, outNames);
        tickMeter.stop();
        // Decode predicted bounding boxes.
        MatOfRotatedRect boxes = new MatOfRotatedRect();
        MatOfFloat confidences = new MatOfFloat();
        cvService.decodeBoundingBoxes(outs.get(0), outs.get(1), confThreshold, boxes, confidences);
        // Apply non-maximum suppression procedure.
        MatOfInt indices = new MatOfInt();
        Dnn.NMSBoxesRotated(boxes, confidences, confThreshold, nmsThreshold, indices);
        Point ratio = new Point(Double.valueOf(frame.cols()) / inpWidth, Double.valueOf(frame.rows()) / inpHeight);
        // Render text.
        List<RotatedRect> boxList = boxes.toList();
        List<Integer> indexList = indices.rows() > 0 ? indices.toList() : new ArrayList<>();
        for (Integer index : indexList) {
            RotatedRect box = boxList.get(index);
            Point[] vertices = new Point[4];
            box.points(vertices);
            for (int j = 0; j < 4; ++j) {
                vertices[j].x *= ratio.x;
                vertices[j].y *= ratio.y;
            }
            Mat cropped = cvService.fourPointsTransform(frame, vertices);
            HighGui.imshow("cropped" + index, cropped);
            Imgproc.putText(frame, String.valueOf(index), vertices[1], Imgproc.FONT_ITALIC, 0.3, new Scalar(0, 0, 255));
            for (int j = 0; j < 4; ++j) {
                Imgproc.line(frame, vertices[j], vertices[(j + 1) % 4], new Scalar(0, 255, 0), 1);
            }
            List<String> text = crnnService.crnn(cropped);
            System.out.println(String.format("text%d:%s", index, StringUtils.join(text.iterator(), "")));
        }
        System.out.println(String.format("Inference time: %.2f ms", tickMeter.getTimeMilli()));
        HighGui.imshow("frame", frame);
        HighGui.waitKey();
    }

    @Test
    public void rainTest() {
        String dataset = "C:\\Users\\Ouyang\\Desktop\\derain\\dataset";
        String road = "C:\\Users\\Ouyang\\Desktop\\derain\\nature";
        for (int i = 1; i <= 84; i++) {
            String rain = dataset + "/rain_image/" + i + "_rain.png";
            String clean = dataset + "/clean_image/" + i + "_clean.png";
            Mat mat = Imgcodecs.imread(road + "/" + i + ".jpg");
            Imgproc.resize(mat, mat, new Size(360, 240));
            Imgcodecs.imwrite(rain, mat);
            Imgcodecs.imwrite(clean, mat);
        }
    }

    @Test
    public void slideTest() {
        JsonNode json = DPUtil.parseJSON(FileUtil.getContent(getClass().getClassLoader().getResource("opencv-verfiy-slide.json"), false, StandardCharsets.UTF_8));
        VerifyService verifyService = new VerifyService();
        Mat image = cvService.imdecode(json.get("image").asText(), Imgcodecs.IMREAD_COLOR);
        Mat slide = cvService.imdecode(json.get("slide").asText(), Imgcodecs.IMREAD_COLOR);
        Mat result = image.clone();
        List<Rect> rects = verifyService.slide(image, slide, result);
        HighGui.imshow("result", result);
        HighGui.imshow("slide", slide);
        System.out.println(rects);
        HighGui.waitKey();
    }

    @Test
    public void roadTest() {
        JsonNode json = DPUtil.parseJSON(FileUtil.getContent(getClass().getClassLoader().getResource("opencv-verify-road.json"), false, StandardCharsets.UTF_8));
        VerifyService verifyService = new VerifyService();
        Mat image = cvService.imdecode(json.get("image").asText(), Imgcodecs.IMREAD_COLOR);
        Mat result = image.clone();
        List<Point> lines = verifyService.road(image, 10, result);
        HighGui.imshow("result", result);
        System.out.println(DPUtil.stringify(lines));
        HighGui.waitKey();
    }

    @Test
    public void deroadTest() {
        MrcnnService mrcnnService = new TensorFlowTests().mrcnnService;
        JsonNode json = DPUtil.parseJSON(FileUtil.getContent(getClass().getClassLoader().getResource("opencv-verify-road.json"), false, StandardCharsets.UTF_8));
        VerifyService verifyService = new VerifyService();
        Mat image = cvService.imdecode(json.get("image").asText(), Imgcodecs.IMREAD_COLOR);
        HighGui.imshow("image", image.clone());
        Mat result = Mat.zeros(image.size(), image.type());
        mrcnnService.mrcnn(image, result, null, Scalar.all(255));
        HighGui.imshow("mrcnn", result.clone());
        List<Point> lines = verifyService.road(result, 10, image);
        HighGui.imshow("road", image.clone());
        System.out.println(DPUtil.stringify(lines));
        HighGui.waitKey();
    }

}
