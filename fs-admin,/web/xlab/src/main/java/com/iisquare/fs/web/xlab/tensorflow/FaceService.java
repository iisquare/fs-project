package com.iisquare.fs.web.xlab.tensorflow;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.framework.ConfigProto;
import org.tensorflow.framework.SignatureDef;

import javax.annotation.PostConstruct;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FaceService extends TensorFlowService {

    @Value("${fs.tensorflow.model.face.preload:false}")
    public boolean isPreload;
    @Value("${fs.tensorflow.model.face.path:}")
    public String modelPath;
    @Value("${fs.tensorflow.model.face.gpu.allowGrowth:true}")
    public boolean gpuAllowGrowth;
    @Value("${fs.tensorflow.model.face.gpu.memoryFraction:0.1}")
    public double gpuMemoryFraction;
    public final Size faceSize = new Size(112, 112);
    public final int eigenvalueSize = 512;

    @PostConstruct
    public boolean init() {
        if (isPreload && null == model("face")) return false;
        return true;
    }

    @Override
    public Map<String, Object> load(String name) {
        ConfigProto config = this.config(gpuAllowGrowth, gpuMemoryFraction);
        SavedModelBundle bundle = this.bundle(modelPath, config, "serve");
        SignatureDef signature = this.signature(bundle, "serving_default");
        if (null == signature) return null;
        Map<String, Object> model = new HashMap<>();
        model.put("bundle", bundle);
        model.put("input",  signature.getInputsMap().get("images").getName());
        model.put("output",  signature.getOutputsMap().get("embedding").getName());
        return model;
    }

    public FloatBuffer face(List<Mat> images) {
        Map<String, Object> model = model("face");
        if (null == model) return null;
        int width = 112, height = 112, channels = 3, batch = images.size();
        FloatBuffer bufferImage = cvService.floatBuffer(images, width, height, channels, 2, 255, 0);
        Tensor<Float> tensorImage = Tensor.create(new long[]{batch, height, width, channels}, bufferImage);
        Session.Runner runner = ((SavedModelBundle) model.get("bundle")).session().runner();
        runner.feed(model.get("input").toString(), tensorImage);
        runner.fetch(model.get("output").toString());
        Tensor<?> tensorDetection = runner.run().get(0);
        long[] shapeDetection = tensorDetection.shape();
        FloatBuffer bufferDetection = floatBuffer(tensorDetection, shapeDetection);
        close(tensorImage, tensorDetection);
        return bufferDetection;
    }

}
