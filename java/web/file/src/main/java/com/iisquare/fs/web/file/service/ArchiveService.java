package com.iisquare.fs.web.file.service;

import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.file.dao.ArchiveDao;
import com.iisquare.fs.web.file.entity.Archive;
import com.iisquare.fs.web.file.mvc.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ArchiveService extends ServiceBase {

    @Autowired
    private ArchiveDao archiveDao;
    @Autowired
    private DefaultRbacService rbacService;
    @Autowired
    private Configuration configuration;

    public Map<?, ?> search(Map<?, ?> param, Map<?, ?> config) {
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, 15);
        Page<Archive> data = archiveDao.findAll((Specification<Archive>) (root, query, cb) -> {
            SpecificationHelper<Archive> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.dateFormat(configuration.getFormatDate());
            helper.equal("id").like("name").equal("bucket").equal("suffix").equal("type").between("size");
            helper.equalWithIntElseNot("status", -1).betweenWithDate("createdTime").betweenWithDate("updatedTime");
            return cb.and(helper.predicates());
        }, PageRequest.of(page - 1, pageSize, Sort.by(new Sort.Order(Sort.Direction.DESC, "updatedTime"))));
        List<?> rows = data.getContent();
        if(!DPUtil.empty(config.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(config.get("withStatusText"))) {
            DPUtil.fillValues(rows, new String[]{"status"}, new String[]{"statusText"}, status("full"));
        }
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", data.getTotalElements());
        result.put("rows", rows);
        return result;
    }

    public boolean miss(String id) {
        return archiveDao.miss(id) == 1;
    }

    public Map<?, ?> status(String level) {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "启用");
        status.put(2, "禁用"); // 人工禁止访问
        status.put(3, "弃用"); // 业务端主动删除
        status.put(4, "丢失"); // 记录对应的文件不存在
        switch (level) {
            case "default":
                break;
            case "full":
                status.put(-1, "已删除"); // 人工删除
                break;
            default:
                return null;
        }
        return status;
    }

    public List<Archive> all(Collection<String> ids) {
        if (null == ids || ids.size() < 1) return new ArrayList<>();
        return archiveDao.findAllById(ids);
    }

    public Archive info(String id) {
        if(DPUtil.empty(id)) return null;
        Optional<Archive> info = archiveDao.findById(id);
        return info.isPresent() ? info.get() : null;
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

    public boolean delete(List<String> ids, int uid) {
        if(null == ids || ids.size() < 1) return false;
        List<Archive> list = archiveDao.findAllById(ids);
        long time = System.currentTimeMillis();
        for (Archive item : list) {
            item.setStatus(3);
            item.setUpdatedTime(time);
            item.setUpdatedUid(uid);
        }
        archiveDao.saveAll(list);
        return true;
    }

}
