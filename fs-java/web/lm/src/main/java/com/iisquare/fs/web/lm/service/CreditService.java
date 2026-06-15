package com.iisquare.fs.web.lm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
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
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CreditService extends JPAServiceBase {

    @Autowired
    private CreditDao creditDao;
    @Autowired
    private DefaultRbacService rbacService;
    @Autowired
    private RateService rateService;
    @Autowired
    private AuthDao authDao;
    @Autowired
    private RateDao rateDao;

    public Map<?, ?> status() {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "启用");
        status.put(2, "禁用");
        return status;
    }

    public ObjectNode authByKey(String key, ObjectNode cache) {
        if (DPUtil.empty(key)) return null;
        Auth auth = authDao.findOne((Specification<Auth>) (root, query, cb) -> cb.and(
                cb.equal(root.get("secret"), key), cb.equal(root.get("status"), "valid"))).orElse(null);
        if (null == auth) return null;
        Long expiredTime = auth.getExpiredTime();
        if (null != expiredTime && expiredTime > 0 && expiredTime <= System.currentTimeMillis()) return null;
        Credit credit = info(auth.getUid());
        if (null == credit || 1 != credit.getStatus()) return null;
        JsonNode identity = rbacService.identity(auth.getUid());
        if (identity.isEmpty()) return null;
        if (1 != identity.at("/status").asInt()) return null;
        ObjectNode result = DPUtil.objectNode();
        result.put("id", auth.getId());
        result.put("name", auth.getName());
        result.put("uid", auth.getUid());
        result.put("secret", auth.getSecret());
        ObjectNode rates = result.putObject("credit")
                .put("uid", credit.getUid())
                .put("remained", credit.getRemained())
                .put("consumed", credit.getConsumed())
                .put("remindEnabled", DPUtil.parseBoolean(credit.getRemindEnabled()))
                .put("remindThreshold", credit.getRemindThreshold())
                .putObject("rates");
        List<Rate> rateList = rateDao.findAllById(DPUtil.parseIntList(credit.getRateIds()));
        for (Rate rate : rateList) {
            if (1 != rate.getStatus()) continue;
            ObjectNode item = rates.putObject(String.valueOf(rate.getId()));
            item.put("id", rate.getId());
            item.put("name", rate.getName());
            item.put("requestCount", rate.getRequestCount());
            item.put("requestInterval", rate.getRequestInterval());
            item.put("tokenCount", rate.getTokenCount());
            item.put("tokenInterval", rate.getTokenInterval());
            item.put("creditCount", rate.getCreditCount());
            item.put("creditInterval", rate.getCreditInterval());
        }
        result.replace("identity", identity);
        ObjectNode models = result.putObject("models");
        Set<Integer> authModelIds = new HashSet<>(DPUtil.parseIntList(auth.getModelIds()));
        Set<Integer> userRoleIds = new HashSet<>(DPUtil.parseIntList(DPUtil.fields(identity.at("/roles"))));
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
        }, Sort.by(Sort.Order.desc("sort")), "uid", "status", "sort");
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

    public boolean remove(List<Integer> uids) {
        return remove(creditDao, uids);
    }

}
