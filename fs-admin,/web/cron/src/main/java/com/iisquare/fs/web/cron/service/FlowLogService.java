package com.iisquare.fs.web.cron.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.*;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.core.RedisKey;
import com.iisquare.fs.web.cron.core.StagePool;
import com.iisquare.fs.web.cron.core.ZooKeeperClient;
import com.iisquare.fs.web.cron.dao.FlowLogDao;
import com.iisquare.fs.web.cron.dao.FlowStageDao;
import com.iisquare.fs.web.cron.entity.FlowLog;
import com.iisquare.fs.web.cron.entity.FlowStage;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class FlowLogService extends JPAServiceBase implements InitializingBean, DisposableBean {

    @Autowired
    private NodeService nodeService;
    @Autowired
    private FlowLogDao logDao;
    @Autowired
    private FlowStageDao stageDao;
    @Autowired
    private StringRedisTemplate redis;
    private volatile long waitTimeout = 0; // 无可调度流程时，长时间等待
    private volatile StagePool pool = new StagePool();
    private final AtomicInteger atomic = new AtomicInteger(0); // 在状态修改并发冲突后，补偿调度一次
    @Autowired
    private FlowService flowService;
    @Autowired
    private FlowLogDao flowLogDao;

    @Override
    public void afterPropertiesSet() throws Exception {
        new Thread(() -> { // 启动时的首次触发，由NodeService.onApplicationEvent在准备完成后进行唤起
            while (null != pool) {
                if (atomic.getAndSet(0) < 1) {
                    synchronized (atomic) {
                        try {
                            atomic.wait(waitTimeout);
                        } catch (InterruptedException e) {
                            continue;
                        }
                    }
                }
                waitTimeout = 10000; // 调度流转过程中短时间等待
                tick(DPUtil.buildMap()); // 拉起待执行的任务
            }
        }).start();
    }

    @Override
    public void destroy() throws Exception {
        pool.close();
        pool = null;
        notify(DPUtil.buildMap(), false);
    }

    public void notify(Map<?, ?> param, boolean bForce) {
        if (bForce) {
            atomic.incrementAndGet();
        }
        synchronized (atomic) {
            atomic.notifyAll();
        }
    }

    public Map<String, Object> onNotice(JsonNode notice) {
        String event = notice.at("/event").asText();
        if ("CancelAll".equals(event)) {
            int total = pool.interrupt(notice.at("/logId").asInt(0));
            return ApiUtil.result(0, null, total);
        }
        return ApiUtil.result(0, null, notice);
    }

    public Map<String, Object> tick(Map<?, ?> param) {
        if (null == pool) {
            return ApiUtil.result(1500, "服务已关闭", param);
        }
        ZooKeeperClient zookeeper = nodeService.zookeeper();
        String nodeId = zookeeper.nodeId();
        String leaderId = zookeeper.leaderId();
        if (!nodeId.equals(leaderId)) {
            String url = String.format("http://%s/flowLog/tick", leaderId);
            JsonNode result = DPUtil.parseJSON(HttpUtil.post(url, DPUtil.stringify(param), null));
            if (null == result) {
                return ApiUtil.result(1501, "请求主节点失败", url);
            }
            return DPUtil.toJSON(result, Map.class);
        }
        if (!redis.opsForValue().setIfAbsent(RedisKey.cronLogLock(), "", RedisKey.TTL_CRON_LOG_LOCK)) {
            atomic.incrementAndGet();
            return ApiUtil.result(1502, "主节点正在调度中", param);
        }
        Map<String, Object> result = dispatch(param);
        redis.delete(RedisKey.cronLogLock());
        notify(param, false);
        return result;
    }

    public Map<String, Object> dispatch(Map<?, ?> param) {
        Map<Integer, FlowLog> logs = DPUtil.list2map(logDao.canRun(), Integer.class, "id");
        if (logs.size() == 0) {
            waitTimeout = 0;
            return ApiUtil.result(0, "暂无可调度流程", param);
        }
        Map<Integer, Map<String, FlowStage>> stages = DPUtil.list2mm(stageDao.findAllByLogId(
                DPUtil.values(logs.values(), Integer.class, "id")), Integer.class, "logId", String.class, "stageId");
        ObjectNode statistics = statistics();
        if (null == statistics) return ApiUtil.result(1503, "获取节点统计信息失败", param);
        Map<Integer, Set<String>> runMap = new LinkedHashMap<>(); // 正在执行
        Map<String, Integer> countMap = new LinkedHashMap<>(); // 节点统计
        Map<Integer, Set<String>> doneMap = new LinkedHashMap<>(); // 执行完成
        Map<Integer, Set<String>> availableMap = new LinkedHashMap<>(); // 待执行
        Map<Integer, Set<String>> canMap = new LinkedHashMap<>(); // 可调度
        Map<Integer, Map<String, String>> stateMap = new LinkedHashMap<>(); // 状态统计
        Iterator<JsonNode> iterator = statistics.iterator();
        while (iterator.hasNext()) { // 统计节点执行情况
            JsonNode state = iterator.next();
            String participant = state.at("/nodeId").asText();
            countMap.put(participant, state.at("/stages").size());
            Iterator<JsonNode> it = state.at("/stages").iterator();
            while (it.hasNext()) {
                JsonNode stage = it.next();
                int logId = stage.at("/logId").asInt();
                String stageId = stage.at("/stageId").asText();
                Set<String> run = runMap.computeIfAbsent(logId, k -> new HashSet<>());
                run.add(stageId);
            }
        }
        for (Map.Entry<Integer, Map<String, FlowStage>> entry : stages.entrySet()) { // 提取作业可执行状态
            Integer logId = entry.getKey();
            Set<String> done = new HashSet<>();
            Set<String> available = new HashSet<>();
            Map<String, String> state = new LinkedHashMap<>();
            doneMap.put(logId, done);
            availableMap.put(logId, available);
            stateMap.put(logId, state);
            int failedCount = 0; // 失败任务总数
            for (Map.Entry<String, FlowStage> et : entry.getValue().entrySet()) {
                FlowStage stage = et.getValue();
                state.put(stage.getStageId(), stage.getState());
                if (FlowStage.isFinished(stage.getState())) {
                    done.add(stage.getStageId());
                    if (FlowLog.isFailed(stage.getState())) failedCount++;
                } else {
                    available.add(stage.getStageId());
                }
            }
            if (failedCount == 0) continue;
            String failure = logs.get(logId).getFailure();
            if (!"FinishAllPossible".endsWith(failure)) {
                availableMap.put(logId, new LinkedHashSet<>()); // 清空可调度任务
                stageDao.terminal(logId, System.currentTimeMillis()); // 将未执行完成的任务设置为终止状态
            }
            if ("CancelAll".equals(failure)) {
                // 通知各节点终止当前流程的执行线程
                ObjectNode notice = DPUtil.objectNode();
                notice.put("subscribe", getClass().getSimpleName());
                notice.put("event", "CancelAll");
                notice.put("logId", logId);
                nodeService.zookeeper().notice(notice);
            }
        }
        for (Map.Entry<Integer, Set<String>> entry : availableMap.entrySet()) {
            Integer logId = entry.getKey();
            Set<String> available = entry.getValue();
            Set<String> can = canMap.computeIfAbsent(logId, k -> new HashSet<>());
            Set<String> run = runMap.computeIfAbsent(logId, k -> new HashSet<>());
            Set<String> done = doneMap.computeIfAbsent(logId, k -> new HashSet<>());
            for (String stageId : available) { // 提取可调度的作业
                FlowStage stage = stages.get(logId).get(stageId);
                String[] depend = DPUtil.explode(",", stage.getDepend());
                int finished = 0;
                for (String item : depend) {
                    if (done.contains(item)) finished++;
                }
                if (finished == depend.length && !run.contains(stageId)) {
                    can.add(stageId);
                }
            }
            FlowLog log = logs.get(logId);
            ObjectNode config = config(log, stateMap); // 传递给任务的配置参数
            int concurrent = log.getConcurrent(); // 剩余可并发执行任务数量
            for (String stageId : can) {
                if (concurrent-- <= 0) break;
                ArrayList<Map.Entry<String, Integer>> list = new ArrayList<>(countMap.entrySet());
                Collections.sort(list, Map.Entry.comparingByValue());
                String participant = list.get(0).getKey();
                countMap.put(participant, countMap.get(participant) + 1);
                String url = String.format("http://%s/flowLog/submit", participant);
                ObjectNode data = DPUtil.objectNode();
                data.replace("config", config);
                data.replace("stage", DPUtil.toJSON(stages.get(logId).get(stageId)));
                HttpUtil.post(url, DPUtil.stringify(data), HttpUtil.JSON_HEADERS);
            }
            if (available.isEmpty()) { // 无可执行任务，当前流程调度结束
                int total = stages.get(logId).size(); // 总任务数
                int finished = 0; // 执行完成的任务数量
                int succeed = 0; // 执行成功的任务数量
                for (Map.Entry<String, String> et : stateMap.get(logId).entrySet()) {
                    String state = et.getValue();
                    if (FlowStage.isFinished(state)) {
                        finished++;
                        if (FlowStage.isSucceed(state)) {
                            succeed++;
                        }
                    }
                }
                String state =  FlowLog.State.SUCCEED.name();
                if (finished < total) {
                    state = FlowLog.State.TERMINATED.name();
                } else if (succeed < total) {
                    state = FlowLog.State.FAILED.name();
                }
                logDao.finish(logId, state, System.currentTimeMillis());
            }
        }
        return ApiUtil.result(0, null, param);
    }

    public ObjectNode config(FlowLog log, Map<Integer, Map<String, String>> stateMap) {
        ObjectNode config = DPUtil.objectNode();
        if (!DPUtil.empty(log.getData())) {
            config = (ObjectNode) DPUtil.parseJSON(log.getData());
        }
        ObjectNode flow = DPUtil.objectNode();
        flow.put("id", log.getId());
        flow.put("flowId", log.getFlowId());
        flow.put("concurrent", log.getConcurrent());
        flow.put("concurrency", log.getConcurrency());
        flow.put("failure", log.getFailure());
        flow.put("state", log.getState());
        config.replace("flow", flow);
        config.replace("state", DPUtil.toJSON(stateMap.get(log.getId()), ObjectNode.class));
        flowService.fillInfo(DPUtil.toArrayNode(config), "flowId");
        return config;
    }

    public ObjectNode statistics() {
        ObjectNode statistics = DPUtil.objectNode();
        List<String> participants = nodeService.zookeeper().participants();
        for (String participant : participants) {
            String url = String.format("http://%s/flowLog/state", participant);
            Map<String, Object> result = DPUtil.toJSON(DPUtil.parseJSON(HttpUtil.get(url)), Map.class);
            if (null == result || ApiUtil.failed(result)) return null;
            statistics.replace(participant, DPUtil.toJSON(ApiUtil.data(result, Object.class)));
        }
        return statistics;
    }

    public Map<String, Object> submit(JsonNode stage, ObjectNode config) {
        return pool.submit(stage, config);
    }

    public ObjectNode state() {
        ObjectNode state = DPUtil.objectNode();
        state.put("nodeId", nodeService.zookeeper().nodeId());
        state.replace("stages", pool.stages());
        return state;
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> config) {
        ObjectNode result = search(flowLogDao, param, (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            int flowId = DPUtil.parseInt(param.get("flowId"));
            if (flowId > 0) predicates.add(cb.equal(root.get("flowId"), flowId));
            String state = DPUtil.trim(DPUtil.parseString(param.get("state")));
            if(!DPUtil.empty(state)) {
                predicates.add(cb.equal(root.get("state"), state));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, Sort.by(Sort.Order.desc("createdTime")), "id", "flowId", "state", "createdTime", "updatedTime");
        JsonNode rows = format(ApiUtil.rows(result));
        if (!DPUtil.empty(config.get("withFlowInfo"))) {
            flowService.fillInfo(rows, "flowId");
        }
        return result;
    }

    public JsonNode format(JsonNode rows) {
        long time = System.currentTimeMillis();
        for (JsonNode row : rows) {
            ObjectNode node = (ObjectNode) row;
            node.replace("content", DPUtil.parseJSON(node.at("/content").asText()));
            String state = node.at("/state").asText();
            long createdTime = node.at("/createdTime").asLong();
            long updatedTime = node.at("/updatedTime").asLong();
            long duration = (FlowLog.isFinished(state) ? updatedTime : time) - createdTime;
            node.put("duration", duration);
            node.put("durationPretty", BeautifyUtil.duration(duration));
        }
        return rows;
    }

    public FlowStage format(FlowStage row, long time) {
        long duration = 0L;
        if (!FlowStage.State.WAITING.name().equals(row.getState())) {
            if (FlowStage.State.RUNNING.name().equals(row.getState())) {
                duration = time - row.getRunTime();
            } else {
                duration = row.getUpdatedTime() - row.getRunTime();
            }
        }
        row.setDuration(duration);
        row.setDurationPretty(BeautifyUtil.duration(duration));
        return row;
    }

    public Map<String, Object> stages(Integer logId) {
        FlowLog flowLog = logDao.findById(logId).orElse(null);
        if (null == flowLog) {
            return ApiUtil.result(1404, "日志信息不存在", logId);
        }
        long time = System.currentTimeMillis();
        ObjectNode json = (ObjectNode) DPUtil.firstNode(format(
                flowService.fillInfo(DPUtil.toArrayNode(flowLog), "flowId")));
        List<FlowStage> stages = stageDao.findAllByLogId(flowLog.getId());
        for (FlowStage row : stages) {
            format(row, time);
        }
        json.replace("stages", DPUtil.toJSON(stages));
        Iterator<JsonNode> iterator = json.at("/stages").iterator();
        while (iterator.hasNext()) {
            ObjectNode node = (ObjectNode) iterator.next();
            node.put("skipped", DPUtil.parseBoolean(node.at("/skipped").asText()));
            JsonNode data = DPUtil.parseJSON(node.at("/data").asText());
            if (data == null || !data.isObject()) data = DPUtil.objectNode();
            node.replace("data", data);
        }
        return ApiUtil.result(0, null, json);
    }

}
