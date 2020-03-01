package com.iisquare.fs.app.crawler.helper;

import com.iisquare.fs.base.core.util.DPUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.sql.*;
import java.sql.Date;
import java.util.*;
import java.util.Map.Entry;

public class SQLHelper implements Closeable {

    protected final static Logger logger = LoggerFactory.getLogger(SQLHelper.class);
    public static final String PARAM_PREFIX = ":qp";
    private static final String PARAM_REGEX = ":[a-zA-Z0-9_]+";
    private int retry = 1; // 失败重试次数
    private Connection resource; // 当前连接资源
    private PreparedStatement statement; // 当前预处理对象
    private String select;
    private String where;
    private String groupBy;
    private String having;
    private String orderBy;
    private Integer limit;
    private Integer offset;
    private List<String[]> join;
    private String sql;
    private SQLException exception;
    private Map<String, Object> pendingParams = new LinkedHashMap<>();
    private boolean transientNeedUpdate = false; // 插入失败时转为更新，执行insert之后自动设置为false
    private String url;
    private String username;
    private String password;
    private String tableName;

    public SQLHelper(String url, String username, String password, String tableName) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.tableName = tableName;
        reload();
    }

    public static SQLHelper build(String url, String username, String password, String tableName) {
        return new SQLHelper(url, username, password, tableName);
    }

    public boolean reload() {
        close();
        try {
            resource = DriverManager.getConnection(url, username, password);
            return true;
        } catch (SQLException e) {
            exception = e;
            return false;
        }
    }

    /**
     * 设置失败重试次数
     */
    public void setRetry(int retry) {
        this.retry = retry;
    }

    /**
     * 插入失败时转为更新，执行insert之后自动设置为false
     */
    public void transientNeedUpdate() {
        transientNeedUpdate = true;
    }

    /**
     * 最后一次执行的SQL语句(不含值)
     */
    public String getLastSql() {
        return sql;
    }

    /**
     * 最后一次执行的异常，执行成功时不会修改该返回值
     */
    public Exception getLastException() {
        return exception;
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
        pendingParams.put(name, value);
        return this;
    }

    public SQLHelper bindValues(Map<String, Object> values) {
        if(null == values) return this;
        for (Entry<String, Object> entry : values.entrySet()) {
            pendingParams.put(entry.getKey(), entry.getValue());
        }
        return this;
    }

    /**
     * 清除已绑定的查询参数数组
     */
    public SQLHelper cancelBindValues() {
        pendingParams = new LinkedHashMap<>();
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

    private void bindPendingParams() throws SQLException {
        String sql = this.sql;
        List<Object> list = new ArrayList<>();
        List<String> params = DPUtil.getMatcher(PARAM_REGEX, sql, false); // 获取全部命名参数
        int size = params.size();
        for (int index = 0; index < size; index++) {
            String key = params.get(index);
            Object value = pendingParams.get(key);
            if(null == value) { // null值
                list.add("");
            } else if(value.getClass().isArray()) { // 数组
                Object[] values = (Object[]) value;
                sql = sql.replaceFirst(key, DPUtil.implode(", ", DPUtil.getFillArray(values.length, "?")));
                for (Object item : values) {
                    list.add(item);
                }
            } else {
                list.add(value);
            }
        }
        sql = sql.replaceAll(PARAM_REGEX, "?"); // 替换命名参数为占位符
        if(null == resource) throw new RuntimeException("resource is null", exception);
        statement = resource.prepareStatement(sql);
        size = list.size();
        for (int index = 0; index < size;) {
            Object param = list.get(index++);
            if(null == param) {
                statement.setObject(index, param);
            } else if (param instanceof String) {
                statement.setString(index, param.toString());
            } else if (param instanceof Date) {
                statement.setDate(index, Date.valueOf(param.toString()));
            } else if (param instanceof Boolean) {
                statement.setBoolean(index, (Boolean) (param));
            } else if (param instanceof Integer) {
                statement.setInt(index, (Integer) param);
            } else if (param instanceof Float) {
                statement.setFloat(index, (Float) param);
            } else if (param instanceof Double) {
                statement.setDouble(index, (Double) param);
            } else {
                statement.setObject(index, param);
            }
        }
        pendingParams = new LinkedHashMap<>();
    }

    private boolean execute(int retry) {
        if(null == sql || "".equals(sql)) return false;
        boolean forRead = true;
        try {
            bindPendingParams();
            return statement.execute();
        } catch (SQLException e) {
            exception = e;
            //if(retry > 0 && 2006 == e.getErrorCode()) {
            if(retry > 0 && null == e.getSQLState()) {
                reload();
                return execute(--retry);
            }
            return false;
        }
    }

    /**
     * 执行一条查询语句，返回PreparedStatement对象
     * 不建议直接使用，需要自己处理参数安全转义
     */
    public Statement execute(String sql) {
        boolean forRead = true;
        try {
            statement = resource.prepareStatement(sql);
            if (statement.execute()) return statement;
            return null;
        } catch (SQLException e) {
            exception = e;
            return null;
        }
    }

    private Number executeUpdate(int retry) {
        if(null == sql || "".equals(sql)) return null;
        try {
            bindPendingParams();
            return statement.executeUpdate();
        } catch (SQLException e) {
            exception = e;
            if(retry > 0 && null == e.getSQLState()) {
                reload();
                return executeUpdate(--retry);
            }
            return null;
        }
    }

    /**
     * 执行一条更新语句，返回受影响行数
     * 不建议直接使用，需要自己处理参数安全转义
     */
    public Number executeUpdate(String sql) {
        if(null == sql || "".equals(sql)) return null;
        try {
            statement = resource.prepareStatement(sql);
            return statement.executeUpdate();
        } catch (SQLException e) {
            exception = e;
            return null;
        }
    }

    /**
     * 返回查询的数据资源对象，使用getResultSet()方法遍历数据，如果取出数据后要循环处理，建议使用该方法
     */
    public PreparedStatement query() {
        sql = build();
        if(execute(retry)) {
            return statement;
        }
        return null;
    }

    /**
     * 读取ResultSet到List<Map<String, Object>>
     */
    private List<Map<String, Object>> fetchResultSet(ResultSet rs) throws Exception {
        ResultSetMetaData rsmd = rs.getMetaData();
        List<Map<String, Object>> tempList = new ArrayList<>();
        Map<String, Object> tempHash = null;
        while (rs.next()) {
            tempHash = new LinkedHashMap<String, Object>();
            for (int i = 0; i < rsmd.getColumnCount(); i++) {
                tempHash.put(rsmd.getColumnName(i + 1), rs.getObject(rsmd.getColumnName(i + 1)));
            }
            tempList.add(tempHash);
        }
        return tempList;
    }

    /**
     * 返回查询的所有数据数组
     */
    public List<Map<String, Object>> all() {
        sql = build();
        if (!execute(retry)) return null;
        try {
            return fetchResultSet(statement.getResultSet());
        } catch (Exception e) {
            logger.warn("all fetchResultSet failed", e);
            return null;
        } finally {
            close();
        }
    }

    public Map<String, Object> one() {
        Integer offset = this.offset;
        Integer limit = this.offset;
        this.offset = null;
        this.limit = 1;
        sql = build();
        List<Map<String, Object>> list = all();
        this.offset = offset;
        this.limit = limit;
        if(null == list) return null;
        if(list.isEmpty()) return new LinkedHashMap<>();
        return list.get(0);
    }

    public Number calculate(String type, String field) {
        String fields = select;
        select = type + "(" + field + ") as calculate";
        Map<String, Object> map = one();
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

    private Number lastInsertId(PreparedStatement statement) {
        try {
            ResultSet keys = statement.executeQuery("SELECT last_insert_id();");
            if(keys.next()) {
                return (Number) keys.getObject(1);
            }
            return null;
        } catch (SQLException e) {
            return null;
        }
    }

    private String duplicateUpdate(Collection<String> fields) {
        List<String> list = new ArrayList<>();
        for (String field : fields) {
            list.add(field + " = VALUES(" + field + ")");
        }
        return " ON DUPLICATE KEY UPDATE " + DPUtil.implode(", ", DPUtil.collectionToStringArray(list));
    }

    /**
     * 预处理表数据
     */
    public LinkedHashMap<String, Object> prepareData(Map<String, Object> data, LinkedHashMap<String, Map<String, Object>> collumnNames) {
        if(null == data) return null;
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        for(Entry<String, Map<String, Object>> entry : collumnNames.entrySet()) {
            String key = entry.getKey();
            if(!data.containsKey(key)) continue ;
            result.put(key, data.get(key));
        }
        return result;
    }

    /**
     * 单条插入
     */
    public Number insert(Map<String, Object> data, boolean needUpdate) {
        Map<String, Object> params = new LinkedHashMap<>(); // 字段参数值
        int i = 0;
        for(Entry<String, Object> entry : data.entrySet()) {
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
        if(null != executeUpdate(retry)) {
            Number lastId = lastInsertId(statement);
            close();
            return lastId;
        } else if(null != exception) {
            return null;
        }
        return null;
    }

    /**
     * 批量插入
     */
    public Number batchInsert(List<Map<String, Object>> datas, boolean needUpdate) {
        Set<String> keys = null;
        List<String> values = new ArrayList<>();
        for (Map<String, Object> data : datas) {
            if(null == keys) keys = data.keySet();
            List<Object> list = new ArrayList<>();
            for (String key : keys) {
                Object value = data.get(key);
                if(null == value) {
                    value = "''";
                } else {
                    value = "'" + value.toString().replaceAll("'", "''") + "'";
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
        sql = sb.toString().replaceAll("\\:", "");
        if(null != executeUpdate(retry)) {
            Number lastId = lastInsertId(statement);
            close();
            return lastId;
        }
        return null;
    }

    public Number update(Map<String, Object> data) {
        List<String> list = new ArrayList<>(); // 设置字段
        Map<String, Object> params = new LinkedHashMap<>(); // 字段参数值
        int i = 0;
        for(Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            String qpKey = PARAM_PREFIX + i++;
            list.add(key + "=" + qpKey);
            params.put(qpKey, entry.getValue());
        }
        StringBuilder sb = new StringBuilder();
        sb.append("UPDATE ").append(tableName);
        sb.append(" SET ").append(DPUtil.implode(", ", DPUtil.collectionToArray(list)));
        if(null != where) sb.append(" WHERE ").append(where);
        sql = sb.toString();
        bindValues(params);
        Number result = executeUpdate(retry);
        close();
        return result;
    }

    public Number delete() {
        StringBuilder sb = new StringBuilder();
        sb.append("DELETE FROM ").append(tableName);
        if(null != where) sb.append(" WHERE ").append(where);
        sql = sb.toString();
        Number result = executeUpdate(retry);
        close();
        return result;
    }

    public void close() {
        if(null != statement) {
            try {
                statement.close();
            } catch (SQLException e) {

            } finally {
                statement = null;
            }
        }
        if(null != resource) {
            try {
                resource.close();
            } catch (SQLException e) {

            } finally {
                resource = null;
            }
        }
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
        pendingParams = new LinkedHashMap<>();
        transientNeedUpdate = false;
        reload();
        return this;
    }

}