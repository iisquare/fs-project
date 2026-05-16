package com.iisquare.fs.web.file.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.tool.ArgumentBuilder;
import com.iisquare.fs.base.core.util.*;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.file.entity.Archive;
import io.minio.GetObjectResponse;
import io.minio.StatObjectResponse;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

@Service
public class FileService {

    @Autowired
    private ArchiveService archiveService;
    @Autowired
    private DefaultRbacService rbacService;

    @Value("${fs.file.shareKey:}")
    private String shareKey; // 文件分享校验码
    @Value("${fs.file.digest:512}")
    private int digest; // 文件头部摘要提取长度
    @Value("${fs.file.wmPath:}")
    private String wmPath; // 水印路径
    @Value("${fs.file.wmWidth:300}")
    private int wmWidth; // 水印最小宽度
    @Value("${fs.file.wmHeight:300}")
    private int wmHeight; // 水印最小高度
    @Value("${fs.file.wmPosition:rb}")
    private String wmPosition; // 水印位置，固定防裁剪拼接
    private static final Map<String, String> abbreviation = new LinkedHashMap<>() {{
        put("w", "width"); // 宽度
        put("h", "height"); // 高度
        put("c", "clip"); // 剪裁缩放
        put("m", "mark"); // 水印类型，保留参数
        put("t", "token"); // 无水印图校验码
        put("q", "quality"); // 图片质量
    }};
    @Value("${fs.site.urls.file}")
    private String fileUrl;
    @Autowired
    private MinIOService minIOService;

    public Map<String, Map<String, String>> scale(String field) {
        java.util.List<Map<String, String>> scale = Arrays.asList(new LinkedHashMap<String, String>() {{
            put("id", "fw");
            put("label", "固定宽度");
            put("value", "fixed-width");
        }}, new LinkedHashMap<String, String>() {{
            put("id", "fh");
            put("label", "固定高度");
            put("value", "fixed-height");
        }});
        return DPUtil.list2map(scale, String.class, field);
    }

    public Map<String, Map<String, String>> position(String field) {
        List<Map<String, String>> position = Arrays.asList(new LinkedHashMap<String, String>() {{
            put("id", "lt");
            put("label", "左上");
            put("value", "left-top");
        }}, new LinkedHashMap<String, String>() {{
            put("id", "rt");
            put("label", "右上");
            put("value", "right-top");
        }}, new LinkedHashMap<String, String>() {{
            put("id", "lb");
            put("label", "左下");
            put("value", "left-bottom");
        }}, new LinkedHashMap<String, String>() {{
            put("id", "rb");
            put("label", "右下");
            put("value", "right-bottom");
        }}, new LinkedHashMap<String, String>() {{
            put("id", "cc");
            put("label", "居中");
            put("value", "center");
        }});
        return DPUtil.list2map(position, String.class, field);
    }

    /**
     * 解析图片参数
     */
    public ObjectNode decode(String filename) {
        int index = filename.lastIndexOf(".");
        String suffix = "";
        if (-1 != index) {
            suffix = filename.substring(index);
            filename = filename.substring(0, index);
        }
        index = filename.indexOf("-");
        String uri = "";
        if (-1 != index) {
            uri = filename.substring(index + 1);
            filename = filename.substring(0, index);
        }
        ObjectNode json = DPUtil.objectNode();
        json.put("id", filename).put("suffix", suffix);
        SortedMap<String, String> arg = ArgumentBuilder.newInstance(abbreviation, uri).withEmptyField(true).arg();
        for (Map.Entry<String, String> entry : arg.entrySet()) {
            json.put(entry.getKey(), entry.getValue());
        }
        return json;
    }

    /**
     * 获取图片校验码
     */
    public String encode(String id) {
        return CodeUtil.md5(CodeUtil.md5(id) + shareKey).substring(0, 6);
    }

    /**
     * 获取文件Token
     */
    public String encode(String id, int expire, Long time) {
        if (null == time) time = System.currentTimeMillis();
        return CodeUtil.md5(CodeUtil.md5(CodeUtil.md5(id + time) + expire) + shareKey);
    }

