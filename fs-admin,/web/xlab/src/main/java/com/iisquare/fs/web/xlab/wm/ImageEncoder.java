package com.iisquare.fs.web.xlab.wm;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class ImageEncoder extends Encoder {
    public ImageEncoder(Converter converter) {
        super(converter);
    }

    @Override
    public void addWatermark(Mat com, String watermark) {
        this.converter.addImageWatermark(com, WMUtil.read(watermark, CvType.CV_8U));
    }
}
