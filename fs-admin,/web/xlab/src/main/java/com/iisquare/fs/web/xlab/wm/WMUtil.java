package com.iisquare.fs.web.xlab.wm;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

public class WMUtil {
    public static Mat read(String image, int type) {
        Mat src = Imgcodecs.imread(image, type);
        if (src.empty()) {
            System.out.println("File not found!");
            System.exit(-1);
        }
        return src;
    }

    public static void show(Mat mat) {
        HighGui.imshow(WMUtil.class.toString(), mat);
        HighGui.waitKey(0);
    }

    public static Mat optimalDft(Mat srcImg) {
        Mat padded = new Mat();
        int opRows = Core.getOptimalDFTSize(srcImg.rows());
        int opCols = Core.getOptimalDFTSize(srcImg.cols());
        Core.copyMakeBorder(srcImg, padded, 0, opRows - srcImg.rows(),
                0, opCols - srcImg.cols(), Core.BORDER_CONSTANT, Scalar.all(0));
        return padded;
    }

    public static boolean isAscii(String str) {
        return "^[ -~]+$".matches(str);
    }

    public static Mat drawNonAscii(String watermark) {
        Font font = new Font("Default", Font.PLAIN, 64);
        FontMetrics metrics = new Canvas().getFontMetrics(font);
        int width = metrics.stringWidth(watermark);
        int height = metrics.getHeight();
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics = bufferedImage.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
        graphics.setFont(font);
        graphics.setColor(Color.WHITE);
        graphics.drawString(watermark, 0, metrics.getAscent());
        graphics.dispose();
        byte[] pixels = ((DataBufferByte) bufferedImage.getRaster().getDataBuffer()).getData();
        Mat res = new Mat(bufferedImage.getHeight(), bufferedImage.getWidth(), CvType.CV_8U);
        res.put(0, 0, pixels);
        return res;
    }

    public static void fixSize(Mat src, Mat mirror) {
        if (src.rows() != mirror.rows()) {
            Core.copyMakeBorder(src, src, 0, mirror.rows() - src.rows(),
                    0, 0, Core.BORDER_CONSTANT, Scalar.all(0));
        }
        if (src.cols() != mirror.cols()) {
            Core.copyMakeBorder(src, src, 0, 0,
                    0, mirror.cols() - src.cols(), Core.BORDER_CONSTANT, Scalar.all(0));
        }
    }
}
