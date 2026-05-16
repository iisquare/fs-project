package com.iisquare.fs.web.lm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.lm.dao.CreditDao;
import com.iisquare.fs.web.lm.entity.Credit;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CreditService extends JPAServiceBase {

    @Autowired
    private CreditDao creditDao;
    @Autowired
    private DefaultRbacService rbacService;
    @Autowired
    private RateService rateService;

    public Map<?, ?> status() {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "启用");
        status.put(2, "禁用");
        return status;
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
        info.setRemained(DPUtil.parseDouble(param.get("remained")));
        info.setConsumed(DPUtil.parseDouble(param.get("consumed")));
        info.setRateIds(DPUtil.implode(",", DPUtil.parseIntList(param.get("rateIds"))));
        info.setReminderEnabled(DPUtil.parseBoolean(param.get("reminderEnabled")) ? 1 : 0);
        info.setReminderThreshold(DPUtil.parseDouble(param.get("reminderThreshold")));
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
            int reminderEnabled = node.at("/reminderEnabled").asInt(0);
            node.put("reminderEnabled", 1 == reminderEnabled);
            List<Integer> ids = DPUtil.parseIntList(node.at("/rateIds").asText(""));
            node.replace("rateIds", DPUtil.toJSON(ids));
        }
        return rows;
    }

    public boolean remove(List<Integer> uids) {
        return remove(creditDao, uids);
    }

}
