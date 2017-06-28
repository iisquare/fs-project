package com.iisquare.jwframe.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

import com.iisquare.jwframe.dao.SettingDao;
import com.iisquare.jwframe.mvc.ServiceBase;

@Service
@Scope("singleton")
public class SettingService extends ServiceBase {
	
	@Autowired
	protected WebApplicationContext webApplicationContext;

	public boolean exists(String type, String key) {
		SettingDao settingDao = webApplicationContext.getBean(SettingDao.class);
		Number result = settingDao.select("content")
				.where("type=:type and parameter=:parameter", ":type", type, ":parameter", key).count();
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
				.where("type=:type and parameter=:parameter", ":type", type, ":parameter", key).one();
		if(null == info) return defaultValue;
		Object value = info.get("content");
		if(null == value) return defaultValue;
		return value.toString();
	}
	
}
