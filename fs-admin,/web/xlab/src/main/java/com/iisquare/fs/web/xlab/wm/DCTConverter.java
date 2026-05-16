package com.iisquare.fs.web.xlab.wm;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

public class DCTConverter implements Converter {

    @Override
    public Mat start(Mat src) {
        if ((src.cols() & 1) != 0) {
            Core.copyMakeBorder(src, src, 0, 0, 0, 1, Core.BORDER_CONSTANT, Scalar.all(0));
        }
        if ((src.rows() & 1) != 0) {
            Core.copyMakeBorder(src, src, 0, 1, 0, 0, Core.BORDER_CONSTANT, Scalar.all(0));
        }
        src.convertTo(src, CvType.CV_32F);
        Core.dct(src, src);
        return src;
    }

    @Override
    public void inverse(Mat com) {
        Core.idct(com, com);
    }

    @Override
    public void addTextWatermark(Mat com, String watermark) {
        Imgproc.putText(com, watermark,
                new Point(com.cols() >> 2, com.rows() >> 2),
                Imgproc.FONT_HERSHEY_COMPLEX, 2.0,
                new Scalar(2, 2, 2, 0), 2, 8, false);
    }

    @Override
    public void addImageWatermark(Mat com, Mat watermark) {
        Mat mask = new Mat();
        Core.inRange(watermark, new Scalar(0, 0, 0, 0), new Scalar(0, 0, 0, 0), mask);
        Mat i2 = new Mat(watermark.size(), watermark.type(), new Scalar(2, 2, 2, 0));
        i2.copyTo(watermark, mask);
        watermark.convertTo(watermark, CvType.CV_32F);
        int row = (com.rows() - watermark.rows()) >> 1;
        int col = (com.cols() - watermark.cols()) >> 1;
        Core.copyMakeBorder(watermark, watermark, row, row, col, col, Core.BORDER_CONSTANT, Scalar.all(0));
        WMUtil.fixSize(watermark, com);
        Core.addWeighted(watermark, 0.03, com, 1, 0.0, com);
    }

    @Override
    public Mat showWatermark(Mat src) {
        src.convertTo(src, Imgproc.COLOR_RGB2HSV);
        Core.inRange(src, new Scalar(0, 0, 0, 0), new Scalar(16, 16, 16, 0), src);
        Core.normalize(src, src, 0, 255, Core.NORM_MINMAX, CvType.CV_8UC1);
        return src;
    }

}
