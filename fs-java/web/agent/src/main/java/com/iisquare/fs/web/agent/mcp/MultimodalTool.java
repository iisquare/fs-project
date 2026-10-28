package com.iisquare.fs.web.agent.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.agent.ai.ToolParam;
import com.iisquare.fs.web.agent.service.AIService;
import com.iisquare.fs.web.agent.tool.FileFetcher;
import com.iisquare.fs.web.agent.tool.FileUploader;
import com.iisquare.fs.web.core.rpc.FileRpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 多模态 (multimodal) 服务工具：把图片 / 视频 / 音频按原文件交给多模态模型分析，返回文本结果。
 *
 * 调用口径与对话调用一致（对照 {@link com.iisquare.fs.web.agent.service.BotService}）：
 * - 密钥：服务级密钥 {@code fs.agent.token}（Authorization: Bearer），与其它模型网关调用共用；
 * - 会话：调用端必须带登录会话（{@code x-auth-token} 或 Authorization: Bearer），
 *   网关据此把这一轮用量记到该用户名下；没有会话或会话无效时工具直接报错；
 * - 报文：OpenAI 兼容的 {@code /v1/chat/completions}，content 为内容块数组
 *   （{@code image_url} / {@code video_url} / {@code audio_url}）。
 *
 * 模型名称由调用方自行指定（方法参数 model），留空用服务配置 {@code fs.agent.multimodal.model} 的默认值。
 * 文件三种给法（都不接受本地路径 —— 服务端读不到调用方的本地磁盘）：
 * - {@code {id}}：文件服务标识，服务端取回原文件后内联（与 {@code AgenticRuntime} 的多模态节点同一做法）；
 * - {@code {data}}：base64 内容（可带 data: 前缀），服务端按类型补成 data URL；
 * - {@code {url}}：可直接访问的地址（http/https/data），原样放进内容块、由上游自行获取。
 * 类型优先取调用方给的 type（contentType），其次文件名后缀，最后按内容魔数嗅探。
 */
@Service
public class MultimodalTool {

    private static final Logger logger = LoggerFactory.getLogger(MultimodalTool.class);

    /** 支持的媒体类型：内容块的类型名即「类型 + _url」 */
    private static final List<String> MEDIA_KINDS = Arrays.asList("image", "video", "audio");
    /** 调用方没给分析要求时的默认提示词 */
    private static final String DEFAULT_PROMPT = "请描述该文件的内容，包含其中的关键信息。";

    @Autowired
    AIService aiService;
    @Autowired
    ToolSessionSupport toolSessionSupport;
    @Autowired
    FileRpc fileRpc;
    /** 默认多模态模型：调用方没在参数里给出 model 时用它 */
    @Value("${fs.agent.multimodal.model:deepseek-flash}")
    private String defaultModel;
    /**
     * 单个文件的字节上限：原文件以 base64 整块进请求体（base64 约为原文件的 4/3），
     * 放得太宽只会把请求撑爆，超限不读、直接把原因告诉调用方
     */
    @Value("${fs.agent.multimodal.maxSize:20971520}")
    private long maxSize;

