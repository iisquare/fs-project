package com.iisquare.fs.web.kg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.helper.SpecificationHelper;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.kg.dao.OntologyDao;
import com.iisquare.fs.web.kg.entity.Ontology;
import com.iisquare.fs.web.kg.mvc.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

@Service
public class OntologyService extends JPAServiceBase {

    @Autowired
    private OntologyDao ontologyDao;
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

    public Ontology info(Integer id) {
        return info(ontologyDao, id);
    }

    public Map<String, Object> info(Map<?, ?> param) {
        Ontology info = info(DPUtil.parseInt(param.get("id")));
        if (null == info) return ApiUtil.result(1404, "流程信息不存在", null);
        JsonNode node = DPUtil.firstNode(format(DPUtil.toArrayNode(info)));
        return ApiUtil.result(0, null, node);
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if(DPUtil.empty(name)) return ApiUtil.result(1001, "本体名称异常", name);
        int status = DPUtil.parseInt(param.get("status"));
        if(!status().containsKey(status)) return ApiUtil.result(1003, "状态异常", status);
        Ontology info;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new Ontology();
        }
        int entityCount = 0, relationshipCount = 0;
        JsonNode json = DPUtil.toJSON(param.get("content"));
        for (JsonNode cell : json.at("/cells")) {
            String shape = cell.at("/shape").asText();
            switch (shape) {
                case "kg-node":
                    entityCount++;
                    break;
                case "flow-edge":
                    relationshipCount++;
                    break;
            }
        }
        info.setName(name);
        info.setEntityCount(entityCount);
        info.setRelationshipCount(relationshipCount);
        info.setContent(DPUtil.stringify(json));
        info.setSort(DPUtil.parseInt(param.get("sort")));
        info.setStatus(status);
        info.setDescription(DPUtil.parseString(param.get("description")));
        info = save(ontologyDao, info, rbacService.uid(request));
        return info(DPUtil.buildMap("id", info.getId()));
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(ontologyDao, param, (root, query, cb) -> {
            SpecificationHelper<Ontology> helper = SpecificationHelper.newInstance(root, cb, param);
            helper.dateFormat(configuration.getFormatDate()).equalWithIntGTZero("id");
            helper.equalWithIntNotEmpty("status").like("model").like("name");
            return cb.and(helper.predicates());
        }, Sort.by(Sort.Order.desc("sort")), "id", "status", "sort");
        JsonNode rows = format(ApiUtil.rows(result));
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
            String content = row.at("/content").asText("{}");
            node.replace("content", DPUtil.parseJSON(content));
        }
        return rows;
    }

    public boolean remove(List<Integer> ids) {
        return remove(ontologyDao, ids);
    }


}
