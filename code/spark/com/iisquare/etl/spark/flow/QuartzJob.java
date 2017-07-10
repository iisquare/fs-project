package com.iisquare.etl.spark.flow;

import java.util.Date;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.SchedulerException;

import com.iisquare.jwframe.utils.DPUtil;

public class QuartzJob implements Job {

	private Logger logger = Logger.getLogger(getClass().getName());
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		System.out.println("Hello, Quartz! - executing its JOB at "+  new Date() + " by " + context.getTrigger().getKey().toString());
		JobDetail jobDetail = context.getJobDetail();
		JobDataMap jobDataMap = jobDetail.getJobDataMap();
		if(!DPUtil.empty(jobDataMap.get("deleteOnCompleted"))) {
			JobKey jobKey = jobDetail.getKey();
			try {
				context.getScheduler().deleteJob(jobKey);
			} catch (SchedulerException e) {
				logger.error("deleteJob[" + jobKey.getName() + ", " + jobKey.getGroup() + "] error!", e);
			}
		}
	}

}
