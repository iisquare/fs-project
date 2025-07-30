package com.iisquare.fs.web.cron.service;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.base.core.util.DPUtil;
import com.iisquare.fs.base.core.util.ValidateUtil;
import com.iisquare.fs.base.jpa.util.JDBCUtil;
import com.iisquare.fs.base.web.mvc.ServiceBase;
import com.iisquare.fs.web.cron.dao.JobDao;
import com.iisquare.fs.web.cron.entity.QuartzJob;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class JobService extends ServiceBase {

    @Autowired
    private NodeService nodeService;
    @Autowired
    private JobDao jobDao;

    public Map<?, ?> search(Map<?, ?> param, Map<?, ?> config) throws Exception {
        Scheduler scheduler = nodeService.scheduler();
        String schedule = scheduler.getSchedulerName();
        Map<String, Object> result = new LinkedHashMap<>();
        int page = ValidateUtil.filterInteger(param.get("page"), true, 1, null, 1);
        int pageSize = ValidateUtil.filterInteger(param.get("pageSize"), true, 1, 500, 15);
        Page<QuartzJob> data = jobDao.findAll((Specification<QuartzJob>) (root, query, cb) -> {
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
            return cb.and(predicates.toArray(new Predicate[0]));
        }, PageRequest.of(page - 1, pageSize, Sort.by(new Sort.Order(Sort.Direction.DESC, "name"))));
        List<QuartzJob> rows = data.getContent();
        for (QuartzJob job : rows) {
            job.setArg(DPUtil.stringify(JDBCUtil.blob2object(job.getData())));
            job.setData(null);
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
        if (DPUtil.empty(name)) return ApiUtil.result(1001, "作业名称不能为空", name);
        String group = DPUtil.trim(DPUtil.parseString(param.get("group")));
        if (DPUtil.empty(group)) return ApiUtil.result(1002, "分组名称不能为空", group);
        String cls = DPUtil.trim(DPUtil.parseString(param.get("cls")));
        if (DPUtil.empty(cls)) return ApiUtil.result(1003, "执行器类名称不能为空", cls);
        String arg = DPUtil.parseString(param.get("arg"));

        JobBuilder builder;
        try {
            builder = JobBuilder.newJob((Class<? extends Job>) Class.forName(cls));
        } catch (Exception e) {
            return ApiUtil.result(5001, "获取执行器失败", e.getMessage());
        }
        JobDetail detail = builder.withIdentity(JobKey.jobKey(name, group))
                .withDescription(DPUtil.parseString(param.get("description"))).storeDurably().build();
        if (!DPUtil.empty(arg)) {
            Map data = DPUtil.toJSON(DPUtil.parseJSON(arg), Map.class);
            if (null == data) return ApiUtil.result(1005, "解析作业参数失败", arg);
            detail.getJobDataMap().putAll(data);
        }
        try {
            scheduler.addJob(detail, true); // 添加或替换作业
        } catch (Exception e) {
            return ApiUtil.result(5003, "保存作业信息失败", e.getMessage());
        }
        try {
            return ApiUtil.result(0, null, quartz2job(scheduler, detail));
        } catch (Exception e) {
            return ApiUtil.result(5501, "转换作业信息失败", e.getMessage());
        }
    }

    public Map<String, Object> command(Map<?, ?> param) {
        Scheduler scheduler = nodeService.scheduler();
        String name = DPUtil.trim(DPUtil.parseString(param.get("name")));
        String group = DPUtil.trim(DPUtil.parseString(param.get("group")));
        String command = DPUtil.trim(DPUtil.parseString(param.get("command")));
        String arg = DPUtil.parseString(param.get("arg"));
        JobKey jobKey = JobKey.jobKey(name, group);
        JobDetail detail;
        try {
            detail = scheduler.getJobDetail(jobKey);
        } catch (Exception e) {
            return ApiUtil.result(5001, "获取作业信息失败", e.getMessage());
        }
        if (null == detail) {
            if ("delete".equals(command)) {
                return ApiUtil.result(0, null, false);
            } else {
                return ApiUtil.result(1404, "作业信息不存在", null);
            }
        }
        switch (command) {
            case "pause":
                try {
                    scheduler.pauseJob(detail.getKey());
                } catch (Exception e) {
                    return ApiUtil.result(5001, "暂停作业失败", e.getMessage());
                }
                break;
            case "resume":
                try {
                    scheduler.resumeJob(detail.getKey());
                } catch (Exception e) {
                    return ApiUtil.result(5001, "恢复作业失败", e.getMessage());
                }
                break;
            case "delete":
                try {
                    scheduler.deleteJob(detail.getKey());
                } catch (Exception e) {
                    return ApiUtil.result(5001, "删除作业失败", e.getMessage());
                }
                break;
            case "trigger":
                JobDataMap data = null;
                if (!DPUtil.empty(arg)) {
                    data = DPUtil.toJSON(DPUtil.parseJSON(arg), JobDataMap.class);
                    if (null == data) return ApiUtil.result(1005, "解析触发参数失败", arg);
                }
                try {
                    scheduler.triggerJob(detail.getKey(), data);
                } catch (Exception e) {
                    return ApiUtil.result(5003, "触发作业失败", e.getMessage());
                }
                break;
            default:
                return ApiUtil.result(1405, "操作指令暂不支持", command);
        }
        try {
            return ApiUtil.result(0, null, quartz2job(scheduler, detail));
        } catch (Exception e) {
            return ApiUtil.result(5501, "转换作业信息失败", e.getMessage());
        }
    }

    public QuartzJob quartz2job(Scheduler scheduler, JobDetail detail) throws Exception {
        QuartzJob.QuartzJobBuilder builder = QuartzJob.builder();
        builder.schedule(scheduler.getSchedulerName());
        builder.name(detail.getKey().getName()).group(detail.getKey().getGroup());
        builder.cls(detail.getJobClass().getName()).description(detail.getDescription());
        builder.arg(DPUtil.stringify(detail.getJobDataMap()));
        builder.durable(bool2str(detail.isDurable()));
        builder.nonConcurrent(bool2str(detail.isConcurrentExectionDisallowed()));
        builder.updateData(bool2str(detail.isPersistJobDataAfterExecution()));
        builder.recovery(bool2str(detail.requestsRecovery()));
        return builder.build();
    }

    public String bool2str(boolean b) {
        return b ? "1" : "0";
    }

}
