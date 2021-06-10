package com.iisquare.fs.web.oa.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.mongodb.MongoCore;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.oa.service.FormDataService;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/formData")
public class FormDataController extends PermitControllerBase {

    @Autowired
    private FormDataService formDataService;
    @Autowired
    private DefaultRbacService rbacService;

    @RequestMapping("/info")
    public String infoAction(@RequestBody Map<?, ?> param) {
        String id = DPUtil.trim(DPUtil.parseString(param.get(MongoCore.FIELD_ID)));
        Document info = formDataService.info(id);
        return ApiUtil.echoResult(null == info ? 404 : 0, null, info);
    }

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<?, ?> param) {
        Map<?, ?> result = formDataService.search(param, DPUtil.buildMap(
            "withUserInfo", true, "withFormFrameInfo", true
        ));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/save")
    @Permission({"add", "modify"})
    public String saveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        String id = DPUtil.trim(DPUtil.parseString(param.get(MongoCore.FIELD_ID)));
        Integer frameId = ValidateUtil.filterInteger(param.get("frameId"), true, 1, null, 0);
        if(frameId < 1) return ApiUtil.echoResult(1001, "所属表单参数异常", frameId);
        String bpmInstance = DPUtil.trim(DPUtil.parseString(param.get("bpmInstance")));
        String bpmStatus = DPUtil.trim(DPUtil.parseString(param.get("bpmStatus")));
        String bpmTask = DPUtil.trim(DPUtil.parseString(param.get("bpmTask")));
        String bpmIdentity = DPUtil.trim(DPUtil.parseString(param.get("bpmIdentity")));
        Document document = Document.parse(DPUtil.stringify(param.get("content")));
        document.append("frameId", frameId).append("bpmInstance", bpmInstance);
        document.append("bpmStatus", bpmStatus).append("bpmTask", bpmTask).append("bpmIdentity", bpmIdentity);
        if(DPUtil.empty(id)) {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.echoResult(9403, null, null);
            document.remove(MongoCore.FIELD_ID);
        } else {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.echoResult(9403, null, null);
            document.put(MongoCore.FIELD_ID, id);
        }
        document = formDataService.save(document, rbacService.uid(request));
        return ApiUtil.echoResult(null == document ? 500 : 0, null, document);
    }

    @RequestMapping("/delete")
    @Permission
    public String deleteAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        List<String> ids = null;
        if(param.get("ids") instanceof List) {
            ids = DPUtil.parseStringList(param.get("ids"));
        } else {
            ids = Arrays.asList(DPUtil.parseString(param.get("ids")));
        }
        long result = formDataService.delete(ids, rbacService.uid(request));
        return ApiUtil.echoResult(result >= 0 ? 0 : 500, null, result);
    }

    @RequestMapping("/config")
    @Permission("")
    public String configAction(ModelMap model) {
        model.put("sort", formDataService.sort());
        return ApiUtil.echoResult(0, null, model);
    }

}
