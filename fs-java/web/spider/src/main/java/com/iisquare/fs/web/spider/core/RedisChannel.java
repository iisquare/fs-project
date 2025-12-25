package com.iisquare.fs.web.spider.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.spider.service.NodeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RedisChannel {

    final String keyPrefix;
    final NodeService nodeService;
    private static final DefaultRedisScript<List> luaTaskTop;
    private static final DefaultRedisScript<Long> luaTaskPut;
    private static final DefaultRedisScript<List> luaTaskTake;

    static {
        luaTaskTop = new DefaultRedisScript<>();
        luaTaskTop.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/task-top.lua")));
        luaTaskTop.setResultType(List.class);
        luaTaskPut = new DefaultRedisScript<>();
        luaTaskPut.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/task-put.lua")));
        luaTaskPut.setResultType(Long.class);
        luaTaskTake = new DefaultRedisScript<>();
        luaTaskTake.setScriptSource(new ResourceScriptSource(new ClassPathResource("/lua/task-take.lua")));
        luaTaskTake.setResultType(List.class);
    }

    public RedisChannel(String keyPrefix, NodeService nodeService) {
        this.keyPrefix = keyPrefix;
        this.nodeService = nodeService;
    }

    public StringRedisTemplate redis() {
        return this.nodeService.redis();
    }

    public String keyToken() {
        return keyPrefix + "channel:token";
    }

    public long sizeToken() {
        Long result = redis().opsForZSet().zCard(keyToken());
        return result == null ? -1 : result;
    }

    public boolean clearToken() {
        Boolean result = redis().delete(keyToken());
        return result != null && result;
    }

    /**
     * 查看最近一次Token令牌
     */
    public RedisToken topToken() {
        Set<ZSetOperations.TypedTuple<String>> tuples = redis().opsForZSet().rangeWithScores(keyToken(), 0, 0);
        if (null == tuples || tuples.size() != 1) return null;
        ZSetOperations.TypedTuple<String> item = tuples.iterator().next();
        return RedisToken.decode(item.getValue());
    }

    /**
     * 获取Token令牌
     */
    public RedisToken takeToken() {
        Set<String> items = redis().opsForZSet().rangeByScore(keyToken(), 0, System.currentTimeMillis(), 0, 1);
        if (null == items || items.size() != 1) return null;
        String item = items.iterator().next();
        Long result = redis().opsForZSet().remove(keyToken(), item);
        if (null == result || result != 1) return null;
        return RedisToken.decode(item);
    }

    /**
     * 放置Token令牌
     */
    public boolean putToken(RedisToken token) {
        Boolean result = redis().opsForZSet().add(keyToken(), RedisToken.encode(token), token.score);
        return result != null && result;
    }

    /**
     * 采用zset+hash组合的方式存放任务，便于对执行中的连接进行去重
     * keyTaskRank用于存储zset链接的排序（url->score）
     * keyTaskDetail用于存储hash链接的任务明细（url->json）
     */
    public String keyTaskRank(String jobId) {
        return keyPrefix + "task:rank-" + jobId;
    }

    public String keyTaskDetail(String jobId) {
        return keyPrefix + "task:detail-" + jobId;
    }

    public long sizeTaskRank(String jobId) {
        Long result = redis().opsForZSet().zCard(keyTaskRank(jobId));
        return result == null ? -1 : result;
    }

    public long sizeTaskDetail(String jobId) {
        return redis().opsForHash().size(keyTaskDetail(jobId));
    }

    public boolean clearTask(String jobId) {
        String keyTaskRank = keyTaskRank(jobId);
        String keyTaskDetail = keyTaskDetail(jobId);
        Long result = redis().delete(Arrays.asList(keyTaskRank, keyTaskDetail));
        return result != null;
    }

    /**
     * 查看最近一次任务
     */
    public RedisTask topTask(String jobId) {
        String keyTaskRank = keyTaskRank(jobId);
        String keyTaskDetail = keyTaskDetail(jobId);
        List<Object> objects = redis().execute(luaTaskTop, Arrays.asList(keyTaskRank, keyTaskDetail));
        if (null == objects || objects.size() != 3) return null;
        return RedisTask.decode(DPUtil.parseString(objects.get(1)));
    }

    /**
     * 获取任务
     */
    public RedisTask takeTask(String jobId) {
        String keyTaskRank = keyTaskRank(jobId);
        String keyTaskDetail = keyTaskDetail(jobId);
        long score = System.currentTimeMillis();
        List<Object> objects = redis().execute(
                luaTaskTake, Arrays.asList(keyTaskRank, keyTaskDetail), String.valueOf(score));
        if (null == objects || objects.size() != 3) return null;
        return RedisTask.decode(DPUtil.parseString(objects.get(1)));
    }

    /**
     * 放置任务
     */
    public boolean putTask(RedisTask task) {
        String keyTaskRank = keyTaskRank(task.getJobId());
        String keyTaskDetail = keyTaskDetail(task.getJobId());
        Long result = redis().execute(
                luaTaskPut, Arrays.asList(keyTaskRank, keyTaskDetail),
                task.url, RedisTask.encode(task), String.valueOf(task.score));
        return null != result && 1 == result;
    }

    public String keyRate(String prefix, ZooRate rate, ZooJob job, RedisTask task, String nodeId, String proxyIP) {
        String key = keyPrefix + "rate:" + prefix + "-" + rate.getId();
        if (rate.perJob) key += "-" + job.getId();
        if (rate.perDomain) key += "-" + task.domain();
        if (rate.perNode) key += "-" + nodeId;
        if (rate.perProxy) key += "-" + proxyIP;
        return key;
    }

    public String keyRateParallel(ZooRate rate, ZooJob job, RedisTask task, String nodeId, String proxyIP) {
        return keyRate("parallel", rate, job, task, nodeId, proxyIP);
    }

    public String keyRateConcurrent(ZooRate rate, ZooJob job, RedisTask task, String nodeId, String proxyIP) {
        return keyRate("concurrent", rate, job, task, nodeId, proxyIP);
    }

    /**
     * 检查请求频率并发限制
     * Redis常用并发限制有两种方式：
     * 1. 按并发间隔生成key = time % interval，
     *    优点是到达指定间隔key直接失效，可以快速放行下一个间隔的请求，
     *    缺点是请求可能分布在每个间隔的后半段，导致间隔内并发数量过低；
     * 2. 在key过期后，采用当前时间生成新的key
     *    优点是并发限制比较均衡，实际接收请求后才生成并发间隔，
     *    缺点需要获取key的实际过期时间，以判定下一间隔开始放行请求的时间；
     * 两种方式共性的缺点是请求可能会集中在上一个间隔的结束和下一个间隔的开始，导致局部并发超限。
     */
    public boolean checkRateConcurrent(ZooRate rate, RedisToken token, ZooJob job, RedisTask task, String nodeId, String proxyIP) {
        if (rate.getConcurrent() < 1) return true;
        String keyRate = keyRateConcurrent(rate, job, task, nodeId, proxyIP);
        Long increment = redis().opsForValue().increment(keyRate);
        if (null == increment) return false;
        if (increment <= 1) { // 初始创建
            redis().expire(keyRate, rate.getInterval(), TimeUnit.MILLISECONDS);
        }
        if (increment <= rate.getConcurrent()) return true;
        if (rate.haltToken) {
            Long expire = redis().getExpire(keyRate, TimeUnit.MILLISECONDS);
            if (null != expire) {
                token.halt(expire);
            }
        }
        if (rate.haltTask) {
            task.halt(job.halt());
        }
        return false;
    }

    /**
     * 增加请求频率并行度计数
     */
    public boolean incrRateParallel(ZooRate rate, RedisToken token, ZooJob job, RedisTask task, String nodeId, String proxyIP) {
        if (rate.getParallel() < 1) return true;
        String keyRate = keyRateParallel(rate, job, task, nodeId, proxyIP);
        Long increment = redis().opsForValue().increment(keyRate);
        if (null == increment) return false;
        if (increment <= 1) { // 防止任务丢失导致计数异常
            redis().expire(keyRate, 15, TimeUnit.MINUTES);
        }
        if (increment <= rate.getParallel()) return true;
        redis().opsForValue().decrement(keyRate);
        int halt = job.halt();
        if (rate.haltToken) token.halt(halt);
        if (rate.haltTask) task.halt(halt);
        return false;
    }

    public boolean decrRateParallel(ZooRate rate, RedisToken token, ZooJob job, RedisTask task, String nodeId, String proxyIP) {
        if (rate.getParallel() < 1) return true;
        String keyRate = keyRateParallel(rate, job, task, nodeId, proxyIP);
        Long decrement = redis().opsForValue().decrement(keyRate);
        return null != decrement && decrement >= 0;
    }

    public String keyAck() {
        return keyPrefix + "channel:ack";
    }

    public boolean putAck(String key, ObjectNode data) {
        redis().opsForHash().put(keyAck(), key, data.toString());
        return true;
    }

    public boolean removeAck(String key) {
        Long result = redis().opsForHash().delete(keyAck(), key);
        return result == 1;
    }

    /**
     * 队列从左到右排列，左进右出，第一个在左边为最新入队，最后一个在右边为最先消费
     */
    public String keyStorage() {
        return keyPrefix + "channel:storage";
    }

    public long sizeStorage() {
        Long result = redis().opsForList().size(keyStorage());
        return result == null ? -1 : result;
    }

    public JsonNode lastStorage() {
        String last = redis().opsForList().getLast(keyStorage());
        return DPUtil.parseJSON(last);
    }

    public JsonNode firstStorage() {
        String first = redis().opsForList().getFirst(keyStorage());
        return DPUtil.parseJSON(first);
    }

    public ObjectNode takeStorage() {
        String pop = redis().opsForList().rightPop(keyStorage());
        return (ObjectNode) DPUtil.parseJSON(pop);
    }

    public boolean putStorage(JsonNode data) {
        Long result = redis().opsForList().leftPush(keyStorage(), data.toString());
        return null != result;
    }

    public boolean backStorage(JsonNode data) {
        Long result = redis().opsForList().rightPush(keyStorage(), data.toString());
        return null != result;
    }

}
