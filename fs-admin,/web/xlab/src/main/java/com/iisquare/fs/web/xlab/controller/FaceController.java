package com.iisquare.fs.web.xlab.controller;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.web.mvc.ControllerBase;
import com.iisquare.fs.web.xlab.core.Box;
import com.iisquare.fs.web.xlab.service.OpenCVService;
import com.iisquare.fs.web.xlab.tensorflow.FaceService;
import com.iisquare.fs.web.xlab.tensorflow.MtcnnService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RefreshScope
@Api(description = "人脸识别")
@RequestMapping("/face")
public class FaceController extends ControllerBase {

    @Autowired
    private OpenCVService cvService;
    @Autowired
    private MtcnnService mtcnnService;
    @Autowired
    private FaceService faceService;
    protected final static Logger logger = LoggerFactory.getLogger(FaceController.class);

    @PostMapping("/detect")
    @ApiOperation(value = "人脸检测", notes = "<table border='1' style='font-size:12px;text-indent:10px;'>" +
            "<tr><th>名称</th><th>类型</th><th>必填</th><th>默认值</th><th>说明</th></tr>" +
            "<tr><td>image</td><td>String</td><td>Y</td><td>无</td><td>图片地址</td></tr>" +
            "<tr><td>minFaceSize</td><td>Integer</td><td>N</td><td>12</td><td>最小人脸尺寸</td></tr>" +
            "<tr><td>maxFaceNumber</td><td>Integer</td><td>N</td><td>0</td><td>最大特征值个数</td></tr>")
    public String detectAction(@RequestBody Map<?, ?> param) {
        Mat image = cvService.imdecode(DPUtil.parseString(param.get("image")), Imgcodecs.IMREAD_COLOR);
        if (cvService.empty(image)) return ApiUtil.echoResult(1001, "读取图片失败", image);
        int minFaceSize = ValidateUtil.filterInteger(param.get("minFaceSize"), true, 1, 100, 12);
        int maxFaceNumber = ValidateUtil.filterInteger(param.get("maxFaceNumber"), true, 0, 100, 0);
        List<Box> boxes = mtcnnService.detect(image, minFaceSize, false);
        int width = image.width(), height = image.height();
        ArrayNode result = mtcnnService.format(boxes, width, height);
        int size = boxes.size();
        if (size > maxFaceNumber) return ApiUtil.echoResult(0, null, result);
        List<Mat> faces = new ArrayList<>();
        for (Box box : boxes) {
            Mat face = image.submat(box.rect());
            Imgproc.resize(face, face, faceService.faceSize);
            faces.add(face);
        }
        FloatBuffer buffer = faceService.face(faces);
        for (int i = 0; i < size; i++) {
            ObjectNode node = (ObjectNode) result.get(i);
            node.put("face", cvService.imencode(faces.get(i), true));
            ArrayNode eigenvalue = node.putArray("eigenvalue");
            for (int j = 0; j < faceService.eigenvalueSize; j++) {
                eigenvalue.add(buffer.get());
            }
        }
        return ApiUtil.echoResult(0, null, result);
    }

}
