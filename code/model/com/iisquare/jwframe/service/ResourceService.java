package com.iisquare.jwframe.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

import com.iisquare.jwframe.Configuration;
import com.iisquare.jwframe.dao.RelationDao;
import com.iisquare.jwframe.dao.ResourceDao;
import com.iisquare.jwframe.dao.UserDao;
import com.iisquare.jwframe.mvc.ServiceBase;
import com.iisquare.jwframe.utils.DPUtil;
import com.iisquare.jwframe.utils.ServiceUtil;

@Service
@Scope("prototype")
public class ResourceService extends ServiceBase {
	
	@Autowired
	protected WebApplicationContext webApplicationContext;
	@Autowired
	protected Configuration configuration;
	
	public Map<String, String> getStatusMap() {
		Map<String, String> map = new LinkedHashMap<String, String>();
		map.put("-1", "全部允许");
		map.put("0", "全部阻止");
		map.put("1", "正常验证");
		return map;
	}
	
	public Set<Object> getIdSetByRoleIds(Object[] roleIdArray) {
		if(DPUtil.empty(roleIdArray)) return new HashSet<>();
		RelationDao dao = webApplicationContext.getBean(RelationDao.class);
		List<Map<String, Object>> list = dao.where(
				"type='role_resource' and aid in (" + DPUtil.arrayToIntegerArray(roleIdArray) + ")", new HashMap<>()).all();
		return ServiceUtil.getFieldValues(list, "bid");
	}
	
	public Map<Object, Object> search(Map<String, Object> map, String orderBy, int page, int pageSize) {
		StringBuilder condition = new StringBuilder("1=1");
		Map<String, Object> params = new HashMap<String, Object>();
		Object name = map.get("name");
		if(!DPUtil.empty(name)) {
			condition.append(" and name like :name");
			params.put(":name", "%" + name + "%");
		}
		Object module = map.get("module");
		if(!DPUtil.empty(module)) {
			condition.append(" and module = :module");
			params.put(":module", module);
		}
		Object controller = map.get("controller");
		if(!DPUtil.empty(controller)) {
			condition.append(" and controller = :controller");
			params.put(":controller", controller);
		}
		Object action = map.get("action");
		if(!DPUtil.empty(action)) {
			condition.append(" and action = :action");
			params.put(":action", action);
		}
		Object operation = map.get("operation");
		if(!DPUtil.empty(operation)) {
			condition.append(" and operation = :operation");
			params.put(":operation", operation);
		}
		Object status = map.get("status");
		if(null != status && !"".equals(status)) {
			condition.append(" and status = :status");
			params.put(":status", status);
		}
		Object description = map.get("description");
		if(!DPUtil.empty(description)) {
			condition.append(" and description like :description");
			params.put(":description", "%" + description + "%");
		}
		Object timeStart = map.get("timeStart");
		if(!DPUtil.empty(timeStart)) {
			condition.append(" and update_time >= :timeStart");
			params.put(":timeStart", DPUtil.dateTimeToMillis(timeStart, configuration.getDateTimeFormat()));
		}
		Object timeEnd = map.get("timeEnd");
		if(!DPUtil.empty(timeEnd)) {
			condition.append(" and update_time <= :timeEnd");
			params.put(":timeEnd", DPUtil.dateTimeToMillis(timeEnd, configuration.getDateTimeFormat()));
		}
		ResourceDao dao = webApplicationContext.getBean(ResourceDao.class);
		int total = dao.where(condition.toString(), params).count().intValue();
		List<Map<String, Object>> rows;
		if(-1 == pageSize) {
			rows = dao.orderBy(orderBy).all();
		} else {
			rows = dao.orderBy(orderBy).page(page, pageSize).all();
		}
		rows = ServiceUtil.fillFields(rows, new String[]{"status"}, new Map<?, ?>[]{getStatusMap()}, null);
		UserDao userDao = webApplicationContext.getBean(UserDao.class);
		rows = ServiceUtil.fillRelations(rows, userDao,
				new String[]{"create_uid", "update_uid"}, new String[]{"id", "username", "name"}, null);
		return DPUtil.buildMap(new String[]{"total", "rows"}, new Object[]{total, rows});
	}
	
	public Map<String, Object> getInfo(Object id) {
		ResourceDao dao = webApplicationContext.getBean(ResourceDao.class);
		return dao.where("id = :id", ":id", id).one();
	}
	
	public Map<String, Object> getInfoByRouter(
			Integer exceptId, String module, String controller, String action, String operation) {
		if(null == exceptId) exceptId = 0;
		ResourceDao dao = webApplicationContext.getBean(ResourceDao.class);
		return dao.where("id!=:id and module=:module and controller=:controller and action=:action and operation=:operation",
			":id", exceptId, ":module", module, ":controller", controller, ":action", action, ":operation", operation).one();
	}
	
	public int insert(Map<String, Object> data) {
		if(DPUtil.empty(data.get("id"))) data.remove("id");
		ResourceDao dao = webApplicationContext.getBean(ResourceDao.class);
		return dao.insert(data).intValue();
	}
	
	public int update(Map<String, Object> data) {
		ResourceDao dao = webApplicationContext.getBean(ResourceDao.class);
		return dao.where("id = :id", ":id", data.get("id")).update(data).intValue();
	}
	
	public int delete(Object ...ids) {
		if(DPUtil.empty(ids)) return -1;
		ResourceDao dao = webApplicationContext.getBean(ResourceDao.class);
		int count = dao.where("parent_id in ("
			+ DPUtil.implode(",", DPUtil.arrayToIntegerArray(ids)) + ")", new HashMap<>()).count().intValue();
		if(count > 0) return -2;
		return dao.where("id in ("
			+ DPUtil.implode(",", DPUtil.arrayToIntegerArray(ids)) + ")", new HashMap<>()).delete().intValue();
	}

}
