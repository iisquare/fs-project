package com.iisquare.jwframe.backend.controller;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.iisquare.jwframe.core.component.RbacController;
import com.iisquare.jwframe.service.FlowService;
import com.iisquare.jwframe.utils.DPUtil;
import com.iisquare.jwframe.utils.ServletUtil;
import com.iisquare.jwframe.utils.ValidateUtil;

@Controller
@Scope("prototype")
public class FlowController extends RbacController {

	@Autowired
	public FlowService flowService;
	
	public Object drawAction() throws Exception {
		Integer id = ValidateUtil.filterInteger(getParam("id"), true, 0, null, null);
		if(ServletUtil.isAjax(request)) {
			String content = DPUtil.trim(getParam("content"));
			if(DPUtil.empty(content)) return displayMessage(10001, "参数异常", null);
			int uid = DPUtil.parseInt(userInfo.get("id"));
			long time = System.currentTimeMillis();
			Map<String, Object> data = params;
			data.put("update_uid", uid);
			data.put("update_time", time);
			int result = flowService.update(data);
			if(0 > result) return displayMessage(500, "修改失败", null);
			return displayMessage(0, null, result);
		}
		Map<String, Object> info;
		if(null == id) {
			info = new HashMap<>();
		} else {
			info = flowService.getInfo(id);
			if(null == info) return displayInfo(404, null, null);
		}
		assign("info", info);
		return displayTemplate();
	}
	
	public Object indexAction () throws Exception {
		assign("qargs", params);
		assign("statusMap", flowService.getStatusMap());
		return displayTemplate();
	}
	
	public Object listAction () throws Exception {
		int page = ValidateUtil.filterInteger(getParam("page"), true, 0, null, 1);
		int pageSize = ValidateUtil.filterInteger(getParam("rows"), true, 0, 500, 30);
		Map<Object, Object> map = flowService.search(params, "sort asc, update_time desc", page, pageSize);
		assign("total", map.get("total"));
		assign("rows", DPUtil.collectionToArray((Collection<?>) map.get("rows")));
		return displayJSON();
	}
	
	public Object editAction() throws Exception {
		Integer id = ValidateUtil.filterInteger(getParam("id"), true, 0, null, null);
		Map<String, Object> info;
		if(null == id) {
			info = new HashMap<>();
		} else {
			info = flowService.getInfo(id);
			if(null == info) return displayInfo(404, null, null);
		}
		assign("info", info);
		assign("statusMap", flowService.getStatusMap());
		return displayTemplate();
	}
	
	public Object saveAction() throws Exception {
		String name = DPUtil.trim(getParam("name"));
		if(DPUtil.empty(name)) return displayMessage(10001, "名称不能为空", null);
		int uid = DPUtil.parseInt(userInfo.get("id"));
		long time = System.currentTimeMillis();
		Map<String, Object> data = params;
		data.put("name", name);
		data.put("sort", DPUtil.parseInt(getParam("sort")));
		data.put("status", DPUtil.parseInt(getParam("status")));
		data.put("update_uid", uid);
		data.put("update_time", time);
		int result = -1;
		if(DPUtil.empty(getParam("id"))) {
			data.put("create_uid", uid);
			data.put("create_time", time);
			result = flowService.insert(data);
			if(1 > result) return displayMessage(500, "添加失败", null);
		} else {
			result = flowService.update(data);
			if(0 > result) return displayMessage(500, "修改失败", null);
		}
		return displayMessage(0, null, result);
	}
	
	public Object deleteAction() throws Exception {
		Object[] idArray = getArray("ids");
		int result = flowService.delete(idArray);
		if(-1 == result) return displayInfo(10001, "参数异常", null);
		if(-2 == result) return displayInfo(10002, "该记录拥有下级节点，不允许删除", null);
		if(result >= 0) {
			return displayInfo(0, null, url("index"));
		} else {
			return displayInfo(500, null, null);
		}
	}
	
	public Object pluginsAction() throws Exception {
		boolean forceReload = !DPUtil.empty(getParam("forceReload"));
		List<Map<String, Object>> list = flowService.pluginTree(forceReload);
		return displayJSON(list);
	}
	
}
