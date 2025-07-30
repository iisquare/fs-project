package com.iisquare.fs.web.lm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.lm.dao.LogDao;
import com.iisquare.fs.web.lm.entity.Log;
import com.iisquare.fs.web.lm.mvc.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@Service
public class LogService extends JPAServiceBase {

    @Autowired
    private LogDao logDao;
    @Autowired
    DefaultRbacService rbacService;
    @Autowired
    Configuration configuration;
    @Autowired
    ServerService serverService;
    @Autowired
    ClientService clientService;

    public Log info(Long id) {
        return info(logDao, id);
    }

    public Map<String, Object> audit(Map<?, ?> param, HttpServletRequest request) {
        Long id = ValidateUtil.filterLong(param.get("id"), true, 1L, null, 0L);
        Log info = info(id);
        if (null == info) {
            return ApiUtil.result(404, null, id);
        }
        info.setAuditReason(DPUtil.implode(DPUtil.parseStringList(param.get("auditReason"))));
        info.setAuditDetail(DPUtil.parseString(param.get("auditDetail")));
        info.setAuditTime(System.currentTimeMillis());
        info.setAuditUid(rbacService.uid(request));
        info = logDao.save(info);
        return ApiUtil.result(0, null, info);
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(logDao, param, (root, query, cb) -> {
            SpecificationHelper<Log> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.dateFormat(configuration.getFormatDate()).withoutDeleted().equalWithLongGTZero("id");
            helper.equalWithIntGTZero("clientId").equalWithIntGTZero("clientEndpointId");
            helper.equalWithIntGTZero("serverId").equalWithIntGTZero("serverEndpointId");
            helper.like("requestIp").like("requestPrompt").like("responseCompletion");
            helper.like("finishReason").like("finishDetail");
            helper.like("auditReason").like("auditDetail").equalWithIntNotEmpty("auditUid");
            helper.betweenWithDate("beginTime").betweenWithDate("endTime").betweenWithDate("auditTime");
            helper.betweenWithDate("requestTime").betweenWithDate("waitingTime").betweenWithDate("responseTime");
            return cb.and(helper.predicates());
        }, Sort.by(Sort.Order.desc("id")), "id", "beginTime", "endTime", "auditTime", "requestTime", "waitingTime", "responseTime");
        JsonNode rows = format(ApiUtil.rows(result));
        if(!DPUtil.empty(args.get("withInfo"))) {
            serverService.fillInfo(rows, "serverId");
            clientService.fillInfo(rows, "clientId");
            rbacService.fillUserInfo(rows, "auditUid");
        }
        return result;
    }

    public JsonNode format(JsonNode rows) {
        for (JsonNode row : rows) {
            ObjectNode node = (ObjectNode) row;
            List<String> auditReason = DPUtil.parseStringList(node.at("/auditReason").asText(""));
            node.replace("auditReason", DPUtil.toJSON(auditReason));
            int requestStream = node.at("/requestStream").asInt(0);
            node.put("requestStream", 1 == requestStream);
        }
        return rows;
    }

    public boolean delete(List<Long> ids, HttpServletRequest request) {
        return delete(logDao, ids, rbacService.uid(request));
    }

}
