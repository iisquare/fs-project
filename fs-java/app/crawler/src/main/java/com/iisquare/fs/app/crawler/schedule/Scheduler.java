package com.iisquare.fs.app.crawler.schedule;

import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.app.crawler.channel.RedisChannel;
import com.iisquare.fs.app.crawler.fetch.HttpFetcher;
import com.iisquare.fs.app.crawler.fetch.HttpFetcherFactory;
import com.iisquare.fs.app.crawler.node.ZooKeeperClient;
import com.iisquare.fs.app.crawler.parse.Parser;
import com.iisquare.fs.app.crawler.parse.ParserFactory;
import io.lettuce.core.api.StatefulRedisConnection;
import lombok.Getter;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.Closeable;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

@Getter
public class Scheduler implements Closeable, Runnable {

    protected final static Logger logger = LoggerFactory.getLogger(Scheduler.class);

    private GenericObjectPoolConfig poolConfig;
    private ObjectPool<Worker> workerPool;
    private ObjectPool<HttpFetcher> fetcherPool;
    private ParserFactory parserFactory;
    private ProxyFactory proxyFactory;
    private ScriptEngineManager scriptEngineManager;
    private ZooKeeperClient zookeeper;
    private RedisChannel channel;
    private volatile boolean isRunning = false;
    private long emptyAwaitTime = 30000; // 阻塞获取Token最大等待时间
    private long emptySleepInterval = 300; // 拉取Token失败后的循环等待间隔
    private int doneCheckThreshold = 10; // 作业完成确认次数，为零时不自动切换完成状态
    private int doneCheckDelay = 15000; // 两次确认之间的等待时长
    private int doingIterateCount = 100; // 工作者每次触发后的最大循环次数
    private final transient ReentrantLock lockWorker = new ReentrantLock();
    private final transient ReentrantLock lockCounter = new ReentrantLock();
    private final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>(); // 占有可用Token计数
    // 使用空容量同步队列复用线程，非阻塞写，半阻塞读
    private final SynchronousQueue<Job> jobSynchronousQueue = new SynchronousQueue<>();

    public Scheduler(int minIdle, int maxIdle, int maxTotal, ZooKeeperClient zookeeper, RedisChannel channel) {
        this.zookeeper = zookeeper;
        this.channel = channel;
        this.poolConfig = new GenericObjectPoolConfig();
        poolConfig.setMinIdle(minIdle);
        poolConfig.setMaxIdle(maxIdle);
        poolConfig.setMaxTotal(maxTotal);
        this.workerPool = new  GenericObjectPool<>(new WorkerFactory(),this. poolConfig);
        this.fetcherPool= new GenericObjectPool<>(new HttpFetcherFactory(), this.poolConfig);
        this.parserFactory = new ParserFactory();
        this.scriptEngineManager = new ScriptEngineManager();
        this.proxyFactory = new ProxyFactory(zookeeper);
    }

    public AtomicInteger counter(String scheduleId, boolean ensure) {
        AtomicInteger counter = counters.get(scheduleId);
        if (null != counter || !ensure) return counter;
        lockCounter.lock();
        counter = counters.get(scheduleId);
        if (null == counter) {
            counter = new AtomicInteger(0);
            counters.put(scheduleId, counter);
        }
        lockCounter.unlock();
        return counter;
    }

    public boolean increment(History history, Token token) {
        int limit = history.getLimit(); // 每个节点允许获取资源限制
        if (limit < 1) return true; // 不执行计数
        AtomicInteger counter = counter(token.getScheduleId(), true); // 获取计数器
        if (counter.get() >= limit) return false;
        int count = counter.incrementAndGet(); // 累加计数
        if (count > limit) { // 超出限制
            counter.decrementAndGet();
            return false; // 标记无效，忽略丢弃
        }
        return true;
    }

    public void decrement(Token token) {
        AtomicInteger counter = counter(token.getScheduleId(), false);
        if (null != counter) {
            int count = counter.decrementAndGet();
            if (count < 0) clearCounter(token.getScheduleId());
        }
    }

    public void clearCounter(String scheduleId) {
        lockCounter.lock();
        counters.remove(scheduleId);
        lockCounter.unlock();
    }

    public Scheduler emptyAwaitTime(long emptyAwaitTime) {
        this.emptyAwaitTime = emptyAwaitTime;
        return this;
    }

    public Scheduler emptySleepInterval(long emptySleepInterval) {
        this.emptySleepInterval = emptySleepInterval;
        return this;
    }

    public Scheduler doneCheckThreshold(int doneCheckThreshold) {
        this.doneCheckThreshold = doneCheckThreshold;
        return this;
    }

    public Scheduler doneCheckDelay(int doneCheckDelay) {
        this.doneCheckDelay = doneCheckDelay;
        return this;
    }

    public Scheduler doingIterateCount(int doingIterateCount) {
        this.doingIterateCount = doingIterateCount;
        return this;
    }

    public ScriptEngine scriptEngine() {
        return scriptEngineManager.getEngineByName("js");
    }

    public Schedule schedule(String scheduleId) {
        return zookeeper.schedule(scheduleId);
    }

    public Template template(String scheduleId, String templateKey) {
        Schedule schedule = schedule(scheduleId);
        if (null == schedule) return null;
        return schedule.template(templateKey);
    }

    public  Parser parser(String name, String template) throws Exception {
        return parserFactory.parser(name, template);
    }

    public HttpFetcher borrowFetcher() {
        try {
            return fetcherPool.borrowObject();
        } catch (Exception e) {
            logger.warn("borrow fetcher failed", e);
            return null;
        }
    }

