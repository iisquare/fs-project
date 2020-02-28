package com.iisquare.jwframe.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.log4j.Logger;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

import com.iisquare.etl.spark.flow.QuartzJob;
import com.iisquare.jwframe.Configuration;
import com.iisquare.jwframe.dao.FlowDao;
import com.iisquare.jwframe.dao.JobDao;
import com.iisquare.jwframe.dao.JobNodeDao;
import com.iisquare.jwframe.mvc.ServiceBase;
import com.iisquare.jwframe.utils.DPUtil;
import com.iisquare.jwframe.utils.PropertiesUtil;
import com.iisquare.jwframe.utils.ServiceUtil;

@Service
@Scope("prototype")
public class JobService extends ServiceBase {
	
	public static final String CONFIG_FILE_NAME = "spark.properties";
	public static final String GROUP_NAME = "ETLVisual";
	@Autowired
	protected WebApplicationContext webApplicationContext;
	@Autowired
	protected Configuration configuration;
	private static Scheduler scheduler = null;
	private Logger logger = Logger.getLogger(getClass().getName());

	@PostConstruct
	public void init() {
		if(null != scheduler) return;
		try {
			SchedulerFactory schedulerFactory = new StdSchedulerFactory(
					PropertiesUtil.load(JobService.class.getClassLoader(), "quartz.properties"));
			scheduler = schedulerFactory.getScheduler();
			scheduler.start();
		} catch (SchedulerException e) {
			logger.error("quartz service init faild!", e);
		}
		
	}
	
	public Map<String, String> getStatusMap() {
		Map<String, String> map = new LinkedHashMap<String, String>();
		map.put("1", "触发执行");
		map.put("2", "开发分发");
		map.put("5", "分发完成");
		map.put("6", "执行完成");
		return map;
	}
	
	public Map<String, Object> getInfo(Object id) {
		JobDao dao = webApplicationContext.getBean(JobDao.class);
		return dao.where("id = :id", ":id", id).one();
	}
	
	public Map<Object, Map<String, Object>> getJobStates(Object jobId) {
		JobNodeDao dao = webApplicationContext.getBean(JobNodeDao.class);
		return dao.where("job_id=:job_id", ":job_id", jobId).all("node_id");
	}
	
	public Map<Object, Object> search(Map<String, Object> map, String orderBy, int page, int pageSize) {
		StringBuilder condition = new StringBuilder("1=1");
		Map<String, Object> params = new HashMap<String, Object>();
		Object flow_id = map.get("flow_id");
		if(!DPUtil.empty(flow_id)) {
			condition.append(" and flow_id = :flow_id");
			params.put(":flow_id", flow_id);
		}
		Object application_id = map.get("application_id");
		if(!DPUtil.empty(application_id)) {
			condition.append(" and application_id = :application_id");
			params.put(":application_id", application_id);
		}
		Object status = map.get("status");
		if(null != status && !"".equals(status)) {
			condition.append(" and status = :status");
			params.put(":status", status);
		}
		Object flow_content = map.get("flow_content");
		if(!DPUtil.empty(flow_content)) {
			condition.append(" and flow_content like :flow_content");
			params.put(":flow_content", "%" + flow_content + "%");
		}
		Object timeTriggerStart = map.get("timeTriggerStart");
		if(!DPUtil.empty(timeTriggerStart)) {
			condition.append(" and trigger_time >= :timeTriggerStart");
			params.put(":timeTriggerStart", DPUtil.dateTimeToMillis(timeTriggerStart, configuration.getDateTimeFormat()));
		}
		Object timeTriggerEnd = map.get("timeTriggerEnd");
		if(!DPUtil.empty(timeTriggerEnd)) {
			condition.append(" and trigger_time <= :timeTriggerEnd");
			params.put(":timeTriggerEnd", DPUtil.dateTimeToMillis(timeTriggerEnd, configuration.getDateTimeFormat()));
		}
		Object timeDispatchStart = map.get("timeDispatchStart");
		if(!DPUtil.empty(timeDispatchStart)) {
			condition.append(" and dispatch_time >= :timeDispatchStart");
			params.put(":timeDispatchStart", DPUtil.dateTimeToMillis(timeDispatchStart, configuration.getDateTimeFormat()));
		}
		Object timeDispatchEnd = map.get("timeDispatchEnd");
		if(!DPUtil.empty(timeDispatchEnd)) {
			condition.append(" and dispatch_time <= :timeDispatchEnd");
			params.put(":timeDispatchEnd", DPUtil.dateTimeToMillis(timeDispatchEnd, configuration.getDateTimeFormat()));
		}
		Object timeDispatchedStart = map.get("timeDispatchedStart");
		if(!DPUtil.empty(timeDispatchedStart)) {
			condition.append(" and dispatched_time >= :timeDispatchedStart");
			params.put(":timeDispatchedStart", DPUtil.dateTimeToMillis(timeDispatchedStart, configuration.getDateTimeFormat()));
		}
		Object timeDispatchedEnd = map.get("timeDispatchedEnd");
		if(!DPUtil.empty(timeDispatchedEnd)) {
			condition.append(" and dispatched_time <= :timeDispatchedEnd");
			params.put(":timeDispatchedEnd", DPUtil.dateTimeToMillis(timeDispatchedEnd, configuration.getDateTimeFormat()));
		}
		Object timeCompleteStart = map.get("timeCompleteStart");
		if(!DPUtil.empty(timeCompleteStart)) {
			condition.append(" and complete_time >= :timeCompleteStart");
			params.put(":timeCompleteStart", DPUtil.dateTimeToMillis(timeCompleteStart, configuration.getDateTimeFormat()));
		}
		Object timeCompleteEnd = map.get("timeCompleteEnd");
		if(!DPUtil.empty(timeCompleteEnd)) {
			condition.append(" and complete_time <= :timeCompleteEnd");
			params.put(":timeCompleteEnd", DPUtil.dateTimeToMillis(timeCompleteEnd, configuration.getDateTimeFormat()));
		}
		JobDao dao = webApplicationContext.getBean(JobDao.class);
		int total = dao.where(condition.toString(), params).count().intValue();
		List<Map<String, Object>> rows = dao.orderBy(orderBy).page(page, pageSize).all();
		rows = ServiceUtil.fillFields(rows, new String[]{"status"}, new Map<?, ?>[]{getStatusMap()}, null);
		FlowDao flowDao = webApplicationContext.getBean(FlowDao.class);
		rows = ServiceUtil.fillRelations(rows, flowDao,
				new String[]{"flow_id"}, new String[]{"id", "name"}, null);
		return DPUtil.buildMap(new String[]{"total", "rows"}, new Object[]{total, rows});
	}
	
