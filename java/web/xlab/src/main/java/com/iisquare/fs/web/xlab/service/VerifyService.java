package com.iisquare.fs.web.xlab.service;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class VerifyService extends ServiceBase {

    protected final static Logger logger = LoggerFactory.getLogger(VerifyService.class);

    public ArrayNode track(Mat slide, List<Rect> rects) {
        ArrayNode nodes = DPUtil.arrayNode();
        for (Rect rect : rects) {
            ObjectNode node = DPUtil.objectNode();
            node.replace("rect", DPUtil.convertJSON(rect));
            int targetX = rect.x, targetY = 0; // 实际目标
            int offsetX = 8, offsetY = 5; // 目标偏移
            int locationX = 0, locationY = 0; // 已运动距离
            int timeTotal = 1500 + 1000 * targetX / 480; // 运动时长（毫秒）
            int timeInterval = 17; // 触发间隔（毫秒），肉眼60HZ约17ms，JS.onmousemove约8ms
            node.put("interval", timeInterval);
            List<Integer> align = Arrays.asList(-3, -4, -3, 2); // 模拟对齐，补偿offsetX目标偏移
            // 三阶贝塞尔曲线@see(https://www.cnblogs.com/hnfxs/p/3148483.html)
            RealVector P0 = new ArrayRealVector(new double[] {0, 0});
            RealVector P1 = new ArrayRealVector(new double[] {(targetX + offsetX) * 3 / 10, targetY - 5});
            RealVector P2 = new ArrayRealVector(new double[] {(targetX + offsetX) * 8 / 10, targetY + 7});
            RealVector P3 = new ArrayRealVector(new double[] {targetX + offsetX, targetY + offsetY});
            node.put("startX", 27).put("startY", 18);
            ArrayNode array = node.putArray("move");
            for (double time = 0; time <= timeTotal; time += timeInterval) { // 运动操作
                double t = time / timeTotal;
                RealVector P = P0.mapMultiply(Math.pow(1 - t, 3))
                        .add(P1.mapMultiply(3 * t * Math.pow(1 - t, 2)))
                        .add(P2.mapMultiply(3 * Math.pow(t, 2) * (1 - t)))
                        .add(P3.mapMultiply(Math.pow(t, 3)));
                int x = (int) (P.getEntry(0) - locationX), y = (int) (P.getEntry(1) - locationY);
                locationX += x;
                locationY += y;
                logger.info("drag move (" + x + ", " + y + ", " + locationX + ", " + locationY + ")");
                array.add(DPUtil.objectNode().put("x", x).put("y", y));
            }
            array = node.putArray("align");
            for (Integer x : align) { // 对齐操作
                int y = (int) (Math.random() * offsetY * (locationY > 0 ? -1 : 1));
                locationX += x; // 偏移补偿
                locationY += y; // Y轴偏移适当抖动，不需要精确对齐
                logger.info("align move (" + x + ", " + y + ", " + locationX + ", " + locationY + ")");
                array.add(DPUtil.objectNode().put("x", x).put("y", y));
            }
            if (locationX != targetX) { // 精确定位
                int x = targetX - locationX, y = (int) (Math.random() * offsetY * (locationY > 0 ? -1 : 1));
                locationX += x;
                locationY += y;
                logger.info("target move (" + x + ", " + y + ", " + locationX + ", " + locationY + ")");
                node.put("stopX", x).put("stopY", y);
            } else {
                node.put("stopX", 0).put("stopY", 0);
            }
            nodes.add(node);
        }
        return nodes;
    }

    /**
     * 滑块验证
     */
    public List<Rect> slide(Mat image, Mat slide, Mat result) {
        Imgproc.cvtColor(image, image, Imgproc.COLOR_RGB2GRAY);
        Core.inRange(image, Scalar.all(150), Scalar.all(240), image);
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(image.clone(), contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_NONE);
        if (null != result) Imgproc.drawContours(result, contours, -1, new Scalar(0));
        List<Rect> rects = new ArrayList<>();
        for (MatOfPoint contour : contours) {
            Rect rect = Imgproc.boundingRect(contour);
            if (rect.x < 3 || rect.x > image.width() - rect.width - 3) continue;
            if(rect.width < 40 || rect.width > 60) continue;
            if(rect.height < 40 || rect.height > 60) continue;
            if(Math.abs(rect.width - rect.height) > 15) continue;
            if(rect.height * rect.height > 3025) continue;
            rects.add(rect);
            if (null != result) Imgproc.rectangle(result, rect.tl(), rect.br(), new Scalar(0, 255, 0));
        }
        if (slide != null) {
            for (Rect rect : rects) {
                offset: for (int x = 0; x < slide.width(); x++) {
                    for (int y = 0; y < rect.height; y++) {
                        double[] doubles = slide.get(rect.y + y, x);
                        if (doubles[0] > 0 || doubles[1] > 0 || doubles[2] > 0) {
                            rect.x -= x;
                            if (rect.x < 0) rect.x = 0;
                            break offset;
                        }
                    }
                }
            }
        }
        return rects;
    }

    public Mat diffMax(Mat image, Size kernelSize) {
        int w = (int) Math.floor(kernelSize.width / 2), h = (int) Math.floor(kernelSize.height / 2);
        Mat mask = Mat.zeros(image.size(), image.type());
        for (int y = w; y < image.rows() - w; y++) {
            for (int x = h; x < image.cols() - h; x++) {
                double max = 0, color = image.get(y, x)[0];
                for (int k = 0; k < kernelSize.height; k++) {
                    for (int j = 0; j < kernelSize.width; j++) {
                        double c = image.get(y - w + k, x - h + j)[0];
                        if (c > color) continue;
                        max = Math.max(max, color - c);
                    }
                }
                mask.put(y, x, Math.min(255, max * 10));
            }
        }
        return mask;
    }

    public Mat lineReduce(Mat image, int interval) {
        int half = (int) Math.floor(interval / 2), min = 10;
        int[][][] poi = new int[][][]{
            new int[][]{new int[]{-1 ,-1}, new int[]{-1, 0}, new int[]{-1, 1}},
            new int[][]{new int[]{0 ,-1}, new int[]{0, 0}, new int[]{0, 1}},
            new int[][]{new int[]{1 ,-1}, new int[]{1, 0}, new int[]{1, 1}}
        };
        Mat mask = Mat.zeros(image.size(), image.type());
        for (int y = interval; y < image.rows() - interval; y++) {
            for (int x = interval; x < image.cols() - interval; x++) {
                double color = image.get(y, x)[0];
                if (color < min) continue;
                r: for (int k = 0; k < 3; k++) {
                    for (int j = 0; j < 3; j++) {
                        int[] point = poi[k][j];
                        if (point[0] == point[1]) continue;
                        double ic = image.get(y + point[0] * interval, x + point[1] * interval)[0];
                        if (ic < min) continue;
                        double ih = image.get(y + point[0] * half, x + point[1] * half)[0];
                        if (ih > min) continue;
                        mask.put(y, x, color);
                        break r;
                    }
                }
            }
        }
        return mask;
    }

    /**
     * 绘制路径
     */
    public List<Point> road(Mat image, int step, Mat result) {
        Size kernelSize = new Size(3, 3);
        Imgproc.cvtColor(image, image, Imgproc.COLOR_BGR2GRAY);
//        image = diffMax(image, kernelSize);
//        image = lineReduce(image, (int) (kernelSize.height * 2 + 2));
        Imgproc.Canny(image, image, 100, 200, 3); // 查找边缘
//        image.copyTo(result); if (true) return null;
//        Imgproc.dilate(image, image, Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, kernelSize));
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(image.clone(), contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);
        List<MatOfPoint> paths = new ArrayList<>();
        for (MatOfPoint contour : contours) {
            Rect rect = Imgproc.boundingRect(contour);
            if (rect.width < 50) continue;
            if(rect.height * rect.height < 4000) continue;
            paths.add(contour);
            if (null != result) Imgproc.rectangle(result, rect.tl(), rect.br(), Scalar.all(paths.size() * 100 % 255));
        }
        if (null != result) Imgproc.drawContours(result, paths, -1, new Scalar(0));
        List<Point> list = new ArrayList<>();
        WeightedObservedPoints obs = new WeightedObservedPoints();
        for (MatOfPoint contour : paths) {
            for (int x = 0; x < contour.cols(); x++) {
                for (int y = 0; y < contour.rows(); y += step) {
                    Point point = new Point(contour.get(y, x));
                    if (point.x < 20 || point.x > image.cols() - 20) continue;
                    if (point.y < 20 || point.y > image.rows() - 20) continue;
                    list.add(point);
                    obs.add(point.x, point.y);
                    if (null != result) {
                        Imgproc.putText(result, String.valueOf(y), point, Imgproc.FONT_HERSHEY_COMPLEX_SMALL, 0.3, new Scalar(y));
                    }
                }
            }
        }
        if (list.size() > 5) {
            PolynomialCurveFitter fitter = PolynomialCurveFitter.create(6);
            PolynomialFunction fitted = new PolynomialFunction(fitter.fit(obs.toList()));
            for (Point point : list) {
                point.y = fitted.value(point.x);
            }
        }
        Collections.sort(list, (o1, o2) -> (int) (o1.x - o2.x));
        List<Point> line = new ArrayList<>();
        for (Point point : list) {
            if (line.size() == 0) {
                line.add(point);
                continue;
            }
            Point compare = line.get(line.size() - 1);
            double distance = Math.sqrt(Math.pow(point.y - compare.y, 2) + Math.pow(point.x - compare.x, 2));
            if (distance > step) {
                line.add(point);
            }
        }
        if (null != result) {
            for (Point point : line) {
                Imgproc.drawMarker(result, point, new Scalar(0, 0, 255));
            }
        }
        return list;
    }

}
