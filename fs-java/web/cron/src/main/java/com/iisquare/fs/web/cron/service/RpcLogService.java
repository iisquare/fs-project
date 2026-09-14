package com.iisquare.fs.web.cron.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.base.web.util.ServiceUtil;
import com.iisquare.fs.web.cron.dao.RpcLogDao;
import com.iisquare.fs.web.cron.entity.RpcLog;
import com.iisquare.fs.web.cron.mvc.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RpcLogService extends JPAServiceBase {

    @Autowired
    RpcLogDao rpcLogDao;
    @Autowired
    Configuration configuration;

    @Override
    public Map<String, String> sorts() {
        Map<String, String> sorts = new LinkedHashMap<>();
        sorts.put("id", "desc");
        sorts.put("requestTime", "desc");
        sorts.put("status", "asc");
        sorts.put("state", "asc");
        sorts.put("duration", "asc");
        return sorts;
    }

    public Map<String, String> states() {
        Map<String, String> status = new LinkedHashMap<>();
        status.put(RpcLog.State.RUNNING.name(), "正在调度");
        status.put(RpcLog.State.SUCCEED.name(), "调度成功");
        status.put(RpcLog.State.FAILED.name(), "调度失败");
        return status;
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(rpcLogDao, param, (Specification<RpcLog>) (root, query, cb) -> {
            SpecificationHelper<RpcLog> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.dateFormat(configuration.getFormatDate());
            helper.equalWithLongGTZero("id");
            helper.equal("schedule").equal("jobName").equal("jobGroup");
            helper.equal("app").like("uri").equal("state");
            helper.betweenWithDate("requestTime");
            return cb.and(helper.predicates());
        }, Sort.by(Sort.Order.desc("requestTime"), Sort.Order.desc("id")), sorts().keySet());
        JsonNode rows = ApiUtil.rows(result);
        ServiceUtil.retain(rows, param.get("columns"));
        return result;
    }

    public Map<String, Object> info(Map<?, ?> param) {
        Long id = DPUtil.parseLong(param.get("id"));
        RpcLog info = info(rpcLogDao, id);
        if (null == info) return ApiUtil.result(1404, "信息不存在", id);
        return ApiUtil.result(0, null, info);
    }

    public boolean remove(List<Long> ids) {
        if (null == ids || ids.isEmpty()) return false;
        rpcLogDao.deleteAllByIdInBatch(ids);
        return true;
    }

}
