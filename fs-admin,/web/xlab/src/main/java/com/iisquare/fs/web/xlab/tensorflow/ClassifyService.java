package com.iisquare.fs.web.xlab.tensorflow;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
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
public class ClassifyService extends TensorFlowService {

    @Value("${fs.tensorflow.model.classify.preload:false}")
    public boolean isPreload;
    @Value("${fs.tensorflow.model.classify.path:}")
    public String modelPath;
    @Value("${fs.tensorflow.model.classify.gpu.allowGrowth:true}")
    public boolean gpuAllowGrowth;
    @Value("${fs.tensorflow.model.classify.gpu.memoryFraction:0.1}")
    public double gpuMemoryFraction;
    @Value("#{'${fs.tensorflow.model.classify.classes:BG,FG}'.split(',')}")
    public List<String> classes;

    @PostConstruct
    public boolean init() {
        if (isPreload && null == model("classify")) return false;
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
        model.put("input",  signature.getInputsMap().get("input").getName());
        model.put("output",  signature.getOutputsMap().get("output").getName());
        return model;
    }

    public ArrayNode classify(List<Mat> images) {
        Map<String, Object> model = model("classify");
        if (null == model) return null;
        int width = 100, height = 100, channels = 3, batch = images.size();
        FloatBuffer bufferImage = cvService.floatBuffer(images, width, height, channels, 1, 255, 0);
        Tensor<Float> tensorImage = Tensor.create(new long[]{batch, height, width, channels}, bufferImage);
        Session.Runner runner = ((SavedModelBundle) model.get("bundle")).session().runner();
        runner.feed(model.get("input").toString(), tensorImage);
        runner.fetch(model.get("output").toString());
        Tensor<?> tensorDetection = runner.run().get(0);
        long[] shapeDetection = tensorDetection.shape();
        FloatBuffer bufferDetection = floatBuffer(tensorDetection, shapeDetection);
        close(tensorImage, tensorDetection);
        ArrayNode nodes = DPUtil.arrayNode();
        for (int b = 0; b < shapeDetection[0]; b++) {
            ObjectNode node = nodes.addObject();
            for (int i = 0; i < shapeDetection[1]; i++) {
                node.put(classes.get(i), bufferDetection.get());
            }
        }
        return nodes;
    }

}
