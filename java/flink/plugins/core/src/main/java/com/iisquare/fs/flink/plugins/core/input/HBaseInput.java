package com.iisquare.fs.flink.plugins.core.input;

import com.iisquare.fs.base.core.util.DPUtil;
import org.apache.flink.addons.hbase.AbstractTableInputFormat;
import org.apache.flink.configuration.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.util.*;

public class HBaseInput extends AbstractTableInputFormat<Map<String, Object>> {

    private String quorum;
    private String znode;
    private String zkRetry;
    private String clientRetry;
    private String tableName;
    private List<List<String>> fields;
    private String startRow;
    private String endRow;

    public HBaseInput(String quorum, String znode, String zkRetry, String clientRetry, String tableName, String fields, String startRow, String endRow) {
        this.quorum = quorum;
        this.znode = znode;
        this.zkRetry = zkRetry;
        this.clientRetry = clientRetry;
        this.tableName = tableName;
        List<List<String>> list = new ArrayList<>();
        String[] familyArray = DPUtil.explode(fields, ",");
        for (String familyItem : familyArray) {
            String[] qualifierArray = DPUtil.explode(familyItem, ":");
            if(qualifierArray.length > 1) {
                list.add(Arrays.asList(qualifierArray[0], qualifierArray[1]));
            } else {
                list.add(Arrays.asList(qualifierArray[0], ""));
            }
        }
        this.fields = list;
        this.startRow = startRow;
        this.endRow = endRow;
    }

    @Override
    protected Scan getScanner() {
        Scan scan = new Scan();
        if (!startRow.isEmpty()) {
            scan.withStartRow(Bytes.toBytes(startRow));
        }
        if (!endRow.isEmpty()) {
            scan.withStopRow(Bytes.toBytes(endRow));
        }
        return scan;
    }

    @Override
    protected String getTableName() {
        return tableName;
    }

    @Override
    protected Map<String, Object> mapResultToOutType(Result result) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("rowkey", new String(result.getRow()));
        for (List<String> item : fields) {
            record.put(item.get(0) + ":" + item.get(1), new String(
                result.getValue(Bytes.toBytes(item.get(0)), Bytes.toBytes(item.get(1)))
            ));
        }
        return record;
    }

    @Override
    public void configure(Configuration parameters) {
        org.apache.hadoop.conf.Configuration config = HBaseConfiguration.create();
        config.set("hbase.zookeeper.quorum", quorum);
        config.set("zookeeper.znode.parent", znode);
        config.set("zookeeper.recovery.retry", zkRetry);
        config.set("hbase.client.retries.number", clientRetry);
        try {
            table = new HTable(config, getTableName());
            scan = getScanner();
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
    }

}
