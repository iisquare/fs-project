package com.iisquare.fs.web.bi.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.bi.service.JarCatalogService;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/diagram/jar")
public class DiagramJarController extends PermitControllerBase {

    @Autowired
    JarCatalogService jarCatalogService;

    @RequestMapping("/list")
    @Permission({"bi:diagram:"})
    public String listAction(@RequestBody(required = false) Map<String, Object> param) {
        return ApiUtil.echoResult(0, null, jarCatalogService.search(
                null == param ? new java.util.LinkedHashMap<>() : param));
    }

    @RequestMapping("/info")
    @Permission({"bi:diagram:"})
    public String infoAction(@RequestParam("id") String id) {
        ObjectNode info = jarCatalogService.detail(id);
        return ApiUtil.echoResult(null == info ? 404 : 0, null, info);
    }

    @PostMapping("/upload")
    @Permission({"bi:diagram:add", "bi:diagram:modify"})
    public String uploadAction(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        return ApiUtil.echoResult(jarCatalogService.upload(file, request));
    }

    @RequestMapping("/nodes")
    @Permission({"bi:diagram:"})
    public String nodesAction(@RequestParam(value = "id", required = false, defaultValue = "") String id) {
        return ApiUtil.echoResult(0, null, jarCatalogService.nodes(id));
    }

    @PostMapping("/delete")
    @Permission({"bi:diagram:add", "bi:diagram:modify"})
    public String deleteAction(@RequestBody Map<?, ?> param) {
        return ApiUtil.echoResult(jarCatalogService.delete(DPUtil.parseString(param.get("id"))));
    }

    /**
     * 计算引擎通过该地址取得文件服务签名链接。
     */
    @GetMapping("/download")
    public String downloadAction(@RequestParam("id") String id, HttpServletResponse response) throws IOException {
        String url = jarCatalogService.downloadUrl(id);
        if (DPUtil.empty(url)) return ApiUtil.echoResult(1404, "JAR 文件不可用", id);
        response.sendRedirect(url);
        return null;
    }

}
