package com.iisquare.etl.spark.flow;

import java.util.Map;

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
		JobDetail jobDetail = context.getJobDetail();
		JobKey jobKey = jobDetail.getKey();
		JobDataMap jobDataMap = jobDetail.getJobDataMap();
		JobService jobService = new JobService(jobDataMap);
		jobService.init(true);
		jobService.record(); // 记录作业
		// TODO:判断作业是否可调度
		jobService.update("dispatch"); // 开始调度
		Map<String, Object> dataMap = jobService.getDataMap();
		try {
			Submitter.submit(DPUtil.stringifyJSON(dataMap), false, false);
		} catch (Exception e1) {
			logger.error("submitJob[" + jobKey.getName() + ", " + jobKey.getGroup() + "] error!", e1);
		}
		if(!DPUtil.empty(jobDataMap.get("deleteJobOnCompleted"))) {
			try {
				context.getScheduler().deleteJob(jobKey);
			} catch (SchedulerException e) {
				logger.error("deleteJob[" + jobKey.getName() + ", " + jobKey.getGroup() + "] error!", e);
			}
		}
		jobService.update("dispatched"); // 调度完成
		jobService.close();
	}

}
