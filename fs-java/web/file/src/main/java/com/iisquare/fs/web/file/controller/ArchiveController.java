package com.iisquare.fs.web.file.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.file.entity.Archive;
import com.iisquare.fs.web.file.service.ArchiveService;
import com.iisquare.fs.web.file.service.OSSService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;

@RestController
@RequestMapping("/archive")
@RefreshScope
public class ArchiveController extends PermitControllerBase {
    @Autowired
    private OSSService ossService;
    @Autowired
    private ArchiveService archiveService;
    @Autowired
    private DefaultRbacService rbacService;
    @Value("${fs.site.urls.file}")
    private String fileUrl;

    @RequestMapping("/info")
    @Permission("")
    public String infoAction(@RequestBody Map<?, ?> param) {
        String id = DPUtil.parseString(param.get("id"));
        Archive info = archiveService.info(id);
        return ApiUtil.echoResult(null == info ? 404 : 0, null, info);
    }

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<?, ?> param) {
        Map<?, ?> result = archiveService.search(param, DPUtil.buildMap(
                "withUserInfo", true, "withStatusText", true
        ));
        return ApiUtil.echoResult(0, null, result);
    }

    @RequestMapping("/save")
    @Permission
    public String saveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        String id = DPUtil.parseString(param.get("id"));
        Archive info = archiveService.info(id);
        if(null == info) return ApiUtil.echoResult(404, null, id);
        int status = DPUtil.parseInt(param.get("status"));
        if(!archiveService.status("default").containsKey(status)) {
            return ApiUtil.echoResult(1002, "状态异常", status);
        }
        info.setStatus(status);
        info = archiveService.update(info, rbacService.uid(request));
        return ApiUtil.echoResult(null == info ? 500 : 0, null, info);
    }

    @RequestMapping("/delete")
    @Permission
    public String deleteAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        String id = DPUtil.parseString(param.get("id"));
        Archive info = archiveService.info(id);
        if(null == info || -1 == info.getStatus()) return ApiUtil.echoResult(404, null, id);
        if (!ossService.delete(info)) return ApiUtil.echoResult(5001, "删除文件失败", info);
        info.setStatus(-1);
        info = archiveService.update(info, rbacService.uid(request));
        return ApiUtil.echoResult(null == info ? 500 : 0, null, info);
    }

    @RequestMapping("/config")
    @Permission("")
    public String configAction(ModelMap model) {
        model.put("status", archiveService.status("default"));
        model.put("scale", ossService.scale("id"));
        model.put("position", ossService.position("id"));
        return ApiUtil.echoResult(0, null, model);
    }

    @PostMapping("/upload")
    @Permission
    public String uploadAction(HttpServletRequest request, @RequestPart("file") MultipartFile file, @RequestParam Map param) throws Exception {
        String bucket = DPUtil.parseString(param.get("bucket"));
        if (DPUtil.empty(bucket)) return ApiUtil.echoResult(1001, "请选择文件归类", param);
        Map<String, Object> result = ossService.archive(bucket, file);
        if (0 != (int) result.get("code")) return ApiUtil.echoResult(result);
        Archive archive = (Archive) result.get("data");
        if (null == archiveService.add(archive, rbacService.uid(request))) {
            return ApiUtil.echoResult(5001, "保存记录失败", archive);
        }
        result = ossService.transfer(archive, file);
        if (0 != (int) result.get("code")) return ApiUtil.echoResult(result);
        ObjectNode data = DPUtil.toJSON(result.get("data"), ObjectNode.class);
        data.put("fileUrl", fileUrl);
        int expire = ValidateUtil.filterInteger(param.get("expire"), true, 1000, null, 0);
        if (expire > 0) {
            String uri = ossService.uri(archive.getId(), archive.getSuffix(), expire, null);
            data.put("fileUri", uri);
        }
        return ApiUtil.echoResult(0, "上传成功", data);
    }

    @GetMapping("/download")
    @Permission
    public String downloadAction(@RequestParam Map param, HttpServletResponse response) throws Exception {
        String id = DPUtil.parseString(param.get("id"));
        ObjectNode json = DPUtil.objectNode();
        json.putObject(id).put("type", "file");
        Map<String, String> urls = ossService.url(json);
        String uri = urls.get(id);
        if (DPUtil.empty(uri)) return ApiUtil.echoResult(404, "获取下载地址失败", id);
        String url = (fileUrl.startsWith("http") ? "" : "http:") + fileUrl + uri;
        return redirect(response, url);
    }

}
