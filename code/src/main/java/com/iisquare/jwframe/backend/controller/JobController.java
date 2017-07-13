package com.iisquare.jwframe.backend.controller;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.iisquare.etl.spark.config.Configuration;
import com.iisquare.jwframe.core.component.RbacController;
import com.iisquare.jwframe.service.JobService;
import com.iisquare.jwframe.utils.DPUtil;
import com.iisquare.jwframe.utils.ValidateUtil;

@Controller
@Scope("prototype")
public class JobController extends RbacController {

	@Autowired
	protected JobService jobService;
	
	public Object indexAction () throws Exception {
		return displayTemplate();
	}
	
	public Object detailAction() throws Exception {
		Integer id = ValidateUtil.filterInteger(getParam("id"), true, 0, null, null);
		Map<String, Object> info;
		if(null == id) {
			info = new HashMap<>();
		} else {
			info = jobService.getInfo(id);
			if(null == info) return displayInfo(404, null, null);
		}
		assign("info", info);
		return displayTemplate();
	}
	
	public Object stateAction() throws Exception {
		Integer id = ValidateUtil.filterInteger(getParam("id"), true, 0, null, null);
		return displayMessage(0, null, jobService.getJobStates(id));
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
	
	public Object historyAction () throws Exception {
		String op = DPUtil.parseString(getParam("op"));
		switch (op) {
		case "list":
			int page = ValidateUtil.filterInteger(getParam("page"), true, 0, null, 1);
			int pageSize = ValidateUtil.filterInteger(getParam("rows"), true, 0, 500, 30);
			Map<Object, Object> map = jobService.search(params, "id desc", page, pageSize);
			assign("total", map.get("total"));
			assign("rows", DPUtil.collectionToArray((Collection<?>) map.get("rows")));
			return displayJSON();
		}
		assign("qargs", params);
		assign("statusMap", jobService.getStatusMap());
		Configuration config = Configuration.getInstance();
		assign("sparkUi", config.getProperty("url.ui", "http://127.0.0.1:8080"));
		return displayTemplate();
	}
	
}