    @Tool(name = "multimodalAnalyze",
            description = "把图片 / 视频 / 音频按原文件交给多模态模型分析，返回模型给出的文本结果。"
                    + "当需要看图（识图、OCR、画面描述）、看视频或听音频时使用；"
                    + "模型名称可自行指定，留空用服务配置的默认多模态模型。"
                    + "files 只接受文件内容或引用：data（base64 / 字节数组）、url（可访问地址）、id（文件服务标识），"
                    + "服务端统一转成 base64 交给模型；"
                    + "不接受本机路径（如 C:/Users/xx/a.png、file:///C:/...）—— 服务端读不到调用方的磁盘。")
    public String multimodalAnalyze(
            @ToolParam(name = "files", description = "待分析的文件列表。每项用下面四种之一，"
                    + "不要传本机路径（C:/Users/xx/a.png、C:\\Users\\xx\\a.png、file:///C:/... 都会被拒绝）："
                    + "① {data, name, type}：data 为文件内容，base64 文本（可带 data: 前缀）或字节数组 [137,80,...]；"
                    + "② {url, name, type}：可直接访问的地址（http/https/data）；"
                    + "③ {id, name, type}：文件服务标识（服务端取回原文件后内联）；"
                    + "④ 数组元素直接是字符串（data URL / base64 / http 地址）或字节数组。"
                    + "name 为文件名，type 为 contentType（如 image/png，也可写 image / video / audio）；"
                    + "缺省时服务端按文件名后缀与内容嗅探判断。"
                    + "调用方有本地文件时：读成 base64 / 字节数组放进 data，或先把文件传到文件服务再按 id 引用"
                    + "（本机文件由调用方自己上传，服务端不读调用方的磁盘）")
            List<Object> files,
            @ToolParam(name = "prompt", description = "分析要求（提示词），例如：描述图片内容 / 提取画面中的所有文字",
                    required = false)
            String prompt,
            @ToolParam(name = "model", description = "多模态模型名称，留空用服务配置的默认模型",
                    required = false)
            String model,
            @ToolParam(name = "system", description = "系统提示词，留空则不发送", required = false)
            String system) {
        String session = toolSessionSupport.requireSession();
        if (null == files || files.isEmpty()) throw new IllegalStateException("待分析的文件不能为空");
        String current = DPUtil.empty(model) ? defaultModel : model;
        if (DPUtil.empty(current)) {
            throw new IllegalStateException(
                    "未指定多模态模型：请在方法参数 model 里给出，或配置 fs.agent.multimodal.model");
        }
        ArrayNode content = DPUtil.arrayNode();
        content.addObject().put("type", "text").put("text", DPUtil.empty(prompt) ? DEFAULT_PROMPT : prompt);
        for (Object item : files) content.add(mediaPart(fileItem(item)));
        ObjectNode body = DPUtil.objectNode();
        body.put("model", current);
        body.put("stream", false);
        ArrayNode messages = body.putArray("messages");
        if (!DPUtil.empty(system)) messages.addObject().put("role", "system").put("content", system);
        ObjectNode user = messages.addObject();
        user.put("role", "user");
        user.set("content", content);
        Map<String, Object> result = aiService.chat(body, session);
        if (ApiUtil.failed(result)) {
            throw new IllegalStateException("多模态模型调用失败：" + DPUtil.parseString(result.get("message"))
                    + " " + DPUtil.stringify(result.get("data")));
        }
        JsonNode payload = ApiUtil.data(result, JsonNode.class);
        if (null == payload) throw new IllegalStateException("多模态模型未返回内容");
        if (payload.has("error")) {
            throw new IllegalStateException("多模态模型调用失败："
                    + payload.at("/error/message").asText(DPUtil.stringify(payload.at("/error"))));
        }
        String answer = payload.at("/choices/0/message/content").asText("");
        if (DPUtil.empty(answer)) throw new IllegalStateException("多模态模型未返回内容：" + DPUtil.stringify(payload));
        JsonNode usage = payload.at("/usage");
        logger.info("Multimodal analyze: model={} files={} promptTokens={} completionTokens={}",
                current, files.size(), usage.at("/prompt_tokens").asInt(0), usage.at("/completion_tokens").asInt(0));
        return answer;
    }

    /**
     * 单个文件的内容块：地址按可访问地址或 data URL 给出，类型按 image / video / audio 拼成
     * {@code <类型>_url}（图片 image_url、视频 video_url、音频 audio_url）。
     */
    protected ObjectNode mediaPart(Map<String, Object> file) {
        String id = DPUtil.parseString(file.get("id"));
        String url = DPUtil.parseString(file.get("url"));
        Object content = content(file);
        String name = DPUtil.parseString(file.get("name"));
        String media = media(file);
        String address = "";
        if (!DPUtil.empty(url)) {
            // 可访问地址：http(s) 原样交给上游获取，data: 前缀的本身就是内联内容
            if (!url.startsWith("http") && !url.startsWith("data:")) {
                throw new IllegalStateException("文件地址只支持 http/https/data：" + cut(url)
                        + "（这是本机路径吗？服务端读不到调用方的磁盘：如果你能读到文件内容，请用 data 传 base64 或字节数组；"
                        + "如果文件已在文件服务里，请用 id 引用；也可以先把文件传到文件服务再按 id 引用）");
            }
            address = url;
            if (address.startsWith("data:")) {
                // data URL 自带 MIME（data:image/png;base64,...）：直接拿来判断媒体类型
                if (DPUtil.empty(media)) media = dataMime(address);
            } else if (DPUtil.empty(name)) {
                name = FileFetcher.filename(address);
            }
        } else {
            byte[] bytes;
            if (!DPUtil.empty(content)) {
                // 二进制：调用方直接给 base64（可带 data: 前缀），服务端按文件类型补成 data URL
                bytes = FileUploader.decode(content);
            } else if (!DPUtil.empty(id)) {
                // 文件服务标识：取回原文件后内联，与编排里的多模态节点同一做法
                String download;
                try {
                    download = FileFetcher.url(fileRpc, id, name);
                } catch (Exception e) {
                    throw new IllegalStateException(e.getMessage()
                            + "（id 需是文件服务里未删除且启用的文件标识："
                            + "可先把文件传到文件服务拿到 id，或从文件管理页取）");
                }
                if (DPUtil.empty(name)) name = FileFetcher.filename(download);
                bytes = bytes(download, name);
            } else {
                throw new IllegalStateException("文件信息缺少 id / url / data：" + DPUtil.stringify(file)
                        + "（三种给法选一种：data base64 内容、url 可访问地址、id 文件服务标识）");
            }
            if (bytes.length > maxSize) {
                throw new IllegalStateException("文件超过直接分析上限（" + (maxSize / 1024 / 1024) + "MB）：" + name);
            }
            if (DPUtil.empty(media)) {
                media = FileUploader.mime("", name, bytes);
            } else if (media.indexOf('/') < 0) {
                // 只给了裸类型（如 image / video / audio，MCP 内容块就是这么写的）：按后缀与内容补出具体 MIME
                String exact = FileUploader.mime("", name, bytes);
                if (!DPUtil.empty(exact)) media = exact;
            }
            address = FileUploader.dataUrl(media, bytes);
        }
        if (DPUtil.empty(media)) media = FileUploader.mime("", name, null);
        String kind = kind(media, name);
        ObjectNode part = DPUtil.objectNode();
        part.put("type", kind + "_url");
        part.putObject(kind + "_url").put("url", address);
        return part;
    }

