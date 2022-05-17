package com.iisquare.fs.base.mongodb.helper;

import com.iisquare.fs.base.core.util.DPUtil;
import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import java.util.*;

public class FilterHelper {

    private List<Bson> filters;
    private Map<?, ?> param;
    private String dateFormat = null;

    public FilterHelper( Map<?, ?> param) {
        this.param = param;
        this.filters = new ArrayList<>();
    }

    public static FilterHelper newInstance(Map<?, ?> param) {
        return new FilterHelper(param);
    }

    public FilterHelper dateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
        return this;
    }

    public Bson filter() {
        if (filters.size() < 1) return null;
        return Filters.and(filters.toArray(new Bson[0]));
    }

    public FilterHelper like(String key) {
        return like(key, key);
    }

    public FilterHelper like(String key, String field) {
        String value = DPUtil.trim(DPUtil.parseString(param.get(key)));
        if(!DPUtil.empty(value)) {
            filters.add(Filters.regex(field, value));
        }
        return this;
    }

    public FilterHelper equalWithIntGTZero(String key) {
        return equalWithIntGTZero(key, key);
    }

    public FilterHelper equalWithIntGTZero(String key, String field) {
        int value = DPUtil.parseInt(param.get(key));
        if(value > 0) {
            filters.add(Filters.eq(field, value));
        }
        return this;
    }

    public FilterHelper equalWithIntNotEmpty(String key) {
        return equalWithIntNotEmpty(key, key);
    }

    public FilterHelper equalWithIntNotEmpty(String key, String field) {
        int value = DPUtil.parseInt(param.get(key));
        if(!"".equals(DPUtil.parseString(param.get(key)))) {
            filters.add(Filters.eq(field, value));
        }
        return this;
    }

    public FilterHelper equalWithObjectId(String key) {
        return equalWithObjectId(key, key);
    }

    public FilterHelper equalWithObjectId(String key, String field) {
        String value = DPUtil.parseString(param.get(key));
        if(!DPUtil.empty(value)) {
            filters.add(Filters.eq(field, new ObjectId(value)));
        }
        return this;
    }

    public FilterHelper equal(String key) {
        return equal(key, key);
    }

    public FilterHelper equal(String key, String field) {
        String value = DPUtil.parseString(param.get(key));
        if(!DPUtil.empty(value)) {
            filters.add(Filters.eq(field, value));
        }
        return this;
    }

    public FilterHelper geWithDate(String key, String field) {
        String value = DPUtil.trim(DPUtil.parseString(param.get(key)));
        if(!DPUtil.empty(value)) {
            filters.add(Filters.gte(field, DPUtil.dateTime2millis(value, dateFormat)));
        }
        return this;
    }

    public FilterHelper leWithDate(String key, String field) {
        String value = DPUtil.trim(DPUtil.parseString(param.get(key)));
        if(!DPUtil.empty(value)) {
            filters.add(Filters.lte(field, DPUtil.dateTime2millis(value, dateFormat) + 999));
        }
        return this;
    }

    public FilterHelper betweenWithDate(String key) {
        return betweenWithDate(key, key);
    }

    public FilterHelper betweenWithDate(String key, String field) {
        String value = DPUtil.trim(DPUtil.parseString(param.get(key)));
        if(!DPUtil.empty(value)) {
            filters.add(Filters.lte(field, DPUtil.dateTime2millis(value, dateFormat) + 999));
        }
        return geWithDate(key + "Begin", key).leWithDate(key + "End", key);
    }

    public FilterHelper inWithObjectId(String key) {
        return inWithObjectId(key, key);
    }

    public FilterHelper inWithObjectId(String key, String field) {
        Object value = param.get(key);
        if (DPUtil.empty(value)) return this;
        List<ObjectId> list = new ArrayList<>();
        if (value instanceof Collection) {
            Iterator iterator = ((Collection) value).iterator();
            while (iterator.hasNext()) {
                String item = DPUtil.parseString(iterator.next());
                if (!DPUtil.empty(item)) list.add(new ObjectId(item));
            }
        } else {
            String[] strings = DPUtil.explode(",", DPUtil.parseString(value));
            for (String item : strings) {
                if (!DPUtil.empty(item)) list.add(new ObjectId(item));
            }
        }
        if (list.size() > 0) filters.add(Filters.in(field, list));
        return this;
    }

    public FilterHelper in(String key) {
        return in(key, key);
    }

    public FilterHelper in(String key, String field) {
        Object value = param.get(key);
        if (DPUtil.empty(value)) return this;
        if (value instanceof Collection) {
            Iterator iterator = ((Collection) value).iterator();
            List<String> list = new ArrayList<>();
            while (iterator.hasNext()) list.add(DPUtil.parseString(iterator.next()));
            if (list.size() > 0) filters.add(Filters.in(field, list));
            return this;
        }
        String[] strings = DPUtil.explode(",", DPUtil.parseString(value));
        if (strings.length > 0) filters.add(Filters.in(field, strings));
        return this;
    }

    public List<Integer> listInteger(String key) {
        Object value = param.get(key);
        if (DPUtil.empty(value)) return null;
        List<Integer> list = new ArrayList<>();
        if (value instanceof Collection) {
            Iterator iterator = ((Collection) value).iterator();
            while (iterator.hasNext()) list.add(DPUtil.parseInt(iterator.next()));
        } else {
            String[] strings = DPUtil.explode(",", DPUtil.parseString(value));
            for (String str : strings) list.add(DPUtil.parseInt(str));
        }
        return list;
    }

    public FilterHelper add(Bson e) {
        filters.add(e);
        return this;
    }

    public FilterHelper addAll(Collection<Bson> list) {
        filters.addAll(list);
        return this;
    }

}
