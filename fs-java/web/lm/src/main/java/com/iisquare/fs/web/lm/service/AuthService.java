package com.iisquare.fs.web.lm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.lm.dao.AuthDao;
import com.iisquare.fs.web.lm.entity.Auth;
import com.iisquare.fs.web.lm.mvc.Configuration;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService extends JPAServiceBase {

    @Autowired
    AuthDao authDao;
    @Autowired
    DefaultRbacService rbacService;
    @Autowired
    Configuration configuration;
    @Autowired
    ModelService modelService;

    public Map<String, String> status() {
        Map<String, String> status = new LinkedHashMap<>();
        status.put("valid", "已启用");
        status.put("invalid", "已禁用");
        return status;
    }

    public Auth info(Integer id) {
        return info(authDao, id);
    }

    public JsonNode fillInfo(JsonNode rows, String... properties) {
        return fillInfo(authDao, rows, properties);
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if (DPUtil.empty(name)) return ApiUtil.result(1001, "名称异常", name);
        String status = DPUtil.parseString(param.get("status"));
        if (!status().containsKey(status)) return ApiUtil.result(1002, "状态异常", status);
        Auth info;
        if (id > 0) {
            if (!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if (null == info) return ApiUtil.result(404, null, id);
        } else {
            if (!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new Auth();
        }
        String secret = DPUtil.trim(DPUtil.parseString(param.get("secret")));
        if (DPUtil.empty(secret)) {
            secret = "sk-" + UUID.randomUUID().toString().replace("-", "");
        }
        info.setName(name);
        info.setUid(DPUtil.parseInt(param.get("uid")));
        info.setSecret(secret);
        info.setModelIds(DPUtil.implode(",", DPUtil.parseIntList(param.get("modelIds"))));
        info.setStatus(status);
        if(param.containsKey("expiredTime")) {
            String expiredTime =  DPUtil.trim(DPUtil.parseString(param.get("expiredTime")));
            if(DPUtil.empty(expiredTime)) {
                info.setExpiredTime(0L);
            } else {
                info.setExpiredTime(DPUtil.dateTime2millis(expiredTime, configuration.getFormatDate()));
            }
        }
        info = save(authDao, info, rbacService.uid(request));
        return ApiUtil.result(0, null, info);
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(authDao, param, (root, query, cb) -> {
            SpecificationHelper<Auth> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.dateFormat(configuration.getFormatDate()).equalWithIntGTZero("id").deleted();
            helper.equal("status").equalWithIntGTZero("uid").like("name").equal("secret");
            return cb.and(helper.predicates());
        }, Sort.by(Sort.Order.desc("id")), "id", "status", "createdTime", "updatedTime", "deletedTime");
        JsonNode rows = format(ApiUtil.rows(result));
        if (!DPUtil.empty(args.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "uid", "createdUid", "updatedUid", "deletedUid");
        }
        if (!DPUtil.empty(args.get("withStatusText"))) {
            fillStatus(rows, status());
        }
        if (!DPUtil.empty(args.get("withModels"))) {
            modelService.fillInfos(rows, "modelIds");
        }
        return result;
    }

    public JsonNode format(JsonNode rows) {
        for (JsonNode row : rows) {
            ObjectNode node = (ObjectNode) row;
            List<Integer> modelIds = DPUtil.parseIntList(node.at("/modelIds").asText(""));
            node.replace("modelIds", DPUtil.toJSON(modelIds));
        }
        return rows;
    }

    public boolean delete(List<Integer> ids, HttpServletRequest request) {
        return delete(authDao, ids, rbacService.uid(request));
    }

}
