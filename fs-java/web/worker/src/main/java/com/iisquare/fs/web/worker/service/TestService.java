package com.iisquare.fs.web.worker.service;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.worker.core.Task;
import com.iisquare.fs.web.worker.rabbit.TestHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class TestService extends ServiceBase {

    @Lazy
    @Autowired
    RabbitAdmin rabbitAdmin;
    @Autowired
    RabbitTemplate rabbitTemplate;
    @Autowired
    TaskService taskService;

    public static final String QUEUE_NAME = "fs-test-queue";
    public static final String EXCHANGE_NAME = "fs-test-exchange";

    public Map<String, Object> makeCase() {
        Queue queue = new Queue(QUEUE_NAME, true, false, false);
        FanoutExchange exchange = new FanoutExchange(EXCHANGE_NAME, true, false);
        String result = rabbitAdmin.declareQueue(queue);
        if (null == result) return ApiUtil.result(1001, "创建队列失败", queue.getName());
        rabbitAdmin.declareExchange(exchange);
        rabbitAdmin.declareBinding(BindingBuilder.bind(queue).to(exchange));
        return ApiUtil.result(0, null, DPUtil.buildMap(
                "queue", queue.getName(),
                "exchange", exchange.getName()
        ));
    }

    public void sendMessage(String message) {
        rabbitTemplate.convertAndSend(EXCHANGE_NAME, QUEUE_NAME, message);
    }

    public Map<String, Object> submit(HttpServletRequest request) {
        String queueName = TestService.QUEUE_NAME;
        String handlerName = TestHandler.class.getName();
        int prefetchCount = 10;
        int consumerCount = 2;
        Task task = Task.create(queueName, handlerName, prefetchCount, consumerCount);
        task = taskService.save(task, true, true);
        return ApiUtil.result(null == task ? 500: 0, null, task);
    }

}
