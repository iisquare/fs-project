package com.iisquare.fs.web.xlab.tensorflow;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import org.opencv.core.*;
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
import java.util.*;

@Service
public class MrcnnService extends TensorFlowService {

    @Value("${fs.tensorflow.model.mrcnn.preload:false}")
    public boolean isPreload;
    @Value("${fs.tensorflow.model.mrcnn.path:}")
    public String modelPath;
    @Value("${fs.tensorflow.model.mrcnn.gpu.allowGrowth:true}")
    public boolean gpuAllowGrowth;
    @Value("${fs.tensorflow.model.mrcnn.gpu.memoryFraction:0.1}")
    public double gpuMemoryFraction;
    @Value("#{'${fs.tensorflow.model.mrcnn.classes:BG,FG}'.split(',')}")
    public List<String> classes;

    @PostConstruct
    public boolean init() {
        if (isPreload && null == model("mrcnn")) return false;
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
        // <tf.Tensor 'input_image:0' shape=(?, ?, ?, 3) dtype=float32>,
        model.put("input_image",  signature.getInputsMap().get("input_image").getName());
        // <tf.Tensor 'input_image_meta:0' shape=(?, 15) dtype=float32>,
        model.put("input_image_meta",  signature.getInputsMap().get("input_image_meta").getName());
        // <tf.Tensor 'input_anchors:0' shape=(?, ?, 4) dtype=float32>
        model.put("input_anchors",  signature.getInputsMap().get("input_anchors").getName());
        // <tf.Tensor 'mrcnn_detection/Reshape_1:0' shape=(1, 100, 6) dtype=float32>
        model.put("mrcnn_detection",  signature.getOutputsMap().get("mrcnn_detection").getName());
        // <tf.Tensor 'mrcnn_class/Reshape_1:0' shape=(?, 1000, 3) dtype=float32>
        model.put("mrcnn_class",  signature.getOutputsMap().get("mrcnn_class").getName());
        // <tf.Tensor 'mrcnn_bbox/Reshape:0' shape=(?, 1000, 3, 4) dtype=float32>
        model.put("mrcnn_bbox",  signature.getOutputsMap().get("mrcnn_bbox").getName());
        // <tf.Tensor 'mrcnn_mask/Reshape_1:0' shape=(?, 100, 28, 28, 3) dtype=float32>
        model.put("mrcnn_mask",  signature.getOutputsMap().get("mrcnn_mask").getName());
        // <tf.Tensor 'ROI/packed_2:0' shape=(1, ?, ?) dtype=float32>
        model.put("ROI",  signature.getOutputsMap().get("ROI").getName());
        // <tf.Tensor 'rpn_class/concat:0' shape=(?, ?, 2) dtype=float32>
        model.put("rpn_class",  signature.getOutputsMap().get("rpn_class").getName());
        // <tf.Tensor 'rpn_bbox/concat:0' shape=(?, ?, 4) dtype=float32>
        model.put("rpn_bbox",  signature.getOutputsMap().get("rpn_bbox").getName());
        return model;
    }

