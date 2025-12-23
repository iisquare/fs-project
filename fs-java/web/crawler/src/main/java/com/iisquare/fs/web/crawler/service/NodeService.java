package com.iisquare.fs.web.crawler.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.crawler.core.*;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class NodeService extends ServiceBase implements Runnable, DisposableBean, InitializingBean {

    @Value("${fs.crawler.zookeeper.host}")
    private String zhHost;
    @Value("${fs.crawler.zookeeper.timeout}")
    private int zhTimeout;
    @Value("${fs.crawler.zookeeper.nodeId}")
    private String zhNodeId;
    @Value("${fs.crawler.fetcher.minIdle}")
    private Integer fetcherMinIdle;
    @Value("${fs.crawler.fetcher.maxIdle}")
    private Integer fetcherMaxIdle;
    @Value("${fs.crawler.fetcher.maxTotal}")
    private Integer fetcherMaxTotal;
    @Value("${fs.crawler.worker.minIdle}")
    private Integer workerMinIdle;
    @Value("${fs.crawler.worker.maxIdle}")
    private Integer workerMaxIdle;
    @Value("${fs.crawler.worker.maxTotal}")
    private Integer workerMaxTotal;
    @Autowired
    MinioClient minioClient;


    private ZooKeeperClient zookeeper;
    private volatile boolean isRunning = false;
    private ObjectPool<Worker> workerPool;
    private ObjectPool<HttpFetcher> fetcherPool;
    private final AtomicLong pollingCounter = new AtomicLong(0); // 用于服务端轮询计数
    private final AtomicInteger runningCounter = new AtomicInteger(0); // 当前正在运行的调度线程数量
    // 使用空容量同步队列复用线程，非阻塞写，半阻塞读
    private final SynchronousQueue<ObjectNode> taskSynchronousQueue = new SynchronousQueue<>();
    private final Object idleLock = new Object(); // 用于空闲等待，便于在进程退出时及时唤醒

    @Override
    public void afterPropertiesSet() throws Exception {
        zookeeper = new ZooKeeperClient(zhHost, zhTimeout, zhNodeId);
        zookeeper.listen(new WatchListener(this));
        zookeeper.open();
        GenericObjectPoolConfig<Worker> workerPoolConfig = new GenericObjectPoolConfig<>();
        workerPoolConfig.setMaxIdle(workerMinIdle);
        workerPoolConfig.setMaxIdle(workerMaxIdle);
        workerPoolConfig.setMaxTotal(workerMaxTotal);
        this.workerPool = new GenericObjectPool<>(new WorkerFactory(), workerPoolConfig);
        GenericObjectPoolConfig<HttpFetcher> fetcherPoolConfig = new GenericObjectPoolConfig<>();
        fetcherPoolConfig.setMaxIdle(fetcherMinIdle);
        fetcherPoolConfig.setMaxIdle(fetcherMaxIdle);
        fetcherPoolConfig.setMaxTotal(fetcherMaxTotal);
        this.fetcherPool= new GenericObjectPool<>(new HttpFetcherFactory(), fetcherPoolConfig);
        this.start();
    }

    @Override
    public void destroy() throws Exception {
        this.stop();
        while (workerPool.getNumActive() > 0 || runningCounter.get() > 0) {
            log.info("still {} workers and {} schedule are running, please wait...", workerPool.getNumActive(), runningCounter.get());
            try {
                if (workerPool.getNumActive() > 0 || runningCounter.get() > 0) {
                    Thread.sleep(3000); // 等待空闲线程释放，一般不会被打断
                }
            } catch (InterruptedException ignored) {}
        }
        FileUtil.close(workerPool, fetcherPool, zookeeper);
        zookeeper = null;
        workerPool = null;
        fetcherPool = null;
    }

    public MinioClient minio() {
        return this.minioClient;
    }

    public ZooKeeperClient zookeeper() {
        return this.zookeeper;
    }

    public String nodeId() {
        return this.zhNodeId;
    }

    public ObjectPool<Worker> workerPool() {
        return this.workerPool;
    }

    public ObjectPool<HttpFetcher> fetcherPool() {
        return this.fetcherPool;
    }

    public ObjectNode state() throws Exception {
        ObjectNode state = DPUtil.objectNode();
        state.put("nodeId", zookeeper.nodeId());
        state.put("role", "crawler");
        state.put("isRunning", isRunning);
        state.put("pollingCounter", pollingCounter.get());
        state.put("runningCounter", runningCounter.get());
        ObjectNode fetcher = state.putObject("fetcher");
        fetcher.put("minIdle", fetcherMinIdle);
        fetcher.put("maxIdle", fetcherMaxIdle);
        fetcher.put("maxTotal", fetcherMaxTotal);
        fetcher.put("numIdle", fetcherPool.getNumIdle());
        fetcher.put("numActive", fetcherPool.getNumActive());
        ObjectNode worker = state.putObject("worker");
        worker.put("minIdle", workerMinIdle);
        worker.put("maxIdle", workerMaxIdle);
        worker.put("maxTotal", workerMaxTotal);
        worker.put("numIdle", workerPool.getNumIdle());
        worker.put("numActive", workerPool.getNumActive());
        return state;
    }

    public void start() {
        if (this.isRunning) return;
        this.isRunning = true;
        new Thread(this).start();
    }

    public void stop() {
        this.isRunning = false;
        synchronized (idleLock) {
            idleLock.notifyAll(); // 唤醒空闲等待线程
        }
    }

    private boolean obtain(HttpFetcher fetcher) {
        List<ZooNode> spiders = zookeeper.spiders().values().stream().toList();
        if (spiders.isEmpty()) { // 无可用服务端
            synchronized (idleLock) {
                try {
                    idleLock.wait(30000);
                } catch (InterruptedException ignored) {}
            }
            return false;
        }
        ZooNode node = spiders.get((int) (pollingCounter.incrementAndGet() % spiders.size()));
        String url = String.format("http://%s:%d/node/obtain", node.getHost(), node.getPort());
        ObjectNode body = DPUtil.objectNode();
        body.put("nodeId", zhNodeId);
        fetcher.url(url).post(body);
        log.info("obtain task: {}", fetcher.getLastResult());
        return true;
    }

    public boolean report(HttpFetcher fetcher, Map<String, Object> result) {
        if (ApiUtil.failed(result)) {
            log.info("report task: {}", DPUtil.stringify(result));
        }
        List<ZooNode> spiders = zookeeper.spiders().values().stream().toList();
        if (spiders.isEmpty()) { // 无可用服务端
            log.info("spiders is empty, retry after {} milliseconds", 30000);
            try {
                Thread.sleep(30000);
            } catch (InterruptedException ignored) {}
            return report(fetcher, result);
        }
        ZooNode node = spiders.get((int) (pollingCounter.incrementAndGet() % spiders.size()));
        String url = String.format("http://%s:%d/node/report", node.getHost(), node.getPort());
        fetcher.url(url).post(DPUtil.toJSON(result));
        return true;
    }

    public ObjectNode checkSynchronousQueue() {
        long interval = 30;
        long await = 100;
        for (long time = interval; time <= await && isRunning; time += interval) {
            ObjectNode data;
            try { // 多次循环读，避免阻塞时间过长，导致关闭过程缓慢
                data = taskSynchronousQueue.poll(interval, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                continue;
            }
            if (null != data) return data;
        }
        return null;
    }

    @Override
    public void run() {
        runningCounter.incrementAndGet();
        HttpFetcher fetcher;
        try {
            fetcher = fetcherPool.borrowObject();
        } catch (Exception e) {
            log.warn("node service borrow fetcher failed", e);
            return;
        }
        while (this.isRunning) {
            if (!obtain(fetcher)) continue;
            JsonNode json = DPUtil.parseJSON(fetcher.getLastResult());
            int code = null == json ? -2 : json.at("/code").asInt(-1);
            if (0 != code) {
                synchronized (idleLock) {
                    try {
                        if (1002 == code) { // 无可用令牌
                            if (workerPool.getNumActive() > 0) {
                                idleLock.wait(300); // 短暂等待
                            } else {
                                idleLock.wait(35000); // 工作者也闲置，等待时间长一些
                            }
                        } else if (1005 == code) { // 无可用任务
                            if (workerPool.getNumActive() > 0) {
                                idleLock.wait(50); // 短暂等待
                            } else {
                                idleLock.wait(15000); // 工作者也闲置，等待时间长一些
                            }
                        }
                    } catch (InterruptedException ignored) {}
                }
                continue; // 获取任务失败，继续尝试
            }
            ObjectNode data = (ObjectNode) json.at("/data");
            if (!taskSynchronousQueue.offer(data)) { // 尝试将作业分配给等待队列
                Worker worker; // 入队失败，将作业直接交给新工作者执行
                try {
                    worker = this.workerPool.borrowObject();
                } catch (Exception e) {
                    Map<String, Object> result = ApiUtil.result(7501, "node service borrow worker failed", data);
                    if (!report(fetcher, result)) continue;
                    json = DPUtil.parseJSON(fetcher.getLastResult());
                    if (null == json || 0 != json.at("/code").asInt(-1)) { // 上报结果异常
                        log.warn("node service report failed: {}", json, fetcher.getLastException());
                    }
                    continue;
                }
                worker.call(this, data);
            }
        }
        try {
            fetcherPool.returnObject(fetcher);
        } catch (Exception e) {
            log.warn("node service return fetcher failed", e);
        }
        runningCounter.decrementAndGet();
    }

}
