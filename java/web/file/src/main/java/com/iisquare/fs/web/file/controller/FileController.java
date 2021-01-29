package com.iisquare.fs.web.file.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.file.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/file")
@RefreshScope
public class FileController extends PermitControllerBase {

    @Autowired
    private FileService fileService;
    @Autowired
    private DefaultRbacService rbacService;

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<?, ?> param) {
        return null;
    }

    @PostMapping("/upload")
    @Permission("add")
    public String uploadAction(HttpServletRequest request, @RequestPart("file") MultipartFile file) {
        if(null == file) {
            return ApiUtil.echoResult(1003, "获取文件句柄失败", null);
        }
        return null;
    }

    @RequestMapping("/delete")
    @Permission
    public String deleteAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        return null;
    }

}
