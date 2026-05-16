package com.iisquare.fs.web.xlab.wm;

import org.opencv.core.CvType;
import org.opencv.imgcodecs.Imgcodecs;

public class Decoder {
    private Converter converter;

    public Decoder(Converter converter) {
        this.converter = converter;
    }

    public Converter getConverter() {
        return converter;
    }

    public void setConverter(Converter converter) {
        this.converter = converter;
    }

    public void decode(String image, String output) {
        Imgcodecs.imwrite(output, this.converter.showWatermark(this.converter.start(WMUtil.read(image, CvType.CV_8U))));
    }
}
