package com.iisquare.fs.web.lm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.lm.dao.ClientEndpointDao;
import com.iisquare.fs.web.lm.entity.Client;
import com.iisquare.fs.web.lm.entity.ClientEndpoint;
import com.iisquare.fs.web.lm.entity.Server;
import com.iisquare.fs.web.lm.mvc.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ClientEndpointService extends JPAServiceBase {

    @Autowired
    private ClientEndpointDao clientEndpointDao;
    @Autowired
    private ServerService serverService;
    @Autowired
    private ClientService clientService;
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

    public ClientEndpoint info(Integer id) {
        return info(clientEndpointDao, id);
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        int status = DPUtil.parseInt(param.get("status"));
        if(!status().containsKey(status)) return ApiUtil.result(1002, "状态异常", status);
        ClientEndpoint info;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new ClientEndpoint();
        }
        Client client = clientService.info(DPUtil.parseInt(param.get("clientId")));
        if (null == client) {
            return ApiUtil.result(2001, "所属客户端不存在", null);
        }
        Server server = serverService.info(DPUtil.parseInt(param.get("serverId")));
        if (null == server) {
            return ApiUtil.result(2002, "所属服务端不存在", null);
        }
        info.setClientId(client.getId());
        info.setServerId(server.getId());
        info.setParallel(DPUtil.parseInt(param.get("parallel")));
        info.setCheckable(DPUtil.parseBoolean(param.get("checkable")) ? 1 : 0);
        info.setSort(DPUtil.parseInt(param.get("sort")));
        info.setStatus(status);
        info.setDescription(DPUtil.parseString(param.get("description")));
        info = save(clientEndpointDao, info, rbacService.uid(request));
        return ApiUtil.result(0, null, info);
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(clientEndpointDao, param, (root, query, cb) -> {
            SpecificationHelper<ClientEndpoint> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.dateFormat(configuration.getFormatDate()).equalWithIntGTZero("id");
            helper.equalWithIntGTZero("clientId").equalWithIntGTZero("serverId").equalWithIntNotEmpty("status");
            return cb.and(helper.predicates());
        }, Sort.by(Sort.Order.desc("sort")), "id", "status", "sort");
        JsonNode rows = format(ApiUtil.rows(result));
        if(!DPUtil.empty(args.get("withClientInfo"))) {
            clientService.fillInfo(rows, "clientId");
        }
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

    public JsonNode format(JsonNode rows) {
        for (JsonNode row : rows) {
            ObjectNode node = (ObjectNode) row;
            int leaf = node.at("/checkable").asInt(0);
            node.put("checkable", 1 == leaf);
        }
        return rows;
    }

    public boolean remove(List<Integer> ids) {
        return remove(clientEndpointDao, ids);
    }

}
