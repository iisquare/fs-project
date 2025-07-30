package com.iisquare.fs.web.cron.service;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.util.JDBCUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.cron.dao.TriggerDao;
import com.iisquare.fs.web.cron.entity.QuartzTrigger;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import java.util.*;

@Service
public class TriggerService extends ServiceBase {

    @Autowired
    private NodeService nodeService;
    @Autowired
    private TriggerDao triggerDao;

    public Map<?, ?> search(Map<?, ?> param, Map<?, ?> config) throws Exception {
        Scheduler scheduler = nodeService.scheduler();
        String schedule = scheduler.getSchedulerName();
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, 15);
        Page<QuartzTrigger> data = triggerDao.findAll((Specification<QuartzTrigger>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("schedule"), schedule)); // 仅获取当前实例下的作业
            String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
            if(!DPUtil.empty(name)) {
                predicates.add(cb.like(root.get("name"), "%" + name + "%"));
            }
            String group = DPUtil.trim(DPUtil.parseString(param.get("group")));
            if(!DPUtil.empty(group)) {
                predicates.add(cb.like(root.get("group"), "%" + group + "%"));
            }
            String jobName = DPUtil.trim(DPUtil.parseString(param.get("jobName")));
            if(!DPUtil.empty(jobName)) {
                predicates.add(cb.like(root.get("jobName"), "%" + jobName + "%"));
            }
            String jobGroup = DPUtil.trim(DPUtil.parseString(param.get("jobGroup")));
            if(!DPUtil.empty(jobGroup)) {
                predicates.add(cb.like(root.get("jobGroup"), "%" + jobGroup + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, PageRequest.of(page - 1, pageSize, Sort.by(new Sort.Order(Sort.Direction.DESC, "name"))));
        List<QuartzTrigger> rows = data.getContent();
        for (QuartzTrigger item : rows) {
            item.setArg(DPUtil.stringify(JDBCUtil.blob2object(item.getData())));
            item.setData(null);
            Trigger trigger = scheduler.getTrigger(TriggerKey.triggerKey(item.getName(), item.getGroup()));
            item.setNextFireTime(trigger.getNextFireTime());
            item.setFinalFireTime(trigger.getFinalFireTime());
            if (trigger instanceof CronTrigger) {
                item.setExpression(((CronTrigger) trigger).getCronExpression());
            }
        }
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("total", data.getTotalElements());
        result.put("rows", rows);
        return result;
    }

    public Map<String, Object> save(Map<?, ?> param) {
        Scheduler scheduler = nodeService.scheduler();
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        if (DPUtil.empty(name)) return ApiUtil.result(1001, "触发器名称不能为空", name);
        String group = DPUtil.trim(DPUtil.parseString(param.get("group")));
        if (DPUtil.empty(group)) return ApiUtil.result(1002, "触发器分组名称不能为空", group);
        String jobName = DPUtil.trim(DPUtil.parseString(param.get("jobName")));
        if (DPUtil.empty(jobName)) return ApiUtil.result(1003, "作业名称不能为空", jobName);
        String jobGroup = DPUtil.trim(DPUtil.parseString(param.get("jobGroup")));
        if (DPUtil.empty(jobGroup)) return ApiUtil.result(1004, "作业分组名称不能为空", jobGroup);
        String expression = DPUtil.trim(DPUtil.parseString(param.get("expression")));
        if (DPUtil.empty(expression)) return ApiUtil.result(1005, "表达式不能为空", expression);
        String arg = DPUtil.parseString(param.get("arg"));

        JobDetail detail;
        try {
            detail = scheduler.getJobDetail(JobKey.jobKey(jobName, jobGroup));
        } catch (SchedulerException e) {
            return ApiUtil.result(5001, "获取作业信息失败", e.getMessage());
        }
        if (null == detail) return ApiUtil.result(1401, "作业信息不存在", null);
        Trigger trigger; // 历史触发器
        try {
            trigger = scheduler.getTrigger(TriggerKey.triggerKey(name, group));
        } catch (SchedulerException e) {
            return ApiUtil.result(5002, "获取触发器失败", e.getMessage());
        }
        if (null != trigger && !(trigger instanceof CronTrigger)) {
            return ApiUtil.result(1402, "仅支持保存Cron类型触发器", trigger.getClass().getName());
        }
        if (null != trigger && !trigger.getJobKey().equals(detail.getKey())) {
            return ApiUtil.result(1403, "不支持更改触发器所属作业", trigger.getClass().getName());
        }
        TriggerBuilder<Trigger> builder = TriggerBuilder.newTrigger().withIdentity(name, group);
        try {
            builder.withSchedule( // 忽略misfire，执行下一个周期的任务
                    CronScheduleBuilder.cronSchedule(expression).withMisfireHandlingInstructionDoNothing());
        } catch (Exception e) {
            return ApiUtil.result(5003, "构建表达式失败", e.getMessage());
        }
        if (param.containsKey("priority")) {
            builder.withPriority(DPUtil.parseInt(param.get("priority")));
        } else if (null != trigger) {
            builder.withPriority(trigger.getPriority());
        }
        builder.forJob(detail).withDescription(DPUtil.parseString(param.get("description")));
        if (!DPUtil.empty(arg)) {
            JobDataMap data = DPUtil.toJSON(DPUtil.parseJSON(arg), JobDataMap.class);
            if (null == data) return ApiUtil.result(1009, "解析触发参数失败", arg);
            builder.usingJobData(data);
        }
        Date date;
        try {
            if (null == trigger) {
                date = scheduler.scheduleJob(builder.build());
            } else {
                date = scheduler.rescheduleJob(trigger.getKey(), builder.build());
            }
        } catch (SchedulerException e) {
            return ApiUtil.result(5003, "保存调度信息失败", e.getMessage());
        }
        return ApiUtil.result(0, null, date);
    }

    public Map<String, Object> command(Map<?, ?> param) {
        Scheduler scheduler = nodeService.scheduler();
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        String group = DPUtil.trim(DPUtil.parseString(param.get("group")));
        String command = DPUtil.trim(DPUtil.parseString(param.get("command")));
        TriggerKey triggerKey = TriggerKey.triggerKey(name, group);
        Trigger trigger;
        try {
            trigger = scheduler.getTrigger(triggerKey);
        } catch (Exception e) {
            return ApiUtil.result(5001, "获取作业信息失败", e.getMessage());
        }
        if (null == trigger) {
            if ("delete".equals(command)) {
                return ApiUtil.result(0, null, false);
            } else {
                return ApiUtil.result(1404, "触发器不存在", null);
            }
        }
        switch (command) {
            case "pause":
                try {
                    scheduler.pauseTrigger(trigger.getKey());
                } catch (Exception e) {
                    return ApiUtil.result(5001, "暂停触发器失败", e.getMessage());
                }
                break;
            case "resume":
                try {
                    scheduler.resumeTrigger(trigger.getKey());
                } catch (Exception e) {
                    return ApiUtil.result(5001, "恢复触发器失败", e.getMessage());
                }
                break;
            case "delete":
                try {
                    scheduler.unscheduleJob(trigger.getKey());
                } catch (Exception e) {
                    return ApiUtil.result(5001, "删除触发器失败", e.getMessage());
                }
                break;
            default:
                return ApiUtil.result(1405, "操作指令暂不支持", command);
        }
        return ApiUtil.result(0, null, true);
    }

}
