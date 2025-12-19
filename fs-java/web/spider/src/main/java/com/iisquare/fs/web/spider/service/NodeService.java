package com.iisquare.fs.web.spider.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.FileUtil;
import com.iisquare.fs.base.core.util.HttpUtil;
import com.iisquare.fs.base.mongodb.MongoCore;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.spider.core.*;
import com.iisquare.fs.web.spider.mongo.HtmlMongo;
import com.mongodb.client.model.Filters;
import org.apache.catalina.connector.Connector;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.curator.framework.recipes.leader.Participant;
import org.apache.tomcat.util.threads.ThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class NodeService extends ServiceBase implements Runnable, InitializingBean, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(NodeService.class);
    @Value("${fs.spider.zookeeper.host}")
    private String zhHost;
    @Value("${fs.spider.zookeeper.timeout}")
    private int zhTimeout;
    @Value("${fs.spider.zookeeper.nodeId}")
    private String zhNodeId;
    @Value("${fs.spider.redis.keyPrefix}")
    private String keyPrefix;
    @Value("${fs.spider.worker.minIdle}")
    private Integer workerMinIdle;
    @Value("${fs.spider.worker.maxIdle}")
    private Integer workerMaxIdle;
    @Value("${fs.spider.worker.maxTotal}")
    private Integer workerMaxTotal;

    private ZooKeeperClient zookeeper;
    private RedisChannel channel;
    private volatile boolean isRunning = false;
    private ObjectPool<Worker> workerPool;
    private final AtomicInteger runningCounter = new AtomicInteger(0); // 当前正在运行的调度线程数量
    // 使用空容量同步队列复用线程，非阻塞写，半阻塞读
    private final SynchronousQueue<ObjectNode> storageSynchronousQueue = new SynchronousQueue<>();
    private final Object idleLock = new Object(); // 用于空闲等待，便于在进程退出时及时唤醒

    @Autowired
    public HtmlMongo htmlMongo;
    @Autowired
    TemplateService templateService;
    @Autowired
    private StringRedisTemplate redis;
    @Autowired
    private ServletWebServerApplicationContext context;

    @Override
    public void afterPropertiesSet() throws Exception {
        zookeeper = new ZooKeeperClient(zhHost, zhTimeout, zhNodeId);
        zookeeper.listen(new WatchListener(this));
        zookeeper.open();
        channel = new RedisChannel(keyPrefix, this);
        GenericObjectPoolConfig<Worker> workerPoolConfig = new GenericObjectPoolConfig<>();
        workerPoolConfig.setMaxIdle(workerMinIdle);
        workerPoolConfig.setMaxIdle(workerMaxIdle);
        workerPoolConfig.setMaxTotal(workerMaxTotal);
        this.workerPool = new GenericObjectPool<>(new WorkerFactory(), workerPoolConfig);
        this.start();
    }

    @Override
    public void destroy() throws Exception {
        this.stop();
        synchronized (idleLock) {
            idleLock.notifyAll(); // 唤醒空闲等待线程
        }
        while (workerPool.getNumActive() > 0 || runningCounter.get() > 0) {
            log.info("still {} workers and {} schedule are running, please wait...", workerPool.getNumActive(), runningCounter.get());
            try {
                if (workerPool.getNumActive() > 0 || runningCounter.get() > 0) {
                    Thread.sleep(3000); // 等待空闲线程释放，一般不会被打断
                }
            } catch (InterruptedException ignored) {}
        }
        FileUtil.close(zookeeper);
        zookeeper = null;
    }

    public ZooKeeperClient zookeeper() {
        return this.zookeeper;
    }

    public StringRedisTemplate redis() {
        return this.redis;
    }

    public RedisChannel channel() {
        return this.channel;
    }

    public String nodeId() {
        return this.zhNodeId;
    }

    public ObjectPool<Worker> workerPool() {
        return this.workerPool;
    }

    public ObjectNode state() throws Exception {
        ObjectNode state = DPUtil.objectNode();
        state.put("nodeId", zookeeper.nodeId());
        state.put("role", "spider");
        state.put("state", zookeeper.state());
        state.put("leader", zookeeper.leaderId());
        state.put("leadership", zookeeper.isLeader());
        state.put("isRunning", isRunning);
        state.put("runningCounter", runningCounter.get());
        ObjectNode worker = state.putObject("worker");
        worker.put("minIdle", workerMinIdle);
        worker.put("maxIdle", workerMaxIdle);
        worker.put("maxTotal", workerMaxTotal);
        worker.put("numIdle", workerPool.getNumIdle());
        worker.put("numActive", workerPool.getNumActive());
        ObjectNode server = state.putObject("server");
        if (context.getWebServer() instanceof TomcatWebServer tomcatWebServer) {
            // 获取连接器
            Connector connector = tomcatWebServer.getTomcat().getConnector();
            // 获取协议处理器
            if (connector.getProtocolHandler() instanceof Http11NioProtocol protocol) {
                // 获取线程池
                ThreadPoolExecutor executor = (ThreadPoolExecutor) protocol.getExecutor();
                if (executor != null) {
                    // 当前并发信息
                    server.put("activeThreads", executor.getActiveCount());
                    server.put("poolSize", executor.getPoolSize());
                    server.put("corePoolSize", executor.getCorePoolSize());
                    server.put("maxPoolSize", executor.getMaximumPoolSize());
                    server.put("largestPoolSize", executor.getLargestPoolSize());
                    server.put("queueSize", executor.getQueue().size());
                    server.put("completedTaskCount", executor.getCompletedTaskCount());
                    server.put("taskCount", executor.getTaskCount());
                }
                // Tomcat配置信息
                server.put("maxConnections", protocol.getMaxConnections());
                server.put("maxThreads", protocol.getMaxThreads());
                server.put("minSpareThreads", protocol.getMinSpareThreads());
                server.put("acceptCount", protocol.getAcceptCount());
                server.put("asyncTimeout", connector.getAsyncTimeout());
            }
        }
        return state;
    }

    public ObjectNode fetch(ZooNode node, Map<String, String> param) {
        String url = "http://" + node.host + ":" + node.port;
        String content = HttpUtil.get(url + "/node/state", param);
        if (null == content) return null;
        JsonNode json = DPUtil.parseJSON(content);
        if (null != json && 0 == json.at("/code").asInt()) {
            json = json.get("data");
            if (null != json && json.isObject()) return (ObjectNode) json;
        }
        return null;
    }

    public Map<String, Object> states(Map<String, String> param) {
        ObjectNode data = DPUtil.objectNode();
        ObjectNode nodes = data.putObject("nodes");
        for (Participant participant : zookeeper.participants()) {
            nodes.putPOJO(participant.getId(), participant);
        }
        ObjectNode spiders = data.putObject("spiders");
        for (Map.Entry<String, ZooNode> entry : zookeeper.spiders().entrySet()) {
            ZooNode node = entry.getValue();
            ObjectNode json = fetch(node, param);
            if (null == json) return ApiUtil.result(5001, "载入服务节点信息失败", node);
            json.put("time", node.time);
            spiders.replace(node.getId(), json);
        }
        ObjectNode crawlers = data.putObject("crawlers");
        for (Map.Entry<String, ZooNode> entry : zookeeper.crawlers().entrySet()) {
            ZooNode node = entry.getValue();
            ObjectNode json = fetch(node, param);
            if (null == json) return ApiUtil.result(5001, "载入采集节点信息失败", node);
            json.put("time", node.time);
            crawlers.replace(node.getId(), json);
        }
        ObjectNode token = data.putObject("token");
        token.put("size", channel.sizeToken());
        token.putObject("job");
        RedisToken topToken = channel.topToken();
        if (null == topToken) {
            token.putObject("top");
        } else {
            token.putPOJO("top", topToken);
            ZooJob job = zookeeper.job(topToken.getJobId());
            if (null != job) {
                token.putPOJO("job", job);
            }
        }
        ObjectNode storage = data.putObject("storage");
        storage.put("size", channel.sizeStorage());
        JsonNode topStorage = channel.topStorage();
        if (null == topStorage) {
            storage.putObject("top");
        } else {
            storage.replace("top", topStorage);
        }
        return ApiUtil.result(0, null, data);
    }

    public Map<String, Object> start(Map<String, Object> param) {
        ZooNotice notice = ZooNotice.startNode(DPUtil.parseString(param.get("nodeId")));
        boolean published = zookeeper.publish(notice);
        return ApiUtil.result(published ? 0 : 500, null, notice);
    }

    public Map<String, Object> stop(Map<String, Object> param) {
        ZooNotice notice = ZooNotice.stopNode(DPUtil.parseString(param.get("nodeId")));
        boolean published = zookeeper.publish(notice);
        return ApiUtil.result(published ? 0 : 500, null, notice);
    }

    public Map<String, Object> submit(JsonNode template) {
        String id = String.valueOf(template.at("/id").asInt(0));
        ZooJob job = zookeeper.job(id);
        long time = System.currentTimeMillis();
        if (null == job) {
            job = new ZooJob();
            job.id = id;
            job.status = ZooJob.Status.CREATED;
            job.createdTime = time;
            job.finishedTime = 0L;
            job.operatingTime = time;
        }
        job.updatedTime = time;
        job.template = template;
        boolean saved = zookeeper.save(job);
        return ApiUtil.result(saved ? 0 : 500, null, job);
    }

    public ObjectNode jobs() {
        ObjectNode data = DPUtil.objectNode();
        Map<String, ZooJob> jobs = zookeeper.jobs();
        Map<String, ZooRate> rates = zookeeper.rates();
        Map<String, String> types = templateService.types();
        for (Map.Entry<String, ZooJob> entry : jobs.entrySet()) {
            ZooJob job = entry.getValue();
            ObjectNode item = DPUtil.toJSON(entry.getValue(), ObjectNode.class);
            item.put("statusText", job.getStatus().text());
            ObjectNode template = (ObjectNode) item.at("/template");
            template.put("typeText", types.get(template.at("/type").asText()));
            ZooRate rate = rates.get(template.at("/rateId").asText());
            if (null == rate) {
                template.putObject("rateInfo");
            } else {
                template.putPOJO("rateInfo", rate);
            }
            ObjectNode channel = item.putObject("channel");
            channel.put("size", this.channel.sizeTask(job.getId()));
            RedisTask topTask = this.channel.topTask(job.getId());
            if (null == topTask) {
                channel.putObject("top");
            } else {
                channel.putPOJO("top", topTask);
            }
            data.replace(entry.getKey(), item);
        }
        return data;
    }

    public Map<String, Object> execute(ObjectNode json) {
        ZooJob job = zookeeper.job(json.at("/id").asText());
        if (null == job) return ApiUtil.result(1404, "作业不存在", null);
        if (job.status == ZooJob.Status.STOP) {
            return ApiUtil.result(1403, "作业已停止，暂不允许提交任务", job);
        }
        // 将任务提交到执行队列
        if (!"pan".equals(job.getTemplate().at("/type").asText())) {
            return ApiUtil.result(1401, "当前仅支持泛采集作业", job);
        }
        boolean force = json.at("/params/force").asBoolean();
        String referer = json.at("/params/referer").asText();
        String[] urls = DPUtil.explode("\n", json.at("/params/urls").asText());
        ArrayNode result = DPUtil.arrayNode();
        for (String url : urls) {
            url = DPUtil.explode("#", url)[0]; // 去除锚点信息
            ObjectNode item = result.addObject();
            item.put("url", url);
            if (url.startsWith("http://") || url.startsWith("https://")) {
                RedisTask task = RedisTask.record(job, url, force, referer);
                item.put("result", channel.putTask(task));
            } else {
                item.put("result", false);
            }
        }
        if (job.status == ZooJob.Status.PAUSE || job.status == ZooJob.Status.RUNNING) {
            return ApiUtil.result(0, null, result);
        }
        // 更新作业状态
        Map<String, Object> r = startJob(job);
        if (ApiUtil.failed(r)) return r;
        return ApiUtil.result(0, null, result);
    }

    private Map<String, Object> startJob(ZooJob job) {
        if (null == job) return ApiUtil.result(1301, "作业不存在", null);
        if (ZooJob.Status.RUNNING.equals(job.status)) {
            return ApiUtil.result(0, null, job);
        }
        job.status = ZooJob.Status.RUNNING;
        job.operatingTime = System.currentTimeMillis();
        if (!zookeeper.save(job)) {
            return ApiUtil.result(1501, "更改作业状态失败", job);
        }
        // 重新生成令牌Token
        int maxThreads = Math.max(1, job.template.at("/maxThreads").asInt());
        for (int i = 0; i < maxThreads; i++) {
            channel.putToken(RedisToken.record(job));
        }
        return ApiUtil.result(0, null, job);
    }

    public Map<String, Object> startJob(ObjectNode json) {
        ZooJob job = zookeeper.job(json.at("/id").asText());
        return startJob(job);
    }

    public Map<String, Object> pauseJob(ObjectNode json) {
        ZooJob job = zookeeper.job(json.at("/id").asText());
        if (null == job) return ApiUtil.result(1001, "作业不存在", json);
        if (ZooJob.Status.PAUSE.equals(job.status)) {
            return ApiUtil.result(0, null, job);
        }
        job.status = ZooJob.Status.PAUSE;
        job.operatingTime = System.currentTimeMillis();
        if (!zookeeper.save(job)) {
            return ApiUtil.result(1501, "更改作业状态失败", job);
        }
        return ApiUtil.result(0, null, job);
    }

    public Map<String, Object> stopJob(ObjectNode json) {
        ZooJob job = zookeeper.job(json.at("/id").asText());
        if (null == job) return ApiUtil.result(1001, "作业不存在", json);
        if (ZooJob.Status.STOP.equals(job.status)) {
            return ApiUtil.result(0, null, job);
        }
        job.status = ZooJob.Status.STOP;
        job.operatingTime = System.currentTimeMillis();
        if (!zookeeper.save(job)) {
            return ApiUtil.result(1501, "更改作业状态失败", job);
        }
        return ApiUtil.result(0, null, job);
    }

    public Map<String, Object> clearJob(ObjectNode json) {
        ZooJob job = zookeeper.job(json.at("/id").asText());
        if (null == job) return ApiUtil.result(1001, "作业不存在", json);
        if (!channel.clearTask(job.getId())) {
            return ApiUtil.result(1002, "清空任务队列失败", job);
        }
        return ApiUtil.result(0, null, job);
    }

    public Map<String, Object> ack(Map<String, String> param) {
        ObjectNode data = DPUtil.objectNode();
        Map<Object, Object> entries = redis.opsForHash().entries(channel().keyAck());
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            data.replace(DPUtil.parseString(entry.getKey()), DPUtil.parseJSON(DPUtil.parseString(entry.getValue())));
        }
        return ApiUtil.result(0, null, data);
    }

    public Map<String, Object> discardAck(ObjectNode json) {
        String key = json.at("/token/id").asText();
        if (DPUtil.empty(key)) {
            return ApiUtil.result(1001, "ACK不合法", json);
        }
        boolean removed = channel.removeAck(key);
        return ApiUtil.result(0, null, removed);
    }

    public boolean checkUrl(ZooJob job, RedisTask task) {
        URI uri;
        try {
            uri = new URI(task.url);
        } catch (URISyntaxException e) {
            return false;
        }
        boolean allowed = false;
        for (JsonNode whitelist : job.template.at("/whitelists")) {
            if (DPUtil.isMatcher(whitelist.at("/regexDomain").asText(), uri.getHost())) {
                if (DPUtil.isMatcher(whitelist.at("/regexPath").asText(), uri.getPath())) {
                    allowed = true;
                    break;
                }
            }
        }
        if (!allowed) return false;
        boolean denied = false;
        for (JsonNode blacklist : job.template.at("/blacklists")) {
            if (DPUtil.isMatcher(blacklist.at("/regexDomain").asText(), uri.getHost())) {
                if (DPUtil.isMatcher(blacklist.at("/regexPath").asText(), uri.getPath())) {
                    denied = true;
                    break;
                }
            }
        }
        if (denied) return false;
        if (task.force) return true;
        long count = htmlMongo.count(Filters.eq(MongoCore.FIELD_ID, task.url));
        return count == 0;
    }

    public Map<String, Object> obtain(ObjectNode json) {
        if (!isRunning) {
            return ApiUtil.result(1000, "服务端已停止调度", json);
        }
        String nodeId = json.at("/nodeId").asText();
        if (DPUtil.empty(nodeId)) {
            return ApiUtil.result(1001, "节点信息异常", json);
        }
        RedisToken token = channel.takeToken();
        if (null == token) {
            return ApiUtil.result(1002, "获取令牌失败", null);
        }
        ZooJob job = zookeeper.job(token.getJobId());
        if (null == job) { // 丢弃令牌
            return ApiUtil.result(1003, "获取作业失败", token);
        }
        if (!token.versionTime.equals(job.operatingTime)) { // 丢弃令牌
            return ApiUtil.result(1004, "Token已过期", token);
        }
        RedisTask task = channel.takeTask(job.getId());
        if (null == task) {
            if (10 > token.ineffectiveCount++) {
                channel.putToken(token.halt(5000)); // 暂停令牌调度
            } else { // 连续无效计数过多时丢弃令牌，并将作业标记为完成状态
                job.status = ZooJob.Status.FINISHED;
                zookeeper.save(job);
            }
            return ApiUtil.result(1005, "获取任务失败", job);
        } else {
            if (token.ineffectiveCount > 0) {
                token.ineffectiveCount = 0; // 重置无效计数
            }
        }
        if (!checkUrl(job, task)) {
            channel.putToken(token.back(job)); // 归还令牌
            return ApiUtil.result(1006, "任务链接地址不满足要求", task);
        }
        ObjectNode data = DPUtil.objectNode();
        data.put("time", System.currentTimeMillis());
        data.put("nodeId", nodeId);
        data.putPOJO("token", token);
        data.putPOJO("task", task);
        String rateId = job.getTemplate().at("/rateId").asText();
        if (DPUtil.empty(rateId)) {
            channel.putAck(token.getId(), data); // 放入待确认队列
            return ApiUtil.result(0, null, data);
        }
        ZooRate rate = zookeeper.rate(rateId);
        if (null == rate) {
            channel.putToken(token.back(job)); // 归还令牌
            channel.putTask(task); // 归还任务
            job.status = ZooJob.Status.PAUSE;
            zookeeper.save(job); // 暂停作业
            return ApiUtil.result(1010, "获取请求频率失败", job);
        }
        if (!channel.incrRateParallel(rate, token, job, task, nodeId, "")) {
            channel.putToken(token); // 归还令牌
            channel.putTask(task); // 归还任务
            return ApiUtil.result(1011, "并行度已达上限", rate);
        }
        if (!channel.checkRateConcurrent(rate, token, job, task, nodeId, "")) {
            channel.putToken(token); // 归还令牌
            channel.putTask(task); // 归还任务
            channel.decrRateParallel(rate, token, job, task, nodeId, ""); // 恢复计数
            return ApiUtil.result(1012, "并发数已达上限", rate);
        }
        channel.putAck(token.getId(), data); // 放入待确认队列
        return ApiUtil.result(0, null, data);
    }

    public Map<String, Object> report(ObjectNode json) {
        JsonNode data = json.at("/data");
        String nodeId = data.at("/nodeId").asText();
        if (DPUtil.empty(nodeId)) {
            return ApiUtil.result(1001, "节点信息异常", json);
        }
        RedisToken token = RedisToken.decode(DPUtil.stringify(data.at("/token")));
        if (null == token) {
            return ApiUtil.result(1002, "解析令牌失败", json);
        }
        RedisTask task = RedisTask.decode(DPUtil.stringify(data.at("/task")));
        if (null == task) {
            return ApiUtil.result(1003, "解析任务失败", json);
        }
        ZooJob job = zookeeper.job(token.getJobId());
        if (null == job) {
            return ApiUtil.result(1004, "获取作业信息失败", json);
        }
        channel.removeAck(token.getId()); // 确认
        channel.putToken(token.back(job)); // 归还令牌
        String rateId = job.getTemplate().at("/rateId").asText();
        if (!DPUtil.empty(rateId)) { // 恢复计数
            ZooRate rate = zookeeper.rate(rateId);
            if (null != rate) {
                channel.decrRateParallel(rate, token, job, task, nodeId, "");
            }
        }
        if (0 != json.at("/code").asInt(-1)) {
            channel.putTask(task.back(job)); // 归还任务
        }
        if (!channel.putStorage(data)) {
            return ApiUtil.result(1005, "写入存储队列失败", json);
        }
        return ApiUtil.result(0, null, null);
    }

    public void start() {
        if (this.isRunning) return;
        this.isRunning = true;
        new Thread(this).start();
    }

    public void stop() {
        this.isRunning = false;
    }

    public Map<String, Object> process(ObjectNode storage) {
        System.out.println(storage);
        return ApiUtil.result(0, null, storage);
    }

    public ObjectNode checkSynchronousQueue() {
        long interval = 300;
        long await = 30000;
        for (long time = interval; time <= await && isRunning; time += interval) {
            ObjectNode data;
            try { // 多次循环读，避免阻塞时间过长，导致关闭过程缓慢
                data = storageSynchronousQueue.poll(interval, TimeUnit.MILLISECONDS);
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
        while (this.isRunning) {
            ObjectNode data = channel.takeStorage();
            if (null == data) {
                synchronized (idleLock) {
                    try {
                        idleLock.wait(15000);
                    } catch (InterruptedException ignored) {}
                }
                continue;
            }
            if (!storageSynchronousQueue.offer(data)) { // 尝试将作业分配给等待队列
                Worker worker; // 入队失败，将作业直接交给新工作者执行
                try {
                    worker = this.workerPool.borrowObject();
                } catch (Exception e) {
                    if (!channel.backStorage(data)) {
                        log.warn("node service back storage failed: {}", data);
                    }
                    continue;
                }
                worker.call(this, data);
            }
        }
        runningCounter.decrementAndGet();
    }
}
