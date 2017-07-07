package com.iisquare.jwframe.service;

import java.util.ArrayList;
import java.util.HashMap;
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
import com.iisquare.jwframe.dao.RoleDao;
import com.iisquare.jwframe.dao.UserDao;
import com.iisquare.jwframe.mvc.ServiceBase;
import com.iisquare.jwframe.utils.DPUtil;
import com.iisquare.jwframe.utils.ServiceUtil;

@Service
@Scope("prototype")
public class RoleService extends ServiceBase {
	
	@Autowired
	protected WebApplicationContext webApplicationContext;
	@Autowired
	protected Configuration configuration;
	
	public Set<Object> getIdSetByUserId(int uid) {
		RelationDao dao = webApplicationContext.getBean(RelationDao.class);
		List<Map<String, Object>> list = dao.where("type='user_role' and aid=:uid", ":uid", uid).all();
		return ServiceUtil.getFieldValues(list, "bid");
	}
	
	public Map<String, String> getStatusMap() {
		Map<String, String> map = new LinkedHashMap<String, String>();
		map.put("0", "禁用");
		map.put("1", "正常");
		return map;
	}
	
	public boolean permit(Object roleId, Object[] resourceIds, Object[] menuIds) {
		RelationDao dao = webApplicationContext.getBean(RelationDao.class);
		Number result = dao.where("type='role_menu' and aid=:roleId", ":roleId", roleId).delete();
		if(null == result) return false;
		List<Map<String, Object>> datas = new ArrayList<>();
		for (Object id : resourceIds) {
			Map<String, Object> item = new LinkedHashMap<>();
			item.put("type", "role_resource");
			item.put("aid", roleId);
			item.put("bid", id);
			datas.add(item);
		}
		for (Object id : menuIds) {
			Map<String, Object> item = new LinkedHashMap<>();
			item.put("type", "role_menu");
			item.put("aid", roleId);
			item.put("bid", id);
			datas.add(item);
		}
		result = dao.batchInsert(datas);
		return null != result;
	}
	
	public List<Map<String, Object>> getResourceRelList(Object roleId) {
		RelationDao dao = webApplicationContext.getBean(RelationDao.class);
		return dao.where("type='role_resource' and aid=:roleId", ":roleId", roleId).all();
	}
	
	public List<Map<String, Object>> getMenuRelList(Object roleId) {
		RelationDao dao = webApplicationContext.getBean(RelationDao.class);
		return dao.where("type='role_menu' and aid=:roleId", ":roleId", roleId).all();
	}
	
	public Map<Object, Object> search(Map<String, Object> map, String orderBy, int page, int pageSize) {
		StringBuilder condition = new StringBuilder("1=1");
		Map<String, Object> params = new HashMap<String, Object>();
		Object name = map.get("name");
		if(!DPUtil.empty(name)) {
			condition.append(" and name like :name");
			params.put(":name", "%" + name + "%");
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
		RoleDao dao = webApplicationContext.getBean(RoleDao.class);
		int total = dao.where(condition.toString(), params).count().intValue();
		List<Map<String, Object>> rows = dao.orderBy(orderBy).page(page, pageSize).all();
		rows = ServiceUtil.fillFields(rows, new String[]{"status"}, new Map<?, ?>[]{getStatusMap()}, null);
		UserDao userDao = webApplicationContext.getBean(UserDao.class);
		rows = ServiceUtil.fillRelations(rows, userDao,
				new String[]{"create_uid", "update_uid"}, new String[]{"id", "username", "name"}, null);
		return DPUtil.buildMap(new String[]{"total", "rows"}, new Object[]{total, rows});
	}
	
	public Map<String, Object> getInfo(Object id) {
		RoleDao dao = webApplicationContext.getBean(RoleDao.class);
		return dao.where("id = :id", ":id", id).one();
	}
	
	public int insert(Map<String, Object> data) {
		if(DPUtil.empty(data.get("id"))) data.remove("id");
		RoleDao dao = webApplicationContext.getBean(RoleDao.class);
		return dao.insert(data).intValue();
	}
	
	public int update(Map<String, Object> data) {
		RoleDao dao = webApplicationContext.getBean(RoleDao.class);
		return dao.where("id = :id", ":id", data.get("id")).update(data).intValue();
	}
	
	public int delete(Object ...ids) {
		if(DPUtil.empty(ids)) return -1;
		RoleDao dao = webApplicationContext.getBean(RoleDao.class);
		return dao.where("id in ("
			+ DPUtil.implode(",", DPUtil.arrayToIntegerArray(ids)) + ")", new HashMap<>()).delete().intValue();
	}
	
}
