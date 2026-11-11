package com.iisquare.fs.web.bi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.JarClassLoader;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.core.*;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.base.web.util.RpcUtil;
import com.iisquare.fs.web.bi.dao.JarDao;
import com.iisquare.fs.web.bi.entity.Jar;
import com.iisquare.fs.web.core.file.StreamFileUploader;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.core.rpc.FileRpc;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.criteria.Predicate;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 自定义计算节点 JAR 目录。
 *
 * 文件本体写入文件服务，扫描结果写入 BI 元数据表。运行时由 ScriptTransformNode
 * 通过 BI 下载地址取得文件服务签名链接并加载插件。
 */
@Service
public class JarCatalogService extends JPAServiceBase {

    private static final Logger log = LoggerFactory.getLogger(JarCatalogService.class);
    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    @Autowired
    JarDao jarDao;
    @Autowired
    DefaultRbacService rbacService;
    @Autowired
    FileRpc fileRpc;
    @Autowired
    StreamFileUploader streamFileUploader;

    @Value("${fs.bi.jar.bucket:fs-bi}")
    String bucket;
    @Value("${fs.bi.jar.public-base-url:http://127.0.0.1:7815}")
    String publicBaseUrl;
    @Value("${fs.bi.jar.max-size:1073741824}")
    long maxSize;

    @Override
    public Map<String, String> sorts() {
        Map<String, String> sorts = new LinkedHashMap<>();
        sorts.put("id", "desc");
        sorts.put("createdTime", "desc");
        sorts.put("name", "asc");
        sorts.put("size", "desc");
        return sorts;
    }

    /**
     * 上传前先解析 JAR，再写入文件服务与元数据表。
     */
    public Map<String, Object> upload(MultipartFile file, HttpServletRequest request) {
        if (null == file || file.isEmpty()) return ApiUtil.result(1001, "请选择 JAR 文件", null);
        String original = Optional.ofNullable(file.getOriginalFilename()).orElse("plugin.jar").trim();
        if (!original.toLowerCase(Locale.ROOT).endsWith(".jar")) {
            return ApiUtil.result(1002, "仅支持 .jar 文件", original);
        }
        if (maxSize > 0 && file.getSize() > maxSize) {
            return ApiUtil.result(1003, "JAR 文件超过大小限制", maxSize);
        }

        Path temp = null;
        String uploadedFileId = null;
        String filepath = null;
        try {
            temp = Files.createTempFile("fs-bi-jar-", ".jar");
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, temp, StandardCopyOption.REPLACE_EXISTING);
            }
            String sha256 = sha256(temp);
            List<ObjectNode> nodes = scan(temp);
            String date = LocalDate.now().format(DATE_PATH);
            filepath = String.format("jars/%s/%s.jar", date,
                    UUID.randomUUID().toString().replace("-", ""));

            Map<String, Object> uploaded = streamFileUploader.upload("/file/upload", DPUtil.buildMap(
                    "bucket", bucket,
                    "filepath", filepath,
                    "traceIdentity", "fs-bi-jar-" + sha256
            ), file, original, "application/java-archive");
            if (ApiUtil.failed(uploaded)) return uploaded;
            JsonNode fileInfo = RpcUtil.data(uploaded, false);
            if (null == fileInfo || !fileInfo.isObject()) {
                cleanupFile(bucket, filepath);
                return ApiUtil.result(1500, "文件服务未返回文件信息", null);
            }
            uploadedFileId = fileInfo.at("/id").asText("");
            if (DPUtil.empty(uploadedFileId)) {
                cleanupFile(bucket, filepath);
                uploadedFileId = null;
                return ApiUtil.result(1500, "文件服务未返回文件标识", fileInfo);
            }

