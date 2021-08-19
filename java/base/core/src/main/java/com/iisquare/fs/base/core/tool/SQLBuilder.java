package com.iisquare.fs.base.core.tool;

import com.iisquare.fs.base.core.util.DPUtil;

import java.beans.Transient;
import java.lang.reflect.Field;
import java.util.*;

public class SQLBuilder {

    public static final String PARAM_PREFIX = "qp";
    public static final String ALIAS_CALCULATE = "calculate";
    private String select;
    private String where;
    private String groupBy;
    private String having;
    private String orderBy;
    private Integer limit;
    private Integer offset;
    private List<String[]> join;
    private String tableName;
    private String[] tableColumns = new String[]{};
    private Map<String, Object> params = new LinkedHashMap<>();

    private SQLBuilder(String tableName) {
        this.tableName = escape(apply(tableName));
    }

    public static SQLBuilder build(String tableName) {
        return new SQLBuilder(tableName);
    }

    public String tableName() {
        return tableName;
    }

    public String[] tableColumns() {
        return tableColumns;
    }

    public Map<String, Object> params() {
        return params;
    }

    public SQLBuilder tableColumns(String... columns) {
        tableColumns = columns;
        return this;
    }

    public SQLBuilder tableColumns(Set<String> columns) {
        return tableColumns(DPUtil.toArray(String.class, columns));
    }

    public SQLBuilder tableColumns(Class<?> entity) {
        Field[] fields = entity.getFields();
        Set<String> columns = new HashSet<>();
        for (Field field : fields) {
            if(null != field.getAnnotation(Transient.class)) continue;
            columns.add(apply(field.getName()));
        }
        return tableColumns(columns);
    }

    /**
     * 预处理表数据
     */
    public Map<String, Object> prepare(Map<String, Object> data) {
        if (null == data || tableColumns.length < 1) return data;
        Map<String, Object> result = new LinkedHashMap<>();
        for(String column : tableColumns) {
            if(!data.containsKey(column)) continue ;
            result.put(column, data.get(column));
        }
        return result;
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
        return Character.isLowerCase(before) && Character.isUpperCase(current) && Character.isLowerCase(after);
    }

