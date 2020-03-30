package com.iisquare.fs.flink.plugins.core.source;

import com.iisquare.fs.base.core.util.DPUtil;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.util.*;

public class HBaseSource extends RichSourceFunction<Map<String, Object>> {

    private String quorum;
    private String znode;
    private String zkRetry;
    private String clientRetry;
    private String tableName;
    private String fields;
    private String startRow;
    private String endRow;
    private Map<String, Table> tables = new HashMap<>();
    private Connection connection = null;

    public HBaseSource(String quorum, String znode, String zkRetry, String clientRetry, String tableName, String fields, String startRow, String endRow) {
        this.quorum = quorum;
        this.znode = znode;
        this.zkRetry = zkRetry;
        this.clientRetry = clientRetry;
        this.tableName = tableName;
        this.fields = fields;
        this.startRow = startRow;
        this.endRow = endRow;
    }

    @Override
    public void run(SourceContext ctx) throws Exception {
        Table table = table(tableName);
        if(null == table) return;
        List<List<String>> list = new ArrayList<>();
        String[] familyArray = DPUtil.explode(fields, ",", " ", true);
        for (String familyItem : familyArray) {
            String[] qualifierArray = DPUtil.explode(familyItem, ":", " ", true);
            if(qualifierArray.length > 1) {
                list.add(Arrays.asList(qualifierArray[0], qualifierArray[1]));
            } else {
                list.add(Arrays.asList(qualifierArray[0], ""));
            }
        }
        Scan scan = new Scan();
        if (!startRow.isEmpty()) {
            scan.withStartRow(Bytes.toBytes(startRow));
        }
        if (!endRow.isEmpty()) {
            scan.withStopRow(Bytes.toBytes(endRow));
        }
        ResultScanner results = table.getScanner(scan);
        for (Result result : results) {
            Map<String, Object> record = new LinkedHashMap<>();
            record.put("rowkey", new String(result.getRow()));
            for (List<String> item : list) {
                record.put(item.get(0) + ":" + item.get(1), new String(
                    result.getValue(Bytes.toBytes(item.get(0)), Bytes.toBytes(item.get(1)))
                ));
            }
            ctx.collect(record);
        }
    }

    @Override
    public void cancel() {

    }

    public Table table(String name) throws IOException {
        if(tables.containsKey(name)) return tables.get(name);
        Admin admin = connection.getAdmin();
        TableName tableName = TableName.valueOf(name);
        if(!admin.tableExists(tableName)) return null;
        Table table = connection.getTable(tableName);
        tables.put(name, table);
        return table;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        org.apache.hadoop.conf.Configuration config = HBaseConfiguration.create();
        config.set("hbase.zookeeper.quorum", quorum);
        config.set("zookeeper.znode.parent", znode);
        config.set("zookeeper.recovery.retry", zkRetry);
        config.set("hbase.client.retries.number", clientRetry);
        connection = ConnectionFactory.createConnection(config);
    }

    @Override
    public void close() throws Exception {
        super.close();
        for (Map.Entry<String, Table> entry : tables.entrySet()) {
            entry.getValue().close();
        }
        connection.close();
    }
}
