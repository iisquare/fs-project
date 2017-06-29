package com.iisquare.jwframe.core.component;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.iisquare.jwframe.service.MenuService;
import com.iisquare.jwframe.service.SettingService;
import com.iisquare.jwframe.service.UserService;
import com.iisquare.jwframe.utils.ServletUtil;

@Controller
@Scope("prototype")
public abstract class RbacController extends CoreController {

	@Autowired
	protected SettingService settingService;
	@Autowired
	protected UserService userService;
	@Autowired
	protected MenuService menuService;
	
	@Override
	protected Object displayTemplate(String controller, String action) throws Exception {
		assign("siteName", settingService.getProperty(null, "siteName", "系统名称未设置"));
		String uri = ServletUtil.getFullUrl(request, false, true);
		assign("menuTree", menuService.generateTree(appPath, null, uri, true));
		return super.displayTemplate(controller, action);
	}
	
}
