package org.apache.spark.sql.execution.datasources.jdbc;

import org.apache.spark.Partition;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.sources.BaseRelation;
import org.apache.spark.sql.types.StructType;
import scala.Function2;
import scala.collection.immutable.Map;

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
}
