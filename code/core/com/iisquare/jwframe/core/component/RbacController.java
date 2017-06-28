package com.iisquare.jwframe.core.component;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.iisquare.jwframe.service.SettingService;

@Controller
@Scope("prototype")
public abstract class RbacController extends CoreController {

	@Autowired
	protected SettingService settingService;
	
	@Override
	protected Object displayTemplate(String controller, String action) throws Exception {
		assign("siteName", settingService.getProperty(null, "siteName", "系统名称未设置"));
		return super.displayTemplate(controller, action);
	}
	
}
