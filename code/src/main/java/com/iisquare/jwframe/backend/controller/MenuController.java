package com.iisquare.jwframe.backend.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.iisquare.jwframe.core.component.RbacController;
import com.iisquare.jwframe.utils.DPUtil;
import com.iisquare.jwframe.utils.ValidateUtil;

@Controller
@Scope("prototype")
public class MenuController extends RbacController {

	public Object indexAction () throws Exception {
		return displayTemplate();
	}
	
	public Object listAction () throws Exception {
		return displayJSON(menuService.generateTree(getParam("parentId")));
	}
	
	public Object editAction() throws Exception {
		Integer id = ValidateUtil.filterInteger(getParam("id"), true, 0, null, null);
		Map<String, Object> info;
		if(null == id) {
			info = new HashMap<>();
			info.put("parent_id", DPUtil.parseInt(getParam("parentId")));
		} else {
			info = menuService.getInfo(id);
			if(null == info) return displayInfo(404, null, null);
		}
		assign("info", info);
		assign("statusMap", menuService.getStatusMap());
		return displayTemplate();
	}
	
	public Object saveAction() throws Exception {
		String name = DPUtil.trim(getParam("name"));
		if(DPUtil.empty(name)) return displayMessage(10001, "名称不能为空", null);
		long time = System.currentTimeMillis();
		Map<String, Object> data = params;
		data.put("name", name);
		data.put("parent_id", DPUtil.parseInt(getParam("parent_id")));
		data.put("sort", DPUtil.parseInt(getParam("sort")));
		data.put("status", DPUtil.parseInt(getParam("status")));
		data.put("update_uid", 0);
		data.put("update_time", time);
		int result = -1;
		if(DPUtil.empty(getParam("id"))) {
			data.put("create_uid", 0);
			data.put("create_time", time);
			result = menuService.insert(data);
			if(1 > result) return displayMessage(500, "添加失败", null);
		} else {
			result = menuService.update(data);
			if(0 > result) return displayMessage(500, "修改失败", null);
		}
		return displayMessage(0, null, result);
	}
	
	public Object deleteAction() throws Exception {
		Object[] idArray = getArray("ids");
		int result = menuService.delete(idArray);
		if(-1 == result) return displayInfo(10001, "参数异常", null);
		if(-2 == result) return displayInfo(10002, "该记录拥有下级节点，不允许删除", null);
		if(result >= 0) {
			return displayInfo(0, null, url("index"));
		} else {
			return displayInfo(500, null, null);
		}
	}
	
}
