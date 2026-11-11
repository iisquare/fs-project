package com.iisquare.fs.app.flink.node;

import com.iisquare.fs.base.core.util.DPUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class ConvertTransformNodeTests {

    @Test
    public void mappingsTest() {
        var mappings = ConvertTransformNode.mappings(DPUtil.parseJSON(
                "[{\"target\":\"user_id\",\"source\":\"after.id\",\"clsType\":\"java.lang.Long\"}]"));
        assertEquals(1, mappings.size());
        assertEquals("user_id", mappings.get(0).target);
        assertEquals("after.id", mappings.get(0).source);
        assertEquals("java.lang.Long", mappings.get(0).clsType);
    }

    @Test
    public void modeTest() {
        assertEquals("ONLY_TARGET", ConvertTransformNode.mode("only_target"));
        assertThrows(IllegalArgumentException.class, () -> ConvertTransformNode.mode("DROP_ALL"));
    }
}
