package com.iisquare.jwframe.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

import com.iisquare.jwframe.dao.SettingDao;
import com.iisquare.jwframe.dao.UserDao;
import com.iisquare.jwframe.mvc.ServiceBase;
import com.iisquare.jwframe.utils.DPUtil;
import com.iisquare.jwframe.utils.ServiceUtil;

@Service
@Scope("singleton")
public class SettingService extends ServiceBase {
	
	@Autowired
	protected WebApplicationContext webApplicationContext;

	public int delete(Map<String, Object> map) {
		StringBuilder where = new StringBuilder("1=1");
		Map<String, Object> params = new HashMap<String, Object>();
		Object type = map.get("type");
		if(!DPUtil.empty(type)) {
			where.append(" and type = :type");
			params.put("type", type);
		}
		Object parameter = map.get("parameter");
		if(!DPUtil.empty(parameter)) {
			where.append(" and parameter = :parameter");
			params.put("parameter", parameter);
		}
		Object name = map.get("name");
		if(!DPUtil.empty(name)) {
			where.append(" and name = :name");
			params.put("name", name);
		}
		SettingDao settingDao = webApplicationContext.getBean(SettingDao.class);
		return settingDao.where(where.toString(), params).delete().intValue();
	}
	
	public Map<Object, Object> search(Map<String, Object> map, String orderBy, int page, int pageSize) {
		StringBuilder where = new StringBuilder("1=1");
		Map<String, Object> params = new HashMap<String, Object>();
		Object type = map.get("type");
		if(!DPUtil.empty(type)) {
			where.append(" and type = :type");
			params.put("type", type);
		}
		Object parameter = map.get("parameter");
		if(!DPUtil.empty(parameter)) {
			where.append(" and parameter like :parameter");
			params.put("parameter", "%" + parameter + "%");
		}
		Object name = map.get("name");
		if(!DPUtil.empty(name)) {
			where.append(" and name like :name");
			params.put("name", "%" + name + "%");
		}
		Object content = map.get("content");
		if(!DPUtil.empty(content)) {
			where.append(" and content like :content");
			params.put("content", "%" + content + "%");
		}
		Object description = map.get("description");
		if(!DPUtil.empty(description)) {
			where.append(" and description like :description");
			params.put("description", "%" + description + "%");
		}
		SettingDao settingDao = webApplicationContext.getBean(SettingDao.class);
		int total = settingDao.where(where.toString(), params).count().intValue();
		List<Map<String, Object>> rows = settingDao.orderBy(orderBy).page(page, pageSize).all();
		UserDao userDao = webApplicationContext.getBean(UserDao.class);
		rows = ServiceUtil.fillRelations(rows, userDao,
				new String[]{"update_uid"}, new String[]{"id", "username", "name"}, null);
		return DPUtil.buildMap(new String[]{"total", "rows"}, new Object[]{total, rows});
	}
	
	public boolean exists(String type, String key) {
		SettingDao settingDao = webApplicationContext.getBean(SettingDao.class);
		Number result = settingDao.select("content")
				.where("type=:type and parameter=:parameter", "type", type, "parameter", key).count();
		return null != result && result.intValue() > 0;
	}
	
	public boolean setProperty(String type, String key, String value) {
		if(null == type) type = "system";
		SettingDao settingDao = webApplicationContext.getBean(SettingDao.class);
		Number result;
		if(exists(type, key)) {
			Map<String, Object> data = new HashMap<>();
			data.put("content", value);
			result = settingDao.where("type=? and parameter=?", type, key).update(data);
		} else {
			Map<String, Object> data = new HashMap<>();
			data.put("type", type);
			data.put("parameter", key);
			data.put("content", value);
			result = settingDao.insert(data);
		}
		return null != result;
	}
	
	public String getProperty(String type, String key, String defaultValue) {
		if(null == type) type = "system";
		SettingDao settingDao = webApplicationContext.getBean(SettingDao.class);
		Map<String, Object> info = settingDao.select("content")
				.where("type=:type and parameter=:parameter", "type", type, "parameter", key).one();
		if(null == info) return defaultValue;
		Object value = info.get("content");
		if(null == value) return defaultValue;
		return value.toString();
	}
	
}
