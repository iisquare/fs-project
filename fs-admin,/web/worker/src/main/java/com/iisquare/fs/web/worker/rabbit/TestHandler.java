package com.iisquare.fs.web.worker.rabbit;

import com.iisquare.fs.base.worker.core.HandlerBase;
import com.iisquare.fs.web.worker.mvc.Configuration;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;

public class TestHandler extends HandlerBase {

    @Autowired
    private Configuration configuration;

    @Override
    public void onMessage(Message message, Channel channel) throws Exception {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        int retryCount = (int) message.getMessageProperties().getHeaders().getOrDefault("retryCount", 0);
        message.getMessageProperties().setHeader("retryCount", ++retryCount);
        String content = new String(message.getBody());
        long threadId = Thread.currentThread().getId();
        System.out.println(threadId + " - " + deliveryTag + " - " + retryCount + " - " + content);
        if (System.currentTimeMillis() % 2 == 0) { // 随机确认
            System.out.println("requeue - " + configuration.getFormatDate());
            channel.basicAck(deliveryTag, false);
        } else {
            channel.basicNack(deliveryTag, false, true);
        }
    }
}
