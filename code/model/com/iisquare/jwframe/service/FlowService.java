package com.iisquare.jwframe.service;

import java.io.File;
import java.util.ArrayList;
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

import com.iisquare.jwframe.mvc.ServiceBase;
import com.iisquare.jwframe.utils.DPUtil;
import com.iisquare.jwframe.utils.FileUtil;

@Service
@Scope("singleton")
public class FlowService extends ServiceBase {
	
	@Autowired
	protected WebApplicationContext webApplicationContext;
	private static List<Map<String, Object>> generateTree = null;
	private static Set<String> generateJars = null;
	private static Map<String, String> generateDependencies = null;
	
	public List<Map<String, Object>> generateTree(Map<String, Map<String, Object>> itemMap, String parent) {
		Map<Integer, Map<String, Object>> map = new TreeMap<>();
		for (Entry<String, Map<String, Object>> entry : itemMap.entrySet()) {
			String key = entry.getKey();
			Map<String, Object> value = entry.getValue();
			if(!parent.equals(value.get("parent"))) continue;
			if(value.containsKey("children")) value.put("children", generateTree(itemMap, key));
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
	public List<Map<String, Object>> generateTree(boolean forceReload) {
		if(!forceReload && null != generateTree) return generateTree;
		File pluginsDir = new File(getPluginsPath());
		if(!pluginsDir.exists() || !pluginsDir.isDirectory()) return generateTree = new ArrayList<>();
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
		return generateTree = generateTree(itemMap, "");
	}
	
	public Set<String> generateJars(String uri, boolean forceReload) {
		if(!forceReload && null != generateJars) return generateJars;
		Set<String> jarsSet = new HashSet<>();
		File pluginsDir = new File(getPluginsPath());
		if(!pluginsDir.exists() || !pluginsDir.isDirectory()) return generateJars = jarsSet;
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
		return generateJars = jarsSet;
	}
	
	public Set<String> generatePackages() {
		Set<String> set = new HashSet<>();
		if(null == generateDependencies(false)) return set;
		for (Entry<String, String> entry : generateDependencies.entrySet()) {
			String key = entry.getKey();
			if(!key.startsWith("packages:")) continue;
			set.add(key.replaceFirst("packages:", "") + ":" + entry.getValue());
		}
		return set;
	}
	
	public Set<String> generateExcludePackages() {
		Set<String> set = new HashSet<>();
		if(null == generateDependencies(false)) return set;
		for (Entry<String, String> entry : generateDependencies.entrySet()) {
			String key = entry.getKey();
			if(!key.startsWith("exclude:")) continue;
			set.add(key.replaceFirst("exclude:", ""));
		}
		return set;
	}
	
	@SuppressWarnings("unchecked")
	public Map<String, String> generateDependencies(boolean forceReload) {
		if(!forceReload && null != generateDependencies) return generateDependencies;
		File pluginsDir = new File(getPluginsPath());
		if(!pluginsDir.exists() || !pluginsDir.isDirectory()) return generateDependencies = new LinkedHashMap<>();
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
		return generateDependencies = itemMap;
	}
	
}
