package com.iisquare.fs.web.file.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.web.core.rbac.PermitControllerBase;
import com.iisquare.fs.web.file.entity.Archive;
import com.iisquare.fs.web.file.service.ArchiveService;
import com.iisquare.fs.web.file.service.OSSService;
import jakarta.servlet.ServletOutputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

@Controller
@RequestMapping("/")
@RefreshScope
public class IndexController extends PermitControllerBase {

    @Autowired
    private OSSService ossService;
    @Autowired
    private ArchiveService archiveService;

    @GetMapping("/image/{filename:.+}")
    public String imageAction(@PathVariable("filename") String filename, HttpServletResponse response) throws Exception {
        ObjectNode args = ossService.decode(filename);
        Archive archive = archiveService.info(args.at("/id").asText());
        if (null == archive || 1 != archive.getStatus()) {
            return displayJSON(response, ApiUtil.result(1404, "文件不可用", filename));
        }
        if (!args.at("/suffix").asText("").equals(archive.getSuffix())) {
            return displayJSON(response, ApiUtil.result(1405, "文件格式不匹配", filename));
        }
        if (!Arrays.asList("image/jpeg").contains(archive.getType())) {
            return displayJSON(response, ApiUtil.result(1406, "文件类型暂不支持", filename));
        }
        File file = ossService.file(archive);
        if (null == file) {
            return displayJSON(response, ApiUtil.result(1501, "获取文件句柄失败", filename));
        }
        Map<String, Object> result = ossService.image(file, args);
        if (0 != (int) result.get("code")) return displayJSON(response, result);
        BufferedImage image = (BufferedImage) result.get("data");
        ServletOutputStream out = response.getOutputStream();
        float quality = ValidateUtil.filterFloat(args.at("/quality").asDouble(0.75f), true, 0.0f, 1.0f, 0.75f);
        try {
            // 获取 JPEG 图片写入器
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
            if (!writers.hasNext()) {
                return displayJSON(response, ApiUtil.result(1502, "No JPEG writers found", null));
            }
            ImageWriter writer = writers.next();
            // 配置编码参数
            ImageWriteParam params = writer.getDefaultWriteParam();
            params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            params.setCompressionQuality(quality); // quality 范围 0.0f ~ 1.0f
            // 创建图片输出流
            response.setContentType("image/jpeg");
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
                writer.setOutput(ios);
                writer.write(null, new IIOImage(image, null, null), params);
            }
            writer.dispose();
            return null;
        } catch (IOException e) {
            return displayJSON(response, ApiUtil.result(1500, "JPEG encoding failed", e));
        }
    }

    @GetMapping("/file/{filename:.+}")
    public String fileAction(@PathVariable("filename") String filename, @RequestParam Map<String, Object> param, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String[] strings = DPUtil.explode("\\.", filename);
        if (!ossService.decode(strings[0], param)) {
            return displayJSON(response, ApiUtil.result(1403, "文件已过期", filename));
        }
        Archive archive = archiveService.info(strings[0]);
        if (null == archive || 1 != archive.getStatus()) {
            return displayJSON(response, ApiUtil.result(1404, "文件不可用", filename));
        }
        if (!filename.equals(archive.getId() + archive.getSuffix())) {
            return displayJSON(response, ApiUtil.result(1405, "文件格式不匹配", filename));
        }
        File file = ossService.file(archive);
        if (null == file) {
            return displayJSON(response, ApiUtil.result(1501, "获取文件句柄失败", filename));
        }
        String range = DPUtil.parseString(request.getHeader("range"));
        String[] split = range.split("bytes=|-");
        long begin = 0;
        if (split.length >= 2) {
            begin = Long.valueOf(split[1]);
        }
        long end = file.length() - 1;
        if (split.length >= 3) {
            end = Long.valueOf(split[2]);
        }
        long len = (end - begin) + 1;
        if (end > file.length()) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return displayJSON(response, ApiUtil.result(1502, "结束位置溢出", filename));
        }

        FileInputStream in = null;
        ServletOutputStream out = response.getOutputStream();
        try {
            in = new FileInputStream(file);
            in.skip(begin);
            if (!DPUtil.empty(range)) {
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT); // 响应状态 206 部分内容
                response.addHeader("Content-Range", "bytes " + begin + "-" + end + "/" + file.length());
                response.addHeader("Accept-Ranges", "bytes");
                response.addHeader("Cache-control", "private");
            }
            response.setContentType(archive.getType());
            response.addHeader("Content-Length", String.valueOf(len));
            response.addHeader("Content-Disposition", "attachment; filename=" + archive.getName());
            response.addHeader("Last-Modified", new SimpleDateFormat("EEE, d MMM yyyy hh:mm:ss Z", Locale.ENGLISH).format(file.lastModified()) + " GMT");
            byte[] buf = new byte[1024];
            while (len > 0) {
                in.read(buf);
                long l = len > 1024 ? 1024 : len;
                out.write(buf, 0, (int) l);
                out.flush();
                len -= l;
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return displayText(out, displayJSON(response, ApiUtil.result(1503, "读取文件异常", e.getMessage())));
        } finally {
            FileUtil.close(out, in);
        }
        return null;
    }

}
