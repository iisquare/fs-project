package com.iisquare.fs.web.oa.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.oa.service.FormService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/form")
public class FormController extends PermitControllerBase {

    @Autowired
    private FormService formService;
    @Autowired
    private DefaultRbacService rbacService;

    @RequestMapping("/frame")
    public String frameAction(@RequestBody Map<?, ?> param) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        ObjectNode frame = formService.frame(id, DPUtil.objectNode(), false, true);
        return ApiUtil.echoResult(null == frame ? 404 : 0, null, frame);
    }

    @RequestMapping("/list")
    public String listAction(@RequestBody Map<String, Object> param) {
        Integer frameId = ValidateUtil.filterInteger(param.get("frameId"), true, 1, null, 0);
        ObjectNode frame = formService.frame(frameId, DPUtil.objectNode(), true, true);
        if (null == frame) return ApiUtil.echoResult(1001, "所属表单异常", frameId);
        Map<?, ?> result = formService.search(frame, param, DPUtil.buildMap(
                String.class, Object.class, "withUserInfo", true
        ));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/save")
    public String saveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Integer frameId = ValidateUtil.filterInteger(param.get("frameId"), true, 1, null, 0);
        ObjectNode frame = formService.frame(frameId, DPUtil.objectNode(), true, true);
        if (null == frame) return ApiUtil.echoResult(1001, "所属表单异常", frameId);
        JsonNode form = DPUtil.convertJSON(param.get("form"));
        if (null == form || !form.isObject()) return ApiUtil.echoResult(1002, "表单数据异常", form);
        Map<String, Object> result = formService.validate(frame.at("/widgets"), form, null);
        if (0 != (int) result.get("code")) return ApiUtil.echoResult(result);
        ObjectNode data = (ObjectNode) result.get("data");
        data = formService.save(frame, data, rbacService.uid(request));
        return ApiUtil.echoResult(null == data ? 500 : 0, null, data);
    }

    @RequestMapping("/delete")
    public String deleteAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Integer frameId = ValidateUtil.filterInteger(param.get("frameId"), true, 1, null, 0);
        ObjectNode frame = formService.frame(frameId, DPUtil.objectNode(), true, true);
        if (null == frame) return ApiUtil.echoResult(1001, "所属表单异常", frameId);
        List<String> ids;
        if(param.get("ids") instanceof List) {
            ids = DPUtil.parseStringList(param.get("ids"));
        } else {
            ids = Arrays.asList(DPUtil.parseString(param.get("ids")));
        }
        long result = formService.delete(frame, ids, rbacService.uid(request));
        return ApiUtil.echoResult(result >= 0 ? 0 : 500, null, result);
    }

}
