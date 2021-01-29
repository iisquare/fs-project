package com.iisquare.fs.web.worker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.base.web.util.SpringUtil;
import com.iisquare.fs.web.worker.core.Container;
import com.iisquare.fs.web.worker.core.HandlerBase;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ContainerService extends ServiceBase implements ApplicationContextAware {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private ConfigurableApplicationContext applicationContext;
    private static final Map<String, Container> containers = new ConcurrentHashMap<>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = (ConfigurableApplicationContext) applicationContext;
    }

    public Map<String, Object> submit(String queueName, String handlerName, int prefetchCount, int consumerCount) {
        Container container = containers.get(queueName);
        Map<String, Object> result;
        if (null == container) {
            result = create(queueName, handlerName, prefetchCount, consumerCount);
        } else {
            result = change(queueName, prefetchCount, consumerCount);
        }
        if (DPUtil.parseInt(result.get("code")) != 0) return result;
        return consumerCount > 0 ? start(queueName) : stop(queueName);
    }

    public Map<String, Object> create(String queueName, String handlerName, int prefetchCount, int consumerCount) {
        if (DPUtil.empty(queueName)) return ApiUtil.result(1001, "队列名称不能为空", queueName);
        if (prefetchCount < 0) return ApiUtil.result(1002, "预先载入数量异常", prefetchCount);
        if (consumerCount < 0) return ApiUtil.result(1003, "消费者数量异常", consumerCount);
        Class<?> handlerClass;
        try {
            handlerClass = Class.forName(handlerName);
        } catch (ClassNotFoundException e) {
            return ApiUtil.result(1004, "获取消费实例异常", e.getMessage());
        }
        if (!HandlerBase.class.isAssignableFrom(handlerClass)) {
            return ApiUtil.result(1005, "无效的消费实例", handlerClass.getName());
        }
        Object handlerInstance;
        try {
            handlerInstance = SpringUtil.registerBean(applicationContext, handlerName, handlerClass);
        } catch (Exception e) {
            return ApiUtil.result(1006, "注册消费实例异常", e.getMessage());
        }
        HandlerBase handler = (HandlerBase) handlerInstance;
        Container container = Container.fromConnectionFactory(
                rabbitTemplate.getConnectionFactory(), queueName, handler, prefetchCount, consumerCount);
        if (null != containers.putIfAbsent(queueName, container)) {
            FileUtil.close(container);
            return ApiUtil.result(2001, "队列配置已存在", queueName);
        }
        return ApiUtil.result(0, null, container.state());
    }

    public Map<String, Object> change(String queueName, int prefetchCount, int consumerCount) {
        if (DPUtil.empty(queueName)) return ApiUtil.result(1001, "队列名称不能为空", queueName);
        if (prefetchCount < 0) return ApiUtil.result(1002, "预先载入数量异常", prefetchCount);
        if (consumerCount < 0) return ApiUtil.result(1003, "消费者数量异常", consumerCount);
        Container container = containers.get(queueName);
        if (null == container) return ApiUtil.result(2001, "队列配置不存在", queueName);
        container.change(prefetchCount, consumerCount);
        return ApiUtil.result(0, null, container.state());
    }

    public Map<String, Object> remove(String queueName) {
        Container container = containers.remove(queueName);
        if (null == container) return ApiUtil.result(2001, "队列配置不存在", queueName);
        FileUtil.close(container);
        return ApiUtil.result(0, null, container.state());
    }

    public Map<String, Object> start(String queueName) {
        Container container = containers.get(queueName);
        if (null == container) return ApiUtil.result(2001, "队列配置不存在", queueName);
        container.start();
        return ApiUtil.result(0, null, container.state());
    }

    public Map<String, Object> stop(String queueName) {
        Container container = containers.get(queueName);
        if (null == container) return ApiUtil.result(2001, "队列配置不存在", queueName);
        container.stop();
        return ApiUtil.result(0, null, container.state());
    }

    public JsonNode state(boolean withQueueKey) {
        if (withQueueKey) {
            ObjectNode result = DPUtil.objectNode();
            for (Map.Entry<String, Container> entry : containers.entrySet()) {
                result.replace(entry.getKey(), entry.getValue().state());
            }
            return result;
        } else {
            ArrayNode result = DPUtil.arrayNode();
            for (Map.Entry<String, Container> entry : containers.entrySet()) {
                result.add(entry.getValue().state());
            }
            return result;
        }
    }

}
