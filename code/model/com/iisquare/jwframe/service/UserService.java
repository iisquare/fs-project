package com.iisquare.jwframe.service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

import com.iisquare.jwframe.Configuration;
import com.iisquare.jwframe.dao.UserDao;
import com.iisquare.jwframe.mvc.ServiceBase;
import com.iisquare.jwframe.utils.CodeUtil;
import com.iisquare.jwframe.utils.DPUtil;
import com.iisquare.jwframe.utils.ServiceUtil;

@Service
@Scope("singleton")
public class UserService extends ServiceBase {
	
	@Autowired
	protected WebApplicationContext webApplicationContext;
	@Autowired
	protected Configuration configuration;
	
	public String generatePassword(String password, String salt) {
		return CodeUtil.md5(password + salt);
	}
	
	public Map<String, String> getStatusMap() {
		Map<String, String> map = new LinkedHashMap<String, String>();
		map.put("0", "禁用");
		map.put("1", "正常");
		return map;
	}
	
	public int delete(Object ...ids) {
		if(DPUtil.empty(ids)) return -1;
		UserDao dao = webApplicationContext.getBean(UserDao.class);
		return dao.where("id in ("
			+ DPUtil.implode(",", DPUtil.arrayToIntegerArray(ids)) + ")", new HashMap<>()).delete().intValue();
	}
	
	public Map<Object, Object> search(Map<String, Object> map, String orderBy, int page, int pageSize) {
		StringBuilder condition = new StringBuilder("1=1");
		Map<String, Object> params = new HashMap<String, Object>();
		Object name = map.get("name");
		if(!DPUtil.empty(name)) {
			condition.append(" and name like :name");
			params.put(":name", "%" + name + "%");
		}
		Object username = map.get("username");
		if(!DPUtil.empty(username)) {
			condition.append(" and username like :username");
			params.put(":username", "%" + username + "%");
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
		Object timeCreateStart = map.get("timeCreateStart");
		if(!DPUtil.empty(timeCreateStart)) {
			condition.append(" and create_time >= :timeCreateStart");
			params.put(":timeCreateStart", DPUtil.dateTimeToMillis(timeCreateStart, configuration.getDateTimeFormat()));
		}
		Object timeCreateEnd = map.get("timeCreateEnd");
		if(!DPUtil.empty(timeCreateEnd)) {
			condition.append(" and create_time <= :timeCreateEnd");
			params.put(":timeCreateEnd", DPUtil.dateTimeToMillis(timeCreateEnd, configuration.getDateTimeFormat()));
		}
		Object timeUpdateStart = map.get("timeUpdateStart");
		if(!DPUtil.empty(timeUpdateStart)) {
			condition.append(" and update_time >= :timeUpdateStart");
			params.put(":timeUpdateStart", DPUtil.dateTimeToMillis(timeUpdateStart, configuration.getDateTimeFormat()));
		}
		Object timeUpdateEnd = map.get("timeUpdateEnd");
		if(!DPUtil.empty(timeUpdateEnd)) {
			condition.append(" and update_time <= :timeUpdateEnd");
			params.put(":timeUpdateEnd", DPUtil.dateTimeToMillis(timeUpdateEnd, configuration.getDateTimeFormat()));
		}
		UserDao dao = webApplicationContext.getBean(UserDao.class);
		int total = dao.where(condition.toString(), params).count().intValue();
		List<Map<String, Object>> rows = dao.orderBy(orderBy).page(page, pageSize).all();
		rows = ServiceUtil.fillFields(rows, new String[]{"status"}, new Map<?, ?>[]{getStatusMap()}, null);
		UserDao userDao = webApplicationContext.getBean(UserDao.class);
		rows = ServiceUtil.fillRelations(rows, userDao,
				new String[]{"create_uid", "update_uid"}, new String[]{"id", "username", "name"}, null);
		return DPUtil.buildMap(new String[]{"total", "rows"}, new Object[]{total, rows});
	}
	
	public Map<String, Object> getInfo(Object id) {
		UserDao dao = webApplicationContext.getBean(UserDao.class);
		return dao.where("id = :id", ":id", id).one();
	}
	
	public int insert(Map<String, Object> data) {
		if(DPUtil.empty(data.get("id"))) data.remove("id");
		UserDao dao = webApplicationContext.getBean(UserDao.class);
		return dao.insert(data).intValue();
	}
	
	public int update(Map<String, Object> data) {
		UserDao dao = webApplicationContext.getBean(UserDao.class);
		return dao.where("id = :id", ":id", data.get("id")).update(data).intValue();
	}

}
