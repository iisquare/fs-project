package com.iisquare.fs.web.xlab;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.xlab.core.Box;
import com.iisquare.fs.web.xlab.service.OpenCVService;
import com.iisquare.fs.web.xlab.tensorflow.*;
import com.iisquare.fs.web.xlab.service.WatermarkService;
import org.junit.Test;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import org.tensorflow.TensorFlow;

import java.nio.FloatBuffer;
import java.util.*;

public class TensorFlowTests {

    public OpenCVService cvService = new OpenCVService();
    public TensorFlowService tfService = new TensorFlowService();
    public WatermarkService wmService = new WatermarkService();
    public ClassifyService classifyService = new ClassifyService();
    public CrnnService crnnService = new CrnnService();
    public MrcnnService mrcnnService = new MrcnnService();
    public MtcnnService mtcnnService = new MtcnnService();
    public FaceService faceService = new FaceService();

    public TensorFlowTests() {
        cvService.init(null);
        tfService.cvService = cvService;
        tfService.wmService = wmService;

        crnnService.cvService = cvService;
        crnnService.wmService = wmService;
        crnnService.isPreload = false;
        crnnService.modelPath = "D:\\xampp\\htdocs\\fs-project-vip\\python\\crnn\\model\\cn_saved";
        crnnService.gpuAllowGrowth = true;
        crnnService.gpuMemoryFraction = 0.8;
        crnnService.charDictPath = "D:\\xampp\\htdocs\\fs-project-vip\\python\\crnn\\data\\char_dict\\char_dict_cn.json";
        crnnService.ordMapPath = "D:\\xampp\\htdocs\\fs-project-vip\\python\\crnn\\data\\char_dict\\ord_map_cn.json";

        mrcnnService.cvService = cvService;
        mrcnnService.wmService = wmService;
        mrcnnService.isPreload = false;
        mrcnnService.modelPath = "D:\\xampp\\htdocs\\fs-project-vip\\python\\mask-rcnn\\model\\mrcnn-watermark";
        mrcnnService.gpuAllowGrowth = true;
        mrcnnService.gpuMemoryFraction = 0.8;
        mrcnnService.classes = Arrays.asList("BG", "58_br", "anjuke_br", "5i5j_wm",
                "58_wm", "anjuke_wm", "centanet_ct", "fang_br", "ke_wm", "lianjia_wm");
//        mrcnnService.classes = Arrays.asList("BG", "road");

        classifyService.cvService = cvService;
        classifyService.wmService = wmService;
        classifyService.isPreload = false;
        classifyService.modelPath = "D:\\xampp\\htdocs\\fs-project-vip\\python\\multi-label-image\\weights\\ml-image";
        classifyService.gpuAllowGrowth = true;
        classifyService.gpuMemoryFraction = 0.8;
        classifyService.classes = Arrays.asList("5i5j", "anjuke", "58", "centanet", "fang", "ke", "lianjia");

        mtcnnService.cvService = cvService;
        mtcnnService.wmService = wmService;
        mtcnnService.isPreload = false;
        mtcnnService.modelPath = "D:\\xampp\\htdocs\\fs-project-vip\\python\\mtcnn\\data\\model\\mtcnn";
        mtcnnService.gpuAllowGrowth = true;
        mtcnnService.gpuMemoryFraction = 0.8;

        faceService.cvService = cvService;
        faceService.wmService = wmService;
        faceService.isPreload = false;
        faceService.modelPath = "D:\\xampp\\htdocs\\fs-project-vip\\python\\insightface\\model\\mc";
    }

    @Test
    public void faceTest() {
        String path = "E:\\workspace\\dataset\\faces\\";
        Mat image = Imgcodecs.imread(path + "00000.jpg");
        FloatBuffer face = faceService.face(Arrays.asList(image));
        System.out.println(face.capacity());
        System.out.println(Arrays.toString(face.array()));
    }

