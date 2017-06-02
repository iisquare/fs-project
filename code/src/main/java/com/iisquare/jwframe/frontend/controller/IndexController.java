package com.iisquare.jwframe.frontend.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.iisquare.jwframe.core.component.CoreController;
import com.iisquare.jwframe.service.DemoService;
import com.iisquare.jwframe.utils.DPUtil;

@Controller
@Scope("prototype")
public class IndexController extends CoreController {

	@Autowired
	public DemoService demoService;
	
	public Object indexAction() throws Exception {
		assign("message", demoService.getMessage());
		return displayTemplate();
	}
	
	public Object newsAction() throws Exception {
		assign("date", StringEscapeUtils.escapeHtml(getParam("date")));
		assign("id", DPUtil.parseInt(getParam("id")));
		return displayMessage(0, "news working!", assign);
	}
	
	public Object modifyAction() throws Exception {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("name", "Node" + DPUtil.getCurrentSeconds());
		data.put("status", 1);
		Integer id = demoService.insert(data);
		if(null == id) return displayMessage(500, null, null);
		return displayMessage(0, null, id);
	}
	
	public Object listAction() throws Exception {
		List<Map<String, Object>> data = demoService.getList();
		return displayMessage(0, null, data);
	}
}
