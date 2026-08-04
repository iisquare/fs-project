package com.iisquare.fs.web.lm.service;

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
import com.iisquare.fs.web.lm.dao.SkillDao;
import com.iisquare.fs.web.lm.dao.SkillVersionDao;
import com.iisquare.fs.web.lm.entity.Skill;
import com.iisquare.fs.web.lm.entity.SkillVersion;
import com.iisquare.fs.web.lm.mvc.Configuration;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    public static final String bucket = "fs-lm-skill";

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
        Map<String, Object> result = RpcUtil.result(fileRpc.form("/file/upload", DPUtil.buildMap(
                "bucket", bucket, "filepath", filepath,
                "traceIdentity", String.format("fs-lm-skill-%d-version-%d", skillId, version.getId())
        ), file));
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
        }, Sort.by(Sort.Order.desc("id")), "id", "status");
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

    public boolean remove(List<Integer> ids) {
        for (SkillVersion version : versionDao.findAllById(ids)) {
            if (!DPUtil.empty(version.getFileId())) {
                ArrayNode args = DPUtil.arrayNode();
                ObjectNode item = args.addObject();
                item.put("id", version.getFileId());
                item.put("bucket", bucket);
                item.put("filepath", version.getFilepath());
                fileRpc.post("/file/delete", args);
            }
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
