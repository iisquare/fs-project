package org.apache.spark.sql.execution.datasources.jdbc;

import org.apache.spark.*;
import org.apache.spark.rdd.RDD;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.connector.expressions.filter.Predicate;
import org.apache.spark.sql.types.StructType;
import org.apache.spark.util.CompletionIterator;
import scala.Function1;
import scala.collection.Iterator;
import scala.collection.mutable.ArrayBuffer;
import scala.reflect.ClassManifestFactory$;
import scala.reflect.ClassTag;
import scala.runtime.BoxedUnit;

import java.sql.Connection;

/**
 * Refer:
 * - [Implementing custom Spark RDD in Java](https://stackoverflow.com/questions/30446706/implementing-custom-spark-rdd-in-java)
 */
public class JDBCRollRDD extends RDD<Row> {

    private static final ClassTag<Row> ROW_CLASS_TAG = ClassManifestFactory$.MODULE$.<Row>fromClass(Row.class);

    transient SparkContext sc;
    Function1<Object, Connection> getConnection;
    StructType schema;
    String[] columns;
    Predicate[] predicates;
    Partition[] partitions;
    String url;
    JDBCOptions options;

    public JDBCRollRDD(SparkContext sc, Function1<Object, Connection> getConnection, StructType schema, String[] columns, Predicate[] predicates, Partition[] partitions, String url, JDBCOptions options) {
        super(sc, new ArrayBuffer<Dependency<?>>(), ROW_CLASS_TAG);
        this.sc = sc;
        this.getConnection = getConnection;
        this.schema = schema;
        this.columns = columns;
        this.predicates = predicates;
        this.partitions = partitions;
        this.url = url;
        this.options = options;
    }

    @Override
    public Iterator<Row> compute(Partition thePart, TaskContext context) {
        final JDBCRollIterator iterator = new JDBCRollIterator(this, thePart, context);
        context.addTaskCompletionListener(ctx -> {
            iterator.close();
        });
        return CompletionIterator.<Row, Iterator<Row>>apply(new InterruptibleIterator<Row>(context, iterator), () -> {
            iterator.close();
            return BoxedUnit.UNIT;
        });
    }

    @Override
    public Partition[] getPartitions() {
        return partitions;
    }
}
