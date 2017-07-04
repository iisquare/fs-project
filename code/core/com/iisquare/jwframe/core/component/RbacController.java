package com.iisquare.jwframe.core.component;

import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.iisquare.jwframe.service.MenuService;
import com.iisquare.jwframe.service.ResourceService;
import com.iisquare.jwframe.service.RoleService;
import com.iisquare.jwframe.service.SettingService;
import com.iisquare.jwframe.service.UserService;
import com.iisquare.jwframe.utils.DPUtil;
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
	@Autowired
	protected ResourceService resourceService;
	@Autowired
	protected RoleService roleService;
	protected Map<String, Object> userInfo = null; // 当前登录用户信息
	private boolean isCheckPermit = true; // 是否进行权限验证
	
	public boolean isCheckPermit() {
		return isCheckPermit;
	}

	public void setCheckPermit(boolean isCheckPermit) {
		this.isCheckPermit = isCheckPermit;
	}
	
	public Map<String, Object> getUserInfo() {
		return userInfo;
	}

	public void setUserInfo(Map<String, Object> userInfo) {
		this.userInfo = userInfo;
	}

	@Override
	public Object init() {
		userInfo = userService.getCurrentUserInfo(request, true);
		preCheckPermit();
		if(!isCheckPermit || hasPermit()) return super.init();
		return new Exception("403");
	}

	/**
	 * 检测权限前执行
	 */
	protected void preCheckPermit() {}
	
	/**
	 * 判断是否拥有当前Action的权限
	 */
	public boolean hasPermit () {
		return hasPermit(moduleName, controllerName, actionName, "");
	}
	
	public boolean hasPermit (String operation) {
		return hasPermit(moduleName, controllerName, actionName, operation);
	}
	
	public boolean hasPermit (String action, String operation) {
		return hasPermit(moduleName, controllerName, action, operation);
	}
	
	public boolean hasPermit (String controller, String action, String operation) {
		return hasPermit(moduleName, controller, action, operation);
	}
	
	/**
	 * 判断是否拥有对应Module、Controller、Action、Operation的权限
	 */
	@SuppressWarnings("unchecked")
	public boolean hasPermit (String module, String controller, String action, String operation) {
		//if(isCheckPermit) return true; // 调试模式，拥有所有权限
		Map<String, Object> resourceInfo = resourceService.getInfoByRouter(null, module, controller, action, operation);
		if(null == resourceInfo) return false; // 资源不存在
		int status = DPUtil.parseInt(resourceInfo.get("status"));
		if(-1 == status) return true; // 全部允许访问
		if(1 != status) return false; // 全部阻止访问
		if(null == userInfo) return false; // 用户未登录
		int uid = DPUtil.parseInt(userInfo.get("id"));
		Set<Object> resourceIdSet = (Set<Object>) userInfo.get("resourceIds");
		if(null == resourceIdSet) {
			Set<Object> roleIdSet = roleService.getIdSetByUserId(uid);
			userInfo.put("roleIds", roleIdSet);
			resourceIdSet = resourceService.getIdSetByRoleIds(DPUtil.collectionToArray(roleIdSet));
			userInfo.put("resourceIds", resourceIdSet);
		}
		if(resourceIdSet.contains(resourceInfo.get("id"))) return true; // 验证登录用户是否拥有当前资源
		return false;
	}
	
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
