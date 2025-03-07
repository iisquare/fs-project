package org.apache.spark.sql.execution.datasources.jdbc;

import com.iisquare.fs.base.core.tool.SQLBuilder;
import com.iisquare.fs.base.core.util.DPUtil;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.errors.QueryExecutionErrors;
import org.apache.spark.sql.jdbc.JdbcDialect;
import org.apache.spark.sql.jdbc.JdbcDialects;
import org.apache.spark.sql.types.StructType;
import scala.Function1;
import scala.Option;
import scala.collection.Iterator;
import scala.runtime.BoxedUnit;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JDBCRollUtils {

    public static void saveTable(Dataset<Row> df, Option<StructType> tableSchema, Boolean isCaseSensitive, JdbcOptionsInWrite options){
        String url = options.url();
        String table = options.table();
        JdbcDialect dialect = JdbcDialects.get(url);
        StructType rddSchema = df.schema();
        int batchSize = options.batchSize();
        int isolationLevel = options.isolationLevel();

        String insertStmt = upsertStatement(table, rddSchema, tableSchema, isCaseSensitive, dialect, options);
        Dataset<Row> repartitionedDF = df;
        Integer n = options.numPartitions().getOrElse(() -> null);
        if (null != n && n <= 0) {
            throw new RuntimeException(QueryExecutionErrors.invalidJdbcNumPartitionsError(n, JDBCOptions.JDBC_NUM_PARTITIONS()));
        } else if (null != n && n < df.rdd().getNumPartitions()) {
            repartitionedDF = df.coalesce(n);
        }
        // $$Lambda$ object not serializable
        repartitionedDF.rdd().foreachPartition((Function1<Iterator<Row>, BoxedUnit> & Serializable) (iterator) -> {
            JdbcUtils.savePartition(table, iterator, rddSchema, insertStmt, batchSize, dialect, isolationLevel, options);
            return BoxedUnit.UNIT;
        });
    }

    public static String upsertStatement(String table, StructType rddSchema, Option<StructType> tableSchema, Boolean isCaseSensitive, JdbcDialect dialect, JdbcOptionsInWrite options) {
        String statement = JdbcUtils.getInsertStatement(table, rddSchema, tableSchema, isCaseSensitive, dialect);
        String[] pks = DPUtil.explode(",", (String) options.parameters().getOrElse("roll", () -> ""));
        if (pks.length == 0) return statement;
        List<String> fields = new ArrayList<>(Arrays.asList(rddSchema.fieldNames()));
        fields.removeAll(Arrays.asList(pks));
        String dialectName = dialect.getClass().getSimpleName();
        if(dialectName.startsWith("MySQLDialect")) {
            statement += SQLBuilder.duplicateUpdate(fields.toArray(new String[0]));
        }
        return statement;
    }

}
