package com.iisquare.jwframe.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

import com.iisquare.jwframe.dao.MenuDao;
import com.iisquare.jwframe.mvc.ServiceBase;

@Service
@Scope("singleton")
public class MenuService extends ServiceBase {
	
	private static Map<String, List<Map<String, Object>>> generateTree = new HashMap<>();
	@Autowired
	protected WebApplicationContext webApplicationContext;

	public List<Map<String, Object>> generateTree(Map<Object, Map<String, Object>> itemMap, Object parent) {
		List<Map<String, Object>> list = new ArrayList<>();
		for (Entry<Object, Map<String, Object>> entry : itemMap.entrySet()) {
			Object key = entry.getKey();
			Map<String, Object> value = entry.getValue();
			if(!parent.equals(value.get("parent_id"))) continue;
			value.put("children", generateTree(itemMap, key));
			list.add(value);
		}
		return list;
	}
	
	public List<Map<String, Object>> generateTree(String module, boolean forceReload) {
		if(null == module) module = "backend";
		if(!forceReload && generateTree.containsKey(module)) return generateTree.get(module);
		MenuDao menuDao = webApplicationContext.getBean(MenuDao.class);
		Map<Object, Map<String, Object>> itemMap = menuDao.select("id,name,parent_id,url,target,icon,state")
			.where("module=:module and status=1", ":module", module).orderBy("sort acs").all("id");
		List<Map<String, Object>> list = generateTree(itemMap, 0);
		generateTree.put(module, list);
		return list;
	}
	
}
