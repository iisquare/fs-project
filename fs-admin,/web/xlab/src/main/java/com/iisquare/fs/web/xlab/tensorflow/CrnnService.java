package com.iisquare.fs.web.xlab.tensorflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
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
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CrnnService extends TensorFlowService {

    @Value("${fs.tensorflow.model.crnn.preload:false}")
    public boolean isPreload;
    @Value("${fs.tensorflow.model.crnn.path:}")
    public String modelPath;
    @Value("${fs.tensorflow.model.crnn.gpu.allowGrowth:true}")
    public boolean gpuAllowGrowth;
    @Value("${fs.tensorflow.model.crnn.gpu.memoryFraction:0.1}")
    public double gpuMemoryFraction;
    @Value("${fs.tensorflow.model.crnn.charDictPath:}")
    public String charDictPath;
    private JsonNode charDict;
    @Value("${fs.tensorflow.model.crnn.ordMapPath:}")
    public String ordMapPath;
    private JsonNode ordMap;

    @PostConstruct
    public boolean init() {
        if (isPreload && null == model("crnn")) return false;
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
        model.put("input",  signature.getInputsMap().get("input_tensor").getName());
        model.put("decodes_indices",  signature.getOutputsMap().get("decodes_indices").getName());
        model.put("decodes_values",  signature.getOutputsMap().get("decodes_values").getName());
        model.put("decodes_dense_shape",  signature.getOutputsMap().get("decodes_dense_shape").getName());
        charDict = DPUtil.parseJSON(FileUtil.getContent(charDictPath));
        ordMap = DPUtil.parseJSON(FileUtil.getContent(ordMapPath));
        return model;
    }

    public List<String> crnn(Mat image) {
        Map<String, Object> model = model("crnn");
        if (null == model) return null;
        int width = image.width();
        int height = image.height();
        if (32 != height) {
            width = 32 * width / height;
            height = 32;
            Imgproc.resize(image, image, new Size(width, height));
        }
        FloatBuffer buffer = cvService.floatBuffer(image, 1f, 127.5f, -1);
        Session session = ((SavedModelBundle) model.get("bundle")).session();
        Tensor<Float> input = Tensor.create(new long[]{1, height, width, 3}, buffer);
        Session.Runner runner = session.runner();
        runner.feed(model.get("input").toString(), input);
        runner.fetch(model.get("decodes_values").toString());
        Tensor<?> output = runner.run().get(0);
        LongBuffer result = longBuffer(output, output.shape());
        close(input, output);
        long total = result.capacity();
        List<String> text = new ArrayList<>();
        for (int index = 0; index < total; index++) {
            String item = charDict.get(ordMap.get(result.get(index) + "_index").asText() + "_ord").asText();
            text.add(item);
        }
        return text;
    }

}
