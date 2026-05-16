package com.iisquare.fs.web.xlab.wm;

import org.opencv.core.Mat;

public class TextEncoder extends Encoder {
    public TextEncoder(Converter converter) {
        super(converter);
    }

    @Override
    public void addWatermark(Mat com, String watermark) {
        if (WMUtil.isAscii(watermark)) {
            this.converter.addTextWatermark(com, watermark);
        } else {
            this.converter.addImageWatermark(com, WMUtil.drawNonAscii(watermark));
        }
    }
}
