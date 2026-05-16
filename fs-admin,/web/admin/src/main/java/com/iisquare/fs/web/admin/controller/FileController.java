package com.iisquare.fs.web.admin.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.base.web.util.RpcUtil;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.core.rpc.FileRpc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.support.StandardMultipartHttpServletRequest;

import jakarta.servlet.http.HttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 客户端直连服务
 */
@RestController
@RequestMapping("/file")
@RefreshScope
public class FileController extends PermitControllerBase {

    @Autowired
    private FileRpc fileRpc;

    public String ueditor(Map param, Object state) {
        String content;
        if (state instanceof String) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("state", state);
            content = DPUtil.stringify(result);
        } else {
            content = DPUtil.stringify(state);
        }
        String callback = DPUtil.parseString(param.get("callback"));
        if (DPUtil.empty(callback)) return content;
        return callback + "(" + content + ")";
    }

    @RequestMapping("/ueditor")
    public String ueditorAction(HttpServletRequest request, @RequestParam Map param) {
        String action = DPUtil.parseString(param.get("action"));
        if ("config".equals(action)) {
            String json = FileUtil.getContent(
                    getClass().getClassLoader().getResource("ueditor.config.json"), true, StandardCharsets.UTF_8
            );
            json = json.replaceAll("\\/\\*[\\s\\S]+?\\*\\/", "");
            JsonNode config = DPUtil.parseJSON(json);
            return ueditor(param, config);
        }
        MultipartHttpServletRequest multiRequest;
        try {
            multiRequest = new StandardMultipartHttpServletRequest(request);
        } catch (Exception e) {
            return ueditor(param, "该操作暂不支持");
        }
        Iterator iterator = multiRequest.getFileNames();
        if(!iterator.hasNext()) return ueditor(param, "请选择上传文件");
        MultipartFile file = multiRequest.getFile(iterator.next().toString());
        if(null == file) return ueditor(param, "获取文件句柄失败");
        String bucket = "ueditor-" + action;
        String json = fileRpc.upload("/archive/upload", file, DPUtil.buildMap("bucket", bucket, "expire", 50 * 360 * 24 * 60 * 60 * 100));
        JsonNode result = RpcUtil.data(json, true);
        if (null == result) return ueditor(param, "调用文件服务失败");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("state", "SUCCESS");
        String url = result.get("fileUrl").asText() + result.get("fileUri").asText();
        data.put("url", url.startsWith("//") ? "http:" + url : url);
        data.put("title", result.get("name").asText());
        data.put("original", result.get("name").asText());
        data.put("type", result.get("type").asText());
        data.put("size", result.get("size").asLong());
        return ueditor(param, data);
    }

}
