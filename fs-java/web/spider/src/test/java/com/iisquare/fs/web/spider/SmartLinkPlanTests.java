package com.iisquare.fs.web.spider;

import com.iisquare.fs.web.spider.core.Worker;
import org.junit.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SmartLinkPlanTests {

    @Test
    public void newLinkTest() {
        Set<String> newLinks = new LinkedHashSet<>();
        newLinks.add("https://example.com/new");
        Set<String> oldListLinks = new LinkedHashSet<>();
        oldListLinks.add("https://example.com/list");

        Worker.SmartLinkPlan plan = new Worker.SmartLinkPlan(newLinks, oldListLinks);

        assertEquals(2, plan.queueLinks().size());
        assertTrue(plan.queueLinks().contains("https://example.com/new"));
        assertTrue(plan.queueLinks().contains("https://example.com/list"));
    }

    @Test
    public void noNewLinkTest() {
        Set<String> oldListLinks = new LinkedHashSet<>();
        oldListLinks.add("https://example.com/list");

        Worker.SmartLinkPlan plan = new Worker.SmartLinkPlan(new LinkedHashSet<>(), oldListLinks);

        assertTrue(plan.queueLinks().isEmpty());
    }
}
