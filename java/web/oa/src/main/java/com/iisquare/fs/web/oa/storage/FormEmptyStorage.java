package com.iisquare.fs.web.oa.storage;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class FormEmptyStorage extends FormStorage {

    @Override
    public Map<String, Object> search(ObjectNode frame, Map<String, Object> param, Map<String, Object> config) {
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, 15);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", 0);
        result.put("rows", new ArrayList<>());
        return result;
    }

    @Override
    public ObjectNode save(ObjectNode frame, ObjectNode info, int uid) {
        return DPUtil.objectNode().put(FIELD_ID, DPUtil.random(6));
    }

    @Override
    public long delete(ObjectNode frame, List<String> ids, int uid) {
        return 0;
    }

    @Override
    public ObjectNode info(ObjectNode frame, String id) {
        return DPUtil.objectNode().put(FIELD_ID, id);
    }

}
