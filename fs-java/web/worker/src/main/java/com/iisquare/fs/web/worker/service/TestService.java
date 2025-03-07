package com.iisquare.fs.web.worker.service;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
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
    private RabbitAdmin rabbitAdmin;
    @Autowired
    private RabbitTemplate rabbitTemplate;

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

}
