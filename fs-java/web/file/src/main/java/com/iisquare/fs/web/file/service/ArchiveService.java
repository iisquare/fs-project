package com.iisquare.fs.web.file.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
    ArchiveDao archiveDao;
    @Autowired
    DefaultRbacService rbacService;
    @Autowired
    Configuration configuration;
    @Autowired
    S3Service s3Service;

    @Override
    public Map<String, String> sorts() {
        Map<String, String> sorts = new LinkedHashMap<>();
        sorts.put("id", "desc");
        sorts.put("name", "asc");
        sorts.put("filepath", "asc");
        sorts.put("size", "asc");
        sorts.put("status", "asc");
        sorts.put("createdTime", "asc");
        sorts.put("updatedTime", "desc");
        return sorts;
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> config) {
        ObjectNode result = search(archiveDao, param, (root, query, cb) -> {
            SpecificationHelper<Archive> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.dateFormat(configuration.getFormatDate());
            helper.equal("id").like("name").equal("bucket").deleted()
                    .equal("suffix").like("type").like("filepath").between("size");
            helper.equalWithIntNotEmpty("status").equal("traceIdentity")
                    .betweenWithDate("createdTime").betweenWithDate("updatedTime");
            return cb.and(helper.predicates());
        }, Sort.by(Sort.Order.desc("updatedTime"), Sort.Order.desc("id"))
                , sorts().keySet());
        JsonNode rows = format(ApiUtil.rows(result));
        if(!DPUtil.empty(config.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(config.get("withStatusText"))) {
            fillStatus(rows, status());
        }
        return result;
    }

    public JsonNode format(JsonNode rows) {
        for (JsonNode row : rows) {
            ObjectNode node = (ObjectNode) row;
            node.put("sharable", 1 == node.at("/sharable").asInt(0));
        }
        return rows;
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
        info.setSharable(DPUtil.parseBoolean(param.get("sharable")) ? 1 : 0);
        info.setTraceIdentity(DPUtil.parseString(param.get("traceIdentity")));
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

    public Map<String, Object> delete(List<Archive> archives, HttpServletRequest request) {
        if (null == archives || archives.isEmpty()) return summary(0, 0, new ArrayList<>());
        long time = System.currentTimeMillis();
        int uid = rbacService.uid(request);
        int deleted = 0;
        List<ObjectNode> missing = new ArrayList<>();
        for (Archive archive : archives) {
            Map<String, Object> param = new LinkedHashMap<>();
            param.put("bucket", archive.getBucket());
            param.put("object", archive.getFilepath());
            Map<String, Object> result = s3Service.removeObject(param);
            if (ApiUtil.failed(result)) {
                return ApiUtil.result(ApiUtil.code(result), ApiUtil.message(result), DPUtil.buildMap(
                        String.class, Object.class,
                        "id", archive.getId(), "bucket", archive.getBucket(),
                        "filepath", archive.getFilepath(), "cause", ApiUtil.data(result, Object.class)));
            }
            ObjectNode state = ApiUtil.data(result, ObjectNode.class);
            if (null == state || state.at("/deleted").asBoolean(true)) {
                deleted++;
            } else {
                ObjectNode item = DPUtil.objectNode();
                item.put("id", archive.getId());
                item.put("bucket", archive.getBucket());
                item.put("filepath", archive.getFilepath());
                missing.add(item);
            }
            archive.setDeletedTime(time);
            archive.setDeletedUid(uid);
        }
        try {
            archiveDao.saveAll(archives);
        } catch (Exception e) {
            return ApiUtil.result(1500, "更新文件记录失败", e.getMessage());
        }
        return summary(archives.size(), deleted, missing);
    }

    private Map<String, Object> summary(int total, int deleted, List<ObjectNode> missing) {
        ObjectNode data = DPUtil.objectNode();
        data.put("total", total);
        data.put("deleted", deleted);
        ArrayNode rows = data.putArray("missing");
        for (ObjectNode item : missing) {
            rows.add(item);
        }
        String message = missing.isEmpty() ? null
                : String.format("操作成功，其中 %d 个对象不存在，已同步清理文件记录", missing.size());
        return ApiUtil.result(0, message, data);
    }

    public Map<String, Object> deleteById(List<String> ids, HttpServletRequest request) {
        if (null == ids || ids.isEmpty()) return summary(0, 0, new ArrayList<>());
        List<Archive> archives;
        try {
            archives = archiveDao.findAll((root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.gt(root.get("deletedTime"), 0));
                predicates.add(root.get("id").in(ids));
                return cb.or(predicates.toArray(new Predicate[0]));
            });
        } catch (Exception e) {
            return ApiUtil.result(1500, "查询文件记录失败", e.getMessage());
        }
        return delete(archives, request);
    }

    public Map<String, Object> deleteByQuery(JsonNode json, HttpServletRequest request) {
        List<Archive> archives;
        try {
            archives = archiveDao.findAll((root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
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
                return cb.and(cb.equal(root.get("deletedTime"), 0), cb.or(predicates.toArray(new Predicate[0])));
            });
        } catch (Exception e) {
            return ApiUtil.result(1500, "查询文件记录失败", e.getMessage());
        }
        return delete(archives, request);
    }

}
