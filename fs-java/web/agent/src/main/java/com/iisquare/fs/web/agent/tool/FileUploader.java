package com.iisquare.fs.web.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Collection;
import java.util.Map;

/**
 * 文件内容工具：类型判定（后缀 / 内容嗅探）、内容解析（base64 文本 / 字节数组）与 data URL 组装。
 *
 * 供多模态工具把二进制内联成 data URL 交给模型使用。前提是：MCP 参数只能是 JSON
 * （二进制按 base64 文本或字节数组传），服务端读不到调用方本地磁盘，所以一律不接受本机路径。
 */
public class FileUploader {

    /**
     * 后缀 → MIME：没给 contentType 时按文件名判断；反向也用于 MIME → 后缀（补文件名用）。
     * 覆盖常见图片 / 视频 / 音频，以及几种常见的文本类文件（上传工具不限类型，判断不出按二进制处理）
     */
    private static final Map<String, String> SUFFIX_TYPES = Map.ofEntries(
            Map.entry("png", "image/png"), Map.entry("jpg", "image/jpeg"), Map.entry("jpeg", "image/jpeg"),
            Map.entry("webp", "image/webp"), Map.entry("gif", "image/gif"), Map.entry("bmp", "image/bmp"),
            Map.entry("mp4", "video/mp4"), Map.entry("webm", "video/webm"), Map.entry("mov", "video/quicktime"),
            Map.entry("m4v", "video/x-m4v"), Map.entry("mkv", "video/x-matroska"), Map.entry("avi", "video/x-msvideo"),
            Map.entry("mp3", "audio/mpeg"), Map.entry("wav", "audio/wav"), Map.entry("m4a", "audio/mp4"),
            Map.entry("aac", "audio/aac"), Map.entry("flac", "audio/flac"), Map.entry("ogg", "audio/ogg"),
            Map.entry("pdf", "application/pdf"), Map.entry("txt", "text/plain"), Map.entry("md", "text/markdown"),
            Map.entry("csv", "text/csv"), Map.entry("json", "application/json"), Map.entry("xml", "application/xml"),
            Map.entry("html", "text/html"), Map.entry("zip", "application/zip"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("ppt", "application/vnd.ms-powerpoint"),
            Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"));

    /** 文件名后缀（小写，不含点）：没有后缀给空串 */
    public static String suffix(String name) {
        String value = DPUtil.parseString(name);
        int index = value.lastIndexOf('.');
        return index < 0 ? "" : value.substring(index + 1).toLowerCase();
    }

    /** contentType 归一化：截掉 ";" 之后的内容（如 image/png;charset=utf-8 → image/png） */
    public static String normalize(Object type) {
        String value = DPUtil.trim(DPUtil.parseString(type));
        int index = value.indexOf(';');
        return index < 0 ? value : DPUtil.trim(value.substring(0, index));
    }

    /**
     * 文件类型判定：显式 contentType 优先，其次文件名后缀，最后按内容魔数嗅探；
     * 三者都判断不出来给空串，由调用方决定是报错还是按二进制处理
     */
    public static String mime(Object type, String name, byte[] data) {
        String value = normalize(type);
        if (!DPUtil.empty(value)) return value;
        value = SUFFIX_TYPES.getOrDefault(suffix(name), "");
        if (!DPUtil.empty(value)) return value;
        return sniff(data);
    }

    /** 内容嗅探：按文件头判断常见图片 / 视频 / 音频，判断不出给空串 */
    public static String sniff(byte[] data) {
        if (null == data || data.length < 12) return "";
        if (match(data, 0, 0x89, 'P', 'N', 'G')) return "image/png";
        if (match(data, 0, 0xFF, 0xD8, 0xFF)) return "image/jpeg";
        if (match(data, 0, 'G', 'I', 'F', '8')) return "image/gif";
        if (match(data, 0, 'B', 'M')) return "image/bmp";
        if (match(data, 0, 'R', 'I', 'F', 'F') && match(data, 8, 'W', 'E', 'B', 'P')) return "image/webp";
        if (match(data, 0, 'R', 'I', 'F', 'F') && match(data, 8, 'W', 'A', 'V', 'E')) return "audio/wav";
        if (match(data, 0, 'O', 'g', 'g', 'S')) return "audio/ogg";
        if (match(data, 0, 'f', 'L', 'a', 'C')) return "audio/flac";
        if (match(data, 0, 'I', 'D', '3')) return "audio/mpeg";
        if (match(data, 0, 0xFF, 0xFB) || match(data, 0, 0xFF, 0xF3) || match(data, 0, 0xFF, 0xF2)) {
            return "audio/mpeg";
        }
        if (match(data, 0, 0xFF, 0xF1) || match(data, 0, 0xFF, 0xF9)) return "audio/aac";
        if (match(data, 0, 0x1A, 0x45, 0xDF, 0xA3)) return "video/webm";
        // ISO 基础媒体文件：ftyp 盒的 brand 为 qt 时是 quicktime，其余按 mp4 处理
        if (match(data, 4, 'f', 't', 'y', 'p')) {
            return match(data, 8, 'q', 't', ' ', ' ') ? "video/quicktime" : "video/mp4";
        }
        return "";
    }

    /**
     * 文件内容 → 字节，三种写法都接受：
     * - base64 文本（允许带 data:...;base64, 前缀，忽略换行与空格）；
     * - 字节数组（JSON 数组，如 [137,80,78,71,...]；0-255 与 Java 风格 -128..127 都能处理）；
     * - Jackson 的二进制节点（BinaryNode）。
     * 内容非法时给出可读原因
     */
    public static byte[] decode(Object content) {
        if (null == content) throw new IllegalStateException("文件内容为空");
        if (content instanceof byte[]) return (byte[]) content;
        if (content instanceof JsonNode) {
            JsonNode node = (JsonNode) content;
            if (node.isBinary()) {
                try {
                    return node.binaryValue();
                } catch (IOException e) {
                    throw new IllegalStateException("文件内容解析失败：" + e.getMessage());
                }
            }
            if (node.isArray()) return bytes(node);
        }
        if (content instanceof Collection) return bytes((Collection<?>) content);
        String value = DPUtil.trim(DPUtil.parseString(content));
        if (DPUtil.empty(value)) throw new IllegalStateException("文件内容为空");
        if (value.startsWith("data:")) {
            int index = value.indexOf("base64,");
            if (index < 0) throw new IllegalStateException("data URL 里没有 base64 内容");
            value = value.substring(index + "base64,".length());
        }
        value = value.replaceAll("\\s", "");
        // 先排除 base64 本身（base64 里也会出现 "/"，例如 JPEG 的 /9j/4AAQ...），再判断是不是路径
        if (!value.matches("^[A-Za-z0-9+/=]+$") && localPath(value)) {
            throw new IllegalStateException("这是调用方的本机路径（" + value + "），服务端读不到调用方的磁盘："
                    + "请改传 base64、字节数组或可访问地址；如果文件已经上传到文件服务，请改用文件标识（id）引用");
        }
        try {
            return Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("文件内容不是合法的 base64 编码（字符数 " + value.length() + "）");
        }
    }

    /** 是否像本机路径：file:// 前缀、盘符路径（C:/…、C:\…）、/ 或 \ 开头的路径 */
    private static boolean localPath(String value) {
        String text = DPUtil.trim(value).replace('\\', '/');
        if (text.toLowerCase().startsWith("file://")) return true;
        if (text.matches("^[a-zA-Z]:/.*")) return true;
        return text.startsWith("/") || text.startsWith("./") || text.startsWith("../");
    }

    /** 字节数组 → 字节：接受 0-255 或 -128..127 的整数（JS 的 Uint8Array 与 Java 的 byte[] 都覆盖） */
    private static byte[] bytes(Iterable<?> values) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (Object value : values) {
            if (value instanceof JsonNode) value = ((JsonNode) value).asInt();
            if (!(value instanceof Number)) {
                throw new IllegalStateException("字节数组里出现了非数字元素：" + DPUtil.parseString(value));
            }
            int item = ((Number) value).intValue();
            if (item < -128 || item > 255) {
                throw new IllegalStateException("字节数组元素超出取值范围（-128 ~ 255）：" + item);
            }
            out.write(item & 0xFF);
        }
        byte[] bytes = out.toByteArray();
        if (bytes.length == 0) throw new IllegalStateException("文件内容为空");
        return bytes;
    }

    /** 字节 → data URL：按文件类型内联，供模型直接读取 */
    public static String dataUrl(Object mime, byte[] data) {
        String value = normalize(mime);
        return "data:" + (DPUtil.empty(value) ? "application/octet-stream" : value)
                + ";base64," + Base64.getEncoder().encodeToString(data);
    }

    /** 从 offset 起逐字节比较：长度不足直接判定不匹配 */
    private static boolean match(byte[] data, int offset, int... expected) {
        if (null == data || data.length < offset + expected.length) return false;
        for (int index = 0; index < expected.length; index++) {
            if ((data[offset + index] & 0xFF) != (expected[index] & 0xFF)) return false;
        }
        return true;
    }

}
