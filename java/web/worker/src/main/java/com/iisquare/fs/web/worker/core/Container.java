package com.iisquare.fs.web.worker.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.DirectMessageListenerContainer;

import java.io.Closeable;
import java.io.IOException;

public class Container implements Closeable {

    private DirectMessageListenerContainer container;
    private String queueName;
    private int prefetchCount;
    private int consumerCount;
    private HandlerBase handler;

    public Container(DirectMessageListenerContainer container,
                     String queueName, HandlerBase handler, int prefetchCount, int consumerCount) {
        container.setAutoStartup(false);
        container.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        container.setQueueNames(this.queueName = queueName);
        container.setPrefetchCount(this.prefetchCount = prefetchCount);
        container.setConsumersPerQueue(this.consumerCount = consumerCount);
        container.setMessageListener(this.handler = handler);
        this.container = container;
    }

    public static Container fromConnectionFactory(
            ConnectionFactory factory, String queueName, HandlerBase handler, int prefetchCount, int consumerCount) {
        DirectMessageListenerContainer container = new DirectMessageListenerContainer(factory);
        return new Container(container, queueName, handler, prefetchCount, consumerCount);
    }

    public JsonNode state() {
        ObjectNode state = DPUtil.objectNode();
        state.put("queueName", queueName);
        state.put("prefetchCount", prefetchCount);
        state.put("consumerCount", consumerCount);
        state.put("handlerName", handler.getClass().getName());
        if (null == container) return state;
        state.put("listenerId", container.getListenerId());
        state.replace("queueNames", array(container.getQueueNames()));
        state.put("isRunning", container.isRunning());
        state.put("isActive", container.isActive());
        state.put("isAutoStartup", container.isAutoStartup());
        state.put("isConsumerBatchEnabled", container.isConsumerBatchEnabled());
        state.put("isChannelTransacted", container.isChannelTransacted());
        state.put("isExposeListenerChannel", container.isExposeListenerChannel());
        state.put("isPossibleAuthenticationFailureFatal", container.isPossibleAuthenticationFailureFatal());
        return state;
    }

    public ArrayNode array(String... items) {
        ArrayNode array = DPUtil.arrayNode();
        for (String item : items) {
            array.add(item);
        }
        return array;
    }

    public void change(int prefetchCount, int consumerCount) {
        if (prefetchCount != this.prefetchCount) {
            container.setPrefetchCount(this.prefetchCount = prefetchCount);
        }
        if (consumerCount != this.consumerCount) {
            container.setConsumersPerQueue(this.consumerCount = consumerCount);
        }
    }

    public void start() {
        container.start();
    }

    public void stop() {
        container.stop();
    }

    @Override
    public void close() throws IOException {
        container.destroy();
        container = null;
    }

}
