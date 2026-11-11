package com.iisquare.fs.web.spider.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.spider.dao.ProxyChannelDao;
import com.iisquare.fs.web.spider.dao.ProxyGroupDao;
import com.iisquare.fs.web.spider.dao.TemplateDao;
import com.iisquare.fs.web.spider.entity.ProxyChannel;
import com.iisquare.fs.web.spider.entity.ProxyGroup;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ProxyGroupService extends JPAServiceBase {

    @Autowired
    ProxyGroupDao groupDao;
    @Autowired
    ProxyChannelDao channelDao;
    @Autowired
    TemplateDao templateDao;
    @Autowired
    ProxyChannelService channelService;
    @Autowired
    DefaultRbacService rbacService;

    @Override
    public Map<String, String> sorts() {
        Map<String, String> sorts = new LinkedHashMap<>();
        sorts.put("id", "desc");
        sorts.put("status", "asc");
        sorts.put("sort", "desc");
        return sorts;
    }

    public Map<Integer, String> status() {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "正常");
        status.put(2, "禁用");
        return status;
    }

    public Map<String, String> strategies() {
        Map<String, String> strategies = new LinkedHashMap<>();
        strategies.put("round_robin", "轮询");
        strategies.put("random", "随机");
        strategies.put("least_used", "最少使用");
        return strategies;
    }

    public ProxyGroup info(Integer id) {
        return info(groupDao, id);
    }

    public ProxyGroup byCode(String code) {
        if (DPUtil.empty(code)) return null;
        List<ProxyGroup> list = groupDao.findAll((Specification<ProxyGroup>) (root, query, cb) ->
                cb.equal(root.get("code"), code.trim()));
        return list.isEmpty() ? null : list.get(0);
    }

    public List<ProxyGroup> all() {
        return groupDao.findAll(Sort.by(Sort.Order.desc("sort"), Sort.Order.desc("id")));
    }

    public ProxyChannel channel(ProxyGroup group) {
        return null == group || null == group.getChannelId() ? null : channelService.info(group.getChannelId());
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(groupDao, param, (Specification<ProxyGroup>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            int id = DPUtil.parseInt(param.get("id"));
            if (id > 0) predicates.add(cb.equal(root.get("id"), id));
            if (!DPUtil.empty(param.get("status"))) {
                predicates.add(cb.equal(root.get("status"), DPUtil.parseInt(param.get("status"))));
            }
            int channelId = DPUtil.parseInt(param.get("channelId"));
            if (channelId > 0) predicates.add(cb.equal(root.get("channelId"), channelId));
            String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
            if (!DPUtil.empty(name)) predicates.add(cb.like(root.get("name"), "%" + name + "%"));
            String code = DPUtil.trim(DPUtil.parseString(param.get("code")));
            if (!DPUtil.empty(code)) predicates.add(cb.like(root.get("code"), "%" + code + "%"));
            return cb.and(predicates.toArray(new Predicate[0]));
        }, Sort.by(Sort.Order.desc("sort"), Sort.Order.desc("id")), sorts().keySet());
        JsonNode rows = format(ApiUtil.rows(result));
        if (!DPUtil.empty(args.get("withUserInfo"))) rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        if (!DPUtil.empty(args.get("withStatusText"))) fillStatus(rows, status());
        if (!DPUtil.empty(args.get("withChannelInfo"))) fillInfo(channelDao, rows, "channelId");
        return result;
    }

    public Map<String, Object> options() {
        List<ProxyGroup> list = groupDao.findAll((Specification<ProxyGroup>) (root, query, cb) ->
                cb.equal(root.get("status"), 1), Sort.by(Sort.Order.desc("sort"), Sort.Order.desc("id")));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (ProxyGroup group : list) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", group.getId());
            item.put("name", group.getName());
            item.put("code", group.getCode());
            item.put("protocol", DPUtil.empty(group.getProtocol()) ? "http" : group.getProtocol());
            item.put("strategy", group.getStrategy());
            rows.add(item);
        }
        return ApiUtil.result(0, null, rows);
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if (DPUtil.empty(name)) return ApiUtil.result(1001, "分组名称不能为空", name);
        String code = DPUtil.trim(DPUtil.parseString(param.get("code"))).toLowerCase(Locale.ROOT);
        if (DPUtil.empty(code) || !code.matches("[a-z0-9][a-z0-9_-]{1,63}")) {
            return ApiUtil.result(1002, "分组标识需为2-64位小写字母、数字、下划线或中划线", code);
        }
        ProxyGroup exists = byCode(code);
        if (null != exists && !Objects.equals(exists.getId(), id)) {
            return ApiUtil.result(1003, "分组标识已存在", code);
        }
        ProxyChannel channel = channelService.info(DPUtil.parseInt(param.get("channelId")));
        if (null == channel) return ApiUtil.result(1004, "所属渠道不存在", null);
        int status = DPUtil.parseInt(param.get("status"));
        if (!status().containsKey(status)) return ApiUtil.result(1005, "状态异常", status);
        String strategy = DPUtil.trim(DPUtil.parseString(param.get("strategy"))).toLowerCase(Locale.ROOT);
        if (DPUtil.empty(strategy)) strategy = "round_robin";
        if (!strategies().containsKey(strategy)) return ApiUtil.result(1006, "调度策略异常", strategy);
        ProxyGroup info;
        if (id > 0) {
            if (!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if (null == info) return ApiUtil.result(404, null, id);
        } else {
            if (!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new ProxyGroup();
        }
        info.setName(name);
        info.setCode(code);
        info.setChannelId(channel.getId());
        info.setProtocol(channelService.normalizeProtocol(param.get("protocol")));
        info.setRegion(DPUtil.parseString(param.get("region")));
        info.setStrategy(strategy);
        info.setBatchSize(atLeastOne(param.get("batchSize"), 1));
        info.setLeaseSeconds(atLeastOne(param.get("leaseSeconds"), 300));
        info.setStickySeconds(atLeastOne(param.get("stickySeconds"), 1800));
        info.setMaxUses(atLeastOne(param.get("maxUses"), 100));
        info.setConcurrency(nonNegative(param.get("concurrency"), 0));
        info.setConnectTimeout(atLeastOne(param.get("connectTimeout"), 10000));
        info.setSocketTimeout(atLeastOne(param.get("socketTimeout"), 30000));
        info.setFailureThreshold(atLeastOne(param.get("failureThreshold"), 3));
        info.setAccessKey(DPUtil.trim(DPUtil.parseString(param.get("accessKey"))));
        info.setSort(DPUtil.parseInt(param.get("sort")));
        info.setStatus(status);
        info.setDescription(DPUtil.parseString(param.get("description")));
        info = save(groupDao, info, rbacService.uid(request));
        return ApiUtil.result(0, null, info);
    }

    public JsonNode format(JsonNode rows) {
        return rows;
    }

    public JsonNode fillInfo(JsonNode rows, String... properties) {
        return fillInfo(groupDao, rows, properties);
    }

    public Map<String, Object> remove(List<Integer> ids) {
        if (null == ids || ids.isEmpty()) return ApiUtil.result(0, null, 0);
        long references = templateDao.count((Specification<com.iisquare.fs.web.spider.entity.Template>)
                (root, query, cb) -> root.get("proxyGroupId").in(ids));
        if (references > 0) {
            return ApiUtil.result(1403, "代理分组已被模板引用，请先调整模板配置", references);
        }
        return remove(groupDao, ids);
    }

    private int atLeastOne(Object value, int defaultValue) {
        int number = DPUtil.parseInt(value);
        return number > 0 ? number : defaultValue;
    }

    private int nonNegative(Object value, int defaultValue) {
        int number = DPUtil.parseInt(value);
        return number >= 0 ? number : defaultValue;
    }

}
