package com.iisquare.jwframe.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

import com.iisquare.jwframe.dao.MenuDao;
import com.iisquare.jwframe.mvc.ServiceBase;
import com.iisquare.jwframe.utils.DPUtil;

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
	
	/**
	 * 生成已选中菜单项集合
	 */
	public void generateChecked(Set<Object> checkedSet, Map<Object, Map<String, Object>> itemMap, Object id) {
		if(DPUtil.empty(id) || checkedSet.contains(id)) return;
		checkedSet.add(id);
		Map<String, Object> item = itemMap.get(id);
		if(null == item) return;
		generateChecked(checkedSet, itemMap, item.get("parent_id"));
	}
	
	public List<Map<String, Object>> generateTree(String webUrl, String module, String uri, boolean forceReload) {
		if(null == module) module = "backend";
		if(!forceReload && generateTree.containsKey(module)) return generateTree.get(module);
		MenuDao menuDao = webApplicationContext.getBean(MenuDao.class);
		Map<Object, Map<String, Object>> itemMap = menuDao.select("id,name,parent_id,url,target,icon,state")
			.where("module=:module and status=1", "module", module).orderBy("sort asc").all("id");
		int id = 0; // 查找当前菜单项
		for (Entry<Object, Map<String, Object>> entry : itemMap.entrySet()) {
			Map<String, Object> value = entry.getValue();
			if(0 == id && 0 == DPUtil.parseInt(value.get("parent_id"))) id = DPUtil.parseInt(value.get("id"));
			if(!uri.equals(value.get("url"))) continue;
			id = DPUtil.parseInt(value.get("id"));
			break;
		}
		// 生成已选中菜单集合
		Set<Object> checkedSet = new HashSet<>();
		generateChecked(checkedSet, itemMap, id);
		// 处理菜单数据
		for (Entry<Object, Map<String, Object>> entry : itemMap.entrySet()) {
			Map<String, Object> value = entry.getValue();
			value.put("checked", checkedSet.contains(value.get("id")));
			String url = DPUtil.parseString(value.get("url"));
			if(DPUtil.empty(url)) {
				url = "javascript:void(0);";
			} else if (url.startsWith("/")) {
				url = webUrl + url.replaceFirst("/", "");
			} else if(!url.matches("^\\w+://.*$")) {
				url = webUrl + url;
			}
			value.put("url", url);
		}
		// 生成树结构
		List<Map<String, Object>> list = generateTree(itemMap, 0);
		generateTree.put(module, list);
		return list;
	}
	
}
