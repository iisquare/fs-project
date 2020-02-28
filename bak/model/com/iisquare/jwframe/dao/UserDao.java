package com.iisquare.jwframe.dao;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.iisquare.jwframe.mvc.MySQLBase;

/**
 * 用户信息表
 * @author Ouyang <iisquare@163.com>
 */
@Component
@Scope("prototype")
public class UserDao extends MySQLBase<UserDao> {

	protected LinkedHashMap<String, Map<String, Object>> columns = null;
	
	public UserDao() {}
	
	@Override
	public String tableName() {
		return tablePrefix() + "user";
	}

	@Override
	public LinkedHashMap<String, Map<String, Object>> columns() {
		if(null != columns) return columns;
		columns = new LinkedHashMap<>();
		columns.put("id", null); // 主键
		columns.put("name", null); // 昵称
		columns.put("username", null); // 账号
		columns.put("password", null); // 密码
		columns.put("salt", null); // 混淆码
		columns.put("status", null); // 状态
		columns.put("sort", null); // 排序
		columns.put("description", null); // 描述
		columns.put("active_ip", null); // 最后登录IP
		columns.put("active_time", null); // 最后登录时间
		columns.put("create_uid", null); // 创建者
		columns.put("create_time", null); // 创建时间
		columns.put("update_uid", null); // 修改者
		columns.put("update_time", null); // 修改时间
		return columns;
	}
	
}
