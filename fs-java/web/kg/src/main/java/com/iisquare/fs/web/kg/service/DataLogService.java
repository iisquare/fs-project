package com.iisquare.fs.web.kg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.kg.dao.DataLogDao;
import com.iisquare.fs.web.kg.entity.DataLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 图数据变更审计
 */
@Service
public class DataLogService extends JPAServiceBase {

    @Autowired
    DataLogDao dataLogDao;
    @Autowired
    DefaultRbacService rbacService;

    @Override
    public Map<String, String> sorts() {
        Map<String, String> sorts = new LinkedHashMap<>();
        sorts.put("id", "desc");
        sorts.put("createdTime", "desc");
        return sorts;
    }

    /**
     * 记录变更，审计失败不影响业务操作
     */
    public void append(Integer ontologyId, String kind, String label, String action, String targets,
                       String payload, int uid, int code, String message) {
        try {
            DataLog log = new DataLog();
            log.setOntologyId(null == ontologyId ? 0 : ontologyId);
            log.setKind(kind);
            log.setLabel(label);
            log.setAction(action);
            log.setTargets(DPUtil.trim(targets));
            log.setPayload(null == payload ? "" : payload);
            log.setUid(uid);
            log.setResultCode(code);
            String result = DPUtil.trim(message);
            log.setResultMessage(result.length() > 500 ? result.substring(0, 500) : result);
            log.setCreatedTime(System.currentTimeMillis());
            dataLogDao.save(log);
        } catch (Exception ignored) {
        }
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(dataLogDao, param, (root, query, cb) -> {
            SpecificationHelper<DataLog> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.equalWithIntGTZero("ontologyId").equalWithIntGTZero("uid").equal("kind").equal("action").like("label").like("targets");
            List<Predicate> predicates = new ArrayList<>(Arrays.asList(helper.predicates()));
            long beginTime = DPUtil.parseLong(param.get("beginTime"));
            if (beginTime > 0) predicates.add(cb.greaterThanOrEqualTo(root.get("createdTime"), beginTime));
            long endTime = DPUtil.parseLong(param.get("endTime"));
            if (endTime > 0) predicates.add(cb.lessThanOrEqualTo(root.get("createdTime"), endTime));
            return cb.and(predicates.toArray(new Predicate[0]));
        }, Sort.by(Sort.Order.desc("id")), sorts().keySet());
        JsonNode rows = ApiUtil.rows(result);
        if (!DPUtil.empty(args.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "uid");
        }
        return result;
    }

    /**
     * 清理指定时间之前的变更记录
     */
    public long clean(long beforeTime) {
        if (beforeTime < 1) return 0;
        return dataLogDao.deleteBefore(beforeTime);
    }

}
