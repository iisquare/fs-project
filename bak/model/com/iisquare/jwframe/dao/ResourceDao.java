package com.iisquare.jwframe.dao;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.iisquare.jwframe.mvc.MySQLBase;

/**
 * 资源信息表
 * @author Ouyang <iisquare@163.com>
 */
@Component
@Scope("prototype")
public class ResourceDao extends MySQLBase<ResourceDao> {

	protected LinkedHashMap<String, Map<String, Object>> columns = null;
	
	public ResourceDao() {}
	
	@Override
	public String tableName() {
		return tablePrefix() + "resource";
	}

	@Override
	public LinkedHashMap<String, Map<String, Object>> columns() {
		if(null != columns) return columns;
		columns = new LinkedHashMap<>();
		columns.put("id", null); // 主键
		columns.put("name", null); // 名称
		columns.put("parent_id", null); // 父级
		columns.put("module", null); // 模块
		columns.put("controller", null); // 控制器
		columns.put("action", null); // 活动
		columns.put("operation", null); // 操作
		columns.put("status", null); // 状态
		columns.put("sort", null); // 排序
		columns.put("description", null); // 描述
		columns.put("create_uid", null); // 创建者
		columns.put("create_time", null); // 创建时间
		columns.put("update_uid", null); // 修改者
		columns.put("update_time", null); // 修改时间
		return columns;
	}
	
}
