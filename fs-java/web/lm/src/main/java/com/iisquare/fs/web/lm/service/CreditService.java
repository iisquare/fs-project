package com.iisquare.fs.web.lm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.lm.core.RedisKey;
import com.iisquare.fs.web.lm.dao.AuthDao;
import com.iisquare.fs.web.lm.dao.CreditDao;
import com.iisquare.fs.web.lm.dao.RateDao;
import com.iisquare.fs.web.lm.entity.Auth;
import com.iisquare.fs.web.lm.entity.Credit;
import com.iisquare.fs.web.lm.entity.Rate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
    @Autowired
    StringRedisTemplate redis;

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
        return authByKey(key, request, cache, true);
    }

    /**
     * 按密钥解析认证信息。
     *
     * requireCredit 为 true 时保持网关原有口径：生效用户必须有启用中的积分记录；
     * 为 false 时只校验密钥、会话与用户身份，供客户端展示积分概览使用——
     * 未开通或已停用的账号也要能拿到自己的状态，而不是被当成认证失败。
     */
    public ObjectNode authByKey(String key, HttpServletRequest request, ObjectNode cache, boolean requireCredit) {
        if (DPUtil.empty(key)) return null;
        Auth auth = authDao.findOne((Specification<Auth>) (root, query, cb) -> cb.and(
                cb.equal(root.get("secret"), key), cb.equal(root.get("status"), "valid"))).orElse(null);
        if (null == auth) return null;
        Long expiredTime = auth.getExpiredTime();
        if (null != expiredTime && expiredTime > 0 && expiredTime <= System.currentTimeMillis()) return null;
        // 生效用户：默认密钥属主；密钥允许代理且调用端带了会话令牌时，按令牌切换（积分判定与消耗都以生效用户为准）
        String session = 1 == DPUtil.parseInt(auth.getProxyable()) && null != request
                ? rbacService.token(request) : "";
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
        if (requireCredit && (null == credit || 1 != credit.getStatus())) return null;
        ObjectNode result = DPUtil.objectNode();
        result.put("id", auth.getId());
        result.put("name", auth.getName());
        result.put("uid", uid);
        result.put("secret", auth.getSecret());
        result.put("proxyable", DPUtil.parseBoolean(auth.getProxyable()));
        result.replace("identity", identity);
        ObjectNode creditNode = result.putObject("credit");
        creditNode.put("uid", uid);
        if (null == credit) {
            creditNode.put("remained", 0).put("consumed", 0).put("status", 0);
        } else {
            creditNode.put("remained", credit.getRemained())
                    .put("consumed", credit.getConsumed())
                    .put("remindEnabled", DPUtil.parseBoolean(credit.getRemindEnabled()))
                    .put("remindThreshold", credit.getRemindThreshold())
                    .put("status", credit.getStatus());
        }
        ObjectNode rates = result.putObject("rates");
        List<Integer> rateIds = null == credit ? Collections.emptyList() : DPUtil.parseIntList(credit.getRateIds());
        for (Integer rateId : rateIds) { // 限流策略与计数都归生效用户
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

    /**
     * 当前用户的积分概览：总消耗、总剩余，以及每个积分限流周期内的已用量与下次重置时间。
     *
     * 间隔用量取网关限流实际使用的 Redis 计数（键与窗口口径同 GatewayService），
     * 这样页面看到的数字与下一次请求是否会被拦截保持一致。
     */
    public ObjectNode summary(ObjectNode auth, ObjectNode cache) {
        ObjectNode result = DPUtil.objectNode();
        JsonNode credit = auth.at("/credit");
        int status = credit.at("/status").asInt(0);
        result.put("uid", auth.at("/uid").asInt());
        result.put("available", 1 == status);
        result.put("status", status);
        result.put("remained", DPUtil.parseDecimal(credit.at("/remained").asText("0")));
        result.put("consumed", DPUtil.parseDecimal(credit.at("/consumed").asText("0")));
        result.put("updatedTime", System.currentTimeMillis());

        ArrayNode intervals = result.putArray("intervals");
        int uid = auth.at("/uid").asInt();
        long now = System.currentTimeMillis();
        for (JsonNode rate : auth.at("/rates")) {
            int rateId = rate.at("/id").asInt();
            int interval = rate.at("/creditInterval").asInt(0);
            BigDecimal limit = DPUtil.parseDecimal(rate.at("/creditCount").asText("0"));
            if (rateId < 1 || interval <= 0 || limit.signum() <= 0) continue;
            long window = now / interval / 1000;
            BigDecimal used = DPUtil.parseDecimal(redis.opsForValue().get(
                    RedisKey.rate("credit", uid, rateId, window)));
            if (used.signum() < 0) used = BigDecimal.ZERO;
            BigDecimal remained = limit.subtract(used);
            if (remained.signum() < 0) remained = BigDecimal.ZERO;
            ObjectNode item = intervals.addObject();
            item.put("id", rateId);
            item.put("name", rate.at("/name").asText(""));
            item.put("limit", limit);
            item.put("used", used);
            item.put("remained", remained);
            item.put("interval", interval);
            item.put("beginTime", window * interval * 1000L);
            item.put("resetTime", (window + 1) * interval * 1000L);
        }
        return result;
    }

    /**
     * 管理端查看指定用户的限流用量。
     *
     * 返回当前用户关联的全部启用策略，每一条包含请求、词元、积分三个维度的
     * 当期用量、剩余额度与窗口重置时间。计数键与 GatewayService 实际限流一致。
     */
    public ObjectNode usage(int uid) {
        ObjectNode result = DPUtil.objectNode();
        Credit credit = info(uid);
        int status = null == credit ? 0 : DPUtil.parseInt(credit.getStatus());
        result.put("uid", uid);
        result.put("status", status);
        result.put("available", 1 == status);
        result.put("statusText", 1 == status ? "启用" : (2 == status ? "禁用" : "未开通"));
        result.put("remained", null == credit || null == credit.getRemained()
                ? BigDecimal.ZERO : credit.getRemained());
        result.put("consumed", null == credit || null == credit.getConsumed()
                ? BigDecimal.ZERO : credit.getConsumed());
        result.put("updatedTime", System.currentTimeMillis());
        ArrayNode limits = result.putArray("limits");
        if (null == credit) return result;

        long now = System.currentTimeMillis();
        for (Integer rateId : DPUtil.parseIntList(credit.getRateIds())) {
            Rate rate = rateService.info(rateId);
            if (null == rate || 1 != DPUtil.parseInt(rate.getStatus())) continue;
            appendLimit(limits, uid, now, rate, "request", "请求次数",
                    DPUtil.parseDouble(rate.getRequestCount()), rate.getRequestInterval(), "次");
            appendLimit(limits, uid, now, rate, "token", "词元用量",
                    DPUtil.parseDouble(rate.getTokenCount()), rate.getTokenInterval(), "词元");
            appendLimit(limits, uid, now, rate, "credit", "积分用量",
                    DPUtil.parseDouble(rate.getCreditCount()), rate.getCreditInterval(), "积分");
        }
        return result;
    }

    private void appendLimit(ArrayNode limits, int uid, long now, Rate rate, String type,
                             String typeText, double count, Integer intervalValue, String unit) {
        int interval = DPUtil.parseInt(intervalValue);
        if (count <= 0 || interval <= 0) return;
        long window = now / interval / 1000;
        BigDecimal limit = BigDecimal.valueOf(count);
        BigDecimal used = DPUtil.parseDecimal(redis.opsForValue().get(
                RedisKey.rate(type, uid, rate.getId(), window)));
        if (used.signum() < 0) used = BigDecimal.ZERO;
        BigDecimal remained = limit.subtract(used);
        if (remained.signum() < 0) remained = BigDecimal.ZERO;
        ObjectNode item = limits.addObject();
        item.put("rateId", rate.getId());
        item.put("rateName", rate.getName());
        item.put("type", type);
        item.put("typeText", typeText);
        item.put("limit", limit);
        item.put("used", used);
        item.put("remained", remained);
        item.put("unit", unit);
        item.put("interval", interval);
        item.put("beginTime", window * interval * 1000L);
        item.put("resetTime", (window + 1) * interval * 1000L);
    }

    /**
     * 手动重置指定用户当前窗口的限流计数。
     *
     * rateId 为 0 时重置该用户全部启用的限流策略，type 为空时同时重置
     * request / token / credit 三个维度。只清理可自动恢复的 Redis 计数器，
     * 不修改积分余额、总消耗与限流配置。
     */
    public Map<String, Object> reset(int uid, int rateId, String type, HttpServletRequest request) {
        if (uid < 1) return ApiUtil.result(1001, "用户ID异常", uid);
        Credit credit = info(uid);
        if (null == credit) return ApiUtil.result(404, "用户积分账户未开通", uid);
        if (!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);

        Set<String> supportedTypes = new LinkedHashSet<>(Arrays.asList("request", "token", "credit"));
        List<String> types;
        if (DPUtil.empty(type)) {
            types = new ArrayList<>(supportedTypes);
        } else {
            if (!supportedTypes.contains(type)) return ApiUtil.result(1002, "限流类型异常", type);
            types = Collections.singletonList(type);
        }

        List<Integer> rateIds = DPUtil.parseIntList(credit.getRateIds());
        if (rateId > 0) {
            if (!rateIds.contains(rateId)) return ApiUtil.result(1003, "限流策略不属于该用户", rateId);
            rateIds = Collections.singletonList(rateId);
        }

        long now = System.currentTimeMillis();
        Set<String> keys = new LinkedHashSet<>();
        for (Integer id : rateIds) {
            Rate rate = rateService.info(id);
            if (null == rate || 1 != DPUtil.parseInt(rate.getStatus())) continue;
            for (String itemType : types) {
                double count;
                int interval;
                switch (itemType) {
                    case "request" -> {
                        count = DPUtil.parseDouble(rate.getRequestCount());
                        interval = DPUtil.parseInt(rate.getRequestInterval());
                    }
                    case "token" -> {
                        count = DPUtil.parseDouble(rate.getTokenCount());
                        interval = DPUtil.parseInt(rate.getTokenInterval());
                    }
                    default -> {
                        count = DPUtil.parseDouble(rate.getCreditCount());
                        interval = DPUtil.parseInt(rate.getCreditInterval());
                    }
                }
                if (count <= 0 || interval <= 0) continue;
                long window = now / interval / 1000;
                keys.add(RedisKey.rate(itemType, uid, id, window));
                // 解除本窗口“已发送限流提醒”的标记，重置后再次触限仍可提醒。
                keys.add(RedisKey.remindLimited(itemType, uid));
            }
        }

        Long deleted = keys.isEmpty() ? 0L : redis.delete(keys);
        ObjectNode result = DPUtil.objectNode();
        result.put("uid", uid);
        result.put("rateId", rateId);
        result.put("type", type);
        result.put("deleted", null == deleted ? 0L : deleted);
        result.put("resetTime", now);
        return ApiUtil.result(0, null, result);
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
