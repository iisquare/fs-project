package com.iisquare.fs.web.worker.service;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CanalService extends ServiceBase {

    @Lazy
    @Autowired
    private RabbitAdmin rabbitAdmin;

    public static final String QUEUE_SYNC_NAME = "fs-canal-sync";
    public static final String EXCHANGE_BINLOG_NAME = "fs-canal-binlog";

    public Map<String, Object> makeCase() {
        Queue queue = new Queue(QUEUE_SYNC_NAME, true, false, false);
        TopicExchange exchange = new TopicExchange(EXCHANGE_BINLOG_NAME, true, false);
        String result = rabbitAdmin.declareQueue(queue);
        if (null == result) return ApiUtil.result(1001, "创建队列失败", queue.getName());
        rabbitAdmin.declareExchange(exchange);
        rabbitAdmin.declareBinding(BindingBuilder.bind(queue).to(exchange).with("#"));
        return ApiUtil.result(0, null, DPUtil.buildMap(
                "queue", queue.getName(),
                "exchange", exchange.getName()
        ));
    }

}
