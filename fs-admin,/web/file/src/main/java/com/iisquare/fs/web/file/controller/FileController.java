package com.iisquare.fs.web.file.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.core.rbac.RpcControllerBase;
import com.iisquare.fs.web.file.service.ArchiveService;
import com.iisquare.fs.web.file.service.FileService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

@RestController
@RequestMapping("/file")
@RefreshScope
public class FileController extends RpcControllerBase {

    @Autowired
    FileService fileService;
    @Autowired
    ArchiveService archiveService;

    @RequestMapping("/url")
    public String urlAction(@RequestBody Map<?, ?> param) {
        return ApiUtil.echoResult(0, null, fileService.url(DPUtil.toJSON(param)));
    }

    @PostMapping("/upload")
    public String uploadAction(HttpServletRequest request, @RequestPart("file") MultipartFile file, @RequestParam Map<String, Object> param) {
        Map<String, Object> result = fileService.upload(request, file, param);
        return ApiUtil.echoResult(result);
    }

    @GetMapping("/download")
    public String downloadAction(@RequestParam Map<String, Object> param, HttpServletResponse response) throws Exception {
        String url = fileService.url(fileService.download(param));
        if (DPUtil.empty(url)) return ApiUtil.echoResult(1404, "获取下载地址失败", param);
        return ApiUtil.echoResult(0, null, url);
    }

    @RequestMapping("/delete")
    public String deleteAction(@RequestBody JsonNode json, HttpServletRequest request) {
        boolean result = archiveService.deleteByQuery(json, request);
        return ApiUtil.echoResult(result ? 0 : 500, null, result);
    }

    @RequestMapping("/config")
    public String configAction(ModelMap model) {
        model.put("scale", fileService.scale("id"));
        model.put("position", fileService.position("id"));
        return ApiUtil.echoResult(0, null, model);
    }

}
