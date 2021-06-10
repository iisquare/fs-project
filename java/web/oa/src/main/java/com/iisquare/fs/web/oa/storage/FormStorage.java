package com.iisquare.fs.web.oa.storage;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;

public abstract class FormStorage {

    public static final String FIELD_ID = "_id";

    public abstract Map<String, Object> search(ObjectNode frame, Map<String, Object> param, Map<String, Object> config);

    public abstract ObjectNode save(ObjectNode frame, ObjectNode info, int uid);

    public abstract long delete(ObjectNode frame, List<String> ids, int uid);

}
