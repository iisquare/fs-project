package com.iisquare.fs.web.file.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.file.service.ArchiveService;
import com.iisquare.fs.web.file.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

@Controller
@RequestMapping("/")
@RefreshScope
public class IndexController extends PermitControllerBase {

    @Autowired
    private FileService fileService;
    @Autowired
    private ArchiveService archiveService;

    @GetMapping("/image/{filename:.+}")
    public String imageAction(@PathVariable("filename") String filename, HttpServletResponse response) throws Exception {
        Map<String, Object> result = fileService.image(filename, response);
        if (ApiUtil.failed(result)) return displayJSON(response, result);
        return null;
    }

    @GetMapping("/file/{filename:.+}")
    public String fileAction(@PathVariable("filename") String filename, @RequestParam Map<String, Object> param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String, Object> result = fileService.file(filename, param, request, response);
        if (ApiUtil.failed(result)) return displayJSON(response, result);
        return null;
    }

}
