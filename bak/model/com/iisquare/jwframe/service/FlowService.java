package com.iisquare.jwframe.service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

import com.iisquare.jwframe.Configuration;
import com.iisquare.jwframe.dao.FlowDao;
import com.iisquare.jwframe.dao.UserDao;
import com.iisquare.jwframe.mvc.ServiceBase;
import com.iisquare.jwframe.utils.DPUtil;
import com.iisquare.jwframe.utils.FileUtil;
import com.iisquare.jwframe.utils.ServiceUtil;

@Service
@Scope("prototype")
public class FlowService extends ServiceBase {
	
	@Autowired
	protected WebApplicationContext webApplicationContext;
	private static List<Map<String, Object>> pluginTree = null;
	private static Set<String> pluginJars = null;
	private static Map<String, String> pluginDependencies = null;
	@Autowired
	protected Configuration configuration;
	
	public Map<String, String> getStatusMap() {
		Map<String, String> map = new LinkedHashMap<String, String>();
		map.put("0", "禁用");
		map.put("1", "正常");
		return map;
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
		FlowDao dao = webApplicationContext.getBean(FlowDao.class);
		int total = dao.where(condition.toString(), params).count().intValue();
		List<Map<String, Object>> rows = dao.orderBy(orderBy).page(page, pageSize).all();
		rows = ServiceUtil.fillFields(rows, new String[]{"status"}, new Map<?, ?>[]{getStatusMap()}, null);
		UserDao userDao = webApplicationContext.getBean(UserDao.class);
		rows = ServiceUtil.fillRelations(rows, userDao,
				new String[]{"create_uid", "update_uid"}, new String[]{"id", "username", "name"}, null);
		return DPUtil.buildMap(new String[]{"total", "rows"}, new Object[]{total, rows});
	}
	
	public Map<String, Object> getInfo(Object id) {
		FlowDao dao = webApplicationContext.getBean(FlowDao.class);
		return dao.where("id = :id", ":id", id).one();
	}
	
	public int insert(Map<String, Object> data) {
		if(DPUtil.empty(data.get("id"))) data.remove("id");
		FlowDao dao = webApplicationContext.getBean(FlowDao.class);
		return dao.insert(data).intValue();
	}
	
	public int update(Map<String, Object> data) {
		FlowDao dao = webApplicationContext.getBean(FlowDao.class);
		return dao.where("id = :id", ":id", data.get("id")).update(data).intValue();
	}
	
	public int delete(Object ...ids) {
		if(DPUtil.empty(ids)) return -1;
		FlowDao dao = webApplicationContext.getBean(FlowDao.class);
		// if(count > 0) return -2; 暂不处理依赖关系
		return dao.where("id in ("
			+ DPUtil.implode(",", DPUtil.arrayToIntegerArray(ids)) + ")", new HashMap<>()).delete().intValue();
	}
	
	public List<Map<String, Object>> pluginTree(Map<String, Map<String, Object>> itemMap, String parent) {
		Map<Integer, Map<String, Object>> map = new TreeMap<>();
		for (Entry<String, Map<String, Object>> entry : itemMap.entrySet()) {
			String key = entry.getKey();
			Map<String, Object> value = entry.getValue();
			if(!parent.equals(value.get("parent"))) continue;
			if(value.containsKey("children")) value.put("children", pluginTree(itemMap, key));
			map.put(DPUtil.parseInt(value.get("sort")), value);
		}
		return new ArrayList<>(map.values());
	}
	
	public String getPluginsPath() {
		String path = System.getProperty("ETL_HOME");
		if(null == path) {
			path = "";
		} else {
			path += "/";
		}
		return path + "plugins";
	}
	
	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> pluginTree(boolean forceReload) {
		if(!forceReload && null != pluginTree) return pluginTree;
		File pluginsDir = new File(getPluginsPath());
		if(!pluginsDir.exists() || !pluginsDir.isDirectory()) return pluginTree = new ArrayList<>();
		Map<String, Map<String, Object>> itemMap = new LinkedHashMap<>();
		for (File file : pluginsDir.listFiles()) {
			String json = FileUtil.getContent(file.getAbsolutePath() + "/config.json");
			if(null == json) continue;
			Map<?, ?> flowMap = DPUtil.parseJSON(json, Map.class);
			if(null == flowMap) continue;
			List<?> itemList = (List<?>) flowMap.get("nodes");
			if(null == itemList) continue;
			for (Object obj : itemList) {
				Map<String, Object> item = (Map<String, Object>) obj;
				itemMap.put(item.get("id").toString(), item);
			}
		}
		return pluginTree = pluginTree(itemMap, "");
	}
	
