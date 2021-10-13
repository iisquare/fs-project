package com.iisquare.fs.web.bi.service;

import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.bi.dao.DiagramDao;
import com.iisquare.fs.web.bi.entity.Diagram;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.Predicate;
import java.util.*;

@Service
public class DiagramService extends ServiceBase {

    @Autowired
    private DiagramDao diagramDao;
    @Autowired
    private DefaultRbacService rbacService;

    public Map<?, ?> search(Map<?, ?> param, Map<?, ?> config) {
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, 15);
        Page<Diagram> data = diagramDao.findAll((Specification<Diagram>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.notEqual(root.get("status"), -1));
            String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
            if(!DPUtil.empty(name)) {
                predicates.add(cb.like(root.get("name"), "%" + name + "%"));
            }
            String engine = DPUtil.trim(DPUtil.parseString(param.get("engine")));
            if(!DPUtil.empty(engine)) {
                predicates.add(cb.equal(root.get("engine"), engine));
            }
            String model = DPUtil.trim(DPUtil.parseString(param.get("model")));
            if(!DPUtil.empty(model)) {
                predicates.add(cb.equal(root.get("model"), model));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, PageRequest.of(page - 1, pageSize, Sort.by(new Sort.Order(Sort.Direction.DESC, "sort"))));
        List<?> rows = data.getContent();
        if(!DPUtil.empty(config.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(config.get("withStatusText"))) {
            DPUtil.fillValues(rows, new String[]{"status"}, new String[]{"statusText"}, status("full"));
        }
        if(!DPUtil.empty(config.get("withEngineText"))) {
            DPUtil.fillValues(rows, new String[]{"engine"}, new String[]{"engineText"}, engines());
        }
        if(!DPUtil.empty(config.get("withModelText"))) {
            DPUtil.fillValues(rows, new String[]{"model"}, new String[]{"modelText"}, models());
        }
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", data.getTotalElements());
        result.put("rows", rows);
        return result;
    }

    public Map<?, ?> status(String level) {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "启用");
        status.put(2, "禁用");
        switch (level) {
            case "default":
                break;
            case "full":
                status.put(-1, "已删除");
                break;
            default:
                return null;
        }
        return status;
    }

    public Map<?, ?> engines() {
        Map<String, String> types = new LinkedHashMap<>();
        types.put("spark", "Apache Spark");
        types.put("flink", "Apache Flink");
        return types;
    }

    public Map<?, ?> models() {
        Map<String, String> types = new LinkedHashMap<>();
        types.put("batch", "批量处理");
        types.put("stream", "流式计算");
        return types;
    }

    public Diagram info(Integer id) {
        if(null == id || id < 1) return null;
        Optional<Diagram> info = diagramDao.findById(id);
        return info.isPresent() ? info.get() : null;
    }

    public Diagram save(Diagram info, int uid) {
        long time = System.currentTimeMillis();
        info.setUpdatedTime(time);
        info.setUpdatedUid(uid);
        if(null == info.getId()) {
            info.setCreatedTime(time);
            info.setCreatedUid(uid);
        }
        return diagramDao.save(info);
    }

    public boolean delete(List<Integer> ids, int uid) {
        if(null == ids || ids.size() < 1) return false;
        List<Diagram> list = diagramDao.findAllById(ids);
        long time = System.currentTimeMillis();
        for (Diagram item : list) {
            item.setStatus(-1);
            item.setUpdatedTime(time);
            item.setUpdatedUid(uid);
        }
        diagramDao.saveAll(list);
        return true;
    }

}
