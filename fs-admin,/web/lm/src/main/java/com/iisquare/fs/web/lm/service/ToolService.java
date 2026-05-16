package com.iisquare.fs.web.lm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.lm.dao.ToolDao;
import com.iisquare.fs.web.lm.entity.Tool;
import com.iisquare.fs.web.lm.mvc.Configuration;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ToolService extends JPAServiceBase {

    @Autowired
    private ToolDao toolDao;
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

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if(DPUtil.empty(name)) return ApiUtil.result(1001, "服务名称不能为空", name);
        int status = DPUtil.parseInt(param.get("status"));
        if(!status().containsKey(status)) return ApiUtil.result(1003, "状态异常", status);
        Tool info = info(toolDao, name);
        if(null != info) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new Tool();
            info.setName(name);
        }
        info.setContent(DPUtil.stringify(param.get("content")));
        info.setStatus(status);
        info = save(toolDao, info, rbacService.uid(request));
        return ApiUtil.result(0, null, DPUtil.firstNode(format(DPUtil.toArrayNode(info))));
    }

    public ObjectNode all(Map<String, Object> param, Map<?, ?> args) {
        List<Tool> list = toolDao.findAll((root, query, cb) -> {
            SpecificationHelper<Tool> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.equalWithIntNotEmpty("status");
            return cb.and(helper.predicates());
        });
        JsonNode result = format(DPUtil.toJSON(list));
        if(!DPUtil.empty(args.get("withUserInfo"))) {
            rbacService.fillUserInfo(result, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(args.get("withStatusText"))) {
            fillStatus(result, status());
        }
        return DPUtil.json2object(result, "name");
    }

    public JsonNode format(JsonNode rows) {
        for (JsonNode row : rows) {
            ObjectNode node = (ObjectNode) row;
            node.replace("content", DPUtil.parseJSON(row.at("/content").asText()));
        }
        return rows;
    }

    public boolean remove(List<String> names) {
        return remove(toolDao, names);
    }

}
