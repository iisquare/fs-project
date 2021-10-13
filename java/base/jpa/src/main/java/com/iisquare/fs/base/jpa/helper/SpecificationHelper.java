package com.iisquare.fs.base.jpa.helper;

import com.iisquare.fs.base.core.util.DPUtil;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
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
        return predicates.toArray(new Predicate[0]);
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
        return equalWithIntNotEmpty(key, key, null);
    }

    public SpecificationHelper equalWithIntNotEmpty(String key, String field) {
        return equalWithIntNotEmpty(key, field, null);
    }

    public SpecificationHelper equalWithIntNotEmpty(String key, Integer defaultValue) {
        return equalWithIntNotEmpty(key, key, defaultValue);
    }

    public SpecificationHelper equalWithIntNotEmpty(String key, String field, Integer defaultValue) {
        if("".equals(DPUtil.parseString(param.get(key)))) {
            if (null != defaultValue) predicates.add(builder.equal(root.get(field), defaultValue));
        } else {
            int value = DPUtil.parseInt(param.get(key));
            predicates.add(builder.equal(root.get(field), value));
        }
        return this;
    }

    public SpecificationHelper equalWithIntElseNot(String key, Integer notValue) {
        return equalWithIntElseNot(key, key, notValue);
    }

    public SpecificationHelper equalWithIntElseNot(String key, String field, Integer notValue) {
        if("".equals(DPUtil.parseString(param.get(key)))) {
            predicates.add(builder.notEqual(root.get(field), notValue));
        } else {
            int value = DPUtil.parseInt(param.get(key));
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
            predicates.add(builder.ge(root.get(field), DPUtil.dateTime2millis(value, dateFormat)));
        }
        return this;
    }

    public SpecificationHelper leWithDate(String key, String field) {
        String value = DPUtil.trim(DPUtil.parseString(param.get(key)));
        if(!DPUtil.empty(value)) {
            predicates.add(builder.le(root.get(field), DPUtil.dateTime2millis(value, dateFormat) + 999));
        }
        return this;
    }

    public SpecificationHelper betweenWithDate(String key) {
        return betweenWithDate(key, key);
    }

    public SpecificationHelper betweenWithDate(String key, String field) {
        String value = DPUtil.trim(DPUtil.parseString(param.get(key)));
        if(!DPUtil.empty(value)) {
            predicates.add(builder.le(root.get(field), DPUtil.dateTime2millis(value, dateFormat) + 999));
        }
        return geWithDate(key + "Begin", field).leWithDate(key + "End", field);
    }

    public SpecificationHelper ge(String key) {
        return ge(key, key);
    }

    public SpecificationHelper ge(String key, String field) {
        String value = DPUtil.trim(DPUtil.parseString(param.get(key)));
        if(!DPUtil.empty(value)) {
            predicates.add(builder.ge(root.get(field), DPUtil.parseDouble(value)));
        }
        return this;
    }

    public SpecificationHelper gt(String key) {
        return gt(key, key);
    }

    public SpecificationHelper gt(String key, String field) {
        String value = DPUtil.trim(DPUtil.parseString(param.get(key)));
        if(!DPUtil.empty(value)) {
            predicates.add(builder.gt(root.get(field), DPUtil.parseDouble(value)));
        }
        return this;
    }

    public SpecificationHelper le(String key) {
        return le(key, key);
    }

    public SpecificationHelper le(String key, String field) {
        String value = DPUtil.trim(DPUtil.parseString(param.get(key)));
        if(!DPUtil.empty(value)) {
            predicates.add(builder.le(root.get(field), DPUtil.parseDouble(value)));
        }
        return this;
    }

    public SpecificationHelper lt(String key) {
        return lt(key, key);
    }

    public SpecificationHelper lt(String key, String field) {
        String value = DPUtil.trim(DPUtil.parseString(param.get(key)));
        if(!DPUtil.empty(value)) {
            predicates.add(builder.lt(root.get(field), DPUtil.parseDouble(value)));
        }
        return this;
    }

    public SpecificationHelper between(String key) {
        return between(key, key);
    }

    public SpecificationHelper between(String key, String field) {
        return ge(key + "Begin", field).lt(key + "End", field);
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

    public SpecificationHelper functionFindInSet(String key) {
        return functionFindInSet(key, key);
    }

    public SpecificationHelper functionFindInSet(String key, String field) {
        Object value = param.get(key);
        if (DPUtil.empty(value)) return this;
        Iterator iterator = value instanceof Collection
                ? ((Collection) value).iterator()
                : Arrays.asList(DPUtil.explode(DPUtil.parseString(value))).iterator();
        List<Predicate> predicates = new ArrayList<>();
        while (iterator.hasNext()) {
            String item = DPUtil.parseString(iterator.next());
            Expression<Integer> expression = builder.function("FIND_IN_SET", Integer.class, builder.literal(item), root.get(field));
            predicates.add(builder.gt(expression, 0));
        }
        if (predicates.size() > 0) {
            this.predicates.add(builder.or(predicates.toArray(new Predicate[0])));
        }
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
