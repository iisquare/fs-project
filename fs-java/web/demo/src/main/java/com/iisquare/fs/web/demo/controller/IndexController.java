package com.iisquare.fs.web.demo.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.web.mvc.ControllerBase;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

@RequestMapping("/")
@RestController
public class IndexController extends ControllerBase implements DisposableBean {

    private final ScheduledExecutorService pool = Executors.newScheduledThreadPool(2);

    @RequestMapping("/")
    public String indexAction(ModelMap model, HttpServletRequest request) {
        return ApiUtil.echoResult(0, "it's work!", null);
    }

    @RequestMapping("/sse")
    public SseEmitter sseAction(@RequestBody String json, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String uuid = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter();
        AtomicBoolean running = new AtomicBoolean(true);
        emitter.onCompletion(() -> {
            running.set(false);
            System.out.printf("[SSE %s] onCompletion\n", uuid);
        });
        emitter.onError(throwable -> {
            // 关闭浏览器或者前端主动Close会触发java.io.IOException: 你的主机中的软件中止了一个已建立的连接。
            running.set(false);
            System.out.printf("[SSE %s] onError\n", uuid);
            throwable.printStackTrace();
        });
        pool.submit(() -> { // 若不直接返回SseEmitter实例，前端请求会一直处于pending状态
            System.out.printf("[SSE %s] Begin %s\n", uuid, json);
            for (int i = 0; i < 10; i++) {
                if (!running.get()) break;
                System.out.printf("[SSE %s] send %d\n", uuid, i);
                ObjectNode message = DPUtil.objectNode();
                message.put("i", i);
                try {
                    emitter.send(SseEmitter.event().data(DPUtil.stringify(message)));
                    Thread.sleep(2000);
                } catch (Exception e) {
                    break;
                }
            }
            emitter.complete();
            System.out.printf("[SSE %s] End\n", uuid);
        });
        return emitter;
    }

    @Override
    public void destroy() throws Exception {
        pool.shutdown();
    }
}
