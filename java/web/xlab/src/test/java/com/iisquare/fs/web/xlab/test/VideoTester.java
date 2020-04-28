package com.iisquare.fs.web.xlab.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.web.xlab.service.MaskService;
import com.iisquare.fs.web.xlab.service.OpenCVService;
import com.iisquare.fs.web.xlab.service.WatermarkService;
import com.iisquare.fs.web.xlab.ui.ImageGui;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.junit.Test;
import org.opencv.core.*;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.Videoio;

import java.io.*;
import java.net.URL;
import java.util.*;

public class VideoTester {

    public String dirName = "C:\\Users\\Ouyang\\Desktop\\video\\full";
    public String fileName = "db6148cc-c2b6-4fcc-8881-9eafca330a12.1578554250585.51544-186410545.mp4";
    public OpenCVService cvService = new OpenCVService();
    public MaskService maskService = new MaskService();
    public WatermarkService watermarkService = new WatermarkService();

    public VideoTester() {
        cvService.init(null);
        maskService.cvService = cvService;
    }

    public ArrayNode offset() throws IOException {
        URL url = getClass().getClassLoader().getResource("video-offset.txt");
        File file = new File(url.getFile());
        InputStream inputStream = new FileInputStream(file);
        InputStreamReader inputReader = new InputStreamReader(inputStream);
        BufferedReader bufferReader = new BufferedReader(inputReader);
        String text;
        ArrayNode nodes = DPUtil.arrayNode();
        while ((text = bufferReader.readLine()) != null) {
            ObjectNode node = nodes.addObject();
            String[] arr = text.split(",");
            if(3 != arr.length) continue;
            node.put("id", arr[0]);
            node.put("name", arr[1]);
            node.put("offset", arr[2]);
        }
        FileUtil.close(bufferReader, inputReader, inputStream);
        return nodes;
    }

    @Test
    /**
     * Pytorch版Faster R-CNN训练自己数据集:http://www.pianshen.com/article/6123333676/
     * 神经网络模型：https://github.com/jwyang/faster-rcnn.pytorch
     * 数据集生成工具：https://github.com/murphypei/create-pascal-voc-dataset
     */
    public void boxTest() throws Exception {
        for (JsonNode node : offset()) {
            int id = node.get("id").asInt();
            String name = node.get("name").asText();
            double offset = node.get("offset").asDouble();
            System.out.println("process id " + id + " with name " + name);
            String fileName = dirName + "/" + name+ ".mp4";
            VideoCapture capture = new VideoCapture();
            if (!capture.open(fileName)) {
                System.out.println("打开文件（" + fileName + "）失败！");
                capture.release();
                continue;
            }
            int width = (int) capture.get(Videoio.CAP_PROP_FRAME_WIDTH);
            int height = (int) capture.get(Videoio.CAP_PROP_FRAME_HEIGHT);
            double count = Math.min(capture.get(Videoio.CAP_PROP_FRAME_COUNT), 960);
            double fps = capture.get(Videoio.CAP_PROP_FPS), repeat = count;
            Mat mask = Mat.zeros(new Size(width + repeat * offset, height), CvType.CV_8UC4);
            SortedMap<Integer, Rect> wrapper = new TreeMap<>();
            if (!maskService.lingan(mask, "logo_lingan.jpg", wrapper)) {
                System.out.println("生成水印图失败！");
                capture.release();
                return;
            }
            Imgproc.cvtColor(mask, mask, Imgproc.COLOR_RGB2GRAY);
            ImageGui gui = new ImageGui();
            gui.window(fileName, new java.awt.Dimension(width, height));
            for (int index = 360; index < count; index += fps) {
                System.out.println("process id " + id + " with name " + name + " in " + index + "/" + count);
                Mat frame = new Mat();
                capture.set(Videoio.CAP_PROP_POS_FRAMES, index);
                if (!capture.read(frame)) continue;
                String out = "frame_" + id + "_" + index;
                Imgcodecs.imwrite(out + ".jpg", frame);
                double shift = index % repeat * offset;
                int left = mask.width() - width - (int) shift;
                Rect rect = new Rect(left, 0, width, height);
                Mat logo = mask.submat(rect);
                System.out.print("/path/to/" + out + ".jpg");
                for (Map.Entry<Integer, Rect> entry : wrapper.entrySet()) {
                    Rect roi = maskService.wrapper(rect, entry.getValue());
                    if (null == roi) continue;
                    Mat mat = frame.submat(roi), image = mat.clone();
                    Core.divide(image, Scalar.all(255), image);
                    image.copyTo(mat, logo.submat(roi));
                    System.out.print(DPUtil.implode(" ", new Object[]{"", "lingan", roi.x, roi.y, roi.width, roi.height}));
                }
                System.out.println();
                gui.imshow(cvService.bufferedImage(frame));
                gui.repaint();
            }
            capture.release();
        }
    }

