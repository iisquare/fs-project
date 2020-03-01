package com.iisquare.fs.base.jpa.helper;

import com.iisquare.fs.base.core.util.DPUtil;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.Transient;
import java.lang.reflect.Field;
import java.util.*;

public class SQLHelper {

    public static final String PARAM_PREFIX = "qp";
    private String select;
    private String where;
    private String groupBy;
    private String having;
    private String orderBy;
    private Integer limit;
    private Integer offset;
    private List<String[]> join;
    private String sql;
    private Map<String, Object> params = new LinkedHashMap<>();
    private EntityManager entityManager;
    private String tableName;

    private SQLHelper(EntityManager entityManager, String tableName) {
        this.entityManager = entityManager;
        this.tableName = escapeSql(apply(tableName));
    }

    public static SQLHelper build(EntityManager entityManager, String tableName) {
        return new SQLHelper(entityManager, tableName);
    }

    /**
     * 预处理表数据
     */
    public LinkedHashMap<String, Object> prepare(Map<String, Object> data, Set<String> columns) {
        if(null == data) return null;
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        for(String column : columns) {
            if(!data.containsKey(column)) continue ;
            result.put(column, data.get(column));
        }
        return result;
    }

    /**
     * 预处理表数据
     */
    public LinkedHashMap<String, Object> prepare(Map<String, Object> data, String... columns) {
        if(null == data) return null;
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        for(String column : columns) {
            if(!data.containsKey(column)) continue ;
            result.put(column, data.get(column));
        }
        return result;
    }

    /**
     * 预处理表数据
     */
    public LinkedHashMap<String, Object> prepare(Map<String, Object> data, Class<?> entity) {
        Field[] fields = entity.getFields();
        Set<String> columns = new HashSet<>();
        for (Field field : fields) {
            if(null != field.getAnnotation(Transient.class)) continue;
            columns.add(apply(field.getName()));
        }
        return prepare(data, columns);
    }

    private String apply(String name) {
        if (name == null) return null;
        StringBuilder builder = new StringBuilder(name.replace('.', '_'));
        for (int i = 1; i < builder.length() - 1; i++) {
            if (isUnderscoreRequired(builder.charAt(i - 1), builder.charAt(i),
                builder.charAt(i + 1))) {
                builder.insert(i++, '_');
            }
        }
        return builder.toString();
    }

    private boolean isUnderscoreRequired(char before, char current, char after) {
        return Character.isLowerCase(before) && Character.isUpperCase(current)
            && Character.isLowerCase(after);
    }

    /**
     * 最后一次执行的SQL语句(不含值)
     */
    public String getLastSql() {
        return sql;
    }

