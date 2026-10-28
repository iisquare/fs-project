package com.iisquare.fs.web.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.base.web.util.RpcUtil;
import com.iisquare.fs.web.agent.dao.ClientVersionDao;
import com.iisquare.fs.web.agent.dao.ClientUpdateLogDao;
import com.iisquare.fs.web.agent.entity.ClientVersion;
import com.iisquare.fs.web.agent.entity.ClientUpdateLog;
import com.iisquare.fs.web.agent.mvc.Configuration;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.core.rpc.FileRpc;
import com.iisquare.fs.web.core.file.StreamFileUploader;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * 客户端安装包版本管理，以及桌面端启动时的版本检查。
 */
@Service
public class ClientVersionService extends JPAServiceBase {

    private static final Pattern VERSION = Pattern.compile("^\\d+(\\.\\d+){0,3}([-+][0-9A-Za-z.-]+)?$");
    private static final int MAX_RELEASE_NOTES = 65535;
    private static final long POLICY_CACHE_MILLIS = 30000;
    private volatile List<ClientVersion> policyCache;
    private volatile long policyCacheTime;

    @Autowired
    ClientVersionDao clientVersionDao;
    @Autowired
    ClientUpdateLogDao clientUpdateLogDao;
    @Autowired
    DefaultRbacService rbacService;
    @Autowired
    Configuration configuration;
    @Autowired
    FileRpc fileRpc;
    @Autowired
    StreamFileUploader streamFileUploader;

    /** 安装包统一放文件服务；默认复用客户端已有权限的桶，可用配置改为专用桶 */
    @Value("${fs.agent.bot.version.bucket:fs-mcp}")
    private String bucket;
    @Value("${fs.agent.bot.version.maxSize:1073741824}")
    private long maxSize;

    @Override
    public Map<String, String> sorts() {
        Map<String, String> sorts = new LinkedHashMap<>();
        sorts.put("id", "desc");
        sorts.put("platform", "asc");
        sorts.put("architecture", "asc");
        sorts.put("status", "asc");
        return sorts;
    }

    public Map<String, String> platforms() {
        Map<String, String> platforms = new LinkedHashMap<>();
        platforms.put("win32", "Windows");
        platforms.put("darwin", "macOS");
        platforms.put("linux", "Linux");
        return platforms;
    }

    public Map<String, String> architectures() {
        Map<String, String> architectures = new LinkedHashMap<>();
        architectures.put("x64", "x64");
        architectures.put("arm64", "ARM64");
        architectures.put("ia32", "x86 32位");
        architectures.put("armv7l", "ARM 32位");
        architectures.put("universal", "通用");
        return architectures;
    }

    public Map<String, String> channels() {
        Map<String, String> channels = new LinkedHashMap<>();
        channels.put("stable", "稳定版");
        channels.put("beta", "测试版");
        return channels;
    }