	public Set<String> pluginJars(String uri, boolean forceReload) {
		if(!forceReload && null != pluginJars) return pluginJars;
		Set<String> jarsSet = new HashSet<>();
		File pluginsDir = new File(getPluginsPath());
		if(!pluginsDir.exists() || !pluginsDir.isDirectory()) return pluginJars = jarsSet;
		for (File file : pluginsDir.listFiles()) {
			if(!file.isDirectory()) continue;
			for (File jar : file.listFiles()) {
				if(!jar.getName().endsWith(".jar")) continue;
				if(null == uri) {
					jarsSet.add(jar.getAbsolutePath());
				} else {
					jarsSet.add(uri + "/" + file.getName() + "/" + jar.getName());
				}
			}
			File libDir = new File(file.getAbsolutePath() + "/lib");
			if(!libDir.exists() || !libDir.isDirectory()) continue;
			for (File libJar : libDir.listFiles()) {
				if(!libJar.getName().endsWith(".jar")) continue;
				if(null == uri) {
					jarsSet.add(libJar.getAbsolutePath());
				} else {
					jarsSet.add(uri + "/" + file.getName() + "/lib/" + libJar.getName());
				}
			}
		}
		return pluginJars = jarsSet;
	}
	
	public Set<String> pluginPackages() {
		Set<String> set = new HashSet<>();
		if(null == pluginDependencies(false)) return set;
		for (Entry<String, String> entry : pluginDependencies.entrySet()) {
			String key = entry.getKey();
			if(!key.startsWith("packages:")) continue;
			set.add(key.replaceFirst("packages:", "") + ":" + entry.getValue());
		}
		return set;
	}
	
	public Set<String> pluginExcludePackages() {
		Set<String> set = new HashSet<>();
		if(null == pluginDependencies(false)) return set;
		for (Entry<String, String> entry : pluginDependencies.entrySet()) {
			String key = entry.getKey();
			if(!key.startsWith("exclude:")) continue;
			set.add(key.replaceFirst("exclude:", ""));
		}
		return set;
	}
	
	@SuppressWarnings("unchecked")
	public Map<String, String> pluginDependencies(boolean forceReload) {
		if(!forceReload && null != pluginDependencies) return pluginDependencies;
		File pluginsDir = new File(getPluginsPath());
		if(!pluginsDir.exists() || !pluginsDir.isDirectory()) return pluginDependencies = new LinkedHashMap<>();
		Map<String, String> itemMap = new LinkedHashMap<>();
		for (File file : pluginsDir.listFiles()) {
			String json = FileUtil.getContent(file.getAbsolutePath() + "/config.json");
			if(null == json) continue;
			Map<?, ?> flowMap = DPUtil.parseJSON(json, Map.class);
			if(null == flowMap) continue;
			Map<?, ?> dependenciesMap = (Map<?, ?>) flowMap.get("dependencies");
			if(null == dependenciesMap) continue;
			List<?> itemList = (List<?>) dependenciesMap.get("packages");
			if(null != itemList) {
				for (Object obj : itemList) {
					Map<String, String> item = (Map<String, String>) obj;
					String key = "packages:" + item.get("groupId") + ":" + item.get("artifactId");
					String value = item.get("version");
					String version = itemMap.get(key);
					if(null != version && version.compareToIgnoreCase(value) < 0) continue;
					itemMap.put(key, value);
				}
			}
			itemList = (List<?>) dependenciesMap.get("exclude");
			if(null != itemList) {
				for (Object obj : itemList) {
					Map<String, String> item = (Map<String, String>) obj;
					String key = "exclude:" + item.get("groupId") + ":" + item.get("artifactId");
					itemMap.put(key, "*");
				}
			}
		}
		return pluginDependencies = itemMap;
	}
	
}