    public void returnFetcher(HttpFetcher fetcher) {
        try {
            fetcherPool.returnObject(fetcher);
        } catch (Exception e) {
            logger.warn("return fetcher failed", e);
        }
    }

    public void schedule(Schedule schedule, boolean reset) {
        if (null == schedule) return;
        this.schedule(schedule, schedule.getInitTask(), schedule.getInitParams(), reset);
    }

    public void schedule(Task task, boolean reset) {
        if (null == task) return;
        if (reset) task.reset(this);
        this.channel.putTask(task);
    }

    public void schedule(Schedule schedule, String key, Map<String, String> param, boolean reset) {
        if (null == schedule || schedule.template(key) == null) return;
        if (null == param) param = new LinkedHashMap<>();
        Task task = new Task(schedule.getId(), key, param);
        this.schedule(task, reset);
    }

    public void schedule(Schedule schedule, String key, List<Map<String, String>> params, boolean reset) {
        if (null == schedule || schedule.template(key) == null) return;
        if (null == params) {
            this.schedule(schedule, key, new LinkedHashMap<>(), reset);
        } else {
            for (Map<String, String> param : params) {
                this.schedule(schedule, key, param, reset);
            }
        }
    }

    public void start() {
        if (this.isRunning) return;
        this.isRunning = true;
        new Thread(this).start();
    }

    public void stop() {
        this.isRunning = false;
    }

    public boolean clear(String scheduleId) {
        channel.clearTask(scheduleId);
        return zookeeper.publish(Notice.clearCounter(scheduleId));
    }

    public boolean isRunning(String scheduleId) {
        History history = zookeeper.history(scheduleId);
        if (null == history) return false;
        return History.STATUS_RUNNING.equals(history.getStatus());
    }

    public void done(History history) {
        zookeeper.save(history);
        this.clear(history.getScheduleId());
    }

    @Override
    public void close() throws IOException {
        this.stop();
        while (workerPool.getNumActive() > 0) {
            System.out.println("still " + workerPool.getNumActive() + " workers are running, please wait...");
            try {
                Thread.sleep(getEmptySleepInterval());
            } catch (InterruptedException e) {}
        }
        if (null != this.workerPool) {
            this.workerPool.close();
            this.workerPool = null;
        }
        if (null != this.fetcherPool) {
            this.fetcherPool.close();
            this.fetcherPool = null;
        }
    }

    protected Job takeJob() {
        long interval = getEmptySleepInterval();
        long await = getEmptyAwaitTime();
        for (long time = interval; time <= await && isRunning; time += interval) {
            Job job;
            try { // 多次循环读，避免阻塞时间过长，导致关闭过程缓慢
                job = jobSynchronousQueue.poll(interval, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                continue;
            }
            if (null != job) return job;
        }
        return null;
    }

    private Token takeToken(StatefulRedisConnection<String, String> connect) {
        long interval = getEmptySleepInterval();
        long await = getEmptyAwaitTime();
        for (long time = interval; time <= await && isRunning; time += interval) {
            Token token = channel.takeToken(connect);
            if (null != token) return token;
            try {
                Thread.sleep(interval);
            } catch (InterruptedException e) {}
        }
        return null;
    }

    private Token dispatch(StatefulRedisConnection<String, String> connect) {
        Token token = takeToken(connect);
        if (null == token) return null; // 未获取到任务
        Schedule schedule = zookeeper.schedule(token.getScheduleId());
        if (null == schedule) return null; // 作业被移除
        History history = zookeeper.history(schedule.getId());
        if (null == history) return null; // 状态被移除
        if (token.getVersion() != history.getVersion()) return null; // 状态发生变更
        if (!History.STATUS_RUNNING.equals(history.getStatus())) return null; // 状态非执行
        int doneCheckThreshold = getDoneCheckThreshold(); // 判断作业是否执行完成
        if (0 != doneCheckThreshold && token.getDoneCheckCount() > doneCheckThreshold && channel.sizeTask(connect, token.getScheduleId()) == 0) {
            history.setStatus(History.STATUS_FINISHED);
            history.setVersion(System.currentTimeMillis());
            done(history);
            return null;
        }
        if (!increment(history, token)) { // 本地持有可用Token数已满，放弃执行并放回
            return token;
        }
        Group group = zookeeper.group(token.getGroupName());
        if (!channel.checkConcurrent(connect, group, token)) return token;
        Task task = channel.takeTask(connect, token.getScheduleId());
        if (null == task) { // 未获取到作业
            token.done();
            return token.halt(getDoneCheckDelay());
        }
        token.active(); // 获取到作业，将Token标记为激活状态
        if (System.currentTimeMillis() - task.getDispatchTime() < task.getDispatchInterval()) {
            schedule(task, false);
            return token;
        }
        if (DPUtil.empty(task.getUrl())) return token;
        Template template = task.template(this);
        if (null == template) {
            token.done();
            return token;
        }
        Job job = new Job(this, token, schedule, task, template);
        if (!jobSynchronousQueue.offer(job)) { // 尝试将作业分配给等待队列
            Worker worker = null; // 入队失败，将作业直接交给新工作者执行
            try {
                worker = this.workerPool.borrowObject();
            } catch (Exception e) {
                logger.warn("scheduler dispatch borrowObject failed", e);
            }
            worker.call(job);
        }
        return null;
    }

    public void tick(Token token) {
        if (null == token) return;
        decrement(token);
        channel.putToken(token);
    }

    @Override
    public void run() {
        StatefulRedisConnection<String, String> connect = channel.borrowConnection();
        while (this.isRunning) {
            tick(dispatch(connect));
        }
        channel.returnConnection(connect);
    }
}
