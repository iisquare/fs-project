package com.iisquare.fs.web.xlab.tensorflow;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.xlab.core.Box;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.HighGui;
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

/**
 * 参考代码：https://github.com/vcvycy/MTCNN4Android
 */
@Service
public class MtcnnService extends TensorFlowService {

    @Value("${fs.tensorflow.model.mtcnn.preload:false}")
    public boolean isPreload;
    @Value("${fs.tensorflow.model.mtcnn.path:}")
    public String modelPath;
    @Value("${fs.tensorflow.model.mtcnn.gpu.allowGrowth:true}")
    public boolean gpuAllowGrowth;
    @Value("${fs.tensorflow.model.mtcnn.gpu.memoryFraction:0.1}")
    public double gpuMemoryFraction;

    private float factor = 0.79f;
    private float pNetSize = 12.0f;
    private float pNetThreshold = 0.6f;
    private int rNetSize = 24;
    private float rNetThreshold = 0.7f;
    private int oNetSize = 48;
    private float oNetThreshold = 0.7f;

    @PostConstruct
    public boolean init() {
        if (isPreload && null == model("mtcnn")) return false;
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
        model.put("p_net_input_image",  signature.getInputsMap().get("p_net_input_image").getName());
        model.put("r_net_input_image",  signature.getInputsMap().get("r_net_input_image").getName());
        model.put("o_net_input_image",  signature.getInputsMap().get("o_net_input_image").getName());
        model.put("p_net_cls_prob",  signature.getOutputsMap().get("p_net_cls_prob").getName());
        model.put("p_net_bbox_pred",  signature.getOutputsMap().get("p_net_bbox_pred").getName());
        model.put("p_net_landmark_pred",  signature.getOutputsMap().get("p_net_landmark_pred").getName());
        model.put("r_net_cls_prob",  signature.getOutputsMap().get("r_net_cls_prob").getName());
        model.put("r_net_bbox_pred",  signature.getOutputsMap().get("r_net_bbox_pred").getName());
        model.put("r_net_landmark_pred",  signature.getOutputsMap().get("r_net_landmark_pred").getName());
        model.put("o_net_cls_prob",  signature.getOutputsMap().get("o_net_cls_prob").getName());
        model.put("o_net_bbox_pred",  signature.getOutputsMap().get("o_net_bbox_pred").getName());
        model.put("o_net_landmark_pred",  signature.getOutputsMap().get("o_net_landmark_pred").getName());
        return model;
    }

    public void predictPNet(Map<String, Object> model, Mat image, float[][] cls, float[][][] bbox) {
        int width = image.width(), height = image.height(), channels = image.channels();
        Mat molded = image.clone();
        FloatBuffer bufferImage = cvService.floatBuffer(molded, 2, 255, -1);
        Tensor<Float> tensorImage = Tensor.create(new long[]{height, width, channels}, bufferImage);
        Session.Runner runner = ((SavedModelBundle) model.get("bundle")).session().runner();
        runner.feed(model.get("p_net_input_image").toString(), tensorImage);
        runner.fetch(model.get("p_net_cls_prob").toString());
        runner.fetch(model.get("p_net_bbox_pred").toString());
        List<Tensor<?>> result = runner.run();
        Tensor<?> tensorCls = result.get(0);
        Tensor<?> tensorBBox = result.get(1);
        long[] shapeCls = tensorCls.shape();
        FloatBuffer bufferCls = floatBuffer(tensorCls, shapeCls);
        long[] shapeBBox = tensorBBox.shape();
        FloatBuffer bufferBBox = floatBuffer(tensorBBox, shapeBBox);
        close(tensorCls, tensorBBox, tensorImage);
//        System.out.println(Arrays.toString(shapeCls));
//        System.out.println(Arrays.toString(shapeBBox));
        for (int h = 0; h < shapeCls[0]; h++) {
            for (int w = 0; w < shapeCls[1]; w++) {
                bufferCls.get(); // ignore [h][w][0]
                cls[h][w] = bufferCls.get();
            }
        }
        for (int h = 0; h < shapeBBox[0]; h++) {
            for (int w = 0; w < shapeBBox[1]; w++) {
                for (int i = 0; i < shapeBBox[2]; i++) {
                    bbox[h][w][i] = bufferBBox.get();
                }
            }
        }
    }

