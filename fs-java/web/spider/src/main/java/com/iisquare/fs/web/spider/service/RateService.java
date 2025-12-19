package com.iisquare.fs.web.spider.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.spider.core.ZooRate;
import com.iisquare.fs.web.spider.dao.RateDao;
import com.iisquare.fs.web.spider.entity.*;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RateService extends JPAServiceBase {

    @Autowired
    private RateDao rateDao;
    @Autowired
    TemplateService templateService;
    @Autowired
    private DefaultRbacService rbacService;
    @Autowired
    NodeService nodeService;

    public Map<?, ?> status() {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "正常");
        status.put(2, "禁用");
        return status;
    }

    public Rate info(Integer id) {
        return info(rateDao, id);
    }

    public JsonNode fillInfo(JsonNode json, String ...properties) {
        return fillInfo(rateDao, json, properties);
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if(DPUtil.empty(name)) return ApiUtil.result(1001, "名称异常", name);
        int status = DPUtil.parseInt(param.get("status"));
        if(!status().containsKey(status)) return ApiUtil.result(1003, "状态异常", status);
        Rate info;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new Rate();
        }
        info.setName(name);
        info.setParallelByKey(DPUtil.parseInt(param.get("parallelByKey")));
        info.setConcurrent(DPUtil.parseInt(param.get("concurrent")));
        info.setIntervalMillisecond(DPUtil.parseInt(param.get("intervalMillisecond")));
        info.setPerJob(DPUtil.parseBoolean(param.get("perJob")) ? 1 : 0);
        info.setPerDomain(DPUtil.parseBoolean(param.get("perDomain")) ? 1 : 0);
        info.setPerNode(DPUtil.parseBoolean(param.get("perNode")) ? 1 : 0);
        info.setPerProxy(DPUtil.parseBoolean(param.get("perProxy")) ? 1 : 0);
        info.setHaltToken(DPUtil.parseBoolean(param.get("haltToken")) ? 1 : 0);
        info.setHaltTask(DPUtil.parseBoolean(param.get("haltTask")) ? 1 : 0);
        info.setSort(DPUtil.parseInt(param.get("sort")));
        info.setStatus(status);
        info.setDescription(DPUtil.parseString(param.get("description")));
        info = save(rateDao, info, rbacService.uid(request));
        return ApiUtil.result(0, null, info);
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(rateDao, param, (Specification<Rate>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            int id = DPUtil.parseInt(param.get("id"));
            if (id > 0) predicates.add(cb.equal(root.get("id"), id));
            int status = DPUtil.parseInt(param.get("status"));
            if (!"".equals(DPUtil.parseString(param.get("status")))) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
            if (!DPUtil.empty(name)) {
                predicates.add(cb.like(root.get("name"),  name));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, Sort.by(Sort.Order.desc("sort")), "id", "status", "sort");
        JsonNode rows = format(ApiUtil.rows(result));
        if(!DPUtil.empty(args.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(args.get("withStatusText"))) {
            fillStatus(rows, status());
        }
        return result;
    }

    public JsonNode format(JsonNode rows) {
        for (JsonNode row : rows) {
            ObjectNode node = (ObjectNode) row;
            node.put("perJob", DPUtil.parseBoolean(row.at("/perJob").asInt()));
            node.put("perDomain", DPUtil.parseBoolean(row.at("/perDomain").asInt()));
            node.put("perNode", DPUtil.parseBoolean(row.at("/perNode").asInt()));
            node.put("perProxy", DPUtil.parseBoolean(row.at("/perProxy").asInt()));
            node.put("haltToken", DPUtil.parseBoolean(row.at("/haltToken").asInt()));
            node.put("haltTask", DPUtil.parseBoolean(row.at("/haltTask").asInt()));
        }
        return rows;
    }

    public boolean remove(List<Integer> ids) {
        return remove(rateDao, ids);
    }

    public Map<String, Object> publish(Map<?, ?> param, HttpServletRequest request) {
        Rate rate = info(DPUtil.parseInt(param.get("id")));
        if (null == rate || 1 != rate.getStatus()) {
            return ApiUtil.result(1401, "信息不存在或已禁用", null);
        }
        ZooRate record = ZooRate.record(rate);
        rate.setPublishedTime(System.currentTimeMillis());
        rate.setPublishedUid(rbacService.uid(request));
        rateDao.save(rate);
        boolean saved = nodeService.zookeeper().save(record);
        return ApiUtil.result(saved ? 0 : 500, null, rate);
    }

    public Map<String, Object> clear(Map<?, ?> param, HttpServletRequest request) {
        Rate rate = info(DPUtil.parseInt(param.get("id")));
        if (null == rate || 1 != rate.getStatus()) {
            return ApiUtil.result(1401, "信息不存在或已禁用", null);
        }
        boolean saved = nodeService.zookeeper().save("/runtime/rate/" + rate.getId(), null);
        if (!saved) {
            return ApiUtil.result(1501, "清理请求频率配置失败", rate);
        }
        rate.setPublishedTime(0L);
        rate.setPublishedUid(rbacService.uid(request));
        rate = rateDao.save(rate);
        return ApiUtil.result(0, null, rate);
    }

}
