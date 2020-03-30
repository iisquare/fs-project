package com.iisquare.fs.web.xlab.service;

import com.iisquare.fs.base.core.util.OSUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.ArrayUtils;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class OpenCVService extends ServiceBase {

    protected final static Logger logger = LoggerFactory.getLogger(OpenCVService.class);
    private static boolean isInit = false;

    public boolean empty(Mat image) {
        if (null == image) return true;
        if (image.width() < 1 || image.height() < 1) return true;
        return false;
    }

    /**
     * 环境初始化，加载相关类库
     */
    @Value("${fs.opencv.libs}")
    public boolean init(String path) {
        if(isInit) return isInit;
        synchronized (OpenCVService.class) {
            if(isInit) return isInit;
            if(null == path) path = "";
            StringBuilder pathBuilder = new StringBuilder((new File(path)).getAbsolutePath()).append(File.separator);
            pathBuilder.append("libs").append(File.separator);
            String bits = String.valueOf(System.getProperty("sun.arch.data.model"));
            pathBuilder.append("64".equals(bits) ? "x64" : "x86").append(File.separator);
            String pathNative = pathBuilder.toString();
            String suffixNative;
            switch (OSUtil.getCurrentOS()) {
                case Windows:
                    suffixNative = ".dll";
                    break;
                case Linux:
                    suffixNative = ".so";
                    break;
                case Mac:
                    suffixNative = ".dylib";
                    break;
                default:
                    return false;
            }
            String fileNative = pathNative + Core.NATIVE_LIBRARY_NAME + suffixNative;
            try {
                System.load(fileNative);
            } catch (Exception e) {
                logger.error("load libs from '" + path + "' failed", e);
            } finally {
                isInit = true;
            }
        }
        return isInit;
    }

    public void close(Closeable...args) {
        try {
            for (Closeable arg : args) {
                if(null != arg) arg.close();
            }
        } catch (Exception e) {}
    }

    public String imencode(Mat img, boolean asBase64) {
        MatOfByte buf = new MatOfByte();
        if(!Imgcodecs.imencode(".png", img, buf)) return null;
        if(asBase64) {
            return "data:image/png;base64," + new String(Base64.encodeBase64(buf.toArray()));
        } else {
            return new String(buf.toArray());
        }
    }

    public Mat imdecode(String data, int flags) {
        if(data.startsWith("data:image")) {
            try {
                data = data.replaceFirst("data:image/\\w+;base64,", "");
                return Imgcodecs.imdecode(new MatOfByte(Base64.decodeBase64(data)), flags);
            } catch (Exception e) {
                logger.warn("load mat from base64 failed!", e);
            }
        }
        if(data.startsWith("http")) {
            URL url;
            try {
                url = new URL(data);
                URLConnection con = url.openConnection();
                InputStream is = con.getInputStream();
                byte[] bs = new byte[1024];
                int len;
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                while ((len = is.read(bs)) != -1) {
                    bytes.write(bs, 0, len);
                }
                is.close();
                return Imgcodecs.imdecode(new MatOfByte(bytes.toByteArray()), flags);
            } catch (Exception e) {
                logger.warn("load mat from [" + data + "] failed!", e);
            }
        }
        if(data.startsWith("file://")) {
            return Imgcodecs.imread(data.replaceFirst("file:\\/\\/", ""), flags);
        }
        return null;
    }

    public BufferedImage bufferedImage(Mat mat) {
        int width = mat.cols();
        int height = mat.rows();
        int dims = mat.channels();
        int[] pixels = new int[width * height];
        byte[] rgbdata = new byte[width * height * dims];
        mat.get(0, 0, rgbdata);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int index = 0;
        int r = 0, g = 0, b = 0;
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                if (dims == 3) {
                    index = row * width * dims + col * dims;
                    b = rgbdata[index] & 0xff;
                    g = rgbdata[index + 1] & 0xff;
                    r = rgbdata[index + 2] & 0xff;
                    pixels[row * width + col] = ((255 & 0xff) << 24) | ((r & 0xff) << 16) | ((g & 0xff) << 8) | b & 0xff;
                }
                if (dims == 1) {
                    index = row * width + col;
                    b = rgbdata[index] & 0xff;
                    pixels[row * width + col] = ((255 & 0xff) << 24) | ((b & 0xff) << 16) | ((b & 0xff) << 8) | b & 0xff;
                }
            }
        }
        rgb(image, 0, 0, width, height, pixels);
        return image;
    }

    public void rgb(BufferedImage image, int x, int y, int width, int height, int[] pixels) {
        int type = image.getType();
        if (type == BufferedImage.TYPE_INT_ARGB || type == BufferedImage.TYPE_INT_RGB) {
            image.getRaster().setDataElements(x, y, width, height, pixels);
        } else {
            image.setRGB(x, y, width, height, pixels, 0, width);
        }
    }

    public Mat adaptiveMedianBlur(Mat mat, int window) {
        Mat result = mat.clone();
        int d = window / 2;
        int width = mat.width(), height = mat.height(), channels = mat.channels();
        for(int px = d; px < width - d; ++px) {
            for(int py = d; py < height - d; ++py) {
                List<List<Double>> list = new ArrayList<>();
                for(int sx = px - d; sx < px + d; ++sx) {
                    for(int sy = py - d; sy < py + d; ++sy) {
                        list.add(Arrays.asList(ArrayUtils.toObject(mat.get(sy, sx))));
                    }
                }
                int size = list.size();
                for(int i = 0; i < size - 1; ++i){
                    List<Double> li = list.get(i);
                    for(int j = i + 1; j < size; ++j){
                        List<Double> lj = list.get(j);
                        for (int c = 0; c < channels; c++) {
                            Double vi = li.get(c), vj = lj.get(c);
                            if (vi > vj) {
                                li.set(c, vj);
                                lj.set(c, vi);
                            }
                        }
                    }
                }
                double[] doubles = mat.get(py, px);
                List<Double> l1 = list.get(0), l2 = list.get(list.size() / 2), l3 = list.get(list.size() - 1);
                for (int c = 0; c < channels; c++) {
                    if (doubles[c] == l3.get(c) || doubles[c] == l1.get(c)) {
                        doubles[c] = l2.get(c);
                    }
                }
                result.put(py, px, doubles);
            }
        }
        return result;
    }

    public void haarWavelet(Mat src, int ni) {
        Mat dst = src.clone();
        double c, dh, dv, dd;
        assert (src.type() == CvType.CV_32FC1);
        assert (dst.type() == CvType.CV_32FC1);
        int width = src.cols();
        int height = src.rows();
        for (int k = 0; k < ni; k++) {
            for (int y = 0; y < (height >> (k + 1)); y++) {
                for (int x = 0; x < (width >> (k + 1)); x++) {
                    c = (src.get(2 * y, 2 * x)[0] + src.get(2 * y, 2 * x + 1)[0] + src.get(2 * y + 1, 2 * x)[0] + src.get(2 * y + 1, 2 * x + 1)[0]) * 0.5;
                    dst.put(y, x, c);

                    dh = (src.get(2 * y, 2 * x)[0] + src.get(2 * y + 1, 2 * x)[0] - src.get(2 * y, 2 * x + 1)[0] - src.get(2 * y + 1, 2 * x + 1)[0]) * 0.5;
                    dst.put(y, x + (width >> (k + 1)), dh);

                    dv = (src.get(2 * y, 2 * x)[0] + src.get(2 * y, 2 * x + 1)[0] - src.get(2 * y + 1, 2 * x)[0] - src.get(2 * y + 1, 2 * x + 1)[0]) * 0.5;
                    dst.put(y + (height >> (k + 1)), x, dv);

                    dd = (src.get(2 * y, 2 * x)[0] - src.get(2 * y, 2 * x + 1)[0] - src.get(2 * y + 1, 2 * x)[0] + src.get(2 * y + 1, 2 * x + 1)[0]) * 0.5;
                    dst.put(y + (height >> (k + 1)), x + (width >> (k + 1)), dd);
                }
            }
        }
        dst.copyTo(src);
    }

    public double sgn(double x) {
        double res = 0;
        if (x == 0) {
            res = 0;
        }
        if (x > 0) {
            res = 1;
        }
        if (x < 0) {
            res = -1;
        }
        return res;
    }

    public double softShrink(double d, double T) {
        double res;
        if (Math.abs(d) > T) {
            res = sgn(d) * (Math.abs(d) - T);
        } else {
            res = 0;
        }

        return res;
    }

    public double hardShrink(double d, double T) {
        double res;
        if (Math.abs(d) > T) {
            res = d;
        } else {
            res = 0;
        }

        return res;
    }

    public double garrotShrink(double d, double T) {
        double res;
        if (Math.abs(d) > T) {
            res = d - ((T * T) / d);
        } else {
            res = 0;
        }

        return res;
    }

    public void invHaarWavelet(Mat src, int ni) {
        Mat dst = src.clone();
        String SHRINKAGE_TYPE = "NONE";
        double SHRINKAGE_T = 50;
        double c, dh, dv, dd;
        assert (src.type() == CvType.CV_32FC1);
        assert (dst.type() == CvType.CV_32FC1);
        int width = src.cols();
        int height = src.rows();
        for (int k = ni; k > 0; k--) {
            for (int y = 0; y < (height >> k); y++) {
                for (int x = 0; x < (width >> k); x++) {
                    c = src.get(y, x)[0];
                    dh = src.get(y, x + (width >> k))[0];
                    dv = src.get(y + (height >> k), x)[0];
                    dd = src.get(y + (height >> k), x + (width >> k))[0];

                    // (shrinkage)
                    switch (SHRINKAGE_TYPE) {
                        case "HARD":
                            dh = hardShrink(dh, SHRINKAGE_T);
                            dv = hardShrink(dv, SHRINKAGE_T);
                            dd = hardShrink(dd, SHRINKAGE_T);
                            break;
                        case "SOFT":
                            dh = softShrink(dh, SHRINKAGE_T);
                            dv = softShrink(dv, SHRINKAGE_T);
                            dd = softShrink(dd, SHRINKAGE_T);
                            break;
                        case "GARROT":
                            dh = garrotShrink(dh, SHRINKAGE_T);
                            dv = garrotShrink(dv, SHRINKAGE_T);
                            dd = garrotShrink(dd, SHRINKAGE_T);
                            break;
                    }
                    dst.put(y * 2, x * 2, 0.5 * (c + dh + dv + dd));
                    dst.put(y * 2, x * 2 + 1, 0.5 * (c - dh + dv - dd));
                    dst.put(y * 2 + 1, x * 2, 0.5 * (c + dh - dv - dd));
                    dst.put(y * 2 + 1, x * 2 + 1, 0.5 * (c - dh - dv + dd));
                }
            }
            Mat C = src.submat(new Rect(0, 0, width >> (k - 1), height >> (k - 1)));
            Mat D = dst.submat(new Rect(0, 0, width >> (k - 1), height >> (k - 1)));
            D.copyTo(C);
        }
    }

}
