package com.iisquare.fs.app.spark.batch;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.app.spark.core.SparkRunner;
import com.iisquare.fs.app.spark.core.SparkTypes;
import com.iisquare.fs.app.spark.util.ExcelUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.dag.core.DAGType;
import com.iisquare.fs.base.dag.source.AbstractFileSource;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Excel 批量读取节点。
 *
 * 首版读取成功后由 Driver 创建 Dataset，适用于中小规模 Excel。
 */
public class ExcelSourceNode extends AbstractFileSource<Dataset<Row>> {

    private String location;
    private String dataAddress;
    private String sheetName;
    private int sheetIndex = -1;
    private boolean header = true;
    private int headerRow = -1;
    private int limit;
    private int maxRows = ExcelUtil.DEFAULT_MAX_ROWS;
    private int maxColumns = ExcelUtil.DEFAULT_MAX_COLUMNS;
    private long maxBytes = ExcelUtil.DEFAULT_MAX_BYTES;

    @Override
    public DAGType<Dataset<Row>> outputType() {
        return SparkTypes.DATASET;
    }

    @Override
    public boolean configure(JsonNode... configs) {
        if (!super.configure(configs)) return false;
        location = options.at("/path").asText(options.at("/location").asText(""));
        if (DPUtil.empty(location)) return false;
        dataAddress = options.at("/dataAddress").asText("");
        sheetName = options.at("/sheetName").asText("");
        sheetIndex = options.at("/sheetIndex").asInt(-1);
        header = options.at("/header").asBoolean(true);
        headerRow = options.at("/headerRow").asInt(-1);
        int nodeLimit = options.at("/limit").asInt(0);
        int debugLimit = options.at("/debugLimit").asInt(0);
        limit = effectiveLimit(nodeLimit, debugLimit);
        maxRows = options.at("/maxRows").asInt(ExcelUtil.DEFAULT_MAX_ROWS);
        maxColumns = options.at("/maxColumns").asInt(ExcelUtil.DEFAULT_MAX_COLUMNS);
        maxBytes = options.at("/maxBytes").asLong(ExcelUtil.DEFAULT_MAX_BYTES);
        return true;
    }

    @Override
    public Dataset<Row> process() throws Exception {
        Map<String, String> config = new LinkedHashMap<>();
        config.put("dataAddress", dataAddress);
        config.put("sheetName", sheetName);
        if (sheetIndex >= 0) config.put("sheetIndex", String.valueOf(sheetIndex));
        config.put("header", String.valueOf(header));
        if (headerRow >= 0) config.put("headerRow", String.valueOf(headerRow));
        if (limit > 0) config.put("limit", String.valueOf(limit));
        config.put("maxRows", String.valueOf(maxRows));
        config.put("maxColumns", String.valueOf(maxColumns));
        config.put("maxBytes", String.valueOf(maxBytes));
        return ExcelUtil.read(runner(SparkRunner.class).session(), location, config);
    }

    static int effectiveLimit(int nodeLimit, int debugLimit) {
        int result = 0;
        if (nodeLimit > 0) result = nodeLimit;
        if (debugLimit > 0) result = result > 0 ? Math.min(result, debugLimit) : debugLimit;
        return result;
    }
}
