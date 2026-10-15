package com.iisquare.fs.web.lm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.core.mvc.FeignInterceptor;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.lm.dao.AuthDao;
import com.iisquare.fs.web.lm.dao.CreditDao;
import com.iisquare.fs.web.lm.dao.RateDao;
import com.iisquare.fs.web.lm.entity.Auth;
import com.iisquare.fs.web.lm.entity.Credit;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CreditService extends JPAServiceBase {

    @Autowired
    CreditDao creditDao;
    @Autowired
    DefaultRbacService rbacService;
    @Autowired
    RateService rateService;
    @Autowired
    AuthDao authDao;
    @Autowired
    RateDao rateDao;

    @Override
    public Map<String, String> sorts() {
        Map<String, String> sorts = new LinkedHashMap<>();
        sorts.put("uid", "desc");
        sorts.put("status", "asc");
        sorts.put("sort", "desc");
        sorts.put("remained", "asc");
        sorts.put("consumed", "asc");
        return sorts;
    }

    public Map<?, ?> status() {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "启用");
        status.put(2, "禁用");
        return status;
    }

    /**
     * 按密钥解析认证信息（不含用户代理）
     */
    public ObjectNode authByKey(String key, ObjectNode cache) {
        return authByKey(key, null, cache);
    }

    /**
     * 按密钥解析认证信息：密钥自身（id / name / secret / 可用模型 / 限流）与记账用户。
     *
     * 密钥开启「允许用户代理」（proxyable）且调用方带上用户会话令牌（请求头 x-auth-token）时，
     * 直接把生效用户切成令牌对应的会话用户：身份、积分与限流都取该用户，密钥只是代理入口
     * （不再查密钥属主，省掉一次无效请求）。会话失效或用户不可用时返回 null（等同认证失败）。
     */
    public ObjectNode authByKey(String key, HttpServletRequest request, ObjectNode cache) {
        if (DPUtil.empty(key)) return null;
        Auth auth = authDao.findOne((Specification<Auth>) (root, query, cb) -> cb.and(
                cb.equal(root.get("secret"), key), cb.equal(root.get("status"), "valid"))).orElse(null);
        if (null == auth) return null;
        Long expiredTime = auth.getExpiredTime();
        if (null != expiredTime && expiredTime > 0 && expiredTime <= System.currentTimeMillis()) return null;
        // 生效用户：默认密钥属主；密钥允许代理且调用端带了会话令牌时，按令牌切换（积分判定与消耗都以生效用户为准）
        String session = 1 == DPUtil.parseInt(auth.getProxyable()) && null != request
                ? DPUtil.parseString(request.getHeader(FeignInterceptor.HEADER_AUTH_TOKEN)) : "";
        JsonNode identity;
        int uid = auth.getUid();
        if (DPUtil.empty(session)) { // 未带会话令牌（定时任务、服务端编排等）：沿用密钥所属用户
            identity = rbacService.identityById(uid);
        } else {
            identity = rbacService.identityByToken(session);
            uid = null == identity ? 0 : identity.at("/id").asInt(0);
            if (uid < 1) return null; // 会话已失效或用户不可用：等同认证失败，避免把用量记到密钥所属用户头上
        }
        if (null == identity || identity.isNull() || identity.isEmpty()) return null;
        JsonNode roles = identity.at("/roles");
        if (null == roles || roles.isEmpty()) return null;
        Credit credit = info(uid); // 只取生效用户的这条积分记录：余额、限流策略都以它为准
        if (null == credit || 1 != credit.getStatus()) return null;
        ObjectNode result = DPUtil.objectNode();
        result.put("id", auth.getId());
        result.put("name", auth.getName());
        result.put("uid", uid);
        result.put("secret", auth.getSecret());
        result.put("proxyable", DPUtil.parseBoolean(auth.getProxyable()));
        result.replace("identity", identity);
        result.putObject("credit")
                .put("uid", uid)
                .put("remained", credit.getRemained())
                .put("consumed", credit.getConsumed())
                .put("remindEnabled", DPUtil.parseBoolean(credit.getRemindEnabled()))
                .put("remindThreshold", credit.getRemindThreshold());
        ObjectNode rates = result.putObject("rates");
        for (Integer rateId : DPUtil.parseIntList(credit.getRateIds())) { // 限流策略与计数都归生效用户
            JsonNode rate = cache.at("/rates/" + rateId);
            if (!rate.isEmpty()) {
                rates.replace(String.valueOf(rateId), rate);
            }
        }
        ObjectNode models = result.putObject("models");
        Set<Integer> authModelIds = new HashSet<>(DPUtil.parseIntList(auth.getModelIds()));
        Set<Integer> userRoleIds = new HashSet<>(DPUtil.parseIntList(DPUtil.fields(roles)));
        for (Map.Entry<String, JsonNode> entry : cache.at("/models").properties()) {
            JsonNode model = entry.getValue();
            int modelId = model.at("/id").asInt();
            if (!authModelIds.isEmpty() && !authModelIds.contains(modelId)) continue;
            List authRoleIds = DPUtil.toJSON(model.at("/roleIds"), List.class);
            if (!authRoleIds.isEmpty() && Collections.disjoint(userRoleIds, authRoleIds)) continue;
            String place = model.at("/alias").asText();
            if (DPUtil.empty(place)) place = model.at("/name").asText();
            (models.has(place) ? (ArrayNode) models.at(place) : models.putArray(place)).add(modelId);
        }
        return result;
    }

    public Credit info(Integer uid) {
        return info(creditDao, uid);
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        int uid = DPUtil.parseInt(param.get("uid"));
        if (uid < 1) return ApiUtil.result(1001, "用户ID异常", uid);
        int status = DPUtil.parseInt(param.get("status"));
        if (!status().containsKey(status)) return ApiUtil.result(1002, "状态异常", status);
        Credit info = info(uid);
        if (null != info) {
            if (!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
        } else {
            if (!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new Credit();
            info.setUid(uid);
        }
        info.setRemained(DPUtil.parseDecimal(param.get("remained")));
        info.setConsumed(DPUtil.parseDecimal(param.get("consumed")));
        info.setRateIds(DPUtil.implode(",", DPUtil.parseIntList(param.get("rateIds"))));
        info.setRemindEnabled(DPUtil.parseBoolean(param.get("remindEnabled")) ? 1 : 0);
        info.setRemindThreshold(DPUtil.parseDouble(param.get("remindThreshold")));
        info.setSort(DPUtil.parseInt(param.get("sort")));
        info.setStatus(status);
        info.setDescription(DPUtil.parseString(param.get("description")));
        info = save(creditDao, info, rbacService.uid(request));
        return ApiUtil.result(0, null, info);
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(creditDao, param, (root, query, cb) -> {
            SpecificationHelper<Credit> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.equalWithIntGTZero("uid").equalWithIntNotEmpty("status");
            return cb.and(helper.predicates());
        }, Sort.by(Sort.Order.desc("sort"), Sort.Order.desc("uid")), sorts().keySet());
        JsonNode rows = format(ApiUtil.rows(result));
        if (!DPUtil.empty(args.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "uid", "createdUid", "updatedUid");
        }
        if (!DPUtil.empty(args.get("withStatusText"))) {
            fillStatus(rows, status());
        }
        if (!DPUtil.empty(args.get("withRates"))) {
            rateService.fillInfos(rows, "rateIds");
        }
        return result;
    }

    public JsonNode format(JsonNode rows) {
        for (JsonNode row : rows) {
            ObjectNode node = (ObjectNode) row;
            int remindEnabled = node.at("/remindEnabled").asInt(0);
            node.put("remindEnabled", 1 == remindEnabled);
            List<Integer> ids = DPUtil.parseIntList(node.at("/rateIds").asText(""));
            node.replace("rateIds", DPUtil.toJSON(ids));
        }
        return rows;
    }

    public Map<String, Object> remove(List<Integer> uids) {
        return remove(creditDao, uids);
    }

}
