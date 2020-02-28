package com.iisquare.jwframe.dao;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.iisquare.jwframe.mvc.MySQLBase;

/**
 * 配置信息表
 * @author Ouyang <iisquare@163.com>
 */
@Component
@Scope("prototype")
public class SettingDao extends MySQLBase<SettingDao> {

	protected LinkedHashMap<String, Map<String, Object>> columns = null;
	
	public SettingDao() {}
	
	@Override
	public String tableName() {
		return tablePrefix() + "setting";
	}

	@Override
	public LinkedHashMap<String, Map<String, Object>> columns() {
		if(null != columns) return columns;
		columns = new LinkedHashMap<>();
		columns.put("type", null); // 类型
		columns.put("parameter", null); // 参数名
		columns.put("name", null); // 名称
		columns.put("content", null); // 参数值
		columns.put("sort", null); // 排序
		columns.put("description", null); // 描述
		columns.put("update_uid", null); // 修改者
		columns.put("update_time", null); // 修改时间
		return columns;
	}
	
}
