package com.iisquare.fs.app.crawler.web.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.app.crawler.channel.RedisChannel;
import com.iisquare.fs.app.crawler.fetch.HttpFetcher;
import com.iisquare.fs.app.crawler.node.ZooKeeperClient;
import com.iisquare.fs.app.crawler.schedule.*;
import com.iisquare.fs.app.crawler.web.Configuration;
import com.iisquare.fs.app.crawler.web.ServiceBase;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import io.lettuce.core.api.StatefulRedisConnection;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ScheduleService extends ServiceBase implements Closeable {

    private static ScheduleService instance = null;
    private Configuration configuration = Configuration.getInstance();
    private NodeService nodeService = NodeService.getInstance();
    private Scheduler scheduler;
    private RedisChannel channel;

    private ScheduleService() {
        configuration.defer(this);
        JsonNode config = configuration.config();
        ZooKeeperClient zookeeper = nodeService.zookeeper();
        int minIdle = config.at("/crawler/schedule/minIdle").asInt(1);
        int maxIdle = config.at("/crawler/schedule/maxIdle").asInt(5);
        int maxTotal = config.at("/crawler/schedule/maxTotal").asInt(100);
        this.channel = new RedisChannel(nodeService.zookeeper(),
                config.at("/crawler/redis/uri").asText("redis://127.0.0.1:6379/0"),
                minIdle, maxIdle, maxTotal + 1,
                config.at("/crawler/redis/prefix").asText("crawler:channel:"),
                config.at("/crawler/redis/timeout").asLong(1500));
        this.scheduler = new Scheduler(minIdle, maxIdle, maxTotal, zookeeper, channel);
        this.scheduler.emptyAwaitTime(config.at("/crawler/schedule/emptyAwaitTime").asInt(30000));
        this.scheduler.emptySleepInterval(config.at("/crawler/schedule/emptySleepInterval").asInt(300));
        this.scheduler.doneCheckThreshold(config.at("/crawler/schedule/doneCheckThreshold").asInt(10));
        this.scheduler.doneCheckDelay(config.at("/crawler/schedule/doneCheckDelay").asInt(15000));
        this.scheduler.doingIterateCount(config.at("/crawler/schedule/doingIterateCount").asInt(100));
        this.start(); // 激活调度器
        zookeeper.listen(new WatchListener(this.scheduler)); // 接收通知
        this.rebalance(); // 恢复任务
    }

    public Scheduler scheduler() {
        return this.scheduler;
    }

    public ArrayNode histories() {
        ArrayNode data = DPUtil.arrayNode();
        Map<String, History> map = nodeService.zookeeper().histories();
        StatefulRedisConnection<String, String> connect = channel.borrowConnection();
        for (Map.Entry<String, History> entry : map.entrySet()) {
            String key = entry.getKey();
            ObjectNode item = (ObjectNode) DPUtil.toJSON(entry.getValue());
            item.put("channel", channel.sizeTask(connect, key));
            item.replace("top", channel.topTask(connect, key));
            data.add(item);
        }
        channel.returnConnection(connect);
        return data;
    }

    public Map<String, Group> groups() {
        return nodeService.zookeeper().groups();
    }

    public Map<String, Proxy> proxies() {
        return nodeService.zookeeper().proxies();
    }

    public Map<String, Schedule> schedules() {
        return nodeService.zookeeper().schedules();
    }

    public Object save(String type, JsonNode content) {
        if (!Arrays.asList("schedule", "group", "proxy").contains(type)) return null;
        String id, data;
        Object object;
        switch (type) {
            case "schedule":
                Schedule schedule = new Schedule(content);
                object = schedule;
                id = schedule.getId();
                data = Schedule.encode(schedule);
                break;
            case "group":
                Group group = Group.decode(DPUtil.stringify(content));
                object = group;
                id = group.getName();
                data = Group.encode(group);
                break;
            case "proxy":
                Proxy proxy = Proxy.decode(DPUtil.stringify(content));
                object = proxy;
                id = proxy.getName();
                data = Proxy.encode(proxy);
                break;
            default:
                return null;
        }

        boolean result = nodeService.zookeeper().save("/" + type + "/" + id, data);
        if (!result) return null;
        return object;
    }

    public boolean remove(String type, String id) {
        if (!Arrays.asList("schedule", "group", "proxy", "history").contains(type)) return false;
        return nodeService.zookeeper().save("/" + type + "/" + id, null);
    }

    public void start() {
        this.scheduler.start();
    }

    public void stop() {
        this.scheduler.stop();
    }

    public static ScheduleService getInstance() {
        if (instance == null) {
            synchronized (ScheduleService.class) {
                if (instance == null) {
                    instance = new ScheduleService();
                }
            }
        }
        return instance;
    }

    public boolean rebalance() {
        ZooKeeperClient zookeeper = nodeService.zookeeper();
        while (null == zookeeper.leaderId()) { // 等待产生主节点
            try {
                Thread.sleep(scheduler.getEmptySleepInterval());
            } catch (InterruptedException e) {}
        }
        if (!zookeeper.isLeader()) return false;
        channel.clearToken();
        Map<String, History> histories = zookeeper.histories();
        for (Map.Entry<String, History> entry : histories.entrySet()) {
            History history = entry.getValue();
            if (!History.STATUS_RUNNING.equals(history.getStatus())) continue;
            Schedule schedule = zookeeper.schedule(history.getScheduleId());
            this.token(schedule, history);
        }
        return true;
    }

    public Map<String, Object> start(String scheduleId) {
        ZooKeeperClient zookeeper = nodeService.zookeeper();
        Schedule schedule = zookeeper.schedule(scheduleId);
        if (null == schedule) return ApiUtil.result(1001, "作业不存在", schedule);
        History history = zookeeper.history(scheduleId);
        if (null == history) return ApiUtil.result(1002, "历史状态不存在", history);
        if (!History.STATUS_PAUSE.equals(history.getStatus())) {
            return ApiUtil.result(1003, "仅允许从暂停状态唤起", history);
        }
        history.setStatus(History.STATUS_RUNNING);
        history.setToken(schedule.getMaxThread());
        history.setLimit(schedule.getMaxPerNode());
        history.setVersion(System.currentTimeMillis());
        if (!zookeeper.save(history)) return ApiUtil.result(1004, "切换状态失败", history);
        if (!token(schedule, history)) return ApiUtil.result(1005, "生成Token失败", history);
        return ApiUtil.result(0, null, history);
    }

    public Map<String, Object> pause(String scheduleId) {
        ZooKeeperClient zookeeper = nodeService.zookeeper();
        History history = zookeeper.history(scheduleId);
        if (null == history) return ApiUtil.result(1001, "历史状态不存在", history);
        if (!History.STATUS_RUNNING.equals(history.getStatus())) {
            return ApiUtil.result(1002, "仅允许从执行状态暂停", history);
        }
        history.setStatus(History.STATUS_PAUSE);
        history.setVersion(System.currentTimeMillis());
        if (!zookeeper.save(history)) return ApiUtil.result(1003, "切换状态失败", history);
        return ApiUtil.result(0, null, history);
    }

    public Map<String, Object> stop(String scheduleId) {
        ZooKeeperClient zookeeper = nodeService.zookeeper();
        History history = zookeeper.history(scheduleId);
        if (null == history) return ApiUtil.result(1001, "历史状态不存在", history);
        if (!Arrays.asList(History.STATUS_RUNNING, History.STATUS_PAUSE).contains(history.getStatus())) {
            return ApiUtil.result(1002, "仅允许从执行或暂停状态停止", history);
        }
        history.setStatus(History.STATUS_STOP);
        history.setVersion(System.currentTimeMillis());
        if (!zookeeper.save(history)) return ApiUtil.result(1003, "切换状态失败", history);
        scheduler.clear(scheduleId);
        return ApiUtil.result(0, null, history);
    }

    public boolean clear(String scheduleId) {
        return scheduler.clear(scheduleId);
    }

    public JsonNode channelState() {
        ObjectNode state = DPUtil.objectNode();
        StatefulRedisConnection<String, String> connect = channel.borrowConnection();
        state.put("size", channel.sizeToken(connect));
        state.put("overstock", channel.overstock());
        state.replace("top", channel.topToken(connect));
        channel.returnConnection(connect);
        return state;
    }

    public JsonNode counterState() {
        ObjectNode state = DPUtil.objectNode();
        for (Map.Entry<String, AtomicInteger> entry : scheduler.getCounters().entrySet()) {
            state.put(entry.getKey(), entry.getValue().get());
        }
        return state;
    }

    public boolean token(Schedule schedule, History history) {
        if (null == schedule || null == history) return false;
        Token token = Token.record(schedule, history);
        if (null == token) return false;
        int size = history.getToken();
        for (int i = 0; i < size; i++) {
            channel.putToken(token.reuse(true));
        }
        return true;
    }

    public Map<String, Object> submit(String scheduleId) {
        if (null == scheduler) return ApiUtil.result(1001, "调度器异常", null);
        ZooKeeperClient zookeeper = nodeService.zookeeper();
        Schedule schedule = zookeeper.schedule(scheduleId);
        if (schedule == null) return ApiUtil.result(1002, "数据读取异常", schedule);
        History history = zookeeper.history(schedule.getId());
        if (null != history && Arrays.asList(History.STATUS_PAUSE, History.STATUS_RUNNING).contains(history.getStatus())) {
            return ApiUtil.result(1003, "当前作业正在执行", history);
        }
        history = History.record(schedule);
        if (null == history) return ApiUtil.result(1004, "生成历史状态失败", history);
        if (!zookeeper.save(history)) return ApiUtil.result(1005, "保存执行状态失败", history);
        scheduler.schedule(schedule, true);
        if (!token(schedule, history)) return ApiUtil.result(1006, "生成Token失败", null);
        return ApiUtil.result(0, null, schedule);
    }

    public Map<String, Object> submit(String scheduleId, String templateKey, JsonNode parameters) {
        if (null == scheduler) return ApiUtil.result(1001, "调度器异常", null);
        ZooKeeperClient zookeeper = nodeService.zookeeper();
        Schedule schedule = zookeeper.schedule(scheduleId);
        if (schedule == null) return ApiUtil.result(1002, "数据读取异常", schedule);
        History history = zookeeper.history(schedule.getId());
        if (null == history || !Arrays.asList(History.STATUS_PAUSE, History.STATUS_RUNNING).contains(history.getStatus())) {
            return ApiUtil.result(1003, "当前作业停止接收任务", history);
        }
        if (null != parameters && parameters.isTextual()) parameters = DPUtil.parseJSON(parameters.asText());
        scheduler.schedule(schedule, templateKey, schedule.params(parameters), true);
        return ApiUtil.result(0, null, history);
    }

    public JsonNode state() {
        if (null == scheduler) return null;
        ObjectNode state = DPUtil.objectNode();
        ObjectNode node = state.putObject("pool");
        GenericObjectPoolConfig poolConfig = scheduler.getPoolConfig();
        node.put("minIdle", poolConfig.getMinIdle());
        node.put("maxIdle", poolConfig.getMaxIdle());
        node.put("maxTotal", poolConfig.getMaxTotal());
        node = state.putObject("fetcher");
        ObjectPool<HttpFetcher> fetcherPool = scheduler.getFetcherPool();
        node.put("numActive", fetcherPool.getNumActive());
        node.put("numIdle", fetcherPool.getNumIdle());
        node = state.putObject("worker");
        ObjectPool<Worker> threadPool = scheduler.getWorkerPool();
        node.put("numActive", threadPool.getNumActive());
        node.put("numIdle", threadPool.getNumIdle());
        return state;
    }

    @Override
    public void close() throws IOException {
        if (null != this.scheduler) {
            this.scheduler.close();
            this.scheduler = null;
        }
        if (null != this.channel) {
            this.channel.close();
            this.channel = null;
        }
    }
}