    public void predictRNet(Map<String, Object> model, Mat image, List<Box> boxes) {
        int batch = boxes.size();
        Size size = new Size(rNetSize, rNetSize);
        FloatBuffer buffer = FloatBuffer.allocate(batch * rNetSize * rNetSize * 3);
        for (Box box : boxes) {
            Mat mat = image.submat(box.rect()).clone();
            Imgproc.resize(mat, mat, size);
            buffer.put(cvService.floatBuffer(mat, 2, 255, -1));
        }
        buffer.flip();
        Tensor<Float> tensorImage = Tensor.create(new long[]{batch, rNetSize, rNetSize, 3}, buffer);
        Session.Runner runner = ((SavedModelBundle) model.get("bundle")).session().runner();
        runner.feed(model.get("r_net_input_image").toString(), tensorImage);
        runner.fetch(model.get("r_net_cls_prob").toString());
        runner.fetch(model.get("r_net_bbox_pred").toString());
        List<Tensor<?>> result = runner.run();
        Tensor<?> tensorCls = result.get(0);
        Tensor<?> tensorBBox = result.get(1);
        long[] shapeCls = tensorCls.shape();
        FloatBuffer bufferCls = floatBuffer(tensorCls, shapeCls);
        long[] shapeBBox = tensorBBox.shape();
        FloatBuffer bufferBBox = floatBuffer(tensorBBox, shapeBBox);
        close(tensorImage, tensorCls, tensorBBox);
        for (Box box : boxes) {
            bufferCls.get(); // ignore [i][0]
            box.score = bufferCls.get();
            if (box.score < rNetThreshold) box.deleted = true;
            for (int i = 0; i < shapeBBox[1]; i++) {
                box.bbr[i] = bufferBBox.get();
            }
        }
    }

    public void predictONet (Map<String, Object> model, Mat image, List<Box> boxes) {
        int batch = boxes.size();
        Size size = new Size(oNetSize, oNetSize);
        FloatBuffer buffer = FloatBuffer.allocate(batch * oNetSize * oNetSize * 3);
        for (Box box : boxes) {
            Mat mat = image.submat(box.rect()).clone();
            Imgproc.resize(mat, mat, size);
            buffer.put(cvService.floatBuffer(mat, 2, 255, -1));
        }
        buffer.flip();
        Tensor<Float> tensorImage = Tensor.create(new long[]{batch, oNetSize, oNetSize, 3}, buffer);
        Session.Runner runner = ((SavedModelBundle) model.get("bundle")).session().runner();
        runner.feed(model.get("o_net_input_image").toString(), tensorImage);
        runner.fetch(model.get("o_net_cls_prob").toString());
        runner.fetch(model.get("o_net_bbox_pred").toString());
        runner.fetch(model.get("o_net_landmark_pred").toString());
        List<Tensor<?>> result = runner.run();
        Tensor<?> tensorCls = result.get(0);
        Tensor<?> tensorBBox = result.get(1);
        Tensor<?> tensorLandmark = result.get(2);
        long[] shapeCls = tensorCls.shape();
        FloatBuffer bufferCls = floatBuffer(tensorCls, shapeCls);
        long[] shapeBBox = tensorBBox.shape();
        FloatBuffer bufferBBox = floatBuffer(tensorBBox, shapeBBox);
        long[] shapeLandmark = tensorLandmark.shape();
        FloatBuffer bufferLandmark = floatBuffer(tensorLandmark, shapeLandmark);
        close(tensorImage, tensorCls, tensorBBox, tensorLandmark);
        for (Box box : boxes) {
            bufferCls.get(); // ignore [i][0]
            box.score = bufferCls.get();
            if (box.score < oNetThreshold) box.deleted = true;
            for (int i = 0; i < shapeBBox[1]; i++) {
                box.bbr[i] = bufferBBox.get();
            }
            long n = shapeLandmark[1] / 2;
            for (int j = 0; j < n; j++) {
                int x = (int) (box.left() + bufferLandmark.get() * box.width());
                int y = (int) (box.top() + bufferLandmark.get() * box.height());
                box.landmark[j] = new Point(x, y);
            }
        }
    }

    public List<Box> detectPNet(Map<String, Object> model, Mat image, int minSize) {
        int width = image.width(), height = image.height();
        int minWH = Math.min(width, height);
        float currentFaceSize = minSize;
        List<Box> totalBoxes = new ArrayList<>();
        while (currentFaceSize <= minWH) {
            float scale = pNetSize / currentFaceSize;
            Mat mat = image.clone();
            Imgproc.resize(mat, mat, new Size(), scale, scale);
            int w = mat.width(), h = mat.height();
            int ow = (int) (Math.ceil(w * 0.5 - 5) + 0.5), oh = (int) (Math.ceil(h * 0.5 - 5) + 0.5);
            float[][]   cls = new float[oh][ow];;
            float[][][] bbox = new float[oh][ow][4];
//            System.out.println(String.format("hw:%dx%d, ohw:%dx%d", h, w, oh, ow));
            predictPNet(model, mat, cls, bbox);
            currentFaceSize /= factor;
            List<Box> curBoxes = boxes(cls, bbox, scale, pNetThreshold);
            nms(curBoxes,0.5f,"Union");
            for (Box box : curBoxes) {
                if (!box.deleted) totalBoxes.add(box);
            }
        }
        nms(totalBoxes,0.7f,"Union");
        return optimize(totalBoxes, width, height, true, true);
    }

    public List<Box> detectRNet(Map<String, Object> model, Mat image, List<Box> boxes) {
        predictRNet(model, image, boxes);
        nms(boxes,0.6f,"Union");
        return optimize(boxes, image.width(), image.height(), false, true);
    }