    /**
     * 验证文件Token
     */
    public boolean decode(String id, Map<String, Object> arg) {
        long t = System.currentTimeMillis();
        long time = DPUtil.parseLong(arg.get("time")); // 分享开始时间
        int expire = DPUtil.parseInt(arg.get("expire")); // 分享过期时长
        if (time > t || expire < 1 || time + expire < t) return false;
        String token = DPUtil.parseString(arg.get("token"));
        return encode(id, expire, time).equals(token);
    }

    /**
     * 将文件转换为图片
     */
    public Map<String, Object> image(InputStream stream, ObjectNode args) {
        Integer width = ValidateUtil.filterInteger(args.at("/width").asInt(0), true, 16, 4096, null);
        Integer height = ValidateUtil.filterInteger(args.at("/height").asInt(0), true, 16, 4096, null);
        if (null == width || null == height) return ApiUtil.result(1503, "图片尺寸异常", null);
        File wm = null;
        if (!DPUtil.empty(wmPath)) {
            try {
                wm = ResourceUtils.getFile(wmPath);
            } catch (FileNotFoundException e) {
                return ApiUtil.result(1401, "导入资源失败", e.getMessage());
            }
        }
        Image watermark = null;
        try {
            if (null != wm ) watermark = ImageIO.read(wm);
        } catch (IOException e) {
            return ApiUtil.result(1402, "加载资源失败", e.getMessage());
        }
        Image background;
        try {
            background = ImageIO.read(stream);
        } catch (IOException e) {
            return ApiUtil.result(1403, "加载文件失败", e.getMessage());
        }
        int sourceWidth = background.getWidth(null);
        int sourceHeight = background.getHeight(null);
        Map<String, Map<String, String>> position = position("id");
        String clip = ""; // 默认不剪裁
        if (args.has("clip")) {
            clip = args.get("clip").asText();
            if ("fw".equals(clip)) { // 固定宽度，高度按比例缩放
                height = (int) Math.ceil(sourceHeight * Double.valueOf(width) / sourceWidth);
            } else if ("fh".equals(clip)) { // 固定高度，宽度按比例缩放
                width = (int) Math.ceil(sourceWidth * Double.valueOf(height) / sourceHeight);
            } else {
                if (!position.containsKey(clip)) clip = "cc";
                // 执行剪裁时，防止图片拉伸
                width = Math.min(width, sourceWidth);
                height = Math.min(height, sourceHeight);
            }
        }
        double rateWidth = Double.valueOf(sourceWidth) / Double.valueOf(width);
        double rateHeight = Double.valueOf(sourceHeight) / Double.valueOf(height);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        int x = 0, y = 0, w = sourceWidth, h = sourceHeight;
        switch (clip) {
            case "lt":
                if (rateWidth > rateHeight) { // 全显示高度，剪裁右部宽度w=sx2
                    w = (int) Math.ceil(width * rateHeight);
                } else { // 全显示宽度，剪裁底部高度h=sy2
                    h = (int) Math.ceil(height * rateWidth);
                }
                break;
            case "rt":
                if (rateWidth > rateHeight) { // 全显示高度，剪裁左部宽度x=sx1
                    x = sourceWidth - (int) Math.ceil(width * rateHeight);
                } else { // 全显示宽度，剪裁底部高度h=sy2
                    h = (int) Math.ceil(height * rateWidth);
                }
                break;
            case "lb":
                if (rateWidth > rateHeight) { // 全显示高度，剪裁右部宽度w=sx2
                    w = (int) Math.ceil(width * rateHeight);
                } else { // 全显示宽度，剪裁顶部高度y=sy1
                    y = sourceHeight - (int) Math.ceil(height * rateWidth);
                }
                break;
            case "rb":
                if (rateWidth > rateHeight) { // 全显示高度，剪裁左部宽度x=sx1
                    x = sourceWidth - (int) Math.ceil(width * rateHeight);
                } else { // 全显示宽度，剪裁顶部高度y=sy1
                    y = sourceHeight - (int) Math.ceil(height * rateWidth);
                }
                break;
            case "cc":
                if (rateWidth > rateHeight) { // 全显示高度，剪裁上下宽度x=sx1和w=sx2
                    x = (int) Math.ceil((sourceWidth - width * rateHeight) / 2);
                    w = sourceWidth - x;
                } else { // 全显示宽度，剪裁上下高度y=sy1和h=sy2
                    y = (int) Math.ceil((sourceHeight - height * rateWidth) / 2);
                    h = sourceHeight - y;
                }
                break;
        }
        graphics.drawImage(background, 0, 0, width, height, x, y, w, h, null);
        if (null == watermark || (width < wmWidth && height < wmHeight)) {
            graphics.dispose();
            return ApiUtil.result(0, null, image);
        }
        String token = args.at("/token").asText();
        if (!DPUtil.empty(token) && encode(args.at("/id").asText()).equals(token)) {
            graphics.dispose();
            return ApiUtil.result(0, null, image);
        }
        sourceWidth = watermark.getWidth(null);
        sourceHeight = watermark.getHeight(null);
        int dx1 = 0;
        int dy1 = 0;
        int dx2 = Math.min(sourceWidth, width);
        int dy2 = Math.min(sourceHeight, height);
        int sx1 = 0;
        int sy1 = 0;
        int sx2 = Math.min(sourceWidth, width);
        int sy2 = Math.min(sourceHeight, height);
        switch (wmPosition) {
            case "lt":
                break;
            case "rt":
                dx1 = Math.max(0, width - sourceWidth);
                dx2 += dx1;
                sx1 = Math.max(0, sourceWidth - width);
                sx2 += sx1;
                break;
            case "lb":
                dy1 = Math.max(0, height - sourceHeight);
                dy2 += dy1;
                sy1 = Math.max(0, sourceHeight - height);
                sy2 += sy1;
                break;
            case "rb":
                dx1 = Math.max(0, width - sourceWidth);
                dx2 += dx1;
                dy1 = Math.max(0, height - sourceHeight);
                dy2 += dy1;
                sx1 = Math.max(0, sourceWidth - width);
                sx2 += sx1;
                sy1 = Math.max(0, sourceHeight - height);
                sy2 += sy1;
                break;
            case "cc":
                if (width > sourceWidth) {
                    dx1 = (width - sourceWidth) / 2;
                    dx2 = dx1 + sourceWidth;
                } else {
                    sx1 = (sourceWidth - width) / 2;
                    sx2 = sx1 + width;
                }
                if (height > sourceHeight) {
                    dy1 = (height - sourceHeight) / 2;
                    dy2 = dy1 + sourceHeight;
                } else {
                    sy1 = (sourceHeight - height) / 2;
                    sy2 = sy1 + height;
                }
                break;
        }
        graphics.drawImage(watermark, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null);
        graphics.dispose();
        return ApiUtil.result(0, null, image);
    }

