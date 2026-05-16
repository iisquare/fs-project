package com.iisquare.fs.web.xlab.torch;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.Closeable;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LamaService extends TorchService implements Closeable {

    @Value("${fs.torch.model.lama.path:}")
    public String modelPath;
    private Module module = null;

    public boolean reload() {
        try {
            this.close();
        } catch (IOException e) {
            return false;
        }
        this.module = Module.load(modelPath);
        return true;
    }

    /**
     * 单图预测
     * @param image RGB图像
     * @param mask 二值化灰度图
     */
    public Mat predict(Mat image, Mat mask) {
        IValue input = IValue.dictStringKeyFrom(new LinkedHashMap(){{
            put("image", IValue.from(Tensor.fromBlob( // RBG图片
                    cvService.floatBufferCHW(image, 1, 255, 0).array(),
                    new long[] {1, image.channels(), image.height(), image.width()}
            )));
            put("mask", IValue.from(Tensor.fromBlob( // 灰度图
                    cvService.floatBufferCHW(mask, 1, 255, 0).array(),
                    new long[] {1, mask.channels(), mask.height(), mask.width()}
            )));
        }});
        IValue predict = module.forward(input);
        Map<String, IValue> output = predict.toDictStringKey();
        Tensor result = output.get("inpainted").toTensor();
        List<Mat> mats = cvService.matBCHW(FloatBuffer.wrap(result.getDataAsFloatArray()), result.shape(), 255, 1, 0);
        return mats.get(0);
    }

    @Override
    public void close() throws IOException {
        synchronized (LamaService.class) {
            if (null != module) module.destroy();
        }
    }
}
