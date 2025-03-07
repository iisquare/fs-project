package com.iisquare.fs.base.mongodb.helper;

import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.mongodb.MongoCore;
import org.bson.Document;

import java.util.Date;
import java.util.List;

public class FiltrationHelper {

    private Document document;
    private Document result;

    public FiltrationHelper(Document document, Document result) {
        this.document = document;
        this.result = null == result ? new Document() : result;
    }

    public static FiltrationHelper newInstance(Document document, Document result) {
        return new FiltrationHelper(document, result);
    }

    public static FiltrationHelper newInstance(Document document) {
        return new FiltrationHelper(document, null);
    }

    public Document result() {
        return result;
    }

    public boolean hasId() {
        return result.containsKey(MongoCore.FIELD_ID) && !DPUtil.empty(result.get(MongoCore.FIELD_ID));
    }

    public FiltrationHelper id() {
        if (document.containsKey(MongoCore.FIELD_ID)) {
            result.put(MongoCore.FIELD_ID, document.get(MongoCore.FIELD_ID));
        }
        return this;
    }

    public FiltrationHelper asString(String field) {
        return asString(field, null);
    }

    public FiltrationHelper asString(String field, String defaultValue) {
        if (document.containsKey(field)) {
            result.put(field, DPUtil.parseString(document.get(field)));
        } else if (null != defaultValue) {
            result.put(field, defaultValue);
        }
        return this;
    }

    public FiltrationHelper useString(String field, String defaultValue) {
        return asString(field, hasId() ? null : defaultValue);
    }

    public FiltrationHelper asObject(String field) {
        return asObject(field, null);
    }

    public FiltrationHelper asObject(String field, Object defaultValue) {
        if (document.containsKey(field)) {
            result.put(field, document.get(field));
        } else if (null != defaultValue) {
            result.put(field, defaultValue);
        }
        return this;
    }

    public FiltrationHelper useObject(String field, Object defaultValue) {
        return asObject(field, hasId() ? null : defaultValue);
    }

    public FiltrationHelper asInteger(String field) {
        return asInteger(field, null);
    }

    public FiltrationHelper asInteger(String field, Integer defaultValue) {
        if (document.containsKey(field)) {
            result.put(field, DPUtil.parseInt(document.get(field)));
        } else if (null != defaultValue) {
            result.put(field, defaultValue);
        }
        return this;
    }

    public FiltrationHelper useInteger(String field, Integer defaultValue) {
        return asInteger(field, hasId() ? null : defaultValue);
    }

    public FiltrationHelper asLong(String field) {
        return asLong(field, null);
    }

    public FiltrationHelper asLong(String field, Long defaultValue) {
        if (document.containsKey(field)) {
            result.put(field, DPUtil.parseLong(document.get(field)));
        } else if (null != defaultValue) {
            result.put(field, defaultValue);
        }
        return this;
    }

    public FiltrationHelper useLong(String field, Long defaultValue) {
        return asLong(field, hasId() ? null : defaultValue);
    }

    public FiltrationHelper asDouble(String field) {
        return asDouble(field, null);
    }

    public FiltrationHelper asDouble(String field, Double defaultValue) {
        if (document.containsKey(field)) {
            result.put(field, DPUtil.parseDouble(document.get(field)));
        } else if (null != defaultValue) {
            result.put(field, defaultValue);
        }
        return this;
    }

    public FiltrationHelper useDouble(String field, Double defaultValue) {
        return asDouble(field, hasId() ? null : defaultValue);
    }

    public FiltrationHelper asDate(String field) {
        return asDate(field, null);
    }

    public FiltrationHelper asDate(String field, Date defaultValue) {
        if (document.containsKey(field)) {
            result.put(field, document.getDate(field));
        } else if (null != defaultValue) {
            result.put(field, defaultValue);
        }
        return this;
    }

    public FiltrationHelper useDate(String field, Date defaultValue) {
        return asDate(field, hasId() ? null : defaultValue);
    }

    public FiltrationHelper asBoolean(String field) {
        return asBoolean(field, null);
    }

    public FiltrationHelper asBoolean(String field, Boolean defaultValue) {
        if (document.containsKey(field)) {
            result.put(field, DPUtil.parseBoolean(document.get(field)));
        } else if (null != defaultValue) {
            result.put(field, defaultValue);
        }
        return this;
    }

    public FiltrationHelper useBoolean(String field, Boolean defaultValue) {
        return asBoolean(field, hasId() ? null : defaultValue);
    }

    public <T> FiltrationHelper asList(String field, Class<T> clazz) {
        return asList(field, null);
    }

    public <T> FiltrationHelper asList(String field, Class<T> clazz, List<T> defaultValue) {
        if (document.containsKey(field)) {
            result.put(field, document.getList(field, clazz));
        } else if (null != defaultValue) {
            result.put(field, defaultValue);
        }
        return this;
    }

    public <T> FiltrationHelper useList(String field, Class<T> clazz, List<T> defaultValue) {
        return asList(field, clazz, hasId() ? null : defaultValue);
    }

}
