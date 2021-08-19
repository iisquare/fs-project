package com.iisquare.fs.web.file.controller;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.core.rbac.RpcControllerBase;
import com.iisquare.fs.web.file.entity.Archive;
import com.iisquare.fs.web.file.service.ArchiveService;
import com.iisquare.fs.web.file.service.OSSService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/oss")
@RefreshScope
public class OSSController extends RpcControllerBase {

    @Autowired
    private DefaultRbacService rbacService;
    @Autowired
    private OSSService ossService;
    @Autowired
    private ArchiveService archiveService;

    @RequestMapping("/url")
    public String urlAction(@RequestBody Map<?, ?> param) {
        return ApiUtil.echoResult(0, null, ossService.url(DPUtil.toJSON(param)));
    }

    @PostMapping("/upload")
    public String uploadAction(HttpServletRequest request, @RequestPart("file") MultipartFile file, @RequestParam Map param) {
        String bucket = DPUtil.parseString(param.get("bucket"));
        Map<String, Object> result = ossService.archive(bucket, file);
        if (0 != (int) result.get("code")) return ApiUtil.echoResult(result);
        Archive archive = (Archive) result.get("data");
        if (null == archiveService.add(archive, rbacService.uid(request))) {
            return ApiUtil.echoResult(5001, "保存记录失败", archive);
        }
        result = ossService.transfer(archive, file);
        return ApiUtil.echoResult(result);
    }

    @RequestMapping("/delete")
    public String deleteAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        List<String> ids;
        if(param.get("ids") instanceof List) {
            ids = DPUtil.parseStringList(param.get("ids"));
        } else {
            ids = Arrays.asList(DPUtil.parseString(param.get("ids")));
        }
        boolean result = archiveService.delete(ids, rbacService.uid(request));
        return ApiUtil.echoResult(result ? 0 : 500, null, result);
    }

    @RequestMapping("/config")
    public String configAction(ModelMap model) {
        model.put("scale", ossService.scale("id"));
        model.put("position", ossService.position("id"));
        return ApiUtil.echoResult(0, null, model);
    }

}
