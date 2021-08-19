package com.iisquare.fs.web.lucene.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.lucene.entity.Dictionary;
import com.iisquare.fs.web.lucene.helper.SCELHelper;
import com.iisquare.fs.web.lucene.service.DictionaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dictionary")
public class DictionaryController extends PermitControllerBase {

    @Autowired
    private DefaultRbacService rbacService;
    @Autowired
    private DictionaryService dictionaryService;

    @GetMapping("/plain")
    public String plainAction(@RequestParam Map<String, Object> param, HttpServletResponse response) {
        String name = DPUtil.trim(DPUtil.parseString(param.get("catalogue")));
        name += "-" + DPUtil.trim(DPUtil.parseString(param.get("type")));
        response.setContentType("text/plain;charset=utf-8");
        response.setHeader("Content-Disposition", "attachment; filename=" + name + ".dict");
        return dictionaryService.plain(param);
    }

    @RequestMapping("/info")
    @Permission("")
    public String infoAction(@RequestBody Map<?, ?> param) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        Dictionary info = dictionaryService.info(id);
        return ApiUtil.echoResult(null == info ? 404 : 0, null, info);
    }

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<?, ?> param) {
        Map<?, ?> result = dictionaryService.search(param,
                DPUtil.buildMap("withUserInfo", true, "withTypeText", true));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/save")
    @Permission({"add", "modify"})
    public String saveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        String catalogue = DPUtil.trim(DPUtil.parseString(param.get("catalogue")));
        if(DPUtil.empty(catalogue)) return ApiUtil.echoResult(1001, "词库目录异常", catalogue);
        String type = DPUtil.trim(DPUtil.parseString(param.get("type")));
        if(!dictionaryService.type().containsKey(type)) {
            return ApiUtil.echoResult(1002, "词库类型异常", type);
        }
        String content = DPUtil.trim(DPUtil.parseString(param.get("content")));
        if(DPUtil.empty(content)) return ApiUtil.echoResult(1003, "词条内容不能为空", content);
        String source = DPUtil.trim(DPUtil.parseString(param.get("source")));
        if(DPUtil.empty(source)) return ApiUtil.echoResult(1004, "词条来源不能为空", source);
        Dictionary info;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.echoResult(9403, null, null);
            info = dictionaryService.info(id);
            if(null == info) return ApiUtil.echoResult(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.echoResult(9403, null, null);
            info = new Dictionary();
        }
        info.setCatalogue(catalogue);
        info.setType(type);
        info.setContent(content);
        info.setSource(source);
        List<Dictionary> list = dictionaryService.saveAll(info, rbacService.uid(request));
        return ApiUtil.echoResult(null == list ? 500 : 0, null, list);
    }

    @RequestMapping("/delete")
    @Permission
    public String deleteAction(@RequestBody Map<?, ?> param) {
        List<Integer> ids;
        if(param.get("ids") instanceof List) {
            ids = DPUtil.parseIntList(param.get("ids"));
        } else {
            ids = Arrays.asList(DPUtil.parseInt(param.get("ids")));
        }
        boolean result = dictionaryService.delete(ids);
        return ApiUtil.echoResult(result ? 0 : 500, null, result);
    }

    @RequestMapping("/unique")
    @Permission("delete")
    public String uniqueAction(@RequestBody Map<?, ?> param) {
        String catalogue = DPUtil.trim(DPUtil.parseString(param.get("catalogue")));
        if (DPUtil.empty(catalogue)) return ApiUtil.echoResult(1001, "词库目录不能为空", catalogue);
        String type = DPUtil.trim(DPUtil.parseString(param.get("type")));
        if (DPUtil.empty(type)) return ApiUtil.echoResult(1001, "词库分类不能为空", type);
        Integer result = dictionaryService.unique(catalogue, type);
        return ApiUtil.echoResult(null == result ? 500 : 0, null, result);
    }

    @RequestMapping("/config")
    @Permission("")
    public String configAction(ModelMap model) {
        model.put("type", dictionaryService.type());
        model.put("sort", dictionaryService.sort());
        return ApiUtil.echoResult(0, null, model);
    }

    @PostMapping("/scel")
    public String scelAction(HttpServletRequest request, @RequestPart("file") MultipartFile file) {
        if(null == file) {
            return ApiUtil.echoResult(1003, "获取文件句柄失败", null);
        }
        File tmp = null;
        try {
            tmp = File.createTempFile("sougou", "scel");
        } catch (IOException e) {
            tmp.delete();
            return ApiUtil.echoResult(1004, "创建临时文件失败", e.getMessage());
        }
        try {
            file.transferTo(tmp);
        } catch (IOException e) {
            tmp.delete();
            return ApiUtil.echoResult(1005, "写入临时文件失败", e.getMessage());
        }
        Map<String, LinkedList<String>> wordList;
        try {
            wordList = SCELHelper.getInstance(tmp).parse().wordList();
        } catch (Exception e) {
            tmp.delete();
            return ApiUtil.echoResult(1006, "解析文件失败", e.getMessage());
        }
        tmp.delete();
        return ApiUtil.echoResult(0, null, wordList);
    }

}
