package com.iisquare.fs.web.worker;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.HttpUtil;
import com.iisquare.fs.web.worker.rabbit.TestHandler;
import com.iisquare.fs.base.worker.service.TaskService;
import com.iisquare.fs.web.worker.service.TestService;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ContainerTests {

    private String url = "http://127.0.0.1:7813";

    public String url(String uri) {
        return this.url + uri;
    }

    @Test
    public void dateTest() {
        Date date = new Date();
        String format  = "yyyy-MM-dd HH:mm:ss Z";
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        String result = dateFormat.format(date);
        System.out.println(result);
    }

    @Test
    public void replaceTest() {
        String str = "/a/b/c/d";
        System.out.println(str.replaceFirst("/a/b/", ""));
    }

    @Test
    public void createTest() {
        ObjectNode data = DPUtil.objectNode();
        data.put("queueName", TestService.QUEUE_NAME);
        data.put("handlerName", TestHandler.class.getName());
        data.put("prefetchCount", 10);
        data.put("consumerCount", 2);
        String result = HttpUtil.post(url("/container/create"), DPUtil.stringify(data), TaskService.header);
        System.out.println(result);
    }

    @Test
    public void startTest() {
        ObjectNode data = DPUtil.objectNode();
        data.put("queueName", TestService.QUEUE_NAME);
        String result = HttpUtil.post(url("/container/start"), DPUtil.stringify(data), TaskService.header);
        System.out.println(result);
    }

}
