package com.iisquare.jwframe.backend.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.iisquare.jwframe.core.component.RbacController;
import com.iisquare.jwframe.service.FlowService;
import com.iisquare.jwframe.service.JobService;
import com.iisquare.jwframe.utils.DPUtil;
import com.iisquare.jwframe.utils.ServletUtil;
import com.iisquare.jwframe.utils.ValidateUtil;

@Controller
@Scope("prototype")
public class FlowController extends RbacController {

	@Autowired
	public FlowService flowService;
	@Autowired
	protected JobService jobService;
	
	public Object scheduleAction () throws Exception {
		Integer id = ValidateUtil.filterInteger(getParam("id"), true, 0, null, null);
		Map<String, Object> info = flowService.getInfo(id);
		String op = DPUtil.parseString(getParam("op"));
		switch (op) {
		case "save":
			if(null == info) return displayMessage(404, null, id);
			String cronExpression = DPUtil.trim(getParam("cronExpression"));
			if(DPUtil.empty(cronExpression)) return displayMessage(10001, "计划任务表达式不能为空", null);
			int priority = DPUtil.parseInt(getParam("priority"));
			boolean result = jobService.scheduleJob(id, cronExpression, priority, DPUtil.parseString(getParam("description")));
			if(!result) {
				if(jobService.hasError()) return displayJSON(jobService.getLastError());
				return displayMessage(500, "操作失败", id);
			}
			return displayMessage(0, null, result);
		case "delete":
			if(jobService.unscheduleJob(id)) {
				return displayInfo(0, "操作成功", null);
			} else {
				if(jobService.hasError()) {
					return displayInfo(500, DPUtil.parseString(jobService.getLastError().get("message")), null);
				}
				return displayInfo(500, "操作失败", null);
			}
		case "trigger":
			if(jobService.triggerJob(id)) {
				return displayMessage(0, "操作成功", null);
			} else {
				if(jobService.hasError()) {
					return displayJSON(jobService.getLastError());
				}
				return displayMessage(500, "操作失败", null);
			}
		}
		if(null == info) return displayInfo(404, null, null);
		assign("info", info);
		return displayTemplate();
	}
	
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
	
	@SuppressWarnings("unchecked")
	public Object listAction () throws Exception {
		int page = ValidateUtil.filterInteger(getParam("page"), true, 0, null, 1);
		int pageSize = ValidateUtil.filterInteger(getParam("rows"), true, 0, 500, 30);
		Map<Object, Object> map = flowService.search(params, "sort asc, update_time desc", page, pageSize);
		assign("total", map.get("total"));
		List<Map<String, Object>> rows = (List<Map<String, Object>>) map.get("rows");
		if(!DPUtil.empty("withCron")) {
			Map<Integer, Map<String, Object>> triggerMap = jobService.parseTriggers(jobService.getTriggers());
			for (Map<String, Object> item : rows) {
				item.put("trigger", triggerMap.get(item.get("id")));
			}
		}
		assign("rows", DPUtil.collectionToArray(rows));
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
		if(DPUtil.empty(idArray) || 1 != idArray.length) {
			return displayInfo(20001, "暂不支持多ID删除", null);
		}
		if(!jobService.unscheduleJob(DPUtil.parseInt(idArray[0]))) {
			if(jobService.hasError()) {
				return displayInfo(500, DPUtil.parseString(jobService.getLastError().get("message")), null);
			}
			return displayInfo(500, "取消任务调度失败", null);
		}
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
