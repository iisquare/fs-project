package com.iisquare.fs.base.jpa.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.iisquare.fs.base.core.tool.SQLBuilder;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.SQLUtil;
import com.iisquare.fs.base.jpa.core.SQLBatchCallback;
import com.iisquare.fs.base.jpa.util.JDBCUtil;
import com.iisquare.fs.base.jpa.util.JPAUtil;
import org.springframework.jdbc.support.JdbcUtils;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SQLHelper {

    private String sql;
    private EntityManager manager;
    private SQLBuilder builder;

    private SQLHelper(EntityManager manager, Class entity) {
        this.manager = manager;
        this.builder = SQLBuilder.build(JPAUtil.tableName(manager, entity));
    }

    public static SQLHelper build(EntityManager entityManager, Class entity) {
        return new SQLHelper(entityManager, entity);
    }

    public SQLBuilder builder() {
        return this.builder;
    }

    public SQLHelper where(String condition, Object... params) {
        this.builder.where(condition, params);
        return this;
    }

    public String lastSql() {
        return this.sql;
    }

    public Query query(String sql, Class requiredType) {
        Query query;
        if(null == requiredType) {
            query = manager.createNativeQuery(sql);
        } else {
            query = manager.createNativeQuery(sql, requiredType);
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
    public Number batchInsert(List<Map<String, Object>> data, boolean needUpdate, String... fieldsUpdate) {
        sql = builder.batchInsert(data, needUpdate, fieldsUpdate);
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

    public PreparedStatement statement(Connection connection, String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        List<Object> list = new ArrayList<>();
        Map<String, Object> pendingParams = builder.params();
        List<String> params = DPUtil.matcher(SQLBuilder.PARAM_PREFIX, sql, false); // 获取全部命名参数
        int size = params.size();
        for (int index = 0; index < size; index++) {
            String key = params.get(index);
            Object value = pendingParams.get(key);
            if(null == value) { // null值
                list.add("");
            } else if(value.getClass().isArray()) { // 数组
                Object[] values = (Object[]) value;
                sql = sql.replaceFirst(key, DPUtil.implode(", ", DPUtil.fillArray("?", values.length)));
                for (Object item : values) {
                    list.add(item);
                }
            } else {
                list.add(value);
            }
        }
        sql = sql.replaceAll(SQLBuilder.PARAM_PREFIX, "?"); // 替换命名参数为占位符
        PreparedStatement statement = connection.prepareStatement(sql, resultSetType, resultSetConcurrency);
        size = list.size();
        for (int index = 0; index < size;) {
            Object param = list.get(index++);
            if(null == param) {
                statement.setObject(index, null);
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
        return statement;
    }

    public PreparedStatement statement(Connection connection, String sql) throws SQLException {
        return statement(connection, sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    }

    public boolean batch(SQLBatchCallback callback) throws Exception {
        String driverClassName = JPAUtil.driverClassName(manager);
        Connection connection = JPAUtil.connection(manager);
        if ("org.postgresql.Driver".equals(driverClassName)) {
            connection.setAutoCommit(false);
        }
        sql = builder.all();
        PreparedStatement statement = null;
        ResultSet rs = null;
        try {
            statement = statement(connection, sql);
            if ("org.postgresql.Driver".equals(driverClassName)) {
                statement.setFetchSize(callback.fetchSize);
            } else {
                statement.setFetchDirection(ResultSet.FETCH_REVERSE);
                // 被HikariProxyPreparedStatement代理，不能直接调用enableStreamingResults
                statement.setFetchSize(Integer.MIN_VALUE);
            }
            rs = statement.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            int count = meta.getColumnCount();
            ArrayNode result = DPUtil.arrayNode();
            while (rs.next()) {
                Map<String, Object> item = new LinkedHashMap<>();
                for (int i = 1; i <= count; i++) {
                    item.put(meta.getColumnName(i), rs.getObject(i));
                }
                result.add(DPUtil.toJSON(item));
                if (result.size() >= callback.batchSize) {
                    if (!callback.call(result)) return false;
                    result = DPUtil.arrayNode();
                }
            }
            if (result.size() > 0) {
                return callback.call(result);
            }
            return true;
        } catch (SQLException e) {
            throw e;
        } finally {
            JdbcUtils.closeResultSet(rs);
            JdbcUtils.closeStatement(statement);
            JPAUtil.release(manager, connection);
        }
    }

    public boolean step(SQLBatchCallback callback) throws Exception {
        JsonNode last = null;
        String table = builder.tableName();
        ArrayNode result = DPUtil.arrayNode();
        Connection connection = JPAUtil.connection(manager);
        try {
            String[] pks = DPUtil.toArray(String.class, JDBCUtil.pks(connection, table));
            do {
                StringBuilder sb = new StringBuilder();
                sb.append("select * from ").append(table).append(" where ");
                sb.append(SQLUtil.pkWhere(last, pks));
                sb.append(" order by ").append(DPUtil.implode(", ", pks)).append(" limit ").append(callback.fetchSize).append(";");
                String sql = sb.toString();
                try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sql)) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int size = 0;
                    int count = meta.getColumnCount();
                    while (rs.next()) {
                        size++;
                        Map<String, Object> item = new LinkedHashMap<>();
                        for (int i = 1; i <= count; i++) {
                            item.put(meta.getColumnName(i), rs.getObject(i));
                        }
                        last = DPUtil.toJSON(item);
                        result.add(last);
                        if (result.size() >= callback.batchSize) {
                            if (!callback.call(result)) return false;
                            result = DPUtil.arrayNode();
                        }
                    }
                    if (0 == size) break;
                }
            } while (true);
            if (result.size() > 0) {
                return callback.call(result);
            }
            return true;
        } finally {
            JPAUtil.release(manager, connection);
        }
    }

}
