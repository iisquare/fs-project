package com.iisquare.jwframe.dao;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.iisquare.jwframe.mvc.MySQLBase;

/**
 * 关联信息表
 * @author Ouyang <iisquare@163.com>
 */
@Component
@Scope("prototype")
public class RelationDao extends MySQLBase<RelationDao> {

	protected LinkedHashMap<String, Map<String, Object>> columns = null;
	
	public RelationDao() {}
	
	@Override
	public String tableName() {
		return tablePrefix() + "relation";
	}

	@Override
	public LinkedHashMap<String, Map<String, Object>> columns() {
		if(null != columns) return columns;
		columns = new LinkedHashMap<>();
		columns.put("type", null); // 类型
		columns.put("aid", null); // 主键
		columns.put("bid", null); // 主键
		return columns;
	}
	
}