    private Map<String, Object> buildValues(Object... values) {
        if(null == values) return null;
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            map.put(values[i].toString(), values[i + 1]);
        }
        return map;
    }

    public SQLBuilder bindValue(String name, Object value) {
        params.put(name, value);
        return this;
    }

    public SQLBuilder bindValues(Map<String, Object> values) {
        if(null == values) return this;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            params.put(entry.getKey(), entry.getValue());
        }
        return this;
    }

    /**
     * 清除已绑定的查询参数数组
     */
    public SQLBuilder cancelBindValues() {
        params = new LinkedHashMap<>();
        return this;
    }

    public SQLBuilder select(String columns) {
        select = columns;
        return this;
    }

    public SQLBuilder where(String condition, Object... params) {
        return where(condition, buildValues(params));
    }

    public SQLBuilder where (String condition, Map<String, Object> params) {
        this.where = condition;
        return bindValues(params);
    }

    private SQLBuilder join(String type, String table, String on) {
        if(null == join) join = new ArrayList<>();
        join.add(new String[]{type, table, on});
        return this;
    }

    public SQLBuilder innerJoin(String table, String on, Object... params) {
        return innerJoin(table, on, buildValues(params));
    }

    public SQLBuilder innerJoin(String table, String on, Map<String, Object> params) {
        join("INNER JOIN", table, on);
        return bindValues(params);
    }

    public SQLBuilder leftJoin(String table, String on, Object... params) {
        return leftJoin(table, on, buildValues(params));
    }

    public SQLBuilder leftJoin(String table, String on, Map<String, Object> params) {
        join("LEFT JOIN", table, on);
        return bindValues(params);
    }

    public SQLBuilder rightJoin(String table, String on, Object... params) {
        return rightJoin(table, on, buildValues(params));
    }

    public SQLBuilder rightJoin(String table, String on, Map<String, Object> params) {
        join("RIGHT JOIN", table, on);
        return bindValues(params);
    }

    public SQLBuilder groupBy(String columns) {
        groupBy = columns;
        return this;
    }

    public SQLBuilder orderBy(String columns) {
        orderBy = columns;
        return this;
    }

    public SQLBuilder having(String condition, Object... params) {
        return having(condition, buildValues(params));
    }

    public SQLBuilder having(String condition, Map<String, Object> params) {
        having = condition;
        return bindValues(params);
    }

    public SQLBuilder limit(int limit) {
        this.limit = limit;
        return this;
    }

    public SQLBuilder offset(int offset) {
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

    public String all() {
        return build();
    }

    public String one() {
        Integer offset = this.offset;
        Integer limit = this.offset;
        this.offset = null;
        this.limit = 1;
        String sql = build();
        this.offset = offset;
        this.limit = limit;
        return sql;
    }

    public String calculate(String type, String field) {
        String fields = select;
        select = type + "(" + field + ") as " + ALIAS_CALCULATE;
        String sql = one();
        select = fields;
        return sql;
    }

    public String count() {
        return count("*");
    }

    public String count(String field) {
        return calculate("COUNT", field);
    }

    public String sum(String field) {
        return calculate("SUM", field);
    }

    public String average(String field) {
        return calculate("AVG", field);
    }

    public String min(String field) {
        return calculate("MIN", field);
    }

    public String max(String field) {
        return calculate("MAX", field);
    }

    public String lastInsertId() {
        return "SELECT last_insert_id();";
    }

    private String duplicateUpdate(Collection<String> fields) {
        List<String> list = new ArrayList<>();
        for (String field : fields) {
            list.add(field + " = VALUES(" + field + ")");
        }
        return " ON DUPLICATE KEY UPDATE " + DPUtil.implode(", ", DPUtil.toArray(String.class, list));
    }

    /**
     * 单条插入
     */
    public String insert(Map<String, Object> data, boolean needUpdate, String... fieldsUpdate) {
        data = prepare(data);
        Map<String, Object> params = new LinkedHashMap<>(); // 字段参数值
        int i = 0;
        for(Map.Entry<String, Object> entry : data.entrySet()) {
            String qpKey = PARAM_PREFIX + i++;
            params.put(qpKey, entry.getValue());
        }
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO ").append(tableName);
        sb.append(" (").append(DPUtil.implode(", ", DPUtil.toArray(String.class, data.keySet()))).append(")");
        sb.append(" VALUES (").append(DPUtil.implode(", ", DPUtil.toArray(String.class, params.keySet()))).append(")");
        if(needUpdate) sb.append(duplicateUpdate(fieldsUpdate.length > 0 ? Arrays.asList(fieldsUpdate) : data.keySet()));
        bindValues(params);
        return sb.toString();
    }

    public static String escape(String str) {
        if(null == str) return null;
        return str.replaceAll("'", "''");
    }

    /**
     * 批量插入
     */
    public String batchInsert(List<Map<String, Object>> datas, boolean needUpdate, String... fieldsUpdate) {
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
                    value = "'" + escape(value.toString()) + "'";
                }
                list.add(value);
            }
            values.add("(" + DPUtil.implode(", ", DPUtil.toArray(Object.class, list)) + ")");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO ").append(tableName);
        sb.append(" (").append(DPUtil.implode(", ", DPUtil.toArray(String.class, keys))).append(")");
        sb.append(" VALUES ").append(DPUtil.implode(", ", DPUtil.toArray(String.class, values)));
        if(needUpdate) sb.append(duplicateUpdate(fieldsUpdate.length > 0 ? Arrays.asList(fieldsUpdate) : keys));
        return sb.toString();
    }

    public String update(Map<String, Object> data) {
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
        sb.append(" SET ").append(DPUtil.implode(", ", DPUtil.toArray(String.class, list)));
        if(null != where) sb.append(" WHERE ").append(where);
        bindValues(params);
        return sb.toString();
    }

    public String delete() {
        StringBuilder sb = new StringBuilder();
        sb.append("DELETE FROM ").append(tableName);
        if(null != where) sb.append(" WHERE ").append(where);
        return sb.toString();
    }

    /**
     * 清除 select() where() limit() offset() orderBy() groupBy() join() having()
     */
    public SQLBuilder reset() {
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
