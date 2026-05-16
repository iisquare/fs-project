package com.iisquare.fs.web.spider;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.spider.core.RedisTask;
import org.junit.Test;

public class ScheduleTests {

    @Test
    public void urlTest() {
        String url = "https://www.iisquare.com/{column}-{id}.html";
        ObjectNode args = DPUtil.objectNode();
        args.put("column", "dataset");
        args.put("id", "123");
        System.out.println(RedisTask.url(url, args));
    }

}