    public List<Box> detectONet(Map<String, Object> model, Mat image, List<Box> boxes) {
        predictONet(model, image, boxes);
        int width = image.width(), height = image.height();
        boxes = optimize(boxes, width, height, true, false);
        nms(boxes,0.6f,"Min");
        return optimize(boxes, width, height, false, false);
    }

    public ArrayNode format(List<Box> boxes, int width, int height) {
        String[] landmark = new String[]{"eyeLeft", "eyeRight", "noseCenter", "mouthLeft", "mouthRight"};
        ArrayNode nodes = DPUtil.arrayNode();
        for (Box box : boxes) {
            ObjectNode node = nodes.addObject();
            ObjectNode item = node.putObject("box");
            item.put("left", box.left());
            item.put("top", box.top());
            item.put("right", box.right());
            item.put("bottom", box.bottom());
            box.square();
            box.limitSquare(width, height);
            item = node.putObject("square");
            item.put("left", box.left());
            item.put("top", box.top());
            item.put("right", box.right());
            item.put("bottom", box.bottom());
            item = node.putObject("landmark");
            for (int i = 0; i < landmark.length; i++) {
                item.putObject(landmark[i]).put("x", box.landmark[i].x).put("y", box.landmark[i].y);
            }
        }
        return nodes;
    }

    public List<Box> detect(Mat image, int minFaceSize, boolean debug) {
        Map<String, Object> model = model("mtcnn");
        if (null == model) return null;
        List<Box> boxes = detectPNet(model, image, minFaceSize);
        if (boxes.size() < 1) return boxes;
        if (debug) display("PNet", image.clone(), boxes);
        boxes = detectRNet(model, image, boxes);
        if (boxes.size() < 1) return boxes;
        if (debug) display("RNet", image.clone(), boxes);
        boxes = detectONet(model, image, boxes);
        if (debug) display("ONet", image.clone(), boxes);
        if (debug) HighGui.waitKey();
        return boxes;
    }

    private void display(String title, Mat image, List<Box> boxes) {
        for (Box box : boxes) {
            Scalar scalar = new Scalar(Math.random() * 255, Math.random() * 255, Math.random() * 255);
            Imgproc.rectangle(image, box.rect(), scalar);
            for (int i = 0; i < box.landmark.length; i++) {
                Point point = box.landmark[i];
                if (null == point) break;
//                Imgproc.putText(image, String.valueOf(i), point, Imgproc.FONT_ITALIC, 1, scalar);
                Imgproc.drawMarker(image, point, scalar, Imgproc.MARKER_CROSS);
            }
        }
        HighGui.imshow(title, image);
    }

    private List<Box> optimize(List<Box> boxes, int w, int h, boolean doCalibrate, boolean doSquare){
        List<Box> list = new ArrayList<>();
        for (Box box : boxes) {
            if (box.deleted) continue; // remove deleted
            if (doCalibrate) { // bounding box regression
                box.calibrate();
            }
            if (doSquare) { // to limit square shape
                box.square();
                box.limitSquare(w, h);
            }
            list.add(box);
        }
        return list;
    }

    private List<Box> boxes(float[][] prob, float[][][] bias, float scale, float threshold) {
        int h = prob.length;
        int w = prob[0].length;
        List<Box> boxes = new ArrayList<>();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float score = prob[y][x];
                if (score > threshold) {
                    Box box = new Box();
                    box.score = score;
                    box.box[0] = Math.round(x * 2 / scale);
                    box.box[1] = Math.round(y * 2 / scale);
                    box.box[2] = Math.round((x * 2 + 11) / scale);
                    box.box[3] = Math.round((y * 2 + 11) / scale);
                    for (int i = 0; i < 4; i++) {
                        box.bbr[i] = bias[y][x][i];
                    }
                    boxes.add(box);
                }
            }
        }
        return boxes;
    }

    private void nms(List<Box> boxes, float threshold, String method) {
        int size = boxes.size();
        for (int i = 0; i < size; i++) {
            Box box = boxes.get(i);
            if (box.deleted) continue;
            for (int j = i + 1; j < size; j++) {
                Box box2 = boxes.get(j);
                if (box2.deleted) continue;
                int x1 = Math.max(box.box[0], box2.box[0]);
                int y1 = Math.max(box.box[1], box2.box[1]);
                int x2 = Math.min(box.box[2], box2.box[2]);
                int y2 = Math.min(box.box[3], box2.box[3]);
                if (x2 < x1 || y2 < y1) continue;
                int areaIoU = (x2 - x1 + 1) * (y2 - y1 + 1);
                float iou = 0f;
                if (method.equals("Union")) {
                    iou = 1.0f * areaIoU / (box.area() + box2.area() - areaIoU);
                } else if (method.equals("Min")) {
                    iou = 1.0f * areaIoU / (Math.min(box.area(), box2.area()));
                }
                if (iou >= threshold) { // 删除prob小的那个框
                    if (box.score > box2.score) {
                        box2.deleted = true;
                    } else {
                        box.deleted = true;
                    }
                }
            }
        }
    }

}
