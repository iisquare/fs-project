package com.iisquare.fs.web.file.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.file.dao.ArchiveDao;
import com.iisquare.fs.web.file.entity.Archive;
import com.iisquare.fs.web.file.mvc.Configuration;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ArchiveService extends JPAServiceBase {

    @Autowired
    private ArchiveDao archiveDao;
    @Autowired
    private DefaultRbacService rbacService;
    @Autowired
    private Configuration configuration;
    @Autowired
    private MinIOService minIOService;

    public ObjectNode search(Map<String, Object> param, Map<?, ?> config) {
        ObjectNode result = search(archiveDao, param, (root, query, cb) -> {
            SpecificationHelper<Archive> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.dateFormat(configuration.getFormatDate());
            helper.equal("id").like("name").equal("bucket")
                    .equal("suffix").like("type").like("filepath").between("size");
            helper.equalWithIntNotEmpty("status")
                    .betweenWithDate("createdTime").betweenWithDate("updatedTime");
            return cb.and(helper.predicates());
        }, Sort.by(Sort.Order.desc("updatedTime"))
                , "id", "name", "filepath", "size", "status", "createdTime", "updatedTime");
        JsonNode rows = ApiUtil.rows(result);
        if(!DPUtil.empty(config.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(config.get("withStatusText"))) {
            fillStatus(rows, status());
        }
        return result;
    }

    public boolean miss(String id) {
        return archiveDao.miss(id) == 1;
    }

    public Map<Integer, String> status() {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "启用");
        status.put(2, "禁用"); // 人工禁止访问
        status.put(3, "弃用"); // 业务端主动弃用
        status.put(4, "丢失"); // 记录对应的文件不存在
        return status;
    }

    public List<Archive> all(Collection<String> ids) {
        if (null == ids || ids.isEmpty()) return new ArrayList<>();
        return archiveDao.findAllById(ids);
    }

    public Archive info(String id) {
        return info(archiveDao, id);
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        String id = DPUtil.parseString(param.get("id"));
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if(DPUtil.empty(name)) return ApiUtil.result(1001, "文件名称异常", name);
        String bucket = DPUtil.trim(DPUtil.parseString(param.get("bucket")));
        if(DPUtil.empty(bucket)) return ApiUtil.result(1002, "文件桶不能为空", bucket);
        String filepath = DPUtil.trim(DPUtil.parseString(param.get("filepath")));
        if(DPUtil.empty(filepath)) return ApiUtil.result(1003, "文件路径不能为空", filepath);
        int status = DPUtil.parseInt(param.get("status"));
        if(!status().containsKey(status)) return ApiUtil.result(1004, "状态异常", status);
        Archive info;
        if(DPUtil.empty(id)) {
            info = new Archive();
            info.setId(id);
        } else {
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
        }
        info.setName(name);
        info.setBucket(bucket);
        info.setFilepath(filepath);
        info.setSuffix(DPUtil.parseString(param.get("suffix")));
        info.setType(DPUtil.parseString(param.get("type")));
        info.setSize(DPUtil.parseLong(param.get("size")));
        info.setDigest(DPUtil.parseString(param.get("digest")));
        info.setHash(DPUtil.parseString(param.get("hash")));
        info.setStatus(status);
        info = save(archiveDao, info, rbacService.uid(request));
        return ApiUtil.result(0, null, info);
    }

    public Archive add(Archive archive, int uid) {
        if (null != info(archive.getId())) return null;
        long time = System.currentTimeMillis();
        archive.setUpdatedTime(time);
        archive.setUpdatedUid(uid);
        archive.setCreatedTime(time);
        archive.setCreatedUid(uid);
        return archiveDao.save(archive);
    }

    public Archive update(Archive archive, int uid) {
        long time = System.currentTimeMillis();
        archive.setUpdatedTime(time);
        archive.setUpdatedUid(uid);
        return archiveDao.save(archive);
    }

    public boolean delete(List<Archive> archives, HttpServletRequest request) {
        if (archives.isEmpty()) return true;
        long time = System.currentTimeMillis();
        int uid = rbacService.uid(request);
        for (Archive archive : archives) {
            Map<String, Object> param = new LinkedHashMap<>();
            param.put("bucket", archive.getBucket());
            param.put("object", archive.getFilepath());
            Map<String, Object> result = minIOService.removeObject(param);
            if (ApiUtil.failed(result)) return false;
            archive.setDeletedTime(time);
            archive.setDeletedUid(uid);
        }
        archiveDao.saveAll(archives);
        return true;
    }

    public boolean deleteById(List<String> ids, HttpServletRequest request) {
        if (ids.isEmpty()) return true;
        List<Archive> archives = archiveDao.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.gt(root.get("deletedTime"), 0));
            predicates.add(root.get("id").in(ids));
            return cb.or(predicates.toArray(new Predicate[0]));
        });
        return delete(archives, request);
    }

    public boolean deleteByQuery(JsonNode json, HttpServletRequest request) {
        List<Archive> archives = archiveDao.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.gt(root.get("deletedTime"), 0));
            for (JsonNode node : json) {
                String id = node.at("/id").asText();
                if (!DPUtil.empty(id)) {
                    predicates.add(cb.equal(root.get("id"), id));
                }
                String bucket = node.at("/bucket").asText();
                String filepath = node.at("/filepath").asText();
                if (!DPUtil.empty(bucket) && !DPUtil.empty(filepath)) {
                    predicates.add(cb.and(
                            cb.equal(root.get("bucket"), bucket),
                            cb.equal(root.get("filepath"), filepath)
                    ));
                }
            }
            return cb.or(predicates.toArray(new Predicate[0]));
        });
        return delete(archives, request);
    }

}
