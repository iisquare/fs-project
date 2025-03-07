package com.iisquare.fs.web.face.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.core.rpc.XlabRpc;
import com.iisquare.fs.web.face.entity.User;
import com.iisquare.fs.web.face.service.FaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/face")
public class FaceController extends PermitControllerBase {

    @Autowired
    private XlabRpc xlabRpc;
    @Autowired
    private FaceService faceService;

    @GetMapping("state")
    public String stateAction() {
        return ApiUtil.echoResult(0, null, faceService.state());
    }

    @PostMapping("/reload")
    public String reloadAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        boolean modeForce = DPUtil.parseBoolean(param.get("modeForce"));
        return ApiUtil.echoResult(0, null, faceService.reload(modeForce));
    }

    @RequestMapping("/detect")
    public String detectAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        String url = DPUtil.trim(DPUtil.parseString(param.get("url")));
        if (DPUtil.empty(url)) return ApiUtil.echoResult(1001, "请输入图片地址", url);
        int maxFaceNumber = ValidateUtil.filterInteger(param.get("maxFaceNumber"), true, 0, 100, 0);
        return xlabRpc.post("/face/detect", DPUtil.buildMap("image", url, "maxFaceNumber", maxFaceNumber));
    }

    @RequestMapping("/compare")
    public String compareAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Object va = param.get("va"), vb = param.get("vb");
        if (!(va instanceof List)) return ApiUtil.echoResult(1001, "参数异常", va);
        if (!(vb instanceof List)) return ApiUtil.echoResult(1002, "参数异常", vb);
        double similarity = faceService.similarity((List) va, (List) vb);
        return ApiUtil.echoResult(0, null, similarity);
    }

    @RequestMapping("/search")
    public String searchAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Object eigenvalue = param.get("eigenvalue");
        if (!(eigenvalue instanceof List)) return ApiUtil.echoResult(1001, "请选择面部区域", eigenvalue);
        int threshold = ValidateUtil.filterInteger(param.get("threshold"), true, 0, 100, 0);
        int topN = ValidateUtil.filterInteger(param.get("topN"), true, 1, 100, 1);
        List<User> list = faceService.search((List) eigenvalue, threshold, topN);
        return ApiUtil.echoResult(0, null, list);
    }

}
