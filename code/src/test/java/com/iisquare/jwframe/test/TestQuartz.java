package com.iisquare.jwframe.test;

import java.util.Date;

import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;

import com.iisquare.jwframe.utils.PropertiesUtil;

public class TestQuartz implements Job {

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		System.out.println("Hello, Quartz! - executing its JOB at "+  new Date() + " by " + context.getTrigger().getKey().toString());
	}
	
	public static void main(String[] args) throws SchedulerException {
		SchedulerFactory schedFact = new StdSchedulerFactory(
				PropertiesUtil.load(TestQuartz.class.getClassLoader(), "quartz.properties"));
		Scheduler sched = schedFact.getScheduler();
		sched.start();
		JobDetail job = JobBuilder.newJob(TestQuartz.class).withIdentity("myJob1", "group1").build();
		Trigger trigger = TriggerBuilder.newTrigger().withIdentity("trigger1", "group1")
				.withSchedule(CronScheduleBuilder.cronSchedule("0/15 * * * * ?")).forJob(job).build();
		sched.scheduleJob(job, trigger);
		job = JobBuilder.newJob(TestQuartz.class).withIdentity("myJob2", "group1").build();
		trigger = TriggerBuilder.newTrigger().withIdentity("trigger2", "group1")
				.withSchedule(CronScheduleBuilder.cronSchedule("0/5 * * * * ?")).forJob(job).build();
		sched.scheduleJob(job, trigger);
		sched.unscheduleJob(TriggerKey.triggerKey("trigger1", "group1"));
		//sched.shutdown(true);
	}

}
