package com.iisquare.fs.web.cron.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.*;
import com.iisquare.fs.base.jpa.util.JPAUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.core.RedisKey;
import com.iisquare.fs.web.cron.core.StagePool;
import com.iisquare.fs.web.cron.core.ZooKeeperClient;
import com.iisquare.fs.web.cron.dao.FlowLogDao;
import com.iisquare.fs.web.cron.dao.FlowStageDao;
import com.iisquare.fs.web.cron.entity.FlowLog;
import com.iisquare.fs.web.cron.entity.FlowStage;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.Predicate;
import java.util.*;

@Service
public class FlowLogService extends ServiceBase implements DisposableBean {

    @Autowired
    private NodeService nodeService;
    @Autowired
    private FlowLogDao logDao;
    @Autowired
    private FlowStageDao stageDao;
    @Autowired
    private StringRedisTemplate redis;
    private StagePool pool = new StagePool();

    public Map<String, Object> onNotice(JsonNode notice) {
        String event = notice.at("/event").asText();
        if ("CancelAll".equals(event)) {
            int total = pool.interrupt(notice.at("/logId").asInt(0));
            return ApiUtil.result(0, null, total);
        }
        return ApiUtil.result(0, null, notice);
    }

    public Map<String, Object> tick(Map<?, ?> param) {
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
            return ApiUtil.result(1502, "主节点正在调度中", param);
        }
        Map<String, Object> result = dispatch(param);
        redis.delete(RedisKey.cronLogLock());
        return result;
    }

    public Map<String, Object> dispatch(Map<?, ?> param) {
        Map<Integer, FlowLog> logs = DPUtil.list2map(logDao.canRun(), Integer.class, "id");
        if (logs.size() == 0) {
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
            if (available.size() == 0) { // 无可执行任务，当前流程调度结束
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
        ObjectNode config = (ObjectNode) DPUtil.parseJSON(log.getData());
        ObjectNode flow = DPUtil.objectNode();
        flow.put("id", log.getId());
        flow.put("project", log.getProject());
        flow.put("name", log.getName());
        flow.put("concurrent", log.getConcurrent());
        flow.put("concurrency", log.getConcurrency());
        flow.put("failure", log.getFailure());
        flow.put("state", log.getState());
        config.replace("flow", flow);
        config.replace("state", DPUtil.toJSON(stateMap.get(log.getId()), ObjectNode.class));
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

    public Map<String, Object> search(Map<?, ?> param, Map<?, ?> config) {
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, 15);
        Sort sort = JPAUtil.sort(DPUtil.parseString(param.get("sort")), Arrays.asList("project", "name", "state", "createdTime", "updatedTime"));
        if (null == sort) sort = Sort.by(Sort.Order.desc("createdTime"));
        Page<FlowLog> data = logDao.findAll((Specification<FlowLog>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            String project = DPUtil.trim(DPUtil.parseString(param.get("project")));
            if(!DPUtil.empty(project)) {
                predicates.add(cb.like(root.get("project"), project));
            }
            String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
            if(!DPUtil.empty(name)) {
                predicates.add(cb.like(root.get("name"), name));
            }
            String state = DPUtil.trim(DPUtil.parseString(param.get("state")));
            if(!DPUtil.empty(state)) {
                predicates.add(cb.equal(root.get("state"), state));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, PageRequest.of(page - 1, pageSize, sort));
        List<FlowLog> rows = data.getContent();
        long time = System.currentTimeMillis();
        for (FlowLog row : rows) {
            format(row, time);
        }
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", data.getTotalElements());
        result.put("rows", rows);
        return result;
    }

    public FlowLog format(FlowLog row, long time) {
        long duration = (FlowLog.isFinished(row.getState()) ? row.getUpdatedTime() : time) - row.getCreatedTime();
        row.setDuration(duration);
        row.setDurationPretty(BeautifyUtil.duration(duration));
        return row;
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
        ObjectNode json = DPUtil.toJSON(format(flowLog, time), ObjectNode.class);
        List<FlowStage> stages = stageDao.findAllByLogId(flowLog.getId());
        for (FlowStage row : stages) {
            format(row, time);
        }
        json.replace("stages", DPUtil.toJSON(stages));
        Iterator<JsonNode> iterator = json.at("/stages").iterator();
        while (iterator.hasNext()) {
            ObjectNode node = (ObjectNode) iterator.next();
            JsonNode data = DPUtil.parseJSON(node.at("/data").asText());
            if (data == null || !data.isObject()) data = DPUtil.objectNode();
            node.replace("data", data);
        }
        return ApiUtil.result(0, null, json);
    }

    @Override
    public void destroy() throws Exception {
        pool.close();
        pool = null;
    }
}
