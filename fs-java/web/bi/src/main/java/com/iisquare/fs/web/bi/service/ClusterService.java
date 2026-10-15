package com.iisquare.fs.web.bi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.base.web.util.RpcUtil;
import com.iisquare.fs.web.bi.dao.ClusterDao;
import com.iisquare.fs.web.bi.entity.Cluster;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.core.rpc.FlinkRpc;
import com.iisquare.fs.web.core.rpc.SparkRpc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

@Service
public class ClusterService extends JPAServiceBase {

    @Autowired
    ClusterDao clusterDao;
    @Autowired
    DefaultRbacService rbacService;
    @Autowired
    SparkRpc sparkRpc;
    @Autowired
    FlinkRpc flinkRpc;

    @Override
    public Map<String, String> sorts() {
        Map<String, String> sorts = new LinkedHashMap<>();
        sorts.put("id", "desc");
        sorts.put("sort", "desc");
        sorts.put("status", "asc");
        return sorts;
    }

    public Map<Integer, String> status() {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "启用");
        status.put(2, "禁用");
        return status;
    }

    public Map<String, String> engines() {
        Map<String, String> engines = new LinkedHashMap<>();
        engines.put("spark", "Apache Spark");
        engines.put("flink", "Apache Flink");
        return engines;
    }

    public Map<String, String> modes() {
        Map<String, String> modes = new LinkedHashMap<>();
        modes.put("local", "本地执行");
        modes.put("remote", "远程集群");
        return modes;
    }

    public Cluster info(Integer id) {
        return info(clusterDao, id);
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(clusterDao, param, (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.notEqual(root.get("status"), -1));
            if (!DPUtil.empty(param.get("name"))) {
                predicates.add(cb.like(root.get("name"), "%" + DPUtil.trim(DPUtil.parseString(param.get("name"))) + "%"));
            }
            if (!DPUtil.empty(param.get("engine"))) {
                predicates.add(cb.equal(root.get("engine"), DPUtil.parseString(param.get("engine"))));
            }
            if (!DPUtil.empty(param.get("mode"))) {
                predicates.add(cb.equal(root.get("mode"), DPUtil.parseString(param.get("mode"))));
            }
            if (!DPUtil.empty(param.get("status"))) {
                predicates.add(cb.equal(root.get("status"), DPUtil.parseInt(param.get("status"))));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, Sort.by(Sort.Order.desc("sort"), Sort.Order.desc("id")), sorts().keySet());
        JsonNode rows = format(ApiUtil.rows(result));
        if (!DPUtil.empty(args.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if (!DPUtil.empty(args.get("withStatusText"))) {
            fillStatus(rows, status());
            DPUtil.fillValues(rows, "engine", "engineText", engines());
            DPUtil.fillValues(rows, "mode", "modeText", modes());
        }
        return result;
    }

    public JsonNode format(JsonNode rows) {
        for (JsonNode row : rows) {
            ObjectNode node = (ObjectNode) row;
            JsonNode content = DPUtil.parseJSON(node.at("/content").asText(), k -> DPUtil.objectNode());
            node.replace("content", content);
            node.put("summary", summary(node.at("/engine").asText(), content));
        }
        return rows;
    }

    public String summary(String engine, JsonNode content) {
        if ("flink".equals(engine)) {
            String rest = content.at("/rest").asText("");
            String jobmanager = content.at("/jobmanager").asText("");
            if (!DPUtil.empty(rest)) return rest;
            return DPUtil.empty(jobmanager) ? "本地执行" : jobmanager;
        }
        String master = content.at("/master").asText("");
        String rest = content.at("/rest").asText("");
        if (!DPUtil.empty(master)) return master;
        return DPUtil.empty(rest) ? "本地执行" : rest;
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        int id = ValidateUtil.filterInteger(param.get("id"), 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if (DPUtil.empty(name)) return ApiUtil.result(1001, "集群名称不能为空", name);
        String engine = DPUtil.trim(DPUtil.parseString(param.get("engine")));
        if (!engines().containsKey(engine)) return ApiUtil.result(1002, "计算引擎异常", engine);
        String mode = DPUtil.trim(DPUtil.parseString(param.get("mode")));
        if (!modes().containsKey(mode)) return ApiUtil.result(1003, "运行模式异常", mode);
        int status = DPUtil.parseInt(param.get("status"));
        if (!status().containsKey(status)) return ApiUtil.result(1004, "状态异常", status);
        Cluster info;
        if (id > 0) {
            if (!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if (null == info) return ApiUtil.result(404, null, id);
        } else {
            if (!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new Cluster();
        }
        if (clusterDao.exist(name, DPUtil.parseInt(info.getId())) > 0) {
            return ApiUtil.result(1501, "名称已存在", name);
        }
        info.setName(name);
        info.setEngine(engine);
        info.setMode(mode);
        info.setContent(DPUtil.stringify(param.get("content")));
        info.setSort(DPUtil.parseInt(param.get("sort")));
        info.setStatus(status);
        info.setDescription(DPUtil.parseString(param.get("description")));
        info = save(clusterDao, info, rbacService.uid(request));
        return ApiUtil.result(0, null, info);
    }

    public Map<String, Object> delete(List<Integer> ids, HttpServletRequest request) {
        if (!rbacService.hasPermit(request, "delete")) return ApiUtil.result(9403, null, null);
        if (null == ids || ids.isEmpty()) return ApiUtil.result(1001, "请选择待删除的集群", ids);
        List<Cluster> list = clusterDao.findAllById(ids);
        long time = System.currentTimeMillis();
        int uid = rbacService.uid(request);
        for (Cluster item : list) {
            item.setStatus(-1);
            item.setUpdatedTime(time);
            item.setUpdatedUid(uid);
        }
        clusterDao.saveAll(list);
        return ApiUtil.result(0, null, true);
    }

    public List<Cluster> options(String engine) {
        return clusterDao.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), 1));
            if (!DPUtil.empty(engine)) predicates.add(cb.equal(root.get("engine"), engine));
            return cb.and(predicates.toArray(new Predicate[0]));
        }, Sort.by(Sort.Order.desc("sort"), Sort.Order.desc("id")));
    }

    /**
     * 连接测试：根据引擎转发至对应的计算服务
     */
    public Map<String, Object> test(Map<?, ?> param) {
        String engine = DPUtil.trim(DPUtil.parseString(param.get("engine")));
        JsonNode content = DPUtil.toJSON(param.get("content"), ObjectNode.class);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("content", DPUtil.toJSON(content, Map.class));
        if ("flink".equals(engine)) {
            return RpcUtil.result(flinkRpc.post("/bi/cluster/test", body));
        }
        if ("spark".equals(engine)) {
            return RpcUtil.result(sparkRpc.post("/bi/cluster/test", body));
        }
        return ApiUtil.result(1002, "计算引擎异常", engine);
    }

}