    /**
     * 获取图片URI
     */
    public String uri(JsonNode json) {
        if (null == json || !json.isObject()) return null;
        String id = json.at("/id").asText();
        if (DPUtil.empty(id)) return null;
        ArgumentBuilder args = ArgumentBuilder.newInstance().abbreviation(abbreviation).withEmptyField(false);
        for (Map.Entry<String, String> entry : abbreviation.entrySet()) {
            String parameter = entry.getValue();
            if ("token".equals(parameter)) {
                args.add(parameter, json.at("/" + parameter).asBoolean(false) ? encode(id) : null);
            } else {
                args.add(parameter, json.at("/" + parameter).asText());
            }
        }
        String uri = args.uri();
        if (uri.length() > 0) uri = "-" + uri;
        return String.format("/image/%s%s%s", id, uri, json.at("/suffix").asText(""));
    }

    /**
     * 获取文件URI
     */
    public String uri(String id, String suffix, int expire, Long time) {
        if (null == time) time = System.currentTimeMillis();
        String token = encode(id, expire, time);
        return String.format("/file/%s%s?time=%d&expire=%d&token=%s", id, suffix, time, expire, token);
    }

    /**
     * 获取文件完整链接
     */
    public String url(String uri) {
        if (DPUtil.empty(uri)) return uri;
        return (fileUrl.startsWith("http") ? "" : "http:") + fileUrl + uri;
    }

