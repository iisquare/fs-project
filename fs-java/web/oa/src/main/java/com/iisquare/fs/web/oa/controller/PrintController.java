package com.iisquare.fs.web.oa.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.oa.entity.Print;
import com.iisquare.fs.web.oa.service.PrintService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/print")
public class PrintController extends PermitControllerBase {

    @Autowired
    PrintService printService;
    @Autowired
    DefaultRbacService rbacService;

    @RequestMapping("/info")
    @Permission("")
    public String infoAction(@RequestBody Map<?, ?> param) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        Print info = printService.info(id);
        return ApiUtil.echoResult(null == info ? 404 : 0, null, info);
    }

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<?, ?> param) {
        Map<?, ?> result = printService.search(param, DPUtil.buildMap(
            "withUserInfo", true, "withStatusText", true, "withFormFrameInfo", true
        ));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/save")
    @Permission({"add", "modify"})
    public String saveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Map<String, Object> result = printService.save(param, request);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/delete")
    @Permission
    public String deleteAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        List<Integer> ids = DPUtil.parseIntList(param.get("ids"));
        return ApiUtil.echoResult(printService.delete(ids, rbacService.uid(request)));
    }

    @RequestMapping("/config")
    @Permission("")
    public String configAction(ModelMap model) {
        model.put("status", printService.status("default"));
        return ApiUtil.echoResult(0, null, model);
    }

    @PostMapping("/document/save")
    @Permission({"add", "modify"})
    public String documentSaveAction(@RequestParam Map<String, Object> param,
                                     @RequestPart("file") MultipartFile file,
                                     HttpServletRequest request) {
        return ApiUtil.echoResult(printService.saveWithDocument(param, file, request));
    }

    @RequestMapping("/document/load")
    @Permission("")
    public String documentLoadAction(@RequestBody Map<?, ?> param) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        byte[] content = printService.document(id);
        if(null == content || content.length < 1) return ApiUtil.echoResult(404, "Word模板不存在", id);
        return ApiUtil.echoResult(0, null, DPUtil.buildMap(
                String.class, Object.class,
                "content", Base64.getEncoder().encodeToString(content),
                "size", content.length
        ));
    }

    @RequestMapping("/fields")
    @Permission("")
    public String fieldsAction(@RequestBody Map<?, ?> param) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        int frameId = ValidateUtil.filterInteger(param.get("frameId"), 1, null, 0);
        return ApiUtil.echoResult(0, null, printService.fields(id, frameId));
    }

    @RequestMapping("/render")
    @Permission("")
    public String renderAction(@RequestBody Map<?, ?> param) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        String dataId = DPUtil.parseString(param.get("dataId"));
        Object data = param.get("data");
        Map<?, ?> model = data instanceof Map ? (Map<?, ?>) data : null;
        boolean sample = DPUtil.parseBoolean(param.get("sample"));
        return ApiUtil.echoResult(printService.render(id, dataId, model, sample));
    }

}
