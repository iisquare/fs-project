package com.iisquare.fs.web.oa.storage;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class FormRestStorage extends FormStorage {

    @Override
    public Map<String, Object> search(ObjectNode frame, Map<String, Object> param, Map<String, Object> config) {
        return null;
    }

    @Override
    public ObjectNode save(ObjectNode frame, ObjectNode info, int uid) {
        return null;
    }

    @Override
    public long delete(ObjectNode frame, List<String> ids, int uid) {
        return 0;
    }

}
