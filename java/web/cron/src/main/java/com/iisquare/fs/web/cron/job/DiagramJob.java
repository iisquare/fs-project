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
import org.quartz.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DiagramJob implements Job {

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        long time = System.currentTimeMillis();
        Trigger trigger = context.getTrigger();
        JobDetail detail = context.getJobDetail();
        FlowDao flowDao = CronApplication.context.getBean(FlowDao.class);
        FlowLogDao flowLogDao = CronApplication.context.getBean(FlowLogDao.class);
        FlowStageDao flowStageDao = CronApplication.context.getBean(FlowStageDao.class);
        FlowLogService logService = CronApplication.context.getBean(FlowLogService.class);
        // 读取流程配置
        Flow flow = flowDao.findById(Flow.IdClass.byJobKey(detail.getKey())).orElse(null);
        if (null == flow) {
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
        logBuilder.project(flow.getProject()).name(flow.getName());
        logBuilder.concurrent(Math.max(1, json.at("/concurrent").asInt(0)));
        logBuilder.concurrency(json.at("/concurrency").asText(""));
        logBuilder.failure(json.at("/failure").asText(""));
        logBuilder.data(flow.getData()).content(DPUtil.stringify(json.at("/cells")));
        logBuilder.state(FlowLog.State.RUNNING.name()).createdTime(time).updatedTime(time);
        if ("SkipExecution".equals(json.at("/concurrency").asText())) {
            int count = flowLogDao.countByState(flow.getProject(), flow.getName(), FlowLog.State.RUNNING.name());
            if (count > 0) {
                logBuilder.state(FlowLog.State.SKIPPED.name());
                ((ObjectNode) json).putArray("cells"); // 清空执行节点
            }
        }
        FlowLog flowLog = flowLogDao.save(logBuilder.build());
        // 解析流程节点
        ObjectNode cells = DPUtil.array2object(json.at("/cells"), "id");
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
            if (!"flow-subprocess".equals(shape) && !"flow-group".equals(shape)) { // 更改所属节点
                if (!node.has("depend")) node.putArray("depend");
                node.put("parent", parent(cells, node.at("/parent").asText("")));
            }
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
            stageBuilder.title(node.at("/data/title").asText(""));
            stageBuilder.skipped(node.at("/data/skipped").asBoolean(false) ? 1 : 0);
            stageBuilder.data(DPUtil.stringify(node.at("/data/options"))).content("");
            stageBuilder.state(FlowStage.State.WAITING.name()).createdTime(time).runTime(0L).updatedTime(time);
            stages.add(stageBuilder.build());
        }
        if (stages.size() > 0) {
            flowStageDao.saveAll(stages);
        }
        // 汇报
        logService.tick(DPUtil.buildMap("logId", flowLog.getId()));
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
