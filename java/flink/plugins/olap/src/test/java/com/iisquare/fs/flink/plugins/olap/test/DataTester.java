package com.iisquare.fs.flink.plugins.olap.test;

import com.iisquare.fs.base.core.util.DPUtil;
import org.junit.Test;

public class DataTester {

    @Test
    public void regexTest() {
        System.out.println("house asc, ddd desc".replaceAll("(?i)\\sasc", ".ASC"));
    }

    @Test
    public void utilTest() {
        System.out.println(DPUtil.parseLong("5147484921"));
    }

}
