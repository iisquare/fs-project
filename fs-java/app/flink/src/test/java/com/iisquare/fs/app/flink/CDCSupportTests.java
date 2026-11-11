package com.iisquare.fs.app.flink;

import com.iisquare.fs.app.flink.stream.MongoCaptureNode;
import com.iisquare.fs.app.flink.stream.MySQLCaptureNode;
import com.iisquare.fs.app.flink.util.CDCSupport;
import com.iisquare.fs.base.core.util.DPUtil;
import org.apache.flink.cdc.connectors.base.options.StartupMode;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CDCSupportTests {

    @Test
    public void patternsTest() {
        assertArrayEquals(new String[]{"db1", "db2", "db3"}, CDCSupport.patterns(
                DPUtil.parseJSON("{\"databaseList\":\"db1;db2\\ndb3\"}"), "databaseList", ".*"));
        assertArrayEquals(new String[]{".*"}, CDCSupport.patterns(
                DPUtil.parseJSON("{}"), "databaseList", ".*"));
    }

    @Test
    public void propertiesTest() {
        Properties properties = CDCSupport.properties(
                DPUtil.parseJSON("{\"debeziumProperties\":{\"decimal.handling.mode\":\"string\"}}"),
                "debeziumProperties");
        assertEquals("string", properties.getProperty("decimal.handling.mode"));
    }

    @Test
    public void startupTest() {
        assertEquals(StartupMode.LATEST_OFFSET, MongoCaptureNode.startup("latest").startupMode);
        assertEquals(StartupMode.INITIAL, MongoCaptureNode.startup("initial").startupMode);
        assertTrue(MySQLCaptureNode.startup("latest").isStreamOnly());
        assertFalse(MySQLCaptureNode.startup("initial").isStreamOnly());
        assertEquals(org.apache.flink.cdc.connectors.mysql.table.StartupMode.INITIAL,
                MySQLCaptureNode.startup("initial").startupMode);
    }
}
