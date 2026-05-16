package com.iisquare.fs.web.file.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.file.entity.Archive;
import com.iisquare.fs.web.file.service.ArchiveService;
import com.iisquare.fs.web.file.service.FileService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/archive")
@RefreshScope
public class ArchiveController extends PermitControllerBase {
    @Autowired
    private FileService fileService;
    @Autowired
    private ArchiveService archiveService;

    @RequestMapping("/info")
    @Permission("")
    public String infoAction(@RequestBody Map<?, ?> param) {
        Archive info = archiveService.info(DPUtil.parseString(param.get("id")));
        return ApiUtil.echoResult(null == info ? 404 : 0, null, info);
    }

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<String, Object> param) {
        Object result = archiveService.search(param, DPUtil.buildMap(
                "withUserInfo", true, "withStatusText", true
        ));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/save")
    @Permission
    public String saveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Map<String, Object> result = archiveService.save(param, request);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/delete")
    @Permission
    public String deleteAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        List<String> ids = DPUtil.parseStringList(param.get("ids"));
        boolean result = archiveService.deleteById(ids, request);
        return ApiUtil.echoResult(result ? 0 : 500, null, result);
    }

    @RequestMapping("/config")
    @Permission("")
    public String configAction(ModelMap model) {
        model.put("status", archiveService.status());
        model.put("scale", fileService.scale("id"));
        model.put("position", fileService.position("id"));
        return ApiUtil.echoResult(0, null, model);
    }

    @PostMapping("/upload")
    @Permission
    public String uploadAction(HttpServletRequest request, @RequestPart("file") MultipartFile file, @RequestParam Map<String, Object> param) throws Exception {
        Map<String, Object> result = fileService.upload(request, file, param);
        return ApiUtil.echoResult(result);
    }

    @GetMapping("/download")
    @Permission
    public String downloadAction(@RequestParam Map<String, Object> param, HttpServletResponse response) throws Exception {
        String url = fileService.url(fileService.download(param));
        if (DPUtil.empty(url)) return ApiUtil.echoResult(1404, "获取下载地址失败", param);
        return redirect(response, url);
    }

}
