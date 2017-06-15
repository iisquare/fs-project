package com.iisquare.jwframe.frontend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.iisquare.jwframe.core.component.CoreController;
import com.iisquare.jwframe.service.DemoService;

@Controller
@Scope("prototype")
public class FlowController extends CoreController {

	@Autowired
	public DemoService demoService;
	
	public Object demoAction() throws Exception {
		return displayTemplate();
	}
	
	public Object editAction() throws Exception {
		return displayTemplate();
	}
	
	public Object pluginsAction() throws Exception {
		return displayTemplate();
	}

}
