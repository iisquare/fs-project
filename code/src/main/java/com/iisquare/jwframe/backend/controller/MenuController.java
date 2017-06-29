package com.iisquare.jwframe.backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.iisquare.jwframe.core.component.RbacController;
import com.iisquare.jwframe.service.MenuService;
import com.iisquare.jwframe.utils.DPUtil;

@Controller
@Scope("prototype")
public class MenuController extends RbacController {

	@Autowired
	private MenuService menuService;
	
	public Object indexAction () throws Exception {
		return displayTemplate();
	}
	
	public Object treeAction () throws Exception {
		boolean forceReload = !DPUtil.empty(getParam("forceReload"));
		List<Map<String, Object>> data = menuService.generateTree(moduleName, forceReload);
		return displayMessage(0, null, data);
	}
	
}
