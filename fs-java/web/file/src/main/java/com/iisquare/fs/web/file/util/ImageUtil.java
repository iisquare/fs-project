package com.iisquare.fs.web.file.util;

import com.iisquare.fs.base.core.util.DPUtil;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Base64;

public class ImageUtil {

    public static BufferedImage read(String imageUrl) throws Exception {
        if (!imageUrl.startsWith("data:image/")) {
            return ImageIO.read(new URL(imageUrl));
        }
        imageUrl = imageUrl.split(",")[1];
        byte[] imageBytes = Base64.getDecoder().decode(imageUrl);
        try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes)) {
            return ImageIO.read(bais);
        }
    }

    public static BufferedImage read(MultipartFile file) {
        try {
            return ImageIO.read(new ByteArrayInputStream(file.getBytes()));
        } catch (Exception e) {
            return null;
        }
    }

    public static BufferedImage scaleToPixels(BufferedImage image, long targetPixels) {
        long pixels = (long) image.getWidth() * image.getHeight();
        if (pixels <= targetPixels) return image;
        double scale = Math.sqrt((double) targetPixels / pixels);
        int newWidth = (int) (image.getWidth() * scale);
        int newHeight = (int) (image.getHeight() * scale);
        try {
            return Thumbnails.of(image).size(newWidth, newHeight).asBufferedImage();
        } catch (Exception e) {
            return image;
        }
    }

    public static String toBase64(BufferedImage image, String format) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, format, out);
            return "data:image/" + format + ";base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            return null;
        }
    }

    public static byte[] image2bytes(BufferedImage image, String format) throws IOException {
        if (!DPUtil.isItemExist(ImageIO.getWriterFormatNames(), format)) {
            throw new IOException(format + " image format is not supported");
        }
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, format, baos);
            return baos.toByteArray();
        }
    }

    public static byte[] convert2webp(BufferedImage image, int width, int height, float quality) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Thumbnails.of(image).size(width, height).outputFormat("webp").outputQuality(quality).toOutputStream(baos);
        return baos.toByteArray();
    }

    public static BufferedImage thumbnail(BufferedImage image, int size) throws Exception {
        return thumbnail(image, size, size);
    }

    public static BufferedImage thumbnail(BufferedImage image, int width, int height) throws Exception {
        // 创建黑色画布 (补0 即补黑)
        BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, width, height);
        // 缩放原图并保持比例
        BufferedImage scaled = Thumbnails.of(image).size(width, height).asBufferedImage();
        // 居中绘制
        int x = (width - scaled.getWidth()) / 2;
        int y = (height - scaled.getHeight()) / 2;
        g.drawImage(scaled, x, y, null);
        g.dispose();
        return canvas;
    }

}
