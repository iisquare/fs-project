package com.iisquare.fs.web.bi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.jpa.util.JPAUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.base.web.util.RpcUtil;
import com.iisquare.fs.web.bi.dao.DiagramDao;
import com.iisquare.fs.web.bi.entity.Cluster;
import com.iisquare.fs.web.bi.entity.Diagram;
import com.iisquare.fs.web.bi.entity.Job;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.core.rpc.FlinkRpc;
import com.iisquare.fs.web.core.rpc.SparkRpc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Service
public class DAGService extends ServiceBase {

    @Autowired
    DiagramDao diagramDao;
    @Autowired
    ClusterService clusterService;
    @Autowired
    JobService jobService;
    @Autowired
    AlarmService alarmService;
    @Autowired
    DefaultRbacService rbacService;
    @Autowired
    SparkRpc sparkRpc;
    @Autowired
    FlinkRpc flinkRpc;

    /**
     * 兼容旧接口：按流程默认集群提交
     */
    public Map<String, Object> run(Integer id) {
        return run(id, DPUtil.buildMap(), null, Job.TRIGGER_MANUAL);
    }

    /**
     * 提交流批计算作业：根据流程配置选择计算引擎与目标集群
     */
    public Map<String, Object> run(Integer id, Map<?, ?> param, HttpServletRequest request, String triggerType) {
        Diagram diagram = JPAUtil.findById(diagramDao, id, Diagram.class);
        if (null == diagram || 1 != diagram.getStatus()) {
            return ApiUtil.result(404, "编排流程不存在或已禁用", id);
        }
        ObjectNode frame = diagram(id, DPUtil.objectNode());
        if (null == frame) return ApiUtil.result(404, "编排流程不存在或已禁用", id);
        int clusterId = DPUtil.parseInt(param.get("clusterId"));
        if (clusterId < 1) clusterId = DPUtil.parseInt(diagram.getClusterId());
        Cluster cluster = clusterService.info(clusterId);
        frame.set("cluster", clusterNode(cluster, diagram.getEngine()));
        frame.set("data", param.containsKey("data") ? DPUtil.toJSON(param.get("data")) : DPUtil.objectNode());
        if (param.containsKey("diagramUrl")) frame.put("diagramUrl", DPUtil.parseString(param.get("diagramUrl")));
        int uid = null == request ? 0 : rbacUid(request);
        Job job = jobService.create(diagram, cluster, triggerType, frame.at("/data"), frame, uid);
        return submit(job, diagram, frame, triggerType);
    }

    /**
     * 指定节点调试：仅执行至目标节点，返回各节点输出结果
     */
    public Map<String, Object> debug(Integer id, Map<?, ?> param, HttpServletRequest request) {
        Diagram diagram = JPAUtil.findById(diagramDao, id, Diagram.class);
        if (null == diagram || 1 != diagram.getStatus()) {
            return ApiUtil.result(404, "编排流程不存在或已禁用", id);
        }
        String nodeId = DPUtil.trim(DPUtil.parseString(param.get("nodeId")));
        if (DPUtil.empty(nodeId)) return ApiUtil.result(1001, "调试节点不能为空", nodeId);
        ObjectNode frame = diagram(id, DPUtil.objectNode());
        if (null == frame) return ApiUtil.result(404, "编排流程不存在或已禁用", id);
        frame.set("cluster", clusterNode(null, diagram.getEngine()));
        frame.put("debug", nodeId);
        frame.set("data", param.containsKey("data") ? DPUtil.toJSON(param.get("data")) : DPUtil.objectNode());
        int uid = null == request ? 0 : rbacUid(request);
        Job job = jobService.create(diagram, null, Job.TRIGGER_DEBUG, frame.at("/data"), frame, uid);
        Map<String, Object> result = submit(diagram.getEngine(), frame);
        if (ApiUtil.succeed(result)) {
            JsonNode data = DPUtil.toJSON(ApiUtil.data(result, Object.class));
            jobService.debugged(job, data.at("/outputs"));
        } else {
            jobService.failed(job, ApiUtil.message(result));
            alarmService.alarm(job, Job.TRIGGER_DEBUG, ApiUtil.message(result));
        }
        result.put("job", job.getId());
        return result;
    }

