package com.iisquare.jwframe.backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.iisquare.jwframe.core.component.RbacController;
import com.iisquare.jwframe.service.JobService;
import com.iisquare.jwframe.utils.DPUtil;

@Controller
@Scope("prototype")
public class JobController extends RbacController {

	@Autowired
	protected JobService jobService;
	
	public Object indexAction () throws Exception {
		return displayTemplate();
	}
	
	public Object scheduleAction () throws Exception {
		String op = DPUtil.parseString(getParam("op"));
		switch (op) {
		case "list":
			List<Map<String, Object>> rows = jobService.getTriggers();
			assign("total", rows.size());
			assign("rows", DPUtil.collectionToArray(rows));
			return displayJSON();
		case "start":
			jobService.start();
			break;
		case "pauseAll":
			jobService.pauseAll();
			break;
		case "pause":
			jobService.pauseTrigger(getParam("triggerName"), getParam("triggerGroup"));
			break;
		case "resume":
			jobService.resumeTrigger(getParam("triggerName"), getParam("triggerGroup"));
			break;
		case "resumeAll":
			jobService.resumeAll();
			break;
		case "shutdown":
			jobService.shutdown(!DPUtil.empty("waitForJobsToComplete"));
			break;
		case "delete":
			jobService.unscheduleJob(getParam("triggerName"), getParam("triggerGroup"));
			break;
		}
		assign("isStarted", jobService.isStarted());
		assign("isShutdown", jobService.isShutdown());
		assign("lastError", jobService.getLastError());
		return displayTemplate();
	}
	
}
