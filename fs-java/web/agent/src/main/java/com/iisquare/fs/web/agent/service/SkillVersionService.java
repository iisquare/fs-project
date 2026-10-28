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
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.core.rpc.FileRpc;
import com.iisquare.fs.web.core.file.StreamFileUploader;
import com.iisquare.fs.web.agent.dao.SkillDao;
import com.iisquare.fs.web.agent.dao.SkillVersionDao;
import com.iisquare.fs.web.agent.entity.Skill;
import com.iisquare.fs.web.agent.entity.SkillVersion;
import com.iisquare.fs.web.agent.mvc.Configuration;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SkillVersionService extends JPAServiceBase {

    @Autowired
    SkillVersionDao versionDao;
    @Autowired
    SkillDao skillDao;
    @Autowired
    DefaultRbacService rbacService;
    @Autowired
    Configuration configuration;
    @Autowired
    FileRpc fileRpc;
    @Autowired
    StreamFileUploader streamFileUploader;

    /** 技能版本包的文件桶：单独存放，便于按技能清理旧版本 */
    public static final String bucket = "fs-skill";

    @Override
    public Map<String, String> sorts() {
        Map<String, String> sorts = new LinkedHashMap<>();
        sorts.put("id", "desc");
        sorts.put("status", "asc");
        return sorts;
    }

    public Map<?, ?> status() {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "启用");
        status.put(2, "禁用");
        return status;
    }

    public SkillVersion info(Integer id) {
        return info(versionDao, id);
    }

    @Transactional
    public Map<String, Object> upload(MultipartFile file, Map<?, ?> param, HttpServletRequest request) {
        if (null == file || file.isEmpty()) return ApiUtil.result(1001, "获取文件句柄失败", null);
        int uid = rbacService.uid(request);
        Integer skillId = ValidateUtil.filterInteger(param.get("skillId"), true, 1, null, 0);
        if (null == skillId || skillId <= 0) return ApiUtil.result(1002, "请选择所属技能", skillId);
        Skill skill = info(skillDao, skillId);
        if (null == skill) return ApiUtil.result(1002, "技能不存在", skillId);
        String filename = file.getOriginalFilename();
        String suffix = "";
        if (null != filename && filename.contains(".")) {
            suffix = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        }
        if (!"zip".equals(suffix)) {
            return ApiUtil.result(1003, "仅支持zip格式文件", suffix);
        }
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if (DPUtil.empty(name)) name = filename;
        String description = DPUtil.parseString(param.get("description"));
        // 创建版本记录
        SkillVersion version = new SkillVersion();
        version.setSkillId(skillId);
        version.setName(name);
        version.setFilepath(String.format("skill-%d/versions/", skillId));
        version.setFileSize(file.getSize());
        version.setStatus(1);
        version.setDescription(description);
        version = save(versionDao, version, uid);
        // 上传到文件服务
        String filepath = String.format("skill-%d/versions/%d.zip", skillId, version.getId());
        Map<String, Object> result = streamFileUploader.upload("/file/upload", DPUtil.buildMap(
                "bucket", bucket, "filepath", filepath,
                "traceIdentity", String.format("fs-skill-%d-version-%d", skillId, version.getId())
        ), file);
        if (ApiUtil.failed(result)) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return result;
        }
        version.setFileId(ApiUtil.data(result, ObjectNode.class).at("/id").asText());
        version.setFilepath(filepath);
        version.setFileSize(file.getSize());
        version = save(versionDao, version, uid);
        return ApiUtil.result(0, null, version);
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(versionDao, param, (root, query, cb) -> {
            SpecificationHelper<SkillVersion> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.dateFormat(configuration.getFormatDate()).equalWithIntGTZero("id");
            helper.equalWithIntGTZero("skillId");
            helper.equalWithIntNotEmpty("status").like("name");
            return cb.and(helper.predicates());
        }, Sort.by(Sort.Order.desc("id")), sorts().keySet());
        JsonNode rows = ApiUtil.rows(result);
        if (!DPUtil.empty(args.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if (!DPUtil.empty(args.get("withStatusText"))) {
            fillStatus(rows, status());
        }
        if (!DPUtil.empty(args.get("withSkillInfo"))) {
            fillInfo(skillDao, rows, "skillId");
        }
        return result;
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        if (id <= 0) return ApiUtil.result(1001, "版本ID异常", id);
        SkillVersion info = info(id);
        if (null == info) return ApiUtil.result(404, null, id);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if (DPUtil.empty(name)) return ApiUtil.result(1002, "版本名称异常", name);
        int status = DPUtil.parseInt(param.get("status"));
        if (!status().containsKey(status)) return ApiUtil.result(1003, "状态异常", status);
        info.setName(name);
        info.setStatus(status);
        info.setDescription(DPUtil.parseString(param.get("description")));
        info = save(versionDao, info, rbacService.uid(request));
        return ApiUtil.result(0, null, info);
    }

    public Map<String, Object> download(Map<String, Object> param) {
        SkillVersion info = info(DPUtil.parseInt(param.get("id")));
        if (null == info) return ApiUtil.result(1404, "信息不存在", param);
        String fileId = info.getFileId();
        if (DPUtil.empty(fileId)) return ApiUtil.result(1401, "文件不存在", param);
        return RpcUtil.result(fileRpc.get("/file/download", DPUtil.buildMap("id", fileId)));
    }

    /**
     * 客户端可用版本清单：调用方（桌面端等）只能看到「技能已启用 + 版本已启用 + 授权命中」的版本，
     * 且不下发 fileId / filepath —— 下载统一走 /skillVersion/package，由服务端校验后再取文件，
     * 避免存储路径与归档标识暴露给客户端。
     */
    public Map<String, Object> available(Map<?, ?> param, HttpServletRequest request) {
        int skillId = DPUtil.parseInt(param.get("skillId"));
        if (skillId < 1) return ApiUtil.result(1001, "技能标识异常", skillId);
        Skill skill = info(skillDao, skillId);
        if (null == skill) return ApiUtil.result(404, null, skillId);
        if (!authorized(skill, request)) return ApiUtil.result(9403, "无权限使用该技能", skillId);
        List<SkillVersion> rows = versionDao.findAll((root, query, cb) -> cb.and(
                cb.equal(root.get("skillId"), skillId), cb.equal(root.get("status"), 1)),
                Sort.by(Sort.Order.desc("id")));
        ArrayNode items = DPUtil.arrayNode();
        for (SkillVersion info : rows) {
            ObjectNode item = items.addObject();
            item.put("id", info.getId());
            item.put("skillId", info.getSkillId());
            item.put("skillName", DPUtil.parseString(skill.getName()));
            item.put("name", DPUtil.parseString(info.getName()));
            item.put("fileSize", null == info.getFileSize() ? 0L : info.getFileSize());
            item.put("description", DPUtil.parseString(info.getDescription()));
            item.put("createdTime", null == info.getCreatedTime() ? 0L : info.getCreatedTime());
        }
        ObjectNode result = DPUtil.objectNode();
        result.put("skillId", skillId);
        result.put("skillName", DPUtil.parseString(skill.getName()));
        result.set("rows", items);
        return ApiUtil.result(0, null, result);
    }

    /**
     * 某个技能下已启用的版本数量：与 {@link #latestEnabled} 一起供技能清单用，
     * 只做 COUNT（不读文件标识 / 存储路径这类字段），技能再多也不会把清单接口拖重。
     */
    public long countEnabled(Integer skillId) {
        return versionDao.count((root, query, cb) -> cb.and(
                cb.equal(root.get("skillId"), skillId), cb.equal(root.get("status"), 1)));
    }

    /** 某个技能下最新的一个启用版本：只取一行，供技能卡片的版本标签直接显示（不必先取整份清单） */
    public ObjectNode latestEnabled(Integer skillId) {
        List<SkillVersion> rows = versionDao.findAll((root, query, cb) -> cb.and(
                        cb.equal(root.get("skillId"), skillId), cb.equal(root.get("status"), 1)),
                PageRequest.of(0, 1, Sort.by(Sort.Order.desc("id")))).getContent();
        if (rows.isEmpty()) return null;
        SkillVersion latest = rows.get(0);
        ObjectNode node = DPUtil.objectNode();
        node.put("id", latest.getId());
        node.put("name", DPUtil.parseString(latest.getName()));
        node.put("description", DPUtil.parseString(latest.getDescription()));
        node.put("fileSize", null == latest.getFileSize() ? 0L : latest.getFileSize());
        node.put("createdTime", null == latest.getCreatedTime() ? 0L : latest.getCreatedTime());
        return node;
    }

    /**
     * 客户端下载技能包：技能与版本都要求已启用，且技能授权命中调用人，
     * 通过后返回文件服务地址由调用方下载（zip，解压后落到本地 skills 目录）
     */
    public Map<String, Object> packageDownload(Map<String, Object> param, HttpServletRequest request) {
        SkillVersion info = info(DPUtil.parseInt(param.get("id")));
        if (null == info) return ApiUtil.result(1404, "版本不存在", param);
        if (null == info.getStatus() || 1 != info.getStatus()) {
            return ApiUtil.result(1404, "版本已停用", param);
        }
        Skill skill = info(skillDao, info.getSkillId());
        if (null == skill || null == skill.getStatus() || 1 != skill.getStatus()) {
            return ApiUtil.result(1404, "技能已停用", param);
        }
        if (!authorized(skill, request)) return ApiUtil.result(9403, "无权限使用该技能", param);
        String fileId = info.getFileId();
        if (DPUtil.empty(fileId)) return ApiUtil.result(1401, "文件不存在", param);
        return RpcUtil.result(fileRpc.get("/file/download", DPUtil.buildMap("id", fileId)));
    }

    /**
     * 技能授权校验：与 SkillService 同一口径，未配置授权角色时所有登录用户可用
     */
    public boolean authorized(Skill info, HttpServletRequest request) {
        if (null == info) return false;
        Set<Integer> allowed = new LinkedHashSet<>(DPUtil.parseIntList(info.getRoleIds()));
        if (allowed.isEmpty()) return true;
        return !Collections.disjoint(allowed, rbacService.roleIds(request));
    }

    public Map<String, Object> remove(List<Integer> ids) {
        if (null == ids || ids.isEmpty()) return ApiUtil.result(0, null, 0);
        try {
            for (SkillVersion version : versionDao.findAllById(ids)) {
                if (!DPUtil.empty(version.getFileId())) {
                    ArrayNode args = DPUtil.arrayNode();
                    ObjectNode item = args.addObject();
                    item.put("id", version.getFileId());
                    item.put("bucket", bucket);
                    item.put("filepath", version.getFilepath());
                    Map<String, Object> result = RpcUtil.result(fileRpc.post("/file/delete", args));
                    if (ApiUtil.failed(result)) {
                        return ApiUtil.result(ApiUtil.code(result), ApiUtil.message(result),
                                ApiUtil.data(result, Object.class));
                    }
                }
            }
        } catch (Exception e) {
            return ApiUtil.result(1500, "删除技能版本文件失败", e.getMessage());
        }
        return remove(versionDao, ids);
    }

    public void deleteBySkillId(Integer skillId) {
        List<SkillVersion> versions = versionDao.findAll((root, query, cb) -> {
            return cb.equal(root.get("skillId"), skillId);
        });
        for (SkillVersion version : versions) {
            if (!DPUtil.empty(version.getFileId())) {
                ArrayNode args = DPUtil.arrayNode();
                ObjectNode item = args.addObject();
                item.put("id", version.getFileId());
                item.put("bucket", bucket);
                item.put("filepath", version.getFilepath());
                fileRpc.post("/file/delete", args);
            }
        }
        versionDao.deleteAll(versions);
    }

}
