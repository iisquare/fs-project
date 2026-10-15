package com.iisquare.fs.web.bi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.bi.dao.JobDao;
import com.iisquare.fs.web.bi.entity.Cluster;
import com.iisquare.fs.web.bi.entity.Diagram;
import com.iisquare.fs.web.bi.entity.Job;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

@Service
public class JobService extends JPAServiceBase {

    @Autowired
    JobDao jobDao;
    @Autowired
    DefaultRbacService rbacService;

    @Override
    public Map<String, String> sorts() {
        Map<String, String> sorts = new LinkedHashMap<>();
        sorts.put("id", "desc");
        sorts.put("startTime", "desc");
        sorts.put("state", "asc");
        return sorts;
    }

    public Map<String, String> states() {
        Map<String, String> states = new LinkedHashMap<>();
        states.put(Job.STATE_SUBMITTING, "提交中");
        states.put(Job.STATE_RUNNING, "运行中");
        states.put(Job.STATE_FINISHED, "已完成");
        states.put(Job.STATE_FAILED, "已失败");
        states.put(Job.STATE_CANCELLED, "已取消");
        states.put(Job.STATE_UNKNOWN, "未知");
        return states;
    }

    public Map<String, String> triggers() {
        Map<String, String> triggers = new LinkedHashMap<>();
        triggers.put(Job.TRIGGER_MANUAL, "手动运行");
        triggers.put(Job.TRIGGER_SCHEDULE, "定时调度");
        triggers.put(Job.TRIGGER_DEBUG, "节点调试");
        triggers.put(Job.TRIGGER_RECOVER, "自动恢复");
        return triggers;
    }

    public Map<String, String> engines() {
        Map<String, String> engines = new LinkedHashMap<>();
        engines.put("spark", "Apache Spark");
        engines.put("flink", "Apache Flink");
        return engines;
    }

    public Map<String, String> models() {
        Map<String, String> models = new LinkedHashMap<>();
        models.put("batch", "批量处理");
        models.put("stream", "流式计算");
        return models;
    }

