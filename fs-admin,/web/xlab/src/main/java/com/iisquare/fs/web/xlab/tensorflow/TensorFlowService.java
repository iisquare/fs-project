package com.iisquare.fs.web.xlab.tensorflow;

import com.google.protobuf.InvalidProtocolBufferException;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.xlab.service.OpenCVService;
import com.iisquare.fs.web.xlab.service.WatermarkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Tensor;
import org.tensorflow.framework.ConfigProto;
import org.tensorflow.framework.GPUOptions;
import org.tensorflow.framework.MetaGraphDef;
import org.tensorflow.framework.SignatureDef;

import javax.annotation.PreDestroy;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TensorFlowService extends ServiceBase {

    @Autowired
    public OpenCVService cvService;
    @Autowired
    public WatermarkService wmService;
    public Map<String, Map<String, Object>> models = new HashMap<>();
    protected final static Logger logger = LoggerFactory.getLogger(TensorFlowService.class);

    @PreDestroy
    public boolean release() {
        Map<String, Map<String, Object>> models = this.models;
        this.models = new HashMap<>();
        for (Map.Entry<String, Map<String, Object>> entry : models.entrySet()) {
            Map<String, Object> map = entry.getValue();
            SavedModelBundle bundle = (SavedModelBundle) map.get("bundle");
            bundle.close();
        }
        return true;
    }

    public Map<String, Object> load(String name) {
        return null;
    }

    public ConfigProto config(boolean gpuAllowGrowth, double gpuMemoryFraction) {
        ConfigProto config = ConfigProto.newBuilder().setGpuOptions(
                GPUOptions.newBuilder()
                        .setPerProcessGpuMemoryFraction(gpuMemoryFraction)
                        .setAllowGrowth(gpuAllowGrowth).build()
        ).build();
        return config;
    }

    public SavedModelBundle bundle(String modelPath, ConfigProto config, String... tags) {
        SavedModelBundle bundle = SavedModelBundle.loader(modelPath)
                .withTags(tags).withConfigProto(config.toByteArray()).load();
        return  bundle;
    }

    public SignatureDef signature(SavedModelBundle bundle, String key) {
        try {
            return MetaGraphDef.parseFrom(bundle.metaGraphDef()).getSignatureDefOrThrow(key);
        } catch (InvalidProtocolBufferException e) {
            logger.error("load " + this.getClass().getSimpleName() + " model failed", e);
            return null;
        }
    }

    public Map<String, Object> model(String name) {
        String fullName = getClass().getSimpleName() + "-" + name;
        Map<String, Object> model = models.get(fullName);
        if (null != model) return model;
        synchronized (TensorFlowService.class) {
            model = models.get(fullName);
            if (null != model) return model;
            model = this.load(name);
            models.put(fullName, model);
        }
        return model;
    }

    public int product(long... args) {
        int result = 1;
        for (int i = 0; i < args.length; i++) {
            result *= args[i];
        }
        return result;
    }

    public ByteBuffer byteBuffer(Tensor<?> tensor, int capacity) {
        ByteBuffer buffer = ByteBuffer.allocate(capacity);
        tensor.writeTo(buffer);
        buffer.rewind();
        return buffer;
    }

    public FloatBuffer floatBuffer(Tensor<?> tensor, long[] shape) {
        FloatBuffer buffer = FloatBuffer.allocate(product(shape));
        tensor.writeTo(buffer);
        buffer.flip();
        return buffer;
    }

    public LongBuffer longBuffer(Tensor<?> tensor, long[] shape) {
        LongBuffer buffer = LongBuffer.allocate(product(shape));
        tensor.writeTo(buffer);
        buffer.flip();
        return buffer;
    }

    public TensorFlowService close(Tensor... tensors) {
        if (null == tensors) return this;
        for (Tensor tensor : tensors) {
            tensor.close();
        }
        return this;
    }

    public TensorFlowService close(List<Tensor<?>>... lists) {
        if (null == lists) return this;
        for (List<Tensor<?>> list : lists) {
            if (null == list) continue;
            for (Tensor tensor : list) {
                tensor.close();
            }
        }
        return this;
    }

}
