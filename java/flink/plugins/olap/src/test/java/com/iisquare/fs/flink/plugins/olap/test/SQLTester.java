package com.iisquare.fs.flink.plugins.olap.test;

import com.iisquare.fs.flink.util.FlinkUtil;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.java.BatchTableEnvironment;
import org.apache.flink.types.Row;

import java.util.Arrays;

public class SQLTester {

    public static void main(String[] args) throws Exception {
        ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
        BatchTableEnvironment envTable = BatchTableEnvironment.getTableEnvironment(env);
        DataSet<Row> data = env.fromCollection(Arrays.asList(
            FlinkUtil.row(1, "a1", 1, 12.21),
            FlinkUtil.row(2, "a2", 4, 43.4),
            FlinkUtil.row(3, "a3", 9, 0.4),
            FlinkUtil.row(4, "a4", 7, 33.75),
            FlinkUtil.row(5, "a5", 3, 454.54)
        ));
        envTable.registerDataSet("test", data, "id, name, rank, score");
        System.out.println("-----TopN Data-------");
        data = data.first(3);
        data.print();
        Table table = envTable.sqlQuery("select * from `test` order by `rank` asc");
        table.printSchema();
        data = envTable.toDataSet(table, Row.class);
        System.out.println("-----Query Data-------");
        data.print();
        System.out.println("-----TopN Data-------");
        data = data.first(3); // wrong result
        data.print();
        table = table.orderBy("rank.asc").fetch(3); // null pointer
        table.printSchema();
        data = envTable.toDataSet(table, Row.class);
        System.out.println("-----Query Data-------");
        data.print();
        System.out.println("-----TopN Data-------");
        data = data.first(3);
        data.print();
    }

}
