package com.iisquare.fs.web.cron.job;

import com.iisquare.fs.web.cron.CronApplication;
import com.iisquare.fs.web.cron.service.RpcService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;

import java.util.LinkedHashMap;
import java.util.Map;

public class RpcJob implements Job {

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Map<String, Object> param = new LinkedHashMap<>(context.getMergedJobDataMap());
        RpcService rpcService = CronApplication.context.getBean(RpcService.class);
        String schedule;
        try {
            schedule = context.getScheduler().getSchedulerName();
        } catch (SchedulerException e) {
            throw new JobExecutionException(e);
        }
        String jobName = context.getJobDetail().getKey().getName();
        String jobGroup = context.getJobDetail().getKey().getGroup();
        String triggerName = context.getTrigger().getKey().getName();
        String triggerGroup = context.getTrigger().getKey().getGroup();
        rpcService.invoke(param, schedule, jobName, jobGroup, triggerName, triggerGroup);
    }

}
