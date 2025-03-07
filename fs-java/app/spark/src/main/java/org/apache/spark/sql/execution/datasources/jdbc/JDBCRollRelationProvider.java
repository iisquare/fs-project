package org.apache.spark.sql.execution.datasources.jdbc;

import com.iisquare.fs.base.core.util.FileUtil;
import org.apache.spark.Partition;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.errors.QueryCompilationErrors;
import org.apache.spark.sql.jdbc.JdbcDialect;
import org.apache.spark.sql.jdbc.JdbcDialects;
import org.apache.spark.sql.sources.BaseRelation;
import org.apache.spark.sql.types.StructType;
import scala.Function2;
import scala.Option;
import scala.collection.immutable.Map;

import java.sql.Connection;

public class JDBCRollRelationProvider extends JdbcRelationProvider {
    @Override
    public String shortName() {
        return "jdbc-roll";
    }

    @Override
    public BaseRelation createRelation(SQLContext sqlContext, Map<String, String> parameters) {
        JDBCOptions jdbcOptions = new JDBCOptions(parameters);
        Function2<String, String, Object> resolver = sqlContext.conf().resolver();
        String timeZoneId = sqlContext.conf().sessionLocalTimeZone();
        StructType schema = JDBCRelation.getSchema(resolver, jdbcOptions);
        Partition[] parts = JDBCRelation.columnPartition(schema, resolver, timeZoneId, jdbcOptions);
        return new JDBCRollRelation(schema, parts, jdbcOptions, sqlContext.sparkSession());
    }

    @Override
    public BaseRelation createRelation(SQLContext sqlContext, SaveMode mode, Map<String, String> parameters, Dataset<Row> df) {
        JdbcOptionsInWrite options = new JdbcOptionsInWrite(parameters);
        boolean isCaseSensitive = sqlContext.conf().caseSensitiveAnalysis();
        JdbcDialect dialect = JdbcDialects.get(options.url());
        Connection conn = dialect.createConnectionFactory(options).apply(-1);
        try {
            boolean tableExists = JdbcUtils.tableExists(conn, options);
            if (tableExists) {
                switch (mode) {
                    case Overwrite:
                        if (options.isTruncate() && !JdbcUtils.isCascadingTruncateTable(options.url()).getOrElse(() -> true)) {
                            // In this case, we should truncate table and then load.
                            JdbcUtils.truncateTable(conn, options);
                            Option<StructType> tableSchema = JdbcUtils.getSchemaOption(conn, options);
                            JdbcUtils.saveTable(df, tableSchema, isCaseSensitive, options);
                        } else {
                            // Otherwise, do not truncate the table, instead drop and recreate it
                            JdbcUtils.dropTable(conn, options.table(), options);
                            JdbcUtils.createTable(conn, options.table(), df.schema(), isCaseSensitive, options);
                            JdbcUtils.saveTable(df, Option.apply(df.schema()), isCaseSensitive, options);
                        }
                        break;
                    case Append:
                        Option<StructType> tableSchema = JdbcUtils.getSchemaOption(conn, options);
                        JDBCRollUtils.saveTable(df, tableSchema, isCaseSensitive, options);
                        break;
                    case ErrorIfExists:
                        throw new RuntimeException(QueryCompilationErrors.tableOrViewAlreadyExistsError(options.table()));
                    case Ignore:
                        // With `SaveMode.Ignore` mode, if table already exists, the save operation is expected
                        // to not save the contents of the DataFrame and to not change the existing data.
                        // Therefore, it is okay to do nothing here and then just return the relation below.
                        break;
                }
            } else {
                JdbcUtils.createTable(conn, options.table(), df.schema(), isCaseSensitive, options);
                JdbcUtils.saveTable(df, Option.apply(df.schema()), isCaseSensitive, options);
            }
        } finally {
            FileUtil.close(conn);
        }
        return createRelation(sqlContext, parameters);
    }

}
