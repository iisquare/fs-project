package com.iisquare.fs.web.govern.tester;

import com.fasterxml.jackson.databind.JsonNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.govern.entity.Model;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class DataTester {

    @Test
    public void safeTest() {
        List<String> list = Arrays.asList("", "/", "/a", "/a/", "/a/b", "/a/b/", "/a.b/", "/a-b/", "/a_b/");
        for (String item : list) {
            System.out.println(item + ":" + Model.safeCatalog(item));
        }

    }

    @Test
    public void jsonTest() {
        JsonNode json = DPUtil.toJSON(new String[]{"", "a", null});
        System.out.println(DPUtil.stringify(json));
    }

}
