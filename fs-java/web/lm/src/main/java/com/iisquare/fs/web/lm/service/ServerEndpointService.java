package com.iisquare.fs.web.lm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.lm.dao.ServerEndpointDao;
import com.iisquare.fs.web.lm.entity.Server;
import com.iisquare.fs.web.lm.entity.ServerEndpoint;
import com.iisquare.fs.web.lm.mvc.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ServerEndpointService extends JPAServiceBase {

    @Autowired
    private ServerEndpointDao serverEndpointDao;
    @Autowired
    private ServerService serverService;
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

    public ServerEndpoint info(Integer id) {
        return info(serverEndpointDao, id);
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        String url = DPUtil.trim(DPUtil.parseString(param.get("url")));
        if(DPUtil.empty(url)) return ApiUtil.result(1001, "模型链接地址不能为空", url);
        int status = DPUtil.parseInt(param.get("status"));
        if(!status().containsKey(status)) return ApiUtil.result(1002, "状态异常", status);
        ServerEndpoint info;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new ServerEndpoint();
        }
        Server server = serverService.info(DPUtil.parseInt(param.get("serverId")));
        if (null == server) {
            return ApiUtil.result(2001, "所属服务端不存在", null);
        }
        info.setServerId(server.getId());
        info.setUrl(url);
        info.setModel(DPUtil.parseString(param.get("model")));
        info.setToken(DPUtil.parseString(param.get("token")));
        info.setParallel(DPUtil.parseInt(param.get("parallel")));
        info.setSort(DPUtil.parseInt(param.get("sort")));
        info.setStatus(status);
        info.setDescription(DPUtil.parseString(param.get("description")));
        info = save(serverEndpointDao, info, rbacService.uid(request));
        return ApiUtil.result(0, null, info);
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(serverEndpointDao, param, (root, query, cb) -> {
            SpecificationHelper<ServerEndpoint> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.dateFormat(configuration.getFormatDate()).equalWithIntGTZero("id");
            helper.equalWithIntGTZero("serverId");
            helper.equalWithIntNotEmpty("status").like("url").like("model");
            return cb.and(helper.predicates());
        }, Sort.by(Sort.Order.desc("sort")), "id", "status", "sort");
        JsonNode rows = ApiUtil.rows(result);
        if(!DPUtil.empty(args.get("withServerInfo"))) {
            serverService.fillInfo(rows, "serverId");
        }
        if(!DPUtil.empty(args.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(args.get("withStatusText"))) {
            fillStatus(rows, status());
        }
        return result;
    }

    public boolean remove(List<Integer> ids) {
        return remove(serverEndpointDao, ids);
    }

}