    /**
     * 根据ID获取文件访问地址
     * { id: { 参数 } }
     */
    public ObjectNode url(JsonNode json) {
        ObjectNode result = DPUtil.objectNode();
        if (null == json) return result;
        long millis = System.currentTimeMillis();
        Map<String, Archive> archives = DPUtil.list2map(archiveService.all(DPUtil.fields(json)), String.class, "id");
        for (Map.Entry<String, JsonNode> entry : json.properties()) {
            String id = entry.getKey();
            ObjectNode args = (ObjectNode) entry.getValue();
            ObjectNode item = result.putObject(id);
            if (DPUtil.empty(id) || !args.isObject()) continue;
            Archive archive = archives.get(id);
            if (1 != archive.getStatus() || archive.getDeletedTime() != 0) continue;
            item.put("id", archive.getId());
            item.put("name", archive.getName());
            item.put("bucket", archive.getBucket());
            item.put("filepath", archive.getFilepath());
            String uri = "";
            switch (args.at("/type").asText("")) {
                case "file":
                    long time = args.at("/time").asLong(millis);
                    int expire = args.at("/expire").asInt(300000);
                    uri = uri(archive.getId(), archive.getSuffix(), expire, time);
                    break;
                case "image":
                    args.put("suffix", archive.getSuffix());
                    uri = uri(args);
                    break;
            }
            item.put("uri", uri);
        }
        return result;
    }

    public Map<String, Object> upload(HttpServletRequest request, MultipartFile file, Map<String, Object> param) {
        String bucket = DPUtil.parseString(param.get("bucket"));
        if (DPUtil.empty(bucket)) return ApiUtil.result(1001, "文件桶不能为空", bucket);
        String filepath = DPUtil.parseString(param.get("filepath"));
        if (DPUtil.empty(filepath)) return ApiUtil.result(1002, "文件路径不能为空", filepath);
        if(null == file) {
            return ApiUtil.result(1003, "获取文件句柄失败", null);
        }
        Archive archive = new Archive();
        archive.setId(Archive.uuid());
        archive.setName(file.getOriginalFilename());
        archive.setBucket(bucket);
        archive.setFilepath(filepath);
        archive.setSuffix(Archive.suffix(archive.getName()));
        archive.setType(file.getContentType());
        archive.setSize(file.getSize());
        InputStream stream = null;
        try {
            stream = file.getInputStream();
            archive.setDigest(FileUtil.digest(stream, digest));
        } catch (IOException e) {
            return ApiUtil.result(1501, "计算文件摘要失败", e.getMessage());
        } finally {
            FileUtil.close( stream);
        }
        try {
            stream = file.getInputStream();
            archive.setHash(DigestUtils.md5Hex(stream));
        } catch (IOException e) {
            return ApiUtil.result(1502, "计算文件哈希失败", e.getMessage());
        } finally {
            FileUtil.close( stream);
        }
        archive.setStatus(1);
        archive = archiveService.add(archive, rbacService.uid(request));
        try {
            stream = file.getInputStream();
            minIOService.putObject(bucket, filepath, stream, file.getContentType(), null, null);
        } catch (Exception e) {
            archive.setStatus(4);
            archiveService.update(archive, rbacService.uid(request));
            return ApiUtil.result(1503, "存储文件失败", e.getMessage());
        } finally {
            FileUtil.close( stream);
        }
        return ApiUtil.result(0, null, archive);
    }

    public String download(Map<String, Object> param) {
        String id = DPUtil.parseString(param.get("id"));
        ObjectNode json = DPUtil.objectNode();
        json.putObject(id).put("type", "file");
        return url(json).at("/" + id + "/uri").asText();
    }

