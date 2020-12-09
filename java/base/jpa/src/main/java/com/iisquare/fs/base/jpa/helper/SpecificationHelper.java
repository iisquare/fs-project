package com.iisquare.fs.base.jpa.helper;

import com.iisquare.fs.base.core.util.DPUtil;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.*;

/**
 * 特殊定制化查询辅助类，不适用于其他项目
 */
public class SpecificationHelper<T> {

    private List<Predicate> predicates;
    private Root<T> root;
    private CriteriaBuilder builder;
    private Map<?, ?> param;
    private String dateFormat = null;

    public SpecificationHelper(Root<T> root, CriteriaBuilder builder, Map<?, ?> param) {
        this.predicates = new ArrayList<>();
        this.root = root;
        this.builder = builder;
        this.param = param;
    }

    public static <T> SpecificationHelper<T> newInstance(Root<T> root, CriteriaBuilder builder, Map<?, ?> param) {
        return new SpecificationHelper<>(root, builder, param);
    }

    public SpecificationHelper dateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
        return this;
    }

    public Predicate[] predicates() {
        return predicates.toArray(new Predicate[predicates.size()]);
    }

    public SpecificationHelper like(String key) {
        return like(key, key);
    }

    public SpecificationHelper like(String key, String field) {
        String value = DPUtil.trim(DPUtil.parseString(param.get(key)));
        if(!DPUtil.empty(value)) {
            predicates.add(builder.like(root.get(field), "%" + value + "%"));
        }
        return this;
    }

    public SpecificationHelper equalWithIntGTZero(String key) {
        return equalWithIntGTZero(key, key);
    }

    public SpecificationHelper equalWithIntGTZero(String key, String field) {
        int value = DPUtil.parseInt(param.get(key));
        if(value > 0) {
            predicates.add(builder.equal(root.get(field), value));
        }
        return this;
    }

    public SpecificationHelper equalWithIntNotEmpty(String key) {
        return equalWithIntNotEmpty(key, key);
    }

    public SpecificationHelper equalWithIntNotEmpty(String key, String field) {
        int value = DPUtil.parseInt(param.get(key));
        if(!"".equals(DPUtil.parseString(param.get(key)))) {
            predicates.add(builder.equal(root.get(field), value));
        }
        return this;
    }

    public SpecificationHelper equal(String key) {
        return equal(key, key);
    }

    public SpecificationHelper equal(String key, String field) {
        String value = DPUtil.parseString(param.get(key));
        if(!DPUtil.empty(value)) {
            predicates.add(builder.equal(root.get(field), value));
        }
        return this;
    }

    public SpecificationHelper geWithDate(String key, String field) {
        String value = DPUtil.trim(DPUtil.parseString(param.get(key)));
        if(!DPUtil.empty(value)) {
            predicates.add(builder.ge(root.get(field), DPUtil.dateTimeToMillis(value, dateFormat)));
        }
        return this;
    }

    public SpecificationHelper leWithDate(String key, String field) {
        String value = DPUtil.trim(DPUtil.parseString(param.get(key)));
        if(!DPUtil.empty(value)) {
            predicates.add(builder.le(root.get(field), DPUtil.dateTimeToMillis(value, dateFormat) + 999));
        }
        return this;
    }

    public SpecificationHelper betweenWithDate(String key) {
        return betweenWithDate(key, key);
    }

    public SpecificationHelper betweenWithDate(String key, String field) {
        String value = DPUtil.trim(DPUtil.parseString(param.get(key)));
        if(!DPUtil.empty(value)) {
            predicates.add(builder.le(root.get(field), DPUtil.dateTimeToMillis(value, dateFormat) + 999));
        }
        return geWithDate(key + "Begin", key).leWithDate(key + "End", key);
    }

    public SpecificationHelper in(String key) {
        return in(key, key);
    }

    public SpecificationHelper in(String key, String field) {
        Object value = param.get(key);
        if (DPUtil.empty(value)) return this;
        if (value instanceof Collection) {
            Iterator iterator = ((Collection) value).iterator();
            List<String> list = new ArrayList<>();
            while (iterator.hasNext()) list.add(DPUtil.parseString(iterator.next()));
            if (list.size() > 0) predicates.add(root.get(field).in(list));
            return this;
        }
        String[] strings = DPUtil.explode(DPUtil.parseString(value));
        if (strings.length > 0) predicates.add(root.get(field).in(strings));
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
            String[] strings = DPUtil.explode(DPUtil.parseString(value));
            for (String str : strings) list.add(DPUtil.parseInt(str));
        }
        return list;
    }

    public SpecificationHelper add(Predicate e) {
        predicates.add(e);
        return this;
    }

}
