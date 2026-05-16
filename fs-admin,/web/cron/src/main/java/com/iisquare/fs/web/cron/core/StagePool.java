package com.iisquare.fs.web.cron.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

public class StagePool implements Closeable {

    private volatile boolean isRunning = true;
    private long takeAwaitTime = 30000; // 阻塞获取任务最大等待时间
    private long takeSleepInterval = 300; // 拉取任务失败后的循环等待间隔

    private final List<StageWorker> workers = new Vector<>();
    private final SynchronousQueue<Stage> queue = new SynchronousQueue<>();

    @Override
    public void close() throws IOException {
        this.isRunning = false;
        while (workers.size() > 0) {
            System.out.println("still " + workers.size() + " workers are running, please wait...");
            clear();
            try {
                Thread.sleep(takeSleepInterval);
            } catch (InterruptedException e) {}
        }
    }

    public Stage take() {
        long await = takeAwaitTime;
        long interval = takeSleepInterval;
        for (long time = interval; time <= await && isRunning; time += interval) {
            Stage stage;
            try { // 多次循环读，避免阻塞时间过长，导致关闭过程缓慢
                stage = queue.poll(interval, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                continue;
            }
            return stage;
        }
        return null;
    }

    public boolean clear(StageWorker worker) {
        return workers.remove(worker);
    }

    public void clear() {
        for (StageWorker worker : workers) {
            if (!worker.isAlive()) clear(worker);
        }
    }

    public int interrupt(int logId) {
        int total = 0;
        for (StageWorker worker : workers) {
            Stage stage = worker.stage();
            if (null == stage) continue;
            if (logId != stage.stage.at("/logId").asInt(0)) continue;
            worker.interrupt(); // 仅发送中断信号，不保证线程一定会退出
            total++;
            clear(worker);
        }
        return total;
    }

    public ArrayNode stages() {
        ArrayNode result = DPUtil.arrayNode();
        for (StageWorker worker : workers) {
            Stage stage = worker.stage();
            if (null == stage) continue;
            result.add(stage.stage);
        }
        return result;
    }

    public Map<String, Object> submit(JsonNode stage, ObjectNode config) {
        if (!isRunning) return ApiUtil.result(3101, "任务池已终止", stage);
        Stage instance = Stage.getInstance(stage, config);
        if (null == instance) return ApiUtil.result(3501, "获取节点实例失败", stage);
        if (queue.offer(instance)) return ApiUtil.result(0, null, stage);
        StageWorker worker = new StageWorker(this);
        workers.add(worker);
        worker.call(instance);
        return ApiUtil.result(0, null, stage);
    }
}