    private Map<String, Object> submit(Job job, Diagram diagram, ObjectNode frame, String triggerType) {
        Map<String, Object> result = submit(diagram.getEngine(), frame);
        if (!ApiUtil.succeed(result)) {
            jobService.failed(job, ApiUtil.message(result));
            alarmService.alarm(job, "SUBMIT_FAILED", ApiUtil.message(result));
            result.put("job", job.getId());
            return result;
        }
        JsonNode data = DPUtil.toJSON(ApiUtil.data(result, Object.class));
        String remoteId = remoteJobId(data);
        // 远程提交与流式计算均为异步执行，本地批处理同步完成后即为终态
        boolean asynchronous = "remote".equals(frame.at("/cluster/mode").asText(""))
                || "stream".equals(diagram.getModel());
        String state = asynchronous ? Job.STATE_RUNNING : Job.STATE_FINISHED;
        jobService.submitted(job, remoteId, state, data);
        result.put("job", job.getId());
        result.put("state", state);
        return result;
    }

    /**
     * 从远程提交返回结果中提取作业标识
     */
    public String remoteJobId(JsonNode data) {
        if (null == data || data.isNull()) return "";
        if (data.isTextual()) return data.asText();
        for (String field : new String[]{"JobId", "jobid", "jobId", "submissionId"}) {
            if (data.has(field) && !data.get(field).isNull()) return data.get(field).asText();
        }
        // 流式作业可能返回多个流查询，取首个查询的标识
        JsonNode queries = data.at("/queries");
        if (queries.isArray() && queries.size() > 0) {
            for (String field : new String[]{"submissionId", "jobId", "id"}) {
                if (queries.get(0).has(field) && !queries.get(0).get(field).isNull()) {
                    return queries.get(0).get(field).asText();
                }
            }
        }
        return "";
    }

    /**
     * 构造传递给计算引擎的集群配置节点
     */
    public ObjectNode clusterNode(Cluster cluster, String engine) {
        ObjectNode node = DPUtil.objectNode();
        if (null == cluster || !"remote".equals(cluster.getMode())) {
            node.put("mode", "local");
            return node;
        }
        node.put("mode", "remote");
        JsonNode content = DPUtil.parseJSON(cluster.getContent());
        if (content.isObject()) node.setAll((ObjectNode) content);
        if (DPUtil.empty(node.at("/entryClass").asText(""))) {
            node.put("entryClass", "flink".equals(engine)
                    ? "com.iisquare.fs.app.flink.FlinkApplication"
                    : "com.iisquare.fs.app.spark.SparkApplication");
        }
        return node;
    }

    private Map<String, Object> submit(String engine, ObjectNode frame) {
        switch (engine) {
            case "flink":
                return RpcUtil.result(flinkRpc.post("/bi/dag", frame));
            case "spark":
                return RpcUtil.result(sparkRpc.post("/bi/dag", frame));
            default:
                return ApiUtil.result(1500, "计算引擎异常", engine);
        }
    }

    /**
     * 取消正在运行的作业
     */
    public Map<String, Object> cancel(Integer jobId) {
        Job job = jobService.info(jobId);
        if (null == job) return ApiUtil.result(404, "作业记录不存在", jobId);
        if (jobService.isTerminal(job.getState())) return ApiUtil.result(1002, "作业已结束，无需取消", job.getState());
        Cluster cluster = clusterService.info(job.getClusterId());
        ObjectNode content = clusterNode(cluster, job.getEngine());
        Map<String, Object> result;
        if ("flink".equals(job.getEngine())) {
            result = RpcUtil.result(flinkRpc.post("/bi/job/cancel",
                    DPUtil.buildMap("content", content, "jobId", job.getJobId())));
        } else {
            result = RpcUtil.result(sparkRpc.post("/bi/job/cancel",
                    DPUtil.buildMap("content", content, "jobId", job.getJobId())));
        }
        if (ApiUtil.succeed(result)) {
            jobService.state(job, Job.STATE_CANCELLED, null, DPUtil.stringify(result.get("data")));
        }
        return result;
    }

    /**
     * 重新提交流程，用于人工重跑与自动恢复
     */
    public Map<String, Object> restart(Integer jobId, Map<?, ?> param, HttpServletRequest request, String triggerType) {
        Job job = jobService.info(jobId);
        if (null == job) return ApiUtil.result(404, "作业记录不存在", jobId);
        Map<String, Object> options = new HashMap<>();
        for (Map.Entry<?, ?> entry : param.entrySet()) {
            options.put(DPUtil.parseString(entry.getKey()), entry.getValue());
        }
        if (DPUtil.parseInt(options.get("clusterId")) < 1) options.put("clusterId", job.getClusterId());
        return run(job.getDiagramId(), options, request, triggerType);
    }

