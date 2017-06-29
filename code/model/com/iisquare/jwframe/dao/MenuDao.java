package com.iisquare.jwframe.dao;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.iisquare.jwframe.mvc.MySQLBase;

/**
 * 菜单信息表
 * @author Ouyang <iisquare@163.com>
 */
@Component
@Scope("prototype")
public class MenuDao extends MySQLBase<MenuDao> {

	protected LinkedHashMap<String, Map<String, Object>> columns = null;
	
	public MenuDao() {}
	
	@Override
	public String tableName() {
		return tablePrefix() + "menu";
	}

	@Override
	public LinkedHashMap<String, Map<String, Object>> columns() {
		if(null != columns) return columns;
		columns = new LinkedHashMap<>();
		columns.put("id", null); // 主键
		columns.put("name", null); // 名称
		columns.put("parent_id", null); // 父级
		columns.put("module", null); // 模块
		columns.put("url", null); // 链接地址
		columns.put("target", null); // 打开方式
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
