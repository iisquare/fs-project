package com.iisquare.jwframe.backend.controller;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.iisquare.jwframe.core.component.RbacController;
import com.iisquare.jwframe.utils.DPUtil;
import com.iisquare.jwframe.utils.ValidateUtil;

@Controller
@Scope("prototype")
public class SettingController extends RbacController {

	public Object indexAction () throws Exception {
		assign("qargs", params);
		return displayTemplate();
	}
	
	public Object listAction () throws Exception {
		int page = ValidateUtil.filterInteger(getParam("page"), true, 0, null, 1);
		int pageSize = ValidateUtil.filterInteger(getParam("rows"), true, 0, 500, 30);
		Map<Object, Object> map = settingService.search(params, "sort asc, update_time desc", page, pageSize);
		assign("total", map.get("total"));
		assign("rows", DPUtil.collectionToArray((Collection<?>) map.get("rows")));
		return displayJSON();
	}
	
	public Object deleteAction() throws Exception {
		if(!params.containsKey("type") || !params.containsKey("parameter")) {
			return displayInfo(10001, "参数异常", url("index"));
		}
		int result = settingService.delete(params);
		if(result >= 0) {
			return displayInfo(0, null, url("index"));
		} else {
			return displayInfo(500, null, null);
		}
	}
	
	public Object editAction() throws Exception {
		String type = getParam("type");
		String parameter = getParam("parameter");
		Map<String, Object> info;
		if(null == type || null == parameter) {
			info = new HashMap<>();
		} else {
			info = settingService.getInfo(type, parameter);
			if(null == info) return displayInfo(404, null, null);
		}
		assign("info", info);
		return displayTemplate();
	}
	
	public Object saveAction() throws Exception {
		String type = DPUtil.trim(getParam("type"));
		if(DPUtil.empty(type)) return displayMessage(10001, "类型不能为空", null);
		String parameter = DPUtil.trim(getParam("parameter"));
		if(DPUtil.empty(parameter)) return displayMessage(10002, "参数名不能为空", null);
		Map<String, Object> data = params;
		data.put("type", type);
		data.put("parameter", parameter);
		data.put("sort", DPUtil.parseInt(getParam("sort")));
		data.put("update_uid", 0);
		data.put("update_time", System.currentTimeMillis());
		if(settingService.exists(type, parameter)) {
			if(!settingService.update(data)) return displayMessage(500, "修改失败", null);
		} else {
			if(!settingService.insert(data)) return displayMessage(500, "添加失败", null);
		}
		return displayMessage(0, null, null);
	}
	
}