    private int rbacUid(HttpServletRequest request) {
        return null == request ? 0 : rbacService.uid(request);
    }

    public ObjectNode diagram(Integer id, ObjectNode cached) {
        if(null == id || id < 1) return null;
        String sid = String.valueOf(id);
        if (null != cached && cached.has(sid)) return (ObjectNode) cached.get(sid);
        Diagram diagram = JPAUtil.findById(diagramDao, id, Diagram.class);
        if (null == diagram || 1 != diagram.getStatus()) {
            if (null != cached) cached.replace(sid, null);
            return null;
        }
        ObjectNode result = DPUtil.objectNode();
        result.put("id", diagram.getId()).put("name", diagram.getName());
        result.put("engine", diagram.getEngine()).put("model", diagram.getModel());
        JsonNode content = DPUtil.parseJSON(diagram.getContent());
        if (null == content || !content.isObject()) content = DPUtil.objectNode();
        ArrayNode nodes = result.putArray("nodes");
        ArrayNode edges = result.putArray("edges");
        List<String> exportIds = new ArrayList<>();
        ObjectNode importIds = DPUtil.objectNode();
        ArrayNode jars = DPUtil.arrayNode();
        ArrayNode exports = DPUtil.arrayNode();
        Iterator<JsonNode> iterator = content.at("/cells").iterator();
        while (iterator.hasNext()) {
            JsonNode json = iterator.next();
            String shape = json.at("/shape").asText("");
            if ("flow-edge".equals(shape)) {
                String source = "dag_" + diagram.getId() + "_" + json.at("/source/cell").asText();
                String target = "dag_" + diagram.getId() + "_" + json.at("/target/cell").asText();
                if (exportIds.contains(target)) {
                    exports.add(target);
                    continue;
                }
                if (importIds.has(source)) {
                    Iterator<JsonNode> it = importIds.get(source).iterator();
                    while (it.hasNext()) {
                        edges.addObject().put("source", it.next().asText()).put("target", target);
                    }
                    continue;
                }
                edges.addObject().put("source", source).put("target", target);
                continue;
            }
            String nodeId = "dag_" + diagram.getId() + "_" + json.at("/id").asText();
            String type = json.at("/data/type").asText();
            JsonNode options = json.at("/data/options");
            if (!options.isObject()) options = DPUtil.objectNode();
            if ("GroupLayout".equals(type)) continue;
            if ("ExportConfig".equals(type)) {
                exportIds.add(nodeId);
                continue;
            }
            if ("ImportConfig".equals(type)) {
                ObjectNode sub = diagram(DPUtil.parseInt(options.at("/id").asText()), cached);
                importIds.replace(nodeId, null == sub ? DPUtil.arrayNode() : sub.at("/exports"));
                nodes.addAll((ArrayNode) sub.at("/nodes"));
                jars.addAll((ArrayNode) sub.at("/jars"));
                continue;
            }
            if ("ScriptTransform".equals(type)) {
                String jar = options.at("/jarURI").asText();
                if (!DPUtil.empty(jar)) jars.add(jar);
            }
            ObjectNode node = nodes.addObject();
            node.put("id", nodeId);
            node.put("type", type);
            String name = json.at("/data/name").asText();
            if (DPUtil.empty(name)) {
                name = String.format("data_%d_node_%d", diagram.getId(), json.at("/data/index").asInt());
            }
            node.put("name", name);
            node.put("parallelism", json.at("/data/parallelism").asInt());
            String parent = json.at("/parent").asText();
            node.put("parent", DPUtil.empty(parent) ? "" : ("dag_" + diagram.getId() + "_" + parent));
            if (type.endsWith("Source") || type.endsWith("Transform") || type.endsWith("Sink")) {
                node.put("kvConfigPrefix", json.at("/data/kvConfigPrefix").asText());
            }
            node.replace("options", options);
        }
        result.replace("exports", exports);
        result.replace("jars", jars);
        if (null != cached) cached.replace(sid, result);
        return result;
    }

}