    public Map<String, Object> image(String filename, HttpServletResponse response) {
        ObjectNode args = decode(filename);
        Archive archive = archiveService.info(args.at("/id").asText());
        if (null == archive || 1 != archive.getStatus() || archive.getDeletedTime() > 0) {
            return ApiUtil.result(1404, "文件不可用", filename);
        }
        if (!args.at("/suffix").asText("").equals(archive.getSuffix())) {
            return ApiUtil.result(1405, "文件格式不匹配", filename);
        }
        if (!Arrays.asList("image/jpeg").contains(archive.getType())) {
            return ApiUtil.result(1406, "文件类型暂不支持", filename);
        }
        GetObjectResponse ores;
        try {
            ores = minIOService.getObject(archive.getBucket(), archive.getFilepath());
        } catch (Exception e) {
            return ApiUtil.result(1501, "获取文件对象失败", e.getMessage());
        }
        Map<String, Object> result = image(ores, args);
        if (ApiUtil.failed(result)) return result;
        BufferedImage image = ApiUtil.data(result, BufferedImage.class);
        ServletOutputStream out;
        try {
            out = response.getOutputStream();
        } catch (IOException e) {
            return ApiUtil.result(1505, "获取输出流失败", e.getMessage());
        }
        float quality = ValidateUtil.filterFloat(args.at("/quality").asDouble(0.75f), true, 0.0f, 1.0f, 0.75f);
        try {
            // 获取 JPEG 图片写入器
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
            if (!writers.hasNext()) {
                return ApiUtil.result(1502, "No JPEG writers found", null);
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
            return ApiUtil.result(0, null, null);
        } catch (IOException e) {
            return ApiUtil.result(1500, "JPEG encoding failed", e);
        } finally {
            FileUtil.close(out, ores);
        }
    }

    public Map<String, Object> file(String filename, Map<String, Object> param, HttpServletRequest request, HttpServletResponse response) {
        String[] strings = DPUtil.explode("\\.", filename);
        if (!decode(strings[0], param)) {
            return ApiUtil.result(1403, "文件已过期", filename);
        }
        Archive archive = archiveService.info(strings[0]);
        if (null == archive || 1 != archive.getStatus() || archive.getDeletedTime() > 0) {
            return ApiUtil.result(1404, "文件不可用", filename);
        }
        if (!filename.equals(archive.getId() + archive.getSuffix())) {
            return ApiUtil.result(1405, "文件格式不匹配", filename);
        }
        StatObjectResponse stat;
        try {
            stat = minIOService.statObject(archive.getBucket(), archive.getFilepath());
        } catch (Exception e) {
            return ApiUtil.result(1500, "获取文件状态失败", e.getMessage());
        }
        GetObjectResponse ores;
        try {
            ores = minIOService.getObject(archive.getBucket(), archive.getFilepath());
        } catch (Exception e) {
            return ApiUtil.result(1501, "获取文件对象失败", e.getMessage());
        }
        String range = DPUtil.parseString(request.getHeader("range"));
        String[] split = range.split("bytes=|-");
        long begin = 0;
        if (split.length >= 2) {
            begin = Long.parseLong(split[1]);
        }
        long end = stat.size() - 1;
        if (split.length >= 3) {
            end = Long.parseLong(split[2]);
        }
        long len = (end - begin) + 1;
        if (end > stat.size()) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return ApiUtil.result(1502, "结束位置溢出", filename);
        }
        ServletOutputStream out;
        try {
            out = response.getOutputStream();
        } catch (IOException e) {
            return ApiUtil.result(1505, "获取输出流失败", e.getMessage());
        }
        try {
            ores.skip(begin);
            if (!DPUtil.empty(range)) {
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT); // 响应状态 206 部分内容
                response.addHeader("Content-Range", "bytes " + begin + "-" + end + "/" + stat.size());
                response.addHeader("Accept-Ranges", "bytes");
                response.addHeader("Cache-control", "private");
            }
            response.setContentType(archive.getType());
            response.addHeader("Content-Length", String.valueOf(len));
            response.addHeader("Content-Disposition", "attachment; filename=" + archive.getName());
            response.addHeader("Last-Modified", new SimpleDateFormat("EEE, d MMM yyyy hh:mm:ss Z", Locale.ENGLISH).format(stat.lastModified()) + " GMT");
            byte[] buf = new byte[1024];
            while (len > 0) {
                ores.read(buf);
                long l = len > 1024 ? 1024 : len;
                out.write(buf, 0, (int) l);
                out.flush();
                len -= l;
            }
            return ApiUtil.result(0, null, null);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return ApiUtil.result(1503, "读取文件异常", e.getMessage());
        } finally {
            FileUtil.close(out, ores);
        }
    }

}
