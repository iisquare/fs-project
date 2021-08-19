package com.iisquare.fs.base.jpa.helper;

import com.iisquare.fs.base.core.tool.SQLBuilder;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;
import java.util.Map;

public class SQLHelper {

    private String sql;
    private EntityManager entityManager;
    private SQLBuilder builder;

    private SQLHelper(EntityManager entityManager, String tableName) {
        this.entityManager = entityManager;
        this.builder = SQLBuilder.build(tableName);
    }

    public static SQLHelper build(EntityManager entityManager, String tableName) {
        return new SQLHelper(entityManager, tableName);
    }

    public SQLBuilder builder() {
        return this.builder;
    }

    public String lastSql() {
        return this.sql;
    }

    private Query query(String sql, Class requiredType) {
        Query query;
        if(null == requiredType) {
            query = entityManager.createNativeQuery(sql);
        } else {
            query = entityManager.createNativeQuery(sql, requiredType);
        }
        for (Map.Entry<String, Object> entry : builder.params().entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
        return query;
    }

    public <T> List<T> all(Class<T> requiredType) {
        sql = builder.all();
        return query(sql, requiredType).getResultList();
    }

    public <T> T one(Class<T> requiredType) {
        sql = builder.one();
        return (T) query(sql, requiredType).getSingleResult();
    }

    public Number calculate(String type, String field) {
        sql = builder.calculate(type, field);
        Map<String, Object> map = (Map<String, Object>) query(sql, Map.class).getSingleResult();
        if(null == map) return null;
        return (Number) map.get(SQLBuilder.ALIAS_CALCULATE);
    }

    public Number count() {
        return count("*");
    }

    public Number count(String field) {
        return calculate("COUNT", field);
    }

    public Number sum(String field) {
        return calculate("SUM", field);
    }

    public Number average(String field) {
        return calculate("AVG", field);
    }

    public Number min(String field) {
        return calculate("MIN", field);
    }

    public Number max(String field) {
        return calculate("MAX", field);
    }

    /**
     * 返回查询的条件是否存在
     */
    public boolean exists() {
        return count().intValue() > 0;
    }

    private Number lastInsertId() {
        String sql = builder.lastInsertId();
        return (Number) query(sql, null).getSingleResult();
    }

    /**
     * 单条插入
     */
    public Number insert(Map<String, Object> data, boolean needUpdate, String... fieldsUpdate) {
        sql = builder.insert(data, needUpdate, fieldsUpdate);
        query(sql, null).executeUpdate();
        return lastInsertId();
    }

    /**
     * 批量插入
     */
    public Number batchInsert(List<Map<String, Object>> datas, boolean needUpdate, String... fieldsUpdate) {
        sql = builder.batchInsert(datas, needUpdate, fieldsUpdate);
        query(sql, null).executeUpdate();
        return lastInsertId();
    }

    public Number update(Map<String, Object> data) {
        sql = builder.update(data);
        return query(sql, null).executeUpdate();
    }

    public Number delete() {
        sql = builder.delete();
        return query(sql, null).executeUpdate();
    }

}
