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
	
	/**
	 * 输出提示信息页面
	 */
	protected Object displayInfo(int code, String message, String forward) throws Exception {
		String icon = "ace-icon fa fa-comments-o";
		switch (code) {
		case 0:
			icon = "ace-icon fa fa-info-circle";
			if(null == message) message = "操作成功";
			break;
		case 403:
			icon = "ace-icon glyphicon glyphicon-road";
			if(null == message) message = "禁止访问";
			break;
		case 404:
			icon = "ace-icon fa fa-sitemap";
			if(null == message) message = "信息不存在";
			break;
		case 500:
			icon = "ace-icon fa fa-random";
			if(null == message) message = "操作失败";
			break;
		default :
			if(null == message) message = "对不起，您要访问页面不存在或出现了出错！";
		}
		assign("icon", icon);
		assign("code", code);
		assign("message", message);
		assign("forward", forward);
		return displayTemplate("public", "info");
	}
	
}
