package com.iisquare.fs.web.bi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.jpa.util.JPAUtil;
import com.iisquare.fs.base.web.util.RpcUtil;
import com.iisquare.fs.web.bi.dao.DiagramDao;
import com.iisquare.fs.web.bi.entity.Cluster;
import com.iisquare.fs.web.bi.entity.Diagram;
import com.iisquare.fs.web.bi.entity.Job;
import com.iisquare.fs.web.core.rpc.FlinkRpc;
import com.iisquare.fs.web.core.rpc.SparkRpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.LinkedHashMap;

/**
 * 作业运行状态监控服务。
 *
 * 周期性轮询运行中作业的远程状态，更新本地作业记录；作业失败时发送预警，并按配置自动重新提交。
 */
@Service
public class JobMonitorService {

    private static final Logger log = LoggerFactory.getLogger(JobMonitorService.class);

    @Autowired
    JobService jobService;
    @Autowired
    ClusterService clusterService;
    @Autowired
    DiagramDao diagramDao;
    @Autowired
    AlarmService alarmService;
    @Autowired
    DAGService dagService;
    @Autowired
    SparkRpc sparkRpc;
    @Autowired
    FlinkRpc flinkRpc;

    @Value("${fs.bi.monitor.enabled:true}")
    private boolean enabled;

    @Scheduled(fixedDelayString = "${fs.bi.monitor.interval:60000}", initialDelayString = "${fs.bi.monitor.initialDelay:30000}")
    public void monitor() {
        if (!enabled) return;
        for (Job job : jobService.running()) {
            try {
                poll(job);
            } catch (Exception e) {
                log.warn("[bi-job-monitor] 作业{}监控异常：{}", job.getId(), e.getMessage());
            }
        }
    }

    public void poll(Job job) {
        Cluster cluster = clusterService.info(job.getClusterId());
        ObjectNode content = dagService.clusterNode(cluster, job.getEngine());
        String remoteState = queryState(job.getEngine(), content, job.getJobId());
        String state = jobService.mapState(remoteState);
        if (Job.STATE_UNKNOWN.equals(state)) return; // 无法判定时保持现有状态，避免误报
        job.setResult(remoteState);
        if (state.equals(job.getState())) {
            jobService.save(job);
            return;
        }
        jobService.state(job, state, null, remoteState);
        if (Job.STATE_FAILED.equals(state)) {
            String message = String.format("作业[%s]运行失败，远程状态：%s", job.getName(), remoteState);
            ObjectNode alarm = alarmService.alarm(job, "JOB_FAILED", message);
            job.setAlarm(DPUtil.stringify(alarm));
            job.setNotified(1);
            jobService.save(job);
            autoRecover(job);
        }
    }

    /**
     * 自动恢复：按流程配置重新提交失败作业，超过最大重试次数后不再自动恢复
     */
    public Map<String, Object> autoRecover(Job job) {
        if (null == job.getAutoRecover() || 1 != job.getAutoRecover()) {
            return ApiUtil.result(1001, "未启用自动恢复", job.getId());
        }
        Diagram diagram = JPAUtil.findById(diagramDao, job.getDiagramId(), Diagram.class);
        if (null == diagram || null == diagram.getAutoRecover() || 1 != diagram.getAutoRecover()) {
            return ApiUtil.result(1002, "流程未启用自动恢复", job.getDiagramId());
        }
        int maxRetry = null == diagram.getMaxRetry() ? 0 : diagram.getMaxRetry();
        int retry = null == job.getRetry() ? 0 : job.getRetry();
        if (retry >= maxRetry) {
            alarmService.alarm(job, "JOB_RECOVER_EXHAUSTED",
                    String.format("作业[%s]自动恢复次数已达上限%d次，停止自动恢复", job.getName(), maxRetry));
            return ApiUtil.result(1003, "自动恢复次数已达上限", retry);
        }
        Map<String, Object> options = new LinkedHashMap<>();
        options.put("clusterId", job.getClusterId());
        JsonNode data = DPUtil.parseJSON(job.getData());
        if (null != data && !data.isNull()) options.put("data", DPUtil.toJSON(data, Map.class));
        Map<String, Object> result = dagService.run(job.getDiagramId(), options, null, Job.TRIGGER_RECOVER);
        Object newJobId = result.get("job");
        if (null != newJobId) {
            Job recover = jobService.info(DPUtil.parseInt(newJobId));
            if (null != recover) {
                recover.setRetry(retry + 1);
                recover.setAutoRecover(job.getAutoRecover());
                jobService.save(recover);
            }
        }
        return result;
    }

    private String queryState(String engine, ObjectNode content, String jobId) {
        Map<String, Object> result;
        if ("flink".equals(engine)) {
            result = RpcUtil.result(flinkRpc.post("/bi/job/status", DPUtil.buildMap("content", content, "jobId", jobId)));
            if (!ApiUtil.succeed(result)) return "UNKNOWN";
            JsonNode data = DPUtil.toJSON(ApiUtil.data(result, Object.class));
            return data.at("/state").asText("UNKNOWN");
        }
        result = RpcUtil.result(sparkRpc.post("/bi/job/status", DPUtil.buildMap("content", content, "jobId", jobId)));
        if (!ApiUtil.succeed(result)) return "UNKNOWN";
        JsonNode data = DPUtil.toJSON(ApiUtil.data(result, Object.class));
        String driverState = data.at("/driverState").asText("");
        if (DPUtil.empty(driverState)) {
            driverState = data.at("/state").asText("UNKNOWN");
        }
        return driverState;
    }

}