    public ArrayNode mrcnn(Mat image, Mat result, Scalar boxScalar, Scalar maskScalar) {
        Map<String, Object> model = model("mrcnn");
        if (null == model) return null;
        int width = image.width();
        int height = image.height();
        int batch = 1, mod = 64, numClasses = classes.size(), channels = image.channels();
        int widthMolded = width % mod == 0 ? width : (width / mod + 1) * mod;
        int heightMolded = height % mod == 0 ? height : (height / mod + 1) * mod;
        Mat molded = Mat.zeros(heightMolded, widthMolded, image.type());
        Rect window = new Rect((widthMolded - width) / 2, (heightMolded - height) / 2, width, height);
        image.copyTo(molded.submat(window));
        Imgproc.cvtColor(molded, molded, Imgproc.COLOR_BGR2RGB);
        molded.convertTo(molded, CvType.CV_32FC3);
        Core.subtract(molded, new Scalar(123.7, 116.8, 103.9), molded);
        FloatBuffer bufferImage = cvService.floatBuffer(molded, 1, 1, 0);
        Tensor<Float> tensorImage = Tensor.create(new long[]{batch, heightMolded, widthMolded, channels}, bufferImage);
        FloatBuffer bufferMeta = FloatBuffer.allocate(12 + numClasses);
        bufferMeta.put(0).put(height).put(width).put(channels).put(heightMolded).put(widthMolded).put(channels);
        bufferMeta.put(window.y).put(window.x).put(window.y + height).put(window.x + width).put(1);
        for (int i = 0; i < numClasses; i++) bufferMeta.put(0);
        bufferMeta.flip();
        Tensor<Float> tensorMeta = Tensor.create(new long[]{batch, bufferMeta.capacity()}, bufferMeta);
        FloatBuffer bufferAnchors = anchors(
                Arrays.asList(32, 64, 128, 256, 512),
                Arrays.asList(0.5f, 1f, 2f),
                backboneShapes(Arrays.asList(heightMolded, widthMolded), Arrays.asList(4, 8, 16, 32, 64)),
                Arrays.asList(4, 8, 16, 32, 64),
                1, heightMolded, widthMolded
        );
        Tensor<Float> tensorAnchors = Tensor.create(new long[]{batch, bufferAnchors.capacity() / 4, 4}, bufferAnchors);
        Session.Runner runner = ((SavedModelBundle) model.get("bundle")).session().runner();
        runner.feed(model.get("input_image").toString(), tensorImage);
        runner.feed(model.get("input_image_meta").toString(), tensorMeta);
        runner.feed(model.get("input_anchors").toString(), tensorAnchors);
        runner.fetch(model.get("mrcnn_detection").toString());
        runner.fetch(model.get("mrcnn_mask").toString());
        List<Tensor<?>> list = runner.run();
        Tensor<?> tensorDetection = list.get(0);
        Tensor<?> tensorMask = list.get(1);
        long[] shapeDetection = tensorDetection.shape();
        FloatBuffer bufferDetection = floatBuffer(tensorDetection, shapeDetection);
        long[] shapeMask = tensorMask.shape();
        FloatBuffer bufferMask = floatBuffer(tensorMask, shapeMask);
        close(tensorImage, tensorMeta, tensorAnchors, tensorDetection, tensorMask);
        ArrayNode nodes = DPUtil.arrayNode();
        float wy1 = Float.valueOf(window.y - 0) / (heightMolded - 1);
        float wx1 = Float.valueOf(window.x - 0) / (widthMolded - 1);
        float wy2 = Float.valueOf(window.y + window.height - 1) / (heightMolded - 1);
        float wx2 = Float.valueOf(window.x + window.width - 1) / (widthMolded - 1);
        for (int i = 0; i < shapeDetection[1]; i++) {
            int y1 = Math.round((bufferDetection.get() - wy1) / (wy2 -wy1) * (height - 1) + 0);
            int x1 = Math.round((bufferDetection.get() - wx1) / (wx2 - wx1) * (width - 1) + 0);
            int y2 = Math.round((bufferDetection.get() - wy1) / (wy2 - wy1) * (height - 1) + 1);
            int x2 = Math.round((bufferDetection.get() - wx1) / (wx2 - wx1) * (width - 1) + 1);
            if ((y2 - y1) * (x2 - x1) <= 1) continue;
            int classId = (int) bufferDetection.get();
            float score = bufferDetection.get();
            ObjectNode node = nodes.addObject();
            node.put("x1", x1).put("y1", y1);
            node.put("x2", x2).put("y2", y2);
            node.put("class", classes.get(classId)).put("score", score);
            node.put("mw", shapeMask[3]).put("mh", shapeMask[2]);
            ArrayNode mask = node.putArray("mask");
            for (int y = 0; y < shapeMask[2]; y++) {
                for (int x = 0; x < shapeMask[3]; x++) {
                    long offset = i * shapeMask[2] * shapeMask[3] * shapeMask[4]; // 所在bbox
                    offset += y * shapeMask[3] * shapeMask[4]; // 所在height行
                    offset += x * shapeMask[4]; // 所在width列
                    offset += classId; // 所对应的类别
                    mask.add(bufferMask.get((int) offset));
                }
            }
            if (null != result) {
                if (null != boxScalar) {
                    Imgproc.rectangle(result, new Point(x1, y1), new Point(x2, y2), boxScalar);
                }
                if (null != maskScalar) {
                    Mat mat =  mask(mask, new Size(shapeMask[3], shapeMask[2]), new Size(x2 - x1, y2 - y1), 0.5f);
                    Mat overlay = Mat.zeros(new Size(x2 - x1, y2 - y1), CvType.CV_8UC4).setTo(maskScalar, mat);
                    wmService.combine(result, overlay, new Point(x1, y1));
                }
            }
        }
        return nodes;
    }

