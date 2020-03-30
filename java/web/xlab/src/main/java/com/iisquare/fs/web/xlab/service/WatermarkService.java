package com.iisquare.fs.web.xlab.service;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.photo.Photo;
import org.springframework.stereotype.Service;

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
        for (int y = Math.max(locationY, 0); y < bCols; ++y) {
            int fY = y - locationY;
            if (fY >= oCols) break;
            for (int x = Math.max(locationX, 0); x < bRows; ++x) {
                int fX = x - locationX;
                if (fX >= oRows) break;
                double[] overlayRGBA = overlay.get(fX, fY);
                double[] backgroundRGBA = background.get(x, y);
                double opacity = overlayRGBA[overlayRGBA.length - 1] / 255;
                if(opacity == 0) continue;
                for (int c = 0; c < channels; ++c) {
                    backgroundRGBA[c] = backgroundRGBA[c] * (1 - opacity) + overlayRGBA[c] * opacity;
                }
                background.put(x, y, backgroundRGBA);
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
        for (int y = Math.max(locationY, 0); y < bCols; ++y) {
            int fY = y - locationY;
            if (fY >= oCols) break;
            for (int x = Math.max(locationX, 0); x < bRows; ++x) {
                int fX = x - locationX;
                if (fX >= oRows) break;
                double[] overlayRGBA = overlay.get(fX, fY);
                double[] backgroundRGBA = background.get(x, y);
                double opacity = overlayRGBA[overlayRGBA.length - 1] / 255;
                if(opacity == 0) continue;
                for (int c = 0; c < channels; ++c) {
                    backgroundRGBA[c] = (backgroundRGBA[c] - overlayRGBA[c] * opacity) / (1 - opacity);
                }
                background.put(x, y, backgroundRGBA);
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

}
