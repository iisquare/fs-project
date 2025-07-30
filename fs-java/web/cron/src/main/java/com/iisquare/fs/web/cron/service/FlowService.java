package com.iisquare.fs.web.cron.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.mvc.JPAServiceBase;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.cron.dao.FlowDao;
import com.iisquare.fs.web.cron.entity.Flow;
import com.iisquare.fs.web.cron.job.DiagramJob;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

@Service
public class FlowService extends JPAServiceBase {

    @Autowired
    private FlowDao flowDao;
    @Autowired
    private DefaultRbacService rbacService;
    @Autowired
    private NodeService nodeService;

    public String group() {
        return this.getClass().getName();
    }

    public Map<?, ?> status() {
        Map<Integer, String> status = new LinkedHashMap<>();
        status.put(1, "启用");
        status.put(2, "禁用");
        return status;
    }

    public ObjectNode search(Map<String, Object> param, Map<?, ?> args) throws Exception {
        Scheduler scheduler = nodeService.scheduler();
        ObjectNode result = search(flowDao, param, (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            int id = DPUtil.parseInt(param.get("id"));
            if (id > 0) predicates.add(cb.equal(root.get("id"), id));
            int status = DPUtil.parseInt(param.get("status"));
            if (!"".equals(DPUtil.parseString(param.get("status")))) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
            if(!DPUtil.empty(name)) {
                predicates.add(cb.like(root.get("name"), "%" + name + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, Sort.by(Sort.Order.desc("sort")), "id", "name", "sort", "status");
        JsonNode rows = format(ApiUtil.rows(result));
        if(!DPUtil.empty(args.get("withTriggerInfo"))) {
            for (JsonNode flow : rows) {
                fillTrigger(scheduler, (ObjectNode) flow);
            }
        }
        if(!DPUtil.empty(args.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        if(!DPUtil.empty(args.get("withStatusText"))) {
            fillStatus(rows, status());
        }
        return result;
    }

    public JsonNode format(JsonNode rows) {
        for (JsonNode row : rows) {
            ObjectNode node = (ObjectNode) row;
            node.replace("content", DPUtil.parseJSON(node.at("/content").asText()));
            node.replace("notify", DPUtil.parseJSON(node.at("/notify").asText()));
        }
        return rows;
    }

    public boolean fillTrigger(Scheduler scheduler, ObjectNode flow) throws SchedulerException {
        if (null == flow) return false;
        int id = flow.at("/id").asInt();
        Trigger trigger = scheduler.getTrigger(TriggerKey.triggerKey(String.valueOf(id), group()));
        if (null == trigger) return false;
        flow.put("state", scheduler.getTriggerState(trigger.getKey()).name());
        flow.put("startTime", DPUtil.parseLong(trigger.getStartTime()));
        flow.put("endTime", DPUtil.parseLong(trigger.getEndTime()));
        flow.put("previousFireTime", DPUtil.parseLong(trigger.getPreviousFireTime()));
        flow.put("nextFireTime", DPUtil.parseLong(trigger.getNextFireTime()));
        flow.put("finalFireTime", DPUtil.parseLong(trigger.getFinalFireTime()));
        return true;
    }

    public Flow info(Integer id) {
        return info(flowDao, id);
    }

    public Map<String, Object> info(Map<?, ?> param) {
        Scheduler scheduler = nodeService.scheduler();
        Flow flow = info(DPUtil.parseInt(param.get("id")));
        if (null == flow) return ApiUtil.result(1404, "流程信息不存在", null);
        JsonNode node = DPUtil.firstNode(format(DPUtil.toArrayNode(flow)));
        try {
            fillTrigger(scheduler, (ObjectNode) node);
        } catch (SchedulerException e) {
            return ApiUtil.result(5001, "获取触发器信息失败", e.getMessage());
        }
        return ApiUtil.result(0, null, node);
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        Scheduler scheduler = nodeService.scheduler();
        Integer id = ValidateUtil.filterInteger(param.get("id"), true, 1, null, 0);
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if (DPUtil.empty(name)) return ApiUtil.result(1001, "流程名称不能为空", name);
        int status = DPUtil.parseInt(param.get("status"));
        if(!status().containsKey(status)) return ApiUtil.result(1003, "状态异常", status);
        String expression = DPUtil.trim(DPUtil.parseString(param.get("expression")));
        CronScheduleBuilder cron = null;
        if (!DPUtil.empty(expression)) {
            try {
                cron = CronScheduleBuilder.cronSchedule(expression).withMisfireHandlingInstructionDoNothing();
            } catch (Exception e) {
                return ApiUtil.result(5001, "构建表达式失败", e.getMessage());
            }
        }
        Flow info;
        if(id > 0) {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            info = info(id);
            if(null == info) return ApiUtil.result(404, null, id);
        } else {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            info = new Flow();
        }
        info.setName(name);
        info.setExpression(expression);
        info.setConcurrent(DPUtil.parseInt(param.get("concurrent")));
        info.setConcurrency(DPUtil.parseString(param.get("concurrency")));
        info.setFailure(DPUtil.parseString(param.get("failure")));
        info.setData(DPUtil.parseString(param.get("data")));
        info.setNotify(DPUtil.stringify(param.get("notify")));
        info.setContent(DPUtil.stringify(param.get("content")));
        info.setSort(DPUtil.parseInt(param.get("sort")));
        info.setStatus(status);
        info.setDescription(DPUtil.parseString(param.get("description")));
        info = save(flowDao, info, rbacService.uid(request));
        JobDetail detail = JobBuilder.newJob(DiagramJob.class) // Flow标识与Job保持一致
                .withIdentity(JobKey.jobKey(String.valueOf(info.getId()), group())).storeDurably().build();
        try {
            scheduler.addJob(detail, true); // 添加或替换作业
        } catch (Exception e) {
            return ApiUtil.result(5003, "提交作业调度信息失败", e.getMessage());
        }
        Trigger trigger;
        try {
            trigger = scheduler.getTrigger(TriggerKey.triggerKey(String.valueOf(info.getId()), group()));
        } catch (SchedulerException e) {
            return ApiUtil.result(5004, "获取触发器信息失败", e.getMessage());
        }
        if (null == cron) { // 未设置触发调度
            if (null != trigger) {
                try {
                    scheduler.unscheduleJob(trigger.getKey());
                } catch (SchedulerException e) {
                    return ApiUtil.result(5005, "解除调度失败", e.getMessage());
                }
            }
        } else {
            TriggerBuilder<CronTrigger> builder = TriggerBuilder.newTrigger()
                    .withIdentity(String.valueOf(info.getId()), group()).withSchedule(cron).forJob(detail);
            try {
                if (null == trigger) {
                    scheduler.scheduleJob(builder.build());
                } else {
                    Trigger.TriggerState state = scheduler.getTriggerState(trigger.getKey());
                    scheduler.rescheduleJob(trigger.getKey(), builder.build());
                    if (Trigger.TriggerState.PAUSED.equals(state)) {
                        scheduler.pauseTrigger(trigger.getKey()); // 保持暂停状态
                    }
                }
            } catch (SchedulerException e) {
                return ApiUtil.result(5003, "保存调度信息失败", e.getMessage());
            }

        }
        return info(DPUtil.buildMap("id", info.getId()));
    }

    public Map<String, Object> delete(Map<?, ?> param, HttpServletRequest request) {
        Scheduler scheduler = nodeService.scheduler();
        Flow info = info(DPUtil.parseInt(param.get("id")));
        if (null == info) return ApiUtil.result(1001, "流程不存在或已被删除", null);
        if(!rbacService.hasPermit(request, "delete")) {
            return ApiUtil.result(9403, null, null);
        }
        boolean result;
        try {
            result = scheduler.deleteJob(JobKey.jobKey(String.valueOf(info.getId()), group()));
        } catch (SchedulerException e) {
            return ApiUtil.result(5001, "移除调度作业失败", e.getMessage());
        }
        flowDao.delete(info);
        return ApiUtil.result(0, null, result);
    }

    public JsonNode fillInfo(JsonNode json, String ...properties) {
        return fillInfo(flowDao, json, properties);
    }

}
