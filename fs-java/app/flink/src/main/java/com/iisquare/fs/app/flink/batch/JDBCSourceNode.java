package com.iisquare.fs.app.flink.batch;

import com.iisquare.fs.app.flink.core.FlinkRunner;
import com.iisquare.fs.app.flink.core.FlinkTypes;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.DAGCore;
import com.iisquare.fs.base.dag.core.DAGType;
import com.iisquare.fs.base.dag.source.AbstractJDBCSource;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.types.Row;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

/**
 * 批处理关系型数据库读取节点。
 *
 * Flink 2.x 移除了 DataSet API，这里以 BATCH 运行模式下的有界 DataStream 实现：
 * 图构建阶段读取结果集元数据生成 RowTypeInfo，运行阶段按行读取并产出 Row。
 */
public class JDBCSourceNode extends AbstractJDBCSource<DataStream<Row>> {

    @Override
    public DAGType<DataStream<Row>> outputType() {
        return FlinkTypes.DATASTREAM;
    }

    @Override
    public DataStream<Row> process() {
        String url = options.at("/url").asText();
        String sql = options.at("/sql").asText();
        String driver = DAGCore.jdbcDriver(options.at("/driver").asText());
        String username = options.at("/username").asText();
        String password = options.at("/password").asText();
        int fetchSize = options.at("/fetchSize").asInt(0);
        RowTypeInfo typeInfo = meta(url, driver, username, password, sql);
        // BATCH模式要求有界数据源，使用FLIP-27实现
        JDBCBoundedSource bounded = new JDBCBoundedSource(url, driver, username, password, sql, fetchSize, typeInfo);
        DataStream<Row> stream = runner(FlinkRunner.class).stream()
                // 显式传入RowTypeInfo，保证下游Table API能识别字段名与类型
                .fromSource(bounded, WatermarkStrategy.noWatermarks(), getName(), typeInfo)
                .setParallelism(parallelism);
        return stream;
    }


    /**
     * 读取结果集元数据，用于推断 Row 的字段名与类型
     */
    public static RowTypeInfo meta(String url, String driver, String username, String password, String sql) {
        try {
            load(driver);
            try (Connection connection = DriverManager.getConnection(url, username, password);
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                ResultSetMetaData meta = statement.getMetaData();
                int count = meta.getColumnCount();
                String[] names = new String[count];
                TypeInformation<?>[] types = new TypeInformation[count];
                for (int index = 0; index < count; index++) {
                    names[index] = meta.getColumnLabel(index + 1);
                    types[index] = type(meta.getColumnType(index + 1));
                }
                return new RowTypeInfo(types, names);
            }
        } catch (Exception e) {
            throw new RuntimeException("读取数据库元数据失败：" + e.getMessage(), e);
        }
    }

    public static void load(String driver) throws ClassNotFoundException {
        if (!DPUtil.empty(driver)) Class.forName(driver);
    }

    /**
     * JDBC类型到Flink类型的映射；未识别类型按字符串处理
     */
    public static TypeInformation<?> type(int jdbcType) {
        switch (jdbcType) {
            case java.sql.Types.BIT:
            case java.sql.Types.BOOLEAN:
                return Types.BOOLEAN;
            case java.sql.Types.TINYINT:
            case java.sql.Types.SMALLINT:
            case java.sql.Types.INTEGER:
                return Types.INT;
            case java.sql.Types.BIGINT:
                return Types.LONG;
            case java.sql.Types.REAL:
            case java.sql.Types.FLOAT:
                return Types.FLOAT;
            case java.sql.Types.DOUBLE:
                return Types.DOUBLE;
            case java.sql.Types.NUMERIC:
            case java.sql.Types.DECIMAL:
                return Types.BIG_DEC;
            case java.sql.Types.DATE:
                return Types.SQL_DATE;
            case java.sql.Types.TIME:
                return Types.SQL_TIME;
            case java.sql.Types.TIMESTAMP:
            case java.sql.Types.TIMESTAMP_WITH_TIMEZONE:
                return Types.SQL_TIMESTAMP;
            default:
                return Types.STRING;
        }
    }

    /**
     * 按声明的Flink类型读取字段值
     */
    public static Object value(ResultSet result, int index, TypeInformation<?> type) throws Exception {
        if (null == result.getObject(index)) return null;
        if (Types.BOOLEAN.equals(type)) return result.getBoolean(index);
        if (Types.INT.equals(type)) return result.getInt(index);
        if (Types.LONG.equals(type)) return result.getLong(index);
        if (Types.FLOAT.equals(type)) return result.getFloat(index);
        if (Types.DOUBLE.equals(type)) return result.getDouble(index);
        if (Types.BIG_DEC.equals(type)) return result.getBigDecimal(index);
        if (Types.SQL_DATE.equals(type)) return result.getDate(index);
        if (Types.SQL_TIME.equals(type)) return result.getTime(index);
        if (Types.SQL_TIMESTAMP.equals(type)) return result.getTimestamp(index);
        return DPUtil.parseString(result.getObject(index));
    }


    /**
     * 静默关闭JDBC资源
     */
    public static void close(ResultSet result, PreparedStatement statement, Connection connection) {
        try {
            if (null != result) result.close();
        } catch (Exception e) {
            // ignore
        }
        try {
            if (null != statement) statement.close();
        } catch (Exception e) {
            // ignore
        }
        try {
            if (null != connection) connection.close();
        } catch (Exception e) {
            // ignore
        }
    }

}
