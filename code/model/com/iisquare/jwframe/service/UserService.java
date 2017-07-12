package com.iisquare.jwframe.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

import com.iisquare.jwframe.Configuration;
import com.iisquare.jwframe.dao.RelationDao;
import com.iisquare.jwframe.dao.RoleDao;
import com.iisquare.jwframe.dao.UserDao;
import com.iisquare.jwframe.mvc.ServiceBase;
import com.iisquare.jwframe.utils.CodeUtil;
import com.iisquare.jwframe.utils.DPUtil;
import com.iisquare.jwframe.utils.ServiceUtil;
import com.iisquare.jwframe.utils.ServletUtil;

@Service
@Scope("prototype")
public class UserService extends ServiceBase {
	
	@Autowired
	protected WebApplicationContext webApplicationContext;
	@Autowired
	protected Configuration configuration;
	
	public String generatePassword(String password, String salt) {
		return CodeUtil.md5(password + salt);
	}
	
	public List<Map<String, Object>> getRoleRelList(Object userId) {
		RelationDao dao = webApplicationContext.getBean(RelationDao.class);
		return dao.where("type='user_role' and aid=:userId", ":userId", userId).all();
	}
	
	public boolean permit(Object userId, Object[] roleIds) {
		RelationDao dao = webApplicationContext.getBean(RelationDao.class);
		Number result = dao.where("type='user_role' and aid=:userId", ":userId", userId).delete();
		if(null == result) return false;
		List<Map<String, Object>> datas = new ArrayList<>();
		for (Object id : roleIds) {
			Map<String, Object> item = new LinkedHashMap<>();
			item.put("type", "user_role");
			item.put("aid", userId);
			item.put("bid", id);
			datas.add(item);
		}
		result = dao.batchInsert(datas);
		return null != result;
	}
	
	public Map<String, String> getStatusMap() {
		Map<String, String> map = new LinkedHashMap<String, String>();
		map.put("0", "禁用");
		map.put("1", "正常");
		return map;
	}
	
	public boolean setCurrentUserInfo(HttpServletRequest request, Map<String, Object> userInfo) {
		if(null == userInfo) return false;
		Object uid = userInfo.get("id");
		if(DPUtil.empty(uid)) return false;
		request.getSession().setAttribute("uid", uid);
		UserDao dao = webApplicationContext.getBean(UserDao.class);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("active_ip", ServletUtil.getRemoteAddr(request));
		data.put("active_time", System.currentTimeMillis());
		dao.where("id = :id", ":id", uid).update(data);
		return true;
	}
	
	public Map<String, Object> getCurrentUserInfo(HttpServletRequest request) {
		int uid = DPUtil.parseInt(request.getSession().getAttribute("uid"));
		if(DPUtil.empty(uid)) return null;
		UserDao dao = webApplicationContext.getBean(UserDao.class);
		Map<String, Object> info = dao.select("id,name,username,status").where("id = :id", ":id", uid).one();
		if(null == info || 1 != DPUtil.parseInt(info.get("status"))) return null;
		return info;
	}
	
	public Map<String, Object> getInfoByUsername(Object username) {
		UserDao dao = webApplicationContext.getBean(UserDao.class);
		return dao.where("username = :username", ":username", username).one();
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
		return DPUtil.buildMap(new String[]{"total", "rows"}, new Object[]{total, fillList(rows)});
	}
	
	@SuppressWarnings("unchecked")
	public String formatRoleRelText(List<Map<String, Object>> list) {
		if(DPUtil.empty(list)) return "";
		List<Object> returnList = new ArrayList<Object>();
		for (Map<String, Object> map : list) {
			Object roleRel = map.get("bid_info");
			if(DPUtil.empty(roleRel)) continue ;
			returnList.add(((Map<String, Object>) roleRel).get("name"));
		}
		return DPUtil.implode(",", DPUtil.collectionToArray(returnList));
	}
	
	public List<Map<String, Object>> fillList(List<Map<String, Object>> list) {
		Set<Object> ids = ServiceUtil.getFieldValues(list, "id");
		if(DPUtil.empty(ids)) return list;
		RelationDao relationDao = webApplicationContext.getBean(RelationDao.class);
		List<Map<String, Object>> roleRelList = relationDao.where(
				"type='user_role' and aid in (" + DPUtil.implode(",", DPUtil.collectionToArray(ids)) + ")").all();
		RoleDao roleDao = webApplicationContext.getBean(RoleDao.class);
		roleRelList = ServiceUtil.fillRelations(roleRelList, roleDao, new String[]{"bid"}, new String[]{"id", "name"}, null);
		Map<Object, List<Map<String, Object>>> roleIndexMapList = ServiceUtil.indexesMapList(roleRelList, "aid");
		String roleRelKey = DPUtil.stringConcat("role", ServiceUtil.relInfoPostfix);
		String roleRelKeyText = DPUtil.stringConcat(roleRelKey, "_text");
		List<Map<String, Object>> tempList;
		for (Map<String, Object> map : list) {
			map.remove("password");
			tempList = roleIndexMapList.get(map.get("id"));
			map.put(roleRelKey, ServiceUtil.getFieldValues(tempList, "bid_info"));
			map.put(roleRelKeyText, formatRoleRelText(tempList));
		}
		return list;
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
