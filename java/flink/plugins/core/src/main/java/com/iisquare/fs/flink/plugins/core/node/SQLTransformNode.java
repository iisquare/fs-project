package com.iisquare.fs.flink.plugins.core.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.flink.flow.BatchNode;
import com.iisquare.fs.flink.flow.Node;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.flink.flow.StreamNode;
import com.iisquare.fs.flink.util.FlinkUtil;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.TableEnvironment;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.api.java.BatchTableEnvironment;
import org.apache.flink.table.api.java.StreamTableEnvironment;
import org.apache.flink.types.Row;

import java.util.Map;

public class SQLTransformNode extends Node {
    @Override
    public JsonNode run() throws Exception {
        return DPUtil.parseJSON(config);
    }

    public static MapFunction map2row() {
        return new MapFunction<Map<String, Object>, Row>() {
            @Override
            public Row map(Map<String, Object> record) throws Exception {
                return FlinkUtil.row(record);
            }
        };
    }

    public static MapFunction row2map(RowTypeInfo typeInfo) {
        return new MapFunction<Row, Map<String, Object>>() {
            @Override
            public Map<String, Object> map(Row record) throws Exception {
                return FlinkUtil.map(typeInfo, record);
            }
        };
    }

    public static RowTypeInfo info(Table table) {
        TableSchema schema = table.getSchema();
        return new RowTypeInfo(schema.getTypes(), schema.getColumnNames());
    }

    @Override
    protected StreamNode stream() {
        return new StreamNode() {
            @Override
            public DataStream<Map<String, Object>> run(JsonNode config) throws Exception {
                StreamTableEnvironment envTable = TableEnvironment.getTableEnvironment(environment());
                for (Node item : source) {
                    DataStream<Row> result = item.getStream().result().map(map2row()).returns(item.getTypeInfo());
                    JsonNode itemConfig = DPUtil.parseJSON(item.getConfig());
                    String alias = itemConfig.get("alias").asText();
                    if(DPUtil.empty(alias)) alias = itemConfig.get("id").asText();
                    envTable.registerDataStream(alias, result);
                }
                Table table = envTable.sqlQuery(config.get("sql").asText());
                TableSchema schema = table.getSchema();
                typeInfo = new RowTypeInfo(schema.getTypes(), schema.getColumnNames());
                return envTable.toAppendStream(table, Row.class).map(row2map(typeInfo));
            }
        };
    }

    @Override
    protected BatchNode batch() {
        return new BatchNode() {
            @Override
            public DataSet<Map<String, Object>> run(JsonNode config) throws Exception {
                BatchTableEnvironment envTable = TableEnvironment.getTableEnvironment(environment());
                for (Node item : source) {
                    DataSet<Row> result = item.getBatch().result().map(map2row()).returns(item.getTypeInfo());
                    JsonNode itemConfig = DPUtil.parseJSON(item.getConfig());
                    String alias = itemConfig.get("alias").asText();
                    if(DPUtil.empty(alias)) alias = itemConfig.get("id").asText();
                    envTable.registerDataSet(alias, result);
                }
                Table table = envTable.sqlQuery(config.get("sql").asText());
                TableSchema schema = table.getSchema();
                typeInfo = new RowTypeInfo(schema.getTypes(), schema.getColumnNames());
                return envTable.toDataSet(table, Row.class).map(row2map(typeInfo));
            }
        };
    }
}
