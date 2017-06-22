package com.iisquare.jwframe.frontend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.iisquare.etl.spark.flow.Submitter;
import com.iisquare.jwframe.core.component.CoreController;
import com.iisquare.jwframe.service.FlowService;
import com.iisquare.jwframe.utils.DPUtil;

@Controller
@Scope("prototype")
public class FlowController extends CoreController {

	@Autowired
	public FlowService flowService;
	
	public Object demoAction() throws Exception {
		return displayTemplate();
	}
	
	public Object editAction() throws Exception {
		assign("flowJson", "");
		return displayTemplate();
	}
	
	public Object pluginsAction() throws Exception {
		boolean forceReload = !DPUtil.empty(getParam("forceReload"));
		List<Map<String, Object>> list = flowService.generateTree(forceReload);
		return displayJSON(list);
	}
	
	public Object saveAction() throws Exception {
		String flowJson = getParam("flow");
		boolean forceReload = !DPUtil.empty(getParam("forceReload"));
		boolean result = Submitter.submit(flowJson, forceReload);
		return displayMessage(0, null, result);
	}

}