	public Map<Integer, Map<String, Object>> parseTriggers(List<Map<String, Object>> list) {
		Map<Integer, Map<String, Object>> map = new LinkedHashMap<>();
		for (Map<String, Object> item : list) {
			map.put(DPUtil.parseInt(item.get("flowId")), item);
		}
		return map;
	}
	
	public List<Map<String, Object>> getTriggers() {
		List<Map<String, Object>> list = new ArrayList<>();
		GroupMatcher<TriggerKey> matcher = GroupMatcher.anyGroup();
		try {
			Set<TriggerKey> triggerKeySet = scheduler.getTriggerKeys(matcher);
			for (TriggerKey triggerKey : triggerKeySet) {
				Trigger trigger = scheduler.getTrigger(triggerKey);
				Map<String, Object> item = new LinkedHashMap<>();
				item.put("calendarName", trigger.getCalendarName());
				item.put("triggerName", trigger.getKey().getName());
				item.put("triggerGroup", trigger.getKey().getGroup());
				item.put("jobName", trigger.getJobKey().getName());
				item.put("jobGroup", trigger.getJobKey().getGroup());
				item.put("description", trigger.getDescription());
				Date date = trigger.getPreviousFireTime();
				item.put("previousFireTime", null == date ? 0 : date.getTime());
				date = trigger.getNextFireTime();
				item.put("nextFireTime", null == date ? 0 : date.getTime());
				item.put("priority", trigger.getPriority());
				date = trigger.getStartTime();
				item.put("startTime", null == date ? 0 : date.getTime());
				date = trigger.getEndTime();
				item.put("endTime", null == date ? 0 : date.getTime());
				if(trigger instanceof CronTriggerImpl) {
					CronTriggerImpl cronTrigger = (CronTriggerImpl) trigger;
					item.put("cronExpression", cronTrigger.getCronExpression());
				} else {
					item.put("cronExpression", "");
				}
				item.put("triggerState", scheduler.getTriggerState(triggerKey).name());
				JobDataMap jobDataMap = scheduler.getJobDetail(trigger.getJobKey()).getJobDataMap();
				item.put("flowId", jobDataMap.get("flowId"));
				list.add(item);
			}
			return list;
		} catch (SchedulerException e) {
			return setLastError(500, e.getMessage(), null, list);
		}
	}
	
	public boolean isStarted() {
		try {
			return scheduler.isStarted();
		} catch (SchedulerException e) {
			return setLastError(500, e.getMessage(), null, false);
		}
	}
	
