package com.iisquare.jwframe.backend.controller;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.iisquare.jwframe.core.component.RbacController;
import com.iisquare.jwframe.service.ResourceService;
import com.iisquare.jwframe.utils.DPUtil;
import com.iisquare.jwframe.utils.ValidateUtil;

@Controller
@Scope("prototype")
public class ResourceController extends RbacController {

	@Autowired
	protected ResourceService resourceService;
	
	public Object indexAction () throws Exception {
		assign("qargs", params);
		assign("statusMap", resourceService.getStatusMap());
		return displayTemplate();
	}
	
	public Object listAction () throws Exception {
		int page = ValidateUtil.filterInteger(getParam("page"), true, 0, null, 1);
		int pageSize = ValidateUtil.filterInteger(getParam("rows"), true, 0, 500, 30);
		if(!DPUtil.empty(getParam("no_refer"))) pageSize = -1;
		Map<Object, Object> map = resourceService.search(params, "sort asc, update_time desc", page, pageSize);
		assign("total", map.get("total"));
		assign("rows", DPUtil.collectionToArray((Collection<?>) map.get("rows")));
		return displayJSON();
	}
	
	public Object deleteAction() throws Exception {
		Object[] idArray = getArray("ids");
		int result = resourceService.delete(idArray);
		if(-1 == result) return displayInfo(10001, "参数异常", null);
		if(-2 == result) return displayInfo(10002, "该记录拥有下级节点，不允许删除", null);
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
			info = resourceService.getInfo(id);
			if(null == info) return displayInfo(404, null, null);
		}
		assign("info", info);
		assign("statusMap", resourceService.getStatusMap());
		return displayTemplate();
	}
	
	public Object saveAction() throws Exception {
		String name = DPUtil.trim(getParam("name"));
		if(DPUtil.empty(name)) return displayMessage(10001, "名称不能为空", null);
		long time = System.currentTimeMillis();
		Map<String, Object> data = params;
		data.put("name", name);
		data.put("module", DPUtil.trim(getParam("module")));
		data.put("controller", DPUtil.trim(getParam("controller")));
		data.put("action", DPUtil.trim(getParam("action")));
		data.put("operation", DPUtil.trim(getParam("operation")));
		data.put("sort", DPUtil.parseInt(getParam("sort")));
		data.put("status", DPUtil.parseInt(getParam("status")));
		data.put("update_uid", 0);
		data.put("update_time", time);
		int result = -1;
		if(DPUtil.empty(getParam("id"))) {
			data.put("create_uid", 0);
			data.put("create_time", time);
			result = resourceService.insert(data);
			if(1 > result) return displayMessage(500, "添加失败", null);
		} else {
			result = resourceService.update(data);
			if(0 > result) return displayMessage(500, "修改失败", null);
		}
		return displayMessage(0, null, result);
	}
	
}
