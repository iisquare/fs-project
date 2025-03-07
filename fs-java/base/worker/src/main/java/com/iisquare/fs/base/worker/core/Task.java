package com.iisquare.fs.base.worker.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.iisquare.fs.base.core.util.DPUtil;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Task {

    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_STOP = "STOP";
    public static final String STATUS_STANDBY = "STANDBY";

    private String queueName; // 队列名称
    private int prefetchCount; // 预加载数量
    private int consumerCount; // 消费者数据量
    private String handlerName; // 处理类名称

    private long version; // 事件版本
    private String status; // 任务状态

    public static String encode(Task item) {
        return DPUtil.stringify(DPUtil.toJSON(item));
    }

    public static Task decode(String item) {
        return DPUtil.toJSON(DPUtil.parseJSON(item), Task.class);
    }

    public static Task create(String queueName, String handlerName, int prefetchCount, int consumerCount) {
        Task task = new Task();
        task.queueName = queueName;
        task.handlerName = handlerName;
        task.prefetchCount = prefetchCount;
        task.consumerCount = consumerCount;
        task.version = System.currentTimeMillis();
        task.status = STATUS_STANDBY;
        return task;
    }

    @JsonIgnore
    public boolean isRunning() {
        return STATUS_RUNNING.endsWith(status);
    }

    public Task start() {
        this.version = System.currentTimeMillis();
        this.status = STATUS_RUNNING;
        return this;
    }

    public Task stop() {
        this.version = System.currentTimeMillis();
        this.status = STATUS_STOP;
        return this;
    }

}
