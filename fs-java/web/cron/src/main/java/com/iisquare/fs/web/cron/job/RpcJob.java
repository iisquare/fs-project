package com.iisquare.fs.web.cron.job;

import com.iisquare.fs.base.core.util.ApiUtil;
import com.iisquare.fs.web.cron.CronApplication;
import com.iisquare.fs.web.cron.service.RpcService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

public class RpcJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(RpcJob.class);

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
        Map<String, Object> result = rpcService.invoke(param, schedule, jobName, jobGroup, triggerName, triggerGroup);
        if (ApiUtil.failed(result)) {
            logger.error("RpcJob execute failed: {}", ApiUtil.message(result));
            throw new JobExecutionException(ApiUtil.message(result));
        }
    }

}
