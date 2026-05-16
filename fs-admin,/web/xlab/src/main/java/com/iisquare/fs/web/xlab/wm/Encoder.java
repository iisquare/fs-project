package com.iisquare.fs.web.xlab.wm;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;

import java.util.ArrayList;
import java.util.List;

public abstract class Encoder {
    Converter converter;

    Encoder(Converter converter) {
        this.converter = converter;
    }

    public Converter getConverter() {
        return converter;
    }

    public void setConverter(Converter converter) {
        this.converter = converter;
    }

    public void encode(String image, String watermark, String output) {
        Mat src = WMUtil.read(image, CvType.CV_8S);

        List<Mat> channel = new ArrayList<>(3);
        List<Mat> newChannel = new ArrayList<>(3);
        Core.split(src, channel);

        for (int i = 0; i < 3; i++) {
            Mat com = this.converter.start(channel.get(i)).clone();
            this.addWatermark(com, watermark);
            this.converter.inverse(com);
            newChannel.add(i, com);
        }

        Mat res = new Mat();
        Core.merge(newChannel, res);

        if (res.rows() != src.rows() || res.cols() != src.cols()) {
            res = new Mat(res, new Rect(0, 0, src.width(), src.height()));
        }

        Imgcodecs.imwrite(output, res);
    }

    public abstract void addWatermark(Mat com, String watermark);
}
