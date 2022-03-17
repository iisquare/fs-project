package com.iisquare.fs.web.cron.job;

import com.iisquare.fs.base.core.util.DPUtil;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.Date;

public class DiagramJob implements Job {
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        System.out.println("Date:" + new Date());
        System.out.println("Job:" + DPUtil.stringify(context.getJobDetail().getJobDataMap()));
        System.out.println("Trigger:" + DPUtil.stringify(context.getTrigger().getJobDataMap()));

    }
}
