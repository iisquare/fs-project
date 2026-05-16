package com.iisquare.fs.app.crawler.channel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.app.crawler.node.ZooKeeperClient;
import com.iisquare.fs.app.crawler.schedule.Group;
import com.iisquare.fs.app.crawler.schedule.Task;
import com.iisquare.fs.app.crawler.schedule.Token;
import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class RedisChannel implements Closeable, Runnable {

    protected final static Logger logger = LoggerFactory.getLogger(RedisChannel.class);
    private RedisClient client; // 仅在内部持有并在退出时关闭，请勿直接使用
    private ObjectPool<StatefulRedisConnection<String, String>> pool;
    private String prefix;
    private ZooKeeperClient zookeeper;
    private long timeout;
    private boolean canRunning = true;
    private LinkedBlockingDeque<RedisCallback> queue = new LinkedBlockingDeque<>();

    public RedisChannel(ZooKeeperClient zookeeper, String uri, int minIdle, int maxIdle, int maxTotal, String prefix, long timeout) {
        this.zookeeper = zookeeper;
        this.prefix = prefix + "channel:";
        this.timeout = timeout;
        this.client = RedisClient.create(uri);
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMinIdle(minIdle);
        config.setMaxIdle(maxIdle);
        config.setMaxTotal(maxTotal);
        this.pool = new GenericObjectPool(new RedisFactory(client), config);
        new Thread(this).start();
    }

    /**
     * 阻塞队列积压量
     */
    public int overstock() {
        return queue.size();
    }

    @Override
    public void run() {
        StatefulRedisConnection<String, String> connect = borrowConnection();
        while (canRunning || queue.size() > 0) {
            RedisCallback callback;
            try {
                callback = queue.take();
            } catch (InterruptedException e) {
                continue;
            }
            while (callback.invoke(connect)) {
                logger.warn("invoke redis callback " + callback + " failed, retry with canRunning=" + canRunning);
                if (canRunning) Thread.yield();
            }
        }
        returnConnection(connect);
    }

    public void clearToken() {
        queue.offer(new RedisCallback("clearToken") {
            @Override
            public boolean invoke(StatefulRedisConnection<String, String> connect) {
                return -1 == clearToken(connect);
            }
        });
    }

    public void clearTask(String scheduleId) {
        queue.offer(new RedisCallback("clearTask") {
            @Override
            public boolean invoke(StatefulRedisConnection<String, String> connect) {
                return -1 == clearTask(connect, scheduleId);
            }
        });
    }

    public void putToken(Token token) {
        queue.offer(new RedisCallback("putToken") {
            @Override
            public boolean invoke(StatefulRedisConnection<String, String> connect) {
                return -1 == putToken(connect, token);
            }
        });
    }

    public void putTask(Task task) {
        queue.offer(new RedisCallback("putTask") {
            @Override
            public boolean invoke(StatefulRedisConnection<String, String> connect) {
                return -1 == putTask(connect, task);
            }
        });
    }

    public String keyToken() {
        return prefix + "token:all";
    }

    public String keyConcurrent(Group group, long time) {
        if (null == group) return null; // 无限制
        String target = group.getTarget(), key = prefix + "concurrent:";
        if ("node".equals(target)) {
            key += "node-" + zookeeper.nodeId();
        } else {
            key += "group-" + group.getName();
        }
        int interval = group.getInterval();
        if (interval < 1) interval = 1;
        key += "-tick" + (time / interval);
        return key;
    }

    public String keyTask(String scheduleId) {
        return prefix + "task:" + scheduleId;
    }

    public StatefulRedisConnection<String, String> borrowConnection() {
        try {
            return pool.borrowObject();
        } catch (Exception e) {
            logger.warn("borrow redis connection failed", e);
            return null;
        }
    }

    public boolean returnConnection(StatefulRedisConnection<String, String> connection) {
        try {
            pool.returnObject(connection);
            return true;
        } catch (Exception e) {
            logger.warn("return redis connection failed", e);
            return false;
        }
    }

    public long sizeToken(StatefulRedisConnection<String, String> connect) {
        Long result = connect.sync().zcard(keyToken());
        return result == null ? -1 : result;
    }

    public long sizeTask(StatefulRedisConnection<String, String> connect, String scheduleId) {
        Long result = connect.sync().zcard(keyTask(scheduleId));
        return result == null ? -1 : result;
    }

    private long clearToken(StatefulRedisConnection<String, String> connect) {
        Long result = connect.sync().del(keyToken());
        return result == null ? -1 : result;
    }

    private long clearTask(StatefulRedisConnection<String, String> connect, String scheduleId) {
        Long result = connect.sync().del(keyTask(scheduleId));
        return result == null ? -1 : result;
    }

    /**
     * 查看最近一次Token执行资质
     */
    public JsonNode topToken(StatefulRedisConnection<String, String> connect) {
        String keyToken = keyToken();
        List<ScoredValue<String>> list = connect.sync().zrangeWithScores(keyToken, 0, 0);
        if (null == list || list.size() != 1) return null;
        ScoredValue<String> item = list.get(0);
        ObjectNode result = DPUtil.objectNode();
        result.put("score", item.getScore());
        result.putPOJO("item", Token.decode(item.getValue()));
        return result;
    }

    /**
     * 获取Token执行资质
     */
    public Token takeToken(StatefulRedisConnection<String, String> connect) {
        String keyToken = keyToken();
        RedisFuture<List<String>> future = connect.async().zrangebyscore(
                keyToken, Range.create(0, System.currentTimeMillis()), Limit.from(1));
        List<String> list;
        try {
            list = timeout > 0 ? future.get(timeout, TimeUnit.MILLISECONDS) : future.get();
        } catch (Exception e) {
            logger.warn("failed to take token", e);
            return null;
        }
        if (list.size() != 1) return null;
        Long result = connect.sync().zrem(keyToken, list.get(0));
        if (null == result || result != 1) return null;
        return Token.decode(list.get(0));
    }

    /**
     * 放置Token执行资质
     */
    private long putToken(StatefulRedisConnection<String, String> connect, Token token) {
        Long result = connect.sync().zadd(keyToken(), token.score(), Token.encode(token));
        return null == result ? -1 : result;
    }

    /**
     * 检查并发限制
     */
    public boolean checkConcurrent(StatefulRedisConnection<String, String> connect, Group group, Token token) {
        long time = System.currentTimeMillis();
        String keyConcurrent = keyConcurrent(group, time); // 限制并发数
        if (null == keyConcurrent) return true;
        Long result = connect.sync().incr(keyConcurrent);
        if (null == result) return false;
        connect.sync().pexpire(keyConcurrent, group.getInterval());
        if (result <= group.getConcurrent()) return true;
        long halt = (time / group.getInterval() + 1) * group.getInterval() - time; // 下一个并发解除点
        token.halt(halt);
        return false;
    }

    /**
     * 查看最近一次任务
     */
    public JsonNode topTask(StatefulRedisConnection<String, String> connect, String scheduleId) {
        String keyTask = keyTask(scheduleId);
        List<ScoredValue<String>> list = connect.sync().zrangeWithScores(keyTask, 0, 0);
        if (null == list || list.size() != 1) return null;
        ScoredValue<String> item = list.get(0);
        ObjectNode result = DPUtil.objectNode();
        result.put("score", item.getScore());
        result.putPOJO("item", Task.decode(item.getValue()));
        return result;
    }

    /**
     * 获取任务
     */
    public Task takeTask(StatefulRedisConnection<String, String> connect, String scheduleId) {
        String keyTask = keyTask(scheduleId);
        RedisFuture<List<String>> future = connect.async().zrangebyscore(keyTask, Range.create(0, System.currentTimeMillis()), Limit.from(1));
        List<String> list;
        try {
            list = timeout > 0 ? future.get(timeout, TimeUnit.MILLISECONDS) : future.get();
        } catch (Exception e) {
            logger.warn("failed to take task", e);
            return null;
        }
        if (list.size() != 1) return null;
        Long result = connect.sync().zrem(keyTask, list.get(0));
        if (null == result || result != 1) return null;
        return Task.decode(list.get(0));
    }

    /**
     * 载入任务
     */
    private long putTask(StatefulRedisConnection<String, String> connect, Task task) {
        Long result = connect.sync().zadd(keyTask(task.getScheduleId()), task.score(), Task.encode(task));
        return null == result ? -1 : result;
    }

    @Override
    public void close() throws IOException {
        canRunning = false; // 在调度器结束后执行关闭，否则可能导致数据丢失
        while (queue.size() > 0) {
            System.out.println("still " + queue.size() + " redis writer in channel, please wait...");
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {}
        }
        if (null != pool) {
            pool.close();
            pool = null;
        }
        if (null != client) {
            client.shutdown();
            client = null;
        }
    }

}
