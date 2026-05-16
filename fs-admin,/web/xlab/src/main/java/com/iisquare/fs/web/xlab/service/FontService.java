package com.iisquare.fs.web.xlab.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.xlab.helper.WoffConverterHelper;
import org.apache.commons.codec.binary.Base64;
import org.apache.fontbox.ttf.CmapSubtable;
import org.apache.fontbox.ttf.GlyphData;
import org.apache.fontbox.ttf.TTFParser;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.fontbox.util.BoundingBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 字体在线编辑工具@see(https://font.qqe2.com/)
 */
@Service
public class FontService extends ServiceBase {

    protected final static Logger logger = LoggerFactory.getLogger(FontService.class);

    public TrueTypeFont fromTTFBase64(String base64) {
        base64 = base64.replaceFirst("^.*?,", "");
        InputStream in = new ByteArrayInputStream(Base64.decodeBase64(base64));
        try {
            return new TTFParser().parse(in);
        } catch (Exception e) {
            logger.warn("parse ttf base64 failed:" + base64, e);
            return null;
        } finally {
            FileUtil.close(in);
        }
    }

    public TrueTypeFont fromWoffBase64(String base64, String filename) {
        base64 = base64.replaceFirst("^.*?,", "");
        InputStream in = new ByteArrayInputStream(Base64.decodeBase64(base64));
        ByteArrayInputStream bin = null;
        try {
            byte[] bytes = new WoffConverterHelper().convertToTTFByteArray(in);
            if (!DPUtil.empty(filename)) {
                FileUtil.putContent(filename, bytes);
            }
            bin = new ByteArrayInputStream(bytes);
            return new TTFParser().parse(bin);
        } catch (Exception e) {
            logger.warn("parse woff base64 failed:" + base64, e);
            return null;
        } finally {
            FileUtil.close(in, bin);
        }
    }

    public BufferedImage image(GlyphData glyph) {
        if (null == glyph) return null;
        BoundingBox box = glyph.getBoundingBox();
        GeneralPath path = glyph.getPath();
        AffineTransform translate = new AffineTransform();
        translate.setToTranslation(-box.getLowerLeftX(), -box.getLowerLeftY());
        path.transform(translate);
        BufferedImage image = new BufferedImage((int) box.getWidth(), (int) box.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.fill(path);
        graphics.dispose();
        return align(image);
    }

    public BufferedImage align(BufferedImage origin) {
        int width = 100, height = 100, rgb = new Color(0, 0, 0, 0).getRGB();
        int w = origin.getWidth(), h = origin.getHeight(), x1 = 0, y1 =0, x2 = w, y2 = h;
        // 查找x1最大空白
        X1: for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                if (origin.getRGB(x, y) != rgb) {
                    break X1;
                }
            }
            x1 = x;
        }
        // 查找y1最大空白
        Y1: for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (origin.getRGB(x, y) != rgb) {
                    break Y1;
                }
            }
            y1 = y;
        }
        // 查找x2最大空白
        X2: for (int x = w - 1; x >= 0; x--) {
            for (int y = h - 1; y >= 0; y--) {
                if (origin.getRGB(x, y) != rgb) {
                    break X2;
                }
            }
            x2 = x;
        }
        // 查找Y2最大空白
        Y2: for (int y = h - 1; y >= 0; y--) {
            for (int x = w - 1; x >= 0; x--) {
                if (origin.getRGB(x, y) != rgb) {
                    break Y2;
                }
            }
            y2 = y;
        }
//        return origin.getSubimage(x1, y1, x2 - x1, y2 - y1);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        int xd = x2 - x1, yd = y2 - y1;
        if (xd > yd) { // 宽大于高，保持宽度
            graphics.drawImage(origin, 0, 0, width, height * yd / xd, x1, y1, x2, y2, null);
        } else { // 保持高度
            graphics.drawImage(origin, 0, 0, width * xd / yd, height, x1, y1, x2, y2, null);
        }
        graphics.dispose();
        return image;
    }

    public Map<String, BufferedImage> image(TrueTypeFont ttf, Collection<String> intersect, String filename) {
        Map<String, BufferedImage> result = new LinkedHashMap<>();
        try {
            int length = ttf.getNumberOfGlyphs();
            CmapSubtable lookup = (CmapSubtable) ttf.getUnicodeCmapLookup();
            for (int gid = 0; gid < length; gid++) {
                Integer code = lookup.getCharacterCode(gid);
                String unicode = null == code || -1 == code ? "" : Integer.toHexString(code);
                BufferedImage image = null;
                if (null == intersect || intersect.contains(unicode)) {
                    image = image(ttf.getGlyph().getGlyph(gid));
                }
//                if (Arrays.asList("f576c", "f824f").contains(unicode)) {
//                    System.out.println("debug not works!");
//                }
                result.put(unicode, image);
                if (null != image && !DPUtil.empty(filename)) {
                    ImageIO.write(image, "jpg", new File(String.format(filename, gid)));
                }
            }
        } catch (Exception e) {
            logger.warn("parse ttf image failed!", e);
        }
        return result;
    }

    public <T> Map<String, T> reflect(Map<String, T> map, JsonNode text) {
        Map<String, T> result = new LinkedHashMap<>();
        for (Map.Entry<String, T> entry : map.entrySet()) {
            result.put(text.get(entry.getKey()).asText(), entry.getValue());
        }
        return result;
    }

    public boolean compare(BufferedImage a, BufferedImage b, float threshold) {
        if (null == a || null == b) return a == b;
        if (a.getWidth() != b.getWidth() || a.getHeight() != b.getHeight()) return false;
        int width = a.getWidth();
        int height = a.getHeight();
        float total = width * height, count = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (Math.abs(a.getRGB(x, y) - b.getRGB(x, y)) == 0) {
                    count++;
                }
            }
        }
        return count / total >= threshold;
    }

    public Map<String, String> compare(Map<String, BufferedImage> refer, Map<String, BufferedImage> follow, float threshold) {
        Map<String, String> result = new LinkedHashMap<>();
        NEXT: for (Map.Entry<String, BufferedImage> followEntry : follow.entrySet()) {
            BufferedImage image = followEntry.getValue();
            for (Map.Entry<String, BufferedImage> referEntry : refer.entrySet()) {
                if (!compare(image, referEntry.getValue(), threshold)) continue;
                result.put(followEntry.getKey(), referEntry.getKey());
                continue NEXT;
            }
            result.put(followEntry.getKey(), null);
        }
        return result;
    }

}
