package com.iisquare.fs.app.flink.stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.app.flink.util.FlinkUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class WindowTransformNodeTests {

    @Test
    public void batchJsonTest() {
        String result = WindowTransformNode.batch("tenant-a", new TimeWindow(1000, 2000),
                Arrays.asList(DPUtil.objectNode().put("id", 1), FlinkUtil.row(2)),
                new RowTypeInfo(new TypeInformation[]{Types.INT}, new String[]{"id"}), "processing");
        JsonNode json = DPUtil.parseJSON(result);
        assertEquals("tenant-a", json.at("/windowKey").asText());
        assertEquals(2, json.at("/count").asInt());
        assertEquals(2, json.at("/items").size());
        assertEquals(1, json.at("/items/0/id").asInt());
    }
}
