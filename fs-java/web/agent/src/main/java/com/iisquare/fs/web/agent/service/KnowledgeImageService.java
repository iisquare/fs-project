package com.iisquare.fs.web.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.base.web.util.RpcUtil;
import com.iisquare.fs.web.agent.dao.KnowledgeImageDao;
import com.iisquare.fs.web.agent.dao.KnowledgeDocumentDao;
import com.iisquare.fs.web.agent.entity.Knowledge;
import com.iisquare.fs.web.agent.entity.KnowledgeDocument;
import com.iisquare.fs.web.agent.entity.KnowledgeImage;
import com.iisquare.fs.web.agent.mvc.Configuration;
import com.iisquare.fs.web.agent.tool.ByteArrayMultipartFile;
import com.iisquare.fs.web.agent.tool.ParsedImage;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.core.rpc.FileRpc;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 知识库图片服务
 * 负责图片入库、权限判定与原图地址签发，图片本体存放于文件服务并由 /raw/ 输出：
 * 1. 文档解析出的图片由 attach 上传至文件服务并落库，正文回填 ![说明](kb:文件标识)
 * 2. 编辑时新插入的图片由 upload 上传并落库
 * 3. 展示时由 urls 按知识库授权角色批量签发带时效校验码的原图地址
 */
@Service
public class KnowledgeImageService extends JPAServiceBase {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeImageService.class);

    public static final String bucket = "fs-lm-knowledge";

    /** 图片引用：![说明](占位地址#kb:文件标识)，兼容仅带标识的写法 */
    private static final Pattern IMAGE_REF = Pattern.compile("!\\[([^\\]]*)\\]\\([^)]*kb:[^)\\s]+\\)");
    /** 解析阶段占位符 */
    private static final Pattern IMAGE_MARKER = Pattern.compile("\\{\\{kb-image:\\d+\\}\\}");

    private static final Set<String> RASTER_TYPES = new LinkedHashSet<>(Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/bmp", "image/webp"
    ));

    @Autowired
    KnowledgeImageDao imageDao;
    @Autowired
    KnowledgeDocumentDao documentDao;
    @Autowired
    KnowledgeService knowledgeService;
    @Autowired
    DefaultRbacService rbacService;
    @Autowired
    FileRpc fileRpc;
    @Autowired
    Configuration configuration;

    @Override
    public Map<String, String> sorts() {
        Map<String, String> sorts = new LinkedHashMap<>();
        sorts.put("sort", "asc");
        sorts.put("id", "desc");
        return sorts;
    }

    public KnowledgeImage info(String id) {
        if (DPUtil.empty(id)) return null;
        return info(imageDao, id);
    }

    /**
     * 按文档查询图片
     */
    public List<KnowledgeImage> listByDocument(List<Integer> documentIds) {
        if (null == documentIds || documentIds.isEmpty()) return Collections.emptyList();
        return imageDao.findAllByDocumentIdIn(documentIds);
    }

    /**
     * 按知识库查询图片
     */
    public List<KnowledgeImage> listByKnowledge(List<Integer> knowledgeIds) {
        if (null == knowledgeIds || knowledgeIds.isEmpty()) return Collections.emptyList();
        return imageDao.findAllByKnowledgeIdIn(knowledgeIds);
    }

    /**
     * 删除图片：对象存储中的原图与图片记录一并清理
     */
    public boolean remove(List<KnowledgeImage> images) {
        if (null == images || images.isEmpty()) return true;
        ArrayNode args = DPUtil.arrayNode();
        List<String> ids = new ArrayList<>();
        for (KnowledgeImage image : images) {
            ObjectNode item = args.addObject();
            item.put("id", image.getId());
            item.put("bucket", DPUtil.parseString(image.getBucket()));
            item.put("filepath", DPUtil.parseString(image.getFilepath()));
            ids.add(image.getId());
        }
        Map<String, Object> result = RpcUtil.result(fileRpc.post("/file/delete", args));
        if (ApiUtil.failed(result)) {
            logger.warn("remove knowledge image failed: {}", ApiUtil.message(result));
            return false;
        }
        remove(imageDao, ids);
        return true;
    }

    /**
     * 解析当前请求的登录用户身份
     */
    public JsonNode identity(HttpServletRequest request) {
        JsonNode identity = rbacService.identity(request);
        return null == identity ? DPUtil.objectNode() : identity;
    }

    /**
     * 知识库访问权限
     * 需要已登录，且命中知识库授权角色；知识库未配置授权角色时不限制角色
     */
    public boolean permit(Knowledge knowledge, JsonNode identity) {
        if (null == knowledge || 1 != knowledge.getStatus()) return false;
        if (null == identity || !identity.has("id")) return false;
        Set<Integer> permits = new LinkedHashSet<>(DPUtil.parseIntList(knowledge.getRoleIds()));
        if (permits.isEmpty()) return true;
        Set<Integer> owned = DPUtil.values(identity.at("/roles"), Integer.class, "id");
        for (Integer roleId : permits) {
            if (owned.contains(roleId)) return true;
        }
        return false;
    }

    /**
     * 批量签发原图访问地址
     * 仅返回有权访问且在用的图片，其余图片由调用方使用默认图片兜底
     */
    public Map<String, Object> urls(Integer knowledgeId, List<String> ids, Integer expire, JsonNode identity) {
        ObjectNode data = DPUtil.objectNode();
        if (null == ids || ids.isEmpty()) return ApiUtil.result(1001, "图片标识不能为空", null);
        Knowledge knowledge = knowledgeService.info(knowledgeId);
        if (null == knowledge || 1 != knowledge.getStatus()) {
            return ApiUtil.result(1002, "知识库不存在或已禁用", knowledgeId);
        }
        if (!permit(knowledge, identity)) {
            logger.info("knowledge image denied: knowledgeId={}, uid={}", knowledgeId, identity.at("/id").asInt());
            return ApiUtil.result(0, "无该知识库的查看权限", data);
        }
        List<String> distinct = new ArrayList<>(new LinkedHashSet<>(ids));
        List<String> granted = new ArrayList<>();
        Map<String, KnowledgeImage> images = DPUtil.list2map(imageDao.findAllById(distinct), String.class, "id");
        for (String id : distinct) {
            KnowledgeImage image = images.get(id);
            if (null == image || 1 != image.getStatus() || !knowledgeId.equals(image.getKnowledgeId())) continue;
            granted.add(id);
        }
        if (granted.isEmpty()) {
            logger.warn("knowledge image not found: knowledgeId={}, requested={}", knowledgeId, distinct.size());
            return ApiUtil.result(0, "未找到可授权的图片", data);
        }
        Map<String, Object> result = issue(granted, ttl(expire));
        if (ApiUtil.failed(result)) {
            logger.warn("issue knowledge image url failed: {}", ApiUtil.message(result));
            return result;
        }
        ObjectNode items = ApiUtil.data(result, ObjectNode.class);
        if (null == items) return ApiUtil.result(1502, "签发图片地址失败", null);
        for (String id : granted) {
            String url = items.at("/" + id + "/url").asText();
            if (DPUtil.empty(url)) continue;
            data.put(id, url);
        }
        return ApiUtil.result(0, null, data);
    }

    /**
     * 编辑时上传图片，返回文件标识与可直接展示的地址，正文引用格式为 ![说明](kb:文件标识)
     */
    public Map<String, Object> upload(Integer knowledgeId, Integer documentId, String alt, MultipartFile file, JsonNode identity) {
        if (null == documentId || documentId < 1) return ApiUtil.result(1001, "请选择图片所属文档", null);
        Knowledge knowledge = knowledgeService.info(knowledgeId);
        if (!permit(knowledge, identity)) return ApiUtil.result(9403, null, null);
        KnowledgeDocument document = info(documentDao, documentId);
        if (null == document || !knowledgeId.equals(document.getKnowledgeId())) {
            return ApiUtil.result(1002, "文档不存在或不属于该知识库", documentId);
        }
        if (null == file || file.isEmpty()) return ApiUtil.result(1001, "图片不能为空", null);
        String name = file.getOriginalFilename();
        String type = DPUtil.parseString(file.getContentType());
        if (DPUtil.empty(type)) type = typeOfName(name);
        if (!RASTER_TYPES.contains(type)) return ApiUtil.result(1002, "仅支持常见图片格式", type);
        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            return ApiUtil.result(1501, "读取图片失败", e.getMessage());
        }
        KnowledgeImage image = store(knowledgeId, documentId, 0, name, type, data, alt, 0, identity.at("/id").asInt());
        if (null == image) return ApiUtil.result(1503, "图片上传失败", null);
        Map<String, Object> result = issue(Collections.singletonList(image.getId()), ttl(null));
        if (ApiUtil.failed(result)) return result;
        ObjectNode items = ApiUtil.data(result, ObjectNode.class);
        ObjectNode item = DPUtil.objectNode();
        item.put("id", image.getId());
        item.put("name", DPUtil.parseString(image.getName()));
        item.put("suffix", DPUtil.parseString(image.getSuffix()));
        item.put("alt", DPUtil.parseString(image.getAlt()));
        item.put("url", null == items ? "" : items.at("/" + image.getId() + "/url").asText());
        return ApiUtil.result(0, null, item);
    }

    /**
     * 文档解析结果入库：图片上传至文件服务并落库，回填正文引用
     */
    public String attach(Integer knowledgeId, Integer documentId, List<ParsedImage> images, String markdown, int uid) {
        String result = null == markdown ? "" : markdown;
        if (null == images || images.isEmpty()) return result;
        List<KnowledgeImage> pending = new ArrayList<>();
        for (int i = 0; i < images.size(); i++) {
            ParsedImage parsed = images.get(i);
            KnowledgeImage image = assemble(knowledgeId, documentId, i, parsed.getName(), parsed.getType(),
                    parsed.getData(), parsed.getAlt(), parsed.getPage(), uid);
            String reference = "";
            if (null != image) {
                pending.add(image);
                reference = String.format("![%s](%s#kb:%s)", DPUtil.parseString(image.getAlt()),
                        configuration.getImagePlaceholder(), image.getId());
            }
            result = result.replace("{{kb-image:" + i + "}}", reference);
        }
        if (!pending.isEmpty()) imageDao.saveAll(pending);
        return result;
    }

    /**
     * 去掉图片引用与占位符后的纯文本，用于词嵌入与全文检索
     */
    public static String plain(String content) {
        if (null == content) return null;
        Matcher matcher = IMAGE_REF.matcher(content);
        String result = matcher.replaceAll("$1");
        return IMAGE_MARKER.matcher(result).replaceAll("").strip();
    }

    private KnowledgeImage store(Integer knowledgeId, Integer documentId, int sort, String name,
                                 String type, byte[] data, String alt, Integer page, int uid) {
        KnowledgeImage image = assemble(knowledgeId, documentId, sort, name, type, data, alt, page, uid);
        return null == image ? null : imageDao.save(image);
    }

    /**
     * 上传图片至文件服务并组装图片记录（不落库），失败返回 null
     */
    private KnowledgeImage assemble(Integer knowledgeId, Integer documentId, int sort, String name,
                                    String type, byte[] data, String alt, Integer page, int uid) {
        if (null == data || data.length == 0) return null;
        String suffix = suffixOfName(name, type);
        if (suffix.isEmpty()) return null;
        String date = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String filepath = String.format("knowledge-%d/image/%s/%s%s", knowledgeId, date,
                UUID.randomUUID().toString().replace("-", ""), suffix);
        String traceIdentity = String.format("fs-lm-knowledge-%d-document-%s-image", knowledgeId, documentId);
        Map<String, Object> result = RpcUtil.result(fileRpc.form("/file/upload", DPUtil.buildMap(
                "bucket", bucket, "filepath", filepath, "traceIdentity", traceIdentity
        ), new ByteArrayMultipartFile("file", name, type, data)));
        if (ApiUtil.failed(result)) {
            logger.warn("upload knowledge image failed: {} - {}", filepath, ApiUtil.message(result));
            return null;
        }
        JsonNode item = ApiUtil.data(result, ObjectNode.class);
        if (null == item || DPUtil.empty(item.at("/id").asText())) return null;
        long time = System.currentTimeMillis();
        KnowledgeImage image = KnowledgeImage.builder()
                .id(item.at("/id").asText())
                .knowledgeId(knowledgeId)
                .documentId(null == documentId ? 0 : documentId)
                .bucket(item.at("/bucket").asText())
                .filepath(item.at("/filepath").asText())
                .name(DPUtil.empty(name) ? item.at("/name").asText() : name)
                .suffix(suffix)
                .type(type)
                .size((long) data.length)
                .alt(DPUtil.empty(alt) ? ("图片" + (sort + 1)) : alt)
                .page(null == page ? 0 : page)
                .sort(sort)
                .status(1)
                .createdTime(time)
                .createdUid(uid)
                .updatedTime(time)
                .updatedUid(uid)
                .build();
        return image;
    }

    /**
     * 向文件服务换取带时效校验码的原图地址
     * 返回文件服务原始结果，失败由调用方向上透传
     */
    private Map<String, Object> issue(List<String> ids, int ttl) {
        ObjectNode param = DPUtil.objectNode();
        for (String id : ids) {
            param.putObject(id).put("type", "raw").put("expire", ttl);
        }
        return RpcUtil.result(fileRpc.post("/file/url", param));
    }

    private int ttl(Integer expire) {
        return ValidateUtil.filterInteger(expire, true, 60000, 86400000, configuration.getImageExpire());
    }

    private static String suffixOfName(String name, String type) {
        if (null != name) {
            int index = name.lastIndexOf(".");
            if (index > -1) {
                String suffix = name.substring(index).toLowerCase();
                if (suffix.matches("^\\.[a-z0-9]+$")) return suffix;
            }
        }
        switch (type) {
            case "image/jpeg":
                return ".jpg";
            case "image/png":
                return ".png";
            case "image/gif":
                return ".gif";
            case "image/bmp":
                return ".bmp";
            case "image/webp":
                return ".webp";
            default:
                return "";
        }
    }

    private static String typeOfName(String name) {
        if (null == name) return "";
        int index = name.lastIndexOf(".");
        if (index < 0) return "";
        switch (name.substring(index).toLowerCase()) {
            case ".jpg":
            case ".jpeg":
                return "image/jpeg";
            case ".png":
                return "image/png";
            case ".gif":
                return "image/gif";
            case ".bmp":
                return "image/bmp";
            case ".webp":
                return "image/webp";
            default:
                return "";
        }
    }

}
