package com.iisquare.fs.web.xlab;

import org.junit.Test;
import org.opencv.core.*;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgcodecs.Imgcodecs;

public class OnnxTests {

    @Test
    public void demoTest() {
        String modelPath = "model.onnx";
        String imPath = "path to image";
        Net net = Dnn.readNet(modelPath);
        Mat frame = Imgcodecs.imread(imPath);
        int inpWidth = frame.width() / 32 * 32;
        int inpHeight = frame.height() / 32 * 32;
        Mat blob = Dnn.blobFromImage(frame, 1.0, new Size(inpWidth, inpHeight), new Scalar(123.68, 116.78, 103.94), true, false);
        TickMeter tickMeter = new TickMeter();
        MatOfInt shape = new MatOfInt(1, blob.channels(), blob.height(), blob.width());
        net.setInput(blob, "input");
        net.setInputShape("input", shape);

        tickMeter.start();
        Mat result = net.forward();
        tickMeter.stop();

        System.out.println(result.dump());
        String label = String.format("Inference time: %.2f ms", tickMeter.getTimeMilli());
        System.out.println(label);
    }

}