	public boolean isShutdown() {
		try {
			return scheduler.isShutdown();
		} catch (SchedulerException e) {
			return setLastError(500, e.getMessage(), null, false);
		}
	}
	
	public boolean start() {
		try {
			if(scheduler.isShutdown()) {
				scheduler = null;
				init();
			} else {
				scheduler.start();
			}
			return true;
		} catch (SchedulerException e) {
			return setLastError(500, e.getMessage(), null, false);
		}
	}
	
	public boolean pauseTrigger(String name, String group) {
		try {
			scheduler.pauseTrigger(TriggerKey.triggerKey(name, group));
			return true;
		} catch (SchedulerException e) {
			return setLastError(500, e.getMessage(), null, false);
		}
	}
	
	public boolean resumeTrigger(String name, String group) {
		try {
			scheduler.resumeTrigger(TriggerKey.triggerKey(name, group));
			return true;
		} catch (SchedulerException e) {
			return setLastError(500, e.getMessage(), null, false);
		}
	}
	
	public boolean pauseAll() {
		try {
			scheduler.pauseAll();
			return true;
		} catch (SchedulerException e) {
			return setLastError(500, e.getMessage(), null, false);
		}
	}
	
	public boolean resumeAll() {
		try {
			scheduler.resumeAll();
			return true;
		} catch (SchedulerException e) {
			return setLastError(500, e.getMessage(), null, false);
		}
	}
	
	public boolean shutdown(boolean waitForJobsToComplete) {
		try {
			scheduler.shutdown(waitForJobsToComplete);
			return true;
		} catch (SchedulerException e) {
			return setLastError(500, e.getMessage(), null, false);
		}
	}
	
	public String jobName(int flowId) {
		return "FlowJob" + flowId;
	}
	
	public String triggerName(int flowId) {
		return "FlowTrigger" + flowId;
	}
	
	public boolean triggerJob(int flowId) {
		try {
			JobKey jobKey = JobKey.jobKey(jobName(flowId), GROUP_NAME);
			if(scheduler.checkExists(jobKey)) {
				scheduler.triggerJob(jobKey);
				return true;
			}
			jobKey = JobKey.jobKey("Temp_" + jobName(flowId), GROUP_NAME);
			JobDataMap jobDataMap = new JobDataMap();
			jobDataMap.put("flowId", flowId);
			jobDataMap.put("deleteJobOnCompleted", true);
			JobDetail jobDetail = JobBuilder.newJob(QuartzJob.class).withIdentity(jobKey).storeDurably().setJobData(jobDataMap).build();
			scheduler.addJob(jobDetail, true);
			scheduler.triggerJob(jobKey);
			return true;
		} catch (SchedulerException e) {
			return setLastError(500, e.getMessage(), null, false);
		}
	}
	
	public boolean scheduleJob(int flowId, String cronExpression, int priority, String description) {
		if(null == scheduler) return false;
		JobDataMap jobDataMap = new JobDataMap();
		jobDataMap.put("flowId", flowId);
		JobDetail job = JobBuilder.newJob(QuartzJob.class).withIdentity(jobName(flowId), GROUP_NAME).setJobData(jobDataMap).build();
		Trigger trigger = TriggerBuilder.newTrigger().withIdentity(triggerName(flowId), GROUP_NAME)
				.withSchedule(CronScheduleBuilder.cronSchedule(cronExpression).withMisfireHandlingInstructionDoNothing())
				.withPriority(priority).withDescription(description).forJob(job).build();
		try {
			scheduler.scheduleJob(job, trigger);
			return true;
		} catch (SchedulerException e) {
			return setLastError(500, e.getMessage(), null, false);
		}
	}
	
	public boolean unscheduleJob(int flowId) {
		if(null == scheduler) return false;
		try {
			return scheduler.unscheduleJob(TriggerKey.triggerKey(triggerName(flowId), GROUP_NAME));
		} catch (SchedulerException e) {
			return setLastError(500, e.getMessage(), null, false);
		}
	}
	
	public boolean unscheduleJob(String name, String group) {
		if(null == scheduler) return false;
		try {
			return scheduler.unscheduleJob(TriggerKey.triggerKey(name, group));
		} catch (SchedulerException e) {
			return setLastError(500, e.getMessage(), null, false);
		}
	}
	
	@PreDestroy
	public void destroy() {
		if(null == scheduler) return;
		try {
			scheduler.shutdown(true);
		} catch (SchedulerException e) {
			logger.error("quartz service destroyed error!", e);
		} finally {
			scheduler = null;
		}
	}
	
}
