package com.iisquare.fs.flink.plugins.core.output;

import com.iisquare.fs.base.core.util.DPUtil;
import lombok.Getter;
import lombok.Setter;
import org.apache.flink.api.common.io.OutputFormat;
import org.apache.flink.configuration.Configuration;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.util.*;

@Getter
@Setter
public class HBaseOutput implements OutputFormat<Map<String, Object>> {

    private String quorum;
    private String znode;
    private String zkRetry;
    private String clientRetry;
    private String tableName;
    private String idField;
    private String tableField;
    private String[] familyNames; // 字段名称列:列族
    private org.apache.hadoop.conf.Configuration config = null;
    private Map<String, Table> tables = new HashMap<>();
    private Connection connection = null;

    public HBaseOutput(String quorum, String znode, String zkRetry, String clientRetry, String tableName, String idField, String tableField, String[] familyNames) {
        this.quorum = quorum;
        this.znode = znode;
        this.zkRetry = zkRetry;
        this.clientRetry = clientRetry;
        this.tableName = tableName;
        this.idField = idField;
        this.tableField = tableField;
        this.familyNames = familyNames;
    }

    @Override
    public void configure(Configuration parameters) {
        config = HBaseConfiguration.create();
        config.set("hbase.zookeeper.quorum", quorum);
        config.set("zookeeper.znode.parent", znode);
        config.set("zookeeper.recovery.retry", zkRetry);
        config.set("hbase.client.retries.number", clientRetry);
    }

    @Override
    public void open(int taskNumber, int numTasks) throws IOException {
        connection = ConnectionFactory.createConnection(config);
    }

    public Table table(String name) throws IOException {
        if(tables.containsKey(name)) return tables.get(name);
        Admin admin = connection.getAdmin();
        TableName tableName = TableName.valueOf(name);
        if(!admin.tableExists(tableName)) {
            HTableDescriptor des = new HTableDescriptor(tableName);
            Set<String> fields = new LinkedHashSet<>();
            for (String familyName : familyNames) {
                fields.add(familyName.split(":")[0]);
            }
            for (String field : fields) {
                des.addFamily(new HColumnDescriptor(field));
            }
            try {
                admin.createTable(des);
            } catch (TableExistsException e) {}
        }
        Table table = connection.getTable(tableName);
        tables.put(name, table);
        return table;
    }

    @Override
    public void writeRecord(Map<String, Object> record) throws IOException {
        String table = tableName;
        if(record.containsKey(tableField)) {
            table = record.get(tableField).toString().replaceAll("\\{table\\}", table);
            record.remove(tableField);
        }
        String id;
        if(record.containsKey(idField)) {
            id = record.get(idField).toString();
            record.remove(idField);
        } else {
            id = UUID.randomUUID().toString().toLowerCase();
        }
        byte[] rowkey = Bytes.toBytes(id);
        Put put = new Put(rowkey);
        Increment incr = new Increment(rowkey);
        boolean isPut = false, isIncr = false;
        String[] familyNames = this.familyNames.length > 0 ? this.familyNames : record.keySet().toArray(new String[record.size()]);
        for (String key : familyNames) {
            Object value = record.get(key);
            if (key.equals("_ttl")) {
                long ttl = DPUtil.parseLong(value);
                if (ttl > 0) {
                    put.setTTL(ttl);
                    incr.setTTL(ttl);
                }
            } else {
                byte[] bytesValue;
                if (value instanceof Integer) {
                    bytesValue = Bytes.toBytes((int) value);
                } else if (value instanceof Long) {
                    bytesValue = Bytes.toBytes((long) value);
                } else if (value instanceof Float) {
                    bytesValue = Bytes.toBytes((float) value);
                } else if (value instanceof Double) {
                    bytesValue = Bytes.toBytes((double) value);
                } else {
                    bytesValue = Bytes.toBytes(DPUtil.parseString(value));
                }
                if (DPUtil.empty(value)) continue;
                String[] fieldNames = key.split(":");
                byte[] family = Bytes.toBytes(fieldNames[0]);
                if (fieldNames.length > 1) {
                    String[] qualifierNames = fieldNames[1].split("@");
                    byte[] qualifier = Bytes.toBytes(qualifierNames[0]);
                    if (qualifierNames.length > 1 && qualifierNames[1].equals("incr")) {
                        isIncr = true;
                        incr.addColumn(family, qualifier, DPUtil.parseLong(value));
                    } else {
                        isPut = true;
                        put.addColumn(family, qualifier, bytesValue);
                    }
                } else {
                    isPut = true;
                    put.addColumn(family, null, bytesValue);
                }
            }
        }
        if (isPut) {
            table(table).put(put);
        }
        if (isIncr) {
            table(table).increment(incr);
        }
    }

    @Override
    public void close() throws IOException {
        for (Map.Entry<String, Table> entry : tables.entrySet()) {
            entry.getValue().close();
        }
        connection.close();
    }
}
