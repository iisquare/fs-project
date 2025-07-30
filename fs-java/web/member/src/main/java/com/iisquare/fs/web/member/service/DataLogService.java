package com.iisquare.fs.web.member.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.member.dao.DataDao;
import com.iisquare.fs.web.member.dao.DataLogDao;
import com.iisquare.fs.web.member.dao.DataPermitDao;
import com.iisquare.fs.web.member.dao.DataPermitLogDao;
import com.iisquare.fs.web.member.entity.*;
import com.iisquare.fs.web.member.mvc.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

@Service
public class DataLogService extends JPAServiceBase {

    @Autowired
    RbacService rbacService;
    @Autowired
    DataLogDao dataLogDao;
    @Autowired
    DataPermitLogDao dataPermitLogDao;
    @Autowired
    DataDao dataDao;
    @Autowired
    DataPermitDao dataPermitDao;
    @Autowired
    Configuration configuration;

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(dataLogDao, param, (Specification<DataLog>) (root, query, cb) -> {
            SpecificationHelper<DataLog> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.dateFormat(configuration.getFormatDate()).equalWithIntGTZero("id");
            helper.functionFindInSet("permits").equal("serverId").equal("clientId");
            helper.like("requestUrl").equal("requestIp").like("requestHeaders");
            helper.like("requestParams").betweenWithDate("requestTime");
            return cb.and(helper.predicates());
        }, Sort.by(Sort.Order.desc("requestTime")), "id", "requestTime");
        JsonNode rows = ApiUtil.rows(result);
        if (rows.isEmpty()) return result;
        List<DataPermitLog> permitLogList = dataPermitLogDao.findAll((Specification<DataPermitLog>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(root.get("logId").in(DPUtil.values(rows, Integer.class, "id")));
            return cb.and(predicates.toArray(new Predicate[0]));
        });
        DPUtil.formatRelation(rows, DPUtil.toJSON(permitLogList), "id", "logId", "permitted");
        return result;
    }

    public Map<String, Object> record(HttpServletRequest request, JsonNode logParams, JsonNode requestParams, Collection<String> permits) {
        if (permits.isEmpty()) return ApiUtil.result(1001, "数据权限标识不能为空", permits);
        int uid = rbacService.uid(request);
        DataLog dataLog = new DataLog();
        dataLog.setPermits(DPUtil.implode(permits));
        dataLog.setServerId(logParams.at("/appName").asText());
        dataLog.setClientId(String.valueOf(uid));
        dataLog.setRequestUrl(logParams.at("/requestUrl").asText());
        dataLog.setRequestIp(logParams.at("/requestIp").asText());
        dataLog.setRequestHeaders(logParams.at("/requestHeaders").toString());
        dataLog.setRequestParams(DPUtil.stringify(requestParams));
        dataLog.setRequestTime(System.currentTimeMillis());
        dataLog = dataLogDao.save(dataLog);
        ObjectNode result = DPUtil.objectNode();
        Set<Integer> roleIds = rbacService.roleIds(request);
        if (roleIds.isEmpty()) {
            return ApiUtil.result(0, "用户暂未分配任何角色", result);
        }
        Map<Integer, Data> dataMap = DPUtil.list2map(dataDao.findAll((Specification<Data>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(root.get("serial").in(permits));
            predicates.add(cb.equal(root.get("status"), 1));
            return cb.and(predicates.toArray(new Predicate[0]));
        }), Integer.class, "id");
        if (dataMap.isEmpty()) {
            return ApiUtil.result(0, "数据模型暂不可用", result);
        }
        Map<Integer, List<DataPermit>> permitListMap = DPUtil.list2ml(dataPermitDao.findAll((Specification<DataPermit>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(root.get("dataId").in(dataMap.keySet()));
            predicates.add(root.get("roleId").in(roleIds));
            predicates.add(cb.equal(root.get("status"), 1));
            return cb.and(predicates.toArray(new Predicate[0]));
        }), Integer.class, "dataId");
        if (permitListMap.isEmpty()) {
            return ApiUtil.result(0, "对应数据标识无权限", result);
        }
        List<DataPermitLog> list = new ArrayList<>();
        for (Map.Entry<Integer, List<DataPermit>> entry : permitListMap.entrySet()) {
            Data data = dataMap.get(entry.getKey());
            DataPermitLog permitLog = new DataPermitLog();
            permitLog.setLogId(dataLog.getId());
            permitLog.setDataId(data.getId());
            permitLog.setDataSerial(data.getSerial());
            ArrayNode filters = DPUtil.arrayNode();
            Set<String> fields = new HashSet<>();
            for (DataPermit permit : entry.getValue()) {
                JsonNode json = DPUtil.parseJSON(permit.getFilters());
                if (!DPUtil.empty(json)) filters.add(json);
                fields.addAll(Arrays.asList(DPUtil.explode(permit.getFields())));
            }
            permitLog.setFilters(filters.toString());
            permitLog.setFields(DPUtil.implode(fields));
            list.add(permitLog);
            ObjectNode node = result.putObject(data.getSerial());
            JsonNode columns = DPUtil.json2object(DPUtil.parseJSON(data.getFields()), "name");
            columns = DPUtil.arrayNode(DPUtil.filterByKey(columns, true, fields));
            node.replace("pks", DPUtil.toJSON(DPUtil.explode(data.getPks())));
            node.replace("filters", filters);
            node.replace("fields", columns);
        }
        dataPermitLogDao.saveAll(list);
        return ApiUtil.result(0, null, result);
    }

    public boolean remove(List<Long> ids) {
        if(null == ids || ids.isEmpty()) return false;
        dataPermitLogDao.deleteByLogIds(ids);
        dataLogDao.deleteInBatch(dataLogDao.findAllById(ids));
        return true;
    }

}
