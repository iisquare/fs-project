package com.iisquare.fs.web.lm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.lm.dao.ClientDao;
import com.iisquare.fs.web.lm.dao.ClientEndpointDao;
import com.iisquare.fs.web.lm.entity.Client;
import com.iisquare.fs.web.lm.mvc.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ClientService extends JPAServiceBase {

    @Autowired
    private ClientDao clientDao;
    @Autowired
    ClientEndpointDao clientEndpointDao;
    @Autowired
    DefaultRbacService rbacService;
    @Autowired
    Configuration configuration;


    public Map<?, ?> status() {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "启用");
        status.put(2, "禁用");
        return status;
    }

    public Client info(Integer id) {
        return info(clientDao, id);
    }

    public boolean existsByToken(String model, Integer ...ids) {
        return clientDao.existsByTokenEqualsAndIdNotIn(model, Arrays.asList(ids));
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        String token = DPUtil.trim(DPUtil.parseString(param.get("token")));
        if(DPUtil.empty(token)) return ApiUtil.result(1001, "认证标识不能为空", token);
        if(existsByToken(token, id)) return ApiUtil.result(2001, "认证标识已存在", token);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if(DPUtil.empty(name)) return ApiUtil.result(1002, "客户端名称异常", name);
        int status = DPUtil.parseInt(param.get("status"));
        if(!status().containsKey(status)) return ApiUtil.result(1003, "状态异常", status);
        Client info;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new Client();
        }
        info.setToken(token);
        info.setName(name);
        info.setSort(DPUtil.parseInt(param.get("sort")));
        info.setStatus(status);
        info.setDescription(DPUtil.parseString(param.get("description")));
        info = save(clientDao, info, rbacService.uid(request));
        return ApiUtil.result(0, null, info);
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(clientDao, param, (root, query, cb) -> {
            SpecificationHelper<Client> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.dateFormat(configuration.getFormatDate()).equalWithIntGTZero("id");
            helper.equalWithIntNotEmpty("status").like("token").like("name");
            return cb.and(helper.predicates());
        }, Sort.by(Sort.Order.desc("sort")), "id", "status", "sort");
        JsonNode rows = ApiUtil.rows(result);
        if(!DPUtil.empty(args.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(args.get("withStatusText"))) {
            fillStatus(rows, status());
        }
        return result;
    }

    public boolean remove(List<Integer> ids) {
        removeByParentId(clientEndpointDao, "clientId", ids);
        return remove(clientDao, ids);
    }

    public JsonNode fillInfo(JsonNode json, String ...properties) {
        return fillInfo(clientDao, json, properties);
    }

}