            long time = System.currentTimeMillis();
            int uid = null == request ? 0 : rbacService.uid(request);
            Jar info = Jar.builder()
                    .fileId(uploadedFileId)
                    .name(original)
                    .bucket(bucket)
                    .filepath(filepath)
                    .sha256(sha256)
                    .size(file.getSize())
                    .nodes(DPUtil.stringify(nodes))
                    .status(1)
                    .createdTime(time)
                    .createdUid(uid)
                    .updatedTime(time)
                    .updatedUid(uid)
                    .build();
            info = jarDao.save(info);
            return ApiUtil.result(0, null, format(info));
        } catch (Exception e) {
            if (!DPUtil.empty(uploadedFileId)) cleanupFile(bucket, filepath);
            log.warn("upload custom jar failed", e);
            return ApiUtil.result(1500, "上传 JAR 失败", e.getMessage());
        } finally {
            if (null != temp) {
                try {
                    Files.deleteIfExists(temp);
                } catch (Exception ignored) {
                }
            }
        }
    }

    public ObjectNode search(Map<String, Object> param) {
        ObjectNode result = search(jarDao, param, (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), 1));
            String keyword = DPUtil.trim(DPUtil.parseString(param.get("keyword")));
            if (!DPUtil.empty(keyword)) {
                String like = "%" + keyword + "%";
                predicates.add(cb.or(
                        cb.like(root.get("name"), like),
                        cb.like(root.get("sha256"), like),
                        cb.like(root.get("nodes"), like)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, Sort.by(Sort.Order.desc("id")), sorts().keySet());
        format(ApiUtil.rows(result));
        return result;
    }

    public ObjectNode detail(String id) {
        Jar info = info(id);
        return null == info || !Integer.valueOf(1).equals(info.getStatus()) ? null : format(info);
    }

    public JsonNode nodes(String id) {
        ObjectNode info = detail(id);
        return null == info ? null : info.get("nodes");
    }

    public Map<String, Object> delete(String id) {
        Jar info = info(id);
        if (null == info || !Integer.valueOf(1).equals(info.getStatus())) {
            return ApiUtil.result(1004, "JAR 文件不存在", id);
        }
        ArrayNode args = DPUtil.arrayNode();
        ObjectNode item = args.addObject();
        item.put("id", info.getFileId());
        item.put("bucket", info.getBucket());
        item.put("filepath", info.getFilepath());
        Map<String, Object> result;
        try {
            result = RpcUtil.result(fileRpc.post("/file/delete", args));
        } catch (Exception e) {
            return ApiUtil.result(1500, "调用文件服务删除失败", e.getMessage());
        }
        if (ApiUtil.failed(result)) return result;

        try {
            info.setStatus(-1);
            info.setUpdatedTime(System.currentTimeMillis());
            jarDao.save(info);
            return ApiUtil.result(0, null, info.getId());
        } catch (Exception e) {
            return ApiUtil.result(1500, "更新 JAR 元数据失败", e.getMessage());
        }
    }

    /**
     * 返回可用于计算引擎下载的稳定 BI 地址。
     */
    public String uri(Integer id) {
        String base = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
        return base + "/diagram/jar/download?id=" + id;
    }

    /**
     * 实时向文件服务换取签名下载地址，避免把过期 URL 固化到流程中。
     */
    public String downloadUrl(String id) {
        Jar info = info(id);
        if (null == info || !Integer.valueOf(1).equals(info.getStatus()) || DPUtil.empty(info.getFileId())) {
            return null;
        }
        JsonNode response = RpcUtil.json(fileRpc.get("/file/download", DPUtil.buildMap("id", info.getFileId())));
        if (null == response || response.at("/code").asInt(-1) != 0) return null;
        return response.at("/data").asText("");
    }

    public Jar info(String id) {
        return info(DPUtil.parseInt(id));
    }

    public Jar info(Integer id) {
        return info(jarDao, id);
    }

    private ObjectNode format(Jar info) {
        ObjectNode item = DPUtil.objectNode();
        item.put("id", info.getId());
        item.put("fileId", info.getFileId());
        item.put("name", info.getName());
        item.put("bucket", info.getBucket());
        item.put("filepath", info.getFilepath());
        item.put("sha256", info.getSha256());
        item.put("shortHash", DPUtil.empty(info.getSha256()) ? "" : info.getSha256().substring(0, 12));
        item.put("size", info.getSize());
        item.put("createdTime", info.getCreatedTime());
        item.put("uploadedTime", info.getCreatedTime());
        item.put("uri", uri(info.getId()));
        JsonNode nodes = DPUtil.parseJSON(info.getNodes());
        if (null == nodes || !nodes.isArray()) nodes = DPUtil.arrayNode();
        item.set("nodes", nodes);
        item.put("nodeCount", nodes.size());
        return item;
    }

    private JsonNode format(JsonNode rows) {
        if (null == rows) return DPUtil.arrayNode();
        for (JsonNode row : rows) {
            ObjectNode item = (ObjectNode) row;
            int id = item.at("/id").asInt();
            String nodesText = item.at("/nodes").asText("");
            JsonNode nodes = DPUtil.parseJSON(nodesText);
            if (null == nodes || !nodes.isArray()) nodes = DPUtil.arrayNode();
            item.set("nodes", nodes);
            item.put("nodeCount", nodes.size());
            String sha256 = item.at("/sha256").asText("");
            item.put("shortHash", DPUtil.empty(sha256) ? ""
                    : sha256.substring(0, Math.min(12, sha256.length())));
            item.put("uploadedTime", item.at("/createdTime").asLong());
            item.put("uri", uri(id));
        }
        return rows;
    }

    private void cleanupFile(String bucket, String filepath) {
        if (DPUtil.empty(bucket) || DPUtil.empty(filepath)) return;
        try {
            ArrayNode args = DPUtil.arrayNode();
            args.addObject().put("bucket", bucket).put("filepath", filepath);
            fileRpc.post("/file/delete", args);
        } catch (Exception e) {
            log.warn("cleanup uploaded jar failed: bucket={}, filepath={}", bucket, filepath, e);
        }
    }

    private String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = input.read(buffer)) >= 0) {
                if (length > 0) digest.update(buffer, 0, length);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private List<ObjectNode> scan(Path path) throws Exception {
        List<ObjectNode> result = new ArrayList<>();
        try (JarFile jar = new JarFile(path.toFile());
             JarClassLoader loader = JarClassLoader.fromUrl(path.toUri().toURL(), getClass().getClassLoader())) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory() || !name.endsWith(".class") || name.contains("$")) continue;
                String className = name.substring(0, name.length() - 6).replace('/', '.');
                try {
                    Class<?> cls = Class.forName(className, false, loader);
                    if (!DAGNode.class.isAssignableFrom(cls) || Modifier.isAbstract(cls.getModifiers())) continue;
                    ObjectNode node = DPUtil.objectNode();
                    node.put("className", className);
                    String simpleName = cls.getSimpleName();
                    node.put("name", simpleName.endsWith("Node")
                            ? simpleName.substring(0, simpleName.length() - 4) : simpleName);
                    node.put("kind", kind(cls));
                    node.put("inputPorts", inputPorts(cls));
                    node.put("outputPorts", outputPorts(cls));
                    node.put("description", cls.getName());
                    result.add(node);
                } catch (Throwable e) {
                    // 跳过依赖不完整或非节点类，单个坏类不影响整个 JAR 的节点发现。
                }
            }
        }
        result.sort(Comparator.comparing(node -> node.at("/name").asText()));
        return result;
    }

    private String kind(Class<?> cls) {
        if (DAGSource.class.isAssignableFrom(cls)) return "source";
        if (DAGTransform.class.isAssignableFrom(cls)) return "transform";
        if (DAGSink.class.isAssignableFrom(cls)) return "sink";
        if (DAGConfig.class.isAssignableFrom(cls)) return "config";
        return "node";
    }

    private int inputPorts(Class<?> cls) {
        if (DAGSource.class.isAssignableFrom(cls) || DAGConfig.class.isAssignableFrom(cls)) return 0;
        return 1;
    }

    private int outputPorts(Class<?> cls) {
        if (DAGSink.class.isAssignableFrom(cls) || DAGConfig.class.isAssignableFrom(cls)) return 0;
        return 1;
    }

}
