package com.iisquare.fs.web.xlab.service;

import com.iisquare.fs.web.xlab.wm.DFTConverter;
import com.iisquare.fs.web.xlab.wm.WMUtil;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.photo.Photo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class WatermarkService {

    /**
     * 水印替换，采用小图替换大图中的水印部分
     */
    public boolean substitute(Mat imageMarked, Mat imageReplace, Mat imageMask, Rect rect) {
        if(imageReplace.size() != imageMarked.size()) {
            Imgproc.resize(imageReplace, imageReplace, imageMarked.size(), 0, 0, Imgproc.INTER_LANCZOS4);
        }
        if(imageMask.size() != imageMarked.size()) {
            Imgproc.resize(imageMask, imageMask, imageMarked.size(), 0, 0, Imgproc.INTER_NEAREST);
        }
        if (null != rect) {
            Mat roi = imageReplace.submat(rect);
            Photo.fastNlMeansDenoising(roi, roi);
        }
        imageReplace.copyTo(imageMarked, imageMask);
        return true;
    }

    /**
     * 打水印，将透明水印合并在图片上
     */
    public boolean combine(Mat background, Mat overlay, Point location) {
        int locationX = null == location ? 0 : (int) location.x;
        int locationY = null == location ? 0 : (int) location.y;
        int bCols = background.cols(), bRows = background.rows(), channels = background.channels();
        int oCols = overlay.cols(), oRows = overlay.rows();
        for (int y = Math.max(locationY, 0); y < bRows; ++y) {
            int fY = y - locationY;
            if (fY >= oRows) break;
            for (int x = Math.max(locationX, 0); x < bCols; ++x) {
                int fX = x - locationX;
                if (fX >= oCols) break;
                double[] overlayRGBA = overlay.get(fY, fX);
                double[] backgroundRGBA = background.get(y, x);
                double opacity = overlayRGBA[overlayRGBA.length - 1] / 255;
                if(opacity == 0) continue;
                for (int c = 0; c < channels; ++c) {
                    backgroundRGBA[c] = backgroundRGBA[c] * (1 - opacity) + overlayRGBA[c] * opacity;
                }
                background.put(y, x, backgroundRGBA);
            }
        }
        return true;
    }

    /**
     * 去水印，通过逆向打水印过程还原图片
     */
    public boolean detach(Mat background, Mat overlay, Point location) {
        int locationX = null == location ? 0 : (int) location.x;
        int locationY = null == location ? 0 : (int) location.y;
        int bCols = background.cols(), bRows = background.rows(), channels = background.channels();
        int oCols = overlay.cols(), oRows = overlay.rows();
        for (int y = Math.max(locationY, 0); y < bRows; ++y) {
            int fY = y - locationY;
            if (fY >= oRows) break;
            for (int x = Math.max(locationX, 0); x < bCols; ++x) {
                int fX = x - locationX;
                if (fX >= oCols) break;
                double[] overlayRGBA = overlay.get(fY, fX);
                double[] backgroundRGBA = background.get(y, x);
                double opacity = overlayRGBA[overlayRGBA.length - 1] / 255;
                if(opacity == 0) continue;
                for (int c = 0; c < channels; ++c) {
                    backgroundRGBA[c] = (backgroundRGBA[c] - overlayRGBA[c] * opacity) / (1 - opacity);
                }
                background.put(y, x, backgroundRGBA);
            }
        }
        return true;
    }

    public void blurAfterDetach(Mat background, Mat image, Mat overlay) {
        Mat mask = new Mat();
        Core.absdiff(background, image, mask);
        Imgproc.cvtColor(mask, mask, Imgproc.COLOR_RGB2GRAY);
        Double radius = 1.0;
        int flags = Photo.INPAINT_TELEA;
        Photo.inpaint(image, mask, image, radius, flags);
        Imgproc.GaussianBlur(image, image, new Size(3, 3), 1.0, 1.0);
    }

    /**
     * 添加盲水印
     * @param image IMREAD_COLOR 原始图片
     * @param watermark IMREAD_GRAYSCALE 水印图片
     * @param text 水印文本
     */
    public Mat encode(Mat image, Mat watermark, String text) {
        List<Mat> images = new ArrayList<>();
        Core.split(image, images); // 拆分通道
        DFTConverter converter = new DFTConverter();
        for (int index = 0; index < images.size(); index++) {
            Mat mat = images.get(index);
            mat = converter.start(mat); // 正变换
            if (null != watermark) { // 添加图片盲水印
                converter.addImageWatermark(mat, watermark.clone());
            }
            if (null != text) { // 添加文字盲水印
                if (WMUtil.isAscii(text)) {
                    converter.addTextWatermark(mat, text);
                } else {
                    converter.addImageWatermark(mat, WMUtil.drawNonAscii(text));
                }
            }
            converter.inverse(mat); // 逆变换
            images.set(index, mat);
        }
        Core.merge(images, image); // 合并通道
        return image;
    }

    /**
     * 提取盲水印
     * @param image IMREAD_GRAYSCALE 添加盲水印之后的图片
     * @param bShift 是否展示频谱图
     */
    public Mat decode(Mat image, boolean bShift) {
        DFTConverter converter = new DFTConverter();
        image = converter.start(image);
        if (bShift) {
            return converter.shift(image);
        }
        return converter.showWatermark(image);
    }

}
