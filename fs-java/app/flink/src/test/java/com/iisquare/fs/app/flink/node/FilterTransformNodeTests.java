package com.iisquare.fs.app.flink.node;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FilterTransformNodeTests {

    @Test
    public void stringConditionTest() {
        assertTrue(FilterTransformNode.matches("ACTIVE", "EQ", "active", "string", false));
        assertTrue(FilterTransformNode.matches("order-active", "CONTAINS", "ACTIVE", "string", false));
        assertTrue(FilterTransformNode.matches("a", "IN", "x, a, b", "string", true));
        assertFalse(FilterTransformNode.matches(null, "IS_NOT_NULL", "", "string", true));
    }

    @Test
    public void typedConditionTest() {
        assertTrue(FilterTransformNode.matches(120L, "GT", "100", "number", true));
        assertFalse(FilterTransformNode.matches("100", "GE", "101", "number", true));
        assertTrue(FilterTransformNode.matches(true, "EQ", "true", "boolean", true));
        assertTrue(FilterTransformNode.matches("tenant-01", "REGEX", "^tenant-\\d+$", "string", true));
    }
}