    private Map<String, Object> buildValues(Object... values) {
        if(null == values) return null;
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            map.put(values[i].toString(), values[i + 1]);
        }
        return map;
    }

    public SQLHelper bindValue(String name, Object value) {
        params.put(name, value);
        return this;
    }

    public SQLHelper bindValues(Map<String, Object> values) {
        if(null == values) return this;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            params.put(entry.getKey(), entry.getValue());
        }
        return this;
    }

    /**
     * 清除已绑定的查询参数数组
     */
    public SQLHelper cancelBindValues() {
        params = new LinkedHashMap<>();
        return this;
    }

    public SQLHelper select(String columns) {
        select = columns;
        return this;
    }

    public SQLHelper where(String condition, Object... params) {
        return where(condition, buildValues(params));
    }

    public SQLHelper where (String condition, Map<String, Object> params) {
        this.where = condition;
        return bindValues(params);
    }

    private SQLHelper join(String type, String table, String on) {
        if(null == join) join = new ArrayList<>();
        join.add(new String[]{type, table, on});
        return this;
    }

    public SQLHelper innerJoin(String table, String on, Object... params) {
        return innerJoin(table, on, buildValues(params));
    }

    public SQLHelper innerJoin(String table, String on, Map<String, Object> params) {
        join("INNER JOIN", table, on);
        return bindValues(params);
    }

    public SQLHelper leftJoin(String table, String on, Object... params) {
        return leftJoin(table, on, buildValues(params));
    }

    public SQLHelper leftJoin(String table, String on, Map<String, Object> params) {
        join("LEFT JOIN", table, on);
        return bindValues(params);
    }

    public SQLHelper rightJoin(String table, String on, Object... params) {
        return rightJoin(table, on, buildValues(params));
    }

    public SQLHelper rightJoin(String table, String on, Map<String, Object> params) {
        join("RIGHT JOIN", table, on);
        return bindValues(params);
    }

    public SQLHelper groupBy(String columns) {
        groupBy = columns;
        return this;
    }

    public SQLHelper orderBy(String columns) {
        orderBy = columns;
        return this;
    }

    public SQLHelper having(String condition, Object... params) {
        return having(condition, buildValues(params));
    }

    public SQLHelper having(String condition, Map<String, Object> params) {
        having = condition;
        return bindValues(params);
    }

    public SQLHelper limit(int limit) {
        this.limit = limit;
        return this;
    }

    public SQLHelper offset(int offset) {
        this.offset = offset;
        return this;
    }

    private String build() {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ").append(null == select ? "*" : select);
        sb.append(" FROM ").append(tableName);
        if(null != join) { // JOIN
            for (String[] item : join) {
                sb.append(" ").append(item[0]).append(" ").append(item[1]);
                if(null == item[2]) continue;
                sb.append(" ON ").append(item[2]);
            }
        }
        if(null != where) sb.append(" WHERE ").append(where);
        if(null != groupBy) sb.append(" GROUP BY ").append(groupBy);
        if(null != having) sb.append(" HAVING ").append(having);
        if(null != orderBy) sb.append(" ORDER BY ").append(orderBy);
        if(null != offset || null != limit) {
            sb.append(" LIMIT ");
            if(null != offset) sb.append(offset).append(", ");
            sb.append(null == limit ? 0 : limit);
        }
        return sb.toString();
    }

    private Query query(String sql, Class requiredType) {
        Query query;
        if(null == requiredType) {
            query = entityManager.createNativeQuery(sql);
        } else {
            query = entityManager.createNativeQuery(sql, requiredType);
        }
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
        return query;
    }

    /**
     * 返回查询的所有数据数组
     */
    public <T> List<T> all(Class<T> requiredType) {
        sql = build();
        return query(sql, requiredType).getResultList();
    }

    public <T> T one(Class<T> requiredType) {
        Integer offset = this.offset;
        Integer limit = this.offset;
        this.offset = null;
        this.limit = 1;
        sql = build();
        Object result = query(sql, requiredType).getSingleResult();
        this.offset = offset;
        this.limit = limit;
        return (T) result;
    }

    public Number calculate(String type, String field) {
        String fields = select;
        select = type + "(" + field + ") as calculate";
        Map<String, Object> map = one(Map.class);
        select = fields;
        if(null == map) return null;
        return (Number) map.get("calculate");
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
        String sql = "SELECT last_insert_id();";
        return (Number) query(sql, null).getSingleResult();
    }

    private String duplicateUpdate(Collection<String> fields) {
        List<String> list = new ArrayList<>();
        for (String field : fields) {
            list.add(field + " = VALUES(" + field + ")");
        }
        return " ON DUPLICATE KEY UPDATE " + DPUtil.implode(", ", DPUtil.collectionToStringArray(list));
    }

    /**
     * 单条插入
     */
    public Number insert(Map<String, Object> data, boolean needUpdate) {
        data = prepare(data);
        Map<String, Object> params = new LinkedHashMap<>(); // 字段参数值
        int i = 0;
        for(Map.Entry<String, Object> entry : data.entrySet()) {
            String qpKey = PARAM_PREFIX + i++;
            params.put(qpKey, entry.getValue());
        }
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO ").append(tableName);
        sb.append(" (").append(DPUtil.implode(", ", DPUtil.collectionToStringArray(data.keySet()))).append(")");
        sb.append(" VALUES (").append(DPUtil.implode(", ", DPUtil.collectionToArray(params.keySet()))).append(")");
        if(needUpdate) sb.append(duplicateUpdate(data.keySet()));
        sql = sb.toString();
        bindValues(params);
        query(sql, null).executeUpdate();
        return lastInsertId();
    }

    public static String escapeSql(String str) {
        if(null == str) return str;
        return str.replaceAll("'", "''");
    }

    /**
     * 批量插入
     */
    public Number batchInsert(List<Map<String, Object>> datas, boolean needUpdate) {
        Set<String> keys = null;
        List<String> values = new ArrayList<>();
        for (Map<String, Object> data : datas) {
            data = prepare(data);
            if(null == keys) keys = data.keySet();
            List<Object> list = new ArrayList<>();
            for (String key : keys) {
                Object value = data.get(key);
                if(null == value) {
                    value = "''";
                } else {
                    value = "'" + escapeSql(value.toString()) + "'";
                }
                list.add(value);
            }
            values.add("(" + DPUtil.implode(", ", DPUtil.collectionToArray(list)) + ")");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO ").append(tableName);
        sb.append(" (").append(DPUtil.implode(", ", DPUtil.collectionToStringArray(keys))).append(")");
        sb.append(" VALUES ").append(DPUtil.implode(", ", DPUtil.collectionToArray(values)));
        if(needUpdate) sb.append(duplicateUpdate(keys));
        sql = sb.toString();
        query(sql, null).executeUpdate();
        return lastInsertId();
    }

    public Number update(Map<String, Object> data) {
        data = prepare(data);
        List<String> list = new ArrayList<>(); // 设置字段
        Map<String, Object> params = new LinkedHashMap<>(); // 字段参数值
        int i = 0;
        for(Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            String qpKey = PARAM_PREFIX + i++;
            list.add(key + "=:" + qpKey);
            params.put(qpKey, entry.getValue());
        }
        StringBuilder sb = new StringBuilder();
        sb.append("UPDATE ").append(tableName);
        sb.append(" SET ").append(DPUtil.implode(", ", DPUtil.collectionToArray(list)));
        if(null != where) sb.append(" WHERE ").append(where);
        sql = sb.toString();
        bindValues(params);
        return query(sql, null).executeUpdate();
    }

    public Number delete() {
        StringBuilder sb = new StringBuilder();
        sb.append("DELETE FROM ").append(tableName);
        if(null != where) sb.append(" WHERE ").append(where);
        sql = sb.toString();
        return query(sql, null).executeUpdate();
    }

    /**
     * 清除 select() where() limit() offset() orderBy() groupBy() join() having()
     */
    public SQLHelper reset() {
        select = null;
        where = null;
        groupBy = null;
        having = null;
        orderBy = null;
        limit = null;
        offset = null;
        join = null;
        params = new LinkedHashMap<>();
        return this;
    }

}
