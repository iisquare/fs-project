package com.iisquare.jwframe.service;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
	
	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> generateTree(boolean forceReload) {
		if(!forceReload && null != generateTree) return generateTree;
		List<Map<String, Object>> list = new ArrayList<>();
		String path = System.getProperty("ETL_HOME");
		if(null == path) {
			path = "";
		} else {
			path += "/";
		}
		File pluginsDir = new File(path + "plugins");
		if(!pluginsDir.exists() || !pluginsDir.isDirectory()) return generateTree = list;
		Map<String, Map<String, Object>> itemMap = new LinkedHashMap<>();
		for (File file : pluginsDir.listFiles()) {
			String json = FileUtil.getContent(file.getAbsolutePath() + "/config.json");
			if(null == json) continue;
			List<?> itemList = DPUtil.parseJSON(json, List.class);
			if(null == itemList) continue;
			for (Object obj : itemList) {
				Map<String, Object> item = (Map<String, Object>) obj;
				itemMap.put(item.get("id").toString(), item);
			}
		}
		return generateTree = list;
	}
	
}
