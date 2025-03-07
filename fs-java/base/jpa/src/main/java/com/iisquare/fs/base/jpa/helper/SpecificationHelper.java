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

    public SpecificationHelper<T> dateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
        return this;
    }

    public Predicate[] predicates() {
        return predicates.toArray(new Predicate[0]);
    }

    public SpecificationHelper<T> like(String key) {
        return like(key, key);
    }

    public SpecificationHelper<T> like(String key, String field) {
        String value = DPUtil.trim(DPUtil.parseString(param.get(key)));
        if(!DPUtil.empty(value)) {
            predicates.add(builder.like(root.get(field), "%" + value + "%"));
        }
        return this;
    }

    public SpecificationHelper<T> likeExp(String key) {
        return likeExp(key, key);
    }

    public SpecificationHelper<T> likeExp(String key, String field) {
        String value = DPUtil.trim(DPUtil.parseString(param.get(key)));
        if(!DPUtil.empty(value)) {
            predicates.add(builder.like(root.get(field), value));
        }
        return this;
    }

    public SpecificationHelper<T> equalWithIntGTZero(String key) {
        return equalWithIntGTZero(key, key);
    }

    public SpecificationHelper<T> equalWithIntGTZero(String key, String field) {
        int value = DPUtil.parseInt(param.get(key));
        if(value > 0) {
            predicates.add(builder.equal(root.get(field), value));
        }
        return this;
    }

    public SpecificationHelper<T> equalWithLongGTZero(String key) {
        return equalWithLongGTZero(key, key);
    }

    public SpecificationHelper<T> equalWithLongGTZero(String key, String field) {
        long value = DPUtil.parseLong(param.get(key));
        if(value > 0) {
            predicates.add(builder.equal(root.get(field), value));
        }
        return this;
    }

    public SpecificationHelper<T> equalWithIntNotEmpty(String key) {
        return equalWithIntNotEmpty(key, key, null);
    }

    public SpecificationHelper<T> equalWithIntNotEmpty(String key, String field) {
        return equalWithIntNotEmpty(key, field, null);
    }

    public SpecificationHelper<T> equalWithIntNotEmpty(String key, Integer defaultValue) {
        return equalWithIntNotEmpty(key, key, defaultValue);
    }

    public SpecificationHelper<T> equalWithIntNotEmpty(String key, String field, Integer defaultValue) {
        if("".equals(DPUtil.parseString(param.get(key)))) {
            if (null != defaultValue) predicates.add(builder.equal(root.get(field), defaultValue));
        } else {
            int value = DPUtil.parseInt(param.get(key));
            predicates.add(builder.equal(root.get(field), value));
        }
        return this;
    }

    public SpecificationHelper<T> equalWithIntElseNot(String key, Integer notValue) {
        return equalWithIntElseNot(key, key, notValue);
    }

    public SpecificationHelper<T> equalWithIntElseNot(String key, String field, Integer notValue) {
        if("".equals(DPUtil.parseString(param.get(key)))) {
            predicates.add(builder.notEqual(root.get(field), notValue));
        } else {
            int value = DPUtil.parseInt(param.get(key));
            predicates.add(builder.equal(root.get(field), value));
        }
        return this;
    }

    public SpecificationHelper<T> withoutDeleted() {
        predicates.add(builder.equal(root.get("deletedTime"), 0));
        return this;
    }

    public SpecificationHelper<T> deleted() {
        return deleted("deleted");
    }

    public SpecificationHelper<T> deleted(String key) {
        String deleted = DPUtil.parseString(param.get(key));
        switch (deleted) {
            case "only":
                predicates.add(builder.gt(root.get("deletedTime"), 0));
                break;
            case "without":
                predicates.add(builder.equal(root.get("deletedTime"), 0));
                break;
        }
        return this;
    }

    public SpecificationHelper<T> equal(String key) {
        return equal(key, key);
    }

    public SpecificationHelper<T> equal(String key, String field) {
        String value = DPUtil.parseString(param.get(key));
        if(!DPUtil.empty(value)) {
            predicates.add(builder.equal(root.get(field), value));
        }
        return this;
    }

    public SpecificationHelper<T> geWithDate(String key, String field) {
        String value = DPUtil.trim(DPUtil.parseString(param.get(key)));
        if(!DPUtil.empty(value)) {
            predicates.add(builder.ge(root.get(field), DPUtil.dateTime2millis(value, dateFormat)));
        }
        return this;
    }

    public SpecificationHelper<T> leWithDate(String key, String field) {
        String value = DPUtil.trim(DPUtil.parseString(param.get(key)));
        if(!DPUtil.empty(value)) {
            predicates.add(builder.le(root.get(field), DPUtil.dateTime2millis(value, dateFormat) + 999));
        }
        return this;
    }

    public SpecificationHelper<T> betweenWithDate(String key) {
        return betweenWithDate(key, key);
    }

    public SpecificationHelper<T> betweenWithDate(String key, String field) {
        String value = DPUtil.trim(DPUtil.parseString(param.get(key)));
        if(!DPUtil.empty(value)) {
            predicates.add(builder.le(root.get(field), DPUtil.dateTime2millis(value, dateFormat) + 999));
        }
        return geWithDate(key + "Begin", field).leWithDate(key + "End", field);
    }

    public SpecificationHelper<T> ge(String key) {
        return ge(key, key);
    }

    public SpecificationHelper<T> ge(String key, String field) {
        String value = DPUtil.trim(DPUtil.parseString(param.get(key)));
        if(!DPUtil.empty(value)) {
            predicates.add(builder.ge(root.get(field), DPUtil.parseDouble(value)));
        }
        return this;
    }

    public SpecificationHelper<T> gt(String key) {
        return gt(key, key);
    }

    public SpecificationHelper<T> gt(String key, String field) {
        String value = DPUtil.trim(DPUtil.parseString(param.get(key)));
        if(!DPUtil.empty(value)) {
            predicates.add(builder.gt(root.get(field), DPUtil.parseDouble(value)));
        }
        return this;
    }

    public SpecificationHelper<T> le(String key) {
        return le(key, key);
    }

    public SpecificationHelper<T> le(String key, String field) {
        String value = DPUtil.trim(DPUtil.parseString(param.get(key)));
        if(!DPUtil.empty(value)) {
            predicates.add(builder.le(root.get(field), DPUtil.parseDouble(value)));
        }
        return this;
    }

    public SpecificationHelper<T> lt(String key) {
        return lt(key, key);
    }

    public SpecificationHelper<T> lt(String key, String field) {
        String value = DPUtil.trim(DPUtil.parseString(param.get(key)));
        if(!DPUtil.empty(value)) {
            predicates.add(builder.lt(root.get(field), DPUtil.parseDouble(value)));
        }
        return this;
    }

    public SpecificationHelper<T> between(String key) {
        return between(key, key);
    }

    public SpecificationHelper<T> between(String key, String field) {
        return ge(key + "Begin", field).lt(key + "End", field);
    }

    public SpecificationHelper<T> in(String key) {
        return in(key, key);
    }

    public SpecificationHelper<T> in(String key, String field) {
        List<String> value = DPUtil.parseStringList(param.get(key));
        if (value.size() > 0) predicates.add(root.get(field).in(value));
        return this;
    }

    public SpecificationHelper<T> notIn(String key) {
        return notIn(key, key);
    }

    public SpecificationHelper<T> notIn(String key, String field) {
        List<String> value = DPUtil.parseStringList(param.get(key));
        if (value.size() > 0) predicates.add(root.get(field).in(value).not());
        return this;
    }

    public SpecificationHelper<T> exceptIds() {
        return notIn("exceptIds", "id");
    }

    public SpecificationHelper<T> functionFindInSet(String key) {
        return functionFindInSet(key, key);
    }

    public SpecificationHelper<T> functionFindInSet(String key, String field) {
        List<String> value = DPUtil.parseStringList(param.get(key));
        List<Predicate> predicates = new ArrayList<>();
        for (String item : value) {
            Expression<Integer> expression = builder.function("FIND_IN_SET", Integer.class, builder.literal(item), root.get(field));
            predicates.add(builder.gt(expression, 0));
        }
        if (predicates.size() > 0) {
            this.predicates.add(builder.or(predicates.toArray(new Predicate[0])));
        }
        return this;
    }

    public SpecificationHelper<T> add(Predicate e) {
        predicates.add(e);
        return this;
    }

}
