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
import com.iisquare.fs.web.spider.entity.ProxyChannel;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ProxyChannelService extends JPAServiceBase {

    @Autowired
    ProxyChannelDao channelDao;
    @Autowired
    ProxyGroupDao groupDao;
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

    public Map<String, String> adapters() {
        Map<String, String> adapters = new LinkedHashMap<>();
        adapters.put("static", "静态资源池");
        adapters.put("generic", "通用 HTTP API");
        adapters.put("kuaidaili", "快代理");
        adapters.put("juliang", "巨量IP");
        adapters.put("zhima", "芝麻代理");
        adapters.put("ipidea", "IPIDEA");
        adapters.put("brightdata", "Bright Data");
        return adapters;
    }

    public Map<String, String> protocols() {
        Map<String, String> protocols = new LinkedHashMap<>();
        protocols.put("http", "HTTP");
        protocols.put("https", "HTTPS");
        protocols.put("socks5", "SOCKS5");
        return protocols;
    }

    public Map<String, Object> config() {
        ObjectNode templates = DPUtil.objectNode();
        templates.set("static", DPUtil.parseJSON("{\"resources\":[\"http://host:port\",\"host:port:username:password\"]}"));
        templates.set("generic", DPUtil.parseJSON("{\"method\":\"GET\",\"responseType\":\"json\",\"listPath\":\"/data/list\",\"hostField\":\"ip\",\"portField\":\"port\",\"usernameField\":\"username\",\"passwordField\":\"password\",\"expireField\":\"expire_at\",\"params\":{},\"headers\":{}}"));
        templates.set("kuaidaili", DPUtil.parseJSON("{\"method\":\"GET\",\"responseType\":\"json\",\"listPath\":\"/data/proxy_list\",\"params\":{\"num\":\"${count}\",\"format\":\"json\"}}"));
        templates.set("juliang", DPUtil.parseJSON("{\"method\":\"GET\",\"responseType\":\"json\",\"listPath\":\"/data/list\",\"params\":{\"num\":\"${count}\",\"format\":\"json\"}}"));
        templates.set("zhima", DPUtil.parseJSON("{\"method\":\"GET\",\"responseType\":\"json\",\"listPath\":\"/data/proxy_list\",\"params\":{\"count\":\"${count}\"}}"));
        templates.set("ipidea", DPUtil.parseJSON("{\"method\":\"GET\",\"responseType\":\"json\",\"listPath\":\"/data/list\",\"params\":{\"num\":\"${count}\",\"protocol\":\"${protocol}\"}}"));
        templates.set("brightdata", DPUtil.parseJSON("{\"method\":\"GET\",\"responseType\":\"text\",\"textSeparator\":\"\\n\",\"params\":{}}"));
        ObjectNode result = DPUtil.objectNode();
        result.putPOJO("status", status());
        result.putPOJO("sorts", sorts());
        result.putPOJO("adapters", adapters());
        result.putPOJO("protocols", protocols());
        result.set("templates", templates);
        return ApiUtil.result(0, null, result);
    }

    public ProxyChannel info(Integer id) {
        return info(channelDao, id);
    }

    public ProxyChannel preview(Map<?, ?> param) {
        String adapter = DPUtil.trim(DPUtil.parseString(param.get("adapter"))).toLowerCase(Locale.ROOT);
        if (!adapters().containsKey(adapter)) throw new IllegalArgumentException("接入适配器异常");
        String credentials = json(param.get("credentials"));
        String config = json(param.get("config"));
        if (null == credentials || null == config) throw new IllegalArgumentException("认证信息或提取配置不是合法 JSON");
        ProxyChannel channel = new ProxyChannel();
        channel.setId(0);
        channel.setName(DPUtil.parseString(param.get("name")));
        channel.setAdapter(adapter);
        channel.setProtocol(normalizeProtocol(param.get("protocol")));
        channel.setEndpoint(DPUtil.trim(DPUtil.parseString(param.get("endpoint"))));
        channel.setCredentials(credentials);
        channel.setConfig(config);
        channel.setStatus(1);
        return channel;
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if (DPUtil.empty(name)) return ApiUtil.result(1001, "渠道名称不能为空", name);
        String adapter = DPUtil.trim(DPUtil.parseString(param.get("adapter"))).toLowerCase(Locale.ROOT);
        if (!adapters().containsKey(adapter)) return ApiUtil.result(1002, "接入适配器异常", adapter);
        String protocol = normalizeProtocol(param.get("protocol"));
        if (!protocols().containsKey(protocol)) return ApiUtil.result(1003, "代理协议异常", protocol);
        int status = DPUtil.parseInt(param.get("status"));
        if (!status().containsKey(status)) return ApiUtil.result(1004, "状态异常", status);
        ProxyChannel info;
        if (id > 0) {
            if (!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if (null == info) return ApiUtil.result(404, null, id);
        } else {
            if (!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new ProxyChannel();
        }
        info.setName(name);
        info.setAdapter(adapter);
        info.setProtocol(protocol);
        info.setEndpoint(DPUtil.trim(DPUtil.parseString(param.get("endpoint"))));
        String credentials = json(param.get("credentials"));
        if (null == credentials) return ApiUtil.result(1005, "认证信息必须是合法 JSON", null);
        String config = json(param.get("config"));
        if (null == config) return ApiUtil.result(1006, "提取配置必须是合法 JSON", null);
        info.setCredentials(credentials);
        info.setConfig(config);
        info.setSort(DPUtil.parseInt(param.get("sort")));
        info.setStatus(status);
        info.setDescription(DPUtil.parseString(param.get("description")));
        info = save(channelDao, info, rbacService.uid(request));
        return ApiUtil.result(0, null, info);
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(channelDao, param, (Specification<ProxyChannel>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            int id = DPUtil.parseInt(param.get("id"));
            if (id > 0) predicates.add(cb.equal(root.get("id"), id));
            if (!DPUtil.empty(param.get("status"))) {
                predicates.add(cb.equal(root.get("status"), DPUtil.parseInt(param.get("status"))));
            }
            String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
            if (!DPUtil.empty(name)) predicates.add(cb.like(root.get("name"), "%" + name + "%"));
            String adapter = DPUtil.parseString(param.get("adapter"));
            if (!DPUtil.empty(adapter)) predicates.add(cb.equal(root.get("adapter"), adapter));
            return cb.and(predicates.toArray(new Predicate[0]));
        }, Sort.by(Sort.Order.desc("sort"), Sort.Order.desc("id")), sorts().keySet());
        JsonNode rows = format(ApiUtil.rows(result));
        if (!DPUtil.empty(args.get("withUserInfo"))) rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        if (!DPUtil.empty(args.get("withStatusText"))) fillStatus(rows, status());
        if (!DPUtil.empty(args.get("withAdapterText"))) DPUtil.fillValues(rows, "adapter", "adapterText", adapters());
        return result;
    }

    public JsonNode format(JsonNode rows) {
        for (JsonNode row : rows) {
            ObjectNode node = (ObjectNode) row;
            node.replace("credentials", DPUtil.parseJSON(node.at("/credentials").asText("{}"), j -> DPUtil.objectNode()));
            node.replace("config", DPUtil.parseJSON(node.at("/config").asText("{}"), j -> DPUtil.objectNode()));
        }
        return rows;
    }

    public Map<String, Object> remove(List<Integer> ids) {
        if (null == ids || ids.isEmpty()) return ApiUtil.result(0, null, 0);
        long references = groupDao.count((Specification<com.iisquare.fs.web.spider.entity.ProxyGroup>)
                (root, query, cb) -> root.get("channelId").in(ids));
        if (references > 0) {
            return ApiUtil.result(1403, "代理渠道已被分组引用，请先调整分组配置", references);
        }
        return remove(channelDao, ids);
    }

    public String normalizeProtocol(Object value) {
        String protocol = DPUtil.trim(DPUtil.parseString(value)).toLowerCase(Locale.ROOT);
        return DPUtil.empty(protocol) ? "http" : protocol;
    }

    private String json(Object value) {
        if (null == value || DPUtil.empty(DPUtil.parseString(value))) return "{}";
        String text = value instanceof String ? DPUtil.parseString(value) : DPUtil.stringify(value);
        JsonNode node = DPUtil.parseJSON(text);
        return null == node ? null : DPUtil.stringify(node);
    }

}