    public List<List<Integer>> backboneShapes(List<Integer> shapes, List<Integer> strides) {
        List<List<Integer>> result = new ArrayList<>();
        for (Integer stride : strides) {
            List<Integer> list = new ArrayList<>();
            for (Integer shape : shapes) {
                list.add(shape / stride);
            }
            result.add(list);
        }
        return result;
    }

    public FloatBuffer anchors(int scale, List<Float> ratios, List<Integer> featureShape, int featureStride, int anchorStride, int heightMolded, int widthMolded) {
        List<Double> heights = new ArrayList<>(), widths = new ArrayList<>();
        for (Float ratio : ratios) {
            heights.add(scale / Math.sqrt(ratio));
            widths.add(scale * Math.sqrt(ratio));
        }
        int height = featureShape.get(0), width = featureShape.get(1);
        List<List<Point>> meshgrid = new ArrayList<>();
        for (int y = 0; y < height; y += anchorStride) {
            int shiftY = y * featureStride;
            List<Point> list = new ArrayList<>();
            for (int x = 0; x < width; x += anchorStride) {
                int shiftX = x * featureStride;
                list.add(new Point(shiftX, shiftY));
            }
            meshgrid.add(list);
        }
        int size = heights.size();
        List<Size> boxSizes = new ArrayList<>();
        List<Point> boxCenters = new ArrayList<>();
        for (List<Point> points : meshgrid) {
            for (Point point : points) {
                for (int i = 0; i < size; i++) {
                    boxCenters.add(point);
                    boxSizes.add(new Size(widths.get(i), heights.get(i)));
                }
            }
        }
        size = boxCenters.size();
        FloatBuffer boxes = FloatBuffer.allocate(size * 4);
        for (int i = 0; i < size; i++) {
            Size boxSize = boxSizes.get(i);
            Point boxCenter = boxCenters.get(i);
            boxes.put((float) (boxCenter.y - 0.5 * boxSize.height - 0) / (heightMolded - 1));
            boxes.put((float) (boxCenter.x - 0.5 * boxSize.width - 0) / (widthMolded - 1));
            boxes.put((float) (boxCenter.y + 0.5 * boxSize.height - 1) / (heightMolded - 1));
            boxes.put((float) (boxCenter.x + 0.5 * boxSize.width - 1) / (widthMolded - 1));
        }
        boxes.flip();
        return boxes;
    }

    public FloatBuffer anchors(List<Integer> scales, List<Float> ratios, List<List<Integer>> featureShapes, List<Integer> featureStrides, int anchorStride, int heightMolded, int widthMolded) {
        int size = scales.size();
        FloatBuffer anchors = FloatBuffer.allocate(0);
        for (int i = 0; i < size; i++) {
            FloatBuffer anchor = anchors(scales.get(i), ratios, featureShapes.get(i), featureStrides.get(i), anchorStride, heightMolded, widthMolded);
            anchors = FloatBuffer.allocate(anchors.capacity() + anchor.capacity()).put(anchors).put(anchor);
            anchors.flip();
        }
        return anchors;
    }

    public Mat mask(ArrayNode data, Size maskSize, Size boxSize, float threshold) {
        Mat mask = Mat.zeros(maskSize, CvType.CV_8UC1);
        for (int y = 0; y < maskSize.height; y++) {
            for (int x = 0; x < maskSize.width; x++) {
                double v = data.get((int) (y * maskSize.width + x)).asDouble();
                mask.put(y, x, v > 0 ? v * 255 : 0);
            }
        }
        Imgproc.resize(mask, mask, boxSize);
        Imgproc.threshold(mask, mask, threshold * 255, 255, Imgproc.THRESH_BINARY);
        return mask;
    }

}
