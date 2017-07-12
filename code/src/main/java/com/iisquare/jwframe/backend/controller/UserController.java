package com.iisquare.jwframe.backend.controller;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.iisquare.jwframe.core.component.RbacController;
import com.iisquare.jwframe.utils.DPUtil;
import com.iisquare.jwframe.utils.ServiceUtil;
import com.iisquare.jwframe.utils.ServletUtil;
import com.iisquare.jwframe.utils.ValidateUtil;

@Controller
@Scope("prototype")
public class UserController extends RbacController {

	public Object indexAction () throws Exception {
		assign("qargs", params);
		assign("statusMap", userService.getStatusMap());
		return displayTemplate();
	}
	
	public Object permitAction () throws Exception {
		Integer id = ValidateUtil.filterInteger(getParam("id"), true, 0, null, null);
		if(!ServletUtil.isAjax(request)) {
			Map<String, Object> info = userService.getInfo(id);
			if(null == info) return displayInfo(404, null, null);
			assign("info", info);
			assign("roleIds", DPUtil.implode(",", DPUtil.collectionToArray(
					ServiceUtil.getFieldValues(userService.getRoleRelList(id), "bid"))));
			return displayTemplate();
		}
		boolean result = userService.permit(id, getArray("roleIds"));
		if(!result) return displayMessage(500, "保存失败", null);
		return displayMessage(0, null, null);
	}
	
	public Object loginAction () throws Exception {
		if(!ServletUtil.isAjax(request)) {
			String forward = convertForward(getParam("forward"));
			if(null != userInfo) return redirect(forward);
			assign("forward", forward);
			return displayTemplate();
		}
		String username = ValidateUtil.filterSimpleString(getParam("username"), true, 1, 64, null);
		String password = ValidateUtil.filterSimpleString(getParam("password"), true, 1, null, null);
		if(DPUtil.empty(username) || DPUtil.empty(password)) {
			return displayMessage(10001, "请输入正确的账号和密码！", null);
		}
		Map<String, Object> info  = userService.getInfoByUsername(username);
		if(null == info) return displayMessage(10002, "账号或密码错误，请重新输入！", null);
		if(1 != DPUtil.parseInt(info.get("status"))) return displayMessage(10002, "账号或密码错误，请重新输入！", null);
		if(!userService.generatePassword(password, DPUtil.parseString(info.get("salt"))).equals(info.get("password"))) {
			return displayMessage(10003, "账号或密码错误，请重新输入！", null);
		}
		userService.setCurrentUserInfo(request, info);
		return displayMessage(0, "登录成功", convertForward(getParam("forward")));
	}
	
	public Object logoutAction() throws Exception {
		request.getSession().invalidate();
		return redirect(appPath);
	}
	
	public Object listAction () throws Exception {
		int page = ValidateUtil.filterInteger(getParam("page"), true, 0, null, 1);
		int pageSize = ValidateUtil.filterInteger(getParam("rows"), true, 0, 500, 30);
		Map<Object, Object> map = userService.search(params, "sort asc, update_time desc", page, pageSize);
		assign("total", map.get("total"));
		assign("rows", DPUtil.collectionToArray((Collection<?>) map.get("rows")));
		return displayJSON();
	}
	
	public Object deleteAction() throws Exception {
		Object[] idArray = getArray("ids");
		int result = userService.delete(idArray);
		if(-1 == result) return displayInfo(10001, "参数异常", null);
		if(result >= 0) {
			return displayInfo(0, null, url("index"));
		} else {
			return displayInfo(500, null, null);
		}
	}
	
	public Object editAction() throws Exception {
		Integer id = ValidateUtil.filterInteger(getParam("id"), true, 0, null, null);
		Map<String, Object> info;
		if(null == id) {
			info = new HashMap<>();
		} else {
			info = userService.getInfo(id);
			if(null == info) return displayInfo(404, null, null);
		}
		assign("info", info);
		assign("statusMap", userService.getStatusMap());
		return displayTemplate();
	}
	
	public Object saveAction() throws Exception {
		String name = DPUtil.trim(getParam("name"));
		if(DPUtil.empty(name)) return displayMessage(10001, "名称不能为空", null);
		String username = DPUtil.trim(getParam("username"));
		if(DPUtil.empty(username)) return displayMessage(10002, "账号不能为空", null);
		String password = DPUtil.trim(getParam("password"));
		int uid = DPUtil.parseInt(userInfo.get("id"));
		long time = System.currentTimeMillis();
		Map<String, Object> data = params;
		data.put("name", name);
		data.put("username", username);
		data.put("sort", DPUtil.parseInt(getParam("sort")));
		data.put("status", DPUtil.parseInt(getParam("status")));
		data.put("update_uid", uid);
		data.put("update_time", time);
		int result = -1;
		if(DPUtil.empty(getParam("id"))) {
			data.put("create_uid", uid);
			data.put("create_time", time);
			if(DPUtil.empty(password)) {
				password = settingService.getProperty(null, "defaultPassword", "admin888");
			}
			String salt = DPUtil.random(6);
			data.put("salt", salt);
			data.put("password", userService.generatePassword(password, salt));
			result = userService.insert(data);
			if(1 > result) return displayMessage(500, "添加失败", null);
		} else {
			if(!DPUtil.empty(password)) {
				String salt = DPUtil.random(6);
				data.put("salt", salt);
				data.put("password", userService.generatePassword(password, salt));
			}
			result = userService.update(data);
			if(0 > result) return displayMessage(500, "修改失败", null);
		}
		return displayMessage(0, null, result);
	}
	
	private String convertForward(String forward) throws Exception {
		String platformUrl = appPath;
		if(DPUtil.empty(forward)) {
			return platformUrl;
		} else {
			if("back".equals(forward)) {
				String backUrl = request.getHeader("Referer");
				if(null == backUrl) {
					return platformUrl;
				} else {
					return backUrl;
				}
			} else if("login".equals(forward)) {
				return appPath + "/login";
			} else {
				return forward;
			}
		}
	}
	
}
