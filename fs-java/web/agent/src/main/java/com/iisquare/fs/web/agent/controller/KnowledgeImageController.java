package com.iisquare.fs.web.agent.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.agent.service.KnowledgeImageService;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 知识库图片访问
 * 图片本体由文件模块的 /raw/ 接口输出，本控制器只做权限判定与地址签发
 * 未获授权的图片不在返回值中，由前端使用公共目录下的默认图片
 */
@RestController
@RequestMapping("/knowledgeImage")
public class KnowledgeImageController extends PermitControllerBase {

    @Autowired
    KnowledgeImageService imageService;

    /**
     * 批量签发原图访问地址
     * { "knowledgeId": 1, "ids": ["abc"], "expire": 1800000 }
     * 身份取自请求会话
     */
    @RequestMapping("/url")
    public String urlAction(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        JsonNode identity = imageService.identity(request);
        Map<String, Object> result = imageService.urls(
                DPUtil.parseInt(param.get("knowledgeId")),
                DPUtil.parseStringList(param.get("ids")),
                DPUtil.parseInt(param.get("expire")),
                identity);
        return ApiUtil.echoResult(result);
    }

    /**
     * 编辑时上传图片，返回文件标识与可展示地址，正文引用格式为 ![说明](kb:文件标识)
     * POST /knowledgeImage/upload（multipart：file、knowledgeId、documentId、alt）
     */
    @RequestMapping("/upload")
    public String uploadAction(@RequestPart("file") MultipartFile file, @RequestParam Map<String, Object> param,
                               HttpServletRequest request) {
        JsonNode identity = imageService.identity(request);
        Map<String, Object> result = imageService.upload(
                DPUtil.parseInt(param.get("knowledgeId")),
                DPUtil.parseInt(param.get("documentId")),
                DPUtil.parseString(param.get("alt")),
                file, identity);
        return ApiUtil.echoResult(result);
    }

}