    public Job info(Integer id) {
        return info(jobDao, id);
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) {
        ObjectNode result = search(jobDao, param, (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!DPUtil.empty(param.get("diagramId"))) {
                predicates.add(cb.equal(root.get("diagramId"), DPUtil.parseInt(param.get("diagramId"))));
            }
            if (!DPUtil.empty(param.get("clusterId"))) {
                predicates.add(cb.equal(root.get("clusterId"), DPUtil.parseInt(param.get("clusterId"))));
            }
            if (!DPUtil.empty(param.get("engine"))) {
                predicates.add(cb.equal(root.get("engine"), DPUtil.parseString(param.get("engine"))));
            }
            if (!DPUtil.empty(param.get("model"))) {
                predicates.add(cb.equal(root.get("model"), DPUtil.parseString(param.get("model"))));
            }
            if (!DPUtil.empty(param.get("state"))) {
                predicates.add(cb.equal(root.get("state"), DPUtil.parseString(param.get("state"))));
            }
            if (!DPUtil.empty(param.get("triggerType"))) {
                predicates.add(cb.equal(root.get("triggerType"), DPUtil.parseString(param.get("triggerType"))));
            }
            if (!DPUtil.empty(param.get("name"))) {
                predicates.add(cb.like(root.get("name"), "%" + DPUtil.trim(DPUtil.parseString(param.get("name"))) + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, Sort.by(Sort.Order.desc("id")), sorts().keySet());
        JsonNode rows = format(ApiUtil.rows(result));
        if (!DPUtil.empty(args.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if (!DPUtil.empty(args.get("withStateText"))) {
            DPUtil.fillValues(rows, "state", "stateText", states());
            DPUtil.fillValues(rows, "triggerType", "triggerTypeText", triggers());
            DPUtil.fillValues(rows, "engine", "engineText", engines());
            DPUtil.fillValues(rows, "model", "modelText", models());
        }
        return result;
    }

    public JsonNode format(JsonNode rows) {
        for (JsonNode row : rows) {
            ObjectNode node = (ObjectNode) row;
            node.replace("result", DPUtil.parseJSON(node.at("/result").asText()));
            node.replace("outputs", DPUtil.parseJSON(node.at("/outputs").asText()));
        }
        return rows;
    }

    /**
     * 创建提交中的作业记录
     */
    public Job create(Diagram diagram, Cluster cluster, String triggerType, JsonNode data, JsonNode frame, int uid) {
        long time = System.currentTimeMillis();
        Job job = Job.builder()
                .diagramId(null == diagram ? null : diagram.getId())
                .clusterId(null == cluster ? null : cluster.getId())
                .name(null == diagram ? "未命名作业" : diagram.getName())
                .engine(null == diagram ? "" : diagram.getEngine())
                .model(null == diagram ? "" : diagram.getModel())
                .triggerType(triggerType)
                .state(Job.STATE_SUBMITTING)
                .retry(0)
                .autoRecover(null != diagram && null != diagram.getAutoRecover() && 1 == diagram.getAutoRecover() ? 1 : 0)
                .notified(0)
                .data(DPUtil.stringify(data))
                .content(DPUtil.stringify(frame))
                .startTime(time)
                .createdUid(uid)
                .createdTime(time)
                .updatedUid(uid)
                .updatedTime(time)
                .build();
        return jobDao.save(job);
    }

    /**
     * 提交完成，回填远程作业标识与运行状态
     */
    public Job submitted(Job job, String jobId, String state, JsonNode result) {
        job.setJobId(jobId);
        job.setState(DPUtil.empty(state) ? Job.STATE_RUNNING : state);
        job.setResult(DPUtil.stringify(result));
        job.setUpdatedTime(System.currentTimeMillis());
        return jobDao.save(job);
    }

    /**
     * 节点调试结果回填
     */
    public Job debugged(Job job, JsonNode outputs) {
        job.setState(DPUtil.empty(job.getState()) ? Job.STATE_FINISHED : job.getState());
        job.setOutputs(DPUtil.stringify(outputs));
        job.setEndTime(System.currentTimeMillis());
        job.setUpdatedTime(job.getEndTime());
        return jobDao.save(job);
    }

    public Job failed(Job job, String message) {
        long time = System.currentTimeMillis();
        job.setState(Job.STATE_FAILED);
        job.setAlarm(message);
        job.setEndTime(time);
        job.setUpdatedTime(time);
        return jobDao.save(job);
    }

    public Job state(Job job, String state, String alarm, String result) {
        long time = System.currentTimeMillis();
        job.setState(state);
        if (!DPUtil.empty(alarm)) job.setAlarm(alarm);
        if (!DPUtil.empty(result)) job.setResult(result);
        if (isTerminal(state)) job.setEndTime(time);
        job.setUpdatedTime(time);
        return jobDao.save(job);
    }

    public boolean isTerminal(String state) {
        return Job.STATE_FINISHED.equals(state) || Job.STATE_FAILED.equals(state)
                || Job.STATE_CANCELLED.equals(state);
    }

    /**
     * 将远程集群返回的状态映射为本地状态
     */
    public String mapState(String remote) {
        if (DPUtil.empty(remote)) return Job.STATE_UNKNOWN;
        switch (remote.toUpperCase()) {
            case "RUNNING":
            case "CREATED":
            case "INITIALIZING":
            case "RESTARTING":
            case "RECONCILING":
            case "SUBMITTED":
                return Job.STATE_RUNNING;
            case "FINISHED":
            case "SUCCEEDED":
                return Job.STATE_FINISHED;
            case "FAILED":
            case "ERROR":
                return Job.STATE_FAILED;
            case "CANCELED":
            case "CANCELLING":
            case "KILLED":
            case "SUSPENDED":
                return Job.STATE_CANCELLED;
            default:
                return Job.STATE_UNKNOWN;
        }
    }

    public List<Job> running() {
        return jobDao.findAllByStateIn(Arrays.asList(Job.STATE_SUBMITTING, Job.STATE_RUNNING));
    }

    public Job save(Job job) {
        job.setUpdatedTime(System.currentTimeMillis());
        return jobDao.save(job);
    }

    public int countRunning(Integer diagramId) {
        return jobDao.countByDiagramIdAndState(diagramId, Job.STATE_RUNNING)
                + jobDao.countByDiagramIdAndState(diagramId, Job.STATE_SUBMITTING);
    }

    public Map<String, Object> delete(List<Integer> ids, HttpServletRequest request) {
        if (!rbacService.hasPermit(request, "delete")) return ApiUtil.result(9403, null, null);
        if (null == ids || ids.isEmpty()) return ApiUtil.result(1001, "请选择待删除的作业", ids);
        jobDao.deleteAllByIdInBatch(ids);
        return ApiUtil.result(0, null, true);
    }

}