    @Test
    public void fullTest() {
        String logo = "/Users/fqq/Documents/work/fs-project/static/resources/images/logo/logo_lingan.png";
        String full = "/Users/fqq/Documents/work/did-mdn/facades/images/logo_lingan.png";
        Mat mat = Mat.zeros(512, 512 * 10, CvType.CV_8UC4);
        logo = "file://" + logo;
        maskService.lingan(mat, logo, null);
        Imgcodecs.imwrite(full, mat);
    }

    @Test
    public void offsetTest() {
        String fileName = dirName + "/" + this.fileName;
        VideoCapture capture = new VideoCapture();
        if (!capture.open(fileName)) {
            System.out.println("打开文件（" + fileName + "）失败！");
            capture.release();
            return;
        }
        int width = (int) capture.get(Videoio.CAP_PROP_FRAME_WIDTH);
        int height = (int) capture.get(Videoio.CAP_PROP_FRAME_HEIGHT);
        double count = capture.get(Videoio.CAP_PROP_FRAME_COUNT);
        double fps = capture.get(Videoio.CAP_PROP_FPS);
        boolean debug = false;
        double lowRate = 0.02, minRate = 1.0, maxRate = 3.0, step = Math.max(fps / 16, 1), checkOffset = 720;
        Mat logo = Mat.zeros(new Size(width + checkOffset * maxRate, height), CvType.CV_8UC4);
        SortedMap<Integer, Rect> wrapper = new TreeMap<>();
        if (!maskService.lingan(logo, "logo_lingan.jpg", wrapper)) {
            System.out.println("生成水印图失败！");
            capture.release();
            return;
        }
        Mat mask = new Mat();
        Imgproc.cvtColor(logo, mask, Imgproc.COLOR_BGR2GRAY);
        Mat ke = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(7, 7));
        Mat kd = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(21, 21));
        WeightedObservedPoints obs1 = new WeightedObservedPoints();
        WeightedObservedPoints obs2 = new WeightedObservedPoints(), obs2r = new WeightedObservedPoints();
        for (int index = 360; index < count; index += step) {
            capture.set(Videoio.CAP_PROP_POS_FRAMES, index);
            Mat frame = new Mat();
            if (!capture.read(frame)) continue;
            Mat image = frame.clone();
            Imgproc.erode(image, image, ke);
            Core.absdiff(frame, image, image);
            Imgproc.dilate(image, image, kd);
            Imgproc.cvtColor(image, image, Imgproc.COLOR_BGR2GRAY);
            Imgproc.threshold(image, image, 10, 255, Imgproc.THRESH_BINARY);
            Mat mat = image.clone();
            Imgproc.Canny(image, image, 255, 255, 3);
            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(image.clone(), contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);
            for (MatOfPoint contour : contours) {
                Rect rect = Imgproc.boundingRect(contour);
                if (rect.width < 138 || rect.height < 72) continue;
                if (rect.width > 160 || rect.height > 100) continue;
                if (Core.mean(mat.submat(new Rect(rect.x, rect.y, 10, 10))).val[0] > 50) continue;
                if (Core.mean(mat.submat(new Rect(rect.x + rect.width - 10, rect.y + rect.height - 10, 10, 10))).val[0] > 50) continue;
                if (Core.mean(mat.submat(new Rect(rect.x + rect.width / 2 - 50, rect.y + rect.height / 2 - 10, 100, 20))).val[0] < 240) continue;
                Rect r1 = new Rect(rect.x + rect.width / 2 - 1, rect.y + rect.height / 2 - 1, 3, 3);
                if (debug) {
                    Imgproc.rectangle(frame, r1, new Scalar(0, 255, 0));
                    Imgproc.rectangle(frame, rect.tl(), rect.br(), new Scalar(255, 0, 0));
                }
                for (Map.Entry<Integer, Rect> entry : wrapper.entrySet()) {
                    Rect r2 = entry.getValue();
                    int x = r2.x + r2.width / 2, y = r2.y + r2.height / 2;
                    if (y < r1.y || y > r1.y + r1.height) continue;
                    Mat view = logo.clone();
                    int offset = logo.width() - x + r1.x + r1.width / 2 - width;
                    double rate = offset * 1D / index;
                    System.out.println("index:" + index + ", offset:" + offset + ", rate:" + rate);
                    if (rate > lowRate && rate < minRate) {
                        obs2.add(index, offset);
                        obs2r.add(offset, index);
                    } else if (rate >= minRate && rate < maxRate) {
                        obs1.add(index, offset);
                    }
                    if (debug) {
                        frame.copyTo(view.submat(new Rect(view.width() - offset - width, 0, width, height)));
                        logo.copyTo(view, mask);
                        Imgcodecs.imwrite("frame_view.png", view);
                        Imgproc.resize(view, view, new Size(1024, view.height() * 1024 / view.width()), 0, 0, Imgproc.INTER_LANCZOS4);
                        HighGui.imshow("view", view);
                        HighGui.waitKey();
                    }
                }
            }
            if (debug) {
                HighGui.imshow("frame", frame);
                HighGui.imshow("mat", mat);
                HighGui.imshow("image", image);
                HighGui.waitKey();
            }
        }
        capture.release();
        int size1 = obs1.toList().size(), size2 = obs2.toList().size();
        Double rate = null, repeat = null;
        if (size2 > 1) { // 预测循环点
            PolynomialCurveFitter fitter = PolynomialCurveFitter.create(1);
            PolynomialFunction fitted = new PolynomialFunction(fitter.fit(obs2r.toList()));
            repeat = fitted.value(0);
            System.out.println("obs1 fit repeat:" + repeat);
            if (repeat > count || repeat < checkOffset) {
                repeat = null;
            }
        }
        if (null != repeat) { // 清理超过循环点的采样点
            List<WeightedObservedPoint> list = obs1.toList();
            obs1.clear();
            for (WeightedObservedPoint point : list) {
                if (point.getX() > repeat) break;
                obs1.add(point);
            }
            size1 = obs1.toList().size();
        }
        if (size1 > 0) { // 通过一段进行拟合
            obs1.add(0, 0); // 经过原点
            PolynomialCurveFitter fitter = PolynomialCurveFitter.create(1);
            PolynomialFunction fitted = new PolynomialFunction(fitter.fit(obs1.toList()));
            rate = fitted.value(checkOffset) / checkOffset;
            System.out.println("obs1 fit rate:" + rate);
            if (rate >= maxRate || rate < minRate) rate = null;
        }
        if ((null == rate || size2 > Math.max(size1 * 1.5, 6)) && null != repeat) { // 尝试通过二段进行拟合
            PolynomialCurveFitter fitter = PolynomialCurveFitter.create(1);
            PolynomialFunction fitted = new PolynomialFunction(fitter.fit(obs2.toList()));
            double rf = fitted.value(repeat + checkOffset) / checkOffset;
            System.out.println("obs2 fit rate:" + rf);
            if (rf < maxRate && rf >= minRate) rate = rf;
        }
        if (null != rate && null == repeat) {
            if (0 == size2) {
                repeat = count;
            } else if (size2 > 0) {
                WeightedObservedPoint point = obs2.toList().get(0);
                repeat = point.getX() - point.getY() / rate;
            }
        }
        System.out.println("rate:" + rate + ", repeat:" + repeat + ", size1:" + size1 + ", size2:" + size2 + ", count:" + count);
    }

    @Test
    public void removeTest() {
        String fileName = dirName + "/" + this.fileName;
        VideoCapture capture = new VideoCapture();
        if (!capture.open(fileName)) {
            System.out.println("打开文件（" + fileName + "）失败！");
            capture.release();
            return;
        }
        boolean debug = false;
        int width = (int) capture.get(Videoio.CAP_PROP_FRAME_WIDTH);
        int height = (int) capture.get(Videoio.CAP_PROP_FRAME_HEIGHT);
        double count = capture.get(Videoio.CAP_PROP_FRAME_COUNT);
        double fps = capture.get(Videoio.CAP_PROP_FPS);
        double offset = 1.5904740017710628, repeat = 1400.3461955911525; // 每帧像素偏移和循环点
        Mat maskLogo = Mat.zeros(new Size(width + repeat * offset * 3, height), CvType.CV_8UC4);
        Mat view = new Mat(maskLogo.size(), CvType.CV_8UC3);
        SortedMap<Integer, Rect> wrapperLogo = new TreeMap<>();
        if (!maskService.lingan(maskLogo, "logo_lingan.jpg", wrapperLogo)) {
            System.out.println("生成水印图失败！");
            capture.release();
            return;
        }
        if (debug) Imgcodecs.imwrite("frame_logo.png", maskLogo);
        Imgproc.cvtColor(maskLogo, maskLogo, Imgproc.COLOR_RGB2GRAY);
        Mat maskMosaic = new Mat(maskLogo.size(), maskLogo.type());
        SortedMap<Integer, Rect> wrapperMosaic = new TreeMap<>();
        if (!maskService.lingan(maskMosaic, "mosaic_lingan.png", wrapperMosaic)) {
            System.out.println("生成掩码图失败！");
            capture.release();
            return;
        }
        Imgproc.cvtColor(maskMosaic, maskMosaic, Imgproc.COLOR_RGB2GRAY);
        if (debug) Imgcodecs.imwrite("frame_mosaic.png", maskMosaic);
        ImageGui gui = new ImageGui();
        gui.window(this.fileName, new java.awt.Dimension(width, height));
        Mat frame = new Mat();
        int index = 0;
        VideoWriter writer = new VideoWriter();
        Size size = new Size(width, height);
        writer.open(fileName.replace(".mp4", "_w.mp4"),
                (int) capture.get(Videoio.CAP_PROP_FOURCC), capture.get(Videoio.CAP_PROP_FPS), size);
        Scalar sa = Scalar.all(255 * 0.04), sb = Scalar.all(0.96);
        while (capture.read(frame)) {
            double shift = index++ % repeat * offset;
            System.out.println("process " + index + " / " + count + " ...");
            if (debug && index == 720 && offset == 1) {
                Imgcodecs.imwrite("frame_" + index + ".jpg", frame);
                break;
            }
            int left = view.width() - width - (int) shift;
            Rect rect = new Rect(left, 0, width, height);
            Mat roi = view.submat(rect);
            frame.copyTo(roi);
            Mat logo = maskLogo.submat(rect), mosaic = maskMosaic.submat(rect);
            for (Map.Entry<Integer, Rect> entry : wrapperLogo.entrySet()) {
                Rect wrapper = maskService.wrapper(rect, entry.getValue());
                if (null == wrapper) continue;
                Imgproc.rectangle(frame, wrapper, new Scalar(0, 255, 0));
//                Mat mat = frame.submat(wrapper), image = mat.clone();
//                Core.absdiff(image, sa, image);
//                Core.divide(image, sb, image);
//                image.copyTo(mat, logo.submat(wrapper));
//                watermarkService.detach(frame.submat(wrapper), logo.submat(wrapper), null);
            }
//            for (Map.Entry<Integer, Rect> entry : wrapperMosaic.entrySet()) {
//                Rect wrapper = maskService.wrapper(rect, entry.getValue());
//                if (null == wrapper) continue;
//                Mat mat = frame.submat(wrapper), image = mat.clone();
////                Imgproc.bilateralFilter(mat, image, 0, 120, 5);
//                Ximgproc.fastGlobalSmootherFilter(mat, mat, image, 5, 120);
//                watermarkService.substitute(mat, image, mosaic.submat(wrapper), null);
//            }
            writer.write(frame);
            gui.imshow(cvService.bufferedImage(frame));
            gui.repaint();
        }
        if (debug) {
            System.out.println("w:" + width);
            System.out.println("h:" + height);
            System.out.println("f:" + fps);
            System.out.println("c:" + count);
            System.out.println("l:" + maskLogo.width());
            Imgcodecs.imwrite("frame_view.png", view);
        }
        capture.release();
        writer.release();
    }

}
