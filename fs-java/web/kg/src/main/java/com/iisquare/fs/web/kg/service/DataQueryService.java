package com.iisquare.fs.web.kg.service;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.kg.dao.DataQueryDao;
import com.iisquare.fs.web.kg.entity.DataQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 数据检索方案
 */
@Service
public class DataQueryService {

    @Autowired
    DataQueryDao dataQueryDao;

    public Map<String, Object> list(Map<String, Object> param) {
        List<DataQuery> rows = dataQueryDao.findAllByOntologyIdAndKindAndLabelAndUidOrderByIdDesc(
                DPUtil.parseInt(param.get("ontologyId")), DPUtil.parseString(param.get("kind")),
                DPUtil.parseString(param.get("label")), DPUtil.parseInt(param.get("uid")));
        ArrayNode data = DPUtil.arrayNode();
        for (DataQuery row : rows) {
            com.fasterxml.jackson.databind.node.ObjectNode node = DPUtil.objectNode();
            node.put("id", row.getId());
            node.put("name", row.getName());
            node.put("params", row.getParams());
            node.put("createdTime", row.getCreatedTime());
            data.add(node);
        }
        return ApiUtil.result(0, null, data);
    }

    public Map<String, Object> save(Map<String, Object> param) {
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if (DPUtil.empty(name)) return ApiUtil.result(1001, "方案名称不能为空", null);
        int ontologyId = DPUtil.parseInt(param.get("ontologyId"));
        String kind = DPUtil.parseString(param.get("kind"));
        String label = DPUtil.parseString(param.get("label"));
        int uid = DPUtil.parseInt(param.get("uid"));
        Optional<DataQuery> optional = dataQueryDao.findFirstByOntologyIdAndKindAndLabelAndUidAndName(
                ontologyId, kind, label, uid, name);
        DataQuery info = optional.orElseGet(DataQuery::new);
        long time = System.currentTimeMillis();
        if (null == info.getId()) {
            info.setOntologyId(ontologyId);
            info.setKind(kind);
            info.setLabel(label);
            info.setName(name);
            info.setUid(uid);
            info.setCreatedTime(time);
        }
        info.setParams(DPUtil.stringify(DPUtil.toJSON(param.get("params"))));
        info.setUpdatedTime(time);
        info = dataQueryDao.save(info);
        return ApiUtil.result(0, null, info.getId());
    }

    public Map<String, Object> remove(Map<String, Object> param) {
        List<Integer> ids = DPUtil.parseIntList(param.get("ids"));
        if (ids.isEmpty()) return ApiUtil.result(1001, "待删除的标识不能为空", null);
        int uid = DPUtil.parseInt(param.get("uid"));
        List<DataQuery> rows = dataQueryDao.findAllById(ids);
        List<DataQuery> removed = new java.util.ArrayList<>();
        for (DataQuery row : rows) {
            if (uid > 0 && null != row.getUid() && row.getUid() != uid) continue; // 只能删除自己的检索方案
            removed.add(row);
        }
        dataQueryDao.deleteAll(removed);
        return ApiUtil.result(0, null, removed.size());
    }

}
