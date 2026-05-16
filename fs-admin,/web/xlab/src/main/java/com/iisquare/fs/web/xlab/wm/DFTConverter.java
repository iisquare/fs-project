package com.iisquare.fs.web.xlab.wm;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class DFTConverter implements Converter {
    @Override
    public Mat start(Mat src) {
        src.convertTo(src, CvType.CV_32F);
        List<Mat> planes = new ArrayList<>(2);
        Mat com = new Mat();
        planes.add(0, src);
        planes.add(1, Mat.zeros(src.size(), CvType.CV_32F));
        Core.merge(planes, com);
        Core.dft(com, com);
        return com;
    }

    @Override
    public void inverse(Mat com) {
        List<Mat> planes = new ArrayList<>(2);
        Core.idft(com, com);
        Core.split(com, planes);
        Core.normalize(planes.get(0), com, 0, 255, Core.NORM_MINMAX, CvType.CV_8UC3);
    }

    @Override
    public void addTextWatermark(Mat com, String watermark) {
        Scalar s = new Scalar(0, 0, 0, 0);
        Point p = new Point(com.cols() / 3, com.rows() / 3);
        Imgproc.putText(com, watermark, p, Imgproc.FONT_HERSHEY_COMPLEX, 1.0, s, 3,
                8, false);
        Core.flip(com, com, -1);
        Imgproc.putText(com, watermark, p, Imgproc.FONT_HERSHEY_COMPLEX, 1.0, s, 3,
                8, false);
        Core.flip(com, com, -1);
    }

    @Override
    public void addImageWatermark(Mat com, Mat watermark) {
        List<Mat> planes = new ArrayList<>(2);
        List<Mat> newPlanes = new ArrayList<>(2);
        Mat temp = new Mat();
        int col = (com.cols() - watermark.cols()) >> 1;
        int row = ((com.rows() >> 1) - watermark.rows()) >> 1;
        watermark.convertTo(watermark, CvType.CV_32F);
        Core.copyMakeBorder(watermark, watermark, row, row, col, col, Core.BORDER_CONSTANT, Scalar.all(0));
        planes.add(0, watermark);
        Core.flip(watermark, temp, -1);
        planes.add(1, temp);
        Core.vconcat(planes, watermark);

        newPlanes.add(0, watermark);
        newPlanes.add(1, watermark);
        Core.merge(newPlanes, watermark);
        WMUtil.fixSize(watermark, com);
        Core.addWeighted(watermark, 8, com, 1, 0.0, com);

        Core.split(com, planes);
    }

    @Override
    public Mat showWatermark(Mat src) {
        List<Mat> newPlanes = new ArrayList<>(2);
        Mat mag = new Mat();
        Core.split(src, newPlanes);
        Core.magnitude(newPlanes.get(0), newPlanes.get(1), mag);
        Core.add(Mat.ones(mag.size(), CvType.CV_32F), mag, mag);
        Core.log(mag, mag);
        mag.convertTo(mag, CvType.CV_8UC1);
        Core.normalize(mag, mag, 0, 255, Core.NORM_MINMAX, CvType.CV_8UC1);
        return mag;
    }

    /**
     * 频谱图（Spectrum Diagram）
     * @param complex 傅里叶变换后的图片，含虚实两个通道
     */
    public Mat shift(Mat complex) {
        List<Mat> planes = new ArrayList<>();
        Core.split(complex, planes); // 分离通道， planes[0] 为实数部分，planes[1]为虚数部分
        Core.magnitude(planes.get(0), planes.get(1), planes.get(0)); // 求模
        Mat mag = planes.get(0);
        Core.add(mag, new Scalar(1), mag);
        Core.log(mag, mag); // 模的对数
        // crop the spectrum, if it has an odd number of rows or columns
        mag = new Mat(mag, new Rect(0, 0, mag.cols() & -2, mag.rows() & -2)); // 保证偶数的边长
        int cx = mag.cols() / 2;
        int cy = mag.rows() / 2;
        // rearrange the quadrants of Fourier image //对傅立叶变换的图像进行重排，4个区块，从左到右，从上到下
        // 分别为q0, q1, q2, q3
        // so that the origin is at the image center // 对调q0和q3, q1和q2
        Mat tmp = new Mat();
        Mat q0 = new Mat(mag, new Rect(0, 0, cx, cy));
        Mat q1 = new Mat(mag, new Rect(cx, 0, cx, cy));
        Mat q2 = new Mat(mag, new Rect(0, cy, cx, cy));
        Mat q3 = new Mat(mag, new Rect(cx, cy, cx, cy));
        q0.copyTo(tmp);
        q3.copyTo(q0);
        tmp.copyTo(q3);
        q1.copyTo(tmp);
        q2.copyTo(q1);
        tmp.copyTo(q2);
        // 规范化值到 0~1 显示图片的需要 归一化
        Core.normalize(mag, mag, 0, 255, Core.NORM_MINMAX, CvType.CV_8UC1, new Mat());
        mag.convertTo(mag, CvType.CV_8U);
        return mag;
    }
}
