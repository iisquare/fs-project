package com.iisquare.fs.web.cron.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.web.cron.CronApplication;
import com.iisquare.fs.web.cron.dao.FlowDao;
import com.iisquare.fs.web.cron.dao.FlowLogDao;
import com.iisquare.fs.web.cron.dao.FlowStageDao;
import com.iisquare.fs.web.cron.entity.Flow;
import com.iisquare.fs.web.cron.entity.FlowLog;
import com.iisquare.fs.web.cron.entity.FlowStage;
import com.iisquare.fs.web.cron.service.FlowLogService;
import com.iisquare.fs.web.cron.service.FlowService;
import org.quartz.*;

import java.util.*;

public class DiagramJob implements Job {

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        long time = System.currentTimeMillis();
        Trigger trigger = context.getTrigger();
        JobDetail detail = context.getJobDetail();
        FlowDao flowDao = CronApplication.context.getBean(FlowDao.class);
        FlowLogDao flowLogDao = CronApplication.context.getBean(FlowLogDao.class);
        FlowStageDao flowStageDao = CronApplication.context.getBean(FlowStageDao.class);
        FlowService flowService = CronApplication.context.getBean(FlowService.class);
        FlowLogService logService = CronApplication.context.getBean(FlowLogService.class);
        // 读取流程配置
        JobKey jobKey = detail.getKey();
        if (!flowService.group().equals(jobKey.getGroup())) {
            return;
        }
        Flow flow = flowDao.findById(DPUtil.parseInt(jobKey.getName())).orElse(null);
        if (null == flow || 1 != flow.getStatus()) {
            flowLogDao.save(FlowLog.missing(detail.getKey(), time));
            return;
        }
        // 解析流程变量
        JsonNode data = DPUtil.parseJSON(flow.getData());
        if (null != data || !data.isObject()) data = DPUtil.objectNode();
        ((ObjectNode) data).setAll(DPUtil.toJSON(detail.getJobDataMap(), ObjectNode.class));
        ((ObjectNode) data).setAll(DPUtil.toJSON(trigger.getJobDataMap(), ObjectNode.class));
        // 生成调度日志
        JsonNode json = DPUtil.parseJSON(flow.getContent());
        if (null == json) json = DPUtil.objectNode();
        FlowLog.FlowLogBuilder logBuilder = FlowLog.builder();
        logBuilder.flowId(flow.getId());
        logBuilder.concurrent(Math.max(1, flow.getConcurrent()));
        logBuilder.concurrency(flow.getConcurrency());
        logBuilder.failure(flow.getFailure());
        logBuilder.data(flow.getData()).content(flow.getContent());
        logBuilder.state(FlowLog.State.RUNNING.name()).createdTime(time).updatedTime(time);
        if ("SkipExecution".equals(flow.getConcurrency())) {
            int count = flowLogDao.countByState(flow.getId(), FlowLog.State.RUNNING.name());
            if (count > 0) {
                logBuilder.state(FlowLog.State.SKIPPED.name());
                ((ObjectNode) json).putArray("cells"); // 清空执行节点
            }
        }
        FlowLog flowLog = flowLogDao.save(logBuilder.build());
        // 解析流程节点
        ObjectNode cells = DPUtil.json2object(json.at("/cells"), "id");
        Iterator<JsonNode> iterator = json.at("/cells").iterator();
        while (iterator.hasNext()) {
            ObjectNode node = (ObjectNode) iterator.next();
            String shape = node.at("/shape").asText();
            if ("flow-edge".equals(shape)) { // 连线依赖
                String sourceId = node.at("/source/cell").asText();
                String targetId = node.at("/target/cell").asText();
                if (!cells.has(targetId)) continue;
                ObjectNode target = (ObjectNode) cells.get(targetId);
                ArrayNode depend = target.has("depend") ? (ArrayNode) target.get("depend") : target.putArray("depend");
                depend.add(sourceId);
                continue;
            }
            // 更改所属节点
            if (!node.has("depend")) node.putArray("depend");
            node.put("parent", parent(cells, node.at("/parent").asText("")));
        }
        skipped(cells, "", false); // 跳过执行向下传递
        iterator = json.at("/cells").iterator();
        while (iterator.hasNext()) {
            JsonNode node = iterator.next();
            String id = node.at("/id").asText();
            String shape = node.at("/shape").asText();
            if ("flow-edge".equals(shape) || "flow-group".equals(shape)) { // 移除非执行节点
                cells.remove(id);
                continue;
            }
            String parent = node.at("/parent").asText();
            if (!DPUtil.empty(parent)) { // 将上级作为依赖节点
                ((ArrayNode) node.get("depend")).add(parent);
            }
        }
        changeSubProcessDepend(cells); // 更改子流程被依赖关系
        // 构建执行阶段
        iterator = cells.iterator();
        List<FlowStage> stages = new ArrayList<>();
        while (iterator.hasNext()) {
            JsonNode node = iterator.next();
            FlowStage.FlowStageBuilder stageBuilder = FlowStage.builder();
            stageBuilder.logId(flowLog.getId()).stageId(node.at("/id").asText());
            stageBuilder.parentId(node.at("/parent").asText(""));
            stageBuilder.depend(DPUtil.implode(",", DPUtil.toJSON(node.at("/depend"), List.class)));
            stageBuilder.type(node.at("/data/type").asText(""));
            stageBuilder.name(node.at("/data/name").asText(""));
            stageBuilder.skipped(node.at("/data/skipped").asBoolean(false) ? 1 : 0);
            stageBuilder.data(DPUtil.stringify(node.at("/data"))).content("");
            stageBuilder.state(FlowStage.State.WAITING.name()).createdTime(time).runTime(0L).updatedTime(time);
            stages.add(stageBuilder.build());
        }
        if (stages.size() > 0) {
            flowStageDao.saveAll(stages);
        }
        // 请确保tick方法在事务提交完成后被调用，若未读取到可调度流程，守护线程将持续等待
        // 参考：TransactionSynchronizationManager.registerSynchronization()
        logService.tick(DPUtil.buildMap("logId", flowLog.getId()));
    }

    public boolean changeSubProcessDepend(ObjectNode cells) {
        Map<String, Set<String>> levels = new LinkedHashMap<>(); // 层级关系：子流程 -> [流程内的子节点]
        Map<String, Set<String>> depends = new LinkedHashMap<>(); // 被依赖关系：被依赖节点 -> [依赖节点]
        Iterator<JsonNode> iterator = cells.iterator();
        while (iterator.hasNext()) {
            JsonNode node = iterator.next();
            String id = node.at("/id").asText();
            String parent = node.at("/parent").asText();
            if (!DPUtil.empty(parent)) {
                Set<String> level = levels.computeIfAbsent(parent, k -> new HashSet<>());
                level.add(id);
            }
            Iterator<JsonNode> it = node.at("/depend").iterator();
            while (it.hasNext()) {
                String target = it.next().asText();
                Set<String> depend = depends.computeIfAbsent(target, k -> new HashSet<>());
                depend.add(id);
            }
        }
        Map<String, Set<String>> fanout = new LinkedHashMap<>(); // 子流程中出度为零的节点：子流程 -> [扇出节点]
        for (Map.Entry<String, Set<String>> entry : levels.entrySet()) {
            String parent = entry.getKey();
            for (String id : entry.getValue()) {
                if (depends.containsKey(id)) continue; // 非扇出节点
                Set<String> out = fanout.computeIfAbsent(parent, k -> new HashSet<>());
                out.add(id);
            }
        }
        for (Map.Entry<String, Set<String>> entry : levels.entrySet()) { // 处理子流程的被依赖关系
            String parent = entry.getKey();
            if (!depends.containsKey(parent)) continue; // 当前子流程未被依赖
            if (!fanout.containsKey(parent)) continue;; // 当前子流程无扇出节点
            // 若子流程内存在循环依赖，调度将终止，但目标节点可能会调用
            for (String id : depends.get(parent)) {
                JsonNode node = cells.get(id);
                if (parent.equals(node.at("/parent").asText())) continue; // 忽略子流程内的兄弟节点
                ArrayNode depend = (ArrayNode) node.at("/depend");
                for (String d : fanout.get(parent)) { // 将扇出节点作为目标节点的依赖
                    depend.add(d);
                }
            }
        }
        return true;
    }

    public void skipped(ObjectNode cells, String parent, boolean skipped) {
        Iterator<JsonNode> iterator = cells.iterator();
        while (iterator.hasNext()) {
            ObjectNode node = (ObjectNode) iterator.next();
            String shape = node.at("/shape").asText();
            if ("flow-edge".equals(shape)) continue;
            if (!node.at("/parent").asText("").equals(parent)) continue;
            node.put("skipped", skipped || node.at("/skipped").asBoolean(false));
            if ("flow-subprocess".equals(shape) || "flow-group".equals(shape)) {
                skipped(cells, node.at("/id").asText(), node.at("/skipped").asBoolean());
            }
        }
    }

    public String parent(ObjectNode cells, String parent) {
        if (!cells.has(parent)) return parent;
        JsonNode node = cells.get(parent);
        String shape = node.at("/shape").asText();
        if ("flow-group".equals(shape)) {
            return parent(cells, node.at("/parent").asText(""));
        }
        return parent;
    }

}
