package com.iisquare.fs.web.cron.service;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.core.rbac.DefaultRbacService;
import com.iisquare.fs.web.cron.dao.FlowDao;
import com.iisquare.fs.web.cron.entity.Flow;
import com.iisquare.fs.web.cron.job.DiagramJob;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.Predicate;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Service
public class FlowService extends ServiceBase {

    @Autowired
    private FlowDao flowDao;
    @Autowired
    private DefaultRbacService rbacService;
    @Autowired
    private NodeService nodeService;

    public Map<?, ?> search(Map<?, ?> param, Map<?, ?> config) throws Exception {
        Scheduler scheduler = nodeService.scheduler();
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, 15);
        Page<Flow> data = flowDao.findAll((Specification<Flow>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            String project = DPUtil.trim(DPUtil.parseString(param.get("project")));
            if(!DPUtil.empty(project)) {
                predicates.add(cb.like(root.get("project"), project));
            }
            String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
            if(!DPUtil.empty(name)) {
                predicates.add(cb.like(root.get("name"), name));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, PageRequest.of(page - 1, pageSize, Sort.by(new Sort.Order(Sort.Direction.DESC, "sort"))));
        List<Flow> rows = data.getContent();
        if(!DPUtil.empty(config.get("withTriggerInfo"))) {
            for (Flow flow : rows) {
                fillTrigger(scheduler, flow);
            }
        }
        if(!DPUtil.empty(config.get("withUserInfo"))) {
            rbacService.fillUserInfo(rows, "createdUid", "updatedUid");
        }
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", data.getTotalElements());
        result.put("rows", rows);
        return result;
    }

    public boolean fillTrigger(Scheduler scheduler, Flow flow) throws SchedulerException {
        if (null == flow) return false;
        Trigger trigger = scheduler.getTrigger(TriggerKey.triggerKey(flow.getName(), flow.getProject()));
        if (null == trigger) return false;
        flow.setState(scheduler.getTriggerState(trigger.getKey()).name());
        flow.setStartTime(trigger.getStartTime());
        flow.setEndTime(trigger.getEndTime());
        flow.setPreviousFireTime(trigger.getPreviousFireTime());
        flow.setNextFireTime(trigger.getNextFireTime());
        flow.setFinalFireTime(trigger.getFinalFireTime());
        return true;
    }

    public Flow info(String project, String name) {
        Optional<Flow> info = flowDao.findById(Flow.IdClass.builder().project(project).name(name).build());
        return info.isPresent() ? info.get() : null;
    }

    public Map<String, Object> info(Map<?, ?> param) {
        Scheduler scheduler = nodeService.scheduler();
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if (DPUtil.empty(name)) return ApiUtil.result(1001, "流程名称不能为空", name);
        String project = DPUtil.trim(DPUtil.parseString(param.get("project")));
        if (DPUtil.empty(project)) return ApiUtil.result(1002, "项目名称不能为空", project);
        Flow flow = info(project, name);
        if (null == flow) return ApiUtil.result(1404, "流程信息不存在", null);
        try {
            fillTrigger(scheduler, flow);
        } catch (SchedulerException e) {
            return ApiUtil.result(5001, "获取触发器信息失败", e.getMessage());
        }
        return ApiUtil.result(0, null, flow);
    }

    public Map<String, Object> save(Map<?, ?> param, HttpServletRequest request) {
        Scheduler scheduler = nodeService.scheduler();
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if (DPUtil.empty(name)) return ApiUtil.result(1001, "流程名称不能为空", name);
        String project = DPUtil.trim(DPUtil.parseString(param.get("project")));
        if (DPUtil.empty(project)) return ApiUtil.result(1002, "项目名称不能为空", project);
        String expression = DPUtil.trim(DPUtil.parseString(param.get("expression")));
        CronScheduleBuilder cron = null;
        if (!DPUtil.empty(expression)) {
            try {
                cron = CronScheduleBuilder.cronSchedule(expression).withMisfireHandlingInstructionDoNothing();
            } catch (Exception e) {
                return ApiUtil.result(5001, "构建表达式失败", e.getMessage());
            }
        }

        boolean isNew = true;
        int uid = rbacService.uid(request);
        long time = System.currentTimeMillis();
        Flow flow = info(project, name);
        if (null == flow) {
            if(!rbacService.hasPermit(request, "add")) return ApiUtil.result(9403, null, null);
            flow = Flow.builder().project(project).name(name)
                    .createdUid(uid).updatedUid(uid).createdTime(time).updatedTime(time).build();
        } else {
            if(!rbacService.hasPermit(request, "modify")) return ApiUtil.result(9403, null, null);
            isNew = false;
            flow.setUpdatedUid(uid);
            flow.setUpdatedTime(time);
        }
        if (isNew || param.containsKey("expression")) flow.setExpression(expression);
        if (isNew || param.containsKey("data")) flow.setData(DPUtil.parseString(param.get("data")));
        if (isNew || param.containsKey("notify")) flow.setNotify(DPUtil.parseString(param.get("notify")));
        if (isNew || param.containsKey("content")) flow.setContent(DPUtil.parseString(param.get("content")));
        if (isNew || param.containsKey("sort")) flow.setSort(DPUtil.parseInt(param.get("sort")));
        if (isNew || param.containsKey("description")) flow.setDescription(DPUtil.parseString(param.get("description")));

        JobDetail detail = JobBuilder.newJob(DiagramJob.class) // Flow标识与Job保持一致
                .withIdentity(JobKey.jobKey(name, project)).storeDurably().build();
        try {
            scheduler.addJob(detail, true); // 添加或替换作业
        } catch (Exception e) {
            return ApiUtil.result(5003, "保存作业信息失败", e.getMessage());
        }

        Trigger trigger;
        try {
            trigger = scheduler.getTrigger(TriggerKey.triggerKey(name, project));
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
                    .withIdentity(name, project).withSchedule(cron).forJob(detail);
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
        flowDao.save(flow);
        return ApiUtil.result(0, null, flow);
    }

    public Map<String, Object> delete(Map<?, ?> param, HttpServletRequest request) {
        Scheduler scheduler = nodeService.scheduler();
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if (DPUtil.empty(name)) return ApiUtil.result(1001, "流程名称不能为空", name);
        String project = DPUtil.trim(DPUtil.parseString(param.get("project")));
        if (DPUtil.empty(project)) return ApiUtil.result(1002, "项目名称不能为空", project);
        if(!rbacService.hasPermit(request, "delete")) return ApiUtil.result(9403, null, null);
        boolean result = false;
        try {
            result = scheduler.deleteJob(JobKey.jobKey(name, project));
        } catch (SchedulerException e) {
            return ApiUtil.result(5001, "移除调度作业失败", e.getMessage());
        }
        flowDao.deleteById(Flow.IdClass.builder().project(project).name(name).build());
        return ApiUtil.result(0, null, result);
    }

}