    public Map<Integer, String> status() {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "启用");
        status.put(2, "停用");
        return status;
    }

    public ClientVersion info(Integer id) {
        return info(clientVersionDao, id);
    }

    public Map<String, Object> detail(Integer id) {
        ClientVersion info = info(id);
        if (null == info) return ApiUtil.result(1404, "版本不存在", id);
        ObjectNode node = DPUtil.toJSON(info, ObjectNode.class);
        stripDeltaRow(node);
        return ApiUtil.result(0, null, node);
    }

    @Transactional
    public Map<String, Object> upload(MultipartFile file, Map<?, ?> param, HttpServletRequest request) {
        if (null == file || file.isEmpty()) return ApiUtil.result(1001, "获取文件句柄失败", null);
        if (file.getSize() > maxSize) return ApiUtil.result(1001, "安装包超过大小上限", file.getSize());
        String platform = normalizePlatform(param.get("platform"));
        if (DPUtil.empty(platform)) return ApiUtil.result(1002, "请选择客户端系统", platform);
        String architecture = normalizeArchitecture(param.get("architecture"));
        if (DPUtil.empty(architecture)) return ApiUtil.result(1002, "请选择客户端架构", architecture);
        String channel = normalizeChannel(param.get("channel"));
        if (DPUtil.empty(channel)) return ApiUtil.result(1002, "请选择发布渠道", channel);
        String versionName = normalizeVersion(param.get("version"));
        if (!isVersion(versionName)) return ApiUtil.result(1003, "版本号格式异常", versionName);
        String minimumVersion = normalizeVersion(param.get("minimumVersion"));
        if (!isVersion(minimumVersion)) return ApiUtil.result(1003, "最低支持版本格式异常", minimumVersion);
        if (compareVersion(minimumVersion, versionName) > 0) {
            return ApiUtil.result(1004, "最低支持版本不能高于安装包版本", minimumVersion);
        }
        String releaseNotes = DPUtil.parseString(param.get("releaseNotes"));
        if (releaseNotes.getBytes(StandardCharsets.UTF_8).length > MAX_RELEASE_NOTES) {
            return ApiUtil.result(1005, "发布日志过长", releaseNotes.length());
        }
        int status = DPUtil.parseInt(param.get("status"));
        if (!status().containsKey(status)) return ApiUtil.result(1006, "状态异常", status);
        int rolloutPercentage = null == param.get("rolloutPercentage")
                ? 100 : DPUtil.parseInt(param.get("rolloutPercentage"));
        if (rolloutPercentage < 0 || rolloutPercentage > 100) {
            return ApiUtil.result(1006, "灰度比例必须在 0 到 100 之间", rolloutPercentage);
        }
        int allowRollback = parseBoolean(param.get("allowRollback")) ? 1 : 0;

        String fileName = fileName(file);
        if (DPUtil.empty(fileName)) return ApiUtil.result(1007, "文件名异常", fileName);
        String suffix = suffix(fileName);
        if (!installerAllowed(platform, suffix, fileName)) {
            return ApiUtil.result(1008, "安装包格式与客户端系统不匹配", suffix);
        }
        long duplicates = clientVersionDao.count((root, query, cb) -> cb.and(
                cb.equal(root.get("platform"), platform),
                cb.equal(root.get("architecture"), architecture),
                cb.equal(root.get("version"), versionName)
        ));
        if (duplicates > 0) {
            return ApiUtil.result(1009, "该系统、架构和版本号的安装包已存在", versionName);
        }
        FileDigestResult digest;
        try (InputStream stream = file.getInputStream()) {
            digest = digest(stream);
        } catch (Exception e) {
            return ApiUtil.result(1010, "计算安装包摘要失败", e.getMessage());
        }

        int uid = rbacService.uid(request);
        ClientVersion info = new ClientVersion();
        info.setPlatform(platform);
        info.setArchitecture(architecture);
        info.setChannel(channel);
        info.setVersion(versionName);
        info.setMinimumVersion(minimumVersion);
        info.setReleaseNotes(releaseNotes);
        info.setFileName(fileName);
        info.setFileSize(file.getSize());
        info.setSha512(digest.sha512);
        info.setDeltaManifest(digest.deltaManifest);
        info.setRolloutPercentage(rolloutPercentage);
        info.setAllowRollback(allowRollback);
        info.setStatus(status);
        info = save(clientVersionDao, info, uid);

        String filepath = String.format("client-versions/%d/%s", info.getId(), fileName);
        Map<String, Object> result = streamFileUploader.upload("/file/upload", DPUtil.buildMap(
                "bucket", bucket,
                "filepath", filepath,
                "traceIdentity", String.format("fs-client-version-%d", info.getId())
        ), file);
        if (ApiUtil.failed(result)) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return result;
        }
        info.setFileId(ApiUtil.data(result, ObjectNode.class).at("/id").asText());
        info.setFilepath(filepath);
        info = save(clientVersionDao, info, uid);
        invalidatePolicyCache();
        ObjectNode resultNode = DPUtil.toJSON(info, ObjectNode.class);
        stripDeltaRow(resultNode);
        return ApiUtil.result(0, null, resultNode);
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(clientVersionDao, param, (root, query, cb) -> {
            SpecificationHelper<ClientVersion> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.dateFormat(configuration.getFormatDate()).equalWithIntGTZero("id");
            helper.equalWithIntNotEmpty("status")
                    .equal("platform")
                    .equal("architecture")
                    .equal("channel")
                    .like("version");
            return cb.and(helper.predicates());
        }, Sort.by(Sort.Order.desc("id")), sorts().keySet());
        JsonNode rows = ApiUtil.rows(result);
        stripDeltaManifest(rows);
        if (!DPUtil.empty(args.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if (!DPUtil.empty(args.get("withStatusText"))) {
            fillStatus(rows, status());
        }
        if (!DPUtil.empty(args.get("withPlatformText"))) {
            fillText(rows, "platform", platforms());
            fillText(rows, "architecture", architectures());
            fillText(rows, "channel", channels());
        }
        return result;
    }

    /** 列表只给差分是否可用，不返回几十 KB 的块清单。 */
    protected void stripDeltaManifest(JsonNode rows) {
        if (null == rows) return;
        if (rows.isObject()) {
            stripDeltaRow((ObjectNode) rows);
            return;
        }
        for (JsonNode row : rows) {
            if (row.isObject()) stripDeltaRow((ObjectNode) row);
        }
    }

    protected void stripDeltaRow(ObjectNode row) {
        row.put("deltaAvailable", !DPUtil.empty(row.at("/deltaManifest").asText("")));
        row.remove("deltaManifest");
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        if (id <= 0) return ApiUtil.result(1001, "版本ID异常", id);
        ClientVersion info = info(id);
        if (null == info) return ApiUtil.result(404, null, id);
        String minimumVersion = normalizeVersion(param.get("minimumVersion"));
        if (!isVersion(minimumVersion)) return ApiUtil.result(1002, "最低支持版本格式异常", minimumVersion);
        if (compareVersion(minimumVersion, info.getVersion()) > 0) {
            return ApiUtil.result(1003, "最低支持版本不能高于安装包版本", minimumVersion);
        }
        int status = DPUtil.parseInt(param.get("status"));
        if (!status().containsKey(status)) return ApiUtil.result(1004, "状态异常", status);
        String channel = normalizeChannel(param.get("channel"));
        if (DPUtil.empty(channel)) return ApiUtil.result(1004, "发布渠道异常", channel);
        int rolloutPercentage = null == param.get("rolloutPercentage")
                ? 100 : DPUtil.parseInt(param.get("rolloutPercentage"));
        if (rolloutPercentage < 0 || rolloutPercentage > 100) {
            return ApiUtil.result(1004, "灰度比例必须在 0 到 100 之间", rolloutPercentage);
        }
        int allowRollback = parseBoolean(param.get("allowRollback")) ? 1 : 0;
        String releaseNotes = DPUtil.parseString(param.get("releaseNotes"));
        if (releaseNotes.getBytes(StandardCharsets.UTF_8).length > MAX_RELEASE_NOTES) {
            return ApiUtil.result(1005, "发布日志过长", releaseNotes.length());
        }
        info.setMinimumVersion(minimumVersion);
        info.setChannel(channel);
        info.setRolloutPercentage(rolloutPercentage);
        info.setAllowRollback(allowRollback);
        info.setStatus(status);
        info.setReleaseNotes(releaseNotes);
        info = save(clientVersionDao, info, rbacService.uid(request));
        invalidatePolicyCache();
        ObjectNode resultNode = DPUtil.toJSON(info, ObjectNode.class);
        stripDeltaRow(resultNode);
        return ApiUtil.result(0, null, resultNode);
    }

    /**
     * 启动 / 手动检查：只读取该系统和架构的启用版本，最低支持版本取最高启用版本的配置。
     */
    public Map<String, Object> check(Map<?, ?> param) {
        String platform = normalizePlatform(param.get("platform"));
        if (DPUtil.empty(platform)) return ApiUtil.result(1001, "客户端系统标识异常", platform);
        String rawArchitecture = DPUtil.trim(DPUtil.parseString(param.get("architecture")));
        String architecture = normalizeArchitecture(param.get("architecture"));
        if (DPUtil.empty(architecture)) {
            if (!DPUtil.empty(rawArchitecture)) {
                return ApiUtil.result(1002, "客户端架构标识异常", rawArchitecture);
            }
            architecture = "x64";
        }
        String currentVersion = normalizeVersion(param.get("currentVersion"));
        if (!isVersion(currentVersion)) {
            return ApiUtil.result(1003, "当前客户端版本格式异常", currentVersion);
        }
        String channel = normalizeChannel(param.get("channel"));
        if (DPUtil.empty(channel)) channel = "stable";
        String clientId = DPUtil.trim(DPUtil.parseString(param.get("clientId")));
        if (clientId.length() > 128) clientId = clientId.substring(0, 128);
        int rolloutBucket = rolloutBucket(clientId, platform, architecture);
        ClientVersion latest = latestEnabled(platform, architecture, channel, rolloutBucket);
        ObjectNode result = DPUtil.objectNode();
        result.put("platform", platform);
        result.put("architecture", architecture);
        result.put("channel", channel);
        result.put("rolloutBucket", rolloutBucket);
        result.put("currentVersion", currentVersion);
        if (null == latest) {
            result.put("supported", true);
            result.put("forceUpgrade", false);
            result.put("hasUpdate", false);
            result.putNull("latest");
            return ApiUtil.result(0, null, result);
        }
        int compared = compareVersion(currentVersion, latest.getVersion());
        boolean supported = compareVersion(currentVersion, latest.getMinimumVersion()) >= 0;
        boolean rollback = null != latest.getAllowRollback() && 1 == latest.getAllowRollback()
                && compared > 0;
        boolean hasUpdate = compared < 0 || rollback;
        result.put("supported", supported);
        result.put("forceUpgrade", !supported || rollback);
        result.put("rollback", rollback);
        result.put("hasUpdate", hasUpdate);
        result.set("latest", clientNode(latest));
        return ApiUtil.result(0, null, result);
    }

    /** 管理端下载：按 id 取文件服务地址 */
    public Map<String, Object> download(Map<String, Object> param) {
        ClientVersion info = info(DPUtil.parseInt(param.get("id")));
        if (null == info) return ApiUtil.result(1404, "版本不存在", param);
        return downloadFile(info);
    }

    /** 客户端差分清单：只有启用且已生成清单的版本可用，失败时客户端回退整包。 */
    public Map<String, Object> delta(Map<String, Object> param) {
        ClientVersion info = info(DPUtil.parseInt(param.get("id")));
        if (null == info || null == info.getStatus() || 1 != info.getStatus()) {
            return ApiUtil.result(1404, "版本不存在或已停用", param);
        }
        if (DPUtil.empty(info.getDeltaManifest())) {
            return ApiUtil.result(1404, "该版本没有差分清单", param);
        }
        JsonNode manifest;
        try {
            manifest = DPUtil.parseJSON(decompressDelta(info.getDeltaManifest()));
        } catch (Exception e) {
            return ApiUtil.result(1500, "差分清单损坏", e.getMessage());
        }
        if (null == manifest) return ApiUtil.result(1500, "差分清单损坏", param);
        ObjectNode result = DPUtil.objectNode();
        result.put("id", info.getId());
        result.put("fileName", info.getFileName());
        result.put("fileSize", null == info.getFileSize() ? 0L : info.getFileSize());
        result.put("sha512", info.getSha512());
        result.put("downloadUrl", "/agent/clientUpdate/download?id=" + info.getId());
        result.set("manifest", manifest);
        return ApiUtil.result(0, null, result);
    }

    /** 客户端下载：只允许下载启用版本；客户端随后必须校验返回的 SHA-512 */
    public Map<String, Object> downloadForClient(Map<String, Object> param) {
        ClientVersion info = info(DPUtil.parseInt(param.get("id")));
        if (null == info) return ApiUtil.result(1404, "版本不存在", param);
        if (null == info.getStatus() || 1 != info.getStatus()) {
            return ApiUtil.result(1404, "版本已停用", param);
        }
        return downloadFile(info);
    }

    protected Map<String, Object> downloadFile(ClientVersion info) {
        if (DPUtil.empty(info.getFileId())) return ApiUtil.result(1401, "安装包文件不存在", info.getId());
        return RpcUtil.result(fileRpc.get("/file/download", DPUtil.buildMap("id", info.getFileId())));
    }

    @Transactional
    public Map<String, Object> remove(List<Integer> ids) {
        if (null == ids || ids.isEmpty()) return ApiUtil.result(0, null, 0);
        for (ClientVersion version : clientVersionDao.findAllById(ids)) {
            if (DPUtil.empty(version.getFileId())) continue;
            ArrayNode args = DPUtil.arrayNode();
            ObjectNode item = args.addObject();
            item.put("id", version.getFileId());
            item.put("bucket", bucket);
            item.put("filepath", version.getFilepath());
            Map<String, Object> result = RpcUtil.result(fileRpc.post("/file/delete", args));
            if (ApiUtil.failed(result)) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                return result;
            }
        }
        Map<String, Object> result = remove(clientVersionDao, ids);
        invalidatePolicyCache();
        return result;
    }

    /** 业务接口的统一版本闸门：只对携带客户端版本请求头的调用生效。 */
    public boolean accessBlocked(Map<?, ?> param) {
        Map<String, Object> result = check(param);
        if (ApiUtil.failed(result)) return true;
        JsonNode data = ApiUtil.data(result, JsonNode.class);
        return null != data && data.at("/forceUpgrade").asBoolean(false);
    }

    protected ClientVersion latestEnabled(String platform, String architecture,
            String channel, int rolloutBucket) {
        return enabledVersions().stream()
                .filter(item -> platform.equals(item.getPlatform()))
                .filter(item -> architecture.equals(item.getArchitecture())
                        || "universal".equals(item.getArchitecture()))
                .filter(item -> channelAllowed(channel, item.getChannel()))
                .filter(item -> rolloutBucket < rolloutPercentage(item))
                .sorted(Comparator.comparing(ClientVersion::getVersion,
                                ClientVersionService::compareVersion)
                        .reversed()
                        .thenComparing(item -> architecture.equals(item.getArchitecture()) ? 0 : 1))
                .findFirst()
                .orElse(null);
    }

    protected List<ClientVersion> enabledVersions() {
        long now = System.currentTimeMillis();
        List<ClientVersion> cache = policyCache;
        if (null != cache && now - policyCacheTime < POLICY_CACHE_MILLIS) return cache;
        List<ClientVersion> rows = List.copyOf(clientVersionDao.findAll(
                (root, query, cb) -> cb.equal(root.get("status"), 1)));
        policyCache = rows;
        policyCacheTime = now;
        return rows;
    }

    /**
     * 产品介绍页的公开发布记录。
     *
     * 只公布已启用、全量放量且属于指定渠道的版本；灰度中的版本不会提前出现在公开页。
     * 列表按创建时间倒序，页面据版本号分组展示跨系统的历史发布日志。
     */
    public Map<String, Object> publicReleases(Map<?, ?> param) {
        String channel = normalizeChannel(param.get("channel"));
        if (DPUtil.empty(channel)) channel = "stable";
        int offset = DPUtil.parseInt(param.get("offset"));
        if (offset < 0) offset = 0;
        int limit = DPUtil.parseInt(param.get("limit"));
        if (limit <= 0) limit = 40;
        limit = Math.min(limit, 200);
        final String requestedChannel = channel;
        List<ClientVersion> filtered = enabledVersions().stream()
                .filter(item -> requestedChannel.equals(DPUtil.empty(item.getChannel())
                        ? "stable" : item.getChannel()))
                .filter(item -> rolloutPercentage(item) >= 100)
                .sorted(Comparator.comparing(ClientVersion::getCreatedTime,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .reversed()
                        .thenComparing(ClientVersion::getId, Comparator.reverseOrder()))
                .toList();
        List<ClientVersion> rows = filtered.stream()
                .skip(offset)
                .limit(limit)
                .toList();
        ArrayNode nodes = DPUtil.arrayNode();
        for (ClientVersion item : rows) {
            nodes.add(clientNode(item));
        }
        ObjectNode result = DPUtil.objectNode();
        result.put("channel", channel);
        result.put("offset", offset);
        result.put("limit", limit);
        result.put("total", filtered.size());
        result.set("rows", nodes);
        return ApiUtil.result(0, null, result);
    }

    protected void invalidatePolicyCache() {
        policyCache = null;
        policyCacheTime = 0;
    }

    protected ObjectNode clientNode(ClientVersion info) {
        ObjectNode node = DPUtil.objectNode();
        node.put("id", info.getId());
        node.put("platform", info.getPlatform());
        node.put("architecture", info.getArchitecture());
        node.put("channel", DPUtil.parseString(info.getChannel()));
        node.put("version", info.getVersion());
        node.put("minimumVersion", info.getMinimumVersion());
        node.put("releaseNotes", DPUtil.parseString(info.getReleaseNotes()));
        node.put("fileName", info.getFileName());
        node.put("fileSize", null == info.getFileSize() ? 0L : info.getFileSize());
        node.put("sha512", DPUtil.parseString(info.getSha512()));
        node.put("rolloutPercentage", rolloutPercentage(info));
        node.put("rollback", null != info.getAllowRollback() && 1 == info.getAllowRollback());
        node.put("deltaAvailable", !DPUtil.empty(info.getDeltaManifest()));
        node.put("downloadUrl", "/agent/clientUpdate/download?id=" + info.getId());
        node.put("createdTime", null == info.getCreatedTime() ? 0L : info.getCreatedTime());
        return node;
    }

    /** 列表附带系统 / 架构的展示名称，页面仍保留原始值用于筛选 */
    protected void fillText(JsonNode rows, String field, Map<String, String> texts) {
        if (null == rows) return;
        if (rows.isObject()) {
            fillTextRow((ObjectNode) rows, field, texts);
            return;
        }
        for (JsonNode row : rows) {
            if (row.isObject()) fillTextRow((ObjectNode) row, field, texts);
        }
    }

    protected void fillTextRow(ObjectNode row, String field, Map<String, String> texts) {
        String value = row.at("/" + field).asText("");
        row.put(field + "Text", texts.getOrDefault(value, value));
    }

    /** 客户端更新结果上报：只接受固定事件，避免公共接口写入任意类型。 */
    public Map<String, Object> report(Map<?, ?> param) {
        String event = DPUtil.trim(DPUtil.parseString(param.get("event")));
        if (!"download_started".equals(event) && !"update_failed".equals(event)
                && !"install_failed".equals(event) && !"install_success".equals(event)) {
            return ApiUtil.result(1001, "更新事件异常", event);
        }
        String clientId = DPUtil.trim(DPUtil.parseString(param.get("clientId")));
        if (DPUtil.empty(clientId) || clientId.length() > 128) {
            return ApiUtil.result(1002, "客户端标识异常", clientId);
        }
        String targetVersion = normalizeVersion(param.get("targetVersion"));
        if (!isVersion(targetVersion)) {
            return ApiUtil.result(1003, "目标版本异常", targetVersion);
        }
        String message = DPUtil.parseString(param.get("message"));
        if (message.length() > 2000) message = message.substring(0, 2000);
        ClientUpdateLog log = new ClientUpdateLog();
        log.setClientId(clientId);
        log.setPlatform(normalizePlatform(param.get("platform")));
        log.setArchitecture(normalizeArchitecture(param.get("architecture")));
        log.setChannel(normalizeChannel(param.get("channel")));
        log.setFromVersion(normalizeVersion(param.get("fromVersion")));
        log.setTargetVersion(targetVersion);
        log.setEvent(event);
        log.setMessage(message);
        log = save(clientUpdateLogDao, log, 0);
        return ApiUtil.result(0, null, DPUtil.toJSON(log, ObjectNode.class));
    }

    /** 管理端更新统计与最近记录。 */
    public ObjectNode stats(Map<String, Object> param) {
        Specification<ClientUpdateLog> specification = (root, query, cb) -> {
            SpecificationHelper<ClientUpdateLog> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.dateFormat(configuration.getFormatDate())
                    .betweenWithDate("createdTime")
                    .equal("platform").equal("architecture").equal("channel").equal("event")
                    .like("targetVersion");
            return cb.and(helper.predicates());
        };
        ObjectNode result = search(clientUpdateLogDao, param, specification,
                Sort.by(Sort.Order.desc("id")), "id");
        ObjectNode summary = DPUtil.objectNode();
        long installSuccess = countEvent("install_success", specification);
        long installFailed = countEvent("install_failed", specification);
        summary.put("downloadStarted", countEvent("download_started", specification));
        summary.put("updateFailed", countEvent("update_failed", specification));
        summary.put("installFailed", installFailed);
        summary.put("installSuccess", installSuccess);
        long installs = installSuccess + installFailed;
        summary.put("installSuccessRate",
                installs > 0 ? Math.round(installSuccess * 10000.0 / installs) / 100.0 : 0);
        result.set("summary", summary);
        return result;
    }

    protected long countEvent(String event, Specification<ClientUpdateLog> specification) {
        return clientUpdateLogDao.count((root, query, cb) -> cb.and(
                cb.equal(root.get("event"), event),
                specification.toPredicate(root, query, cb)
        ));
    }

    protected boolean channelAllowed(String requested, String published) {
        String channel = DPUtil.empty(published) ? "stable" : published;
        if ("beta".equals(requested)) return "beta".equals(channel) || "stable".equals(channel);
        return "stable".equals(channel);
    }

    protected int rolloutPercentage(ClientVersion info) {
        return null == info.getRolloutPercentage() ? 100 : info.getRolloutPercentage();
    }

    /** 以本机安装标识做稳定分桶，同一设备在同一系统 / 架构下始终落在同一灰度桶。 */
    protected int rolloutBucket(String clientId, String platform, String architecture) {
        if (DPUtil.empty(clientId)) return 99;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((clientId + ":" + platform + ":" + architecture)
                    .getBytes(StandardCharsets.UTF_8));
            return Math.floorMod(ByteBuffer.wrap(hash, 0, 4).getInt(), 100);
        } catch (NoSuchAlgorithmException e) {
            return 99;
        }
    }

    protected String normalizeChannel(Object value) {
        String channel = DPUtil.trim(DPUtil.parseString(value)).toLowerCase(Locale.ROOT);
        return channels().containsKey(channel) ? channel : "";
    }

    protected boolean parseBoolean(Object value) {
        String text = DPUtil.trim(DPUtil.parseString(value)).toLowerCase(Locale.ROOT);
        return "true".equals(text) || "1".equals(text) || "on".equals(text);
    }

    protected String normalizePlatform(Object value) {
        String platform = DPUtil.trim(DPUtil.parseString(value)).toLowerCase(Locale.ROOT);
        if ("windows".equals(platform)) platform = "win32";
        if ("mac".equals(platform) || "macos".equals(platform) || "osx".equals(platform)) {
            platform = "darwin";
        }
        return platforms().containsKey(platform) ? platform : "";
    }

    /**
     * 一次读取同时生成完整文件 SHA-512 和固定 1MB 块清单。
     * 稳定发布文件与整包摘要共同作为完整性的信任边界。
     */
    protected FileDigestResult digest(InputStream stream) throws IOException, NoSuchAlgorithmException {
        int blockSize = 1024 * 1024;
        MessageDigest fileDigest = MessageDigest.getInstance("SHA-512");
        byte[] buffer = new byte[blockSize];
        int filled = 0;
        ArrayNode blocks = DPUtil.arrayNode();
        while (true) {
            int length = stream.read(buffer, filled, buffer.length - filled);
            if (length < 0) break;
            if (length == 0) continue;
            fileDigest.update(buffer, filled, length);
            filled += length;
            if (filled == buffer.length) {
                appendBlock(blocks, buffer, filled);
                filled = 0;
            }
        }
        if (filled > 0) appendBlock(blocks, buffer, filled);
        ObjectNode manifest = DPUtil.objectNode();
        manifest.put("blockSize", blockSize);
        manifest.set("blocks", blocks);
        return new FileDigestResult(
                HexFormat.of().formatHex(fileDigest.digest()),
                compressDelta(manifest)
        );
    }

    protected String compressDelta(ObjectNode manifest) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
            gzip.write(DPUtil.stringify(manifest).getBytes(StandardCharsets.UTF_8));
        }
        return Base64.getEncoder().encodeToString(output.toByteArray());
    }

    protected String decompressDelta(String value) throws IOException {
        byte[] compressed = Base64.getDecoder().decode(value);
        try (GZIPInputStream gzip = new GZIPInputStream(new java.io.ByteArrayInputStream(compressed))) {
            return new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    protected void appendBlock(ArrayNode blocks, byte[] buffer, int size) throws NoSuchAlgorithmException {
        MessageDigest blockDigest = MessageDigest.getInstance("SHA-256");
        blockDigest.update(buffer, 0, size);
        ObjectNode block = blocks.addObject();
        block.put("checksum", HexFormat.of().formatHex(blockDigest.digest()));
        block.put("size", size);
    }

    protected static class FileDigestResult {
        protected final String sha512;
        protected final String deltaManifest;

        protected FileDigestResult(String sha512, String deltaManifest) {
            this.sha512 = sha512;
            this.deltaManifest = deltaManifest;
        }
    }

    protected String normalizeArchitecture(Object value) {
        String architecture = DPUtil.trim(DPUtil.parseString(value)).toLowerCase(Locale.ROOT);
        if ("amd64".equals(architecture) || "x86_64".equals(architecture)) architecture = "x64";
        if ("aarch64".equals(architecture)) architecture = "arm64";
        if ("arm".equals(architecture)) architecture = "armv7l";
        return architectures().containsKey(architecture) ? architecture : "";
    }

    protected String normalizeVersion(Object value) {
        String version = DPUtil.trim(DPUtil.parseString(value));
        if (version.startsWith("v") || version.startsWith("V")) version = version.substring(1);
        return version;
    }

    protected boolean isVersion(String value) {
        return !DPUtil.empty(value) && value.length() <= 128 && VERSION.matcher(value).matches();
    }

    protected String fileName(MultipartFile file) {
        String name = DPUtil.trim(DPUtil.parseString(file.getOriginalFilename()));
        if (DPUtil.empty(name)) return "";
        name = name.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        if (name.length() > 255) name = name.substring(name.length() - 255);
        return name;
    }

    protected String suffix(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".tar.gz")) return "tar.gz";
        int index = lower.lastIndexOf('.');
        return index < 0 ? "" : lower.substring(index + 1);
    }

    protected boolean installerAllowed(String platform, String suffix, String fileName) {
        switch (platform) {
            case "win32":
                // 自动替换和重启依赖 electron-builder 的 NSIS 安装参数
                return "exe".equals(suffix)
                        && fileName.toLowerCase(Locale.ROOT).endsWith("-setup.exe");
            case "darwin":
                return "dmg".equals(suffix);
            case "linux":
                return "appimage".equals(suffix);
            default:
                return false;
        }
    }

    /** 语义化版本比较：先比较数字段，再按 SemVer 规则比较预发布标识 */
    public static int compareVersion(String left, String right) {
        VersionParts a = VersionParts.parse(left);
        VersionParts b = VersionParts.parse(right);
        int count = Math.max(a.numbers.length, b.numbers.length);
        for (int i = 0; i < count; i++) {
            String x = i < a.numbers.length ? a.numbers[i] : "0";
            String y = i < b.numbers.length ? b.numbers[i] : "0";
            int result = compareNumeric(x, y);
            if (0 != result) return result;
        }
        if (a.pre.isEmpty() && b.pre.isEmpty()) return 0;
        if (a.pre.isEmpty()) return 1;
        if (b.pre.isEmpty()) return -1;
        String[] x = a.pre.split("\\.");
        String[] y = b.pre.split("\\.");
        int countPre = Math.max(x.length, y.length);
        for (int i = 0; i < countPre; i++) {
            if (i >= x.length) return -1;
            if (i >= y.length) return 1;
            boolean xNumeric = x[i].matches("\\d+");
            boolean yNumeric = y[i].matches("\\d+");
            int result;
            if (xNumeric && yNumeric) {
                result = compareNumeric(x[i], y[i]);
            } else if (xNumeric) {
                result = -1;
            } else if (yNumeric) {
                result = 1;
            } else {
                result = x[i].compareTo(y[i]);
            }
            if (0 != result) return result;
        }
        return 0;
    }

    protected static int compareNumeric(String left, String right) {
        String a = left.replaceFirst("^0+(?!$)", "");
        String b = right.replaceFirst("^0+(?!$)", "");
        if (a.length() != b.length()) return Integer.compare(a.length(), b.length());
        return a.compareTo(b);
    }

    protected static class VersionParts {
        protected final String[] numbers;
        protected final String pre;

        protected VersionParts(String[] numbers, String pre) {
            this.numbers = numbers;
            this.pre = pre;
        }

        protected static VersionParts parse(String value) {
            String source = DPUtil.trim(DPUtil.parseString(value));
            if (source.startsWith("v") || source.startsWith("V")) source = source.substring(1);
            int plus = source.indexOf('+');
            if (plus >= 0) source = source.substring(0, plus);
            int hyphen = source.indexOf('-');
            String core = hyphen < 0 ? source : source.substring(0, hyphen);
            String pre = hyphen < 0 ? "" : source.substring(hyphen + 1);
            return new VersionParts(core.split("\\."), pre);
        }
    }

}
