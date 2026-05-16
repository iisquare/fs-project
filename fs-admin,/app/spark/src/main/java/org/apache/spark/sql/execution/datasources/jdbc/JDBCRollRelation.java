package org.apache.spark.sql.execution.datasources.jdbc;

import org.apache.spark.Partition;
import org.apache.spark.SparkContext;
import org.apache.spark.rdd.RDD;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.connector.expressions.filter.Predicate;
import org.apache.spark.sql.jdbc.JdbcDialect;
import org.apache.spark.sql.jdbc.JdbcDialects;
import org.apache.spark.sql.sources.Filter;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class JDBCRollRelation extends JDBCRelation {

    StructType schema;
    Partition[] parts;
    JDBCOptions jdbcOptions;
    SparkSession sparkSession;

    public JDBCRollRelation(StructType schema, Partition[] parts, JDBCOptions jdbcOptions, SparkSession sparkSession) {
        super(schema, parts, jdbcOptions, sparkSession);
        this.schema = schema;
        this.parts = parts;
        this.jdbcOptions = jdbcOptions;
        this.sparkSession = sparkSession;
    }

    @Override
    public RDD<Row> buildScan(String[] requiredColumns, Filter[] filters) {
        List<Predicate> predicates;
        if (jdbcOptions.pushDownPredicate()) {
            predicates = Arrays.stream(filters).map(Filter::toV2).collect(Collectors.toList());
        } else {
            predicates = new ArrayList<>();
        }
        return scanTable(
                sparkSession.sparkContext(),
                schema,
                requiredColumns,
                predicates.toArray(new Predicate[0]),
                parts,
                jdbcOptions);
    }

    public static RDD<Row> scanTable(
            SparkContext sc, StructType schema, String[] requiredColumns, Predicate[] predicates, Partition[] parts, JDBCOptions options) {
        String url = options.url();
        JdbcDialect dialect = JdbcDialects.get(url);
        String[] quotedColumns = Arrays.stream(requiredColumns).map(dialect::quoteIdentifier).toArray(String[]::new);
        return new JDBCRollRDD(
                sc,
                dialect.createConnectionFactory(options),
                pruneSchema(schema, requiredColumns),
                quotedColumns,
                predicates,
                parts,
                url,
                options
        );
    }

    public static StructType pruneSchema(StructType schema, String[] columns) {
        StructField[] fields = schema.fields();
        return new StructType(Arrays.stream(columns).map(name -> fields[schema.fieldIndex(name)]).toArray(StructField[]::new));
    }

    @Override
    public boolean needConversion() {
        return true;
    }
}
