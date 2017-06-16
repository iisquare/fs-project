package com.iisquare.jwframe.frontend.controller;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.iisquare.jwframe.core.component.CoreController;
import com.iisquare.jwframe.service.DemoService;
import com.iisquare.jwframe.utils.FileUtil;

@Controller
@Scope("prototype")
public class FlowController extends CoreController {

	@Autowired
	public DemoService demoService;
	private static String flowJson = null;
	
	public Object demoAction() throws Exception {
		return displayTemplate();
	}
	
	public Object editAction() throws Exception {
		if(null == flowJson) flowJson = FileUtil.getContent(rootPath + File.separator + "WEB-INF"
			+ File.separator + "template" + File.separator + "frontend" + File.separator + "flow" + File.separator + "test.json");
		assign("flowJson", flowJson);
		return displayTemplate();
	}
	
	public Object pluginsAction() throws Exception {
		return displayTemplate();
	}
	
	public Object saveAction() throws Exception {
		flowJson = getParam("flow");
		return displayMessage(0, null, null);
	}

}