    @Test
    public void resultTest() {
        String json = "[{\"box\":{\"left\":404,\"top\":169,\"right\":558,\"bottom\":397},\"square\":{\"left\":367,\"top\":169,\"right\":595,\"bottom\":397},\"landmark\":{\"eyeLeft\":{\"x\":470.0,\"y\":248.0},\"eyeRight\":{\"x\":545.0,\"y\":254.0},\"noseCenter\":{\"x\":532.0,\"y\":272.0},\"mouthLeft\":{\"x\":480.0,\"y\":329.0},\"mouthRight\":{\"x\":543.0,\"y\":327.0}}},{\"box\":{\"left\":889,\"top\":331,\"right\":1015,\"bottom\":513},\"square\":{\"left\":861,\"top\":331,\"right\":1043,\"bottom\":513},\"landmark\":{\"eyeLeft\":{\"x\":949.0,\"y\":396.0},\"eyeRight\":{\"x\":1015.0,\"y\":401.0},\"noseCenter\":{\"x\":969.0,\"y\":425.0},\"mouthLeft\":{\"x\":952.0,\"y\":456.0},\"mouthRight\":{\"x\":990.0,\"y\":464.0}}}]\n";
        JsonNode node = DPUtil.parseJSON(json);
        String path = "E:\\workspace\\dataset\\face\\";
        Mat image = Imgcodecs.imread(path + "all\\3.jpeg");
        Iterator<JsonNode> iterator = node.iterator();
        while (iterator.hasNext()) {
            JsonNode face = iterator.next();
            int left = face.get("square").get("left").asInt();
            int top = face.get("square").get("top").asInt();
            int right = face.get("square").get("right").asInt();
            int bottom = face.get("square").get("bottom").asInt();
            Rect rect = new Rect(left, top, right - left, bottom - top);
            Imgproc.rectangle(image, rect, Scalar.all(100));
        }
        HighGui.imshow("image", image);
        HighGui.waitKey();
    }

    @Test
    public void mtcnnTest() {
        String path = "E:\\workspace\\dataset\\face\\";
        Mat image = Imgcodecs.imread(path + "all\\3.jpeg");
        List<Box> boxes = mtcnnService.detect(image, 12, true);
        ArrayNode nodes = mtcnnService.format(boxes, image.width(), image.height());
        System.out.println(DPUtil.stringify(nodes));
    }

    @Test
    public void classifyTest() {
        String path = "C:\\Users\\Ouyang\\Desktop\\dataset\\test\\";
        List<Mat> images = Arrays.asList(
                Imgcodecs.imread(path + "5i5j-sn-1.jpg"),
                Imgcodecs.imread(path + "centanet-sn-2.jpg")
        );
        ArrayNode result = classifyService.classify(images);
        System.out.println(result);
        Iterator<JsonNode> iterator = result.iterator();
        while (iterator.hasNext()) {
            JsonNode node = iterator.next();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            List<String> data = new ArrayList<>();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> item = fields.next();
                if (item.getValue().floatValue() > 0.5) data.add(item.getKey());
            }
            System.out.println(data);
        }
    }

    @Test
    public void mrcnnTest() {
        Mat image = Imgcodecs.imread("C:\\Users\\Ouyang\\Desktop\\dataset\\test\\lianjia-sn-1.jpg");
        ArrayNode result = mrcnnService.mrcnn(image, image, new Scalar(0, 0, 255), new Scalar(0, 255, 0, 100));
        System.out.println(result);
        HighGui.imshow("image", image);
        HighGui.waitKey();
    }

    @Test
    public void crnnTest() {
        Mat image = Imgcodecs.imread("C:\\Users\\Ouyang\\Desktop\\text\\test\\authcode.jpg");
        List<String> text = crnnService.crnn(image);
        System.out.println(text);
    }

    @Test
    public void demoTest() throws Exception {
        try (Graph g = new Graph()) {
            final String value = "Hello from " + TensorFlow.version();

            // Construct the computation graph with a single operation, a constant
            // named "MyConst" with a value "value".
            try (Tensor t = Tensor.create(value.getBytes("UTF-8"))) {
                // The Java API doesn't yet include convenience functions for adding operations.
                g.opBuilder("Const", "MyConst").setAttr("dtype", t.dataType()).setAttr("value", t).build();
            }

            // Execute the "MyConst" operation in a Session.
            try (Session s = new Session(g);
                 Tensor output = s.runner().fetch("MyConst").run().get(0)) {
                System.out.println(new String(output.bytesValue(), "UTF-8"));
            }
        }
    }

}
