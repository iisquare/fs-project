package com.iisquare.fs.web.flink.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.core.rbac.Permission;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.flink.entity.Plugin;
import com.iisquare.fs.web.flink.service.PluginService;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.support.StandardMultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@RestController
@RequestMapping("/plugin")
@RefreshScope
public class PluginController extends PermitControllerBase {

    @Autowired
    private PluginService pluginService;
    @Value("${fs.flink.plugins.path}")
    private String PluginsPath;
    @Autowired
    private DefaultRbacService rbacService;

    @GetMapping("/tree")
    @Permission("")
    public String treeAction(@RequestParam Map<?, ?> param) {
        return DPUtil.stringify(pluginService.tree(new File(PluginsPath)));
    }

    @RequestMapping("/list")
    @Permission("")
    public String listAction(@RequestBody Map<?, ?> param) {
        Map<?, ?> result = pluginService.search(param, DPUtil.buildMap(
            "withUserInfo", true, "withStatusText", true
        ));
        return ApiUtil.echoResult(0, null, result);
    }

    @PostMapping("/upload")
    @Permission("add")
    public String uploadAction(HttpServletRequest request) {
        MultipartHttpServletRequest multiRequest = new StandardMultipartHttpServletRequest(request);
        Iterator iterator = multiRequest.getFileNames();
        if(!iterator.hasNext()) {
            return ApiUtil.echoResult(1002, "请选择上传文件", null);
        }
        MultipartFile file = multiRequest.getFile(iterator.next().toString());
        if(null == file) {
            return ApiUtil.echoResult(1003, "获取文件句柄失败", null);
        }
        if(file.getOriginalFilename().toLowerCase().endsWith(".jar")) { // 主JAR文件
            try {
                File jar = new File(PluginsPath + "/app.jar");
                file.transferTo(jar.getAbsoluteFile());
                return ApiUtil.echoResult(0, "主程序写入成功", null);
            } catch (IOException e) {
                return ApiUtil.echoResult(2001, "主文件写入失败", e.getMessage());
            }
        }
        // 插件ZIP安装
        ZipInputStream zipin = null;
        String json = "";
        Map<String, List<Integer>> zipfiles = new LinkedHashMap<>();
        try {
            zipin = new ZipInputStream(file.getInputStream(), Charset.defaultCharset());
            ZipEntry ze = null;
            while ((ze = zipin.getNextEntry()) != null) {
                String name = ze.getName();
                if(!name.startsWith("flink/")) continue;
                name = name.replaceFirst("flink/", "");
                if(ze.isDirectory()) {
                    zipfiles.put(name, null);
                } else if("config.json".equals(name)) {
                    json = new String(IOUtils.toByteArray(zipin), Charset.defaultCharset());
                } else {
                    List<Integer> zipfile = new ArrayList<>();
                    for (int c = zipin.read(); c != -1; c = zipin.read()) {
                        zipfile.add(c);
                    }
                    zipfiles.put(name, zipfile);
                }
            }
        } catch (IOException e) {
            return ApiUtil.echoResult(1001, "读取压缩文件失败", e.getMessage());
        } finally {
            IOUtils.closeQuietly(zipin);
        }
        if(json.length() < 1) {
            return ApiUtil.echoResult(1004, "未读取到有效配置文件", null);
        }
        JsonNode config = DPUtil.parseJSON(json);
        if(null == config) {
            return ApiUtil.echoResult(1005, "解析配置文件失败", null);
        }
        if(!config.has("name") || !config.has("version") || !config.has("description")) {
            return ApiUtil.echoResult(1007, "配置文件不完整", null);
        }
        String pluginName = ValidateUtil.filterSimpleString(config.get("name").asText(), true, 1, 64, null);
        if(DPUtil.empty(pluginName)) {
            return ApiUtil.echoResult(1006, "插件名称异常", pluginName);
        }
        if(pluginService.exists(pluginName) || FileUtil.isExists(PluginsPath + "/" + pluginName)) {
            return ApiUtil.echoResult(1007, "插件已存在，请卸载后重新安装", null);
        }
        for (Map.Entry<String, List<Integer>> entry : zipfiles.entrySet()) {
            String name = entry.getKey();
            String filepath = PluginsPath + "/" + pluginName + "/" + name;
            List<Integer> zipfile = entry.getValue();
            if(null == zipfile) {
                if(!FileUtil.mkdirs(filepath)) {
                    return ApiUtil.echoResult(1008, "创建目录失败", name);
                }
            } else {
                FileOutputStream out = null;
                try {
                    out = new FileOutputStream(filepath);
                    for (int c : zipfile) {
                        out.write(c);
                    }
                } catch (IOException e) {
                    return ApiUtil.echoResult(1009, "创建文件失败", name);
                } finally {
                    IOUtils.closeQuietly(out);
                }
            }
        }
        if(!FileUtil.putContent(PluginsPath + "/" + pluginName + "/config.json", json, "utf-8")) {
            return ApiUtil.echoResult(1010, "生成配置文件失败", json);
        }
        Plugin info = Plugin.builder()
            .name(pluginName).version(config.get("version").asText()).status(1)
            .description(config.get("description").asText()).config(json).build();
        info = pluginService.save(info, rbacService.uid(request));
        return ApiUtil.echoResult(null == info ? 500 : 0, null, info);
    }

    @RequestMapping("/save")
    @Permission("modify")
    public String saveAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        int status = DPUtil.parseInt(param.get("status"));
        if(!pluginService.status("default").containsKey(status)) return ApiUtil.echoResult(1002, "状态异常", status);
        Plugin info = pluginService.info(id);
        if(null == info) return ApiUtil.echoResult(404, null, id);
        info.setStatus(status);
        info = pluginService.save(info, rbacService.uid(request));
        return ApiUtil.echoResult(null == info ? 500 : 0, null, info);
    }

    @RequestMapping("/delete")
    @Permission
    public String deleteAction(@RequestBody Map<?, ?> param, HttpServletRequest request) {
        List<Integer> ids = null;
        if(param.get("ids") instanceof List) {
            ids = DPUtil.parseIntList((List<?>) param.get("ids"));
        } else {
            ids = Arrays.asList(DPUtil.parseInt(param.get("ids")));
        }
        if(ids.size() != 1) {
            return ApiUtil.echoResult(1001, "当前仅支持单个插件操作", ids);
        }
        Plugin info = pluginService.info(ids.get(0));
        if(null == info || -1 == info.getStatus()) return ApiUtil.echoResult(404, null, info);
        String filepath = PluginsPath + "/" + info.getName();
        if(!FileUtil.delete(filepath, true)) {
            return ApiUtil.echoResult(5001, "文件删除失败", filepath);
        }
        boolean result = pluginService.delete(ids, rbacService.uid(request));
        return ApiUtil.echoResult(result ? 0 : 500, null, result);
    }

    @RequestMapping("/config")
    @Permission("")
    public String configAction(ModelMap model) {
        model.put("status", pluginService.status("default"));
        return ApiUtil.echoResult(0, null, model);
    }

}