    /**
     * 数组元素 → 文件信息：对象原样使用（data / url / id）；
     * 字符串按内容识别（data URL、http(s) 地址、base64）；
     * 数字数组当作字节数组（等价于 {data: [...]}），方便调用方直接把二进制内容作为数组元素传入
     */
    protected Map<String, Object> fileItem(Object item) {
        Map<String, Object> file = new LinkedHashMap<>();
        if (item instanceof Map) {
            ((Map<?, ?>) item).forEach((key, value) -> file.put(DPUtil.parseString(key), value));
            return file;
        }
        if (item instanceof Collection) {
            file.put("data", item);
            return file;
        }
        String value = DPUtil.trim(DPUtil.parseString(item));
        if (DPUtil.empty(value)) throw new IllegalStateException("文件信息为空");
        if (value.startsWith("http") || value.startsWith("data:")) {
            file.put("url", value);
        } else if (base64(value)) {
            file.put("data", value);
        } else {
            throw new IllegalStateException("文件信息无法识别：" + cut(value)
                    + "（数组元素可以是 {data} / {url} / {id} 对象，也可以是 data URL、base64、http 地址字符串"
                    + "或字节数组；本机路径读不到，请改传文件内容或用文件服务 id）");
        }
        return file;
    }

    /** 粗判是否 base64：去掉空白后只有 base64 字符集且长度足够，避免把随便一段文本当成文件内容 */
    protected boolean base64(String value) {
        String text = value.replaceAll("\\s", "");
        return text.length() >= 32 && text.matches("^[A-Za-z0-9+/]+={0,2}$");
    }

    /** data URL 里的 MIME：data:image/png;base64,xxx → image/png */
    protected String dataMime(String address) {
        int index = address.indexOf(';');
        String value = address.substring("data:".length(), index < 0 ? address.length() : index);
        return FileUploader.normalize(value);
    }

    /** 错误文案里的值截断：内容可能很长（base64），只展示开头便于定位 */
    protected String cut(String value) {
        String text = DPUtil.parseString(value);
        return text.length() <= 60 ? text : text.substring(0, 60) + "...";
    }

    /**
     * 二进制内容：data / base64 / content / bytes 四个键名都接受，
     * 值可以是 base64 文本，也可以是字节数组（JSON 数组，如 [137,80,78,71,...]）
     */
    protected Object content(Map<String, Object> file) {
        for (String key : new String[]{"data", "base64", "content", "bytes"}) {
            Object value = file.get(key);
            if (!DPUtil.empty(value)) return value;
        }
        return null;
    }

    /**
     * 媒体类型：type / mimeType / mediaType 三个键名都接受
     * （调用方常按 MCP 内容块的写法给出 {type: image, mimeType: image/png, data: ...}）
     */
    protected String media(Map<String, Object> file) {
        for (String key : new String[]{"type", "mimeType", "mediaType"}) {
            String value = FileUploader.normalize(file.get(key));
            if (!DPUtil.empty(value)) return value;
        }
        return "";
    }

    /**
     * 媒体类型：优先看 contentType（形如 image/png，也接受 image / video / audio 这类裸类型），
     * 再按文件名后缀兜底；两者都判断不出来就报错，由调用方补上 type
     */
    protected String kind(String media, String name) {
        String value = DPUtil.empty(media) ? "" : media.toLowerCase();
        for (String kind : MEDIA_KINDS) {
            if (value.startsWith(kind)) return kind;
        }
        throw new IllegalStateException("暂不支持的文件类型（支持图片 / 视频 / 音频）："
                + (DPUtil.empty(name) ? media : name) + "，请在文件信息里给出 type（如 image/png）");
    }

    /** 按下载地址取回原文件字节：失败原因带上文件名，便于调用方定位是哪个文件 */
    protected byte[] bytes(String download, String name) {
        try {
            return FileFetcher.bytes(download, name);
        } catch (Exception e) {
            throw new IllegalStateException("下载文件失败：" + name + "（" + e.getMessage() + "）");
        }
    }

}
