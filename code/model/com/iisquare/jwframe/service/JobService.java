package com.iisquare.jwframe.service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.log4j.Logger;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

import com.iisquare.etl.spark.flow.QuartzJob;
import com.iisquare.jwframe.mvc.ServiceBase;
import com.iisquare.jwframe.test.TestQuartz;
import com.iisquare.jwframe.utils.PropertiesUtil;

@Service
@Scope("singleton")
public class JobService extends ServiceBase {
	
	public static final String CONFIG_FILE_NAME = "spark.properties";
	public static final String GROUP_NAME = "ETLVisual";
	@Autowired
	protected WebApplicationContext webApplicationContext;
	private Scheduler scheduler = null;
	private Logger logger = Logger.getLogger(getClass().getName());

	@PostConstruct
	public void init() {
		try {
			SchedulerFactory schedulerFactory = new StdSchedulerFactory(
					PropertiesUtil.load(TestQuartz.class.getClassLoader(), "quartz.properties"));
			scheduler = schedulerFactory.getScheduler();
			scheduler.start();
		} catch (SchedulerException e) {
			logger.error("quartz service init faild!", e);
		}
		
	}
	
	public boolean updateJob(int flowId) {
		if(!unscheduleJob(flowId)) return false;
		return scheduleJob(flowId);
	}
	
	public boolean scheduleJob(int flowId) {
		if(null == scheduler) return false;
		JobDetail job = JobBuilder.newJob(QuartzJob.class).withIdentity("flowJob" + flowId, GROUP_NAME).build();
		Trigger trigger = TriggerBuilder.newTrigger().withIdentity("flowTrigger" + flowId, GROUP_NAME)
				.withSchedule(CronScheduleBuilder.cronSchedule("0/15 * * * * ?")).forJob(job).build();
		try {
			scheduler.scheduleJob(job, trigger);
			return true;
		} catch (SchedulerException e) {
			logger.error("scheduleJobFlow error!", e);
			return false;
		}
	}
	
	public boolean unscheduleJob(int flowId) {
		if(null == scheduler) return false;
		try {
			return scheduler.unscheduleJob(TriggerKey.triggerKey("flowTrigger" + flowId, GROUP_NAME));
		} catch (SchedulerException e) {
			logger.error("unscheduleJobFlowerror!", e);
			return false;
		}
	}
	
	@PreDestroy
	public void destroy() {
		if(null == scheduler) return;
		try {
			scheduler.shutdown(true);
		} catch (SchedulerException e) {
			logger.error("quartz service destroyed error!", e);
		}
	}
	
}
